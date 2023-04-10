package com.example.interceptor;

import com.example.enums.BizCodeEnum;
import com.example.model.LoginUser;
import com.example.util.CommonUtil;
import com.example.util.JWTUtil;
import com.example.util.JsonData;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    //就认为它是全局变量
     public static ThreadLocal<LoginUser> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //token解密
        String accessToken = request.getHeader("token");
        if(accessToken == null){
            accessToken = request.getParameter("token");//有时通过url传递过来
        }
        //不为空，解密
        if(StringUtils.isNotBlank(accessToken)){
            Claims claims = JWTUtil.checkJWT(accessToken);
            if(claims == null){
                //未登录，这时其实是过期的，解密失败了
                //这时需要通过response返回出去
                CommonUtil.sendJsonMessage(response, JsonData.buildResult(BizCodeEnum.ACCOUNT_UNLOGIN));
                return false;
            }
            long userId = Long.valueOf(claims.get("id").toString());
            String headImg = (String)claims.get("head_img");
            String name = (String)claims.get("name");
            String mail = (String)claims.get("mail");
//            LoginUser loginUser = new LoginUser();
//            loginUser.setHeadImg(headImg);
//            loginUser.setId(userId);
//            loginUser.setName(name);
//            loginUser.setMail(mail);
            //建造者模式
            LoginUser loginUser = LoginUser.builder().headImg(headImg)
                    .id(userId)
                    .name(name)
                    .mail(mail)
                    .build();

//            request.setAttribute("loginUser",loginUser);
            //通过threadLocal传递用户登录信息
            threadLocal.set(loginUser);
            return true;
        }
        CommonUtil.sendJsonMessage(response, JsonData.buildResult(BizCodeEnum.ACCOUNT_UNLOGIN));
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
