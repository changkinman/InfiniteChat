package com.shanyangcode.infinitechat.messageingservice.service.impl;

import com.shanyangcode.infinitechat.messageingservice.constants.ConfigEnum;
import com.shanyangcode.infinitechat.messageingservice.constants.SessionType;
import com.shanyangcode.infinitechat.messageingservice.data.sendMsg.AppMessage;
import com.shanyangcode.infinitechat.messageingservice.data.sendMsg.SendMsgRequest;
import com.shanyangcode.infinitechat.messageingservice.mapper.FriendMapper;
import com.shanyangcode.infinitechat.messageingservice.routing.OnlineRouteLookup;
import com.shanyangcode.infinitechat.messageingservice.service.SessionService;
import com.shanyangcode.infinitechat.messageingservice.service.UserService;
import com.shanyangcode.infinitechat.messageingservice.service.UserSessionService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageServiceImplTest {

    private HttpServer server;

    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void singleMessageUsesResolvedEndpointWhenDiscoveryHasNoInstances() throws Exception {
        CountDownLatch delivered = new CountDownLatch(1);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(ConfigEnum.MSG_URL.getValue(), exchange -> {
            delivered.countDown();
            byte[] body = "ok".getBytes("UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });
        server.start();

        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("user:session:42", "endpoint")).thenReturn(endpoint);

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances("RealTimeCommunicationService")).thenReturn(Collections.emptyList());

        MessageServiceImpl messageService = new MessageServiceImpl(
                mock(UserService.class),
                mock(FriendMapper.class),
                mock(UserSessionService.class),
                mock(SessionService.class),
                discoveryClient,
                mock(RedisTemplate.class),
                new OnlineRouteLookup(stringRedisTemplate),
                mock(KafkaTemplate.class)
        );

        SendMsgRequest request = new SendMsgRequest();
        request.setSessionType(SessionType.SINGLE.getValue());
        request.setReceiveUserId(42L);

        invokeSendRealTimeMessage(messageService, request, new AppMessage());

        assertTrue(delivered.await(1, TimeUnit.SECONDS));
    }

    private void invokeSendRealTimeMessage(MessageServiceImpl messageService, SendMsgRequest request, AppMessage message) throws Exception {
        Method method = MessageServiceImpl.class.getDeclaredMethod("sendRealTimeMessage", SendMsgRequest.class, AppMessage.class, Date.class);
        method.setAccessible(true);
        try {
            method.invoke(messageService, request, message, new Date());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }
}
