package com.example.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "用户登录对象",description = "用户登录请求对象")
@Data
public class UserLoginRequest {

    @ApiModelProperty(value = "邮箱",example = "321516900@qq.com")
    private String mail;

    @ApiModelProperty(value = "密码",example = "1234")
    private String pwd;

    //未来还可能添加重试次数

}
