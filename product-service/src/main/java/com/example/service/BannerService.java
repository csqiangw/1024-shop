package com.example.service;

import com.example.vo.BannerVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author qiangw
 * @since 2023-04-01
 */
public interface BannerService {

    /**
     * 返回轮播图列表
     * @return
     */
    List<BannerVO> list();
}
