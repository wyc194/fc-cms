package club.freecity.cms.repository;

import club.freecity.cms.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {
    Page<Article> findByPublishedTrue(Pageable pageable);
    Page<Article> findByCategoryIdIn(List<Long> categoryIds, Pageable pageable);
    Page<Article> findByCategoryIdInAndPublishedTrue(List<Long> categoryIds, Pageable pageable);
    Page<Article> findByTagsId(Long tagId, Pageable pageable);
    Page<Article> findByTagsIdAndPublishedTrue(Long tagId, Pageable pageable);
    long countByPublishedTrue();

    @Query("SELECT SUBSTRING(CAST(a.createTime AS string), 1, 4) AS year, COUNT(a) FROM Article a WHERE a.published = true GROUP BY year ORDER BY year DESC")
    List<Object[]> countArticlesByYear();

    @Query("SELECT a FROM Article a WHERE a.published = true AND SUBSTRING(CAST(a.createTime AS string), 1, 4) = :year")
    Page<Article> findByPublishedTrueAndYear(String year, Pageable pageable);

    Optional<Article> findFirstByPublishedTrueOrderByUpdateTimeDesc();

    /**
     * 获取相关推荐文章（同分类下的最新3篇，排除当前文章）
     */
    List<Article> findTop3ByPublishedTrueAndCategoryIdAndIdNotOrderByCreateTimeDesc(Long categoryId, Long id);

    /**
     * 获取上一篇文章：创建时间小于当前文章且已发布，按创建时间降序取第一个
     */
    Optional<Article> findFirstByPublishedTrueAndCreateTimeLessThanOrderByCreateTimeDesc(java.time.LocalDateTime createTime);

    /**
     * 获取下一篇文章：创建时间大于当前文章且已发布，按创建时间升序取第一个
     */
    Optional<Article> findFirstByPublishedTrueAndCreateTimeGreaterThanOrderByCreateTimeAsc(java.time.LocalDateTime createTime);

    /**
     * 基础搜索：根据标题或摘要模糊匹配（已发布文章）
     */
    @Query("SELECT a FROM Article a WHERE a.published = true AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword%)")
    Page<Article> searchArticles(String keyword, Pageable pageable);

    @Query("SELECT SUM(a.viewCount) FROM Article a")
    Long sumViewCount();

    @Modifying
    @Query("UPDATE Article a SET a.commentCount = COALESCE(a.commentCount, 0) + :delta WHERE a.id = :id")
    void incrementCommentCount(Long id, int delta);
}
