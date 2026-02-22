package club.freecity.cms.dto;

import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    
    @NotNull(groups = UpdateGroup.class)
    private Long id;

    @NotBlank(groups = CreateGroup.class)
    private String username;

    @NotBlank(groups = CreateGroup.class)
    private String password;

    private String nickname;

    @Email
    private String email;

    private String avatar;

    private String bio;

    @NotBlank(groups = CreateGroup.class)
    private String role;

    private String status;

    private Long tenantId;

    private String tenantCode;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
