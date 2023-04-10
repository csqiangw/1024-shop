package com.example.biz;

import com.example.ProductApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = ProductApplication.class)
@RunWith(SpringRunner.class)
@Slf4j
public class MQTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void sendDelayMsg(){
        rabbitTemplate.convertAndSend("stock.event.exchange","stock.release.delay.routing.key","this is stock record lock msg");
    }


}
