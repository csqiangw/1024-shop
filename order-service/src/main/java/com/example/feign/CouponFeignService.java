package com.example.feign;

import com.example.request.LockCouponRecordRequest;
import com.example.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("coupon-service")
public interface CouponFeignService {

    @GetMapping("/api/coupon_record/v1/detail/{record_id}")
    public JsonData findUserCouponRecordById(@PathVariable("record_id")long recordId);

    /**
     * 锁定优惠券
     * @param recordRequest
     * @return
     */
    @PostMapping("/api/coupon_record/v1/lock_records")
    public JsonData lockCouponRecords(@RequestBody LockCouponRecordRequest recordRequest);

}
