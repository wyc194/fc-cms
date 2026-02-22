package club.freecity.cms.service;

import club.freecity.cms.dto.CategoryDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface CategoryService {
    CategoryDto saveCategory(@NotNull CategoryDto categoryDto);
    void deleteCategory(@NotNull Long id);
    CategoryDto getCategoryById(@NotNull Long id);
    List<CategoryDto> listAllCategories();
    List<CategoryDto> listCategoryTree();
    List<CategoryDto> listByParentId(@NotNull Long parentId);
    List<Long> getAllDescendantIds(@NotNull Long parentId);
    CategoryDto getCategoryByName(@NotBlank String name);
    long countCategories();
    void increaseArticleCount(Long id);
    void decreaseArticleCount(Long id);
}
