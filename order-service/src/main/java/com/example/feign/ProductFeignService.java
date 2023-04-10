package com.example.feign;

import com.example.request.LockProductRequest;
import com.example.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("product-service")
public interface ProductFeignService {

    /**
     * 获取购物车的最新商品价格，也会情况对应购物车的商品
     * @param productIdList
     * @return
     */
    @PostMapping("/api/cart/v1/confirm_order_cart_items")
    JsonData confirmOrderCartItems(@RequestBody List<Long> productIdList);

    @PostMapping("/api/product/v1/lock_products")
    JsonData lockProducts(@RequestBody LockProductRequest lockProductRequest);

}
