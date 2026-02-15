package club.freecity.cms.controller.admin;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.dto.CategoryDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import club.freecity.cms.service.CategoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.CATEGORY_CREATE, message = "'创建分类: ' + #categoryDto.name", logArgs = true)
    public Result<CategoryDto> saveCategory(@Validated(CreateGroup.class) @RequestBody @NotNull CategoryDto categoryDto) {
        return Result.success(categoryService.saveCategory(categoryDto));
    }

    @PutMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.CATEGORY_UPDATE, message = "'更新分类 ID: ' + #categoryDto.id", logArgs = true)
    public Result<CategoryDto> updateCategory(@Validated(UpdateGroup.class) @RequestBody @NotNull CategoryDto categoryDto) {
        return Result.success(categoryService.saveCategory(categoryDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.CATEGORY_DELETE, message = "'删除分类 ID: ' + #id")
    public Result<Void> deleteCategory(@PathVariable @NotNull Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<CategoryDto> getCategoryById(@PathVariable @NotNull Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<List<CategoryDto>> listAllCategories() {
        return Result.success(categoryService.listAllCategories());
    }

    @GetMapping("/tree")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<List<CategoryDto>> listCategoryTree() {
        return Result.success(categoryService.listCategoryTree());
    }

    @GetMapping("/parent/{parentId}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<List<CategoryDto>> listByParentId(@PathVariable @NotNull Long parentId) {
        return Result.success(categoryService.listByParentId(parentId));
    }

    @GetMapping("/name/{name}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<CategoryDto> getCategoryByName(@PathVariable @NotBlank String name) {
        return Result.success(categoryService.getCategoryByName(name));
    }
}
