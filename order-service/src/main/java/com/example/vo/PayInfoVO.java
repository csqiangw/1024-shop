package com.example.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayInfoVO {

    /**
     * 订单号
     */
    private String outTradeNo;

    /**
     * 订单总金额
     */
    private BigDecimal payFee;

    /**
     * 支付类型
     */
    private String payType;

    /**
     * 端类型
     */
    private String clientType;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 订单超时时间,毫秒
     */
    private long orderPayTimeoutMills;

}
