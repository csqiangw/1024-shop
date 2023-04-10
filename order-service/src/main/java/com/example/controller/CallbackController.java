package com.example.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.example.config.AliPayConfig;
import com.example.enums.ProductOrderPayTypeEnum;
import com.example.service.ProductOrderService;
import com.example.util.JsonData;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Api("订单回调通知模块")
//和第三方，返回字符串就行了，不用转位json
@Controller("/api/callback/order/v1")
@Slf4j
public class CallbackController {

    @Autowired
    private ProductOrderService productOrderService;

    @PostMapping("/alipay")
    public String alipayCallback(HttpServletRequest request, HttpServletResponse response){
        //将异步通知中
        Map<String, String> paramsMap = convertRequestParamsToMap(request);
        log.info("支付宝回调结果:{}",paramsMap);
        try {
            boolean signVerified = AlipaySignature.rsaCertCheckV1(paramsMap, AliPayConfig.ALIPAY_PUBLIC_KEY, AliPayConfig.CHARSET, AliPayConfig.SIGN_TYPE);
            if(signVerified){
                JsonData jsonData = productOrderService.handlerOrderCallbackMsg(ProductOrderPayTypeEnum.ALIPAY,paramsMap);
                if(jsonData.getCode() == 0){
                    //通知结果确认成功，不然会一直通知，八次都没返回success就认为交易失败
                    return "success";
                }
            }
        } catch (AlipayApiException e) {
            log.info("支付宝回调验证签名失败:异常:{},参数:{}",e,paramsMap);
        }
        return "failure";
    }

    /**
     * 将request中的参数转换成Map
     * @param request
     * @return
     */
    private static Map<String, String> convertRequestParamsToMap(HttpServletRequest request) {
        Map<String, String> paramsMap = new HashMap<>(16);
        Set<Map.Entry<String, String[]>> entrySet = request.getParameterMap().entrySet();

        for (Map.Entry<String, String[]> entry : entrySet) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            int size = values.length;
            if (size == 1) {
                paramsMap.put(name, values[0]);
            } else {
                paramsMap.put(name, "");
            }
        }
        return paramsMap;
    }

}
