package club.freecity.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "package")
public class Package extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // 套餐编码，如 'FREE', 'PRO'

    @Column(nullable = false)
    private String name; // 套餐名称

    private String description; // 套餐描述

    @Column(columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private Double price = 0.00; // 月价格

    @Column(name = "max_articles")
    private Integer maxArticles = 100; // 最大文章数

    @Column(name = "max_storage")
    private Long maxStorage = 1024L; // 最大存储空间 (MB)

    @Column(name = "custom_domain_enabled")
    private Boolean customDomainEnabled = false; // 是否支持自定义域名

    @Column(name = "advanced_stats_enabled")
    private Boolean advancedStatsEnabled = false; // 是否支持高级统计报表
}
