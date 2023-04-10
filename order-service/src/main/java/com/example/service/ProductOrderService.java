package com.example.service;

import com.example.enums.ProductOrderPayTypeEnum;
import com.example.model.OrderMessage;
import com.example.request.ConfirmOrderRequest;
import com.example.request.RepayOrderRequest;
import com.example.util.JsonData;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author qiangw
 * @since 2023-04-02
 */
public interface ProductOrderService {

    JsonData confirmOrder(ConfirmOrderRequest orderRequest);

    /**
     * 查询订单状态
     * @param outTradeNo
     * @return
     */
    String queryProductOrderState(String outTradeNo);

    /**
     * 队列监听，定时关单
     * @param orderMessage
     * @return
     */
    boolean closeProductOrder(OrderMessage orderMessage);

    /**
     * 处理支付结果回调通知
     * @param alipay
     * @param paramsMap
     * @return
     */
    JsonData handlerOrderCallbackMsg(ProductOrderPayTypeEnum alipay, Map<String, String> paramsMap);

    /**
     * 根据订单状态分页查询我的订单列表
     * @param page
     * @param size
     * @param state
     * @return
     */
    Map<String, Object> page(int page, int size, String state);

    /**
     * 订单二次支付
     * @param repayOrderRequest
     * @return
     */
    JsonData repay(RepayOrderRequest repayOrderRequest);
}
