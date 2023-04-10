package com.example.feign;

import com.example.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("user-service")
public interface UserFeignService {

    @GetMapping("/api/address/v1/detail/{address id}")
    JsonData detail(@PathVariable("address id") long addressId);

}
