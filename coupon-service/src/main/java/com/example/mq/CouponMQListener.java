package com.example.mq;

import com.example.model.CouponRecordMessage;
import com.example.service.CouponRecordService;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
@RabbitListener(queues = "${mqconfig.coupon_release_queue}")
public class CouponMQListener {

    @Autowired
    private CouponRecordService couponRecordService;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 重复消费-幂等性
     * 消费失败，重新入队后最大入队次数：
     * 如果消费失败不重新入队，可以记录日志，然后插到数据库，人工排查
     *
     * @param recordMessage
     * @param message
     * @param channel
     * @throws IOException
     */
    //报错原因，因为测试的时候，发了一条String，然后这面连接的时候，String发到了recordMessage，转换不过来，所以失败了
    @RabbitHandler
    public void releaseCouponRecord(CouponRecordMessage recordMessage, Message message, Channel channel) throws IOException {
        log.info("监听到消息:releaseCouponRecord消息内容:{}",recordMessage);
        long msgTag = message.getMessageProperties().getDeliveryTag();

        //多个重复消息进入，考虑加锁，解决重复消息问题
        //考虑是否会释放重复,等等看这面的业务场景 TODO
        //处理对应的逻辑
        boolean flag = couponRecordService.releaseCouponRecord(recordMessage);

        try {
            if (flag){
                //消息消费成功
                channel.basicAck(msgTag,false);
            }else{//用redis，记录一下多少次了，多了就不投了，插到数据库表示异常
                log.error("释放优惠券失败,flag=false,{}",recordMessage);
                channel.basicReject(msgTag,true);
            }
        }catch (IOException e){
            log.error("释放优惠券记录异常:{},msg:{}",e,recordMessage);
            channel.basicReject(msgTag,true);
        }
    }

}
