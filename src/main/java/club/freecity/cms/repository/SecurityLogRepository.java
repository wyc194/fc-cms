package club.freecity.cms.repository;

import club.freecity.cms.entity.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 安全审计日志 Repository
 */
@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {
}
