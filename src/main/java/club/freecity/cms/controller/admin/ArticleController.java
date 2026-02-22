package club.freecity.cms.controller.admin;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.dto.ArticleDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import club.freecity.cms.service.ArticleService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
@Validated
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.ARTICLE_CREATE, message = "'发布新文章: ' + #articleDto.title", logArgs = true)
    public Result<ArticleDto> saveArticle(@Validated(CreateGroup.class) @RequestBody @NotNull ArticleDto articleDto) {
        return Result.success(articleService.saveArticle(articleDto));
    }

    @PutMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.ARTICLE_UPDATE, message = "'编辑文章 ID: ' + #articleDto.id", logArgs = true)
    public Result<ArticleDto> updateArticle(@Validated(UpdateGroup.class) @RequestBody @NotNull ArticleDto articleDto) {
        return Result.success(articleService.updateArticle(articleDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.ARTICLE_DELETE, message = "'删除文章 ID: ' + #id")
    public Result<Void> deleteArticle(@PathVariable @NotNull Long id) {
        articleService.deleteArticle(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<ArticleDto> getArticleById(@PathVariable @NotNull Long id) {
        return Result.success(articleService.getArticleById(id));
    }

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<Page<ArticleDto>> listArticles(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean published,
            @PageableDefault(sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return Result.success(articleService.listAllArticles(title, published, pageable));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<Page<ArticleDto>> listArticlesByCategory(@PathVariable @NotNull Long categoryId, 
                                                           @PageableDefault(sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return Result.success(articleService.listArticlesByCategory(categoryId, pageable));
    }

    @GetMapping("/tag/{tagId}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<Page<ArticleDto>> listArticlesByTag(@PathVariable @NotNull Long tagId, 
                                                      @PageableDefault(sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return Result.success(articleService.listArticlesByTag(tagId, pageable));
    }

    @PatchMapping("/{id}/view")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<Void> incrementViewCount(@PathVariable @NotNull Long id) {
        articleService.incrementViewCount(id);
        return Result.success();
    }
}
