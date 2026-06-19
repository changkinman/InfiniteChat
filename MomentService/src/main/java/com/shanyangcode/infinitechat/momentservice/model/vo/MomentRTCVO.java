package com.shanyangcode.infinitechat.momentservice.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class MomentRTCVO implements Serializable {

    private List<Long> receiveUserIds;

    private Integer noticeType;

    private String avatar;

    private Integer total;
}
