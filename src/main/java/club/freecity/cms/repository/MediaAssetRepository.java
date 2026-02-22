package club.freecity.cms.repository;

import club.freecity.cms.entity.MediaAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long>, JpaSpecificationExecutor<MediaAsset> {
    Page<MediaAsset> findAll(Pageable pageable);
    Optional<MediaAsset> findByUrl(String url);
}
