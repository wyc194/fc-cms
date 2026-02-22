package club.freecity.cms.repository;

import club.freecity.cms.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {
    @EntityGraph(attributePaths = {"replies"})
    Page<Comment> findByArticleIdAndParentIsNullAndStatus(Long articleId, Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {"replies"})
    Page<Comment> findByArticleIdAndParentIsNull(Long articleId, Pageable pageable);

    @Modifying
    @Query("UPDATE Comment c SET c.status = :status WHERE c.id IN :ids")
    void updateStatusByIds(List<Long> ids, Integer status);
}
