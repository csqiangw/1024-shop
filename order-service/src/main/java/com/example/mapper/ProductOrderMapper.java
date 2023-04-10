package com.example.mapper;

import com.example.model.ProductOrderDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.data.repository.query.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author qiangw
 * @since 2023-04-02
 */
public interface ProductOrderMapper extends BaseMapper<ProductOrderDO> {

    void updateOrderPayState(@Param("outTradeNo") String outTradeNo, @Param("newState") String newState, @Param("oldState") String oldState);
}
