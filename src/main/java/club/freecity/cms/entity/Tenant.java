package club.freecity.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tenant")
public class Tenant extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // 租户编码，用于识别，如 'admin', 'user1'

    @Column(nullable = false)
    private String name; // 租户名称

    private String status; // ACTIVE, DISABLED, PENDING

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private Package packageInfo;

    @Column(columnDefinition = "TEXT")
    private String webInfo; // 网站信息与SEO配置(JSON)

    @Column(columnDefinition = "TEXT")
    private String socialInfo; // 社交与联系方式(JSON)

    @Column(columnDefinition = "TEXT")
    private String links; // 友情链接/网址收藏(JSON格式)

    @Column(columnDefinition = "TEXT")
    private String customCode; // 自定义代码注入(JSON)

    private LocalDateTime expireTime;
}
