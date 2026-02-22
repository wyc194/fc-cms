package club.freecity.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义代码注入信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomCode {
    private String css; // 自定义 CSS
    private String js;  // 自定义 JS
}
