# 电商退换货智能客服系统

这是一个面向《复杂软件系统实践》的电商售后智能客服项目，聚焦退货、换货、退款进度、物流异常、规则解释、投诉与人工转接等场景。系统采用 `Spring Boot + Vue 3 + MySQL + LangChain4j`，后端负责业务流程、知识检索、订单上下文和 AI 调用日志，前端负责客服工作台、知识库、订单管理、日志中心和 AI 测试展示。

## 功能概览

- 顾客售后中心：客户登录后默认进入“我的售后”，可查看订单、发起售后、补充凭证、跟踪进度和提交评价。
- 管理员售后工作台：管理员登录后默认进入售后审核队列，可处理补材料、审核、SLA、工单、AI 草稿和客户画像。
- 咨询工作台：支持会话列表、订单绑定、用户提问、AI 增强回复、本地规则兜底。
- 意图识别：覆盖售前咨询、退货申请、换货申请、退款进度、物流查询、规则说明、投诉转接。
- 知识库管理：支持分类、文档 CRUD、关键词检索和命中依据展示。
- 订单与售后：内置订单样例，支持订单查询、售后记录维护和订单上下文融合。
- AI 接入：通过 LangChain4j 调用 OpenAI-compatible 接口，可接入 OpenAI 或本地 sub2api 网关。
- 业务工具：订单查询、知识库检索、人工工单创建已封装为 LangChain4j `@Tool` 风格工具，并注入模型上下文。
- 模型切换：前端顶部可运行时切换模型，例如 `gpt-4o-mini`、`gpt-4.1-mini`。
- 多轮追问：结合会话摘要和最近消息承接上一轮语境，支持退货后追问退款、物流异常后追问投诉等连续咨询。
- 流式反馈：聊天发送后立即显示处理进度，最终回复采用逐字呈现效果。
- 人工工单：投诉、人工客服和异常升级场景可自动生成工单，客服可在管理端手动转人工。
- 登录注册与权限：支持客户自助注册、演示账号登录；顾客端和管理员端拥有不同默认入口、菜单和接口权限边界。
- 日志诊断：记录 AI 调用、知识检索和处理轨迹，并聚合展示 AI 成功率、平均耗时、知识命中文档和会话处理进度，便于调试和答辩举证。
- 答辩展示中心：`/showcase` 作为管理员菜单入口，以 Apple-like 视觉展示闭环流程、10+ 特色功能池、系统状态和版本路线图。

特色功能闭环与后续版本记录见 [`docs/feature-roadmap.md`](docs/feature-roadmap.md)。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17, Spring Boot 3.3.13, MyBatis XML Mapper, PageHelper |
| 数据库 | MySQL 8.x |
| AI | LangChain4j `langchain4j-open-ai`, OpenAI-compatible API |
| 前端 | Vue 3, Vite, Pinia, Vue Router, Axios, Element Plus |
| 交付 | Docker Compose, PowerShell smoke test, Playwright browser smoke test, GitHub Actions CI |

## 目录结构

```text
.
├─ server/                 Spring Boot 后端
├─ web/                    Vue 3 前端
├─ sql/                    MySQL 建表和初始化数据
├─ docs/                   数据库、接口、前后端设计文档
├─ tools/                  本地启动、烟测和一键验证脚本
├─ .github/workflows/      GitHub Actions 构建门禁
├─ docker-compose.yml       Docker 一键演示环境
├─ tmp/                    本地临时测试脚本，默认不提交
├─ output/                 本地日志、截图、报告输出，默认不提交
├─ .env.example            环境变量模板
└─ README.md
```

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 18+，建议 20+
- MySQL 8.x
- 可选：本地 sub2api 或其他 OpenAI-compatible 网关

检查命令：

```powershell
java -version
mvn -version
node -v
npm -v
mysql --version
```

## 1. 克隆项目

```powershell
git clone https://github.com/sybooss/xm.git returns-assistant
cd returns-assistant
```

如果你是在当前本机目录继续开发，项目根目录是：

```powershell
cd D:\复制软件系统
```

## 2. 初始化 MySQL 数据库

登录 MySQL：

```powershell
mysql -uroot -p
```

创建数据库并导入表结构和种子数据：

```sql
CREATE DATABASE IF NOT EXISTS test3 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE test3;
SOURCE D:/复制软件系统/sql/schema.sql;
SOURCE D:/复制软件系统/sql/seed.sql;
```

如果项目不在 `D:/复制软件系统`，把 `SOURCE` 后面的路径改成你本机实际路径。也可以直接在 PowerShell 中执行：

```powershell
mysql -uroot -p1234 -e "CREATE DATABASE IF NOT EXISTS test3 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -uroot -p1234 test3 < .\sql\schema.sql
mysql -uroot -p1234 test3 < .\sql\seed.sql
```

默认数据库配置在 [server/src/main/resources/application.yml](server/src/main/resources/application.yml)：

```yaml
spring:
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/test3?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:1234}
```

如果你的 MySQL 账号密码不是 `root / 1234`，请在环境变量或本地 `.env` 中覆盖。

## 3. 配置环境变量

复制模板：

```powershell
copy .env.example .env
```

按需修改 `.env`：

```env
MYSQL_URL=jdbc:mysql://localhost:3306/test3?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
MYSQL_USERNAME=root
MYSQL_PASSWORD=1234

AI_ENABLED=true
OPENAI_API_KEY=replace-with-your-key
OPENAI_BASE_URL=http://127.0.0.1:8080/v1
OPENAI_MODEL=gpt-4o-mini
OPENAI_MODEL_OPTIONS=gpt-4o-mini,gpt-4.1-mini,gpt-4.1,o4-mini
APP_AUTH_ADMIN_PASSWORD=123456
APP_AUTH_CUSTOMER_PASSWORD=123456
APP_AUTH_TOKEN_HOURS=8
```

说明：

- 使用本地 sub2api 时，`OPENAI_BASE_URL` 通常是 `http://127.0.0.1:8080/v1`。
- 使用官方 OpenAI 时，`OPENAI_BASE_URL` 可改为 `https://api.openai.com/v1`。
- `OPENAI_API_KEY` 不要提交到 Git；仓库已通过 `.gitignore` 忽略 `.env`。
- 如果没有配置 AI，系统仍可使用本地规则兜底，但 AI 测试和 AI 增强回复会显示跳过或失败。
- 默认演示管理员为 `admin / 123456`，默认演示客户为 `demo_customer / 123456`。
- 新客户可在登录页切换到“注册”，注册后默认角色为 `CUSTOMER`，只能进入“我的售后”、在线咨询和自己的订单售后入口。
- 注册用户使用个人密码登录；历史演示账号没有单独密码哈希时，继续兼容 `APP_AUTH_ADMIN_PASSWORD` / `APP_AUTH_CUSTOMER_PASSWORD`。

已有数据库升级到注册版本时，执行一次迁移脚本：

```powershell
mysql -uroot -p1234 test3 < .\sql\migration-20260505-add-user-password.sql
```

## 4. 启动后端

方式一：开发模式启动。

```powershell
cd server
mvn spring-boot:run
```

方式二：打包后用脚本启动，脚本会读取根目录 `.env`。

```powershell
cd server
mvn -q -DskipTests package
cd ..
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\start-server-with-sub2api.ps1
```

后端默认地址：

```text
http://localhost:8081
```

验证：

```powershell
Invoke-RestMethod http://localhost:8081/system/status
Invoke-RestMethod http://localhost:8081/system/enums
```

## 5. 启动前端

```powershell
cd web
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

前端开发环境通过 [web/vite.config.js](web/vite.config.js) 把 `/api` 代理到后端 `http://localhost:8081`。如需修改代理地址，调整 Vite 配置或 `.env.development`。

## 6. 常用页面

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 登录/注册 | `/login` | 管理员/客户登录，客户自助注册 |
| 我的售后 | `/customer/after-sales` | 顾客默认入口，可查看订单、提交售后、补凭证、查看进度和评价；支持 `?focus=售后ID` 直达详情 |
| 售后审核工作台 | `/admin/after-sales/review` | 管理员默认入口，处理顾客提交的售后申请、补材料、审核、完成和工单联动 |
| SLA 中心 | `/admin/sla` | 管理员集中查看待补材料、即将超时和风险售后任务 |
| 客户画像 | `/admin/customers/profile` | 管理员查看客户订单、售后、工单、评价和风险聚合 |
| 答辩展示中心 | `/showcase` | 管理员菜单入口，集中展示演示顺序、已实现亮点、现场检查清单和关键页面跳转 |
| 咨询工作台 | `/chat` | 发送售后问题，查看 AI 决策摘要、业务工具、建议追问、意图、依据和轨迹 |
| 系统总览 | `/dashboard` | 查看数据库、AI、模型和快速入口 |
| 知识库 | `/knowledge` | 管理知识文档，调试检索，并查看命中数量、意图覆盖、排序依据和命中解释 |
| 订单管理 | `/orders` | 查看订单和售后上下文 |
| 人工工单 | `/service-tickets` | 查看人工转接工单、优先级、AI 摘要、SLA 风险、下一步动作和售后处理时间线 |
| 日志诊断中心 | `/logs` | 聚合查看 AI 成功率、平均耗时、知识命中、处理进度，并保留 AI 调用日志、检索日志和处理轨迹原始证据 |
| AI 测试 | `/ai-test` | 测试真实模型连通性 |

## 7. 关键接口

更多细节见 [docs/backend-api-design.md](docs/backend-api-design.md)。

```http
GET    /system/status
GET    /system/enums
GET    /system/ai-models
PUT    /system/ai-models/current

POST   /auth/login
POST   /auth/register
GET    /auth/me
POST   /auth/logout

POST   /chat-sessions
GET    /chat-sessions
GET    /chat-sessions/{id}
POST   /chat-sessions/{id}/messages
POST   /chat-sessions/{id}/message-stream
GET    /chat-sessions/{id}/process-traces

GET    /knowledge-categories
GET    /knowledge-docs
GET    /knowledge-docs/search
POST   /knowledge-docs
PUT    /knowledge-docs/{id}
DELETE /knowledge-docs/{id}

GET    /orders
GET    /orders/no/{orderNo}
GET    /after-sale-records

GET    /service-tickets
POST   /service-tickets
GET    /service-tickets/{id}
PUT    /service-tickets/{id}
DELETE /service-tickets/{id}
GET    /chat-sessions/{id}/service-tickets
POST   /chat-sessions/{id}/service-tickets

GET    /ai-call-logs
GET    /retrieval-logs
POST   /ai-tests
```

统一返回格式：

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

## 8. 测试

本地一键构建验证：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify-program.ps1
```

该脚本会先执行交付预检，再按顺序执行后端打包和前端构建，适合作为提交前的基础门禁。仓库也提供 `.github/workflows/ci.yml`，在推送 `main`、`codex/**` 分支或向 `main` 提交 PR 时自动执行同一套基础构建验证。

只做交付环境预检：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\delivery-preflight.ps1
```

带运行环境的预检会检查后端、前端、MySQL 和 sub2api 是否可用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\delivery-preflight.ps1 -RequireRunningServices -RequireMysql -RequireSub2api
```

后端打包：

```powershell
cd server
mvn -q -DskipTests package
```

前端构建：

```powershell
cd web
npm run build
```

当前前端已启用路由懒加载和 Vite 手动拆包，构建产物会拆分为 Vue、Element Plus、图标、业务视图等多个 chunk，避免单个业务包过大。

全功能接口烟测：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1
```

浏览器烟测：

```powershell
cd web
npm run test:browser
```

浏览器测试会访问 `http://localhost:5173`，所以需要先启动后端和前端。

如果后端和前端已经启动，也可以用一键脚本附加完整烟测：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify-program.ps1 -WithSmoke -WithBrowser -WithRoleBrowser
```

完整模式会在构建前要求后端、前端、MySQL 和 sub2api 处于可用状态，并覆盖接口主链路、顾客/管理员真实业务流、角色菜单和越权路由拦截。

如果本机安装了 Docker，也可以验证容器化交付：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify-program.ps1 -WithDocker
```

## 9. Docker 演示部署

容器化演示环境包含 MySQL、Spring Boot 后端和 Nginx 前端，不需要把真实密钥写入仓库。默认 `AI_ENABLED=true`，如果同时配置了 `OPENAI_API_KEY` 和 `OPENAI_BASE_URL`，系统会优先使用 LangChain4j 调用真实模型；如果没有配置密钥或模型调用失败，聊天主链路仍会使用本地规则兜底。

```powershell
docker compose --project-name returns-assistant up -d --build
```

启动后访问：

```text
前端：http://localhost:5173
后端：http://localhost:8081
健康检查：http://localhost:8081/system/health
```

如需启用真实模型，在本机环境变量或 `.env` 中设置 `AI_ENABLED=true`、`OPENAI_API_KEY`、`OPENAI_BASE_URL` 和 `OPENAI_MODEL` 后再执行上面的 Compose 命令。Compose 默认把宿主机 `8080` 网关映射为 `http://host.docker.internal:8080/v1`。

## 10. 常见问题

### 10.1 后端提示数据库连接失败

确认 MySQL 已启动，且 `test3` 数据库已导入：

```powershell
Get-NetTCPConnection -LocalPort 3306 -State Listen
mysql -uroot -p1234 -e "SHOW DATABASES;"
```

如果密码不是 `1234`，修改 `.env` 中的 `MYSQL_PASSWORD`。

### 10.2 AI 状态是 `SKIPPED`

一般是没有设置 `OPENAI_API_KEY`，或者运行环境显式把 `AI_ENABLED` 改成了 `false`。检查：

```powershell
Get-Content .env
Invoke-RestMethod http://localhost:8081/system/status
```

### 10.3 sub2api 健康检查

如果使用本地 sub2api：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/health
```

返回 `status=ok` 后，再启动本项目后端。

### 10.4 Maven 打包时 jar 被锁

说明旧后端进程还在运行，先停掉 8081 端口进程：

```powershell
$conn = Get-NetTCPConnection -LocalPort 8081 -State Listen
Stop-Process -Id $conn.OwningProcess -Force
cd server
mvn -q -DskipTests package
```

## 11. 文档入口

- 数据库设计：[docs/database-design.md](docs/database-design.md)
- 后端接口设计：[docs/backend-api-design.md](docs/backend-api-design.md)
- 后端项目文档：[docs/backend-project-doc.md](docs/backend-project-doc.md)
- 前端项目文档：[docs/frontend-project-doc.md](docs/frontend-project-doc.md)
- LangChain4j 接入说明：[docs/langchain4j-real-model-integration.md](docs/langchain4j-real-model-integration.md)
- 高分增强功能设计：[docs/high-score-feature-enhancement-design.md](docs/high-score-feature-enhancement-design.md)
- 真实业务骨架改造方案：[docs/real-business-skeleton-refactor-plan.md](docs/real-business-skeleton-refactor-plan.md)
- 真实双端系统改造进度：[docs/real-business-skeleton-refactor-progress.md](docs/real-business-skeleton-refactor-progress.md)
- Agent 协作规则：[AGENTS.md](AGENTS.md)
- 演示脚本：[docs/demo-script.md](docs/demo-script.md)
- 测试用例：[docs/test-cases.md](docs/test-cases.md)
- 结项报告草稿：[docs/final-report-draft.md](docs/final-report-draft.md)
- 正式结项报告：[docs/final-report.docx](docs/final-report.docx)
- 个人答辩讲解稿：[docs/personal-defense-script.md](docs/personal-defense-script.md)
- 个人答辩 Q&A：[docs/personal-defense-qa.md](docs/personal-defense-qa.md)
