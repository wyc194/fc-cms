package club.freecity.cms.service;

import club.freecity.cms.dto.CommentDto;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {
    CommentDto saveComment(@NotNull CommentDto commentDto);
    void deleteComment(@NotNull Long id);
    Page<CommentDto> listCommentsByArticle(@NotNull Long articleId, Pageable pageable);
    Page<CommentDto> listAllCommentsByArticle(@NotNull Long articleId, Pageable pageable);
    Page<CommentDto> listAllComments(String keyword, Integer status, Pageable pageable);
    
    /**
     * 审核评论
     * @param id 评论 ID
     * @param status 目标状态
     */
    void auditComment(Long id, Integer status);

    /**
     * 批量审核评论
     */
    void batchAudit(List<Long> ids, Integer status);

    /**
     * 批量删除评论
     */
    void batchDelete(List<Long> ids);

    /**
     * 批量回复评论
     */
    void batchReply(List<Long> ids, String content);
}
