package com.shangyangcode.infinitechat.contactservice.data.CreateGroup;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Accessors(chain = true)
public class CreateGroupRequest {

    @NotNull(message = "群主id不能为空")
    private Long creatorId;

    @NotNull(message = "群成员不能为空")
    private List<Long> memberIds;
}
