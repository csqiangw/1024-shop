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
                .addPathPatterns("/api/banner/*/**","/api/product/*/**","/api/cart/*/**")
                //不拦截路径
                .excludePathPatterns("/api/banner/*/list","/api/product/*/page","/api/product/*/detail/**");
    }
}
