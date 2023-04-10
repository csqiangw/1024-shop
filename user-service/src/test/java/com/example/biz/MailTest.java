package com.example.biz;

import com.example.UserApplication;
import com.example.component.MailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = UserApplication.class)
@Slf4j
public class MailTest {

    @Autowired
    private MailService mailService;

    @Test
    public void testSendEmail(){
        mailService.sendSimpleMail("321516900@qq.com","测试","测试邮箱使用");
    }

}
