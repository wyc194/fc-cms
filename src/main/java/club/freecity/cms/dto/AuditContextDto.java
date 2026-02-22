package club.freecity.cms.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 审计日志环境信息 DTO
 * 用于在主线程捕获请求信息并传递给异步审计线程，防止 Request 对象被回收导致报错
 */
@Data
@Builder
public class AuditContextDto implements Serializable {
    private String ip;
    private String browser;
    private String os;
    private String device;
    private String location;
    private Long tenantId;
    private String username;
    private Long userId;
}
