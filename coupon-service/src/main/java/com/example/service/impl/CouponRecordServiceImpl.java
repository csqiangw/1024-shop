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
import com.example.interceptor.LoginInterceptor;
import com.example.mapper.CouponRecordMapper;
import com.example.mapper.CouponTaskMapper;
import com.example.model.*;
import com.example.request.LockCouponRecordRequest;
import com.example.service.CouponRecordService;
import com.example.util.JsonData;
import com.example.vo.CouponRecordVO;
import com.example.vo.CouponVO;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wq
 * @since 2023-03-30
 */
@Service
@Slf4j
public class CouponRecordServiceImpl implements CouponRecordService {

    @Autowired
    private CouponRecordMapper couponRecordMapper;

    @Autowired
    private CouponTaskMapper couponTaskMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Autowired
    private ProductOrderFeignService productOrderFeignService;

    @Override
    public Map<String, Object> page(int page, int size) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();

        //第1页，每页2条
        Page<CouponRecordDO> pageInfo = new Page<>(page, size);
        IPage<CouponRecordDO> recordDOPage = couponRecordMapper.selectPage(pageInfo, new QueryWrapper<CouponRecordDO>().eq("user_id",loginUser.getId()).orderByDesc("create_time"));
        Map<String, Object> pageMap = new HashMap<>(3);

        pageMap.put("total_record", recordDOPage.getTotal());
        pageMap.put("total_page", recordDOPage.getPages());
        pageMap.put("current_data", recordDOPage.getRecords().stream().map(obj -> beanProcess(obj)).collect(Collectors.toList()));

        return pageMap;
    }

    @Override
    public CouponRecordVO findById(long recordId) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        CouponRecordDO recordDO = couponRecordMapper.selectOne(new QueryWrapper<CouponRecordDO>().eq("id", recordId).eq("user_id", loginUser.getId()));
        if(recordDO == null){return null;}

        CouponRecordVO couponRecordVO = beanProcess(recordDO);
        return couponRecordVO;
    }

    /**
     * 1.锁定优惠券记录
     * 2.task表插入记录
     * 3.发送延迟消息
     * @param recordRequest
     * @return
     */
    @Override
    public JsonData lockCouponRecords(LockCouponRecordRequest recordRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        String orderOutTradeNo = recordRequest.getOrderOutTradeNo();
        List<Long> lockCouponRecordIds = recordRequest.getLockCouponRecordIds();

        //数据库每条语句具有原子性，所以不用担心多端锁定
        //这面就判断更新行数是否等于给的id记录数即可
        int updateRows = couponRecordMapper.lockUseStateBatch(loginUser.getId(), CouponStateEnum.USED.name(),lockCouponRecordIds);

        //批量插入记录
        List<CouponTaskDO> couponTaskDOList = lockCouponRecordIds.stream().map(obj -> {
            CouponTaskDO couponTaskDO = new CouponTaskDO();
            couponTaskDO.setCreateTime(new Date());
            couponTaskDO.setCouponRecordId(obj);
            couponTaskDO.setOutTradeNo(orderOutTradeNo);
            couponTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
            return couponTaskDO;
        }).collect(Collectors.toList());
        int insertRows = couponTaskMapper.insertBatch(couponTaskDOList);
        log.info("优惠券记录锁定updateRows={}",updateRows);
        log.info("新增优惠券记录insertRows={}",insertRows);

        if(lockCouponRecordIds.size() == insertRows && insertRows == updateRows){
            //发送延迟消息 TODO
            //这个消息要包括订单id，因为之后要去订单那面确认订单的状态，也需要taskId，因为，之后要修改优惠券锁定状态
            //多个优惠券，for循环遍历发
            for (CouponTaskDO couponTaskDO : couponTaskDOList) {
                CouponRecordMessage couponRecordMessage = new CouponRecordMessage();
                couponRecordMessage.setOutTradeNo(orderOutTradeNo);
                couponRecordMessage.setTaskId(couponTaskDO.getId());
                rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(),rabbitMQConfig.getCouponReleaseRoutingKey(),couponRecordMessage);
                log.info("优惠券锁定消息发送成功:{}",couponRecordMessage.toString());
            }
            return JsonData.buildSuccess();
        }else{
            throw new BizException(BizCodeEnum.COUPON_RECORD_LOCK_FAIL);
        }
    }

    /**
     * 解锁优惠券记录
     * (1)查询task工作单是否存在
     * (2)查询订单状态
     * (3)
     * @param recordMessage
     * @return
     */
    //这里需要事物回滚
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    @Override
    public boolean releaseCouponRecord(CouponRecordMessage recordMessage) {
        //查询task工作单是否存在
        CouponTaskDO couponTaskDO = couponTaskMapper.selectOne(new QueryWrapper<CouponTaskDO>().eq("id", recordMessage.getTaskId()));
        if(couponTaskDO == null){
            log.warn("工作单不存在，消息:{}",recordMessage);
            return true;//标记消息已经消费了
        }
        //lock状态才处理
        if(couponTaskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())){
            //查询订单状态
            JsonData jsonData = productOrderFeignService.queryProductOrderState(recordMessage.getOutTradeNo());
            if(jsonData.getCode() == 0){//正常响应再判断
                String state = jsonData.getData().toString();
                if(ProductOrderStateEnum.NEW.name().equalsIgnoreCase(state)){
                    //状态是NEW新建状态，则返回给消息队列，重新投递 一般不存在这个逻辑
                    log.warn("订单状态是NEW，返回给消息队列，重新投递:{}",recordMessage);
                    return false;
                }
                //如果已经支付
                if(ProductOrderStateEnum.PAY.name().equalsIgnoreCase(state)){
                    //修改Task状态为FINISH
                    couponTaskDO.setLockState(StockTaskStateEnum.FINISH.name());
                    couponTaskMapper.update(couponTaskDO,new QueryWrapper<CouponTaskDO>().eq("id",recordMessage.getTaskId()));
                    log.info("订单已经支付，修改库存锁定工作单为FINISH状态:{}",recordMessage);
                    return true;
                }
            }
            //订单不存在，或者订单被取消，确认消息即可
            log.warn("订单不存在，或者订单被取消，确认消息，修改task状态为CANCEL，恢复优惠券使用记录为NEW，message：{}",recordMessage);
            couponTaskDO.setLockState(StockTaskStateEnum.CANCEL.name());
            couponTaskMapper.update(couponTaskDO,new QueryWrapper<CouponTaskDO>().eq("id",recordMessage.getTaskId()));
            couponRecordMapper.updateState(couponTaskDO.getCouponRecordId(),CouponStateEnum.NEW.name());
            return true;
        }else{
            log.warn("工作单状态不是LOCK,state={},消息体={}",couponTaskDO.getLockState(),recordMessage);
            return true;
        }
    }

    private CouponRecordVO beanProcess(CouponRecordDO couponRecordDO) {
        CouponRecordVO couponRecordVO = new CouponRecordVO();
        BeanUtils.copyProperties(couponRecordDO,couponRecordVO);
        return couponRecordVO;
    }

}
