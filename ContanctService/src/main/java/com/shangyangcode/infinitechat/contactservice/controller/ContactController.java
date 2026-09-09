package com.shangyangcode.infinitechat.contactservice.controller;

import com.shangyangcode.infinitechat.contactservice.common.Result;
import com.shangyangcode.infinitechat.contactservice.data.AddFriend.AddFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.AddFriend.AddFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.ApplyList.ApplyListRequest;
import com.shangyangcode.infinitechat.contactservice.data.ApplyList.ApplyListResponse;
import com.shangyangcode.infinitechat.contactservice.data.BlockFriend.BlockFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.BlockFriend.BlockFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.CreateGroup.CreateGroupRequest;
import com.shangyangcode.infinitechat.contactservice.data.CreateGroup.CreateGroupResponse;
import com.shangyangcode.infinitechat.contactservice.data.DeleteFriend.DeleteFriendRequest;
import com.shangyangcode.infinitechat.contactservice.data.DeleteFriend.DeleteFriendResponse;
import com.shangyangcode.infinitechat.contactservice.data.ExitGroup.ExitGroupRequest;
import com.shangyangcode.infinitechat.contactservice.data.ExitGroup.ExitGroupResponse;
import com.shangyangcode.infinitechat.contactservice.data.FriendDetail.FriendDetailRequest;
import com.shangyangcode.infinitechat.contactservice.data.FriendDetail.FriendDetailResponse;
import com.shangyangcode.infinitechat.contactservice.data.GetGroupMembers.GroupMembersRequest;
import com.shangyangcode.infinitechat.contactservice.data.GetGroupMembers.GroupMembersResponse;
import com.shangyangcode.infinitechat.contactservice.data.InviteGroup.InviteGroupRequest;
import com.shangyangcode.infinitechat.contactservice.data.InviteGroup.InviteGroupResponse;
import com.shangyangcode.infinitechat.contactservice.data.KickGroup.KickGroupMembersRequest;
import com.shangyangcode.infinitechat.contactservice.data.KickGroup.KickGroupMembersResponse;
import com.shangyangcode.infinitechat.contactservice.data.ModifyApply.ModifyApplyRequest;
import com.shangyangcode.infinitechat.contactservice.data.ModifyApply.ModifyApplyResponse;
import com.shangyangcode.infinitechat.contactservice.data.SearchUser.SearchUserRequest;
import com.shangyangcode.infinitechat.contactservice.data.SearchUser.SearchUserResponse;
import com.shangyangcode.infinitechat.contactservice.data.UnreadApply.UnreadApplyRequest;
import com.shangyangcode.infinitechat.contactservice.data.UnreadApply.UnreadApplyResponse;
import com.shangyangcode.infinitechat.contactservice.data.User.UserResponse;
import com.shangyangcode.infinitechat.contactservice.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {
    @Autowired
    private FriendService friendService;

    @Autowired
    private ApplyFriendService applyFriendService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GetGroupMembersService getGroupMembersService;

    @Autowired
    private KickGroupService kickGroupMembers;

    @Autowired
    private ExitGroupService exitGroupService;

    @GetMapping("/user")
    public Result<UserResponse> getUser() {
        UserResponse userResponse = new UserResponse();
        userResponse.setAvatar("www.baidu.com");

        return Result.OK(userResponse);
    }

    @GetMapping("/{userUuid}/user")
    public Result<SearchUserResponse> searchUser(@Valid @ModelAttribute SearchUserRequest request){
        SearchUserResponse response = friendService.searchUser(request);

        return Result.OK(response);
    }

    @PostMapping("/{userUuid}/friend/{receiveUserUuid}")
    public Result<AddFriendResponse> addFriend(
            @NotNull(message = "发起人不能为空") @PathVariable("userUuid") String userUuid,
            @NotNull(message = "接受者不能为空") @PathVariable("receiveUserUuid") String receiveUserUuid,
            @RequestBody AddFriendRequest request) throws Exception {


        AddFriendResponse response = applyFriendService.addFriend(userUuid, receiveUserUuid, request);

        return Result.OK(response);
    }

    @GetMapping("/{userUuid}/applyCount")
    public Result<UnreadApplyResponse> getUnreadApplyCount(@Valid @ModelAttribute UnreadApplyRequest request) {
        UnreadApplyResponse response = applyFriendService.getUnreadApply(request);

        return Result.OK(response);
    }

    @GetMapping("/{userUuid}/apply")
    public Result<ApplyListResponse> getApplyList(@Valid @ModelAttribute ApplyListRequest request) {
        ApplyListResponse response = applyFriendService.getApplyList(request);

        return Result.OK(response);
    }

    @PostMapping("/{userUuid}/application/{status}")
    public Result<ModifyApplyResponse> modifyFriendApplicationStatus(@Valid @ModelAttribute ModifyApplyRequest request) throws Exception {
        ModifyApplyResponse response = applyFriendService.modifyApply(request);

        return Result.OK(response);
    }

    @DeleteMapping("/{userUuid}/friend/{receiveUserUuid}")
    public Result<DeleteFriendResponse> deleteFriend(@Valid @ModelAttribute DeleteFriendRequest request) {
        DeleteFriendResponse response = friendService.deleteFriend(request);

        return Result.OK(response);
    }

    @PostMapping("/{userUuid}/block/{receiveUserUuid}")
    public Result<BlockFriendResponse> blockFriend(@Valid @ModelAttribute BlockFriendRequest request) {
        BlockFriendResponse response = friendService.blockFriend(request);

        return Result.OK(response);
    }

    @GetMapping("/{userUuid}/friend/{friendUuid}")
    public Result<FriendDetailResponse> getFriendDetail(@Valid @ModelAttribute FriendDetailRequest request) {
        FriendDetailResponse response = friendService.getFriendDetails(request);

        return Result.OK(response);
    }


    @PostMapping("/groups")
    public Result<CreateGroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        CreateGroupResponse response = sessionService.createGroup(request);

        return Result.OK(response);
    }


    @PostMapping("/group/invite")
    public Result<InviteGroupResponse> inviteGroup(@Valid @RequestBody InviteGroupRequest inviteGroupRequest) throws Exception {
        InviteGroupResponse response = groupService.inviteGroup(inviteGroupRequest);

        return Result.OK(response);
    }


    @PostMapping("/group/kick")
    public Result<KickGroupMembersResponse> kickGroupMembers(@Valid @RequestBody KickGroupMembersRequest request) {
        KickGroupMembersResponse response = kickGroupMembers.kickGroupMembers(request);

        return Result.OK(response);
    }


    @PostMapping("/group/exit")
    public Result<ExitGroupResponse> exitGroup(@RequestBody ExitGroupRequest groupExitRequest) {
        ExitGroupResponse response = exitGroupService.exitGroup(groupExitRequest);

        return Result.OK(response);
    }

    @GetMapping("/group/{sessionId}/members")
    public Result<GroupMembersResponse> getGroupMembers(@Valid GroupMembersRequest request) {
        GroupMembersResponse response = getGroupMembersService.getGroupMembers(request);

        return Result.OK(response);
    }
}