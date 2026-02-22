package club.freecity.cms.dto;

import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ArticleDto {
    @Null(groups = CreateGroup.class, message = "新增时文章ID必须为空")
    @NotNull(groups = UpdateGroup.class, message = "更新时文章ID不能为空")
    private Long id;

    @NotBlank(message = "文章标题不能为空")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private String renderedContent;

    private String summary;
    private String thumbnail;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Boolean published;
    private Boolean top;
    private CategoryDto category;
    private Set<TagDto> tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
