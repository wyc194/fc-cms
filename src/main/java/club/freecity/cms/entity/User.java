package club.freecity.cms.entity;

import club.freecity.cms.enums.UserRole;
import club.freecity.cms.enums.UserStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "`user`", uniqueConstraints = {
        @UniqueConstraint(name = "uk_username_tenant", columnNames = {"username", "tenant_id"})
})
public class User extends BaseTenantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nickname;

    private String email;

    private String avatar;

    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // SUPER_ADMIN, TENANT_ADMIN, EDITOR, VIEWER

    @Column(nullable = false)
    private String status = UserStatus.ENABLED.getValue(); // ENABLED, DISABLED

    @Column(nullable = false)
    private Integer loginFailCount = 0;

    private LocalDateTime lockoutTime;

    private LocalDateTime passwordUpdateTime;
}
