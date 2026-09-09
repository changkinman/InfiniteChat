package com.shanyangcode.infinitechat.authenticationservice.service.impl;

import com.shanyangcode.infinitechat.authenticationservice.constants.config.OSSConstant;
import com.shanyangcode.infinitechat.authenticationservice.constants.user.registerConstant;
import com.shanyangcode.infinitechat.authenticationservice.data.common.sms.SMSRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.common.sms.SMSResponse;
import com.shanyangcode.infinitechat.authenticationservice.data.common.uploadUrl.UploadUrlRequest;
import com.shanyangcode.infinitechat.authenticationservice.data.common.uploadUrl.UploadUrlResponse;
import com.shanyangcode.infinitechat.authenticationservice.service.CommonService;
import com.shanyangcode.infinitechat.authenticationservice.utils.OSSUtils;
import com.shanyangcode.infinitechat.authenticationservice.utils.RandomNumUtil;
import com.shanyangcode.infinitechat.authenticationservice.utils.SMSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OSSUtils ossUtils;

    @Override
    public SMSResponse sendSms(SMSRequest request) throws Exception {
        String phone = request.getPhone();
        String code = RandomNumUtil.getRandomNum();

        redisTemplate.opsForValue().set(registerConstant.REGISTER_CODE + phone, code, 5, TimeUnit.MINUTES);
        new SMSUtil().sendServiceSms(phone, code);

        return new SMSResponse().setPhone(phone);
    }

    @Override
    public UploadUrlResponse uploadUrl(UploadUrlRequest request) throws Exception {
        String fileName = request.getFileName();

        String uploadUrl = ossUtils.uploadUrl(OSSConstant.BUCKET_NAME, fileName, OSSConstant.PICTURE_EXPIRE_TIME);
        String downUrl = ossUtils.downUrl(OSSConstant.BUCKET_NAME, fileName);

        UploadUrlResponse response = new UploadUrlResponse();
        response.setUploadUrl(uploadUrl)
                .setDownloadUrl(downUrl);

        return response;
    }
}