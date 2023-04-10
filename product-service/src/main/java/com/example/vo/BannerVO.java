package com.example.vo;

import lombok.Data;

/**
 * <p>
 * 
 * </p>
 *
 * @author qiangw
 * @since 2023-04-01
 */
@Data
public class BannerVO {

    private Integer id;

    /**
     * 图片
     */
    private String img;

    /**
     * 跳转地址
     */
    private String url;

    /**
     * 权重
     */
    private Integer weight;


}
