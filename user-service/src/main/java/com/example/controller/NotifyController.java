package com.example.controller;

import com.example.enums.BizCodeEnum;
import com.example.enums.SendCodeEnum;
import com.example.service.NotifyService;
import com.example.util.CommonUtil;
import com.example.util.JsonData;
import com.google.code.kaptcha.Producer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

//之后有各种各样的通知，如邮箱，手机号等
//可以根据后期业务需求添加
@Api(tags = "通知模块")
@RestController
@RequestMapping("/api/user/v1/")
@Slf4j
public class NotifyController {

    @Autowired
    private Producer captchaProducer;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private NotifyService notifyService;

    //图形验证码有效期，10分钟
    private static final long CAPTCHA_CODE_EXPIRED = 60 * 1000 * 10;

    @ApiOperation("获取图形验证码")
    @GetMapping("captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String text = captchaProducer.createText();
        //存储
        //截取验证码内容
        redisTemplate.opsForValue().set(getCaptchaKey(request),text.substring(text.length() - 4),CAPTCHA_CODE_EXPIRED, TimeUnit.MILLISECONDS);
        BufferedImage image = captchaProducer.createImage(text);
        ServletOutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            ImageIO.write(image,"jpg",outputStream);
            outputStream.flush();
        }catch (IOException e){
            e.printStackTrace();
            log.error("获取图形验证码异常");
        }finally {
            if(outputStream != null){
                outputStream.close();
            }
        }
    }

    /**
     * 1.匹配图形验证码是否正常
     * 2.发送验证码
     * 3.
     * @param to
     * @param captcha
     * @return
     */
    @ApiOperation("发送邮箱注册验证码")
    @GetMapping("send_code")
    public JsonData sendRegisterCode(@ApiParam("收信人") @RequestParam(value = "to",required = true) String to,
                                     @ApiParam("图形验证码") @RequestParam(value = "captcha",required = true) String captcha,
                                     HttpServletRequest request){
        String key = getCaptchaKey(request);
        String cacheCaptcha = redisTemplate.opsForValue().get(key);
        //匹配图形验证码是否一样
        if(captcha != null && cacheCaptcha != null && captcha.equalsIgnoreCase(cacheCaptcha)){
            //成功
            //60s后重新请求验证码应该是前端的事情
            redisTemplate.delete(key);
            JsonData jsonData = notifyService.sendCode(SendCodeEnum.USER_REGISTER, to);
            return jsonData;
        }else{
            return JsonData.buildResult(BizCodeEnum.CODE_CAPTCHA_ERROR);
        }
    }

    private String getCaptchaKey(HttpServletRequest request){
        String ip = CommonUtil.getIpAddr(request);
        String userAgent = request.getHeader("user-Agent");

        String key = "user-service:captcha:" + CommonUtil.MD5(ip + userAgent);
        return key;
    }

}
