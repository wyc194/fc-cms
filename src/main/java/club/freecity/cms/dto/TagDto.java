package club.freecity.cms.dto;

import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TagDto {
    @Null(groups = CreateGroup.class, message = "新增时标签ID必须为空")
    @NotNull(groups = UpdateGroup.class, message = "更新时标签ID不能为空")
    private Long id;

    @NotBlank(message = "标签名称不能为空")
    private String name;

    private Integer articleCount;
    private LocalDateTime createTime;
}
