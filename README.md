# 电商退换货智能客服系统

这是一个面向《复杂软件系统实践》的电商售后智能客服项目，聚焦退货、换货、退款进度、物流异常、规则解释、投诉与人工转接等场景。系统采用 `Spring Boot + Vue 3 + MySQL + LangChain4j`，后端负责业务流程、知识检索、订单上下文和 AI 调用日志，前端负责客服工作台、知识库、订单管理、日志中心和 AI 测试展示。

## 功能概览

- 咨询工作台：支持会话列表、订单绑定、用户提问、AI 增强回复、本地规则兜底。
- 意图识别：覆盖售前咨询、退货申请、换货申请、退款进度、物流查询、规则说明、投诉转接。
- 知识库管理：支持分类、文档 CRUD、关键词检索和命中依据展示。
- 订单与售后：内置订单样例，支持订单查询、售后记录维护和订单上下文融合。
- AI 接入：通过 LangChain4j 调用 OpenAI-compatible 接口，可接入 OpenAI 或本地 sub2api 网关。
- 模型切换：前端顶部可运行时切换模型，例如 `gpt-4o-mini`、`gpt-4.1-mini`。
- 日志追踪：记录 AI 调用日志、知识检索日志和处理轨迹，便于调试和答辩展示。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17, Spring Boot 3.3.13, MyBatis, PageHelper |
| 数据库 | MySQL 8.x |
| AI | LangChain4j `langchain4j-open-ai`, OpenAI-compatible API |
| 前端 | Vue 3, Vite, Pinia, Vue Router, Axios, Element Plus |
| 测试 | PowerShell API smoke test, Playwright browser smoke test |

## 目录结构

```text
.
├─ server/                 Spring Boot 后端
├─ web/                    Vue 3 前端
├─ sql/                    MySQL 建表和初始化数据
├─ docs/                   数据库、接口、前后端设计文档
├─ tools/                  本地启动脚本
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
git clone https://github.com/sybooss/--.git returns-assistant
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
```

说明：

- 使用本地 sub2api 时，`OPENAI_BASE_URL` 通常是 `http://127.0.0.1:8080/v1`。
- 使用官方 OpenAI 时，`OPENAI_BASE_URL` 可改为 `https://api.openai.com/v1`。
- `OPENAI_API_KEY` 不要提交到 Git；仓库已通过 `.gitignore` 忽略 `.env`。
- 如果没有配置 AI，系统仍可使用本地规则兜底，但 AI 测试和 AI 增强回复会显示跳过或失败。

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
| 咨询工作台 | `/chat` | 发送售后问题，查看 AI 回复、意图、依据和轨迹 |
| 系统总览 | `/dashboard` | 查看数据库、AI、模型和快速入口 |
| 知识库 | `/knowledge` | 管理知识文档，调试检索 |
| 订单管理 | `/orders` | 查看订单和售后上下文 |
| 日志中心 | `/logs` | 查看 AI 调用日志、检索日志、处理轨迹 |
| AI 测试 | `/ai-test` | 测试真实模型连通性 |

## 7. 关键接口

更多细节见 [docs/backend-api-design.md](docs/backend-api-design.md)。

```http
GET    /system/status
GET    /system/enums
GET    /system/ai-models
PUT    /system/ai-models/current

POST   /chat-sessions
GET    /chat-sessions
GET    /chat-sessions/{id}
POST   /chat-sessions/{id}/messages
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

## 9. 常见问题

### 9.1 后端提示数据库连接失败

确认 MySQL 已启动，且 `test3` 数据库已导入：

```powershell
Get-NetTCPConnection -LocalPort 3306 -State Listen
mysql -uroot -p1234 -e "SHOW DATABASES;"
```

如果密码不是 `1234`，修改 `.env` 中的 `MYSQL_PASSWORD`。

### 9.2 AI 状态是 `SKIPPED`

一般是没有设置 `AI_ENABLED=true` 或 `OPENAI_API_KEY`。检查：

```powershell
Get-Content .env
Invoke-RestMethod http://localhost:8081/system/status
```

### 9.3 sub2api 健康检查

如果使用本地 sub2api：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/health
```

返回 `status=ok` 后，再启动本项目后端。

### 9.4 Maven 打包时 jar 被锁

说明旧后端进程还在运行，先停掉 8081 端口进程：

```powershell
$conn = Get-NetTCPConnection -LocalPort 8081 -State Listen
Stop-Process -Id $conn.OwningProcess -Force
cd server
mvn -q -DskipTests package
```

## 10. 文档入口

- 数据库设计：[docs/database-design.md](docs/database-design.md)
- 后端接口设计：[docs/backend-api-design.md](docs/backend-api-design.md)
- 后端项目文档：[docs/backend-project-doc.md](docs/backend-project-doc.md)
- 前端项目文档：[docs/frontend-project-doc.md](docs/frontend-project-doc.md)
- LangChain4j 接入说明：[docs/langchain4j-real-model-integration.md](docs/langchain4j-real-model-integration.md)
- 演示脚本：[docs/demo-script.md](docs/demo-script.md)
- 测试用例：[docs/test-cases.md](docs/test-cases.md)
