# Course Goal Progress

## Goal

- Project: 电商退换货智能客服系统
- Path: `D:\复制软件系统`
- Target: 按完整《复杂软件系统实践》项目持续迭代到高分水平，先保证完整系统稳定可演示，再提炼个人答辩材料。
- Current branch: `codex/fuse-peer-features`

## Assignment And Teacher Guidance

- Required functions: 登录注册与权限、客服会话、多轮追问、退货/换货/退款/物流/投诉售后场景、订单与售后记录、知识库检索、AI 回复、本地规则兜底、人工工单、日志追踪、演示数据与测试说明。
- Scoring standard: 开题报告 20 分；结项答辩团队 40 分；结项答辩个人 20 分；结项报告 20 分；自拟题目存在难度系数风险，需要用完整度、AI 特色和演示稳定性补强。
- Teacher guidance: 基础功能必须完整可运行；有特色功能和眼前一亮的亮点更容易拿高分。
- Personal defense requirement: 从完整项目中提炼个人负责模块、核心代码、创新点、难点、技术取舍和问答。

## Current Baseline

- Backend: Spring Boot 3.3.13 + MyBatis + MySQL，已有用户、订单、售后、聊天、知识库、AI 日志、检索日志、工单、处理轨迹等分层代码。
- Frontend: Vue 3 + Vite + Pinia + Element Plus，已有登录/注册、咨询工作台、系统总览、知识库、订单、工单、日志、AI 测试页面。
- Database: `sql/schema.sql` 覆盖核心业务表；`sql/seed.sql` 提供演示数据。
- AI: LangChain4j 接入 OpenAI-compatible 服务；保留本地规则兜底；已有 SSE 流式消息接口和 AI/检索/轨迹日志。
- Docs/report: README、后端/前端/数据库/测试/演示文档已存在；结项报告和个人答辩材料还需要后续系统化补齐。
- Current blockers: 本轮功能验证已通过；报告与个人答辩材料仍需后续强化。

## Score-Gap Table

| Category | Weight | Current | Target | Gap | Next Action | Evidence |
| --- | ---: | ---: | ---: | --- | --- | --- |
| 开题报告可行性与创新 | 20 | 17 | 18+ | 需要与最终系统同步 | 后续补最终功能变化说明 | `docs/opening-report.md` |
| 结项答辩运行顺畅与需求满足 | 30 | 25 | 28+ | 演示入口与稳定证据需更清晰 | 新增答辩展示中心并验证 | `/showcase` |
| 程序设计规范与 UI 友好 | 10 | 8 | 9+ | 页面质感仍需持续 polish | 本轮先做高级展示页 | Vue 页面与浏览器截图 |
| 个人自述与问答 | 20 | 13 | 18+ | 个人贡献、核心代码、Q&A 尚未成册 | 后续新增个人答辩材料 | TBD |
| 结项报告详实度与拓展建议 | 20 | 13 | 18+ | 缺结项报告正文和真实验证证据整理 | 后续生成/更新报告材料 | TBD |

## Required Function Checklist

| Function | Status | Entry/UI | API/Code | Data | Evidence |
| --- | --- | --- | --- | --- | --- |
| 登录注册与权限 | Done | `/login` | `/auth/login`, `/auth/register`, `AuthInterceptor` | `user_account` | 浏览器角色烟测 |
| 客服会话与多轮追问 | Done | `/chat` | `/chat-sessions`, `/message-stream` | `chat_session`, `chat_message` | 全链路烟测 |
| 退货/换货/退款/物流/投诉 | Done | `/chat`, `/orders` | `ChatServiceImpl`, `OrderController` | seed 订单与知识库 | 演示脚本 |
| 订单与售后记录 | Done | `/orders` | `/orders`, `/after-sale-records` | `demo_order`, `after_sale_record` | 全链路烟测 |
| 知识库管理与检索 | Done | `/knowledge` | `/knowledge-docs/search` | `knowledge_doc` | 检索日志 |
| AI 回复与本地兜底 | Done | `/chat`, `/ai-test` | `AiServiceImpl` | `ai_call_log` | AI 测试和日志 |
| 工单创建与处理 | Done | `/service-tickets` | `/service-tickets` | `service_ticket` | 工单页面 |
| 日志追踪与演示数据 | Done | `/logs` | `/ai-call-logs`, `/retrieval-logs`, `/process-traces` | 日志表 | 日志中心 |

## Feature Highlight Pool

| Priority | Highlight | Score Value | Status | Validation | Demo Script |
| ---: | --- | --- | --- | --- | --- |
| 1 | 答辩展示中心 | 高 | Done | `npm run test:browser` 已覆盖 | 先总览再进入工作台 |
| 2 | AI 流式客服 | 高 | Done | 浏览器烟测覆盖 | 发送退货问题 |
| 3 | RAG 知识依据 | 高 | Done | 全链路与日志 | 展示命中文档 |
| 4 | LangChain4j 工具调用 | 高 | Done | 后端编译与文档 | 讲业务工具封装 |
| 5 | 本地规则兜底 | 高 | Done | AI SKIPPED/FALLBACK | 说明现场稳定性 |
| 6 | 多轮上下文 | 高 | Done | 全链路烟测 | 退款追问 |
| 7 | 智能工单升级 | 高 | Done | 全链路与浏览器 | 投诉转人工 |
| 8 | 权限与角色隔离 | 中高 | Done | `npm run test:browser:roles` | 客户访问限制 |
| 9 | 日志可追溯 | 中高 | Done | 日志中心 | AI/检索/轨迹 |

## Frontend Premium Review

| Page | Current Quality | Apple-like Premium Gap | Next Polish | Evidence |
| --- | --- | --- | --- | --- |
| 答辩展示中心 | High | 已完成第一版，截图显示布局稳定、亮点清楚 | 后续可继续细化微交互与数据统计 | `output/playwright/01-showcase.png` |
| 咨询工作台 | Medium | 信息密度高，仍有普通后台感 | 后续 polish 聊天气泡和洞察面板 | `ChatWorkbenchView.vue` |
| 知识库 | Medium | 表格页需要更强检索解释感 | 后续增加命中分析概览 | `KnowledgeDocView.vue` |
| 工单/售后 | Medium | 处理状态可视化还可更强 | 后续增加流程时间线 | `ServiceTicketView.vue` |
| 日志/看板 | Medium | 可视分析偏弱 | 后续增加统计摘要 | `LogCenterView.vue` |

## Iteration Log

| Iteration | Change | Validation | Result | Remaining Gap |
| --- | --- | --- | --- | --- |
| 2026-05-05-1 | 新增 `/showcase` 答辩展示中心、管理员默认入口、侧栏菜单、浏览器烟测覆盖、README/docs 同步 | `mvn -q -DskipTests package` 通过；`npm.cmd run build` 通过；`tools/full-smoke-test.ps1` FAILED_COUNT=0；`npm.cmd run test:browser` FAILED_COUNT=0；`npm.cmd run test:browser:roles` FAILED_COUNT=0 | Passed | 结项报告和个人答辩材料仍需补齐；后续可继续 polish 看板/日志数据分析 |

## Validation Commands

- `cd server; mvn -q -DskipTests package`
- `cd web; npm run build`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1`
- `cd web; npm run test:browser`
- `cd web; npm run test:browser:roles`

## Report And Defense Artifacts

- README: 已新增答辩展示中心入口说明。
- Backend docs: 本轮未改后端接口。
- Frontend docs: 已新增 `/showcase` 设计说明。
- Database docs: 本轮未改数据库。
- Test cases: 已新增展示中心与客户权限核验项。
- Demo script: 已改为六步演示，第一步从展示中心开始。
- Report: 待补结项报告。
- Personal defense notes: 待补个人贡献、核心代码和 Q&A。
- Q&A: 待补。

## Final Review

- Full demo: Passed for current iteration via full smoke + browser smoke
- Required functions: Current iteration verified
- Highlight features: 8+ real highlights, current iteration verified
- Frontend premium quality: Showcase page first pass verified by screenshot
- Report consistency: In Progress
- Personal defense readiness: Pending
- Self-iteration completed: Current iteration reviewed; continue because report/personal defense remain high-value
- Git status: Pending
- Commit/push: Pending
