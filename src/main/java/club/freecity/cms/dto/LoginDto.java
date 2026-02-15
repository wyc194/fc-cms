package club.freecity.cms.dto;

import lombok.Data;

@Data
public class LoginDto {
    private String username;
    private String password;
    private String tenantCode; // 租户编码，可选
}
