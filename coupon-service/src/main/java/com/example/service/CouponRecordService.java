package com.example.service;

import com.example.model.CouponRecordMessage;
import com.example.request.LockCouponRecordRequest;
import com.example.util.JsonData;
import com.example.vo.CouponRecordVO;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wq
 * @since 2023-03-30
 */
public interface CouponRecordService {

    /**
     * 分页查询领券记录
     * @param page
     * @param size
     * @return
     */
    Map<String,Object> page(int page,int size);

    /**
     * 根据id查找优惠券详情
     * @param recordId
     * @return
     */
    CouponRecordVO findById(long recordId);

    /**
     * 锁定优惠券
     * @param recordRequest
     * @return
     */
    JsonData lockCouponRecords(LockCouponRecordRequest recordRequest);

    /**
     * 释放优惠券
     * @param recordMessage
     * @return
     */
    boolean releaseCouponRecord(CouponRecordMessage recordMessage);
}
