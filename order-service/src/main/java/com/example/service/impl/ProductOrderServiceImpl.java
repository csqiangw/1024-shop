package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.component.PayFactory;
import com.example.config.RabbitMQConfig;
import com.example.constant.CacheKey;
import com.example.constant.TimeConstant;
import com.example.enums.*;
import com.example.exception.BizException;
import com.example.feign.CouponFeignService;
import com.example.feign.ProductFeignService;
import com.example.feign.UserFeignService;
import com.example.interceptor.LoginInterceptor;
import com.example.mapper.ProductOrderItemMapper;
import com.example.mapper.ProductOrderMapper;
import com.example.model.LoginUser;
import com.example.model.OrderMessage;
import com.example.model.ProductOrderDO;
import com.example.model.ProductOrderItemDO;
import com.example.request.*;
import com.example.service.ProductOrderService;
import com.example.util.CommonUtil;
import com.example.util.JsonData;
import com.example.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author qiangw
 * @since 2023-04-02
 */
@Service
@Slf4j
public class ProductOrderServiceImpl implements ProductOrderService {

    @Autowired
    private ProductOrderMapper productOrderMapper;

    @Autowired
    private ProductOrderItemMapper orderItemMapper;

    @Autowired
    private UserFeignService userFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Autowired
    private PayFactory payFactory;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * service编写伪代码
     *
     * 本身业务复杂，qps上不去，做限流
     * 会和很多微服务交互
     *
     * 优化：让一部分操作并行，比如用户微服务 / 商品微服务，开启多个线程
     * 比如一些校验不做，比如用户微服务，或者晚做，或者让前端做
     * 学京东，先创建订单 再支付
     *
     * 反作弊服务：也耗性能，难扩展
     *
     * 一步拆多步 + 异步
     *
     * - 防重提交
     * - 用户微服务-确认收货地址 越权处理
     * - 商品微服务-获取最新购物项和价格
     * - 订单验价
     *   - 优惠券微服务-获取优惠券
     *   - 验证价格
     *
     * - 锁定优惠券
     * - 锁定商品库存
     * - 创建订单对象
     * - 创建子订单对象
     * - 发送延迟消息-用于自动关单
     * - 创建支付信息-对接三方支付
     * @param orderRequest
     * @return
     */
    @Override
    @Transactional
    public JsonData confirmOrder(ConfirmOrderRequest orderRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        String orderToken = orderRequest.getToken();
        if(StringUtils.isBlank(orderToken)){
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_TOKEN_NOT_EXIST);
        }
        //原子操作 校验令牌 删除令牌
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(String.format(CacheKey.SUBMIT_ORDER_TOKEN_KEY, loginUser.getId())),orderToken);
        if(result == 0){
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_TOKEN_EQUAL_FAIL);
        }
        String orderOutTradeNo = CommonUtil.getStringNumRandom(32);//生成一个32位的订单号

        ProductOrderAddressVO addressVO = this.getUserAddress(orderRequest.getAddressId());
        log.info("收获地址信息:{}",addressVO);

        //获取用户加入购物车的商品
        List<Long> productIdList = orderRequest.getProductIdList();
        JsonData cartItemDate = productFeignService.confirmOrderCartItems(productIdList);
        List<OrderItemVO> orderItemVOList = cartItemDate.getData(new TypeReference<List<OrderItemVO>>(){});
        if(orderItemVOList == null){
            //购物车商品不存在
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_CART_ITEM_NOT_EXIST);
        }
        //TODO 下单失败后，购物车数据已经被清理了，这时候，要回滚，但不要用分布式事物，还是用消息队列

        //验证价格,减去商品优惠券
        checkPrice(orderItemVOList,orderRequest);

        //锁定优惠券
        lockCouponRecords(orderRequest,orderOutTradeNo);
        //锁定库存
        lockProductStocks(orderItemVOList,orderOutTradeNo);

        //创建订单 TODO
        ProductOrderDO productOrderDO = saveProductOrder(orderRequest, loginUser, orderOutTradeNo, addressVO);

        //创建订单项
        saveProductOrderItems(orderOutTradeNo,productOrderDO.getId(),orderItemVOList);
        //发送延迟消息，用于自动关单
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOutTradeNo(orderOutTradeNo);
        rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(), rabbitMQConfig.getOrderCloseDelayRoutingKey(), orderMessage);

        //创建支付
        PayInfoVO payInfoVO = new PayInfoVO(orderOutTradeNo,productOrderDO.getPayAmount(),orderRequest.getPayType(),orderRequest.getClientType()
                            ,orderItemVOList.get(0).getProductTitle(),"", TimeConstant.ORDER_PAY_TIMEOUT_MILLS);
        String payResult = payFactory.pay(payInfoVO);
        if(StringUtils.isNotBlank(payResult)){
            log.info("创建支付订单成功:payInfoVO={},payResult={}",payInfoVO,payResult);
            return JsonData.buildSuccess(payResult);
        }else{
            log.error("创建支付订单失败:payInfoVO={},payResult={}",payInfoVO,payResult);
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_FAIL);
        }
    }

    /**
     * 新增订单项
     * @param orderOutTradeNo
     * @param productOrderId
     * @param orderItemVOList
     */
    private void saveProductOrderItems(String orderOutTradeNo, Long productOrderId, List<OrderItemVO> orderItemVOList) {
        List<ProductOrderItemDO> list = orderItemVOList.stream().map(obj -> {
            ProductOrderItemDO itemDO = new ProductOrderItemDO();
            itemDO.setBuyNum(obj.getBuyNum());
            itemDO.setCreateTime(new Date());
            itemDO.setProductId(obj.getProductId());
            itemDO.setOutTradeNo(orderOutTradeNo);
            itemDO.setProductImg(obj.getProductImg());
            itemDO.setProductName(obj.getProductTitle());
            itemDO.setProductOrderId(productOrderId);
            itemDO.setAmount(obj.getAmount());
            itemDO.setTotalAmount(obj.getTotalAmount());
            return itemDO;
        }).collect(Collectors.toList());
        orderItemMapper.insertBatch(list);
    }

    private ProductOrderDO saveProductOrder(ConfirmOrderRequest orderRequest, LoginUser loginUser, String orderOutTradeNo, ProductOrderAddressVO addressVO) {
        ProductOrderDO productOrderDO = new ProductOrderDO();
        productOrderDO.setUserId(loginUser.getId());
        productOrderDO.setHeadImg(loginUser.getHeadImg());
        productOrderDO.setNickname(loginUser.getName());
        productOrderDO.setOutTradeNo(orderOutTradeNo);
        productOrderDO.setCreateTime(new Date());
        productOrderDO.setDel(0);
        productOrderDO.setOrderType(ProductOrderTypeEnum.DAILY.name());

        //实际支付的价格
        productOrderDO.setPayAmount(orderRequest.getRealPayAmount());

        //总价，未使用优惠券的价格
        productOrderDO.setTotalAmount(orderRequest.getTotalAmount());
        productOrderDO.setState(ProductOrderStateEnum.NEW.name());
        productOrderDO.setPayType(ProductOrderPayTypeEnum.valueOf(orderRequest.getPayType()).name());

        //收获地址
        productOrderDO.setReceiverAddress(JSON.toJSONString(addressVO));

        productOrderMapper.insert(productOrderDO);

        return productOrderDO;

    }

    //下面的两步锁定都可以采用异步，这样不阻塞
    /**
     * 锁定商品库存
     * @param orderItemVOList
     * @param orderOutTradeNo
     */
    private void lockProductStocks(List<OrderItemVO> orderItemVOList, String orderOutTradeNo) {
        List<OrderItemRequest> itemRequestList = orderItemVOList.stream().map(obj -> {
            OrderItemRequest request = new OrderItemRequest();
            request.setBuyNum(obj.getBuyNum());
            request.setProductId(obj.getProductId());
            return request;
        }).collect(Collectors.toList());
        LockProductRequest lockProductRequest = new LockProductRequest();
        lockProductRequest.setOrderOutTradeNo(orderOutTradeNo);
        lockProductRequest.setOrderItemList(itemRequestList);
        JsonData jsonData = productFeignService.lockProducts(lockProductRequest);
        if(jsonData.getCode() != 0){
            log.error("锁定商品库存失败:{}",lockProductRequest);
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);
        }
    }

    /**
     * 锁定优惠券
     * @param orderRequest
     * @param orderOutTradeNo
     */
    private void lockCouponRecords(ConfirmOrderRequest orderRequest, String orderOutTradeNo) {
        List<Long> lockCouponRecordsIds = new ArrayList<>();
        if(orderRequest.getCouponRecordId() > 0){
            lockCouponRecordsIds.add(orderRequest.getCouponRecordId());//这里只使用了一张优惠券
            LockCouponRecordRequest lockCouponRecordRequest = new LockCouponRecordRequest();
            lockCouponRecordRequest.setOrderOutTradeNo(orderOutTradeNo);
            lockCouponRecordRequest.setLockCouponRecordIds(lockCouponRecordsIds);

            //发起锁定优惠券请求
            JsonData jsonData = couponFeignService.lockCouponRecords(lockCouponRecordRequest);
            if(jsonData.getCode() != 0){
                throw new BizException(BizCodeEnum.COUPON_RECORD_LOCK_FAIL);
            }
        }
    }

    /**
     * 统计全部商品的价格
     * 获取优惠券（判断是否满足优惠券的条件），减去总价再减去优惠券的价格，就是最终的价格
     * @param orderItemVOList
     * @param orderRequest
     */
    private void checkPrice(List<OrderItemVO> orderItemVOList, ConfirmOrderRequest orderRequest) {
        //统计商品总价格
        BigDecimal realPayAmount = new BigDecimal("0");
        if(orderItemVOList != null){
            for (OrderItemVO orderItemVO : orderItemVOList){
                BigDecimal itemRealPayAmount = orderItemVO.getTotalAmount();
                realPayAmount = realPayAmount.add(itemRealPayAmount);
            }
        }
        //获取优惠券，判断是否可用
        CouponRecordVO couponRecordVO = CoupongetCartCouponRecord(orderRequest.getCouponRecordId());

        //计算购物车价格是否满足优惠券满减条件
        if(couponRecordVO != null){
            //计算是否满足满减
            if(realPayAmount.compareTo(couponRecordVO.getConditionPrice()) < 0){
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_COUPON_FAIL);
            }
            if(couponRecordVO.getPrice().compareTo(realPayAmount) > 0){
                realPayAmount = BigDecimal.ZERO;
            }else{
                realPayAmount = realPayAmount.subtract(couponRecordVO.getPrice());
            }
        }
        //验价，这里的价格和后台价格要匹配
        if(realPayAmount.compareTo(orderRequest.getRealPayAmount()) != 0){
            log.error("订单验价失败:{}",orderRequest);
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_PRICE_FAIL);
        }
    }

    /**
     * 获取优惠券
     * @param couponRecordId
     * @return
     */
    private CouponRecordVO CoupongetCartCouponRecord(Long couponRecordId) {
        if(couponRecordId == null || couponRecordId < 0){
            return null;
        }
        JsonData couponData = couponFeignService.findUserCouponRecordById(couponRecordId);
        if(couponData.getCode() != 0){
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_COUPON_FAIL);
        }
        //若成功 获取优惠券记录
        CouponRecordVO couponRecordVO = couponData.getData(new TypeReference<CouponRecordVO>(){});
        if(!couponAvaiable(couponRecordVO)){
            log.error("优惠券使用失败");
            throw new BizException(BizCodeEnum.COUPON_UNAVAILABLE);
        }
        return couponRecordVO;
    }

    /**
     * 判断优惠券是否可用
     * @param couponRecordVO
     * @return
     */
    private boolean couponAvaiable(CouponRecordVO couponRecordVO) {
        if(couponRecordVO.getUseState().equalsIgnoreCase(CouponStateEnum.NEW.name())){
            long currentTimestamp = CommonUtil.getCurrentTimestamp();
            long end = couponRecordVO.getEndTime().getTime();
            long start = couponRecordVO.getStartTime().getTime();
            if(currentTimestamp >= start && currentTimestamp <= end){//在日期内才可用
                return true;
            }
        }
        return false;
    }

    /**
     * 获取收获地址详情
     * @param addressId
     * @return
     */
    private ProductOrderAddressVO getUserAddress(long addressId) {
        JsonData jsonData = userFeignService.detail(addressId);
        if(jsonData.getCode() != 0){
            log.error("获取收获地址失败,msg:{}",jsonData);
            throw new BizException(BizCodeEnum.ADDRESS_NO_EXITS);
        }
        ProductOrderAddressVO addressVO = jsonData.getData(new TypeReference<ProductOrderAddressVO>(){});
        return addressVO;
    }

    /**
     * 查询订单状态
     * @param outTradeNo
     * @return
     */
    @Override
    public String queryProductOrderState(String outTradeNo) {
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(new QueryWrapper<ProductOrderDO>().eq("out_trade_no", outTradeNo));
        if(productOrderDO == null){
            return "";
        }else{
            return productOrderDO.getState();
        }
    }

    @Override
    public boolean closeProductOrder(OrderMessage orderMessage) {
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(new QueryWrapper<ProductOrderDO>().eq("out_trade_no",orderMessage.getOutTradeNo()));
        if(productOrderDO == null){
            log.warn("直接确认消息，订单不存在");
            return true;
        }
        if(productOrderDO.getState().equalsIgnoreCase(ProductOrderStateEnum.PAY.name())){
            //已经支付
            log.info("直接确认消息，订单已经支付");
            return true;
        }
        //向第三方查询订单是否真的支付，因为有可能第三方支付订单的消息没传过来，所以此时不能直接关单
        PayInfoVO payInfoVO = new PayInfoVO();
        payInfoVO.setPayType(productOrderDO.getPayType());
        payInfoVO.setOutTradeNo(orderMessage.getOutTradeNo());
        String payResult = payFactory.queryPaySuccess(payInfoVO);
        //结果未空，则未支付成功，本地取消订单
        if(StringUtils.isBlank(payResult)){
            //取消
            //玩意高并发情景出问题
            productOrderMapper.updateOrderPayState(productOrderDO.getOutTradeNo(),ProductOrderStateEnum.CANCEL.name(),ProductOrderStateEnum.NEW.name());
            return true;
        }else{//支付成功
            //将订单状态改为已经支付
            productOrderMapper.updateOrderPayState(productOrderDO.getOutTradeNo(),ProductOrderStateEnum.PAY.name(),ProductOrderStateEnum.NEW.name());
            log.warn("支付成功，主动的把订单装药改为已经支付，要排查第三方支付回调的问题:{}",orderMessage);
            return true;
        }
    }

    /**
     * 支付通知结果更新订单状态
     * @param payType
     * @param paramsMap
     * @return
     */
    @Override
    public JsonData handlerOrderCallbackMsg(ProductOrderPayTypeEnum payType, Map<String, String> paramsMap) {

        //MQ投递 -》 再慢慢得消费

        if(payType.name().equalsIgnoreCase(ProductOrderPayTypeEnum.ALIPAY.name())){
            //获取商户订单号
            String outTradeNo = paramsMap.get("out_trade_no");
            //交易的状态
            String tradeStatus = paramsMap.get("trade_status");
            if("TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) || "TRADE_FINISHED".equalsIgnoreCase(tradeStatus)){
                //更新订单状态
                productOrderMapper.updateOrderPayState(outTradeNo,ProductOrderStateEnum.PAY.name(),ProductOrderStateEnum.NEW.name());
                return JsonData.buildSuccess();
            }
        }else {//其它支付方式

        }
        return JsonData.buildResult(BizCodeEnum.PAY_ORDER_CALLBACK_NOT_SUCCESS);
    }

    /**
     * 分页查询我的订单
     * @param page
     * @param size
     * @param state
     * @return
     */
    @Override
    public Map<String, Object> page(int page, int size, String state) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        Page<ProductOrderDO> pageInfo = new Page<>(page,size);
        IPage<ProductOrderDO> orderDOIPage = null;
        if(StringUtils.isBlank(state)){//查询全部
            orderDOIPage = productOrderMapper.selectPage(pageInfo,new QueryWrapper<ProductOrderDO>().eq("user_id",loginUser.getId()));
        }else{
            orderDOIPage = productOrderMapper.selectPage(pageInfo,new QueryWrapper<ProductOrderDO>().eq("user_id",loginUser.getId()).eq("state",state));
        }
        //先获取订单列表
        List<ProductOrderDO> productOrderDOList = orderDOIPage.getRecords();

        //这面可以连表查询 订单列表包含订单项
        List<ProductOrderVO> productOrderVOList = productOrderDOList.stream().map(orderDO->{
            List<ProductOrderItemDO> itemDOList = orderItemMapper.selectList(new QueryWrapper<ProductOrderItemDO>().eq("product_order_id", orderDO.getId()));
            List<OrderItemVO> orderItemVOS = itemDOList.stream().map(item -> {
                OrderItemVO itemVO = new OrderItemVO();
                BeanUtils.copyProperties(item, itemVO);
                return itemVO;
            }).collect(Collectors.toList());
            ProductOrderVO productOrderVO = new ProductOrderVO();
            BeanUtils.copyProperties(orderDO,productOrderVO);
            productOrderVO.setOrderItemVOList(orderItemVOS);
            return productOrderVO;
        }).collect(Collectors.toList());
        Map<String, Object> pageMap = new HashMap<>(3);
        pageMap.put("total_record",orderDOIPage.getTotal());
        pageMap.put("total_page",orderDOIPage.getPages());
        pageMap.put("current_data",productOrderVOList);
        return pageMap;
    }

    @Override
    @Transactional
    public JsonData repay(RepayOrderRequest repayOrderRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(new QueryWrapper<ProductOrderDO>().eq("out_trade_no", repayOrderRequest.getOutTradeNo())
                .eq("user_id", loginUser.getId()));
        log.info("订单状态：{}",productOrderDO);
        if(productOrderDO == null){
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_NOT_EXIST);//订单不存在
        }
        if(!productOrderDO.getState().equalsIgnoreCase(ProductOrderStateEnum.NEW.name())){
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_STATE_ERROR);//订单状态不正常
        }else{
            //订单状态是new，创建二次支付
            //订单创建到现在的存活时间
            long orderLiveTime = CommonUtil.getCurrentTimestamp() - productOrderDO.getCreateTime().getTime();
            //创建订单是临界点，所以再增加一分钟多几秒，假如29分，则也不能支付了，相当于多加了延迟时间
            orderLiveTime = orderLiveTime + 70 * 1000;//多加70秒
            //要注意用户是否一直在页面停留，等待超时后支付，所以一定要和第三方控制超时时间，并在这里判断，若超时直接不找第三方
            if(orderLiveTime > TimeConstant.ORDER_PAY_TIMEOUT_MILLS){//大于订单超时时间则失效
                return JsonData.buildResult(BizCodeEnum.PAY_ORDER_PAY_TIMEOUT);
            }else{
                //记得更新DB的支付端 支付类型等

                long timeout = TimeConstant.ORDER_PAY_TIMEOUT_MILLS - orderLiveTime;//总时间 - 存活的时间就是剩下的有效时间
                PayInfoVO payInfoVO = new PayInfoVO(productOrderDO.getOutTradeNo(),productOrderDO.getPayAmount(),repayOrderRequest.getPayType(),
                        repayOrderRequest.getClientType(),productOrderDO.getOutTradeNo(),"",timeout);
                log.info("payInfoVO={}",payInfoVO);
                String payResult = payFactory.pay(payInfoVO);
                if(StringUtils.isNotBlank(payResult)){
                    log.info("创建二次支付订单成功:payInfoVO={},payResult={}",payInfoVO,payResult);
                    return JsonData.buildSuccess(payResult);
                }else{
                    log.error("创建二次支付订单失败:payInfoVO={},payResult={}",payInfoVO,payResult);
                    return JsonData.buildResult(BizCodeEnum.PAY_ORDER_FAIL);
                }
            }
        }
    }
}
