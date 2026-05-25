package com.shanyangcode.infinitechat.realtimecommunicationservice.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TextMessage extends Message {

    private TextMessageBody body;

    @Override
    public String toString() {
        return super.toString();
    }
}
