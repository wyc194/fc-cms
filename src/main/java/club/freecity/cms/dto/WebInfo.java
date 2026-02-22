package club.freecity.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网站基本信息与 SEO 配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebInfo {
    private String title;          // 网站标题
    private String description;    // 网站描述
    private String keywords;       // 关键词
    private String authorName;     // 作者名称
    private String favicon;        // Favicon URL
    private String logo;           // Logo URL
    private String indexImg;       // 首页背景图 URL
    private String notFoundImg;    // 通用 404 图 URL
    private String loadingImg;     // Loading 图 URL
    private String wxRewardImg;    // 微信收款码 URL
    private String aliRewardImg;   // 支付宝收款码 URL
    private String announcement;   // 站点公告
    private String aboutMe;        // 关于我 (支持 HTML)
    private String copyright;      // 版权信息
    private String icp;            // ICP 备案号
    private String theme;          // 主题模式 (light/dark/auto)
    private Boolean generateSitemap; // 是否自动生成站点地图
}
