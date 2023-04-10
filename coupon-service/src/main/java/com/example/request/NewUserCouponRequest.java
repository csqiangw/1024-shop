package com.example.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class NewUserCouponRequest {

    @ApiModelProperty(value = "用户id",example = "19")
    @JsonProperty("user_id")
    private long userId;

    @ApiModelProperty(value = "用户名",example = "wq")
    private String name;

}
