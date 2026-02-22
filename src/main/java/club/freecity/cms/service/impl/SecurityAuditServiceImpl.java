package club.freecity.cms.service.impl;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.TenantContext;
import club.freecity.cms.dto.AuditContextDto;
import club.freecity.cms.entity.SecurityLog;
import club.freecity.cms.repository.SecurityLogRepository;
import club.freecity.cms.service.SecurityAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 安全审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditServiceImpl implements SecurityAuditService {

    private final SecurityLogRepository securityLogRepository;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "oldPassword", "newPassword", "confirmPassword",
            "token", "accessToken", "refreshToken", "secret", "appSecret"
    ));

    @Override
    @Async("auditLogExecutor")
    public void log(JoinPoint joinPoint, SecurityAudit securityAudit, Object result, Throwable throwable, long executionTime, AuditContextDto context) {
        saveLog(joinPoint, securityAudit, result, throwable, executionTime, context);
    }

    private void saveLog(JoinPoint joinPoint, SecurityAudit securityAudit, Object result, Throwable throwable, long executionTime, AuditContextDto context) {
        try {
            SecurityLog securityLog = new SecurityLog();
            
            // 1. 基础信息
            securityLog.setAction(securityAudit.action().getValue());
            securityLog.setStatus(throwable == null ? SecurityAuditService.STATUS_SUCCESS : SecurityAuditService.STATUS_FAILURE);
            
            // 2. 租户与用户信息 (从 context 获取，如果 context 为空则尝试从上下文获取作为兜底，但主要依赖 context)
            Long tenantId = context.getTenantId();
            if (tenantId == null) {
                tenantId = TenantContext.getCurrentTenantId();
            }
            
            if (tenantId == null) {
                log.warn("SecurityAudit: TenantId is null, skipping log persistence. Action: {}", securityAudit.action());
                return;
            }
            securityLog.setTenantId(tenantId);
            
            securityLog.setUserId(context.getUserId());
            securityLog.setUsername(context.getUsername());

            // 3. 环境信息 (直接从 context 获取)
            securityLog.setIp(context.getIp());
            securityLog.setBrowser(context.getBrowser());
            securityLog.setOs(context.getOs());
            securityLog.setDevice(context.getDevice());
            securityLog.setLocation(context.getLocation());

            // 4. 详情消息处理 (SpEL)
            String message = parseMessage(securityAudit.message(), joinPoint);
            StringBuilder detail = new StringBuilder(message);

            // 5. 请求参数记录
            if (securityAudit.logArgs()) {
                Object[] args = joinPoint.getArgs();
                List<Object> processedArgs = new ArrayList<>();
                if (args != null && args.length > 0) {
                    for (Object arg : args) {
                        if (isLoggableArg(arg)) {
                            processedArgs.add(processLogData(arg, securityAudit));
                        }
                    }
                }
                detail.append("\n[Args]: ").append(objectMapper.writeValueAsString(processedArgs));
            }

            // 6. 响应结果记录
            if (securityAudit.logResponse()) {
                detail.append("\n[Response]: ").append(objectMapper.writeValueAsString(processLogData(result, securityAudit)));
            }

            if (throwable != null) {
                detail.append("\n[Error]: ").append(throwable.getMessage());
            }
            
            detail.append("\n[ExecutionTime]: ").append(executionTime).append("ms");
            securityLog.setMessage(detail.toString());

            securityLogRepository.save(securityLog);
        } catch (Exception e) {
            log.error("保存审计日志失败", e);
        }
    }

    /**
     * 对审计数据进行脱敏和长度/数量限制处理
     */
    private Object processLogData(Object data, SecurityAudit config) {
        if (data == null) return null;

        try {
            // 1. 处理字符串：长度限制
            if (data instanceof String str) {
                if (str.length() > config.maxFieldLength()) {
                    return str.substring(0, config.maxFieldLength()) + "...(truncated, total " + str.length() + ")";
                }
                return str;
            }

            // 2. 处理基本类型及其包装类
            if (isPrimitiveOrWrapper(data.getClass())) {
                return data;
            }

            // 3. 处理集合：数量限制 + 递归处理元素
            if (data instanceof Collection<?> collection) {
                int originalSize = collection.size();
                return collection.stream()
                        .limit(config.maxCollectionSize())
                        .map(item -> processLogData(item, config))
                        .collect(Collectors.toList())
                        .stream().collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                            if (originalSize > config.maxCollectionSize()) {
                                list.add("...(truncated, total " + originalSize + " items)");
                            }
                            return list;
                        }));
            }

            // 4. 处理 Map：脱敏 + 递归处理 Value
            if (data instanceof Map<?, ?> map) {
                return processMap(map, config);
            }

            // 5. 其他 POJO 对象：先转 Map 再处理
            Map<String, Object> map = objectMapper.convertValue(data, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            return processMap(map, config);
        } catch (Exception e) {
            log.warn("处理审计日志数据失败: {}", e.getMessage());
            return data;
        }
    }

    private Map<String, Object> processMap(Map<?, ?> map, SecurityAudit config) {
        Map<String, Object> processedMap = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            // 脱敏判断
            if (SENSITIVE_KEYS.stream().anyMatch(key::equalsIgnoreCase)) {
                processedMap.put(key, "******");
            } else {
                processedMap.put(key, processLogData(value, config));
            }
        }
        return processedMap;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == Double.class || clazz == Float.class || clazz == Long.class ||
                clazz == Integer.class || clazz == Short.class || clazz == Character.class ||
                clazz == Byte.class || clazz == Boolean.class;
    }

    /**
     * 判断参数是否适合记录到日志（排除掉 Request, Response, MultipartFile 等无法序列化或内容过大的对象）
     */
    private boolean isLoggableArg(Object arg) {
        if (arg == null) return false;
        return !(arg instanceof ServletRequest)
                && !(arg instanceof ServletResponse)
                && !(arg instanceof HttpSession)
                && !(arg instanceof MultipartFile)
                && !(arg instanceof BindingResult)
                && !(arg instanceof Model)
                && !(arg instanceof ModelMap);
    }

    private String parseMessage(String template, JoinPoint joinPoint) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] params = discoverer.getParameterNames(method);

            StandardEvaluationContext context = new StandardEvaluationContext();
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    context.setVariable(params[i], args[i]);
                }
            }
            return parser.parseExpression(template).getValue(context, String.class);
        } catch (Exception e) {
            log.warn("解析审计日志消息模板失败: {}", template);
            return template;
        }
    }
}
