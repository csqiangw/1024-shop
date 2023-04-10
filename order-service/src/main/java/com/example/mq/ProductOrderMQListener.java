package com.example.mq;

import com.example.model.OrderMessage;
import com.example.service.ProductOrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
@RabbitListener(queues = "${mqconfig.order_close_queue}")
public class ProductOrderMQListener {

    @Autowired
    private ProductOrderService productOrderService;

    /**
     * 重复消费-幂等性
     * 消费失败，重新入队后最大入队次数：
     * 如果消费失败不重新入队，可以记录日志，然后插到数据库，人工排查
     *
     * @param orderMessage
     * @param message
     * @param channel
     * @throws IOException
     */
    //报错原因，因为测试的时候，发了一条String，然后这面连接的时候，String发到了recordMessage，转换不过来，所以失败了
    @RabbitHandler
    public void closeProductOrder(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        log.info("监听到消息:closeProductOrder消息内容:{}",orderMessage);
        long msgTag = message.getMessageProperties().getDeliveryTag();

        //多个重复消息进入，考虑加锁，解决重复消息问题
        //考虑是否会释放重复,等等看这面的业务场景 TODO
        //处理对应的逻辑
        boolean flag = productOrderService.closeProductOrder(orderMessage);

        try {
            if (flag){
                //消息消费成功
                channel.basicAck(msgTag,false);
            }else{//用redis，记录一下多少次了，多了就不投了，插到数据库表示异常
                log.error("关单失败,flag=false,{}",orderMessage);
                channel.basicReject(msgTag,true);
            }
        }catch (IOException e){
            log.error("关单异常:{},msg:{}",e,orderMessage);
            channel.basicReject(msgTag,true);
        }
    }

}
