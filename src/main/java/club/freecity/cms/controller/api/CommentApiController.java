package club.freecity.cms.controller.api;

import club.freecity.cms.annotation.RateLimit;
import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.dto.CommentDto;
import club.freecity.cms.dto.VerificationRequestDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.service.CommentService;
import club.freecity.cms.service.VerificationService;
import club.freecity.cms.util.IpUtils;
import club.freecity.cms.validator.group.CreateGroup;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评论接口 (包含读者端和管理端)
 */
@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Validated
public class CommentApiController {

    private final CommentService commentService;
    private final VerificationService verificationService;
    
    // --- 读者端接口 ---

    @RateLimit(window = 60, count = 2, key = "send_verification_code:")
    @PostMapping("/verification-code")
    public Result<Void> sendVerificationCode(@Validated @RequestBody VerificationRequestDto requestDto, HttpServletRequest request) {
        verificationService.sendVerificationCode(requestDto.getEmail(), IpUtils.getClientIp(request));
        return Result.success();
    }

    @RateLimit(window = 60, count = 1)
    @PostMapping
    @SecurityAudit(action = AuditAction.COMMENT_CREATE, message = "'提交评论: 文章 ID ' + #commentDto.articleId", logArgs = true)
    public Result<CommentDto> submitComment(@Validated(CreateGroup.class) @RequestBody CommentDto commentDto, 
                                            HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        
        commentDto.setIp(ip);
        commentDto.setUserAgent(request.getHeader("User-Agent"));
        commentDto.setAdminReply(false);
        
        CommentDto saved = commentService.saveComment(commentDto);
        
        return Result.success(saved);
    }

    @GetMapping("/article/{articleId}")
    public Result<Page<CommentDto>> listComments(@PathVariable Long articleId,
                                                 @PageableDefault(sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return Result.success(commentService.listCommentsByArticle(articleId, pageable));
    }

    // --- 管理端接口 ---

    @PostMapping("/admin")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.COMMENT_CREATE, message = "'管理员回复评论: 文章 ID ' + #commentDto.articleId", logArgs = true)
    public Result<CommentDto> saveAdminComment(@Validated(CreateGroup.class) @RequestBody @NotNull CommentDto commentDto) {
        return Result.success(commentService.saveComment(commentDto));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.COMMENT_DELETE, message = "'删除评论 ID: ' + #id")
    public Result<Void> deleteComment(@PathVariable @NotNull Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.COMMENT_UPDATE, message = "'审核评论 ID: ' + #id + ', 状态: ' + #status")
    public Result<Void> auditComment(@PathVariable @NotNull Long id, @RequestParam @NotNull Integer status) {
        commentService.auditComment(id, status);
        return Result.success();
    }

    @GetMapping("/admin/article/{articleId}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<Page<CommentDto>> listAdminCommentsByArticle(@PathVariable @NotNull Long articleId, 
                                                               @PageableDefault(size = 5, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return Result.success(commentService.listAllCommentsByArticle(articleId, pageable));
    }

    @GetMapping("/admin/all")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<Page<CommentDto>> listAllAdminComments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return Result.success(commentService.listAllComments(keyword, status, pageable));
    }

    @PutMapping("/admin/status")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.COMMENT_UPDATE, message = "'审核评论: ' + #ids + ', 状态: ' + #status")
    public Result<Void> auditComments(@RequestBody java.util.List<Long> ids, @RequestParam Integer status) {
        commentService.batchAudit(ids, status);
        return Result.success();
    }

    @DeleteMapping("/admin")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.COMMENT_DELETE, message = "'删除评论: ' + #ids")
    public Result<Void> deleteComments(@RequestBody java.util.List<Long> ids) {
        commentService.batchDelete(ids);
        return Result.success();
    }

    @PostMapping("/admin/reply")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.COMMENT_CREATE, message = "'回复评论: ' + #ids")
    public Result<Void> replyComments(@RequestBody java.util.List<Long> ids, @RequestParam String content) {
        commentService.batchReply(ids, content);
        return Result.success();
    }
}
