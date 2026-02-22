package club.freecity.cms.service.impl;

import club.freecity.cms.dto.ArticleDto;
import club.freecity.cms.dto.ArchiveDto;
import club.freecity.cms.dto.TagDto;
import club.freecity.cms.entity.Article;
import club.freecity.cms.entity.Tag;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.converter.BeanConverter;
import club.freecity.cms.repository.ArticleRepository;
import club.freecity.cms.repository.CategoryRepository;
import club.freecity.cms.repository.TagRepository;
import club.freecity.cms.service.ArticleService;
import club.freecity.cms.service.CategoryService;
import club.freecity.cms.service.TagService;
import club.freecity.cms.util.MarkdownUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final TagService tagService;

    @Override
    @Transactional
    public ArticleDto saveArticle(ArticleDto articleDto) {
        // 安全清洗标题和摘要
        if (articleDto.getTitle() != null) {
            articleDto.setTitle(MarkdownUtils.sanitizeText(articleDto.getTitle()));
        }
        if (articleDto.getSummary() != null) {
            articleDto.setSummary(MarkdownUtils.sanitizeText(articleDto.getSummary()));
        }
        
        Article article = BeanConverter.toEntity(articleDto);
        
        // 处理分类关联
        if (articleDto.getCategory() != null && articleDto.getCategory().getId() != null) {
            categoryRepository.findById(articleDto.getCategory().getId()).ifPresent(article::setCategory);
        } else {
            article.setCategory(null);
        }

    // 处理标签关联
    if (articleDto.getTags() != null && !articleDto.getTags().isEmpty()) {
        Set<Tag> tags = new HashSet<>();
        for (TagDto tagDto : articleDto.getTags()) {
            Tag tag = null;
            if (tagDto.getId() != null) {
                tag = tagRepository.findById(tagDto.getId()).orElse(null);
            } else if (tagDto.getName() != null && !tagDto.getName().isBlank()) {
                tag = tagRepository.findByName(tagDto.getName()).orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(tagDto.getName());
                    newTag.setArticleCount(0);
                    return tagRepository.save(newTag);
                });
            }
            if (tag != null) {
                tags.add(tag);
            }
        }
        article.setTags(tags);
    } else {
        article.setTags(new HashSet<>());
    }

        if (article.getViewCount() == null) article.setViewCount(0);
        if (article.getCommentCount() == null) article.setCommentCount(0);
        if (article.getLikeCount() == null) article.setLikeCount(0);
        if (article.getPublished() == null) article.setPublished(false);
        if (article.getTop() == null) article.setTop(false);

        Article savedArticle = articleRepository.save(article);
        
        // 更新分类计数
        if (savedArticle.getCategory() != null) {
            categoryService.increaseArticleCount(savedArticle.getCategory().getId());
        }
        // 更新标签计数
        if (savedArticle.getTags() != null) {
            savedArticle.getTags().forEach(tag -> tagService.increaseArticleCount(tag.getId()));
        }

        return BeanConverter.toDto(savedArticle);
    }

    @Override
    @Transactional
    public ArticleDto updateArticle(ArticleDto articleDto) {
        if (articleDto.getId() == null) {
            throw new BusinessException("文章ID不能为空");
        }
        
        Article article = articleRepository.findById(articleDto.getId())
                .orElseThrow(() -> new BusinessException("文章不存在"));

        // 安全清洗标题和摘要
        if (articleDto.getTitle() != null) {
            articleDto.setTitle(MarkdownUtils.sanitizeText(articleDto.getTitle()));
        }
        if (articleDto.getSummary() != null) {
            articleDto.setSummary(MarkdownUtils.sanitizeText(articleDto.getSummary()));
        }
        
        // 处理分类变更计数
        Long oldCategoryId = article.getCategory() != null ? article.getCategory().getId() : null;
        Long newCategoryId = articleDto.getCategory() != null ? articleDto.getCategory().getId() : null;
        
        if (oldCategoryId != null && !oldCategoryId.equals(newCategoryId)) {
            categoryService.decreaseArticleCount(oldCategoryId);
        }
        if (newCategoryId != null && !newCategoryId.equals(oldCategoryId)) {
            categoryService.increaseArticleCount(newCategoryId);
        }

        // 处理标签变更计数
        Set<Long> oldTagIds = article.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
        Set<Long> newTagIds = articleDto.getTags() != null ? 
                articleDto.getTags().stream().map(TagDto::getId).filter(id -> id != null).collect(Collectors.toSet()) : 
                new HashSet<>();
        
        // 减少被移除标签的计数
        oldTagIds.stream().filter(id -> !newTagIds.contains(id)).forEach(tagService::decreaseArticleCount);
        // 增加新添加标签的计数
        newTagIds.stream().filter(id -> !oldTagIds.contains(id)).forEach(tagService::increaseArticleCount);

        BeanConverter.updateEntity(article, articleDto);
        article.setUpdateTime(LocalDateTime.now());

        // 更新分类关联
        if (newCategoryId != null) {
            categoryRepository.findById(newCategoryId).ifPresent(article::setCategory);
        } else {
            article.setCategory(null);
        }

        // 更新标签关联
        if (articleDto.getTags() != null) {
            Set<Tag> tags = new HashSet<>();
            for (TagDto tagDto : articleDto.getTags()) {
                Tag tag = null;
                if (tagDto.getId() != null) {
                    tag = tagRepository.findById(tagDto.getId()).orElse(null);
                } else if (tagDto.getName() != null && !tagDto.getName().isBlank()) {
                    tag = tagRepository.findByName(tagDto.getName()).orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(tagDto.getName());
                        newTag.setArticleCount(0);
                        return tagRepository.save(newTag);
                    });
                }
                if (tag != null) {
                    tags.add(tag);
                }
            }
            article.setTags(tags);
        }

        return BeanConverter.toDto(articleRepository.save(article));
    }

    @Override
    @Transactional
    public void deleteArticle(Long id) {
        articleRepository.findById(id).ifPresent(article -> {
            // 减少分类计数
            if (article.getCategory() != null) {
                categoryService.decreaseArticleCount(article.getCategory().getId());
            }
            // 减少标签计数
            if (article.getTags() != null) {
                article.getTags().forEach(tag -> tagService.decreaseArticleCount(tag.getId()));
            }
            articleRepository.delete(article);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleDto getArticleById(Long id) {
        return articleRepository.findById(id)
                .map(article -> {
                    ArticleDto dto = BeanConverter.toDto(article);
                    if (dto.getContent() != null) {
                        dto.setRenderedContent(MarkdownUtils.renderHtml(dto.getContent()));
                    }
                    return dto;
                })
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDto> listAllArticles(String title, Boolean published, Pageable pageable) {
        Specification<Article> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (published != null) {
                predicates.add(cb.equal(root.get("published"), published));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return articleRepository.findAll(spec, pageable)
                .map(BeanConverter::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDto> listPublishedArticles(Pageable pageable) {
        return articleRepository.findByPublishedTrue(pageable)
                .map(BeanConverter::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDto> listArticlesByCategory(Long categoryId, Pageable pageable) {
        List<Long> categoryIds = categoryService.getAllDescendantIds(categoryId);
        return articleRepository.findByCategoryIdIn(categoryIds, pageable)
                .map(BeanConverter::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDto> listPublishedArticlesByCategory(Long categoryId, Pageable pageable) {
        List<Long> categoryIds = categoryService.getAllDescendantIds(categoryId);
        return articleRepository.findByCategoryIdInAndPublishedTrue(categoryIds, pageable)
                .map(BeanConverter::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDto> listArticlesByTag(Long tagId, Pageable pageable) {
        return articleRepository.findByTagsId(tagId, pageable)
                .map(BeanConverter::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDto> listPublishedArticlesByTag(Long tagId, Pageable pageable) {
        return articleRepository.findByTagsIdAndPublishedTrue(tagId, pageable)
                .map(BeanConverter::toDto);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        articleRepository.findById(id).ifPresent(article -> {
            Integer currentViewCount = article.getViewCount();
            article.setViewCount(currentViewCount == null ? 1 : currentViewCount + 1);
            articleRepository.save(article);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long countPublishedArticles() {
        return articleRepository.countByPublishedTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchiveDto> listArchives() {
        return articleRepository.countArticlesByYear().stream()
                .map(obj -> new ArchiveDto((String) obj[0], (Long) obj[1]))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDto> listPublishedArticlesByYear(String year, Pageable pageable) {
        return articleRepository.findByPublishedTrueAndYear(year, pageable)
                .map(BeanConverter::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getLastUpdateTime() {
        return articleRepository.findFirstByPublishedTrueOrderByUpdateTimeDesc()
                .map(Article::getUpdateTime)
                .orElse(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleDto> listRelatedArticles(Long articleId, Long categoryId) {
        if (categoryId == null) {
            return java.util.Collections.emptyList();
        }
        return articleRepository.findTop3ByPublishedTrueAndCategoryIdAndIdNotOrderByCreateTimeDesc(categoryId, articleId)
                .stream()
                .map(BeanConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleDto getPreviousArticle(LocalDateTime createTime) {
        return articleRepository.findFirstByPublishedTrueAndCreateTimeLessThanOrderByCreateTimeDesc(createTime)
                .map(BeanConverter::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleDto getNextArticle(LocalDateTime createTime) {
        return articleRepository.findFirstByPublishedTrueAndCreateTimeGreaterThanOrderByCreateTimeAsc(createTime)
                .map(BeanConverter::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticles(String keyword, Pageable pageable) {
        return articleRepository.searchArticles(keyword, pageable)
                .map(BeanConverter::toDto);
    }
}
