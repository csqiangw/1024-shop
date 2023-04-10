package com.example.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("商品订单子项")
@Data
public class OrderItemRequest {

    @ApiModelProperty(value = "商品id",example = "12321321321")
    @JsonProperty("product_id")
    private long productId;

    @ApiModelProperty(value = "购买数量",example = "2")
    @JsonProperty("buy_num")
    private int buyNum;

}
