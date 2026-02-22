package club.freecity.cms.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全审计日志实体
 */
@Data
@Entity
@Table(name = "security_log")
public class SecurityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 租户ID
     */
    @Column(nullable = false)
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    @Column(length = 50)
    private String username;

    /**
     * 动作名称
     */
    @Column(nullable = false, length = 100)
    private String action;

    /**
     * 状态 (SUCCESS/FAILURE)
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * IP地址
     */
    @Column(length = 50)
    private String ip;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 设备信息
     */
    private String device;

    /**
     * 浏览器信息
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 详情消息（包含可能的请求参数或响应数据）
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
    }
}
