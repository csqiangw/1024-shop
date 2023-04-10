package com.example.mapper;

import com.example.model.ProductOrderItemDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author qiangw
 * @since 2023-04-02
 */
public interface ProductOrderItemMapper extends BaseMapper<ProductOrderItemDO> {

    /**
     * 批量插入
     * @param orderItemList
     */
    int insertBatch(@Param("orderItemList") List<ProductOrderItemDO> orderItemList);
}
