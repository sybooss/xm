# Course Goal Progress

## Current Goal

- Project: 电商退换货智能客服系统
- Path: `D:\复制软件系统`
- Branch: `codex/fuse-peer-features`
- Scope: 只做程序本体优化迭代，聚焦后端、前端、数据库、接口、测试脚本、运行稳定性、UI 交互和真实可演示功能。
- Paused non-goals: 暂停 PPT、论文、报告正文、答辩材料、`docs/final-report.docx`、`docs/final-report-draft.md`、`docs/personal-defense-slides.pptx`、个人答辩讲稿和 Q&A 的生成或修改。

## Program Baseline

- Backend: Spring Boot 3.3.13 + MyBatis + MySQL，已有用户、订单、售后、聊天、知识库、AI 日志、检索日志、工单、处理轨迹等分层代码。
- Frontend: Vue 3 + Vite + Pinia + Element Plus，已有登录/注册、答辩展示中心、咨询工作台、系统总览、知识库、订单、工单、日志、AI 测试页面。
- Build performance: 前端已启用路由懒加载、Element Plus 按需注册和 Vite 手动拆包，构建不再出现大 chunk 警告。
- Database: `sql/schema.sql` 覆盖核心业务表；`sql/seed.sql` 提供演示数据；注册密码迁移脚本已存在。
- AI: LangChain4j 接入 OpenAI-compatible 服务；本地规则兜底保留；已有 SSE 流式消息接口和 AI/检索/轨迹日志。
- Tests: `tools/full-smoke-test.ps1`、`web/browser-smoke.mjs`、`web/browser-role-smoke.mjs` 覆盖核心接口、页面和角色权限。

## Required Function Checklist

| Function | Status | Entry/UI | API/Code | Data | Evidence |
| --- | --- | --- | --- | --- | --- |
| 登录注册与权限 | Done | `/login` | `/auth/login`, `/auth/register`, `AuthInterceptor` | `user_account` | 浏览器角色烟测 |
| 客服会话与多轮追问 | Done | `/chat` | `/chat-sessions`, `/message-stream` | `chat_session`, `chat_message` | 浏览器烟测已覆盖 AI 决策摘要、业务工具、建议追问和上下文承接 |
| 退货/换货/退款/物流/投诉 | Done | `/chat`, `/orders` | `ChatServiceImpl`, `OrderController` | seed 订单与知识库 | 接口和浏览器烟测 |
| 订单与售后记录 | Done | `/orders` | `/orders`, `/after-sale-records` | `demo_order`, `after_sale_record` | 全链路烟测 |
| 知识库管理与检索 | Done | `/knowledge` | `/knowledge-docs/search` | `knowledge_doc`, `retrieval_log` | 浏览器烟测已覆盖命中数量、意图覆盖、排序依据和命中解释 |
| AI 回复与本地兜底 | Done | `/chat`, `/ai-test` | `AiServiceImpl` | `ai_call_log` | AI 测试和日志 |
| 工单创建与处理 | Done | `/service-tickets` | `/service-tickets`, `/chat-sessions/{id}/service-tickets` | `service_ticket` | 浏览器烟测已覆盖 SLA 风险、下一步动作和时间线 |
| 日志追踪与演示数据 | Done | `/logs` | `/ai-call-logs`, `/retrieval-logs`, `/process-traces` | 日志表 | 日志中心和浏览器烟测 |

## Feature Highlight Pool

| Priority | Highlight | Score Value | Status | Validation |
| ---: | --- | --- | --- | --- |
| 1 | AI 流式客服与处理过程 | 高 | Done | `npm run test:browser` |
| 2 | RAG 知识依据与检索日志 | 高 | Done | `tools/full-smoke-test.ps1`, `npm run test:browser` |
| 3 | LangChain4j 业务工具封装 | 高 | Done | 后端编译 |
| 4 | 本地规则兜底 | 高 | Done | AI SKIPPED/FALLBACK 路径 |
| 5 | 多轮上下文 | 高 | Done | 全链路烟测 |
| 6 | 智能工单升级 | 高 | Done | `npm run test:browser` |
| 7 | 权限与角色隔离 | 中高 | Done | `npm run test:browser:roles` |
| 8 | 日志诊断中心 | 高 | Done | `npm run test:browser` |
| 9 | 程序级展示入口 | 中高 | Done | `/showcase` |

## Frontend Premium Review

| Page | Current Quality | Program Gap | Next Action | Evidence |
| --- | --- | --- | --- | --- |
| `/showcase` | High | 暂无高收益程序缺口 | 保持稳定 | 浏览器烟测截图 |
| `/chat` | High | 已补齐 AI 决策摘要、业务工具和建议追问 | 保持稳定 | `ChatWorkbenchView.vue`, `output/playwright/04-chat-ai-enhanced.png` |
| `/knowledge` | High | 已补齐检索解释和 RAG 调试证据 | 保持稳定 | `KnowledgeDocView.vue`, `output/playwright/06-knowledge.png` |
| `/service-tickets` | High | 已补齐工单闭环演示感 | 后续只需保持稳定 | `ServiceTicketView.vue`, `output/playwright/08-service-tickets.png` |
| `/logs` | High | 趋势图可扩展，但当前收益低于工单闭环 | 暂缓 | 浏览器烟测截图 |

## Current Iteration

| Item | Decision |
| --- | --- |
| Audit result | 程序已覆盖核心链路；最高收益缺口是人工工单页缺少真实售后处理闭环的可视化证据。 |
| Selected improvement | 优化 `/service-tickets`，在现有数据字段基础上展示 SLA 风险、下一步动作和售后处理时间线。 |
| Why high return | 投诉转人工是课程演示中最容易体现“业务闭环 + AI 辅助 + 客服处理”的特色功能。 |
| Touched files | `.codex/course_goal.md`, `README.md`, `web/src/views/ServiceTicketView.vue`, `web/browser-smoke.mjs` |
| Validation result | `cd server; mvn -q -DskipTests package` 通过；`cd web; npm run build` 通过，仅有既有 Vite chunk size 警告；`tools/full-smoke-test.ps1` FAILED_COUNT=0；`cd web; npm run test:browser` FAILED_COUNT=0；`cd web; npm run test:browser:roles` FAILED_COUNT=0；`output/playwright/08-service-tickets.png` 已生成且非零。 |

## Iteration Log

| Iteration | Change | Validation | Result | Remaining Program Gap |
| --- | --- | --- | --- | --- |
| 2026-05-06-program-1 | 工单页新增 SLA 风险、下一步动作和售后处理时间线；浏览器烟测补新区域断言；README 同步程序运行说明 | `cd server; mvn -q -DskipTests package` 通过；`cd web; npm run build` 通过，仅有既有 Vite chunk size 警告；`tools/full-smoke-test.ps1` FAILED_COUNT=0；`cd web; npm run test:browser` FAILED_COUNT=0；`cd web; npm run test:browser:roles` FAILED_COUNT=0 | Passed | 知识库检索解释和聊天洞察面板仍是下一批候选，但收益低于本轮工单闭环 |
| 2026-05-06-program-2 | 知识库页新增检索诊断卡、意图覆盖、排序依据、命中解释和关键词标签；`/knowledge-docs/search` 增加整句未命中时的售后意图推断召回，并补 rank/score/hitReason | `cd server; mvn -q -DskipTests package` 通过；`cd web; npm run build` 通过，仅有既有 Vite chunk size 警告；`tools/full-smoke-test.ps1` FAILED_COUNT=0；`cd web; npm run test:browser` FAILED_COUNT=0；`cd web; npm run test:browser:roles` FAILED_COUNT=0 | Passed | 聊天工作台洞察区仍可进一步产品化，但本轮 RAG 可解释性已补齐 |
| 2026-05-06-program-3 | 聊天工作台右侧新增 AI 决策摘要、实时进度、业务工具、建议追问、知识命中分数和命中解释；浏览器烟测补关键断言；README 同步运行说明 | `cd server; mvn -q -DskipTests package` 通过；`cd web; npm run build` 通过，仅有既有 Vite chunk size 警告；`tools/full-smoke-test.ps1` FAILED_COUNT=0；`cd web; npm run test:browser` FAILED_COUNT=0；`cd web; npm run test:browser:roles` FAILED_COUNT=0 | Passed | 核心高收益页面已完成；剩余候选多为低收益细节 polish |
| 2026-05-06-program-4 | 前端工程化优化：路由视图懒加载、Element Plus 组件按需注册、移除全量图标注册、Vite 拆分 Vue/Element Plus/图标/业务视图 chunks，消除构建大 chunk 警告 | `cd web; npm run build` 通过且无 chunk size 警告；`cd server; mvn -q -DskipTests package` 通过；`tools/full-smoke-test.ps1` FAILED_COUNT=0；`cd web; npm run test:browser` FAILED_COUNT=0；`cd web; npm run test:browser:roles` FAILED_COUNT=0 | Passed | 无新的明显高收益程序缺口 |

## Validation Commands

- `cd server; mvn -q -DskipTests package`
- `cd web; npm run build`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1`
- `cd web; npm run test:browser`
- `cd web; npm run test:browser:roles`
- `git status --short`

## Program-Only Stop Gate

- 只有当核心链路、特色功能、接口烟测、浏览器烟测、构建验证、工作区检查、提交和推送都满足，并且实际审计没有明显高收益程序优化点时，才允许输出完成承诺。
- 当前状态: 第四轮程序优化已提交并推送到 `origin/codex/fuse-peer-features`；最终完成审计通过，当前未发现新的明显高收益程序优化点。
