CREATE DATABASE IF NOT EXISTS `fc_cms` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

-- 设置数据库字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建套餐表
CREATE TABLE IF NOT EXISTS package (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '套餐ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '套餐编码',
    name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    description VARCHAR(500) COMMENT '套餐描述',
    price DECIMAL(10, 2) DEFAULT 0.00 COMMENT '月价格',
    max_articles INT DEFAULT 100 COMMENT '最大文章数',
    max_storage BIGINT DEFAULT 1024 COMMENT '最大存储空间(MB)',
    custom_domain_enabled TINYINT DEFAULT 0 COMMENT '是否支持自定义域名',
    advanced_stats_enabled TINYINT DEFAULT 0 COMMENT '是否支持高级统计报表',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐表';

-- 创建租户表
CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '租户ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '租户编码',
    name VARCHAR(100) NOT NULL COMMENT '租户名称',
    status VARCHAR(20) DEFAULT 'active' COMMENT '租户状态',
    package_id BIGINT COMMENT '关联套餐ID',
    web_info TEXT COMMENT '网站信息与SEO配置(JSON)',
    social_info TEXT COMMENT '社交与联系方式(JSON)',
    links TEXT COMMENT '友情链接/网址收藏(JSON格式)',
    custom_code TEXT COMMENT '自定义代码注入(JSON)',
    expire_time DATETIME COMMENT '到期时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像',
    bio VARCHAR(500) COMMENT '个人简介',
    role VARCHAR(20) NOT NULL COMMENT '角色',
    tenant_id BIGINT NOT NULL COMMENT '所属租户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',
    lockout_time DATETIME COMMENT '锁定截止时间',
    password_update_time DATETIME COMMENT '密码更新时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_tenant_id (tenant_id),
    UNIQUE KEY uk_username_tenant (username, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 创建分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID (0表示顶级分类)',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) COMMENT '分类描述',
    article_count INT DEFAULT 0 COMMENT '文章数量',
    weight INT DEFAULT 0 COMMENT '排序权重',
    level INT DEFAULT 1 COMMENT '层级 (1: 顶级, 2: 二级...)',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_weight (weight),
    INDEX idx_tenant_id (tenant_id),
    UNIQUE KEY uk_name_tenant (name, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- 创建标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    article_count INT DEFAULT 0 COMMENT '文章数量',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_tenant_id (tenant_id),
    UNIQUE KEY uk_name_tenant (name, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签表';

-- 创建文章表
CREATE TABLE IF NOT EXISTS article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文章ID',
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    content LONGTEXT NOT NULL COMMENT '文章内容',
    summary VARCHAR(500) COMMENT '文章摘要',
    thumbnail VARCHAR(255) COMMENT '缩略图URL',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    published TINYINT DEFAULT 0 COMMENT '是否发布',
    top TINYINT DEFAULT 0 COMMENT '是否置顶',
    category_id BIGINT COMMENT '分类ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_category_id (category_id),
    INDEX idx_create_time (create_time),
    INDEX idx_view_count (view_count),
    INDEX idx_published_top (published, top, create_time),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 创建文章标签关联表
CREATE TABLE IF NOT EXISTS article_tag (
    article_id BIGINT NOT NULL COMMENT '文章ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (article_id, tag_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- 创建评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    nickname VARCHAR(50) NOT NULL COMMENT '评论者昵称',
    email VARCHAR(100) NOT NULL COMMENT '评论者邮箱',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT COMMENT '父评论ID',
    ip VARCHAR(45) COMMENT '评论者IP',
    user_agent VARCHAR(500) COMMENT '用户代理',
    is_admin_reply TINYINT DEFAULT 0 COMMENT '是否管理员回复',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '评论状态 (0: PENDING, 1: PUBLISHED, 2: REJECTED)',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_article_id (article_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_create_time (create_time),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 创建媒体资源表
CREATE TABLE IF NOT EXISTS media_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资源ID',
    name VARCHAR(255) NOT NULL COMMENT '文件原始名称',
    url VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    file_path VARCHAR(500) NOT NULL COMMENT '文件物理路径',
    type VARCHAR(100) COMMENT '文件MIME类型',
    size BIGINT COMMENT '文件大小(Bytes)',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否已删除(回收站)',
    delete_time DATETIME COMMENT '删除时间',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_type (type),
    INDEX idx_is_deleted (is_deleted),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒体资源表';

-- 创建安全审计日志表
CREATE TABLE IF NOT EXISTS security_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    action VARCHAR(100) NOT NULL COMMENT '动作',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    ip VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(255) COMMENT '地理位置',
    device VARCHAR(255) COMMENT '设备信息',
    browser VARCHAR(255) COMMENT '浏览器信息',
    os VARCHAR(255) COMMENT '操作系统',
    message TEXT COMMENT '详情消息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_action (action),
    INDEX idx_ip (ip),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全审计日志表';

SET FOREIGN_KEY_CHECKS = 1;