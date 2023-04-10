package com.example.component;

import com.example.enums.ProductOrderPayTypeEnum;
import com.example.vo.PayInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PayFactory {

    @Autowired
    private AlipayStrategy alipayPayStrategy;

    /**
     * 创建支付
     * @param payInfoVO
     * @return
     */
    public String pay(PayInfoVO payInfoVO){
        String payType = payInfoVO.getPayType();
        if(ProductOrderPayTypeEnum.ALIPAY.name().equalsIgnoreCase(payType)){
            PayStrategyContext payStrategyContext = new PayStrategyContext(alipayPayStrategy);
            return payStrategyContext.executeUnifiedorder(payInfoVO);
        }
        return null;
    }

    /**
     * 查询订单支付状态
     * @param payInfoVO
     * @return
     */
    public String queryPaySuccess(PayInfoVO payInfoVO){
        String payType = payInfoVO.getPayType();
        if(ProductOrderPayTypeEnum.ALIPAY.name().equalsIgnoreCase(payType)){
            PayStrategyContext payStrategyContext = new PayStrategyContext(alipayPayStrategy);
            return payStrategyContext.executeQueryPaySuccess(payInfoVO);
        }
        return null;
    }

}
