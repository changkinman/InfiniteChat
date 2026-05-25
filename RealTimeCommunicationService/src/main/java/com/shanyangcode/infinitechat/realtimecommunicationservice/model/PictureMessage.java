package com.shanyangcode.infinitechat.realtimecommunicationservice.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PictureMessage extends Message {

    private PictureMessageBody body;

    @Override
    public String toString() {
        return super.toString();
    }
}
