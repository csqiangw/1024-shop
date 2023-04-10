package com.example.service;

import com.example.model.ProductMessage;
import com.example.request.LockProductRequest;
import com.example.util.JsonData;
import com.example.vo.ProductVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author qiangw
 * @since 2023-04-01
 */
public interface ProductService {

    /**
     * 商品页分页显示
     * @param page
     * @param size
     * @return
     */
    Map<String, Object> page(int page, int size);

    /**
     * 根据商品id找商品详情
     * @param productId
     * @return
     */
    ProductVO findDetailById(long productId);

    /**
     * 根据id批量查询商品
     * @param productIdList
     * @return
     */
    List<ProductVO> findProductsByIdBatch(List<Long> productIdList);

    /**
     * 锁定商品库存
     * @param lockProductRequest
     * @return
     */
    JsonData lockProductStock(LockProductRequest lockProductRequest);

    /**
     * 释放商品库存
     * @param productMessage
     * @return
     */
    boolean releaseProductStock(ProductMessage productMessage);
}
