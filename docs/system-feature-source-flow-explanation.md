# 系统功能源码链路讲解文档

这份文档是给老师检查和答辩时使用的源码讲解材料。写法不是泛泛介绍 Spring Boot 或 Vue，而是按当前项目源码说明：一个功能从前端页面怎么布局、前端怎么收集参数、怎么调用接口、后端 Controller 怎么接收、Service 层怎么处理业务、Mapper 和 MyBatis XML 怎么查询数据库、数据怎么包装成 JSON 返回、前端最后怎么绑定到页面。

项目当前主栈是 Vue 3 + Element Plus + Pinia + Axios + Spring Boot + MyBatis + MySQL + LangChain4j。AI 在系统里不是直接替代业务规则，而是作为售后客服辅助层，用来生成回复、识别意图、检索知识库、记录调用日志；真正的售后状态流转、订单校验、工单处理仍然在 Java Service 层完成。

## 1. 整体请求链路

我做每个功能时都按同一条链路设计：

```text
Vue 页面
  -> v-model / ref / reactive 收集页面参数
  -> web/src/api/*.js 里封装 Axios 请求
  -> web/src/api/request.js 自动加 Authorization: Bearer <JWT>
  -> Spring Controller 接收 query/path/body/header
  -> ServiceImpl 做权限、校验、状态流转、AI 或兜底逻辑
  -> 标准列表页在 ServiceImpl 调用 PageHelper.startPage
  -> Mapper 接口调用 MyBatis XML
  -> MyBatis XML 负责动态条件、关联查询和排序
  -> MySQL 表返回数据，PageHelper 生成分页总数和当前页 rows
  -> Service 组装对象或 PageResult
  -> Controller 用 Result.success(data) 包成 JSON
  -> Axios 响应拦截器取 result.data
  -> Vue 把数据写回 ref/reactive，表格、详情、时间线自动刷新
```

统一响应在 `server/src/main/java/com/user/returnsassistant/pojo/Result.java`，字段是 `code`、`msg`、`data`。成功时后端返回 `Result.success(data)`，失败时返回 `Result.error(msg)`。分页对象在 `PageResult.java`，字段是 `total` 和 `rows`。订单、售后、知识库、工单、日志等标准列表页统一在 Service 层调用 `PageHelper.startPage(page, pageSize)`，Mapper XML 只保留查询条件和排序，避免把分页 `limit` 分散写在各个 XML 中。

前端统一请求封装在 `web/src/api/request.js`。这里创建了 Axios 实例，`baseURL` 默认是 `/api`，请求拦截器从 `localStorage` 读取 `returns_assistant_token`，然后写入请求头 `Authorization: Bearer <JWT>`。响应拦截器判断后端返回的 `code`，如果 `code === 1` 就直接返回 `result.data`，所以页面里拿到的就是业务数据，不需要每个页面重复判断 `code/msg/data`。

数据库表主要在 `sql/schema.sql`。核心表包括：

| 模块 | 表 |
| --- | --- |
| 用户与权限 | `user_account` |
| 订单 | `demo_order`、旧版 `after_sale_record` |
| 新售后闭环 | `after_sale_application`、`after_sale_evidence`、`after_sale_process_log`、`service_review` |
| 聊天与 AI | `chat_session`、`chat_message`、`intent_record`、`retrieval_log`、`ai_call_log`、`process_trace` |
| 知识库 | `knowledge_category`、`knowledge_doc` |
| 工单 | `service_ticket`、`reply_draft` |

老师如果问我整体怎么做的，我可以这样回答：

> 我不是只写了一个页面，也不是让前端直接拼假数据。我把前端页面、接口封装、后端 Controller、Service、Mapper、XML 和数据库表连成了完整链路。比如一个查询请求，前端先用 `v-model` 收集查询条件，再调用 `api` 文件里的函数，Axios 自动带上 JWT token。后端 Controller 接收参数后调用 Service，Service 再调用 Mapper。Mapper 的 SQL 写在 XML 里，查询数据库后返回 Java 对象，Controller 用 `Result.success` 包成 JSON。前端响应拦截器取出 `data`，页面把 `rows`、`total` 或详情对象写回响应式变量，表格和详情区域就会自动更新。

## 2. 登录、注册和权限路由

### 前端页面和参数绑定

登录页是 `web/src/views/LoginView.vue`。页面用一个登录卡片承载账号、密码、注册昵称、手机号、确认密码等输入项。`mode` 控制当前是登录还是注册，`form` 是 `reactive` 对象，字段包括 `username`、`password`、`confirmPassword`、`displayName`、`phone`。页面上的 `el-input` 通过 `v-model="form.username"`、`v-model="form.password"` 等方式把输入框内容绑定到 `form`。

登录页还有两个快捷按钮 `useAdmin()` 和 `useCustomer()`。点击后会把账号填成 `admin` 或 `demo_customer`，密码填成演示密码，方便老师演示管理员端和顾客端。

点击登录按钮时调用 `submit()`。如果 `mode === 'login'`，它调用 `authStore.login(form.username, form.password)`；如果是注册，就调用 `authStore.register({...form})`。

### API 和后端 Controller

前端 API 在 `web/src/api/authApi.js`：

```js
login(data) -> POST /auth/login
register(data) -> POST /auth/register
getMe() -> GET /auth/me
logout() -> POST /auth/logout
```

后端入口是 `server/src/main/java/com/user/returnsassistant/controller/AuthController.java`，类上有 `@RequestMapping("/auth")`。登录方法是 `@PostMapping("/login")`，接收 `@RequestBody LoginRequest request`；注册方法是 `@PostMapping("/register")`，接收 `RegisterRequest`；`/auth/me` 和 `/auth/logout` 通过 `HttpServletRequest` 读取请求头里的 `Authorization`，也就是前端传来的 `Bearer <JWT>`。

### Service 和数据库

业务在 `AuthServiceImpl.java`。登录时 `login(username, password)` 先用 `UserAccountMapper.getByUsername(username)` 查询 `user_account` 表，判断账号是否存在、`status` 是否启用，再用 `passwordMatches(user, password)` 校验密码。注册时 `register(...)` 会校验用户名格式、手机号格式、两次密码是否一致，然后调用 `UserAccountMapper.insert(userAccount)` 插入新用户。

现在登录已经改成 JWT。`AuthServiceImpl` 登录或注册成功后不再把会话放进内存 `sessions`，而是调用 `JwtTokenProvider.createToken(user, Duration.ofHours(tokenHours))` 签发 JWT。`JwtTokenProvider` 位于 `server/src/main/java/com/user/returnsassistant/utils/JwtTokenProvider.java`，使用 `io.jsonwebtoken` 生成三段式 token，里面写入：

```text
jti -> token 唯一编号，用于退出登录黑名单
iss -> 签发者，对应 application.yml 的 app.auth.jwt-issuer
sub -> 用户 ID
username/displayName/role -> 用户展示信息和角色
iat -> 签发时间
exp -> 过期时间
```

JWT 的密钥和签发者配置在 `server/src/main/resources/application.yml`：

```yaml
app:
  auth:
    token-hours: ${APP_AUTH_TOKEN_HOURS:8}
    jwt-secret: ${APP_AUTH_JWT_SECRET:returns-assistant-dev-jwt-secret-change-me-2026}
    jwt-issuer: ${APP_AUTH_JWT_ISSUER:returns-assistant}
```

后续接口鉴权时，Controller 或拦截器把 `Authorization` 传给 `authService.requireUser/requireAdmin/requireCustomer`。Service 先用 `normalize(token)` 去掉 `Bearer ` 前缀，再调用 `JwtTokenProvider.parseToken` 校验签名、签发者、过期时间和必要字段。解析出 `sub` 后，Service 还会再调用 `UserAccountMapper.getById(userId)` 查询数据库，确认账号仍然存在且 `status = 1`，这样即使 JWT 没过期，账号被停用后也不能继续访问。

管理员接口使用 `@OperatorAnno` 标记，`AuthInterceptor` 只负责识别这个标记并调用 `authService.requireAdmin(...)`，不引入 AOP，也不把它扩展成操作日志审计。顾客和管理员的数据边界放在 Controller/Service 中处理，例如 `OrderController`、`ChatSessionController`、`CustomerAfterSaleController` 会结合当前用户调用 `authService.isAdmin(user)` 或 `authService.ensureSelfOrAdmin(user, ownerId, message)`，保证顾客只能访问自己的订单、会话、售后和工单相关数据。

退出登录时，`logout(token)` 会解析 JWT 的 `jti` 和过期时间，把 `jti` 放入 `revokedJwtIds` 黑名单。之后同一个 token 再访问接口时，`parseRequired` 会发现这个 `jti` 还没过期，就返回“登录已退出，请重新登录”。`cleanupRevokedJwtIds()` 会清理已经过期的黑名单记录。

`LoginResponse` 里仍然包含 `token`、`userId`、`username`、`displayName`、`role`、`expiresAt`，所以前端登录存储逻辑不用大改，只是 token 内容从原来的普通随机串变成了标准 JWT。

MyBatis 映射在 `server/src/main/resources/mapper/UserAccountMapper.xml`，包括：

```text
getById
getByUsername
insert
```

查询或插入的数据库表是 `user_account`。

### 前端返回绑定和路由拦截

`web/src/stores/authStore.js` 用 Pinia 保存登录状态。`login()` 调用接口成功后执行 `saveAuth(data)`，把 JWT 存入 `returns_assistant_token`，把用户信息存入 `returns_assistant_user`。`isLoggedIn` 根据 token 判断是否登录，`isAdmin` 根据 `user.role === 'ADMIN'` 判断是不是管理员。页面刷新后，Pinia 会从 `localStorage` 恢复 token；访问接口时 Axios 自动带上 `Authorization: Bearer <JWT>`，后端再解析 JWT 获得当前用户。

路由在 `web/src/router/index.js`。`beforeEach` 做权限控制：

| 情况 | 处理 |
| --- | --- |
| 未登录访问业务页面 | 跳转 `/login` |
| 管理员访问顾客页面 | 跳转 `/admin/after-sales/review` |
| 顾客访问管理员页面 | 跳转 `/customer/after-sales` |
| 登录后访问 `/login` | 管理员进审核台，顾客进售后中心 |

老师问登录怎么做，我可以这样回答：

> 我登录页不是只判断本地账号，而是通过 `authApi.login` 请求后端 `/auth/login`。后端 `AuthController` 接收 `LoginRequest`，交给 `AuthServiceImpl`。Service 通过 `UserAccountMapper.getByUsername` 查询 `user_account` 表，先判断账号是否启用，再校验密码。登录成功后调用 `JwtTokenProvider.createToken` 生成 JWT，JWT 里包含用户 ID、账号、昵称、角色、签发时间、过期时间和 `jti`。后端把 JWT 和用户角色包装成 `LoginResponse` 返回，前端 Pinia 的 `authStore` 把 JWT 保存到 `returns_assistant_token`。后面所有请求都会由 Axios 拦截器自动把 JWT 放到 `Authorization: Bearer ...` 请求头。后端每次鉴权会解析 JWT，并根据 token 里的用户 ID 回查数据库确认账号仍然可用。路由守卫根据 `authStore.isAdmin` 判断用户是管理员还是顾客，所以管理员和顾客会进入不同页面。

## 3. 顾客售后中心

顾客端页面是 `web/src/views/CustomerAfterSaleCenterView.vue`，路由是 `/customer/after-sales`。这个页面是顾客真实使用的售后中心，不只是聊天入口。

### 3.1 顾客查询自己的订单

页面左侧有“我的订单”区域，包含订单搜索框、订单表格、分页和“申请”按钮。查询条件保存在：

```js
const orderQuery = reactive({ page: 1, pageSize: 8, keyword: '' })
const orders = ref([])
const orderTotal = ref(0)
const selectedOrder = ref(null)
```

输入框使用 `v-model="orderQuery.keyword"`，点击查询按钮或回车时调用 `loadOrders()`。这个方法调用 `pageOrders(orderQuery)`。

前端 API 在 `web/src/api/orderApi.js`：

```js
pageOrders(params) -> GET /orders
```

后端入口是 `OrderController.page(OrderSearch search, HttpServletRequest request)`。这里接收查询参数，`OrderSearch` 继承 `BaseSearch`，所以有 `page`、`pageSize`、`keyword`，还可以带 `orderStatus`、`logisticsStatus`、`afterSaleStatus`。

Controller 会根据 token 拿当前用户。如果是顾客，后端会把 `search.userId` 限制成当前顾客 ID，这样顾客只能看到自己的订单；管理员则可以看全部订单。

Service 是 `OrderServiceImpl.page(OrderSearch search)`，它先调用 PageHelper：

```text
PageHelper.startPage(search.getPage(), search.getPageSize())
DemoOrderMapper.page(search)
new PageResult<>(page.getTotal(), page.getResult())
```

XML 是 `DemoOrderMapper.xml`，查询从 `demo_order` 表读取数据。`keyword` 会匹配订单号或商品名，XML 只保留 `where` 动态条件和 `order by updated_at desc, id desc`，分页总数和当前页截取由 PageHelper 处理。返回结果被组装成 `PageResult<DemoOrder>`。

前端拿到数据后执行：

```js
orders.value = data.rows || []
orderTotal.value = data.total || 0
```

表格通过 `:data="orders"` 渲染订单号、商品、金额、订单状态、售后状态。点击某一行触发 `selectOrder(row)`，把这一行写入 `selectedOrder.value`，顶部“申请售后”按钮就可以使用这条订单。

老师问订单查询怎么做，我可以这样回答：

> 顾客订单查询是从 `CustomerAfterSaleCenterView.vue` 的订单搜索区域开始的。页面用 `orderQuery.keyword` 保存输入的订单号或商品名，点击查询后调用 `loadOrders()`，再通过 `orderApi.pageOrders` 请求 `/orders`。后端 `OrderController.page` 接收 `OrderSearch`，如果当前用户是顾客，就把 `userId` 限制为当前登录用户，避免查到别人的订单。Service 先调用 `PageHelper.startPage`，再调用 `DemoOrderMapper.page`，XML 从 `demo_order` 表按动态条件查询并排序。返回的 `PageResult` 里有 `rows` 和 `total`，前端把它们写到 `orders` 和 `orderTotal`，Element Plus 表格就自动刷新。

### 3.2 顾客提交售后申请

顾客选择订单后点击“申请售后”，页面调用 `openApplyDialog(order)`，打开 `el-dialog`。弹窗表单绑定 `applyForm`：

```js
const applyForm = reactive({
  serviceType: 'RETURN',
  reasonCode: '',
  reasonText: '',
  refundAmount: 0
})
```

页面用 `el-radio-group` 选择售后类型：`RETURN`、`EXCHANGE`、`REFUND`、`COMPLAINT`；用 `el-select` 选择原因类型；用 `el-input-number` 填退款金额；用 `el-input type="textarea"` 填问题说明。点击“提交申请”调用 `submitApplication()`，它组装：

```js
{
  orderId: applyingOrder.value.id,
  serviceType: applyForm.serviceType,
  reasonCode: applyForm.reasonCode,
  reasonText: applyForm.reasonText,
  refundAmount: applyForm.refundAmount
}
```

然后调用 `createCustomerAfterSale(data)`。

前端 API 在 `customerAfterSaleApi.js`：

```js
createCustomerAfterSale(data) -> POST /customer/after-sales
```

后端入口是 `CustomerAfterSaleController.create(@RequestBody AfterSaleApplicationCreateRequest createRequest, HttpServletRequest request)`。Controller 先调用 `authService.requireCustomer(...)`，保证只有顾客身份能提交，然后把请求交给 `AfterSaleApplicationServiceImpl.create(createRequest, customer)`。

Service 层做了关键业务规则：

| 规则 | 代码位置 |
| --- | --- |
| 必须选择订单 | `request.getOrderId()` 校验 |
| 订单必须存在 | `orderMapper.getById(request.getOrderId())` |
| 顾客只能给自己的订单申请售后 | 比较 `order.userId` 和 `customer.id` |
| 未支付或关闭订单不能申请 | 校验 `payStatus`、`orderStatus` |
| 同一订单不能有进行中售后 | `applicationMapper.countActiveByOrderId(order.getId())` |
| 金额不能超过订单金额 | 比较 `refundAmount` 和 `order.orderAmount` |
| 类型必须在四类中 | `SERVICE_TYPES = RETURN/EXCHANGE/REFUND/COMPLAINT` |

校验通过后，Service 创建 `AfterSaleApplication` 对象，生成 `applicationNo`，设置 `orderId`、`userId`、`serviceType`、`reasonCode`、`reasonText`、`status = SUBMITTED`、`refundAmount`、`priority`、`riskLevel`、`slaDeadline` 等字段。然后：

```text
applicationMapper.insert(application)
writeLog(..., "SUBMIT", null, "SUBMITTED", ...)
orderMapper.updateAfterSaleStatus(...)
return getById(application.getId())
```

MyBatis XML 是 `AfterSaleApplicationMapper.xml`。`insert` 插入 `after_sale_application` 表；`AfterSaleProcessLogMapper.xml` 插入 `after_sale_process_log` 表；`DemoOrderMapper.xml` 的 `updateAfterSaleStatus` 更新 `demo_order.after_sale_status`。

后端最后返回完整售后详情，里面包括主表数据、流程日志和凭证列表。前端提交成功后关闭弹窗，调用 `reloadAll()` 重新加载订单列表和售后列表。

老师问售后申请怎么做，我可以这样回答：

> 顾客提交售后时，前端先在弹窗里用 `applyForm` 收集售后类型、原因、说明和退款金额，再把当前选中的订单 ID 一起提交到 `/customer/after-sales`。后端 `CustomerAfterSaleController` 先校验当前必须是顾客，然后调用 `AfterSaleApplicationServiceImpl.create`。Service 会查 `demo_order`，判断订单是不是当前顾客的、有没有支付、有没有进行中的售后，金额是否超过订单金额。通过后插入 `after_sale_application`，同时写入 `after_sale_process_log`，并同步更新订单的售后状态。这样老师看数据库时能看到主表、日志表和订单状态是一致的。

### 3.3 顾客查看售后列表和详情

页面右侧有“我的售后”区域，查询条件是：

```js
const afterSaleQuery = reactive({ page: 1, pageSize: 8, status: '' })
const afterSales = ref([])
const afterSaleTotal = ref(0)
const selectedAfterSale = ref(null)
```

状态下拉框绑定 `afterSaleQuery.status`，点击查询调用 `loadAfterSales()`。API 是：

```js
pageCustomerAfterSales(params) -> GET /customer/after-sales
```

后端 `CustomerAfterSaleController.page` 会从 token 中取当前顾客，并强制设置 `search.userId = customer.getId()`，然后调用 `AfterSaleApplicationServiceImpl.page(search)`。Service 中先执行 `PageHelper.startPage(search.getPage(), search.getPageSize())`，再调用 Mapper 查询列表，最后用 `page.getTotal()` 和 `page.getResult()` 组装 `PageResult`。

Mapper XML 是 `AfterSaleApplicationMapper.xml`。它的 `BaseSelect` 从 `after_sale_application a` 查询，并关联：

```text
left join demo_order o on a.order_id=o.id
left join user_account u on a.user_id=u.id
left join user_account assignee on a.assigned_to=assignee.id
left join service_ticket t on a.ticket_id=t.id
```

`SearchWhere` 支持 `userId`、`orderId`、`status`、`serviceType`、`priority`、`assignedTo`、`keyword` 等条件。XML 的 `page` 查询保留关联字段和 `order by a.updated_at desc, a.id desc`，不再手写分页 `limit`。分页返回 `applicationNo`、订单号、商品名、顾客名、处理人、工单号等展示字段。

点击某一条售后记录时，前端触发 `selectAfterSale(row)`，调用：

```js
getCustomerAfterSale(row.id) -> GET /customer/after-sales/{id}
```

后端 `CustomerAfterSaleController.getById` 调用 Service 查询详情后，还会校验当前顾客只能访问自己的售后。Service 的 `getById(id)` 会查询主表，再查：

```text
processLogMapper.listByApplicationId(id)
evidenceMapper.listByApplicationId(id)
hydrateCustomerResult(application)
```

所以前端详情区域可以展示：

| 页面区域 | 数据来源 |
| --- | --- |
| 售后类型、优先级、金额、SLA | `after_sale_application` |
| 最终处理结果 | Service 通过 `hydrateCustomerResult` 补充 |
| 进度时间线 | `after_sale_process_log` |
| 凭证列表 | `after_sale_evidence` |
| 评价信息 | `service_review` |

老师问售后详情怎么做，我可以这样回答：

> 售后详情不是前端自己拼出来的。顾客点击售后单后，前端调用 `/customer/after-sales/{id}`。后端 Service 先查售后主表，再按申请 ID 查处理日志和凭证列表，并生成给顾客看的结果摘要和下一步动作。前端拿到对象后，把 `selectedAfterSale` 设置成返回值，详情卡片、时间线、凭证列表和结果区域都绑定这个对象，所以数据一回来页面就自动更新。

### 3.4 顾客补充凭证

详情页点击“补充凭证”打开弹窗，表单绑定：

```js
const evidenceForm = reactive({
  evidenceType: 'TEXT',
  fileUrl: '',
  content: ''
})
```

凭证类型可以是 `TEXT`、`LOGISTICS_NO`、`IMAGE`。点击提交调用 `submitEvidence()`，它调用：

```js
addCustomerAfterSaleEvidence(id, data) -> POST /customer/after-sales/{id}/evidence
```

后端 `CustomerAfterSaleController.addEvidence` 接收 `@PathVariable Long id` 和 `@RequestBody AfterSaleEvidenceRequest evidenceRequest`。Service 的 `addEvidence` 会校验：

| 校验 | 说明 |
| --- | --- |
| 售后单存在 | `getById(id)` |
| 顾客只能给自己的售后补凭证 | 比较 `application.userId` 和 `customer.id` |
| 终态不能补凭证 | `REJECTED/COMPLETED/CANCELLED` 不允许 |
| 凭证类型合法 | `IMAGE/VIDEO/TEXT/LOGISTICS_NO` |
| 凭证内容必填 | `cleanRequired` |

然后插入 `after_sale_evidence`，并写一条 `SUPPLEMENT_EVIDENCE` 到 `after_sale_process_log`。返回后前端关闭弹窗，再重新查询当前售后详情，时间线和凭证列表同步刷新。

老师问补充凭证怎么做，我可以这样回答：

> 补充凭证是单独接口，不是改售后主表。前端弹窗用 `evidenceForm` 收集凭证类型和内容，请求 `/customer/after-sales/{id}/evidence`。后端 Service 判断这个售后是不是当前顾客自己的、状态是否还允许补充，然后插入 `after_sale_evidence`，同时在 `after_sale_process_log` 写一条补充凭证日志。这样管理员审核时能看到凭证，顾客详情页也能看到时间线。

### 3.5 顾客评价服务

当售后状态是 `COMPLETED` 且还没有评价时，`canReviewSelected` 为真，页面显示“评价服务”按钮。弹窗表单绑定：

```js
const reviewForm = reactive({
  rating: 5,
  tags: '',
  comment: ''
})
```

提交时调用：

```js
createCustomerAfterSaleReview(id, data) -> POST /customer/after-sales/{id}/reviews
```

后端入口是 `ServiceReviewController.create`。Controller 从 `HttpServletRequest` 取 token，调用 `authService.requireCustomer`，然后交给 `ServiceReviewServiceImpl.create(applicationId, request, customer)`。

Service 会查售后申请，确认是当前顾客自己的售后，并把评分、标签、评论插入 `service_review`。同时它会写入 `after_sale_process_log`，动作是 `SUBMIT_REVIEW`。查询评价时走 `GET /customer/after-sales/{applicationId}/reviews`，Mapper 是 `ServiceReviewMapper.getByApplicationId`。

老师问评价怎么做，我可以这样回答：

> 评价功能挂在售后闭环最后。前端只有在售后完成并且没有评价时才显示评价按钮。提交时把评分、标签和评论发到 `/customer/after-sales/{id}/reviews`。后端先校验售后单属于当前顾客，再往 `service_review` 插入评价，并往处理日志写 `SUBMIT_REVIEW`。管理员客户画像里后面也会统计这些评价，所以它不是一个孤立表单。

## 4. 管理员售后审核工作台

管理员审核页是 `web/src/views/AdminAfterSaleReviewView.vue`，路由是 `/admin/after-sales/review`。这是管理员处理真实售后申请的核心页面。

### 4.1 审核队列查询

页面顶部有指标卡片，中间是审核队列表格，右侧是详情和处理面板。查询条件是：

```js
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', priority: '' })
const applications = ref([])
const total = ref(0)
const selected = ref(null)
```

搜索框绑定 `query.keyword`，状态下拉绑定 `query.status`，优先级下拉绑定 `query.priority`。点击查询调用 `loadApplications()`，API 是：

```js
pageAdminAfterSales(params) -> GET /admin/after-sales
```

后端 `AdminAfterSaleController.page(AfterSaleApplicationSearch search)` 接收查询参数，调用 `AfterSaleApplicationServiceImpl.page(search)`。Mapper 的 `AfterSaleApplicationMapper.xml` 使用同一个 `SearchWhere`，但管理员不强制 `userId`，所以可以按状态、优先级、关键字查询全部售后单。

前端返回后写：

```js
applications.value = data.rows || []
total.value = data.total || 0
```

表格通过 `:data="applications"` 展示售后单号、订单号、商品、类型、状态、优先级、SLA 截止时间。顶部 `pendingCount`、`moreEvidenceCount`、`highPriorityCount` 是 `computed`，直接从当前列表统计。

老师问管理员查询怎么做，我可以这样回答：

> 管理员审核台的查询条件在前端 `query` 对象里，包含关键词、状态、优先级和分页。点击查询后调用 `/admin/after-sales`。后端 Controller 接收 `AfterSaleApplicationSearch`，Service 调用 Mapper 的 count 和 page。XML 里从 `after_sale_application` 主表查询，并关联订单、顾客、处理人和工单表，所以前端表格能直接展示订单号、商品名、顾客名、工单状态等字段。

### 4.2 查看售后详情

管理员点击表格行时调用 `selectApplication(row)`，前端请求：

```js
getAdminAfterSale(row.id) -> GET /admin/after-sales/{id}
```

后端 `AdminAfterSaleController.getById` 调用 `AfterSaleApplicationServiceImpl.getById(id)`。和顾客详情一样，Service 会查主表、日志和凭证，只是管理员没有“只能看自己”的限制。

页面详情用 `selected` 渲染：

| 页面组件 | 数据字段 |
| --- | --- |
| `el-descriptions` | 顾客、售后类型、申请金额、风险等级、原因、AI 摘要 |
| 凭证列表 | `selected.evidences` |
| 处理时间线 | `selected.processLogs` |
| 关联工单提示 | `selected.ticketNo` |
| AI 草稿列表 | `replyDrafts` |

`hydrateDecisionForm()` 会把申请金额带入 `decisionForm.approvedAmount`，这样管理员审核通过时默认金额不需要重新输入。

老师问管理员详情怎么做，我可以这样回答：

> 管理员点击一条售后单后，前端拿 ID 请求 `/admin/after-sales/{id}`。后端 Service 查询售后主表，然后把凭证和流程日志一起查出来。前端把返回对象放到 `selected`，所以右侧详情、凭证、日志时间线、AI 摘要和关联工单都来自同一个真实详情接口。

### 4.3 审核通过、驳回、要求补材料、确认完成

管理员处理表单绑定：

```js
const decisionForm = reactive({ approvedAmount: 0, remark: '' })
```

审核按钮分别调用：

```js
approveSelected() -> POST /admin/after-sales/{id}/approve
rejectSelected() -> POST /admin/after-sales/{id}/reject
requestEvidenceSelected() -> POST /admin/after-sales/{id}/request-evidence
completeSelected() -> POST /admin/after-sales/{id}/complete
```

这些 API 在 `adminAfterSaleApi.js`，后端都在 `AdminAfterSaleController.java`。Controller 方法都接收 `@PathVariable Long id`、`@RequestBody AfterSaleActionRequest actionRequest` 和 `HttpServletRequest request`。它会通过 `authService.requireAdmin(...)` 获取管理员账号，再调用 Service。

Service 处理规则集中在 `AfterSaleApplicationServiceImpl`：

| 动作 | Service 方法 | 关键规则 |
| --- | --- | --- |
| 通过 | `approve` | 只能处理 `SUBMITTED/UNDER_REVIEW/NEED_MORE_EVIDENCE`，通过金额不能超过申请金额 |
| 驳回 | `reject` | 驳回必须有备注，状态改为 `REJECTED` |
| 要求补材料 | `requestEvidence` | 必须填写补充说明，状态改为 `NEED_MORE_EVIDENCE` |
| 完成 | `complete` | 状态改为 `COMPLETED`，设置关闭时间 |

每个动作都会调用 `applicationMapper.updateDecision(update)` 更新 `after_sale_application`，调用 `writeLog(...)` 往 `after_sale_process_log` 写动作日志，并调用 `orderMapper.updateAfterSaleStatus(...)` 同步 `demo_order.after_sale_status`。

状态流转不是前端随便改的。Service 里有：

```text
REVIEWABLE_STATUSES = SUBMITTED / UNDER_REVIEW / NEED_MORE_EVIDENCE
TERMINAL_STATUSES = REJECTED / COMPLETED / CANCELLED
```

所以已经终态的售后不能再审核。

老师问审核动作怎么做，我可以这样回答：

> 管理员审核不是前端直接改状态。前端只是把审核金额和备注提交到对应动作接口，例如 `/admin/after-sales/{id}/approve`。后端 Controller 校验管理员身份后交给 Service。Service 先判断当前状态是否允许审核，再校验金额或备注，然后更新售后主表、写处理日志、同步订单售后状态。最后重新查询详情返回给前端。这样每次状态变化都有数据库日志，不会只留下一个状态字段。

### 4.4 创建关联人工工单

管理员在售后详情中点击“创建关联工单”，前端调用：

```js
createAfterSaleTicket(id, data) -> POST /admin/after-sales/{id}/tickets
```

后端 `AdminAfterSaleController.createTicket` 调用 `AfterSaleApplicationServiceImpl.createTicket`。Service 逻辑是：

1. 查询售后详情。
2. 如果 `ticketId` 已存在，就返回已有工单，避免重复创建。
3. 创建一条 `ChatSession`，渠道设为 `ADMIN_TEST`，摘要说明这是由售后申请转人工。
4. 创建一条 `ServiceTicket`，写入 `ticketNo`、`sessionId`、`orderId`、`userId`、`intentCode`、`priority`、`customerIssue`、`aiSummary`、`suggestedAction`。
5. 调用 `ticketMapper.insert(ticket)` 插入 `service_ticket`。
6. 调用 `applicationMapper.bindTicket(id, ticket.getId())` 把售后申请和工单绑定。
7. 写入 `CREATE_TICKET` 处理日志。

前端创建成功后重新选择当前售后，详情区域会显示 `ticketNo` 和工单状态。

老师问售后怎么转工单，我可以这样回答：

> 我没有把售后和工单做成两个割裂模块。管理员在售后详情里点创建工单后，后端会先判断这个售后是否已有工单，没有才创建会话和工单。工单插入 `service_ticket` 后，再把工单 ID 回写到 `after_sale_application.ticket_id`，并写一条流程日志。这样从售后单能追到工单，从工单也能知道来源会话和订单。

### 4.5 AI 回复草稿

管理员在售后详情页可以生成回复草稿。前端 `generateDraft()` 调用：

```js
generateReplyDraft(id, data) -> POST /admin/after-sales/{id}/reply-drafts
listReplyDrafts(id) -> GET /admin/after-sales/{id}/reply-drafts
useReplyDraft(id, draftId, data) -> POST /admin/after-sales/{id}/reply-drafts/{draftId}/use
discardReplyDraft(id, draftId, data) -> POST /admin/after-sales/{id}/reply-drafts/{draftId}/discard
```

后端入口有两个：

| Controller | 职责 |
| --- | --- |
| `AdminReplyDraftController` | 列表、生成、采纳、废弃草稿 |
| `ReplyDraftServiceImpl` | 真正生成草稿、写库、写处理日志 |

生成草稿时，Service 做了三步：

1. `applicationService.getById(applicationId)` 查询售后详情，包括凭证和日志。
2. `knowledgeDocService.search(application.getReasonText(), toIntentCode(application.getServiceType()), 3)` 检索知识库。
3. 先用 `buildTemplateDraft` 生成本地兜底草稿，再用 `buildPrompt` 调用 `aiService.generate(prompt)`。

如果 AI 可用，就保存 AI 回复；如果 AI 跳过或失败，就保存本地模板草稿。草稿写入 `reply_draft` 表，字段包括 `draftContent`、`sourceType`、`status`、`riskLevel`、`knowledgeRefs`、`aiStatus`、`aiProvider`、`aiModelName`。生成、采纳、废弃都会写入 `after_sale_process_log`，动作分别是 `GENERATE_REPLY_DRAFT`、`USE_REPLY_DRAFT`、`DISCARD_REPLY_DRAFT`。

老师问 AI 草稿怎么做，我可以这样回答：

> AI 回复草稿不是让模型直接决定售后结果。后端先查售后单、凭证、处理日志，再检索知识库，把这些内容组成 prompt，并明确告诉 AI 只能生成客服回复草稿，不能承诺退款或修改状态。如果 AI 成功就保存 AI 草稿，如果 AI 不可用就保存本地模板草稿。管理员在前端可以采纳或废弃，采纳和废弃都会写处理日志，所以 AI 只是辅助层，最终动作仍然由管理员确认。

## 5. SLA 中心

SLA 页面是 `web/src/views/SlaCenterView.vue`，路由是 `/admin/sla`。

### 前端布局和参数

页面顶部是 SLA 指标卡，下面是风险任务表格。查询条件是：

```js
const query = reactive({ page: 1, pageSize: 10, keyword: '', riskType: '', status: '' })
const tasks = ref([])
const total = ref(0)
```

`riskType` 下拉支持 `OVERDUE`、`DUE_SOON`、`HIGH_PRIORITY`、`WAITING_CUSTOMER`。点击查询调用 `loadTasks()`，API 是：

```js
pageSlaTasks(params) -> GET /admin/sla/tasks
```

### 后端和 SQL

Controller 是 `SlaTaskController.tasks(SlaTaskSearch search)`。Service 是 `SlaTaskServiceImpl.page(search)`。Mapper 是 `SlaTaskMapper`，XML 是 `SlaTaskMapper.xml`。

SLA 任务列表同样由 Service 先调用 `PageHelper.startPage(search.getPage(), search.getPageSize())`，再调用 XML 查询。查出的每条记录会在 Service 里补充 `remainingHours` 和 `riskLabel`，所以页面看到的不只是数据库原始字段，还有后端计算出的风险提示。

XML 的 `BaseFrom` 从 `after_sale_application a` 查询，并关联 `demo_order`、`user_account`。`SearchWhere` 有一个关键条件：

```sql
where a.status not in ('COMPLETED', 'CANCELLED', 'REJECTED')
```

也就是说 SLA 中心只看还没有结束的售后。`riskType` 会筛选超时、即将超时、高优先级、待顾客补材料等风险。Service 的 `enrich(task, now)` 会根据 `slaDeadline` 和当前时间计算 `riskLabel`、`remainingHours`。

### 前端返回绑定

返回的 `PageResult<SlaTask>` 写入 `tasks` 和 `total`。顶部指标 `overdueCount`、`dueSoonCount`、`highPriorityCount` 都是 computed，从当前 `tasks` 中统计。表格的“处理”按钮调用 `goReview(row)`，跳转到 `/admin/after-sales/review` 并带上售后单号或 ID，让管理员回到审核台处理。

老师问 SLA 怎么做，我可以这样回答：

> SLA 中心不是单独造假数据，它直接从 `after_sale_application` 查未完结的售后申请。前端用 `query.riskType` 选择已超时、即将超时、高优先级等条件，请求 `/admin/sla/tasks`。后端 Mapper 关联订单和顾客表查出任务，Service 根据 `slaDeadline` 计算剩余小时和风险标签。前端把结果显示成风险任务表，点击处理会跳回售后审核台。

## 6. 客户画像

客户画像页面是 `web/src/views/AdminCustomerProfileView.vue`，路由是 `/admin/customers/profile`。

### 前端布局和参数

页面顶部用 `queryUserId` 输入客户 ID，点击“查询客户”调用 `loadProfile(userId)`。页面包括指标卡、客户基础信息、评价列表、近期售后、近期订单、近期工单。

前端 API 在 `customerProfileApi.js`：

```js
getCustomerProfile(userId) -> GET /admin/customers/{userId}/profile
listCustomerReviews(userId) -> GET /admin/customers/{userId}/reviews
```

当前页面主要使用 `getCustomerProfile`。

### 后端 Service 和数据库

Controller 是 `ServiceReviewController.customerProfile(@PathVariable Long userId)`。业务在 `ServiceReviewServiceImpl.customerProfile(userId)`。它把多个模块的数据汇总成 `CustomerProfile`：

| 指标 | 来源 |
| --- | --- |
| 客户账号 | `UserAccountMapper.getById` |
| 订单数、订单金额 | `DemoOrderMapper.count/sumAmount` |
| 售后数、活跃售后、重复售后 | `AfterSaleApplicationMapper` |
| 工单数 | `ServiceTicketMapper.countByUserId` |
| 评价数、低分数、平均评分、低分原因 | `ServiceReviewMapper` |
| 近期订单、近期售后、近期工单、评价列表 | 各自 Mapper 分页或列表 |

Service 还会根据投诉、重复售后、低评分等计算 `riskLevel` 和 `operationSuggestion`。

### 前端返回绑定

前端把返回值写入：

```js
profile.value = data
```

页面通过 `profile.customer`、`profile.orderCount`、`profile.totalOrderAmount`、`profile.recentAfterSales`、`profile.recentOrders`、`profile.recentTickets`、`profile.reviews` 渲染。`averageRating` 和 `riskText` 是 computed，用来格式化评分和风险文案。

老师问客户画像怎么做，我可以这样回答：

> 客户画像是一个聚合接口。前端只传客户 ID 到 `/admin/customers/{userId}/profile`，后端 Service 会分别查用户、订单、售后、工单和评价表，然后把订单数、售后数、投诉数、低分评价、近期记录都放到 `CustomerProfile` 对象。页面拿到这个对象后，指标卡、评价列表、近期售后和近期工单都是直接绑定这些字段。

## 7. 聊天工作台和 AI 售后辅助

聊天页是 `web/src/views/ChatWorkbenchView.vue`，路由是 `/chat`。它既能给顾客在线咨询，也能给管理员演示 AI 辅助、知识库命中、流程追踪和工单转接。

### 7.1 会话创建和列表

页面左侧是会话列表和订单绑定区。状态包括：

```js
const orderNo = ref('DD202604290001')
const selectedChannel = ref('WEB')
const channelFilter = ref('ALL')
```

点击新建会话或绑定订单时调用 `newSession()`，内部使用 `chatStore.startSession({ orderNo, channel })`。Pinia Store 在 `web/src/stores/chatStore.js`，创建会话调用：

```js
createSession(data) -> POST /chat-sessions
pageSessions(params) -> GET /chat-sessions
getSession(id) -> GET /chat-sessions/{id}
deleteSession(id) -> DELETE /chat-sessions/{id}
```

后端 Controller 是 `ChatSessionController`。创建会话时接收 `@RequestBody ChatSession session`，根据 token 补充当前用户，Service 是 `ChatServiceImpl.save(session)`。会话列表走 `ChatServiceImpl.page(search)`，它也使用 `PageHelper.startPage(search.getPage(), search.getPageSize())`，再调用 `ChatSessionMapper.page(search)` 查询会话、订单和用户关联信息。

`ChatServiceImpl.save` 会生成 `sessionNo`，没有标题就设置默认标题，有订单号时调用 `DemoOrderMapper.getByOrderNo` 绑定订单 ID。然后 `ChatSessionMapper.insert(session)` 插入 `chat_session`。

### 7.2 发送消息和 AI 回复

页面中间是消息区，底部输入框绑定：

```js
const draft = ref('这个订单能不能退货？')
const useAi = ref(true)
```

点击发送调用 `submit()`，再调用 `chatStore.ask(draft.value, orderNo.value, useAi.value)`。Store 里会先插入临时用户消息和“正在分析”的临时助手消息，给页面一个流式体验。真正请求走：

```js
sendMessageStream(id, data, handlers) -> POST /chat-sessions/{id}/message-stream
```

后端 `ChatSessionController.sendMessageStream` 返回 `SseEmitter`；普通非流式接口也有：

```js
sendMessage(id, data) -> POST /chat-sessions/{id}/messages
```

核心业务在 `ChatServiceImpl.sendMessage(id, request)`。流程是：

1. `requireSession(id)` 校验会话存在且未关闭。
2. `resolveOrder(session, request.getOrderNo())` 根据订单号或会话绑定订单查询 `demo_order`。
3. `messageMapper.listRecentBySessionId(id, 8)` 查询最近消息，用来判断追问上下文。
4. 插入用户消息到 `chat_message`。
5. `recognizeIntent(...)` 做规则意图识别，并把结果插入 `intent_record`。
6. 根据意图和问题调用知识库检索，命中结果写入 `retrieval_log`。
7. `buildReply(...)` 生成本地规则兜底回复。
8. `buildBusinessToolEvidence(...)` 把订单查询、知识检索、工单能力组织成工具证据。
9. `buildAiPrompt(...)` 组装 prompt。
10. 如果 `useAi` 为真，调用 `aiService.generate(aiPrompt)`；否则直接跳过 AI，使用本地兜底。
11. 调用 `aiCallLogMapper.insert(aiService.toLog(...))` 记录 AI 调用。
12. 插入助手消息到 `chat_message`。
13. `handleTicketHandoff(...)` 判断是否需要创建人工工单。
14. `sessionMapper.updateSummary(...)` 更新会话摘要和当前意图。
15. 多个关键节点通过 `trace(...)` 写入 `process_trace`。

返回给前端的数据包含用户消息、助手消息、意图、订单上下文、知识库命中、AI 状态、工单信息、建议追问等。前端 `chatStore.ask` 收到结果后替换临时消息：

```js
messages.value = messages.value
  .filter(message => message.id !== tempUserId && message.id !== tempAssistantId)
  .concat(persistedMessages)
insight.value = data
```

右侧洞察面板绑定 `chatStore.insight`，展示意图、上下文、订单状态、知识库命中、AI 状态、业务工具和建议追问。

老师问聊天 AI 怎么做，我可以这样回答：

> 聊天不是直接把问题丢给模型。前端发送消息时，先把内容、订单号、是否启用 AI 发到 `/chat-sessions/{id}/message-stream`。后端 Service 会保存用户消息，识别意图，查订单，检索知识库，生成本地兜底回复，再根据配置调用 LangChain4j 的 AI 服务。无论 AI 成功还是失败，系统都会把 AI 调用写入 `ai_call_log`，把知识库命中写入 `retrieval_log`，把处理步骤写入 `process_trace`。前端右侧洞察面板展示这些结果，所以老师能看到 AI 的依据和流程，而不是黑盒回答。

### 7.3 人工工单转接

`ChatServiceImpl.handleTicketHandoff` 会根据用户问题、意图和订单状态判断是否需要人工介入。如果需要，就调用：

```text
ServiceTicketService.createFromSession(...)
```

`ServiceTicketServiceImpl.createFromSession` 会创建或复用工单，插入 `service_ticket`，字段包括 `ticketNo`、`sessionId`、`messageId`、`orderId`、`userId`、`intentCode`、`priority`、`status`、`customerIssue`、`aiSummary`、`suggestedAction`。

前端右侧如果收到 `ticket`，就显示工单信息。后续工单页面也能查到这条记录。

老师问聊天怎么转人工，我可以这样回答：

> 后端会根据投诉、物流异常、高风险问题等规则判断是否需要人工。需要时调用 `ServiceTicketService.createFromSession`，把当前会话、订单、消息、意图、用户问题和 AI 摘要都写入 `service_ticket`。所以工单不是管理员手工造的，也不是前端假展示，而是聊天过程中由业务规则创建出来的。

### 7.4 证据报告和流程追踪

聊天页点击“证据报告”调用：

```js
downloadEvidenceReport(id) -> GET /chat-sessions/{id}/evidence-report
```

后端 `ChatServiceImpl.buildEvidenceReport(id)` 会查询：

```text
chat_session
demo_order
chat_message
intent_record
retrieval_log
ai_call_log
service_ticket
process_trace
```

然后生成 Markdown 文本，返回 `text/markdown`。日志中心也可以用 `listTraces(id)` 查询 `GET /chat-sessions/{id}/process-traces`，展示每一步处理过程。

老师问证据报告怎么做，我可以这样回答：

> 证据报告是从真实日志生成的。后端根据会话 ID 查询消息、意图识别、知识检索、AI 调用、工单和处理轨迹，然后拼成 Markdown 返回。这样答辩时可以证明一次 AI 回复背后查了什么订单、命中了什么知识、是否调用模型、是否创建工单。

## 8. 知识库管理和搜索

知识库页面是 `web/src/views/KnowledgeDocView.vue`，路由是 `/knowledge`。

### 前端布局和参数

页面左侧是知识文档列表，右侧是搜索调试区。列表查询条件：

```js
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', intentCode: '' })
const docs = ref([])
const categories = ref([])
const total = ref(0)
```

新增或编辑弹窗绑定 `form`，字段包含分类、标题、文档类型、意图编码、场景、常见问法、标准答复、详细内容、关键词、优先级、状态等。搜索调试区用 `searchText`、`searchIntent`、`searchHits` 保存查询词、意图和命中结果。

API 在 `knowledgeApi.js`：

```js
listCategories(params) -> GET /knowledge-categories
createCategory(data) -> POST /knowledge-categories
updateCategory(id, data) -> PUT /knowledge-categories/{id}
deleteCategory(id) -> DELETE /knowledge-categories/{id}
pageKnowledgeDocs(params) -> GET /knowledge-docs
createKnowledgeDoc(data) -> POST /knowledge-docs
getKnowledgeDoc(id) -> GET /knowledge-docs/{id}
updateKnowledgeDoc(id, data) -> PUT /knowledge-docs/{id}
deleteKnowledgeDoc(id) -> DELETE /knowledge-docs/{id}
searchKnowledgeDocs(params) -> GET /knowledge-docs/search
```

### 后端和 MyBatis

分类 Controller 是 `KnowledgeCategoryController`，Service 是 `KnowledgeCategoryServiceImpl`，Mapper XML 是 `KnowledgeCategoryMapper.xml`，操作 `knowledge_category` 表。

文档 Controller 是 `KnowledgeDocController`，Service 是 `KnowledgeDocServiceImpl`，Mapper XML 是 `KnowledgeDocMapper.xml`，操作 `knowledge_doc` 表。

分页查询由 `KnowledgeDocServiceImpl.page(search)` 发起，Service 先调用 PageHelper，再调用 `KnowledgeDocMapper.page(search)`。`KnowledgeDocMapper.xml` 支持：

```text
categoryId
docType
intentCode
status
keyword
```

并且 `left join knowledge_category c on d.category_id=c.id`，所以返回时能带上分类名。XML 负责动态条件和排序，`PageResult` 的 `total/rows` 由 Service 层根据 PageHelper 结果组装。删除不是物理删除，而是 `softDelete`，把 `deleted=1`。

搜索接口 `GET /knowledge-docs/search` 调用 `KnowledgeDocServiceImpl.search(query, intentCode, limit)`。如果没传意图，Service 会用 `inferIntent(query)` 根据关键词推断意图；然后 XML 的 `search` 方法在启用状态的文档中按 `intentCode`、问题、答案、内容、关键词匹配，并按 `priority desc, updated_at desc` 排序。Service 的 `enrichHits` 会给每个命中文档补充 `score`、`rankNo`、`hitReason`、`contentPreview`。

### 前端返回绑定

列表查询把返回的 `rows` 写入 `docs`，`total` 写入分页。搜索把返回数组写入 `searchHits`，右侧页面显示命中文档标题、意图、分数、命中原因和摘要。`decoratedHits` 是 computed，用来加工展示命中理由。

老师问知识库怎么做，我可以这样回答：

> 知识库分为分类表和文档表。前端列表页用 `query` 保存关键词、状态和意图编码，调用 `/knowledge-docs` 分页查询。后端 Mapper 从 `knowledge_doc` 查询并关联分类表。搜索时调用 `/knowledge-docs/search`，Service 会根据用户输入推断意图，然后用 MyBatis 查询启用的知识文档，按优先级排序，并给每条结果补充分数和命中原因。聊天模块和 AI 草稿模块都会复用这个搜索能力。

## 9. 订单管理

订单管理页面是 `web/src/views/OrderView.vue`，路由是 `/orders`。

### 前端布局和参数

页面上方是查询条件：关键词、订单状态、售后状态。状态保存在：

```js
const query = reactive({ page: 1, pageSize: 10, keyword: '', orderStatus: '', afterSaleStatus: '' })
const orders = ref([])
const selected = ref(null)
const afterSales = ref([])
```

管理员可以新增或编辑订单，弹窗表单 `orderForm` 包含订单号、用户 ID、商品、规格、金额、支付状态、订单状态、物流状态、售后状态、支付时间、发货时间、签收时间。

订单详情下方还有旧版售后记录表，调用 `listOrderAfterSales(id)` 查询 `/orders/{id}/after-sale-records`。这部分保留是为了兼容原有订单售后记录，当前真实闭环主要使用新的 `after_sale_application`。

API 在 `orderApi.js`：

```js
pageOrders(params) -> GET /orders
getOrder(id) -> GET /orders/{id}
getOrderByNo(orderNo) -> GET /orders/no/{orderNo}
createOrder(data) -> POST /orders
updateOrder(id, data) -> PUT /orders/{id}
deleteOrder(id) -> DELETE /orders/{id}
listOrderAfterSales(id) -> GET /orders/{id}/after-sale-records
createOrderAfterSale(id, data) -> POST /orders/{id}/after-sale-records
```

### 后端 Service 和 XML

Controller 是 `OrderController`，Service 是 `OrderServiceImpl`，Mapper 是 `DemoOrderMapper`，XML 是 `DemoOrderMapper.xml`。

列表查询从 `demo_order` 表读取数据，支持 `userId`、`orderStatus`、`logisticsStatus`、`afterSaleStatus`、`keyword`。分页由 `OrderServiceImpl.page` 中的 PageHelper 统一处理，XML 只保留条件和排序。新增订单调用 `insert`，更新订单调用 `update`，删除调用 `delete`。

如果顾客访问订单接口，`OrderController` 会根据 token 限制 `userId`；查看详情、按订单号查询、查看订单售后记录和发起订单售后时，会通过 `authService.ensureSelfOrAdmin(...)` 校验订单归属。管理员能增删改查。这个权限边界保证顾客端订单列表和管理员端订单管理复用同一个接口，但展示范围不同。

### 前端返回绑定

`loadOrders()` 把 `rows` 写入 `orders`。点击一行 `selectOrder(row)` 会设置 `selected`，再调用 `listOrderAfterSales(row.id)`，把旧售后记录写入 `afterSales`。新增或编辑成功后关闭弹窗并重新加载列表。

老师问订单管理怎么做，我可以这样回答：

> 订单管理前端用 `query` 收集订单号、商品名、订单状态和售后状态，然后调用 `/orders`。后端 `OrderController` 接收 `OrderSearch`，顾客身份会自动加上自己的 `userId`，管理员不限制。Service 用 `PageHelper.startPage` 控制分页，再调用 `DemoOrderMapper`，XML 从 `demo_order` 表按条件查询和排序。新增、编辑、删除订单分别走 POST、PUT、DELETE，最后前端重新加载列表。订单详情里还能查这个订单关联的旧售后记录，方便兼容原始功能。

## 10. 工单管理

工单页面是 `web/src/views/ServiceTicketView.vue`，路由是 `/service-tickets`。

### 前端布局和参数

页面上方是查询区，条件保存在：

```js
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', priority: '' })
const tickets = ref([])
const selected = ref(null)
```

表格展示工单号、订单号、意图、优先级、状态、用户问题、处理人、创建时间。点击一行后 `selectTicket(row)` 设置当前工单。详情区域里 `selected.status` 绑定到状态下拉，点击保存调用 `saveStatus()`。

API 在 `serviceTicketApi.js`：

```js
pageTickets(params) -> GET /service-tickets
getTicket(id) -> GET /service-tickets/{id}
updateTicket(id, data) -> PUT /service-tickets/{id}
deleteTicket(id) -> DELETE /service-tickets/{id}
listSessionTickets(sessionId) -> GET /chat-sessions/{sessionId}/service-tickets
createSessionTicket(sessionId, data) -> POST /chat-sessions/{sessionId}/service-tickets
```

### 后端 Service 和 Mapper

Controller 是 `ServiceTicketController`。分页查询走 `ServiceTicketServiceImpl.page(search)`，Service 先调用 PageHelper，再调用 `ServiceTicketMapper.page(search)`；更新状态走 `ServiceTicketServiceImpl.update(id, ticket)`。

Mapper XML 是 `ServiceTicketMapper.xml`。分页查询从 `service_ticket t` 查询，并关联：

```text
left join chat_session s on t.session_id=s.id
left join demo_order o on t.order_id=o.id
where t.deleted=0
```

查询条件支持 `sessionId`、`orderId`、`status`、`priority`、`intentCode`、`keyword`。XML 保留关联查询、动态条件和排序，分页结果由 PageHelper 生成。删除是软删除，`delete` 方法执行 `update service_ticket set deleted=1 where id=#{id}`。

工单也可以从聊天或售后创建。聊天创建走 `createFromSession`，售后创建走 `AfterSaleApplicationServiceImpl.createTicket`。这让工单既可以服务聊天转人工，也可以服务售后审核升级。

### 前端返回绑定

列表查询后写入 `tickets` 和 `total`。状态保存成功后重新调用 `getTicket(selected.id)` 或 `loadTickets()`，让详情与表格同步。页面上的 `slaRisk`、`nextAction`、`timelineSteps` 是 computed，根据工单状态、优先级和时间生成处理提示。

老师问工单怎么做，我可以这样回答：

> 工单表是 `service_ticket`。前端查询条件放在 `query`，调用 `/service-tickets`，后端 Mapper 关联会话和订单表返回工单列表。工单可以由聊天自动转人工创建，也可以由售后审核台创建。管理员在工单页修改状态时，前端把整个工单对象提交到 PUT `/service-tickets/{id}`，后端更新状态、处理人、解决时间等字段。删除是软删除，不直接删数据库记录。

## 11. 运营指挥中心

运营中心页面是 `web/src/views/OperationCenterView.vue`，路由是 `/operations`。它用于把系统的新增能力、运营指标和待处理动作集中展示。

前端状态：

```js
const insights = reactive({
  metrics: [],
  newFeatures: [],
  intentInsights: [],
  ticketInsights: [],
  channelInsights: [],
  knowledgeInsights: [],
  orderRiskInsights: [],
  aiInsights: [],
  actionItems: [],
  versionMilestones: []
})
```

点击刷新调用：

```js
getOperationInsights() -> GET /operation-insights
```

后端 Controller 是 `OperationInsightsController.insights()`，Service 是 `OperationInsightsServiceImpl.getInsights()`。这个 Service 使用 `JdbcTemplate` 和各类统计方法，生成 `OperationInsights` 对象。它不是单表 CRUD，而是根据订单、聊天、知识库、工单、AI 日志等数据生成运营看板。

前端拿到结果后用 `Object.assign(insights, data)` 更新响应式对象。页面用 `v-for` 渲染指标卡、新功能卡、意图分布、工单风险、渠道占比、知识命中、订单风险、AI 状态、行动项和版本里程碑。

老师问运营中心怎么做，我可以这样回答：

> 运营中心是一个聚合看板。前端只请求 `/operation-insights`，后端 `OperationInsightsServiceImpl` 会从多个业务表统计数据，例如工单状态、聊天渠道、知识库命中、AI 调用和订单风险，然后组装成 `OperationInsights`。前端按模块 `v-for` 渲染这些数组，所以它不是静态页面，而是由后端统计结果驱动。

## 12. 特色闭环中心

特色闭环页面是 `web/src/views/FeatureClosureView.vue`，路由是 `/feature-closures`。它主要用来证明项目不是散功能，而是形成了“功能闭环”。

前端状态：

```js
const dashboard = reactive({
  metrics: [],
  closures: [],
  demoSteps: [],
  references: []
})
```

API 在 `operationApi.js`：

```js
getFeatureClosures() -> GET /feature-closures
```

后端 Controller 是 `FeatureClosureController.dashboard()`，Service 是 `FeatureClosureServiceImpl.getDashboard()`。Service 会构造闭环指标、功能闭环列表、演示步骤和参考来源，同时通过 SQL 统计实际数据数量，比如知识库条数、AI 日志、处理轨迹、工单等。

前端用 `closedCount` 统计已经闭环的功能数量，用 `tagType(tone)` 映射展示颜色。页面本身不处理业务动作，但它把系统各模块是否形成闭环展示出来。

老师问这个页面有什么意义，我可以这样回答：

> 这个页面是项目自检页面。它不是核心业务入口，而是把订单、售后、聊天、知识库、工单、日志这些模块的闭环情况汇总出来。后端会统计真实数据，前端用卡片展示哪些能力已经闭环、每个能力的证据是什么，适合答辩时说明系统从 demo 升级成完整项目。

## 13. 日志中心和诊断

日志中心页面是 `web/src/views/LogCenterView.vue`，路由是 `/logs`。

### 前端布局和参数

页面包括诊断概览、风险信号、AI 调用日志、知识检索日志、处理轨迹三类 Tab。状态包括：

```js
const aiQuery = reactive({ page: 1, pageSize: 50, status: '' })
const retrievalQuery = reactive({ page: 1, pageSize: 50, keyword: '' })
const aiLogs = ref([])
const retrievalLogs = ref([])
const diagnostics = ref(null)
const traceSessionId = ref(null)
const traces = ref([])
```

API 在 `logApi.js`：

```js
getLogDiagnostics() -> GET /log-diagnostics
pageAiCallLogs(params) -> GET /ai-call-logs
pageRetrievalLogs(params) -> GET /retrieval-logs
listTraces(id) -> GET /chat-sessions/{id}/process-traces
```

### 后端 Service 和数据库

Controller 是 `LogController`。Service 是 `LogServiceImpl`。

| 页面数据 | Service 方法 | Mapper / 表 |
| --- | --- | --- |
| AI 调用日志 | `pageAiLogs` | `AiCallLogMapper` / `ai_call_log` |
| 知识检索日志 | `pageRetrievalLogs` | `RetrievalLogMapper` / `retrieval_log` |
| 日志诊断 | `getDiagnostics` | AI 日志、检索日志、处理轨迹综合分析 |
| 处理轨迹 | `ChatServiceImpl.listTraces` | `ProcessTraceMapper` / `process_trace` |

AI 调用日志和知识检索日志的分页也在 `LogServiceImpl` 中统一处理。Service 会对 `page/pageSize` 做默认值保护，然后调用 `PageHelper.startPage(currentPage, size)`，再分别调用 `AiCallLogMapper.page(status)` 和 `RetrievalLogMapper.page(keyword)`。XML 只负责按状态或关键词过滤并排序，不再传入 offset/limit 参数。

`LogServiceImpl.getDiagnostics()` 会计算 AI 成功数、失败数、跳过数、平均耗时、token 总数、主要模型、成功率趋势、知识库平均命中分数、热门命中文档、最近处理轨迹进度、风险信号和行动建议。

### 前端返回绑定

`refreshAll()` 会同时加载诊断、AI 日志和检索日志。页面的 `successCount`、`failedCount`、`healthLevel`、`riskSignals`、`actionItems` 等都是 computed，从 `diagnostics` 或日志数组里计算。处理轨迹 Tab 输入会话 ID 后调用 `loadTrace()`，把 `process_trace` 渲染为时间线。

老师问日志中心怎么做，我可以这样回答：

> 日志中心主要证明 AI 和知识库不是黑盒。聊天时后端会写 `ai_call_log`、`retrieval_log` 和 `process_trace`。日志页通过 `/ai-call-logs` 查模型调用，通过 `/retrieval-logs` 查知识命中，通过 `/log-diagnostics` 做健康诊断。前端把这些日志做成成功率、风险信号、热门命中文档和处理时间线，所以老师可以看到一次回复背后的证据。

## 14. AI 测试页和模型状态

AI 测试页是 `web/src/views/AiTestView.vue`，路由是 `/ai-test`。它用于单独验证后端模型调用是否正常。

前端用：

```js
const prompt = ref('请回复：后端已经通过 sub2api 调用真实模型。')
const result = ref(null)
```

点击“测试”调用：

```js
testAi(data) -> POST /ai-tests
```

后端 Controller 是 `AiTestController.test(@RequestBody AiTestRequest request)`，Service 是 `AiServiceImpl.test(prompt)`。`AiServiceImpl` 负责通过 LangChain4j 获取当前模型、调用模型、记录是否使用、是否兜底、Provider、模型名、耗时和错误信息。页面还会调用 `systemStore.loadStatus()` 获取系统 AI 状态。

系统状态 API 在 `systemApi.js`：

```js
getSystemStatus() -> GET /system/status
getEnums() -> GET /system/enums
getAiModels() -> GET /system/ai-models
switchAiModel(modelName) -> PUT /system/ai-models/current
```

后端是 `SystemController`，可以返回健康状态、AI 模型列表、枚举值，并允许管理员切换当前模型。

老师问 AI 测试页怎么做，我可以这样回答：

> AI 测试页是为了单独验证模型链路。前端输入 prompt，调用 `/ai-tests`，后端 `AiTestController` 把 prompt 交给 `AiServiceImpl.test`。Service 通过 LangChain4j 调用当前模型，如果模型没启用或调用失败，会返回兜底状态和错误信息。前端把 `used`、`fallbackUsed`、Provider、模型、耗时、错误都展示出来，所以能直接证明后端是否真正连到了模型。

## 15. 系统总览和答辩展示页

系统总览页是 `web/src/views/DashboardView.vue`，路由是 `/dashboard`。展示页是 `web/src/views/ShowcaseView.vue`，路由是 `/showcase`。

### DashboardView

Dashboard 主要调用 `systemStore.loadStatus()`，后端接口是：

```js
GET /system/status
```

页面展示 AI 状态、Provider、模型名、AI 是否启用、Key 是否配置、Base URL、数据库状态等。它的作用是让老师看到系统运行环境和后端服务状态。

### ShowcaseView

Showcase 是答辩演示入口。页面里有演示步骤、能力路线图、系统状态侧栏和版本计划。它也会调用 `systemStore.loadStatus()`，并提供按钮跳转到 `/chat`、`/logs`、`/operations` 等核心演示路径。

老师问这两个页面怎么做，我可以这样回答：

> Dashboard 是系统状态页，主要从 `/system/status` 拿 AI 和数据库健康信息。Showcase 是答辩演示入口，前端写了演示步骤和跳转按钮，同时也加载系统状态。它们不是主要业务处理页，但能帮助老师快速进入聊天、日志、运营中心等核心功能。

## 16. 公共前端状态和组件绑定方式

这个项目的前端主要有三类状态：

| 类型 | 用法 |
| --- | --- |
| `ref` | 保存列表、详情、加载状态、弹窗开关 |
| `reactive` | 保存查询条件和表单对象 |
| `computed` | 根据已有数据计算指标、按钮可用状态、风险标签 |

例如售后顾客页：

```text
orderQuery -> 订单查询参数
afterSaleQuery -> 售后查询参数
orders -> 订单表格数据
afterSales -> 售后表格数据
selectedAfterSale -> 详情区域数据
applyForm/evidenceForm/reviewForm -> 三个弹窗表单
```

所有页面都遵循同一模式：页面 `v-model` 修改查询对象或表单对象，点击按钮调用 `async function`，函数调用 API，返回后写入 `ref/reactive`，模板自动刷新。

公共状态主要有：

| Store | 文件 | 职责 |
| --- | --- | --- |
| `authStore` | `web/src/stores/authStore.js` | 保存 JWT、用户信息、登录状态、管理员判断 |
| `chatStore` | `web/src/stores/chatStore.js` | 保存会话、消息、AI 洞察、发送状态 |
| `systemStore` | `web/src/stores/systemStore.js` | 保存系统状态、枚举、AI 模型列表、当前模型 |

老师问前端数据怎么绑定，我可以这样回答：

> 我前端没有直接操作 DOM，而是用 Vue 3 的响应式数据。查询条件用 `reactive`，列表和详情用 `ref`，统计数字和按钮状态用 `computed`。例如售后列表返回后，我把 `rows` 写到 `afterSales.value`，表格 `:data="afterSales"` 就会自动更新。详情返回后写到 `selectedAfterSale.value`，页面里的描述项、时间线、凭证列表都会同步变化。

## 17. 后端分层和 MyBatis 设计

后端基本按四层走：

```text
Controller: 接收 HTTP 参数、读取 token、返回 Result
Service: 处理业务规则、权限、状态流转、AI 和兜底
Mapper Interface: 定义数据库访问方法
Mapper XML: 写 SQL 和动态查询条件
```

标准分页的职责现在放在 Service 层：`OrderServiceImpl`、`AfterSaleApplicationServiceImpl`、`KnowledgeDocServiceImpl`、`ServiceTicketServiceImpl`、`ChatServiceImpl`、`LogServiceImpl` 等都会先调用 `PageHelper.startPage(...)`，再调用 Mapper 的列表查询。Mapper XML 不再把主分页 `limit #{offset}, #{limit}` 写死在 SQL 里，而是专注于 `BaseSelect`、`SearchWhere`、关联表和排序。这样既保留 MyBatis XML 的可读性，又让分页入口和 `PageResult(total, rows)` 组装逻辑更统一。

几个代表性模块如下：

| 功能 | Controller | ServiceImpl | Mapper XML |
| --- | --- | --- | --- |
| 登录注册 | `AuthController` | `AuthServiceImpl` | `UserAccountMapper.xml` |
| 订单 | `OrderController` | `OrderServiceImpl` | `DemoOrderMapper.xml` |
| 顾客售后 | `CustomerAfterSaleController` | `AfterSaleApplicationServiceImpl` | `AfterSaleApplicationMapper.xml` |
| 管理员售后 | `AdminAfterSaleController` | `AfterSaleApplicationServiceImpl` | `AfterSaleApplicationMapper.xml` |
| 回复草稿 | `AdminReplyDraftController` | `ReplyDraftServiceImpl` | `ReplyDraftMapper.xml` |
| 聊天 | `ChatSessionController` | `ChatServiceImpl` | `ChatSessionMapper.xml`、`ChatMessageMapper.xml` |
| 知识库 | `KnowledgeDocController` | `KnowledgeDocServiceImpl` | `KnowledgeDocMapper.xml` |
| 工单 | `ServiceTicketController` | `ServiceTicketServiceImpl` | `ServiceTicketMapper.xml` |
| SLA | `SlaTaskController` | `SlaTaskServiceImpl` | `SlaTaskMapper.xml` |
| 日志 | `LogController` | `LogServiceImpl` | `AiCallLogMapper.xml`、`RetrievalLogMapper.xml` |

我在 Service 层保留业务规则，而不是把规则写在前端。比如 JWT 解析后还要回查用户状态、售后金额不能超过订单金额、终态不能补凭证、顾客只能访问自己的订单、管理员才能审核，这些都在 Java 后端校验。通用的“本人或管理员”判断收口到 `AuthService.isAdmin` 和 `AuthService.ensureSelfOrAdmin`，Controller 只负责在资源入口处调用它们。前端只负责交互和展示。

老师问为什么这样分层，我可以这样回答：

> 我按 Spring Boot 常见分层写。Controller 只负责接请求和返回 JSON；Service 放业务规则，比如 JWT 鉴权、权限判断、状态机、金额校验、日志写入和 PageHelper 分页入口；Mapper 接口只定义数据库方法；SQL 写在 MyBatis XML 中，主要负责动态条件、关联查询和排序。这样老师问一个接口时，我可以从前端页面一路讲到数据库表，也能说明为什么业务规则不放在前端。

## 18. 一条完整例子：顾客申请售后从页面到数据库再返回页面

这里用“顾客申请退货退款”作为完整例子串起来：

1. 顾客进入 `/customer/after-sales`，页面组件是 `CustomerAfterSaleCenterView.vue`。
2. 页面加载时 `onMounted(reloadAll)`，先调用 `loadOrders()` 和 `loadAfterSales()`。
3. 顾客在订单表格中点击某个订单，`selectOrder(row)` 把订单写入 `selectedOrder`。
4. 点击“申请售后”，`openApplyDialog(order)` 打开弹窗，`applyForm` 保存售后类型、原因、金额和说明。
5. 点击“提交申请”，`submitApplication()` 组装 `orderId/serviceType/reasonCode/reasonText/refundAmount`。
6. 前端 `customerAfterSaleApi.createCustomerAfterSale` 请求 `POST /customer/after-sales`。
7. Axios 在 `request.js` 自动带上 `Authorization: Bearer <JWT>`。
8. 后端 `CustomerAfterSaleController.create` 接收 `AfterSaleApplicationCreateRequest`。
9. Controller 调用 `authService.requireCustomer`，后端解析 JWT、回查 `user_account`，确认当前是顾客。
10. Service `AfterSaleApplicationServiceImpl.create` 查询 `demo_order`，校验订单归属、支付状态、金额、是否已有进行中售后。
11. Service 创建 `AfterSaleApplication`，状态设为 `SUBMITTED`，优先级和风险等级由规则计算。
12. `AfterSaleApplicationMapper.insert` 插入 `after_sale_application`。
13. `writeLog` 调用 `AfterSaleProcessLogMapper.insert` 插入 `after_sale_process_log`，动作是 `SUBMIT`。
14. `DemoOrderMapper.updateAfterSaleStatus` 更新 `demo_order.after_sale_status`。
15. Service 调用 `getById(application.getId())` 重新查询完整详情，包括凭证和流程日志。
16. Controller 返回 `Result.success(application)`。
17. 前端响应拦截器取出 `result.data`。
18. 页面关闭弹窗，调用 `reloadAll()`，售后列表显示新申请。
19. 顾客点击新售后单，详情区展示状态、下一步、流程时间线和凭证入口。

这条链路对应的核心文件是：

```text
web/src/views/CustomerAfterSaleCenterView.vue
web/src/api/customerAfterSaleApi.js
web/src/api/request.js
server/src/main/java/com/user/returnsassistant/controller/CustomerAfterSaleController.java
server/src/main/java/com/user/returnsassistant/service/impl/AfterSaleApplicationServiceImpl.java
server/src/main/java/com/user/returnsassistant/mapper/AfterSaleApplicationMapper.java
server/src/main/resources/mapper/AfterSaleApplicationMapper.xml
server/src/main/resources/mapper/AfterSaleProcessLogMapper.xml
server/src/main/resources/mapper/DemoOrderMapper.xml
sql/schema.sql
```

老师如果让我现场讲一个功能，我会优先讲这条，因为它最能体现前后端、权限、业务规则、数据库和页面绑定的完整闭环。

## 19. 答辩时可直接使用的总回答

如果老师问“你这个系统的功能到底是怎么做出来的”，我可以用下面这段作为总回答：

> 我的系统不是只做了静态页面，而是按真实双端售后系统做的。前端是 Vue 3，页面用 Element Plus 做表格、表单、弹窗、时间线和状态标签，用 `ref`、`reactive`、`computed` 管理状态。每个页面的按钮都会调用 `web/src/api` 里的接口函数，Axios 统一带 JWT 并处理后端返回的 `Result`。
>
> 后端是 Spring Boot。每个请求先到 Controller，比如顾客售后到 `CustomerAfterSaleController`，管理员审核到 `AdminAfterSaleController`，聊天到 `ChatSessionController`。Controller 只接参数和做身份入口校验，真正业务放在 Service，比如 `AfterSaleApplicationServiceImpl` 负责售后状态流转、金额校验、凭证、流程日志和订单状态同步，`ChatServiceImpl` 负责意图识别、知识库检索、AI 调用、日志和工单转接。
>
> 数据访问使用 MyBatis。Mapper 接口定义 Java 方法，具体 SQL 写在 `server/src/main/resources/mapper` 的 XML 里。比如售后查询从 `after_sale_application` 查，并关联 `demo_order`、`user_account`、`service_ticket`；详情会再查 `after_sale_process_log` 和 `after_sale_evidence`。数据库返回后，Service 组装对象，Controller 用 `Result.success(data)` 返回 JSON。前端拿到 JSON 后写回响应式变量，表格、详情、弹窗和时间线就自动显示。
>
> AI 部分我也没有让它直接控制业务。AI 只负责生成回复草稿、辅助识别意图、知识库推荐和日志诊断。售后通过、驳回、退款金额、补材料、完成这些关键动作仍然由管理员在页面确认，并由后端 Service 写入数据库和流程日志。

## 20. 老师可能追问的问题

### Q1：为什么说这是双端系统？

因为路由和权限明确区分顾客端和管理员端。顾客端 `/customer/after-sales` 只能查看自己的订单和售后，管理员端 `/admin/after-sales/review`、`/admin/sla`、`/service-tickets`、`/knowledge` 等可以处理全部业务。前端 `router.beforeEach` 会根据 Pinia 里的用户角色做页面跳转，后端 `authService.requireCustomer/requireAdmin` 会解析 JWT 并回查数据库做真实权限限制。对于复用同一个接口的资源，例如订单和会话，后端还会用 `isAdmin` 或 `ensureSelfOrAdmin` 做资源归属校验。

### Q2：前端参数是怎么传给后端的？

页面用 `v-model` 把输入框、下拉框、评分、金额等绑定到 `reactive` 表单对象。点击按钮时，页面方法把这些对象传给 `api` 文件里的函数。查询接口用 `GET` 的 `params`，提交接口用 `POST/PUT` 的 JSON body，路径参数用模板字符串拼到 URL 中。

### Q3：后端 Controller 怎么接参数？

查询参数用普通对象接收，例如 `AfterSaleApplicationSearch search`、`OrderSearch search`。路径参数用 `@PathVariable Long id`。提交表单用 `@RequestBody`，比如 `AfterSaleApplicationCreateRequest`、`AfterSaleActionRequest`、`ServiceReviewRequest`。需要身份时用 `HttpServletRequest` 读取 `Authorization`，再交给 `AuthServiceImpl` 去掉 `Bearer ` 前缀并解析 JWT。

### Q4：Service 层具体做什么？

Service 层负责业务规则。比如售后申请会校验订单归属、支付状态、金额、重复申请；审核会校验状态能不能流转；补凭证会校验是否终态；聊天会做意图识别、知识库检索和 AI 兜底；工单会处理优先级和状态。Service 还负责写日志、同步相关表，以及在标准列表页统一调用 PageHelper 生成分页结果。

### Q5：MyBatis XML 有什么作用？

Mapper XML 写真实 SQL。比如 `AfterSaleApplicationMapper.xml` 的 `BaseSelect` 会关联订单、顾客、处理人、工单；`SearchWhere` 根据用户 ID、状态、类型、优先级、关键词动态拼条件。现在主列表分页不再在 XML 里手写 `limit`，而是由 Service 层的 PageHelper 自动生成分页 SQL 和总数统计。这样接口返回的数据不是假数据，而是数据库查询结果。

### Q6：JSON 返回给前端后怎么显示？

后端统一返回 `Result`。前端 Axios 响应拦截器判断 `code`，成功就返回 `data`。页面方法拿到 `data` 后写入 `orders.value`、`afterSales.value`、`selected.value`、`profile.value` 等响应式变量。模板里表格、描述组件、时间线通过这些变量渲染，所以数据更新后页面自动刷新。

### Q7：AI 如果失败，系统还能不能用？

可以。聊天模块有本地规则兜底回复；AI 回复草稿模块有 `buildTemplateDraft` 本地模板；`AiServiceImpl` 会返回 `SKIPPED` 或 `FAILED` 状态；日志中心能显示失败原因。售后审核、补凭证、工单、评价这些核心业务都不依赖 AI 成功。

### Q8：如何证明这个项目不是演示页面？

可以从三点证明：第一，数据库有真实业务表，包括售后主表、凭证表、处理日志表、工单表、评价表；第二，后端 Service 有真实校验和状态流转，不是前端改状态；第三，日志中心和证据报告会记录 AI 调用、知识库命中和流程轨迹，能追溯每次处理过程。
