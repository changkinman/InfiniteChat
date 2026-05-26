package com.shanyangcode.infinitechat.messageingservice.feign;

import com.shanyangcode.infinitechat.messageingservice.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("ContactService")
public interface ContactServiceFeign {
    @GetMapping("/api/v1/contact/user")
    Result<?> getUser();
}