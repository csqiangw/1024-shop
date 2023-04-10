package com.example.config;

import com.example.interceptor.LoginInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class InteceptorConfig implements WebMvcConfigurer {

    public LoginInterceptor loginInterceptor(){
        return new LoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //拦截路径
        registry.addInterceptor(loginInterceptor())
                .addPathPatterns("/api/order/*/**")
                //不拦截路径 和第三方交互时不拦截，回调不拦截
                //用户和第三方交互付钱，付钱后，第三方会告诉服务器，服务器再告诉用户支付成功，此时这些接口不能被拦截

                //还有一个，微服务之间定时调用，看看订单状态，比如取消，此时要回收优惠券等
                .excludePathPatterns("/api/callback/*/**","/api/order/*/query_state","/api/order/*/test_pay");
    }

}
