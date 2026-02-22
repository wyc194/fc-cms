package club.freecity.cms.dto;

import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {
    
    @NotNull(groups = UpdateGroup.class)
    private Long id;

    @NotBlank(groups = CreateGroup.class)
    private String code;

    @NotBlank(groups = CreateGroup.class)
    private String name;

    private String status;

    private WebInfo webInfo;

    private SocialInfo socialInfo;

    private Map<String, List<LinkItem>> links;

    private CustomCode customCode;

    private Long packageId;
    
    private String packageName;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
