package club.freecity.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "media_asset")
public class MediaAsset extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "type")
    private String type;

    @Column(name = "size")
    private Long size;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "delete_time")
    private LocalDateTime deleteTime;
}
