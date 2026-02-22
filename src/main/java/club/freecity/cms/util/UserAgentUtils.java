package club.freecity.cms.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * User-Agent 解析工具类（简单实现）
 */
public class UserAgentUtils {

    public static Map<String, String> parse(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        Map<String, String> info = new HashMap<>();
        
        if (ua == null || ua.isEmpty()) {
            info.put("browser", "Unknown");
            info.put("os", "Unknown");
            info.put("device", "Unknown");
            return info;
        }

        // 简单浏览器解析
        if (ua.contains("Edg")) info.put("browser", "Edge");
        else if (ua.contains("Chrome")) info.put("browser", "Chrome");
        else if (ua.contains("Firefox")) info.put("browser", "Firefox");
        else if (ua.contains("Safari") && !ua.contains("Chrome")) info.put("browser", "Safari");
        else info.put("browser", "Other");

        // 简单操作系统解析
        if (ua.contains("Windows")) info.put("os", "Windows");
        else if (ua.contains("Mac OS")) info.put("os", "macOS");
        else if (ua.contains("Android")) info.put("os", "Android");
        else if (ua.contains("iPhone") || ua.contains("iPad")) info.put("os", "iOS");
        else if (ua.contains("Linux")) info.put("os", "Linux");
        else info.put("os", "Other");

        // 简单设备解析
        if (ua.contains("Mobile")) info.put("device", "Mobile");
        else if (ua.contains("Tablet") || ua.contains("iPad")) info.put("device", "Tablet");
        else info.put("device", "Desktop");

        return info;
    }
}
