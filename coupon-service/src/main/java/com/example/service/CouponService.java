package com.example.service;

import com.example.enums.CouponCategoryEnum;
import com.example.model.CouponDO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.request.NewUserCouponRequest;
import com.example.util.JsonData;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wq
 * @since 2023-03-30
 */
public interface CouponService {

    /**
     * 分页查询优惠券
     * @return
     */
    Map<String,Object> pageCouponActivity(int page,int size);

    /**
     * 领取优惠券接口
     * @param couponId
     * @param promotion
     * @return
     */
    JsonData addCoupon(long couponId, CouponCategoryEnum promotion);

    /**
     * 新用户注册发放优惠券
     * @param newUserCouponRequest
     * @return
     */
    JsonData initNewUserCoupon(NewUserCouponRequest newUserCouponRequest);
}
