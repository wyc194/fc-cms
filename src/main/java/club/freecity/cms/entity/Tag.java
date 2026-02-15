package club.freecity.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tag", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_name_tenant", columnNames = {"name", "tenant_id"})
})
public class Tag extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "article_count")
    private Integer articleCount = 0;
}