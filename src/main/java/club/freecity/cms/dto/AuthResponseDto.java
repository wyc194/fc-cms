package club.freecity.cms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private String role;
        private Long tenantId;
    }
}
