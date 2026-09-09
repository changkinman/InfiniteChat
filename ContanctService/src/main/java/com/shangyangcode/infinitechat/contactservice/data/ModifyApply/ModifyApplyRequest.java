package com.shangyangcode.infinitechat.contactservice.data.ModifyApply;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Accessors(chain = true)
public class ModifyApplyRequest {

    @NotNull(message = "发起者 uuid 不能为空")
    private Long userUuid;

    @NotNull(message = "状态不能为空")
    private Integer status;

    @NotNull(message = "接收者列表不能为空")
    @Length(min = 1, max = 100)
    private List<Long> receiveUserUuids;
}