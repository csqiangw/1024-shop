package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mapper.BannerMapper;
import com.example.model.BannerDO;
import com.example.service.BannerService;
import com.example.vo.BannerVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author qiangw
 * @since 2023-04-01
 */
@Service
@Slf4j
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Override
    public List<BannerVO> list() {
        List<BannerDO> list = bannerMapper.selectList(new QueryWrapper<BannerDO>()
                .orderByAsc("weight"));

        List<BannerVO> bannerVOList  = list.stream().map(obj -> {
                    BannerVO vo = new BannerVO();
                    BeanUtils.copyProperties(obj, vo);
                    return vo;
                }
        ).collect(Collectors.toList());

        return bannerVOList;
    }
}
