package com.shanyangcode.infinitechat.momentservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.infinitechat.momentservice.model.Friend;

import java.util.List;

public interface FriendService extends IService<Friend> {
    List<Long> getFriendIds(Long userId);
}