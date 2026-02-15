package club.freecity.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社交平台与联系方式信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialInfo {
    private String github;
    private String email;
    private String wechat;
    private String qq;
}
