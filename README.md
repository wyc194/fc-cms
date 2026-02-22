# FC-CMS 后端服务

基于 Spring Boot 3.3.2 + Java 21 + MySQL 开发的技术博客与内容管理系统后端。

## 技术栈

- **JDK 21**
- **Spring Boot 3.3.2**
- **Spring Data JPA** - 数据库持久化
- **Spring Security** - 身份认证与授权 (JWT)
- **Caffeine** - 本地缓存与限流
- **MySQL 8.0+**
- **Lombok** - 简化开发
- **Jakarta Mail** - 邮件服务

## 核心功能

- **多租户支持**：支持多租户独立内容管理。
- **文章管理**：支持 Markdown 渲染、分类、标签。
- **评论系统**：支持邮件验证码验证、管理员审核、批量回复/删除。
- **安全审计**：操作日志记录。
- **限流保护**：基于 IP 和令牌桶的 API 限流。

## 项目结构
- `config/`: 缓存、安全、CORS 等配置。
- `controller/`: API 接口实现。
- `entity/`: JPA 实体。
- `repository/`: 数据库访问层。
- `service/`: 业务逻辑层。
- `dto/`: 数据传输对象。
- `enums/`: 枚举定义。
- `exception/`: 异常处理。

## 快速开始

### 1. 环境准备
- 安装 JDK 21+
- 安装 MySQL 8.0+
- 安装 Gradle 8.0+

### 2. 数据库配置
1. 创建数据库：
   ```sql
   CREATE DATABASE fc_cms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. 初始化表结构：
   执行 `src/main/resources/schema.sql`。

### 3. 修改配置
编辑 `src/main/resources/application.properties`，建议通过环境变量覆盖以下配置：
- `DB_URL`: 数据库连接 URL
- `DB_USERNAME`: 数据库用户名
- `DB_PASSWORD`: 数据库密码
- `APP_JWT_SECRET`: JWT 密钥 (生产环境必填)
- `APP_CORS_ALLOWED_ORIGINS`: 跨域允许域名
- `APP_MAIL_HOST`/`PORT`/`USERNAME`/`PASSWORD`: 邮件服务器配置 (用于评论验证码)

### 4. 运行
```bash
./gradlew bootRun
```

## Docker 部署

项目根目录下提供了 `docker-compose.yml`，支持一键部署后端服务及 Nginx 反向代理。

### 1. 准备工作
- 确保已安装 Docker 和 Docker Compose。
- 本项目采用“后端驱动全栈编排”模式，`docker-compose.yml` 位于 `fc-cms` 根目录。
- **目录依赖**：确保 `fc-cms` 和 `fc-cms-web` 位于同一级父目录下。
- **前端构建**：请先在 `fc-cms-web` 目录下执行 `npm run build` 生成 `dist` 目录。

### 2. 配置环境变量
建议创建 `.env` 文件来管理敏感信息：
```env
DB_URL=jdbc:mysql://your-db-host:3306/fc_cms...
DB_USERNAME=root
DB_PASSWORD=your_password
APP_JWT_SECRET=your_secret
APP_CORS_ALLOWED_ORIGINS=https://yourdomain.com
APP_MAIL_HOST=smtp.exmail.qq.com
APP_MAIL_PORT=465
APP_MAIL_USERNAME=noreply@yourdomain.com
APP_MAIL_PASSWORD=your_mail_password
```

### 3. 启动服务
```bash
docker-compose up -d
```
该命令会同时启动：
- **fc-cms-app**: 后端 API 服务（内部端口 8080）。
- **fc-cms-nginx**: Nginx 服务，负责静态资源分发及 API 转发。

## 常见问题排查

### 1. 数据库连接失败
- **错误**: `Communications link failure`
- **解决**: 检查 MySQL 服务是否启动，确认 `application.properties` 中的用户名和密码正确。

### 2. 端口被占用
- **错误**: `Web server failed to start. Port 8080 was already in use.`
- **解决**: 修改 `application.properties` 中的 `server.port`，或关闭占用 8080 端口的进程。

### 3. 中文乱码
- **解决**: 确保数据库字符集为 `utf8mb4`，且连接 URL 包含 `characterEncoding=utf8` 参数。

## 许可证

本项目采用 [CC BY-NC 4.0 (知识共享-署名-非商业性使用 4.0 国际)](https://creativecommons.org/licenses/by-nc/4.0/deed.zh) 许可协议。

- **署名**：您必须给出适当的署名，提供指向本许可协议的链接，同时标明是否（对源码）作了修改。
- **非商业性使用**：您不得将本软件或其衍生作品用于商业目的。

本项目代码逻辑及原创内容采用上述协议发布，项目所引用的第三方库之版权归其原作者所有，并遵循其各自的开源协议。
