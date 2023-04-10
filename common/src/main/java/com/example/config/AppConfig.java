package com.example.config;

import feign.RequestInterceptor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
@Slf4j
@Data
public class AppConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private String redisPort;

    @Value("${spring.redis.password}")
    private String redisPwd;

    /**
     * 配置分布式锁
     * @return
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        //单机模式
        //config.useSingleServer().setPassword("123456").setAddress("redis://8.129.113.233:3308");
        config.useSingleServer().setPassword(redisPwd).setAddress("redis://" + redisHost + ":" + redisPort);

        //集群模式
        //config.useClusterServers()
        //.setScanInterval(2000)
        //.addNodeAddress("redis://10.0.29.30:6379", "redis://10.0.29.95:6379")
        // .addNodeAddress("redis://127.0.0.1:6379");

        RedissonClient redisson = Redisson.create(config);

        return redisson;
    }

    /**
     * 避免存储的key乱码，hash结构依旧会乱码
     * @param factory
     * @return
     */
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        RedisSerializer redisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(redisSerializer);//设置结构的序列化方式
        redisTemplate.setValueSerializer(redisSerializer);
        return redisTemplate;
    }

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if(attributes!=null){
                HttpServletRequest request = attributes.getRequest();
                if (null == request){
                    return;
                }
                log.info(request.getHeaderNames().toString());
                template.header("token", request.getHeader("token"));
            }else {
                log.warn("requestInterceptor获取Header空指针异常");
            }
        };
    }

}
