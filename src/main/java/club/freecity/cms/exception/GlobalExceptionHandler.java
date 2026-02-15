package club.freecity.cms.exception;

import club.freecity.cms.common.Result;
import club.freecity.cms.common.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = {"club.freecity.cms.controller.admin", "club.freecity.cms.controller.api"})
public class GlobalExceptionHandler {

    /**
     * 处理 Spring Security 认证异常 (如登录失败、Token失效等)
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<String> handleAuthenticationException(AuthenticationException ex) {
        log.warn("认证失败: {}", ex.getMessage());
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), ex.getMessage());
    }

    /**
     * 处理 Spring Security 权限异常 (如角色不足)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("拒绝访问: {}", ex.getMessage());
        return Result.error(ResultCode.FORBIDDEN.getCode(), "权限不足，拒绝访问");
    }

    /**
     * 处理 DTO 校验异常 (@RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("参数校验失败: {}", errors);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), errors.toString());
    }

    /**
     * 处理方法级参数校验异常 (@PathVariable, @RequestParam)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("参数约束校验失败: {}", ex.getMessage());
        return Result.error(ResultCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleAllExceptions(Exception ex) {
        log.error("系统未知异常: ", ex);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "系统繁忙，请稍后再试");
    }
}
