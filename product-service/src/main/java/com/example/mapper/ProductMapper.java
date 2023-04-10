package com.example.mapper;

import com.example.model.ProductDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.data.repository.query.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author qiangw
 * @since 2023-04-01
 */
public interface ProductMapper extends BaseMapper<ProductDO> {

    /**
     * 锁定商品库存
     * @param productId
     * @param buyNum
     */
    int lockProductStock(@Param("productId") long productId, @Param("buyNum") int buyNum);

    /**
     * 解锁商品库存
     * @param productId
     * @param buyNum
     * @return
     */
    int unlockProductStock(@Param("productId") Long productId, @Param("buyNum") Integer buyNum);
}
