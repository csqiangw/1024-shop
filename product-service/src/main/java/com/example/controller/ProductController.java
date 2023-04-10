package com.example.controller;


import com.example.request.LockProductRequest;
import com.example.service.ProductService;
import com.example.util.JsonData;
import com.example.vo.ProductVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author qiangw
 * @since 2023-04-01
 */
@Api("商品模块")
@RestController
@RequestMapping("/api/product/v1")
public class ProductController {

    @Autowired
    private ProductService productService;

    @ApiOperation("分页查询商品列表")
    @GetMapping("/page")
    public JsonData pageProductList(@ApiParam(value = "当前页") @RequestParam(value = "page",defaultValue = "1") int page,
                                    @ApiParam(value = "每页显示多少条") @RequestParam(value = "size",defaultValue = "10") int size){
        Map<String,Object> pageMap = productService.page(page,size);
        return JsonData.buildSuccess(pageMap);
    }

    @ApiOperation("商品详情")
    @GetMapping("/detail/{product_id}")
    public JsonData detail(@ApiParam(value = "商品id",required = true) @PathVariable("product_id") long productId){
        ProductVO productVO = productService.findDetailById(productId);
        return JsonData.buildSuccess(productVO);
    }

    /**
     * 商品库存锁定
     * @return
     */
    @ApiOperation("商品库存锁定")
    @PostMapping("/lock_products")
    public JsonData lockProducts(@ApiParam("多顶订单请求对象") @RequestBody LockProductRequest lockProductRequest){
        JsonData jsonData = productService.lockProductStock(lockProductRequest);
        return JsonData.buildSuccess(jsonData);
    }

}

