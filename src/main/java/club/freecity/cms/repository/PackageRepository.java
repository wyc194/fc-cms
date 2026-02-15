package club.freecity.cms.repository;

import club.freecity.cms.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PackageRepository extends JpaRepository<Package, Long> {
    Optional<Package> findByCode(String code);
}
