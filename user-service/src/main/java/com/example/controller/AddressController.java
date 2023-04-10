package com.example.controller;


import com.example.enums.BizCodeEnum;
import com.example.exception.BizException;
import com.example.model.AddressDO;
import com.example.request.AddressAddRequest;
import com.example.service.AddressService;
import com.example.util.JsonData;
import com.example.vo.AddressVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 电商-公司收发货地址表 前端控制器
 * </p>
 *
 * @author qiangw
 * @since 2023-03-19
 */
@Api(tags = "收获地址模块")
@RestController
//面向客户端一般为api，然后要写版本号，若是给管理控制台，可以写api-admin
@RequestMapping("/api/address/v1")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @ApiOperation("新增收获地址")
    @PostMapping("add")
    public JsonData add(@ApiParam("地址对象") @RequestBody AddressAddRequest addressAddRequest){
        addressService.add(addressAddRequest);
        return JsonData.buildSuccess();
    }

    @ApiOperation("根据id查找地址详情")
    @GetMapping("/detail/{address id}")
    public JsonData detail(@ApiParam(value = "地址id",required = true)
                             @PathVariable("address id") long addressId){
        AddressVO addressVO = addressService.detail(addressId);
        return addressVO == null ? JsonData.buildResult(BizCodeEnum.ADDRESS_NO_EXITS) : JsonData.buildSuccess(addressVO);
    }

    /**
     * 删除指定收获地址
     * @param addressId
     * @return
     */
    @ApiOperation("根据id删除地址")
    @DeleteMapping("/del/{address id}")
    public JsonData del(@ApiParam(value = "地址id",required = true)
                           @PathVariable("address id") long addressId){
        int rows = addressService.del(addressId);
        return rows == 1 ? JsonData.buildSuccess() : JsonData.buildResult(BizCodeEnum.ADDRESS_DEL_FAIL);
    }

    /**
     * 列举全部收获地址
     * @return
     */
    @ApiOperation("列举全部收货地址")
    @GetMapping("/list")
    public JsonData findUserAllAddress(){
        List<AddressVO> list = addressService.listUserAllAddress();
        return JsonData.buildSuccess(list);
    }

}

