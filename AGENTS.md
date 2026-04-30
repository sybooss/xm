# AGENTS.md

## 项目协作规则

1. 每次修改代码后，必须执行与修改范围匹配的验证，至少包括后端编译或前端构建；涉及接口或页面时要补充接口冒烟测试或浏览器测试。
2. 每次修改完成后，必须运行 `git status --short` 检查工作区，确认只包含本次任务相关文件。
3. 每次代码修改完成且验证通过后，必须提交并上传到 GitHub。
4. 推送 GitHub 时优先使用当前远程仓库 `origin`；如网络不通，可使用本机代理：`git -c http.proxy=http://127.0.0.1:6738 -c https.proxy=http://127.0.0.1:6738 push`。
5. 不允许提交真实密钥、`.env`、`output/`、`tmp/`、`web/dist/`、`server/target/`、`node_modules/` 等运行产物。
6. 项目主栈保持为 Spring Boot + Vue 3 + MySQL + LangChain4j；LangChain4j 只作为 AI 增强层，业务规则和本地兜底必须保留。
7. OpenAI 或本地 Sub2API Key 只能通过环境变量或本机 `.env` 注入，不能写入仓库文档、源码或提交历史。
8. REST 接口优先使用资源名词和标准 HTTP 方法，例如 `GET /service-tickets`、`POST /chat-sessions/{id}/messages`。

## 交付前检查

- 后端：`mvn -q -DskipTests package`
- 前端：`npm run build`
- 数据库变更：执行 `sql/schema.sql` 并确认新增表可访问
- 全链路：`tools/full-smoke-test.ps1`
- 浏览器：`npm run test:browser`
