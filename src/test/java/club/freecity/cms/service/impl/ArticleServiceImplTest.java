package club.freecity.cms.service.impl;

import club.freecity.cms.dto.ArticleDto;
import club.freecity.cms.entity.Article;
import club.freecity.cms.entity.Category;
import club.freecity.cms.repository.ArticleRepository;
import club.freecity.cms.repository.CategoryRepository;
import club.freecity.cms.repository.TagRepository;
import club.freecity.cms.service.CategoryService;
import club.freecity.cms.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagService tagService;

    @InjectMocks
    private ArticleServiceImpl articleService;

    private Article testArticle;
    private ArticleDto testArticleDto;

    @BeforeEach
    void setUp() {
        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Title");
        testArticle.setContent("Test Content");
        testArticle.setPublished(true);

        testArticleDto = new ArticleDto();
        testArticleDto.setId(1L);
        testArticleDto.setTitle("Test Title");
        testArticleDto.setContent("Test Content");
    }

    @Test
    @DisplayName("根据ID获取文章 - 成功")
    void getArticleById_Success() {
        // Arrange
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // Act
        ArticleDto result = articleService.getArticleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Title");
        verify(articleRepository).findById(1L);
    }

    @Test
    @DisplayName("分页查询所有文章")
    void listAllArticles_Success() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Article> page = new PageImpl<>(Collections.singletonList(testArticle));
        when(articleRepository.findAll(ArgumentMatchers.<Specification<Article>>any(), any(PageRequest.class))).thenReturn(page);

        // Act
        Page<ArticleDto> result = articleService.listAllArticles(null, null, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Title");
    }

    @Test
    @DisplayName("删除文章 - 同时减少分类和标签计数")
    void deleteArticle_Success() {
        // Arrange
        Category category = new Category();
        category.setId(1L);
        testArticle.setCategory(category);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // Act
        articleService.deleteArticle(1L);

        // Assert
        verify(categoryService).decreaseArticleCount(1L);
        verify(articleRepository).delete(testArticle);
    }

    @Test
    @DisplayName("增加阅读量 - 成功")
    void incrementViewCount_Success() {
        // Arrange
        testArticle.setViewCount(5);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // Act
        articleService.incrementViewCount(1L);

        // Assert
        assertThat(testArticle.getViewCount()).isEqualTo(6);
        verify(articleRepository).save(testArticle);
    }

    @Test
    @DisplayName("增加阅读量 - 处理null值")
    void incrementViewCount_HandleNull() {
        // Arrange
        testArticle.setViewCount(null);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // Act
        articleService.incrementViewCount(1L);

        // Assert
        assertThat(testArticle.getViewCount()).isEqualTo(1);
        verify(articleRepository).save(testArticle);
    }
}
