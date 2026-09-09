package com.shangyangcode.infinitechat.contactservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shangyangcode.infinitechat.contactservice.data.AddFriend.AddFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.AddFriend.AddFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.ApplyList.ApplyListRequest;
import com.shangyangcode.infinitechat.contactservice.data.ApplyList.ApplyListResponse;
import com.shangyangcode.infinitechat.contactservice.data.ModifyApply.ModifyApplyRequest;
import com.shangyangcode.infinitechat.contactservice.data.ModifyApply.ModifyApplyResponse;
import com.shangyangcode.infinitechat.contactservice.data.UnreadApply.UnreadApplyRequest;
import com.shangyangcode.infinitechat.contactservice.data.UnreadApply.UnreadApplyResponse;
import com.shangyangcode.infinitechat.contactservice.model.ApplyFriend;

public interface ApplyFriendService extends IService<ApplyFriend> {
    /**
     * 添加好友
     */
    AddFriendResponse addFriend(String userUuid, String receiveUserUuid, AddFriendRequest request) throws Exception;

    /**
     * 获取好友申请列表
     */
    ApplyListResponse getApplyList(ApplyListRequest request);

    UnreadApplyResponse getUnreadApply(UnreadApplyRequest request);

    ModifyApplyResponse modifyApply(ModifyApplyRequest request) throws Exception;

}