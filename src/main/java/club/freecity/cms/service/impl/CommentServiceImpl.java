package club.freecity.cms.service.impl;

import club.freecity.cms.dto.CommentDto;
import club.freecity.cms.entity.Comment;
import club.freecity.cms.converter.BeanConverter;
import club.freecity.cms.enums.CommentStatus;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.repository.CommentRepository;
import club.freecity.cms.repository.ArticleRepository;
import club.freecity.cms.service.CommentService;
import club.freecity.cms.service.VerificationService;
import club.freecity.cms.util.MarkdownUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final VerificationService verificationService;

    @Override
    @Transactional
    public CommentDto saveComment(CommentDto commentDto) {
        // 1. 验证码校验 (仅限非管理员提交)
        if (commentDto.getAdminReply() == null || !commentDto.getAdminReply()) {
            if (!verificationService.verifyCode(commentDto.getEmail(), commentDto.getVerificationCode())) {
                throw new BusinessException(400, "验证码错误或已失效");
            }
            // 读者提交，默认待审核
            commentDto.setStatus(CommentStatus.PENDING.getValue());
        } else {
            // 管理员提交，直接发布
            commentDto.setStatus(CommentStatus.PUBLISHED.getValue());
        }

        // 安全清洗评论内容和昵称
        if (commentDto.getContent() != null) {
            commentDto.setContent(MarkdownUtils.sanitize(commentDto.getContent()));
        }
        if (commentDto.getNickname() != null) {
            commentDto.setNickname(MarkdownUtils.sanitizeText(commentDto.getNickname()));
        }
        
        Comment comment = BeanConverter.toEntity(commentDto);
        
        // 处理父评论
        if (commentDto.getParentId() != null) {
            commentRepository.findById(commentDto.getParentId()).ifPresent(comment::setParent);
        }
        
        Comment savedComment = commentRepository.save(comment);

        // 只有在发布状态下才更新文章评论数
        if (CommentStatus.PUBLISHED.getValue() == savedComment.getStatus()) {
            updateArticleCommentCount(savedComment.getArticleId(), 1);
        }
        
        return BeanConverter.toDto(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        commentRepository.findById(id).ifPresent(comment -> {
            // 如果删除的是已发布的评论，需要减去评论数
            if (CommentStatus.PUBLISHED.getValue() == comment.getStatus()) {
                updateArticleCommentCount(comment.getArticleId(), -1);
            }
            commentRepository.delete(comment);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> listCommentsByArticle(Long articleId, Pageable pageable) {
        // 读者端只查询已发布的评论
        return commentRepository.findByArticleIdAndParentIsNullAndStatus(
                articleId, CommentStatus.PUBLISHED.getValue(), pageable).map(BeanConverter::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> listAllCommentsByArticle(Long articleId, Pageable pageable) {
        // 管理端查询所有评论
        return commentRepository.findByArticleIdAndParentIsNull(articleId, pageable).map(BeanConverter::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> listAllComments(String keyword, Integer status, Pageable pageable) {
        // 管理端查询租户下所有评论，支持关键字和状态筛选
        return commentRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (StringUtils.hasText(keyword)) {
                String likeKeyword = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("nickname"), likeKeyword),
                    cb.like(root.get("email"), likeKeyword),
                    cb.like(root.get("content"), likeKeyword)
                ));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable).map(BeanConverter::toDto);
    }

    @Override
    @Transactional
    public void auditComment(Long id, Integer status) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));
        
        Integer oldStatus = comment.getStatus();
        comment.setStatus(status);
        commentRepository.save(comment);

        // 状态从非 PUBLISHED 变为 PUBLISHED，增加文章评论数
        if (oldStatus != CommentStatus.PUBLISHED.getValue() && status == CommentStatus.PUBLISHED.getValue()) {
            updateArticleCommentCount(comment.getArticleId(), 1);
        }
        // 状态从 PUBLISHED 变为非 PUBLISHED，减少文章评论数
        else if (oldStatus == CommentStatus.PUBLISHED.getValue() && status != CommentStatus.PUBLISHED.getValue()) {
            updateArticleCommentCount(comment.getArticleId(), -1);
        }
    }

    @Override
    @Transactional
    public void batchAudit(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Comment> comments = commentRepository.findAllById(ids);
        if (comments.isEmpty()) {
            return;
        }

        // 统计各文章评论数变化
        Map<Long, Integer> articleDeltas = comments.stream()
                .filter(c -> !c.getStatus().equals(status)) // 只处理状态有变化的
                .collect(Collectors.groupingBy(
                        Comment::getArticleId,
                        Collectors.summingInt(c -> {
                            int delta = 0;
                            // 状态从非 PUBLISHED 变为 PUBLISHED，+1
                            if (!c.getStatus().equals(CommentStatus.PUBLISHED.getValue()) && 
                                status.equals(CommentStatus.PUBLISHED.getValue())) {
                                delta = 1;
                            }
                            // 状态从 PUBLISHED 变为非 PUBLISHED，-1
                            else if (c.getStatus().equals(CommentStatus.PUBLISHED.getValue()) && 
                                     !status.equals(CommentStatus.PUBLISHED.getValue())) {
                                delta = -1;
                            }
                            return delta;
                        })
                ));

        // 批量更新评论状态
        commentRepository.updateStatusByIds(ids, status);

        // 批量更新文章评论数
        articleDeltas.forEach((articleId, delta) -> {
            if (delta != 0) {
                articleRepository.incrementCommentCount(articleId, delta);
            }
        });
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Comment> comments = commentRepository.findAllById(ids);
        if (comments.isEmpty()) {
            return;
        }

        // 统计各文章评论数变化（只有已发布的评论被删除时才需要减1）
        Map<Long, Integer> articleDeltas = comments.stream()
                .filter(c -> c.getStatus().equals(CommentStatus.PUBLISHED.getValue()))
                .collect(Collectors.groupingBy(
                        Comment::getArticleId,
                        Collectors.summingInt(c -> -1)
                ));

        // 批量删除评论
        commentRepository.deleteAllByIdInBatch(ids);

        // 批量更新文章评论数
        articleDeltas.forEach((articleId, delta) -> {
            if (delta != 0) {
                articleRepository.incrementCommentCount(articleId, delta);
            }
        });
    }

    @Override
    @Transactional
    public void batchReply(List<Long> ids, String content) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Comment> parents = commentRepository.findAllById(ids);
        if (parents.isEmpty()) {
            return;
        }

        String sanitizedContent = MarkdownUtils.sanitize(content);
        
        List<Comment> replies = parents.stream().map(parent -> {
            Comment reply = new Comment();
            reply.setArticleId(parent.getArticleId());
            reply.setParent(parent);
            reply.setContent(sanitizedContent);
            reply.setNickname("管理员");
            reply.setEmail("admin@freecity.club"); // 默认管理员邮箱
            reply.setAdminReply(true);
            reply.setStatus(CommentStatus.PUBLISHED.getValue());
            return reply;
        }).collect(Collectors.toList());

        // 批量保存回复
        commentRepository.saveAll(replies);

        // 统计各文章评论数变化（管理员回复默认是已发布，所以每个回复+1）
        Map<Long, Integer> articleDeltas = replies.stream()
                .collect(Collectors.groupingBy(
                        Comment::getArticleId,
                        Collectors.summingInt(c -> 1)
                ));

        // 批量更新文章评论数
        articleDeltas.forEach((articleId, delta) -> {
            articleRepository.incrementCommentCount(articleId, delta);
        });
    }

    private void updateArticleCommentCount(Long articleId, int delta) {
        if (delta != 0) {
            articleRepository.incrementCommentCount(articleId, delta);
        }
    }
}
