package com.shanyangcode.infinitechat.messageingservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.infinitechat.messageingservice.model.UserSession;

import java.util.List;

/**
* @author 奇奇怪怪的沙小石
* @description 针对表【user_session】的数据库操作Service
* @createDate 2024-10-17 11:02:09
*/
public interface UserSessionService extends IService<UserSession> {

    /**
     * 通过sessionId得到群聊内的所有用户id
     * @param sessionId
     * @return
     */
    List<Long> getUserIdsBySessionId(Long sessionId);

}
