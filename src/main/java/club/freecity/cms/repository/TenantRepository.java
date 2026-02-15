package club.freecity.cms.repository;

import club.freecity.cms.config.CacheConfig;
import club.freecity.cms.entity.Tenant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    @Cacheable(value = CacheConfig.CACHE_TENANTS, key = "#code")
    @EntityGraph(attributePaths = {"packageInfo"})
    Optional<Tenant> findByCode(String code);
    
    long countByStatus(String status);
}
