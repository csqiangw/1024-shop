package com.example.controller;


import com.example.enums.BizCodeEnum;
import com.example.request.UserLoginRequest;
import com.example.request.UserRegisterRequest;
import com.example.service.FileService;
import com.example.service.UserService;
import com.example.util.JsonData;
import com.example.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author qiangw
 * @since 2023-03-19
 */
@Api(tags = "用户模块")
@RestController
@RequestMapping("/api/user/v1/")
@Slf4j
public class UserController {

    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    /**
     * 头像上传
     * 默认最大是1M，超过会报错
     * @param file
     * @return
     */
    @ApiOperation("用户头像上传")
    @PostMapping(value = "upload")
    public JsonData uploadUserImg(@ApiParam(value = "文件上传",required = true)
                                      @RequestPart("file") MultipartFile file){
        String result = fileService.uploadUserImg(file);
        return result != null ? JsonData.buildSuccess(result) : JsonData.buildResult(BizCodeEnum.FILE_UPLOAD_USER_IMG_FAIL);
    }

    //前端声明的类，需要用一个类取接收
    /**
     * 用户注册
     * 用对象包装的好处，以后若对象字段发生变化，controller这里不需要进行改动
     * @param registerRequest
     * @return
     */
    @ApiOperation("用户注册")
    @PostMapping("register")
    public JsonData register(@ApiParam("用户注册对象") @RequestBody UserRegisterRequest registerRequest){
        JsonData jsonData = userService.register(registerRequest);
        return jsonData;
    }

    /**
     * 用户登录
     * @return
     */
    @ApiOperation("用户登录")
    @PostMapping("login")
    public JsonData login(@ApiParam("用户登录对象") @RequestBody UserLoginRequest loginRequest){
        JsonData jsonData = userService.login(loginRequest);
        return jsonData;
    }

    /**
     * 用户个人信息查询
     * @return
     */
    @ApiOperation("个人信息查询")
    @PostMapping("detail")
    public JsonData detail(){
        //这里不能直接暴露数据库的DO类，要建立一个VO，按需给
        UserVO userVO = userService.findUserDetail();
        return JsonData.buildSuccess(userVO);
    }

}

