package com.example.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "用户注册对象",description = "用户注册请求对象")
@Data
public class UserRegisterRequest {

    //写example在swagger测试时也更加方便

    @ApiModelProperty(value = "昵称",example = "wq")
    private String name;

    @ApiModelProperty(value = "密码",example = "1234")
    private String pwd;

    @ApiModelProperty(value = "头像",example = "https://1024shop-user-service-imgs.oss-cn-shanghai.aliyuncs.com/user/2023/03/29/9be6218089784f4a87f406fab98037c7.png")
    //提醒，前端提交过来的字段是head_img，用JsonProperty做映射
    @JsonProperty("head_img")
    private String headImg;

    @ApiModelProperty(value = "描述",example = "人生需要动态规划，学习需要贪心算法")
    private String slogan;

    @ApiModelProperty(value = "0表示女，1表示男",example = "1")
    private String sex;

    @ApiModelProperty(value = "邮箱",example = "321516900@qq.com")
    private String mail;

    @ApiModelProperty(value = "验证码",example = "232131")
    private String code;

}
