package com.shanyangcode.infinitechat.realtimecommunicationservice.controller;

import com.shanyangcode.infinitechat.realtimecommunicationservice.common.Result;
import com.shanyangcode.infinitechat.realtimecommunicationservice.data.connection.ConnectionTakeoverRequest;
import com.shanyangcode.infinitechat.realtimecommunicationservice.websocket.ConnectionBinding;
import com.shanyangcode.infinitechat.realtimecommunicationservice.websocket.ChannelManager;
import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalConnectionControllerTest {

    @Mock
    private Channel currentChannel;

    @Test
    void takeoverClosesOnlyTheExactConnectionAndStillSucceedsWhenStale() {
        ChannelManager channelManager = new ChannelManager();
        channelManager.register(new ConnectionBinding("42", "current-id", currentChannel));
        InternalConnectionController controller = new InternalConnectionController(channelManager);

        Result<Void> staleResult = controller.takeover(new ConnectionTakeoverRequest("42", "old-id"));

        assertThat(staleResult.getCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(staleResult.getData()).isNull();
        verify(currentChannel, never()).close();
        assertThat(channelManager.findByUserId("42")).isPresent();

        Result<Void> currentResult = controller.takeover(new ConnectionTakeoverRequest("42", "current-id"));

        assertThat(currentResult.getCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(currentResult.getData()).isNull();
        verify(currentChannel).close();
    }
}
