package club.freecity.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category", uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_name_tenant", columnNames = {"name", "tenant_id"})
})
public class Category extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id", nullable = false)
    private Long parentId = 0L;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "article_count")
    private Integer articleCount = 0;

    @Column(name = "weight")
    private Integer weight = 0;

    @Column(name = "level", nullable = false)
    private Integer level = 1;
}