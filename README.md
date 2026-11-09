                             # 鲜农（Fresh Farm）农产品电商与供应链后台服务

本项目是一个基于 Spring Boot 构建的前后端分离电商后台 API 服务，涵盖商品管理、分类维护与用户鉴权等核心模块。

## 🛠 技术栈
- **核心框架**：Spring Boot, Spring MVC
- **持久层**：MyBatis-Plus, MySQL
- **缓存与中间件**：Redis, RabbitMQ
- **安全与鉴权**：Sa-Token, JSR-303 Validation
- **工具与部署**：Knife4j, Docker, Maven

## ✨ 核心功能
1. **缓存高可用**：使用 Redis 实现 Cache-Aside 模式，解决高频读取问题；通过空值缓存防穿透，并实现 Redis 宕机时的 try-catch 降级兜底。
2. **异步解耦**：基于 RabbitMQ 实现新增商品后分类统计数据的异步落库，提升主链路响应速度。
3. **全局安全与规范**：集成 Sa-Token 实现无状态登录拦截；自定义 `Result<T>` 与 `@RestControllerAdvice` 全局异常处理器，实现参数校验与业务异常的规范返回。

## 🚀 快速启动 (Quick Start)

本项目依赖以下环境，推荐使用 Docker 一键启动：

### 1. 启动基础中间件 (MySQL / Redis / RabbitMQ)
请在项目根目录下执行以下命令：
\`\`\`bash
docker-compose up -d
\`\`\`
> 启动后：
> - MySQL 端口：3306 (账号 root / 密码 root)
> - Redis 端口：6379
> - RabbitMQ 端口：5672 (管理后台访问 http://localhost:15672，账号/密码 guest/guest)

### 2. 导入数据库
将项目 `sql/` 目录下的 `farm_mall.sql` 导入到 MySQL 数据库中。（如果你没建 sql 目录，就把建表语句贴在这里或者新建一个 SQL 文件）

### 3. 启动项目
修改 `application.yml` 中的数据库、Redis、RabbitMQ 连接配置（如果你的本地环境是默认的，则无需修改）。
启动 `FamerdemoApplication` 主类。
访问接口文档：`http://localhost:8080/doc.html`档：`http://localhost:8080/doc.html`（Knife4j）。