package club.freecity.cms.service.impl;

import club.freecity.cms.dto.CategoryDto;
import club.freecity.cms.entity.Category;
import club.freecity.cms.converter.BeanConverter;
import club.freecity.cms.repository.CategoryRepository;
import club.freecity.cms.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDto saveCategory(CategoryDto categoryDto) {
        // 防止循环引用
        if (categoryDto.getId() != null && categoryDto.getParentId() != null && categoryDto.getParentId() != 0) {
            if (categoryDto.getId().equals(categoryDto.getParentId())) {
                throw new IllegalArgumentException("分类的父级不能是自己");
            }
            List<Long> descendantIds = getAllDescendantIds(categoryDto.getId());
            if (descendantIds.contains(categoryDto.getParentId())) {
                throw new IllegalArgumentException("分类的父级不能是自己的子分类");
            }
        }

        Category category;
        if (categoryDto.getId() != null) {
            // 更新：获取现有实体以保留 tenant_id 等不可变字段
            category = categoryRepository.findById(categoryDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
            BeanConverter.updateEntity(category, categoryDto);
        } else {
            // 新增
            category = BeanConverter.toEntity(categoryDto);
        }
        

        // 设置层级
        if (category.getParentId() != null && category.getParentId() != 0) {
            categoryRepository.findById(category.getParentId()).ifPresent(parent -> {
                category.setLevel(parent.getLevel() + 1);
            });
        } else {
            category.setLevel(1);
            category.setParentId(0L);
        }

        return BeanConverter.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        // 如果有子分类，递归删除或抛出异常
        List<Category> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            for (Category child : children) {
                deleteCategory(child.getId());
            }
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(BeanConverter::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> listAllCategories() {
        return categoryRepository.findAllByOrderByWeightDesc().stream()
                .map(BeanConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> listCategoryTree() {
        List<CategoryDto> allCategories = listAllCategories();
        Map<Long, List<CategoryDto>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != 0)
                .collect(Collectors.groupingBy(CategoryDto::getParentId));

        List<CategoryDto> rootCategories = allCategories.stream()
                .filter(c -> c.getParentId() == 0)
                .collect(Collectors.toList());

        for (CategoryDto root : rootCategories) {
            fillChildren(root, childrenMap);
        }

        return rootCategories;
    }

    private int fillChildren(CategoryDto parent, Map<Long, List<CategoryDto>> childrenMap) {
        List<CategoryDto> children = childrenMap.get(parent.getId());
        int totalArticleCount = parent.getArticleCount() != null ? parent.getArticleCount() : 0;
        
        if (children != null) {
            parent.setChildren(children);
            for (CategoryDto child : children) {
                totalArticleCount += fillChildren(child, childrenMap);
            }
        } else {
            parent.setChildren(new ArrayList<>());
        }
        
        // 更新父分类的文章总数（包含子分类）
        parent.setArticleCount(totalArticleCount);
        return totalArticleCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> listByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId).stream()
                .map(BeanConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getAllDescendantIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        ids.add(parentId);
        fetchDescendantIds(parentId, ids);
        return ids;
    }

    private void fetchDescendantIds(Long parentId, List<Long> ids) {
        List<Category> children = categoryRepository.findByParentId(parentId);
        for (Category child : children) {
            ids.add(child.getId());
            fetchDescendantIds(child.getId(), ids);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .map(BeanConverter::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCategories() {
        return categoryRepository.count();
    }

    @Override
    @Transactional
    public void increaseArticleCount(Long id) {
        categoryRepository.findById(id).ifPresent(category -> {
            Integer count = category.getArticleCount();
            category.setArticleCount(count == null ? 1 : count + 1);
            categoryRepository.save(category);
        });
    }

    @Override
    @Transactional
    public void decreaseArticleCount(Long id) {
        categoryRepository.findById(id).ifPresent(category -> {
            if (category.getArticleCount() > 0) {
                category.setArticleCount(category.getArticleCount() - 1);
                categoryRepository.save(category);
            }
        });
    }
}
