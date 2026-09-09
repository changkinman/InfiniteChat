package com.shanyangcode.infinitechat.momentservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.infinitechat.momentservice.common.Result;
import com.shanyangcode.infinitechat.momentservice.data.createMoment.CreateMomentRequest;
import com.shanyangcode.infinitechat.momentservice.data.createMoment.CreateMomentResponse;
import com.shanyangcode.infinitechat.momentservice.data.deleteMoment.DeleteMomentRequest;
import com.shanyangcode.infinitechat.momentservice.data.deleteMoment.DeleteMomentResponse;
import com.shanyangcode.infinitechat.momentservice.model.Moment;

public interface MomentService extends IService<Moment> {
    CreateMomentResponse createMoment(CreateMomentRequest request) throws Exception;

    DeleteMomentResponse deleteMoment(DeleteMomentRequest request);

    Long getMomentOwnerId(Long momentId);
}