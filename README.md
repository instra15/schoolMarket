# 校园二手交易平台（SchoolMarket）

> 面向高校学生的 C2C 二手交易平台 · Spring Boot 4 + MyBatis 深度使用 + 乐观锁防超卖 + JDK 原生线程池异步化

一个覆盖「发布 → 检索 → 下单 → 扣库存 → 通知」完整链路的校园闲置流转平台。
典型场景是毕业季的教材、数码、生活用品转让。

**项目的技术主线不是功能数量，而是三件事：**

| 主线 | 解决什么 | 怎么做的 |
|---|---|---|
| MyBatis 深度使用 | 动态 SQL、多表关联、分页的工程化写法 | 动态 `<if>` 条件、`<association>`/`<collection>` 关联映射、手写 `LIMIT/OFFSET` 分页 |
| 并发控制 | 多人抢同一商品会**超卖** | 数据库乐观锁（`version` 字段），扣库存 SQL 带 `version` + `stock` 双条件 |
| 性能解耦 | 通知/日志写入拖慢核心交易 | JDK 原生 `ThreadPoolExecutor` + **事务提交后**才提交异步任务 |

---

## 一、技术栈

| 分类 | 选型 | 说明 |
|---|---|---|
| 框架 | Spring Boot **4.0.8** | Java 17 |
| 持久层 | MyBatis 3（`mybatis-spring-boot-starter` 4.0.1） | SQL 全部写在 XML，不使用注解式 SQL |
| 数据库 | MySQL 8.0 | |
| 文档 | springdoc-openapi 3.0.2 | Swagger UI（已移除 knife4j，理由见「技术决策」） |
| 工具 | Lombok、Jackson 3、雪花 ID | |

**明确不使用**（刻意的技术约束）：Redis、消息队列、Spring Cloud、分布式锁、`@Async`、`CompletableFuture`。
所有并发能力基于 **JDK 原生线程池 + 数据库乐观锁** 实现。

---

## 二、快速开始

### 1. 环境要求
- JDK 17+（本项目在 JDK 24 下编译验证通过，target 17）
- Maven 3.9+
- MySQL 8.0

### 2. 建库

```bash
mysql -u root -p < sql/init.sql
```

脚本会创建 `school_market` 库、7 张表，并预置 2 个用户 / 3 个分类 / 5 件商品。
种子账号：`zhangsan` / `lisi`，密码均为 `123456`。

### 3. 启动

```bash
export DATABASE_PASSWORD='你的MySQL密码'
mvn spring-boot:run
```

> 密码有默认值 `123456`，不设环境变量时按默认走。

### 4. 访问

| 用途 | 地址 |
|---|---|
| **Web 界面** | http://localhost:8080/ |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| 接口文档 JSON | http://localhost:8080/v3/api-docs |

### 5. 快速试一下

Web 界面已内置全部功能，浏览器打开 http://localhost:8080/ 即可
（种子账号 `zhangsan` / `lisi`，密码 `123456`）。

也可以用 curl：

```bash
# 登录（种子用户）
curl -s -c ck.txt -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"lisi","password":"123456"}'

# 下单（商品3的卖家是 zhangsan，所以用 lisi 买）
curl -s -b ck.txt -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":3,"quantity":1}]}'

# 查通知（异步写入，稍等 1 秒）
sleep 1 && curl -s -b ck.txt http://localhost:8080/api/notifications
```

---

## 三、接口一览

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| **用户** | POST | `/api/users/register` | 注册 |
| | POST | `/api/users/login` | 登录（写 Session） |
| | GET | `/api/users/me` | 当前用户（`@CurrentUser` 注入） |
| | GET | `/api/users/{id}` | 用户详情（脱敏） |
| | PUT | `/api/users/profile` | 改资料 |
| | PUT | `/api/users/password` | 改密码（校验旧密码） |
| | POST | `/api/users/logout` | 退出 |
| **商品** | POST | `/api/products` | 发布商品 |
| | PUT | `/api/products/{id}` | 编辑商品 |
| | PUT | `/api/products/{id}/status` | 上下架 |
| | DELETE | `/api/products/{id}` | 删除商品 |
| | GET | `/api/products` | 分页搜索（动态条件） |
| | GET | `/api/products/mine` | 我的商品 |
| | GET | `/api/products/{id}` | 商品详情 |
| **分类** | GET | `/api/categories` | 分类列表（含商品数） |
| | GET | `/api/categories/{id}` | 分类详情 |
| | POST | `/api/categories` | 新建分类 |
| | PUT | `/api/categories/{id}` | 编辑分类 |
| | DELETE | `/api/categories/{id}` | 删除分类 |
| **订单** | POST | `/api/orders` | 下单（乐观锁扣库存） |
| | PUT | `/api/orders/{orderNo}/pay` | 模拟支付（幂等） |
| | PUT | `/api/orders/{orderNo}/cancel` | 取消订单（回补库存） |
| | GET | `/api/orders/{orderNo}` | 订单详情 |
| | GET | `/api/orders` | 我买到的 |
| | GET | `/api/orders/sold` | 我卖出的 |
| **通知** | GET | `/api/notifications` | 我的通知 |
| | GET | `/api/notifications/unread-count` | 未读数 |
| | PUT | `/api/notifications/{id}/read` | 标记已读 |
| | PUT | `/api/notifications/read-all` | 全部已读 |

---

## 四、核心实现

### 4.1 乐观锁防超卖（项目技术核心）

**问题**：20 个用户同时抢库存为 1 的商品，会发生什么？

```
线程A 读 stock=1, version=0
线程B 读 stock=1, version=0        ← 两者读到的是同一份快照
线程A 算 stock-1=0, 写回 stock=0
线程B 算 stock-1=0, 写回 stock=0  ← 直接覆盖了A的结果
                                    → 库存只扣了1次，但2个人都"买到了"
```

**解决**：`product` 表加 `version` 字段，扣库存 SQL 带**双条件**：

```sql
UPDATE product
SET stock   = stock - ?,
    version = version + 1
WHERE id      = ?
  AND version = ?      -- ① 乐观锁：版本必须与读取时一致
  AND stock  >= ?      -- ② 兜底：绝不允许扣成负数
```

任一条件不满足 → 影响行数为 0 → Service 抛 `StockNotEnoughException` → 事务回滚。

**两个条件缺一不可**：只有 `version` 万一判断被绕过仍可能扣成负数；只有 `stock` 则会丢失更新。

**一个反模式**（本项目刻意避免）：影响 0 行后**在同一事务内重查重试**。
MySQL 默认 `REPEATABLE READ` 下重查会读到旧快照，`version` 永远对不上 → 死循环。
正确做法是直接回滚让用户重新下单。

**多商品订单按 `productId` 升序扣减**：避免两个订单分别含 `[商品1,商品2]` 与 `[商品2,商品1]` 时
交叉加锁导致死锁。

### 4.2 事务提交后异步执行

**问题**：下单成功后要写通知表。如果直接在事务里写——

- 通知写失败 → 下单事务回滚 → 主流程被辅助数据拖累
- 通知写成功但事务后续回滚 → 通知说"下单成功"而订单实际不存在（脏数据）

**解决**：把任务挂到事务的 `afterCommit` 回调上。

```java
if (TransactionSynchronizationManager.isSynchronizationActive()) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { threadPoolHolder.submit(task); }
    });
} else {
    threadPoolHolder.submit(task);   // 无事务则直接执行
}
```

> **注意**：异步线程是全新线程，**不继承主线程的事务上下文**。
> 任务内单条写库会自动提交；若需要多条原子性，得在任务内部自己用 `TransactionTemplate`。

**线程池不用 `Executors.newFixedThreadPool`**：它用无界 `LinkedBlockingQueue`，
队列满时会一直堆积直到 OOM。本项目用有界 `ArrayBlockingQueue(256)` +
`CallerRunsPolicy`（队列满时由提交线程自己执行，形成背压且不丢任务）。

### 4.3 MyBatis 用法要点

**动态 SQL**：`<where>` + `<if>` 实现关键词模糊匹配、分类、价格区间、状态等组合查询。

**association（一对一）**：商品详情一次 `LEFT JOIN` 带出分类名与卖家昵称，避免 N+1 查询。

```xml
<association property="seller" javaType="com.schoolMarket.vo.UserVO">
    <id     column="u_id"       property="id"/>
    <result column="u_nickname" property="nickname"/>
</association>
```

**collection（一对多）**：订单详情一次 JOIN 带出全部订单项，MyBatis 自动按 `<id>` 合并行。

**手写分页**：`LIMIT #{pageSize} OFFSET #{offset}`，偏移量由 `PageQueryDTO.getOffset()` 计算。
列表查询与总数查询**共用同一段 `<sql>` 片段**，保证条件一致 —— 这是手写分页最容易出错的地方。

### 4.4 统一响应与异常

```json
{ "code": 200, "message": "success", "data": { } }
```

响应体的 `code` 与 HTTP 状态码**保持一致**，不用「HTTP 200 + body 里 code=500」那套混合风格。

| 异常 | HTTP | 场景 |
|---|---|---|
| `BusinessException` | 400 | 业务规则错误（用户名重复、越权操作…） |
| `StockNotEnoughException` | **409** | 库存不足 / 乐观锁冲突（资源状态冲突，不是参数错） |
| `MethodArgumentNotValidException` | 400 | 参数校验失败 |
| `ServletRequestBindingException` | 401 | 未登录 |

---

## 五、Web 前端

`src/main/resources/static/` 下是一套**零依赖**的原生 HTML/CSS/JS 单页应用，
由 Spring Boot 直接托管，不需要 Node、不需要构建、不需要单独的服务器。

```
static/
├── index.html   页面结构（5 个 Tab + 3 个弹窗）
├── app.css      样式（CSS 变量统一配色，响应式适配移动端）
└── app.js       逻辑（API 封装、状态管理、渲染）
```

### 功能覆盖

| 页面 | 能做什么 |
|---|---|
| 逛市场 | 商品列表、关键词搜索、分类筛选、价格区间筛选、分页 |
| 商品详情 | 图片、描述、分类、卖家、库存、购买（选数量） |
| 发布 | 标题/描述/图片地址/价格/库存/分类，提交后回到市场 |
| 我买到的 | 按状态筛选（待支付/已支付/已取消）、去支付、取消订单 |
| 我卖出的 | 卖家视角的订单列表 |
| 通知 | 通知列表、未读红点、全部标记已读 |
| 登录/注册 | 弹窗表单，带字段级校验与后端错误提示 |

### 三个关键实现点

**1. 登录态用后端探针，不用前端存储**

```js
async function refreshLoginState() {
    const res = await api('/api/users/me');   // 未登录返回 401
    if (res.code === 200 && res.data) setUser(res.data);
    else setUser(null);
}
```

前端自己记 userId 会出现「前端显示已登录、后端认为未登录」的错觉。
直接问后端要最准 —— 后端用 Session 存身份，`/api/users/me` 是唯一真相来源。

**2. `credentials: 'same-origin'` 保证 Session Cookie 携带**

后端靠 `JSESSIONID` Cookie 维持登录态，跨请求必须带上。
同源请求浏览器默认会带，显式写上更保险。

**3. 所有用户输入渲染前都做 HTML 转义**

商品标题、描述、昵称都是用户可填的，直接 `innerHTML` 拼接会构成 XSS。
`escapeHtml()` 处理 `& < > " '` 五个字符后再插入。

### 前端契约回归测试

字段名对不上是前端最常见的坑（后端改了字段名，前端静默显示 undefined）。
`scripts/frontend-api-check.js` 按 `app.js` 的调用顺序打一遍后端，验证每个字段真实存在：

```bash
node scripts/frontend-api-check.js http://localhost:8080
```

```
[PASS] 分类含 productCount（前端渲染用）
[PASS] 未登录 /api/users/me → 401
[PASS] 商品含 category.name（association）
[PASS] 商品含 seller.nickname（association）
[PASS] 拿到 Session Cookie
[PASS] 已登录 /api/users/me → 200
[PASS] 重复支付仍 200（幂等）
[PASS] 下单后通知已异步写入
[PASS] GET /api/products?categoryId 过滤生效
[PASS] GET /api/products?keyword 搜索生效
...
```

---

## 六、测试

### 6.1 运行

```bash
# 未配置数据库时，45 个测试自动跳过（不是失败）
mvn test

# 配置密码后跑全部测试
export DATABASE_PASSWORD='你的MySQL密码'
mysql -u root -p < sql/init-test.sql   # 建测试库
mvn test
```

> 测试库 `school_market_test` 与开发库 `school_market` 隔离。
> 需要真实 MySQL —— 项目 SQL 用了 `CONCAT`、反引号、`LIMIT...OFFSET` 等 MySQL 专有语法，
> H2 等内存库无法替代。

### 6.2 测试覆盖

| 测试类 | 数量 | 重点 |
|---|---|---|
| `ProductMapperTest` | 10 | 动态 SQL 各条件组合、手写分页、association 关联、扣库存双条件 |
| `OrderMapperTest` | 6 | collection 一对多、支付/取消幂等、卖家视角 EXISTS 查询、JOIN 后计数不放大 |
| `NotificationMapperTest` | 7 | **`is_read → read` 显式映射**、按已读状态筛选、标记已读幂等 |
| `UserMapperTest` | 6 | 密码哈希存储、VO 脱敏 |
| `ConcurrentStockTest` | 4 | **并发防超卖**、事务整体回滚、乐观锁拒绝过期 version |
| `ApiControllerTest` | 11 | 统一响应体、401 未登录、参数校验 400、越权拦截 |

### 6.3 并发压测

```bash
# 方式一：JUnit 测试（推荐，CountDownLatch 保证同一瞬间起跑）
mvn test -Dtest=ConcurrentStockTest

# 方式二：独立压测程序（对运行中的应用发起真实 HTTP 并发）
mvn clean compile
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes;$(cat cp.txt)" com.schoolMarket.concurrent.StockRaceTest 20 1

# 方式三：Shell 脚本
bash scripts/stock-race-test.sh
```

**通过标准**：20 线程抢 1 件库存 → 成功数 ≤ 1、最终 `stock = 0`、订单项条数 ≤ 1。

---

## 七、技术决策记录

几个偏离"常规写法"的决定，都是踩过坑后确认的：

### 6.1 移除 knife4j，只用原生 Swagger UI

knife4j 4.5.0 的 `Knife4jOpenApiCustomizer` 字节码调用 `SpringDocConfigProperties.getGroupConfigs()`
期望返回 `List`，而 springdoc 3.0.2 已改为返回 `Set` —— 启用即 `NoSuchMethodError`，文档页全空。
（用 `javap` 反编译确认，非推测。）

### 6.2 状态字段存 varchar 而非 tinyint

MyBatis 内置 `EnumTypeHandler` 零配置即可双向映射，且查库看到 `ON_SALE` 比 `1` 直观。
省掉 35 行自定义 TypeHandler + 注册配置。

### 6.3 日期配置只能写在 YAML

Spring Boot 4 换用 **Jackson 3**（`tools.jackson`），`Jackson2ObjectMapperBuilderCustomizer`
类**已不存在**，写自定义 Config 类会编译失败。

更关键的是 Jackson 3 把日期开关从 `SerializationFeature` 拆到了独立的 `DateTimeFeature`：

```yaml
# ❌ 启动失败：No enum constant SerializationFeature.write-dates-as-timestamps
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false

# ✅ 正确
spring:
  jackson:
    datatype:
      datetime:
        write-dates-as-timestamps: false
```

### 6.4 Entity 扁平化外键，关联映射放 VO

Entity 必须与表字段一一对应才能 `insert`。`Product.category` 若是 `Category` 对象，
`#{category}` 绑定到 `category_id` 列必然报错。因此 Entity 全部用扁平 `categoryId`/`sellerId`，
`<association>`/`<collection>` 放在 VO 的 resultMap 上。

### 6.5 `Notification.read` 字段不能叫 `isRead`

Lombok 的 `@Data` 对 `boolean isRead` 生成 `isRead()`/`setRead()`，MyBatis 的 `PropertyNamer`
把 getter 名 `isRead` 归一化成属性名 **`read`**，而列 `is_read` 驼峰转换是 `isRead` —— 不匹配，
**且不报任何错**，字段永远是 `false`。命名为 `read` 并显式映射彻底避开。

### 6.6 MyBatis resultMap 元素顺序受 DTD 强制

顺序必须是 `constructor? → id* → result* → association* → collection* → discriminator?`。
**所有 `<result>` 必须排在所有 `<association>`/`<collection>` 之前**，
否则 XML 格式合法但 DTD 校验失败，启动报 `SAXParseException`。
只有真正解析 XML 时才暴露，XML 格式校验查不出来。

---

## 八、项目结构

```
src/main/java/com/schoolMarket/
├── annotation/     @CurrentUser 注解
├── aspect/         @LogOperation + AOP 切面
├── common/         Result / PageResult 统一响应
├── config/         WebMvcConfig / CurrentUserResolver / OpenApiConfig
├── controller/     5 个 Controller
├── dto/            请求入参（含校验注解）
├── entity/         7 个实体，与表字段一一对应
├── enums/          OrderStatus / ProductStatus
├── exception/      BusinessException / StockNotEnoughException / GlobalExceptionHandler
├── mapper/         7 个 Mapper 接口（SQL 全在 XML）
├── service/        业务接口 + impl
├── util/           PasswordEncoder / SnowflakeIdGenerator / ThreadPoolHolder / TransactionAfterCommitExecutor
├── vo/             出参对象（association / collection 落点）
└── concurrent/     StockRaceTest 独立压测程序

src/main/resources/mapper/    7 个 Mapper XML（约 700 行）
src/main/resources/static/    Web 前端（index.html / app.css / app.js）
src/test/java/               6 个测试类，45 个测试
sql/init.sql                 建库 + 种子数据
sql/init-test.sql            测试库
scripts/stock-race-test.sh   并发压测脚本
scripts/frontend-api-check.js  前端 API 契约回归测试
```

---

## 九、已知限制

作为个人练手项目，以下是刻意未做的：

- 无图片上传（`image_url` 字段预留，前端传 URL）
- 无真实支付对接（`pay` 为模拟接口，幂等逻辑已完整）
- 无管理员角色（分类的增删改未做权限控制）
- 无 Redis 缓存 / 消息队列（明确约束在技术选型范围外）
- 通知为站内通知，无推送 / 邮件
