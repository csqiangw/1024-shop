package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.config.RabbitMQConfig;
import com.example.enums.BizCodeEnum;
import com.example.enums.CouponStateEnum;
import com.example.enums.ProductOrderStateEnum;
import com.example.enums.StockTaskStateEnum;
import com.example.exception.BizException;
import com.example.feign.ProductOrderFeignService;
import com.example.mapper.ProductMapper;
import com.example.mapper.ProductTaskMapper;
import com.example.model.ProductDO;
import com.example.model.ProductMessage;
import com.example.model.ProductTaskDO;
import com.example.request.LockProductRequest;
import com.example.request.OrderItemRequest;
import com.example.service.ProductService;
import com.example.util.JsonData;
import com.example.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author qiangw
 * @since 2023-04-01
 */
@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductTaskMapper productTaskMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Autowired
    private ProductOrderFeignService productOrderFeignService;

    @Override
    public Map<String, Object> page(int page, int size) {
        Page<ProductDO> pageInfo = new Page<>(page,size);
        IPage<ProductDO> productDOPage = productMapper.selectPage(pageInfo, null);
        Map<String,Object> pageMap = new HashMap<>(3);
        pageMap.put("total_record",productDOPage.getTotal());
        pageMap.put("total_page",productDOPage.getPages());
        pageMap.put("current_data",productDOPage.getRecords().stream().map(obj->beanProcess(obj)).collect(Collectors.toList()));
        return pageMap;
    }

    @Override
    public ProductVO findDetailById(long productId) {
        ProductDO productDO = productMapper.selectOne(new QueryWrapper<ProductDO>().eq("id", productId));
        return beanProcess(productDO);
    }

    /**
     * 批量查询
     * @param productIdList
     * @return
     */
    @Override
    public List<ProductVO> findProductsByIdBatch(List<Long> productIdList) {
        List<ProductDO> productDOList = productMapper.selectList(new QueryWrapper<ProductDO>().in("id", productIdList));
        List<ProductVO> productVOList = productDOList.stream().map(obj -> beanProcess(obj)).collect(Collectors.toList());
        return productVOList;
    }

    /**
     * 锁定商品库存
     * 1)遍历商品，锁定每个商品购买数量
     * 2)每一次锁定的时候，都要发送延迟消息
     * @param lockProductRequest
     * @return
     */
    @Override
    public JsonData lockProductStock(LockProductRequest lockProductRequest) {
        String orderOutTradeNo = lockProductRequest.getOrderOutTradeNo();
        List<OrderItemRequest> itemList = lockProductRequest.getOrderItemList();
        //一行代码提取对象里面的id并加入到集合里面
        List<Long> productIdList = itemList.stream().map(OrderItemRequest::getProductId).collect(Collectors.toList());
        //批量查询
        List<ProductVO> productVOList = this.findProductsByIdBatch(productIdList);
        //分组
        Map<Long, ProductVO> productMaps = productVOList.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));
        for (OrderItemRequest item : itemList){
            //锁定商品记录
            int rows = productMapper.lockProductStock(item.getProductId(), item.getBuyNum());
            if(rows != 1){
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);//库存锁定错误
            }else{
                //插入商品product_task
                ProductVO productVO = productMaps.get(item.getProductId());
                ProductTaskDO productTaskDO = new ProductTaskDO();
                productTaskDO.setBuyNum(item.getBuyNum());
                productTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
                productTaskDO.setProductId(item.getProductId());
                productTaskDO.setProductName(productVO.getTitle());
                productTaskDO.setOutTradeNo(orderOutTradeNo);
                productTaskMapper.insert(productTaskDO);

                //发送MQ延迟消息
                ProductMessage productMessage = new ProductMessage();
                productMessage.setOutTradeNo(orderOutTradeNo);
                productMessage.setTaskId(productTaskDO.getId());
                rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(),rabbitMQConfig.getStockReleaseDelayRoutingKey(),productMessage);
                log.info("商品库存锁定延迟信息发送成功:{}",productMessage);
            }
        }
        return JsonData.buildSuccess();
    }

    /**
     * 释放商品库存
     * @param productMessage
     * @return
     */
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    @Override
    public boolean releaseProductStock(ProductMessage productMessage) {
        //查询工作单状态
        ProductTaskDO productTaskDO = productTaskMapper.selectOne(new QueryWrapper<ProductTaskDO>().eq("id", productMessage.getTaskId()));
        if(productTaskDO != null){
            log.warn("工作单不存在，消息体为:{}",productMessage);
        }
        //lock状态才处理
        if(productTaskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())){
            //查询订单状态
            JsonData jsonData = productOrderFeignService.queryProductOrderState(productMessage.getOutTradeNo());
            if(jsonData.getCode() == 0){
                String state = jsonData.getData().toString();
                if(ProductOrderStateEnum.NEW.name().equalsIgnoreCase(state)){
                    log.warn("订单状态是NEW，返回给消息队列，重新投递:{}",productMessage);
                    return false;
                }
                //如果已经支付
                if(ProductOrderStateEnum.PAY.name().equalsIgnoreCase(state)){
                    //修改Task状态为FINISH
                    productTaskDO.setLockState(StockTaskStateEnum.FINISH.name());
                    productTaskMapper.update(productTaskDO,new QueryWrapper<ProductTaskDO>().eq("id",productMessage.getTaskId()));
                    log.info("订单已经支付，修改库存锁定工作单为FINISH状态:{}",productMessage);
                    return true;
                }
            }
            //订单不存在，或者订单被取消，确认消息即可
            log.warn("订单不存在，或者订单被取消，确认消息，修改task状态为CANCEL，恢复商品库存为NEW，message：{}",productMessage);
            productTaskDO.setLockState(StockTaskStateEnum.CANCEL.name());
            productTaskMapper.update(productTaskDO,new QueryWrapper<ProductTaskDO>().eq("id",productMessage.getTaskId()));
            //恢复商品库存，既锁定库存的值-当前购买的值
            productMapper.unlockProductStock(productTaskDO.getProductId(),productTaskDO.getBuyNum());
            return true;
        }else{
            log.warn("工作单状态不是LOCK,state={},消息体={}",productTaskDO.getLockState(),productMessage);
            return true;
        }
    }

    private ProductVO beanProcess(ProductDO productDO) {
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(productDO,productVO);
        //这里的库存应该是，总库存 - 锁定库存
        productVO.setStock(productDO.getStock() - productDO.getLockStock());
        return productVO;
    }

}
