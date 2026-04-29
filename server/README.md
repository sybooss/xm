# 后端服务说明

后端使用 Spring Boot 3.3.13、MyBatis、MySQL 和 LangChain4j，提供会话、知识库、订单、售后记录、AI 测试、模型切换和日志查询接口。

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
    enabled: ${AI_ENABLED:false}
    model-options: ${OPENAI_MODEL_OPTIONS:gpt-4o-mini,gpt-4.1-mini,gpt-4.1,o4-mini}

langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      model-name: ${OPENAI_MODEL:gpt-4o-mini}
```

建议复制根目录 `.env.example` 为 `.env`，再填写本机 MySQL 密码和 OpenAI-compatible API Key。

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

