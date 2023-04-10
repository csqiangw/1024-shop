package com.example.controller;

import com.example.constant.CacheKey;
import com.example.enums.BizCodeEnum;
import com.example.enums.ClientType;
import com.example.enums.ProductOrderPayTypeEnum;
import com.example.interceptor.LoginInterceptor;
import com.example.model.LoginUser;
import com.example.request.ConfirmOrderRequest;
import com.example.request.RepayOrderRequest;
import com.example.service.ProductOrderService;
import com.example.util.CommonUtil;
import com.example.util.JsonData;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author qiangw
 * @since 2023-04-02
 */
@RestController
@RequestMapping("/api/order/v1")
@Slf4j
public class ProductOrderController {

    @Autowired
    private ProductOrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;


    @ApiOperation("获取提交订单令牌")
    @GetMapping("/get_token")
    public JsonData getOrderToken(){
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        String key = String.format(CacheKey.SUBMIT_ORDER_TOKEN_KEY, loginUser.getId());
        String token = CommonUtil.getStringNumRandom(32);
        redisTemplate.opsForValue().set(key,token,30, TimeUnit.MINUTES);
        return JsonData.buildSuccess(token);
    }

    /**
     * 分页查询我的订单列表
     * 再复杂一些可以封装查询参数
     * @param page
     * @param size
     * @param state
     * @return
     */
    @ApiOperation("分页查询我的订单列表")
    @GetMapping("/page")
    public JsonData pageOrderList(@ApiParam(value = "当前页") @RequestParam(value = "page",defaultValue = "1") int page,
                                    @ApiParam(value = "每页显示多少条") @RequestParam(value = "size",defaultValue = "10") int size,
                                    @ApiParam(value = "订单状态") @RequestParam(value = "state",required = false) String state){
        Map<String,Object> pageMap = orderService.page(page,size,state);
        return JsonData.buildSuccess(pageMap);
    }

    /**
     * 查询订单状态
     *
     * 此接口没有登录拦截，可以增加一个密钥进行rpc通讯
     * @param outTradeNo
     * @return
     */
    @ApiOperation("查询订单状态")
    @GetMapping("/api/order/v1/query_state")
    JsonData queryProductOrderState(@ApiParam("订单号") @RequestParam("out_trade_no") String outTradeNo){
        String state = orderService.queryProductOrderState(outTradeNo);
        return StringUtils.isBlank(state) ? JsonData.buildResult(BizCodeEnum.ORDER_CONFIRM_NOT_EXIST) : JsonData.buildSuccess(state);
    }

    /**
     * 如果支付返回的是二维码，此时需要流去响应，所以用response
     * @param orderRequest
     * @param response
     */
    @ApiOperation("提交订单")
    @PostMapping("/confirm")
    public void confirmOrder(@ApiParam("订单对象") @RequestBody ConfirmOrderRequest orderRequest, HttpServletResponse response){
        JsonData jsonData = orderService.confirmOrder(orderRequest);
        if(jsonData.getCode() == 0){//为0成功
            String client = orderRequest.getClientType();
            String payType = orderRequest.getPayType();
            //如果是支付宝网页支付，都是跳转网页，APP除外
            if(payType.equalsIgnoreCase(ProductOrderPayTypeEnum.ALIPAY.name())){//支付宝支付
                log.info("创建支付宝订单成功:{}",orderRequest.toString());
                if(client.equalsIgnoreCase(ClientType.H5.name())){
                    writeData(response,jsonData);
                }else if(client.equalsIgnoreCase(ClientType.APP.name())){
                    //APP SDK支付 TODO
                }else{
                    //TODO
                }
            }else if(payType.equalsIgnoreCase(ProductOrderPayTypeEnum.WECHAT.name())){
                //微信支付 TODO
            }else{
                //银行卡支付 TODO
            }
        }else{
            log.error("创建订单失败",jsonData.toString());
            CommonUtil.sendJsonMessage(response,jsonData);//让前端感受到失败了
        }
    }

    //把对应的流写出去
    private void writeData(HttpServletResponse response, JsonData jsonData) {
        try {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(jsonData.getData().toString());
            response.getWriter().flush();
            response.getWriter().close();
        }catch (IOException e){
            log.info("写出Html异常:{}",e);
        }
    }

    /**
     * 如果支付返回的是二维码，此时需要流去响应，所以用response
     * @param repayOrderRequest
     * @param response
     */
    @ApiOperation("重新支付订单")
    @PostMapping("/repay")
    public void repay(@ApiParam("订单对象") @RequestBody RepayOrderRequest repayOrderRequest, HttpServletResponse response){
        JsonData jsonData = orderService.repay(repayOrderRequest);
        if(jsonData.getCode() == 0){//为0成功
            String client = repayOrderRequest.getClientType();
            String payType = repayOrderRequest.getPayType();
            //如果是支付宝网页支付，都是跳转网页，APP除外
            if(payType.equalsIgnoreCase(ProductOrderPayTypeEnum.ALIPAY.name())){//支付宝支付
                log.info("创建重新支付 支付宝订单成功:{}",repayOrderRequest.toString());
                if(client.equalsIgnoreCase(ClientType.H5.name())){
                    writeData(response,jsonData);
                }else if(client.equalsIgnoreCase(ClientType.APP.name())){
                    //APP SDK支付 TODO
                }else{
                    //TODO
                }
            }else if(payType.equalsIgnoreCase(ProductOrderPayTypeEnum.WECHAT.name())){
                //微信支付 TODO
            }else{
                //银行卡支付 TODO
            }
        }else{
            log.error("创建重新支付订单失败",jsonData.toString());
            CommonUtil.sendJsonMessage(response,jsonData);//让前端感受到失败了
        }
    }

}

