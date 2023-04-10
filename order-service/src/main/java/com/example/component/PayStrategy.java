package com.example.component;

import com.example.vo.PayInfoVO;

public interface PayStrategy {

    /**
     * 下单
     * @return
     */
    String unifiedOrder(PayInfoVO payInfoVO);

    /**
     * 退款
     */
    default String refund(PayInfoVO payInfoVO){
        return "";
    };

    /**
     * 查询支付是否成功
     * @param payInfoVO
     * @return
     */
    default String queryPaySuccess(PayInfoVO payInfoVO){
        return "";
    }

}
