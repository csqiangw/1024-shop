package com.example.service.impl;

import com.example.constant.CacheKey;
import com.example.enums.BizCodeEnum;
import com.example.enums.SendCodeEnum;
import com.example.component.MailService;
import com.example.service.NotifyService;
import com.example.util.CheckUtil;
import com.example.util.CommonUtil;
import com.example.util.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    @Autowired
    private MailService mailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    //验证码标题
    private static final String SUBJECT = "1024-shop验证码";

    //验证码内容
    private static final String CONTENT = "您的验证码是%s，有效时间是10分钟，请不要告诉任何人";

    //10分钟有效
    private static final int CODE_EXPIRED = 60 * 1000 * 10;

    /**
     * 之后可以根据sendCodeEnum进行扩展，不同的场景给不同的验证码模板
     * @param sendCodeEnum
     * @param to
     * @return
     */
    @Override
    public JsonData sendCode(SendCodeEnum sendCodeEnum, String to) {
        String cacheKey = String.format(CacheKey.CHECK_CODE_KEY,sendCodeEnum.name(),to);
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        //如果不为空，则判断60s内是否重复发送
        if(StringUtils.isNotBlank(cacheValue)){
            long ttl = Long.parseLong(cacheValue.split("_")[1]);
            //当前时间戳-验证码发送时间戳
            if(CommonUtil.getCurrentTimestamp() - ttl < 1000 * 60){
                log.info("重复发送验证码，时间间隔：{}秒",(CommonUtil.getCurrentTimestamp() - ttl) / 1000);
                return JsonData.buildResult(BizCodeEnum.CODE_LIMITED);
            }
        }

        //拼接验证码 2322_32132131321
        String randomCode = CommonUtil.getRandomCode(6);
        String value = randomCode + "_" +CommonUtil.getCurrentTimestamp();
        redisTemplate.opsForValue().set(cacheKey,value,CODE_EXPIRED, TimeUnit.MILLISECONDS);

        if(CheckUtil.isEmail(to)){
            mailService.sendSimpleMail(to,SUBJECT,String.format(CONTENT,randomCode));
            return JsonData.buildSuccess();
        }//手机号判断
        return JsonData.buildResult(BizCodeEnum.CODE_TO_ERROR);
    }

    /**
     * 校验验证码
     * @param sendCodeEnum
     * @param to
     * @param code
     * @return
     */
    @Override
    public boolean checkCode(SendCodeEnum sendCodeEnum, String to, String code) {
        String cacheKey = String.format(CacheKey.CHECK_CODE_KEY,sendCodeEnum.name(),to);
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if(StringUtils.isNotBlank(cacheValue)){
            String cacheCode = cacheValue.split("_")[0];
            if(cacheCode.equals(code)){
                //在这里，校验用户填写邮箱的验证码（前端负责发过来），此时校验成功后，就可以删除了
                redisTemplate.delete(cacheKey);
                return true;
            }
        }
        return false;
    }

}
