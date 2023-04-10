package com.example.component;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.example.config.AliPayConfig;
import com.example.config.PayUrlConfig;
import com.example.enums.BizCodeEnum;
import com.example.enums.ClientType;
import com.example.exception.BizException;
import com.example.vo.PayInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
public class AlipayStrategy implements PayStrategy{

    @Autowired
    private PayUrlConfig payUrlConfig;

    @Override
    public String unifiedOrder(PayInfoVO payInfoVO) {
        HashMap<String,String> content = new HashMap<>();
        //商户订单号,64个字符以内、可包含字母、数字、下划线；需保证在商户端不重复
        String no = UUID.randomUUID().toString();
        log.info("订单号:{}",no);
        content.put("out_trade_no", payInfoVO.getOutTradeNo());
        content.put("product_code", "FAST_INSTANT_TRADE_PAY");
        //订单总金额，单位为元，精确到小数点后两位
        content.put("total_amount", payInfoVO.getPayFee().toString());
        //商品标题/交易标题/订单标题/订单关键字等。 注意：不可使用特殊字符，如 /，=，&amp; 等。
        content.put("subject", payInfoVO.getTitle());
        //商品描述，可空
        content.put("body", payInfoVO.getDescription());

        double timeout = Math.floor(payInfoVO.getOrderPayTimeoutMills() / (1000 * 60));
        //前端也需要判断订单是否要关闭了，如果快要到期，则不给二次支付
        if(timeout < 1){//设置小于1的原因，就是快要超时了，就提醒它重新下单
            throw new BizException(BizCodeEnum.PAY_ORDER_PAY_TIMEOUT);
        }
        // 该笔订单允许的最晚付款时间，逾期将关闭交易。取值范围：1m～15d。m-分钟，h-小时，d-天，1c-当天（1c-当天的情况下，无论交易何时创建，都在0点关闭）。 该参数数值不接受小数点， 如 1.5h，可转换为 90m。
        content.put("timeout_express", Double.valueOf(timeout).intValue() + "m");

        String clientType = payInfoVO.getClientType();
        String form = "";

        try{
            if(clientType.equalsIgnoreCase(ClientType.H5.name())){//h5 手机网页支付
                AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
                request.setBizContent(JSON.toJSONString(content));
                request.setNotifyUrl(payUrlConfig.getAlipayCallbackUrl());
                request.setReturnUrl(payUrlConfig.getAlipaySuccessReturnUrl());
                AlipayTradeWapPayResponse wapPayResponse = AliPayConfig.getInstance().pageExecute(request);
                if(wapPayResponse.isSuccess()){
                    form = wapPayResponse.getBody();
                }else{
                    log.info("支付宝构建H5表达失败:alipayResponse={},payInfo={}",wapPayResponse,payInfoVO);
                }
            }else if(clientType.equalsIgnoreCase(ClientType.PC.name())){//PC 电脑网页支付

            }//..APP支付
        }catch (AlipayApiException e){

        }
        return form;
    }

    /**
     * 订单状态查询
     * 未支付 code 400004
     * 已经支付 code 100000
     * @param payInfoVO
     * @return
     */
    @Override
    public String queryPaySuccess(PayInfoVO payInfoVO) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        HashMap<String,String> content = new HashMap<>();
        //订单商户号 64位
        content.put("out_trade_no",payInfoVO.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(content));
        AlipayTradeQueryResponse response = null;
        try {
            response = AliPayConfig.getInstance().execute(request);
            log.info("订单查询响应:{}",response.getBody());
        }catch (AlipayApiException e){
            e.printStackTrace();
            log.error("支付宝订单查询异常:{}",e);
        }
        if(response.isSuccess()){
            log.info("支付宝订单状态查询成功:{}",payInfoVO);
            return response.getTradeStatus();
        }else{
            log.info("支付宝订单状态查询失败:{}",payInfoVO);
            return "";
        }
    }
}
