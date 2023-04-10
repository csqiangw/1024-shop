package com.example.controller;

import com.example.request.CartItemRequest;
import com.example.service.CartService;
import com.example.util.JsonData;
import com.example.vo.CartItemVO;
import com.example.vo.CartVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api("购物车")
@RestController
@RequestMapping("/api/cart/v1")
public class CartController {

    @Autowired
    private CartService cartService;

    @ApiOperation("添加到购物车")
    @PostMapping("/add")
    public JsonData addToCart(@ApiParam("购物项") @RequestBody CartItemRequest cartItemRequest){
        cartService.addToCart(cartItemRequest);
        return JsonData.buildSuccess();
    }

    @ApiOperation("清空购物车")
    @DeleteMapping("/clear")
    public JsonData clearMyCart(){
        cartService.clear();
        return JsonData.buildSuccess();
    }

    @ApiOperation("查看我的购物车")
    @GetMapping("/mycart")
    public JsonData findMyCart(){
        CartVO cartVO = cartService.getMyCart();
        return JsonData.buildSuccess(cartVO);
    }

    @ApiOperation("删除购物项")
    @DeleteMapping("/delete/{product_id}")
    public JsonData deleteItem(@ApiParam(value = "商品id",required = true) @PathVariable("product_id") long productId){
        cartService.deleteItem(productId);
        return JsonData.buildSuccess();
    }

    @ApiOperation("修改购物项数量")
    @PostMapping("/change")
    public JsonData changeItem(@ApiParam("购物项") @Valid @RequestBody CartItemRequest cartItemRequest){
        cartService.changeItemNum(cartItemRequest);
        return JsonData.buildSuccess();
    }

    /**
     * 用于订单服务，确认订单，获取对应的商品详情信息
     *
     * 会清空购物车的商品数据
     * @param productIdList
     * @return
     */
    @ApiOperation("获取对应订单的商品信息")
    @PostMapping("/api/cart/v1/confirm_order_cart_items")
    JsonData confirmOrderCartItems(@ApiParam("商品id列表") @RequestBody List<Long> productIdList){
        List<CartItemVO> cartItemVOList = cartService.confirmOrderCartItems(productIdList);
        return JsonData.buildSuccess(cartItemVOList);
    }

}
