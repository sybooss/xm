# 后端服务说明

后端使用 Spring Boot 3.3.13、MyBatis、MySQL 和 LangChain4j，提供会话、知识库、订单、售后记录、AI 测试、模型切换和日志查询接口。
数据访问层采用 MyBatis Mapper 接口 + XML 映射文件，SQL 位于 `src/main/resources/mapper`。

## 配置

数据库和 AI 配置都支持环境变量，默认值见 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/test3?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:1234}

app:
  ai:
    enabled: ${AI_ENABLED:true}
    model-options: ${OPENAI_MODEL_OPTIONS:gpt-4o-mini,gpt-4.1-mini,gpt-4.1,o4-mini}
    remote-models-enabled: ${OPENAI_REMOTE_MODELS_ENABLED:true}
    remote-models-cache-seconds: ${OPENAI_REMOTE_MODELS_CACHE_SECONDS:300}
    remote-models-timeout-seconds: ${OPENAI_REMOTE_MODELS_TIMEOUT_SECONDS:5}

langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      model-name: ${OPENAI_MODEL:gpt-4o-mini}
```

建议复制根目录 `.env.example` 为 `.env`，再填写本机 MySQL 密码和 OpenAI-compatible API Key。启用 `OPENAI_REMOTE_MODELS_ENABLED=true` 时，后端会从 `OPENAI_BASE_URL` 对应网关的 `/models` 接口自动合并模型列表；`OPENAI_MODEL_OPTIONS` 只作为手工补充和远程不可用时的兜底。

如果是在旧数据库上启用注册功能，先执行一次迁移：

```powershell
mysql -uroot -p1234 test3 < ..\sql\migration-20260505-add-user-password.sql
```

认证接口包括：

```http
POST /auth/login
POST /auth/register
GET /auth/me
POST /auth/logout
```

`/auth/register` 只创建客户账号，注册后直接返回 token；管理员账号仍通过初始化数据维护。

## 启动

开发模式：

```powershell
cd server
mvn spring-boot:run
```

打包：

```powershell
cd server
mvn -q -DskipTests package
```

使用根目录脚本启动：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\start-server-with-sub2api.ps1
```

## 验证

```powershell
Invoke-RestMethod http://localhost:8081/system/status
Invoke-RestMethod http://localhost:8081/system/enums
Invoke-RestMethod http://localhost:8081/system/ai-models
```

详细接口见 `docs/backend-api-design.md`。
