package com.shanyangcode.infinitechat.offlinedatastoreservice.service.Impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shangyangcode.infinitechat.offlinedatastoreservice.mapper.SessionMapper;
import com.shangyangcode.infinitechat.offlinedatastoreservice.model.Session;
import com.shangyangcode.infinitechat.offlinedatastoreservice.service.SessionService;
import org.springframework.stereotype.Service;

@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session>
    implements SessionService {

}




