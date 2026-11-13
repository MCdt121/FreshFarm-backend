# 鲜农（Fresh Farm）农产品电商与供应链后台服务

基于 Spring Boot 的前后端分离电商后台 API 服务，涵盖商品管理、分类维护、库存扣减、用户鉴权与 AI 文案生成。重点落地缓存高可用、库存防超卖、MQ 异步解耦与统一异常处理。

> 测试账号：`admin / 123456`

## 🛠 技术栈
- **核心框架**：Spring Boot、Spring MVC
- **持久层**：MyBatis-Plus、MySQL
- **缓存与中间件**：Redis（Cache-Aside、Lua 原子扣减）、RabbitMQ（异步解耦、死信队列）
- **安全与治理**：Sa-Token、JSR-303 Validation、Sentinel（限流/熔断）
- **AI**：Spring AI + DeepSeek（商品卖点文案）
- **工具与部署**：Knife4j、Docker Compose、Maven

## ✨ 核心功能

1. **商品缓存与降级**：Cache-Aside 模式，写操作先更新 DB 后删缓存；空值缓存 1 分钟防穿透；Redis 异常时 try-catch 降级直查 MySQL，不阻断主流程。
2. **库存防超卖**：Redis Lua 脚本原子预扣 → RabbitMQ 异步落库 → DB 条件更新 `stock >= quantity` 兜底；MQ 投递失败回滚 Redis 预扣；消费失败进入死信队列并补偿回滚。
3. **消费幂等与死信补偿**：库存消费基于 `messageId + Redis SETNX` 幂等，避免重复扣库；死信补偿带幂等键，避免重复死信重复加库存；对账任务只告警不自动覆盖，避免冲掉在途扣减。
4. **异步解耦**：新增商品后通过 MQ 异步重算分类商品数，消费端“重算覆盖”保证重复消息幂等，提升主链路响应速度。
5. **鉴权与统一异常**：Sa-Token 无状态登录拦截；`Result<T>` 统一响应体；`@RestControllerAdvice` 分类处理业务异常、未登录、参数校验、Sentinel 限流与兜底异常。
6. **AI 文案与保护**：Spring AI 接入 DeepSeek 生成商品文案，按商品缓存 1 小时，异常或输出不合法时回退模板文案；Sentinel QPS 限流，超限返回 429。

## 🚀 快速启动

### 1. 启动基础中间件

项目根目录执行：
```bash
docker-compose up -d

启动后：

MySQL：localhost:3306，账号 root / root，库 farm_mall

Redis：localhost:6379

RabbitMQ：localhost:5672，管理台 http://localhost:15672（guest / guest）

首次启动会自动执行 sql/ 目录下的初始化脚本。若之前起过容器，先 docker-compose down -v 再 up -d，否则初始化脚本不会重新执行。

2. 配置
默认配置已适配 docker-compose，本地默认环境无需修改。DeepSeek Key 通过环境变量注入：

bash
export DEEPSEEK_API_KEY=你的key
3. 启动项目
启动 FamerdemoApplication 主类，访问接口文档：http://localhost:8080/doc.html（Knife4j）。

🔌 核心接口
方法	路径	说明
POST	/user/login	登录，返回 Sa-Token
GET	/user/info	当前登录用户信息
POST	/product/add	新增商品，异步更新分类统计
GET	/product/get/{id}	商品详情，走缓存
POST	/product/update	更新商品，删缓存
DELETE	/product/remove/{id}	删除商品，删缓存
POST	/stock/deduct	库存扣减，Redis 预扣 + MQ 落库
POST	/stock/sync/{productId}	从 DB 同步库存到 Redis
POST	/product/ai-desc/{id}	AI 生成商品文案，带缓存与限流
POST	/kind/add	新增分类
GET	/kind/get/{id}	查询分类
DELETE	/kind/remove/{id}	删除分类，有商品时业务异常

📁 项目结构
text
src/main/java/com/briup/famerdemo
├── common        # Result、BizException、全局异常处理
├── config        # RabbitMQ、Sa-Token、Sentinel 配置
├── consumer      # 库存、分类统计、死信消费者
├── controller    # 商品、分类、库存、用户、AI 接口
├── entity        # 实体与请求对象
├── mapper        # MyBatis-Plus Mapper
├── service       # 业务接口与实现
└── task          # 库存对账定时任务
src/main/resources
├── application.yml
└── stock.lua     # Redis 原子扣减脚本