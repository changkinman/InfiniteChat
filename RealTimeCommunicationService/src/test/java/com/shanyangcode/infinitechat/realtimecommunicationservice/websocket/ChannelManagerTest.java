package com.shanyangcode.infinitechat.realtimecommunicationservice.websocket;

import io.netty.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChannelManagerTest {

    @Mock
    private Channel oldChannel;

    @Mock
    private Channel currentChannel;

    @Mock
    private Channel oldSecondChannel;

    private ChannelManager channelManager;

    @BeforeEach
    void setUp() {
        channelManager = new ChannelManager();
    }

    @Test
    void removeOldBindingDoesNotRemoveCurrentUserMapping() {
        ConnectionBinding oldBinding = new ConnectionBinding("42", "old", oldChannel);
        ConnectionBinding currentBinding = new ConnectionBinding("42", "current", currentChannel);
        channelManager.register(oldBinding);
        channelManager.register(currentBinding);

        boolean removed = channelManager.remove(oldBinding);

        assertThat(removed).isFalse();
        assertThat(channelManager.findByUserId("42")).contains(currentBinding);
        assertThat(channelManager.getChannelByUserId("42")).isSameAs(currentChannel);
    }

    @Test
    void closeIfMatchesOnlyClosesCurrentConnectionId() {
        ConnectionBinding currentBinding = new ConnectionBinding("42", "current", currentChannel);
        channelManager.register(currentBinding);

        assertThat(channelManager.closeIfMatches("42", "old")).isFalse();
        verify(currentChannel, never()).close();

        assertThat(channelManager.closeIfMatches("42", "current")).isTrue();
        verify(currentChannel).close();
        assertThat(channelManager.findByUserId("42")).isEmpty();
        assertThat(channelManager.findByChannel(currentChannel)).isEmpty();
    }

    @Test
    void closingOldBindingKeepsCurrentUserMappingAndExactOldChannelMapping() {
        ConnectionBinding oldBinding = new ConnectionBinding("42", "old", oldChannel);
        ConnectionBinding currentBinding = new ConnectionBinding("42", "current", currentChannel);
        channelManager.register(oldBinding);

        ConnectionBinding prior = channelManager.register(currentBinding);

        assertThat(prior).isEqualTo(oldBinding);
        assertThat(channelManager.findByChannel(oldChannel)).contains(oldBinding);

        assertThat(channelManager.close(oldBinding)).isTrue();

        verify(oldChannel).close();
        assertThat(channelManager.findByUserId("42")).contains(currentBinding);
        assertThat(channelManager.findByChannel(oldChannel)).isEmpty();
        assertThat(channelManager.findByChannel(currentChannel)).contains(currentBinding);
    }

    @Test
    void multipleChannelFirstLegacyRemovalsCannotDeleteCurrentUserMapping() {
        ConnectionBinding oldBinding = new ConnectionBinding("42", "old", oldChannel);
        ConnectionBinding oldSecondBinding = new ConnectionBinding("42", "old-second", oldSecondChannel);
        ConnectionBinding currentBinding = new ConnectionBinding("42", "current", currentChannel);
        channelManager.register(oldBinding);
        channelManager.register(oldSecondBinding);
        channelManager.register(currentBinding);

        channelManager.removeChannelUser(oldChannel);
        channelManager.removeChannelUser(oldSecondChannel);
        channelManager.removeUserChannel("42");
        channelManager.removeUserChannel("42");

        assertThat(channelManager.findByUserId("42")).contains(currentBinding);
        assertThat(channelManager.findByChannel(currentChannel)).contains(currentBinding);
    }
}
