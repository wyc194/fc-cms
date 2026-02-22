package club.freecity.cms.entity;

import club.freecity.cms.support.jpa.BaseEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(BaseEntityListener.class)
public abstract class BaseEntity {

    @Column(name = "create_time", nullable = false, updatable = false)
    protected LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    protected LocalDateTime updateTime;
}
