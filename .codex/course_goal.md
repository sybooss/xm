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
- Docs/report: README、后端/前端/数据库/测试/演示文档已存在；已补结项报告草稿、正式 DOCX、个人答辩讲解稿、个人答辩 Q&A 和个人答辩 PPT。
- Current blockers: 本轮功能验证已通过；正式结项报告、日志诊断中心和个人答辩 PPT 已完成。后续可继续 polish 工单/知识库页面，但相对当前评分材料的收益已经较低。

## Score-Gap Table

| Category | Weight | Current | Target | Gap | Next Action | Evidence |
| --- | ---: | ---: | ---: | --- | --- | --- |
| 开题报告可行性与创新 | 20 | 17 | 18+ | 需要与最终系统同步 | 后续补最终功能变化说明 | `docs/opening-report.md` |
| 结项答辩运行顺畅与需求满足 | 30 | 25 | 28+ | 演示入口与稳定证据需更清晰 | 新增答辩展示中心并验证 | `/showcase` |
| 程序设计规范与 UI 友好 | 10 | 9 | 9+ | 展示中心和日志诊断中心已有高级产品感，工单/知识库仍可继续 polish | 后续按答辩收益选择局部页面 | Vue 页面与浏览器截图 |
| 个人自述与问答 | 20 | 19 | 18+ | 讲解稿、Q&A 和 11 页 PPT 已完成 | 答辩前按个人实际分工微调姓名/贡献措辞 | `docs/personal-defense-script.md`, `docs/personal-defense-qa.md`, `docs/personal-defense-slides.pptx` |
| 结项报告详实度与拓展建议 | 20 | 17 | 18+ | 已有 Markdown 草稿，后续可转为课程模板 DOC | 转为正式报告格式并补截图索引 | `docs/final-report-draft.md` |

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
| 9 | 日志诊断中心 | 高 | Done | `npm run test:browser` 覆盖 | AI 成功率/平均耗时/知识命中/轨迹步骤 |

## Frontend Premium Review

| Page | Current Quality | Apple-like Premium Gap | Next Polish | Evidence |
| --- | --- | --- | --- | --- |
| 答辩展示中心 | High | 已完成第一版，截图显示布局稳定、亮点清楚 | 后续可继续细化微交互与数据统计 | `output/playwright/01-showcase.png` |
| 咨询工作台 | Medium | 信息密度高，仍有普通后台感 | 后续 polish 聊天气泡和洞察面板 | `ChatWorkbenchView.vue` |
| 知识库 | Medium | 表格页需要更强检索解释感 | 后续增加命中分析概览 | `KnowledgeDocView.vue` |
| 工单/售后 | Medium | 处理状态可视化还可更强 | 后续增加流程时间线 | `ServiceTicketView.vue` |
| 日志/看板 | High | 已新增诊断总览、状态分布、高频命中文档和轨迹摘要 | 后续可扩展趋势图，但短期收益低于 PPT | `output/playwright/09-logs.png` |

## Iteration Log

| Iteration | Change | Validation | Result | Remaining Gap |
| --- | --- | --- | --- | --- |
| 2026-05-05-1 | 新增 `/showcase` 答辩展示中心、管理员默认入口、侧栏菜单、浏览器烟测覆盖、README/docs 同步 | `mvn -q -DskipTests package` 通过；`npm.cmd run build` 通过；`tools/full-smoke-test.ps1` FAILED_COUNT=0；`npm.cmd run test:browser` FAILED_COUNT=0；`npm.cmd run test:browser:roles` FAILED_COUNT=0 | Passed | 结项报告和个人答辩材料仍需补齐；后续可继续 polish 看板/日志数据分析 |
| 2026-05-05-2 | 新增结项报告草稿、个人答辩讲解稿、个人答辩 Q&A，并在 README/演示脚本中加入入口 | `mvn.cmd -q -DskipTests package` 通过；`npm.cmd run build` 通过；文档材料存在且包含核心代码与验证命令引用 | Passed | 需要正式 DOC 模板化和 PPT 化 |
| 2026-05-05-3 | 新增 `tools/generate-final-report-docx.py`，生成正式结项报告 `docs/final-report.docx`，并完成 DOCX->PDF->PNG 渲染检查 | `python tools\generate-final-report-docx.py` 通过；`soffice.com --headless --convert-to pdf --outdir output\doc docs\final-report.docx` 通过；`pdftoppm.exe -png output\doc\final-report.pdf output\doc\final-report-pages\page` 通过；DOCX 结构检查为 65 段、4 表；7 页渲染图人工检查无乱码/重叠/截断；`mvn.cmd -q -DskipTests package` 通过；`npm.cmd run build` 通过，只有 Vite chunk size 警告 | Passed | 个人答辩材料仍可继续压缩为 PPT/演示卡片；日志/看板数据可视化仍有高收益 |
| 2026-05-05-4 | 将 `/logs` 升级为日志诊断中心，新增 AI 成功率、平均耗时、知识命中、平均检索分数、轨迹步骤、高频命中文档和状态分布；同步 README、前端文档、测试用例、演示脚本、个人答辩材料和正式报告 | `mvn.cmd -q -DskipTests package` 通过；`npm.cmd run build` 通过；`tools/full-smoke-test.ps1` FAILED_COUNT=0；`npm.cmd run test:browser` FAILED_COUNT=0；`npm.cmd run test:browser:roles` FAILED_COUNT=0；`python tools\generate-final-report-docx.py` 通过；`soffice.com --headless --convert-to pdf --outdir output\doc docs\final-report.docx` 通过；`pdftoppm.exe -png output\doc\final-report.pdf output\doc\final-report-pages\page` 通过；`docs/final-report.docx` 包含“日志诊断中心/AI 成功率/平均耗时/知识命中”；`output/playwright/09-logs.png` 人工检查无明显溢出/遮挡 | Passed | PPT/演示卡片仍是最高收益；工单/知识库可继续细化但收益次之 |
| 2026-05-05-5 | 新增 `tools/generate-personal-defense-slides-pptx.py`，生成 11 页个人答辩 PPT `docs/personal-defense-slides.pptx`，同步 README、演示脚本和个人答辩讲解稿 | `python -X utf8` in-memory compile 通过；`python tools\generate-personal-defense-slides-pptx.py` 通过；PPTX OpenXML 检查为 11 页且每页 slide relationship 完整；关键文本“电商退换货 AI 客服系统/LangChain4j/本地兜底//showcase/日志诊断中心/FAILED_COUNT=0”存在；`soffice.com --headless --convert-to pdf --outdir output\doc\personal-defense-slides-render docs\personal-defense-slides.pptx` 通过；`pdfinfo.exe` 显示 11 页、无加密、无 suspects；`pdftoppm.exe -png -r 160 ...` 生成 11 张非零 PNG；人工检查封面、流程页、日志诊断页和验证证据页无明显乱码/重叠/截断 | Passed | 工单/知识库页面仍可细化，但对当前评分材料的边际收益低 |
| 2026-05-06-1 | 将个人答辩 PPT 生成器改为确定性文档时间戳，避免每次验证后 PPTX 因 core.xml 时间变化而变脏 | `python -X utf8` compile 通过；连续两次 `python tools\generate-personal-defense-slides-pptx.py` 通过；PPTX 检查为 11 页、每页 rel 完整、固定时间戳存在、包含“日志诊断中心”；`soffice.com --headless --convert-to pdf --outdir output\doc\personal-defense-slides-render docs\personal-defense-slides.pptx` 通过；`pdfinfo.exe` 显示 11 页、无加密、无 suspects；`pdftoppm.exe -png -r 160 ...` 生成 11 张非零 PNG；`cd server; mvn.cmd -q -DskipTests package` 通过；`cd web; npm.cmd run build` 通过，仅有 Vite chunk size 警告 | Passed | 无新的高收益缺口；剩余为低收益页面细节 polish |
| 2026-05-06-2 | 修复 `web/browser-smoke.mjs` 对工单状态的脆弱断言：不再依赖数据库中一定存在“待处理”状态，改为验证人工转接、工单页详情、优先级、状态、AI 摘要和处理建议等稳定证据 | `cd server; mvn.cmd -q -DskipTests package` 通过；`cd web; npm.cmd run build` 通过，仅有 Vite chunk size 警告；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` FAILED_COUNT=0；`cd web; npm.cmd run test:browser` FAILED_COUNT=0；`cd web; npm.cmd run test:browser:roles` FAILED_COUNT=0 | Passed | 无新的高收益缺口；测试覆盖更稳定 |

## Validation Commands

- `cd server; mvn -q -DskipTests package`
- `cd web; npm run build`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1`
- `cd web; npm run test:browser`
- `cd web; npm run test:browser:roles`

## Report And Defense Artifacts

- README: 已新增答辩展示中心、结项报告和个人答辩 PPT 入口。
- Backend docs: 本轮未改后端接口。
- Frontend docs: 已新增 `/showcase` 设计说明。
- Database docs: 本轮未改数据库。
- Test cases: 已新增展示中心、客户权限核验项和日志诊断中心核验项。
- Demo script: 已改为七步演示，从展示中心开始，以日志诊断中心收尾，并链接配套答辩材料。
- Report: `docs/final-report-draft.md`, `docs/final-report.docx`
- Personal defense notes: `docs/personal-defense-script.md`
- Personal defense slides: `docs/personal-defense-slides.pptx`
- Q&A: `docs/personal-defense-qa.md`

## Final Review

- Full demo: Passed for current iteration via full smoke + browser smoke
- Required functions: Current iteration verified
- Highlight features: 8+ real highlights, current iteration verified
- Frontend premium quality: Showcase page and log diagnostic center verified by screenshots
- Report consistency: Markdown draft and formal DOCX report ready; PDF/PNG render checked
- Personal defense readiness: Script, Q&A and 11-page PPT ready; PPTX/PDF/PNG render checked
- Self-iteration completed: Current iteration reviewed; remaining polish ideas are lower-return than the completed system/report/defense package
- Git status: Clean on `codex/fuse-peer-features...origin/codex/fuse-peer-features`
- Commit/push: Pushed through `08f01df Stabilize browser smoke ticket assertions`
