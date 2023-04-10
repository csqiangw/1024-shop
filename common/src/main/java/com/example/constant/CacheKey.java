package com.example.constant;

public class CacheKey {

    /**
     * 注册验证码，第一个是类型（注册等），第二个是接收人
     */
    public static final String CHECK_CODE_KEY = "code:%s:%s";

    /**
     * 购物车hash结果，key是用户唯一标识
     */
    public static final String CART_KEY = "cart:%s";

    /**
     * 提交表单的token key
     */
    public static final String SUBMIT_ORDER_TOKEN_KEY = "order:submit:%s";

}
