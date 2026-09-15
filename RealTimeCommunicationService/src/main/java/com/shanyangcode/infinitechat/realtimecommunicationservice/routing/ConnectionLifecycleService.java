package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import com.shanyangcode.infinitechat.realtimecommunicationservice.websocket.ChannelManager;
import com.shanyangcode.infinitechat.realtimecommunicationservice.websocket.ConnectionBinding;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class ConnectionLifecycleService {

    private static final int STRIPE_COUNT = 256;

    private final OnlineRouteRegistry routes;
    private final ChannelManager channels;
    private final ConnectionTakeoverClient takeoverClient;
    private final LocalNode localNode;
    private final ReentrantLock[] stripes = new ReentrantLock[STRIPE_COUNT];

    public ConnectionLifecycleService(OnlineRouteRegistry routes,
                                      ChannelManager channels,
                                      ConnectionTakeoverClient takeoverClient,
                                      LocalNode localNode) {
        this.routes = routes;
        this.channels = channels;
        this.takeoverClient = takeoverClient;
        this.localNode = localNode;
        for (int i = 0; i < stripes.length; i++) {
            stripes[i] = new ReentrantLock();
        }
    }

    public boolean establish(String userId, Channel channel) {
        ReentrantLock lock = stripe(userId);
        lock.lock();
        String connectionId = UUID.randomUUID().toString();
        ConnectionBinding newBinding = new ConnectionBinding(userId, connectionId, channel);
        boolean registered = false;
        try {
            OnlineRoute newRoute = new OnlineRoute(localNode.getNodeId(), localNode.getEndpoint(), connectionId);
            Optional<OnlineRoute> oldRoute = routes.claim(userId, newRoute);
            ConnectionBinding previousLocal = channels.register(newBinding);
            registered = true;

            if (!routes.isOwner(userId, connectionId)) {
                channels.remove(newBinding);
                channel.close();
                return false;
            }

            if (previousLocal != null) {
                channels.close(previousLocal);
            }
            if (oldRoute.isPresent() && !localNode.getNodeId().equals(oldRoute.get().getNodeId())) {
                requestRemoteTakeover(oldRoute.get(), userId);
            }
            return true;
        } catch (RuntimeException ex) {
            log.warn("Failed to establish routed websocket connection for user {}", userId, ex);
            if (registered) {
                channels.remove(newBinding);
            }
            try {
                routes.release(userId, connectionId);
            } catch (RuntimeException releaseEx) {
                log.warn("Failed to release routed websocket connection {} for user {}", connectionId, userId, releaseEx);
            }
            channel.close();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean heartbeat(Channel channel) {
        Optional<ConnectionBinding> binding = channels.findByChannel(channel);
        if (!binding.isPresent()) {
            channel.close();
            return false;
        }

        ConnectionBinding current = binding.get();
        try {
            if (routes.renew(current.getUserId(), current.getConnectionId())) {
                return true;
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to renew routed websocket connection {} for user {}",
                    current.getConnectionId(), current.getUserId(), ex);
        }
        channels.close(current);
        return false;
    }

    public void disconnect(Channel channel) {
        Optional<ConnectionBinding> binding = channels.findByChannel(channel);
        if (!binding.isPresent()) {
            channel.close();
            return;
        }

        ConnectionBinding current = binding.get();
        channels.remove(current);
        try {
            routes.release(current.getUserId(), current.getConnectionId());
        } catch (RuntimeException ex) {
            log.warn("Failed to release routed websocket connection {} for user {}",
                    current.getConnectionId(), current.getUserId(), ex);
        } finally {
            channel.close();
        }
    }

    private ReentrantLock stripe(String userId) {
        return stripes[userId.hashCode() & 255];
    }

    private void requestRemoteTakeover(OnlineRoute oldRoute, String userId) {
        try {
            takeoverClient.requestTakeover(oldRoute, userId);
        } catch (RuntimeException ex) {
            log.warn("Connection takeover request failed for user {} and old route {}", userId, oldRoute, ex);
        }
    }
}
