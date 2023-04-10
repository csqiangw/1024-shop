package com.example.controller;


import com.example.service.BannerService;
import com.example.util.JsonData;
import com.example.vo.BannerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author qiangw
 * @since 2023-04-01
 */
@Api("轮播图")
@RestController
@RequestMapping("/api/banner/v1")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @ApiOperation("轮播图列表接口")
    @GetMapping("/list")
    public JsonData list(){
        List<BannerVO> bannerList = bannerService.list();
        return JsonData.buildSuccess(bannerList);
    }

}

