package com.example.mapper;

import com.example.model.CouponTaskDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wq
 * @since 2023-04-04
 */
public interface CouponTaskMapper extends BaseMapper<CouponTaskDO> {

    int insertBatch(@Param("couponTaskDOList") List<CouponTaskDO> couponTaskDOList);
}
