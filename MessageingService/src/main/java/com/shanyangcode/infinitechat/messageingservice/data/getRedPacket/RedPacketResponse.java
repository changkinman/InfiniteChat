package com.shanyangcode.infinitechat.messageingservice.data.getRedPacket;

import com.shanyangcode.infinitechat.messageingservice.data.receiveRedPackage.RedPacketUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class RedPacketResponse {

    private List<RedPacketUser> list;

    private String senderName;

    private String senderAvatar;

    /**
     * 红包封面文案
     */
    private String redPacketWrapperText;

    /**
     * 红包类型：1普通红包，2拼手气红包
     */
    private Integer redPacketType;

    /**
     * 红包总金额
     */
    private BigDecimal totalAmount;

    /**
     * 红包总个数
     */
    private Integer totalCount;

    /**
     * 剩余金额
     */
    private BigDecimal remainingAmount;

    /**
     * 剩余个数
     */
    private Integer remainingCount;

    /**
     * 状态：0正常,1未领取完，2已领取完，3已过期
     */
    private Integer status;

    public RedPacketResponse(Integer status) {
        this.status = status;
    }
}
