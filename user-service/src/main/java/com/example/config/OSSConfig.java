package com.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class OSSConfig {

    //配置中的横杠会自动转为驼峰

     private String endpoint;

     private String accessKeyId;

     private String accessKeySecret;

     private String bucketname;

}
