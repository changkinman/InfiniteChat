package com.shanyangcode.infinitechat.authenticationservice.controller;

import com.shanyangcode.infinitechat.authenticationservice.common.Result;
import com.shanyangcode.infinitechat.authenticationservice.data.common.sms.SMSRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.common.sms.SMSResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.common.uploadUrl.UploadUrlRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.common.uploadUrl.UploadUrlResponse;
import com.shanyangcode.infinitechat.authenticationservice.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@Slf4j
@RequestMapping("/api/v1/user/common")
public class CommonController {
    @Resource
    private CommonService commonService;

    @GetMapping("/sms")
    public Result<SMSResponse> sendSms(@Valid SMSRequest request) throws Exception {
        SMSResponse response = commonService.sendSms(request);

        return Result.OK(response);
    }

    @GetMapping("/uploadUrl")
    public Result<UploadUrlResponse> getUploadUrl(@Valid UploadUrlRequest request) throws Exception {
        UploadUrlResponse response = commonService.uploadUrl(request);

        return Result.OK(response);
    }
}