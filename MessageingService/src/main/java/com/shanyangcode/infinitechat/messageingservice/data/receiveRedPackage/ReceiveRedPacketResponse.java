package com.shanyangcode.infinitechat.messageingservice.data.receiveRedPackage;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ReceiveRedPacketResponse {

    private BigDecimal receivedAmount;

    private Integer status;

    private String message;

    public ReceiveRedPacketResponse(BigDecimal receivedAmount, Integer status) {
        this(receivedAmount, status, null);
    }

    public ReceiveRedPacketResponse(BigDecimal receivedAmount, Integer status, String message) {
        this.receivedAmount = receivedAmount;
        this.status = status;
        this.message = message;
    }
}
