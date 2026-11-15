# 校园场地预约系统 (Campus Reservation System)

## 📖 项目简介
这是一个基于 Spring Boot 3 和 Redis 开发的高并发校园场地预约系统。系统支持用户查看场地、时间槽库存、高并发抢座预约、取消预约、查看热门排行榜等功能，同时包含完整的管理端（场地管理、预约流水查询、库存初始化）。

## 🛠️ 技术栈
- **核心框架**：Spring Boot 3.2.x、JDK 17
- **持久层**：MyBatis-Plus 3.5.x、MySQL 8.0
- **缓存与并发**：Redis (String, ZSet, Lua脚本)、Redisson (分布式锁、延迟队列)
- **消息队列**：Redis Stream (消费者组、Pending List 死信重试)
- **安全鉴权**：JWT (JSON Web Token)、Spring MVC 拦截器
- **接口文档**：Knife4j (OpenAPI 3)

## ✨ 核心功能与技术亮点
1. **高并发防超卖**：基于 Redis + Lua 脚本实现库存预扣，保证原子性；结合 Redis Stream 异步落库，削峰填谷。
2. **订单超时自动取消**：基于 Redisson 延迟队列 (DelayedQueue) 实现订单 15 分钟未支付自动取消，并回滚库存。
3. **缓存与一致性**：采用 Cache Aside 模式处理场地详情与库存查询；空值缓存防穿透，随机 TTL 防雪崩。
4. **定时任务与分布式锁**：利用 Redisson 保证多节点环境下库存预热任务不重复执行。
5. **复杂业务查询**：管理端预约流水采用 MyBatis XML 动态 SQL 连表查询，解决 N+1 问题。

## 📂 项目结构
```text
com.campus.reservation
├── common        # 公共模块 (统一返回 Result、全局异常、JWT工具、UserContext)
├── config        # 配置类 (Redis, Redisson, MyBatisPlus, RabbitMQ/Stream, Knife4j)
├── controller    # 控制层 (分为 user 和 admin 两个包)
├── dto           # 数据传输对象 (接收前端参数)
├── entity        # 数据库实体类
├── interceptor   # 拦截器
├── mapper        # MyBatis-Plus Mapper 接口
├── mq            # 消息队列消费者 (Stream消费者、延迟队列消费者)
├── service       # 业务逻辑层
├── task          # 定时任务 (库存预热)
└── vo            # 视图对象 (返回给前端的脱敏数据)