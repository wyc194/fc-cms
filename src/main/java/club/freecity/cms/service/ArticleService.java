package club.freecity.cms.service;

import club.freecity.cms.dto.ArticleDto;
import club.freecity.cms.dto.ArchiveDto;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface ArticleService {
    ArticleDto saveArticle(@NotNull ArticleDto articleDto);
    ArticleDto updateArticle(@NotNull ArticleDto articleDto);
    void deleteArticle(@NotNull Long id);
    ArticleDto getArticleById(@NotNull Long id);
    Page<ArticleDto> listAllArticles(String title, Boolean published, Pageable pageable);
    Page<ArticleDto> listPublishedArticles(Pageable pageable);
    Page<ArticleDto> listArticlesByCategory(@NotNull Long categoryId, Pageable pageable);
    Page<ArticleDto> listPublishedArticlesByCategory(@NotNull Long categoryId, Pageable pageable);
    Page<ArticleDto> listArticlesByTag(@NotNull Long tagId, Pageable pageable);
    Page<ArticleDto> listPublishedArticlesByTag(@NotNull Long tagId, Pageable pageable);
    void incrementViewCount(@NotNull Long id);
    long countPublishedArticles();
    List<ArchiveDto> listArchives();
    Page<ArticleDto> listPublishedArticlesByYear(String year, Pageable pageable);
    LocalDateTime getLastUpdateTime();

    /**
     * 获取相关文章推荐
     */
    List<ArticleDto> listRelatedArticles(Long articleId, Long categoryId);

    /**
     * 获取上一篇文章
     */
    ArticleDto getPreviousArticle(@NotNull LocalDateTime createTime);

    /**
     * 获取下一篇文章
     */
    ArticleDto getNextArticle(@NotNull LocalDateTime createTime);

    /**
     * 搜索文章
     */
    Page<ArticleDto> searchArticles(String keyword, Pageable pageable);
}
