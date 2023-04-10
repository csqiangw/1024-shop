package com.example.mapper;

import com.example.model.CouponDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.data.repository.query.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wq
 * @since 2023-03-30
 */
public interface CouponMapper extends BaseMapper<CouponDO> {

    /**
     * 扣减库存
     * @param couponId
     */
    int reduceStock(@Param("couponId") long couponId);

}
