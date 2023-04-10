package com.example.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "锁定优惠券请求对象",description = "锁定优惠券请求对象")
public class LockCouponRecordRequest {

    /**
     * 优惠券记录id列表
     */
    @ApiModelProperty(value = "优惠券记录id列表",example = "[1,2,3]")
    private List<Long> lockCouponRecordIds;

    /**
     * 订单号
     */
    @ApiModelProperty(value = "订单号",example = "21dasdsae12e")
    private String orderOutTradeNo;

}
