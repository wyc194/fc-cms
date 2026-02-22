package club.freecity.cms.support.jpa;

import club.freecity.cms.common.TenantContext;
import club.freecity.cms.entity.BaseTenantEntity;
import jakarta.persistence.PrePersist;
import org.springframework.stereotype.Component;

/**
 * 多租户实体生命周期监听器
 * 负责在持久化前自动填充 tenantId 字段
 */
@Component
public class BaseTenantEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof BaseTenantEntity tenantEntity) {
            // 设置租户ID
            if (tenantEntity.getTenantId() == null) {
                Long tenantId = TenantContext.getCurrentTenantId();
                if (tenantId != null) {
                    tenantEntity.setTenantId(tenantId);
                }
            }
        }
    }
}
