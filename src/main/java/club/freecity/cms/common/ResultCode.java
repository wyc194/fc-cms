package club.freecity.cms.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数校验失败"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    BUSINESS_ERROR(1000, "业务处理失败"),
    
    // 认证相关 (401x)
    USER_ACCOUNT_LOCKED(4011, "账号已被锁定"),
    USER_BAD_CREDENTIALS(4012, "用户名或密码错误"),
    TOKEN_EXPIRED(4013, "认证已过期，请重新登录"),
    TOKEN_INVALID(4014, "无效的认证令牌");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
