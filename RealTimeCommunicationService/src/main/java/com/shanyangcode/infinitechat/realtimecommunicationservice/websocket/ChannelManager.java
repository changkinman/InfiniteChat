package com.shanyangcode.infinitechat.realtimecommunicationservice.websocket;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class ChannelManager {
    private final ConcurrentHashMap<String, ConnectionBinding> userBindings = new ConcurrentHashMap<String, ConnectionBinding>();
    private final ConcurrentHashMap<Channel, ConnectionBinding> channelBindings = new ConcurrentHashMap<Channel, ConnectionBinding>();
    private final ConcurrentHashMap<String, ConnectionBinding> legacyChannelFirstRemovals = new ConcurrentHashMap<String, ConnectionBinding>();

    public ConnectionBinding register(ConnectionBinding binding) {
        ConnectionBinding prior = userBindings.put(binding.getUserId(), binding);
        channelBindings.put(binding.getChannel(), binding);
        legacyChannelFirstRemovals.remove(binding.getUserId(), binding);
        return prior;
    }

    public Optional<ConnectionBinding> findByUserId(String userId) {
        return Optional.ofNullable(userBindings.get(userId));
    }

    public Optional<ConnectionBinding> findByChannel(Channel channel) {
        return Optional.ofNullable(channelBindings.get(channel));
    }

    public boolean remove(ConnectionBinding binding) {
        boolean userRemoved = userBindings.remove(binding.getUserId(), binding);
        channelBindings.remove(binding.getChannel(), binding);
        legacyChannelFirstRemovals.remove(binding.getUserId(), binding);
        return userRemoved;
    }

    public boolean close(ConnectionBinding binding) {
        userBindings.remove(binding.getUserId(), binding);
        boolean channelRemoved = channelBindings.remove(binding.getChannel(), binding);
        legacyChannelFirstRemovals.remove(binding.getUserId(), binding);
        if (channelRemoved) {
            binding.getChannel().close();
        }
        return channelRemoved;
    }

    public boolean closeIfMatches(String userId, String connectionId) {
        ConnectionBinding binding = userBindings.get(userId);
        if (binding == null || !binding.getConnectionId().equals(connectionId)) {
            return false;
        }
        if (!userBindings.remove(userId, binding)) {
            return false;
        }
        channelBindings.remove(binding.getChannel(), binding);
        legacyChannelFirstRemovals.remove(userId, binding);
        binding.getChannel().close();
        return true;
    }

    public int activeUserCount() {
        return userBindings.size();
    }

    public int activeChannelCount() {
        return channelBindings.size();
    }

    public Channel getChannelByUserId(String userId) {
        ConnectionBinding binding = userBindings.get(userId);
        return binding == null ? null : binding.getChannel();
    }

    public void addUserChannel(String userUuid, Channel channel) {
        register(new ConnectionBinding(userUuid, channel.id().asLongText(), channel));
    }

    public void addChannelUser(String userUuid, Channel channel) {
        ConnectionBinding binding = userBindings.get(userUuid);
        if (binding == null || !binding.getChannel().equals(channel)) {
            binding = new ConnectionBinding(userUuid, channel.id().asLongText(), channel);
            userBindings.putIfAbsent(userUuid, binding);
        }
        channelBindings.putIfAbsent(channel, binding);
        legacyChannelFirstRemovals.remove(userUuid, binding);
    }

    public void removeUserChannel(String userUuid) {
        ConnectionBinding channelFirstRemoval = legacyChannelFirstRemovals.remove(userUuid);
        if (channelFirstRemoval != null) {
            userBindings.remove(userUuid, channelFirstRemoval);
            return;
        }

        ConnectionBinding binding = userBindings.remove(userUuid);
        if (binding != null) {
            channelBindings.remove(binding.getChannel(), binding);
        }
    }

    public void removeChannelUser(Channel channel) {
        ConnectionBinding binding = channelBindings.remove(channel);
        if (binding != null) {
            legacyChannelFirstRemovals.put(binding.getUserId(), binding);
        }
    }

    public String getUserByChannel(Channel channel) {
        ConnectionBinding binding = channelBindings.get(channel);
        return binding == null ? null : binding.getUserId();
    }
}
