package com.shangyangcode.infinitechat.contactservice.data.GetGroupMembers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class GroupMembersResponse {

    private List<GroupMemberDTO> groupMembers;

    private int total;
}
