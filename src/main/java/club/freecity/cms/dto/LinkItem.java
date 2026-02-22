package club.freecity.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 友情链接/网址收藏子项模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkItem {
    /**
     * 链接名称
     */
    private String name;

    /**
     * 链接描述
     */
    private String desc;

    /**
     * 链接地址
     */
    private String url;
}
