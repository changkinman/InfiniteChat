package com.shanyangcode.infinitechat.momentservice.controller;

import com.shanyangcode.infinitechat.momentservice.common.Result;
import com.shanyangcode.infinitechat.momentservice.data.createComment.CreateCommentRequest;
import com.shanyangcode.infinitechat.momentservice.data.createComment.CreateCommentResponse;
import com.shanyangcode.infinitechat.momentservice.data.createComment.MomentCommentDTO;
import com.shanyangcode.infinitechat.momentservice.data.createLike.CreateLikeRequest;
import com.shanyangcode.infinitechat.momentservice.data.createLike.CreateLikeResponse;
import com.shanyangcode.infinitechat.momentservice.data.createMoment.CreateMomentRequest;
import com.shanyangcode.infinitechat.momentservice.data.createMoment.CreateMomentResponse;
import com.shanyangcode.infinitechat.momentservice.data.deleteComment.DeleteCommentRequest;
import com.shanyangcode.infinitechat.momentservice.data.deleteComment.DeleteCommentResponse;
import com.shanyangcode.infinitechat.momentservice.data.deleteLike.DeleteLikeRequest;
import com.shanyangcode.infinitechat.momentservice.data.deleteLike.DeleteLikeResponse;
import com.shanyangcode.infinitechat.momentservice.data.deleteMoment.DeleteMomentRequest;
import com.shanyangcode.infinitechat.momentservice.data.deleteMoment.DeleteMomentResponse;
import com.shanyangcode.infinitechat.momentservice.service.MomentCommentService;
import com.shanyangcode.infinitechat.momentservice.service.MomentLikeService;
import com.shanyangcode.infinitechat.momentservice.service.MomentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/api/v1/moment")
@RequiredArgsConstructor
public class MomentController {
    @Autowired
    private MomentService momentService;

    @Autowired
    private MomentLikeService momentLikeService;

    @Autowired
    private MomentCommentService momentCommentService;

    @PostMapping("")
    public Result<CreateMomentResponse> createMoment(@Valid @RequestBody CreateMomentRequest request) throws Exception {
        CreateMomentResponse response = momentService.createMoment(request);

        return Result.OK(response);
    }

    @DeleteMapping("/{momentId}")
    public Result<DeleteMomentResponse> deleteMoment(@Valid @ModelAttribute DeleteMomentRequest request) {
        DeleteMomentResponse response = momentService.deleteMoment(request);

       return Result.OK(response);
    }

    @PostMapping("/like/{momentId}")
    public Result<CreateLikeResponse> likeMoment(@PathVariable Long momentId, @Valid @RequestBody CreateLikeRequest request) throws Exception {
        CreateLikeResponse response = momentLikeService.likeMomentResponse(momentId, request);

        return Result.OK(response);
    }

    @DeleteMapping("/like/{momentId}")
    public Result<DeleteLikeResponse> deleteLikeMoment(@Valid @ModelAttribute DeleteLikeRequest request) {
        DeleteLikeResponse response = momentLikeService.deleteLikeMoment(request);

        return Result.OK(response);
    }

    @PostMapping("/comment/{momentId}")
    public Result<CreateCommentResponse> createMoment(
            @NotNull(message = "朋友圈 ID 不能为空") @PathVariable("momentId") Long momentId,
            @Valid @RequestBody MomentCommentDTO momentCommentDTO) throws Exception {
        CreateCommentRequest request = new CreateCommentRequest()
                .setMomentId(momentId)
                .setMomentCommentDTO(momentCommentDTO);


        CreateCommentResponse response = momentCommentService.createComment(request);

        return Result.OK(response);
    }

    @DeleteMapping("/comment/{momentId}")
    public Result<DeleteCommentResponse> deleteComment(@Valid @ModelAttribute DeleteCommentRequest request) {
        DeleteCommentResponse response = momentCommentService.deleteComment(request);

        return Result.OK(response);
    }


}