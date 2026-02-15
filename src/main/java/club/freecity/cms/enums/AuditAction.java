package club.freecity.cms.enums;

import lombok.Getter;

/**
 * 安全审计操作行为枚举
 */
@Getter
public enum AuditAction {
    /**
     * 认证相关
     */
    AUTH_LOGIN("用户登录"),
    AUTH_REFRESH("令牌刷新"),

    /**
     * 用户管理
     */
    USER_CREATE("创建用户"),
    USER_UPDATE("更新用户"),
    USER_DELETE("删除用户"),
    USER_PROFILE_UPDATE("更新个人资料"),
    USER_PASSWORD_UPDATE("修改密码"),
    USER_SELF_DELETE("注销账户"),

    /**
     * 租户管理
     */
    TENANT_CREATE("创建租户"),
    TENANT_UPDATE("更新租户"),
    TENANT_DELETE("删除租户"),
    TENANT_CONFIG_UPDATE("更新租户配置"),

    /**
     * 套餐管理
     */
    PACKAGE_CREATE("创建套餐"),
    PACKAGE_UPDATE("更新套餐"),
    PACKAGE_DELETE("删除套餐"),

    /**
     * 内容管理 - 文章
     */
    ARTICLE_CREATE("发布文章"),
    ARTICLE_UPDATE("编辑文章"),
    ARTICLE_DELETE("删除文章"),

    /**
     * 内容管理 - 分类
     */
    CATEGORY_CREATE("创建分类"),
    CATEGORY_UPDATE("更新分类"),
    CATEGORY_DELETE("删除分类"),

    /**
     * 内容管理 - 标签
     */
    TAG_CREATE("创建标签"),
    TAG_UPDATE("更新标签"),
    TAG_DELETE("删除标签"),

    /**
     * 内容管理 - 评论
     */
    COMMENT_CREATE("提交评论"),
    COMMENT_UPDATE("更新评论"),
    COMMENT_DELETE("删除评论"),

    /**
     * 文件管理
     */
    FILE_UPLOAD("上传文件"),
    FILE_DELETE("删除文件"),
    FILE_RESTORE("还原文件"),
    FILE_TRASH_EMPTY("清空回收站");

    private final String value;

    AuditAction(String value) {
        this.value = value;
    }
}
