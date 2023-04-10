package com.example.controller;


import com.example.enums.BizCodeEnum;
import com.example.request.LockCouponRecordRequest;
import com.example.service.CouponRecordService;
import com.example.service.CouponService;
import com.example.util.JsonData;
import com.example.vo.CouponRecordVO;
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
 * @author wq
 * @since 2023-03-30
 */
@RestController
@RequestMapping("/api/coupon_record/v1")
public class CouponRecordController {

    @Autowired
    private CouponRecordService couponRecordService;

    @ApiOperation("分页查询个人优惠券")
    @GetMapping("/page")
    public JsonData page(@ApiParam("当前页") @RequestParam(value = "page",defaultValue = "1") int page,
                         @ApiParam("每页显示多少条") @RequestParam(value = "size",defaultValue = "20") int size){
        Map<String, Object> pageMap = couponRecordService.page(page, size);
        return JsonData.buildSuccess(pageMap);
    }

    /**
     * 查询优惠券记录信息
     * 水平权限攻击：也叫作访问控制攻击,Web应用程序接收到用户请求，修改某条数据时，没有判断数据的所属人，
     * 或者在判断数据所属人时从用户提交的表单参数中获取了userid。
     * 导致攻击者可以自行修改userid修改不属于自己的数据
     * @param recordId
     * @return
     */
    @ApiOperation("查询优惠券记录信息")
    @GetMapping("/detail/{record_id}")
    public JsonData findUserCouponRecordById(@PathVariable("record_id")long recordId ){

        CouponRecordVO couponRecordVO = couponRecordService.findById(recordId);
        return  couponRecordVO == null? JsonData.buildResult(BizCodeEnum.COUPON_NO_EXITS):JsonData.buildSuccess(couponRecordVO);
    }

    @ApiOperation("rpc-锁定，优惠券记录")
    @PostMapping("/lock_records")
    public JsonData lockCouponRecords(@ApiParam("锁定优惠券请求对象") @RequestBody LockCouponRecordRequest recordRequest){
        JsonData jsonData = couponRecordService.lockCouponRecords(recordRequest);
        return jsonData;
    }

}

