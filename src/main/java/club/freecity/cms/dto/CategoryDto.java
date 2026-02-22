package club.freecity.cms.dto;

import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CategoryDto {
    @Null(groups = CreateGroup.class, message = "新增时分类ID必须为空")
    @NotNull(groups = UpdateGroup.class, message = "更新时分类ID不能为空")
    private Long id;

    private Long parentId = 0L;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String description;
    private Integer articleCount;
    private Integer weight;
    private Integer level = 1;
    private LocalDateTime createTime;
    private List<CategoryDto> children;
}
