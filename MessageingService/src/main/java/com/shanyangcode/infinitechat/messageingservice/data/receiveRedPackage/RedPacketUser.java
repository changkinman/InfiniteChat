package com.shanyangcode.infinitechat.messageingservice.data.receiveRedPackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class RedPacketUser {

    private String userName;

    private String avatar;

    private String receivedAt;

    private BigDecimal amount;
}
