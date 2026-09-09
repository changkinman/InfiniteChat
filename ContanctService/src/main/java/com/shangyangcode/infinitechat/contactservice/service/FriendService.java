package com.shangyangcode.infinitechat.contactservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shangyangcode.infinitechat.contactservice.data.BlockFriend.BlockFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.BlockFriend.BlockFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.DeleteFriend.DeleteFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.DeleteFriend.DeleteFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.FriendDetail.FriendDetailRequest;
import com.shangyangcode.infinitechat.contactservice.data.FriendDetail.FriendDetailResponse;
import com.shangyangcode.infinitechat.contactservice.data.ModifyApply.ModifyApplyResponse;
import com.shangyangcode.infinitechat.contactservice.data.SearchUser.SearchUserRequest;
import com.shangyangcode.infinitechat.contactservice.data.SearchUser.SearchUserResponse;
import com.shangyangcode.infinitechat.contactservice.model.Friend;

import java.util.List;

public interface FriendService extends IService<Friend> {
    SearchUserResponse searchUser(SearchUserRequest request);

    DeleteFriendResponse deleteFriend(DeleteFriendRequest request);

    BlockFriendResponse blockFriend(BlockFriendRequest request);

    ModifyApplyResponse addFriend(Long userId, Long friendId) throws Exception;

    FriendDetailResponse getFriendDetails(FriendDetailRequest request);
}