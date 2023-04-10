package com.example.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class CartVO {

    /**
     * 购物项
     */
    @JsonProperty("cart_items")
    private List<CartItemVO> cartItems;

    /**
     * 购买总件数
     */
    @JsonProperty("total_num")
    private Integer totalNum;

    /**
     * 购物车总价格
     */
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    /**
     * 购物车实际支付价格
     */
    @JsonProperty("real_pay_amount")
    private BigDecimal realPayAmount;

    /**
     * 总件数
     * @return
     */
    public Integer getTotalNum() {
        if(cartItems != null){
            int total = cartItems.stream().mapToInt(CartItemVO::getBuyNum).sum();
            return total;
        }
        return 0;
    }

    /**
     * 总价格
     * 最后包装时，这些方法会被调用
     * @return
     */
    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        if(cartItems != null){
            for (CartItemVO cartItemVO : cartItems){
                BigDecimal itemVOTotalAmount = cartItemVO.getTotalAmount();
                amount = amount.add(itemVOTotalAmount);
            }
        }
        return amount;
    }

    /**
     * 购物车里面实际支付的价格
     * @return
     */
    public BigDecimal getRealPayAmount() {
        BigDecimal amount = new BigDecimal("0");
        if(cartItems != null){
            for (CartItemVO cartItemVO : cartItems){
                BigDecimal itemVOTotalAmount = cartItemVO.getTotalAmount();
                amount = amount.add(itemVOTotalAmount);
            }
        }
        return amount;
    }

    public List<CartItemVO> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItemVO> cartItems) {
        this.cartItems = cartItems;
    }

}
