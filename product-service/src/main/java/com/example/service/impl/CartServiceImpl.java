package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.constant.CacheKey;
import com.example.enums.BizCodeEnum;
import com.example.exception.BizException;
import com.example.interceptor.LoginInterceptor;
import com.example.model.LoginUser;
import com.example.request.CartItemRequest;
import com.example.service.CartService;
import com.example.service.ProductService;
import com.example.vo.CartItemVO;
import com.example.vo.CartVO;
import com.example.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(CartItemRequest cartItemRequest) {
        long productId = cartItemRequest.getProductId();
        int buyNum = cartItemRequest.getBuyNum();

        //获取购物车
        BoundHashOperations<String, Object, Object> myCart = getMyCartOps();
        Object cacheObj = myCart.get(productId);
        String result = "";
        if(cacheObj != null){
            result = (String)cacheObj;
        }
        if(StringUtils.isBlank(result)){//如果为空，则没有这个结果，不存在则新建商品
            CartItemVO cartItemVO = new CartItemVO();
            ProductVO productVO = productService.findDetailById(productId);
            if(productVO == null){//商品不存在，非法id
                throw new BizException(BizCodeEnum.CART_FAIL);
            }
            cartItemVO.setAmount(productVO.getAmount());
            cartItemVO.setBuyNum(buyNum);
            cartItemVO.setProductId(productId);
            cartItemVO.setProductImg(productVO.getCoverImg());
            cartItemVO.setProductTitle(productVO.getTitle());
            myCart.put(productId, JSON.toJSONString(cartItemVO));
        }else{//存在商品，修改数量
            CartItemVO cartItemVO = JSON.parseObject(result, CartItemVO.class);
            cartItemVO.setBuyNum(cartItemVO.getBuyNum() + buyNum);
            myCart.put(productId,JSON.toJSONString(cartItemVO));
        }
    }

    /**
     * 清空购物车
     */
    @Override
    public void clear() {
        String cartKey = getCartKey();
        redisTemplate.delete(cartKey);
    }

    @Override
    public CartVO getMyCart() {
        //获取全部购物项
        List<CartItemVO> cartItemVOList = buildCartItem(false);
        //封装成cartvo
        CartVO cartVO = new CartVO();
        cartVO.setCartItems(cartItemVOList);
        return cartVO;
    }

    @Override
    public void deleteItem(long productId) {
        BoundHashOperations<String, Object, Object> myCartOps = getMyCartOps();
        myCartOps.delete(productId);
    }

    @Override
    public void changeItemNum(CartItemRequest cartItemRequest) {
        BoundHashOperations<String, Object, Object> myCartOps = getMyCartOps();
        Object cacheObj = myCartOps.get(cartItemRequest.getProductId());
        if(cacheObj == null){
            throw new BizException(BizCodeEnum.CART_FAIL);//传了一个不存在的过来就失败
        }
        String obj = (String)cacheObj;
        CartItemVO cartItemVO = JSON.parseObject(obj, CartItemVO.class);
        cartItemVO.setBuyNum(cartItemRequest.getBuyNum());//字段校验，通过hibernate valid
        myCartOps.put(cartItemRequest.getProductId(),JSON.toJSONString(cartItemVO));
    }

    @Override
    public List<CartItemVO> confirmOrderCartItems(List<Long> productIdList) {
        //获取全部购物车的购物项，并且是最新价格
        List<CartItemVO> cartItemVOList = buildCartItem(true);
        //根据需要的商品id进行过滤，并清空对应的购物项
        List<CartItemVO> resultList = cartItemVOList.stream().filter(obj->{//过滤
            if(productIdList.contains(obj.getProductId())){
                this.deleteItem(obj.getProductId());//删掉购物车对应商品信息
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        return resultList;
    }

    //是否获取最新的价格

    /**
     * 获取最新的购物项
     * @param latestPrice 是否获取最新的价格
     * @return
     */
    private List<CartItemVO> buildCartItem(boolean latestPrice) {
        BoundHashOperations<String,Object,Object> myCart = getMyCartOps();
        List<Object> itemList = myCart.values();//全部购物项
        List<CartItemVO> cartItemVOList = new ArrayList<>();
        //拼接id列表查询最新价格
        List<Long> productIdList = new ArrayList<>();
        for (Object item:itemList){
            CartItemVO cartItemVO = JSON.parseObject((String) item, CartItemVO.class);
            cartItemVOList.add(cartItemVO);
            productIdList.add(cartItemVO.getProductId());
        }
        //查询最新的商品价格
        if(latestPrice){
            setProductLatestPrice(cartItemVOList,productIdList);
        }
        return cartItemVOList;
    }

    /**
     * 设置商品最新价格
     * @param cartItemVOList
     * @param productIdList
     */
    private void setProductLatestPrice(List<CartItemVO> cartItemVOList, List<Long> productIdList) {
        //批量查询
        List<ProductVO> productVOList = productService.findProductsByIdBatch(productIdList);

        //分组
        Map<Long, ProductVO> maps = productVOList.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));

        cartItemVOList.stream().forEach(item->{
            ProductVO productVO = maps.get(item.getProductId());
            item.setProductTitle(productVO.getTitle());
            item.setProductImg(productVO.getCoverImg());
            item.setAmount(productVO.getAmount());
        });
    }

    /**
     * 抽取我的购物车通用方法
     * @return
     */
    //BoundHashOperationsgetMyCartOps双重hash结构
    private BoundHashOperations<String,Object,Object> getMyCartOps(){
        String cartKey = getCartKey();
        return redisTemplate.boundHashOps(cartKey);
    }

    /**
     * 购物车 key
     * @return
     */
    private String getCartKey(){
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        String cartKey = String.format(CacheKey.CART_KEY, loginUser.getId());
        return cartKey;
    }

}
