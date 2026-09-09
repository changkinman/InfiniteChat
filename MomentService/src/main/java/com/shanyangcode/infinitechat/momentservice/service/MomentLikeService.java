package com.shanyangcode.infinitechat.momentservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.infinitechat.momentservice.common.Result;
import com.shanyangcode.infinitechat.momentservice.data.createLike.CreateLikeRequest;
import com.shanyangcode.infinitechat.momentservice.data.createLike.CreateLikeResponse;
import com.shanyangcode.infinitechat.momentservice.data.deleteLike.DeleteLikeRequest;
import com.shanyangcode.infinitechat.momentservice.data.deleteLike.DeleteLikeResponse;
import com.shanyangcode.infinitechat.momentservice.model.MomentLike;

/**
 * 朋友圈点赞服务接口
 * 提供朋友圈点赞相关功能和响应构建
 */
public interface MomentLikeService extends IService<MomentLike> {
    /**
     * 创建点赞
     *
     * @param momentId 朋友圈ID
     * @param userId 点赞用户ID
     * @return 创建的点赞ID
     */
    Long createLike(Long momentId, Long userId);

    CreateLikeResponse likeMomentResponse(Long momentId, CreateLikeRequest request) throws Exception;

    DeleteLikeResponse deleteLikeMoment(DeleteLikeRequest request);
}

