# 新增特色功能闭环与版本记录

本文档专门记录后续新增特色功能、每轮版本变更、参考项目和验证证据。这里的 `10+` 按“10 多个新增特色功能”执行，不能把旧功能简单凑数；每个新增功能都必须能在代码、页面、接口、数据或测试脚本中闭环验证。

## 总目标

- 项目主栈保持为 `Spring Boot + Vue 3 + MySQL + LangChain4j`。
- LangChain4j 只作为 AI 增强层，订单、工单、权限、知识库等业务规则仍由 Spring Boot 服务层控制。
- 新增 10 多个可演示特色功能，每个功能都有入口、接口、证据和验收方式。
- 每轮优化后执行匹配范围验证，使用 Git 提交并推送到 GitHub。
- 前端逐步升级为类似 Apple 官网的克制、通透、强层级视觉体验，但首屏仍是可用系统，不做空泛营销页。

## 外部参考

| 参考对象 | 可借鉴点 | 本项目落点 |
| --- | --- | --- |
| [`lilishop/lilishop`](https://github.com/lilishop/lilishop) | 成熟电商系统的订单、售后、后台管理完整度 | 保持订单、售后、工单、知识库和日志的业务闭环 |
| [`spring-petclinic/spring-petclinic-langchain4j`](https://github.com/spring-petclinic/spring-petclinic-langchain4j) | Spring Boot 中接入 LangChain4j 的样例方式 | AI 作为增强服务，不替代业务服务层 |
| [`mahoneliu/KMatrix-service`](https://github.com/mahoneliu/KMatrix-service) | 知识库、RAG、检索增强和诊断能力 | 强化知识命中、检索日志和回答依据 |
| [`dengzhekun/projectku-web`](https://github.com/dengzhekun/projectku-web) | Spring Boot + Vue 项目工程结构和后台体验 | 保持前后端分层、接口资源化和管理端清晰入口 |
| Apple 官网与 Apple Human Interface Guidelines | 清晰、克制、通透、内容优先、层级明确 | 重做展示中心、侧边栏、顶部栏和运营指挥中心视觉系统 |

## 本轮新增 12 个特色功能

本轮新增一个 `/operations` 售后运营指挥中心，并通过 `GET /operation-insights` 聚合数据。它不是单纯页面美化，而是一次性承载 12 个新增特色功能闭环。

| 序号 | 新增特色功能 | 闭环说明 | 页面入口 | 接口/证据 | 验收方式 |
| ---: | --- | --- | --- | --- | --- |
| 1 | 运营指挥中心 | 聚合会话、意图、工单、渠道、知识、订单和 AI 质量 | `/operations` | `GET /operation-insights` | 浏览器看到“售后运营指挥中心” |
| 2 | 意图热力雷达 | 按意图聚合用户问题，识别退货、退款、物流、投诉热点 | `/operations` | `intentInsights` | 页面展示“意图热力雷达” |
| 3 | 工单 SLA 风险队列 | 按工单状态和数量识别待响应、处理中、积压风险 | `/operations` | `ticketInsights` | 页面展示“工单 SLA 风险队列” |
| 4 | 多渠道会话分布 | 聚合 WEB、ADMIN_TEST 等入口，为后续 App/小程序接入留扩展位 | `/operations` | `channelInsights` | 页面展示“多渠道会话分布” |
| 5 | 知识命中 Top 榜 | 统计高频命中文档和平均相关度，反向优化知识库 | `/operations` | `knowledgeInsights` | 页面展示“知识命中 Top 榜” |
| 6 | 订单风险扫描 | 扫描物流异常、退款中、退换货处理中订单并给出处置建议 | `/operations` | `orderRiskInsights` | 页面展示“订单风险扫描” |
| 7 | AI 运行质量摘要 | 聚合 AI 成功率、平均耗时、失败次数和兜底次数 | `/operations` | `aiInsights` | 页面展示“AI 运行质量摘要” |
| 8 | 下一步动作清单 | 将高风险工单、异常订单、知识短板、AI 失败转为待办动作 | `/operations` | `actionItems` | 页面展示“下一步动作清单” |
| 9 | 答辩亮点矩阵 | 把新增功能按类别、接口、证据、验收方式集中展示 | `/operations` | `newFeatures` | 页面展示“答辩亮点矩阵” |
| 10 | 版本里程碑面板 | 将每轮功能、验证和 Git 版本节奏可视化 | `/operations` | `versionMilestones` | 页面展示“版本里程碑面板” |
| 11 | 外部项目借鉴落点 | 在文档中把参考项目的能力映射到本项目实现位置 | `docs/feature-roadmap.md` | 外部参考表 | 文档保留参考对象和本项目落点 |
| 12 | 闭环验收证据索引 | 每个新增功能绑定入口、接口、证据和测试口径 | `/operations`、本文档 | `evidence` / `validation` 字段 | 接口和浏览器脚本共同覆盖 |

## 版本记录

### V2.0 售后运营指挥中心与 12 个新增特色功能

日期：2026-05-06

本轮完成：

- 后端新增 `GET /operation-insights`，聚合会话、意图、工单、渠道、知识命中、订单风险、AI 质量、动作清单和版本里程碑。
- 前端新增 `/operations` 售后运营指挥中心页面，采用 Apple-like 浅色玻璃质感、强层级首屏和高信息密度卡片布局。
- 侧边栏新增“运营指挥”入口，答辩展示中心新增“运营指挥中心”跳转按钮和功能卡片。
- `tools/full-smoke-test.ps1` 新增运营指挥中心接口断言。
- `web/browser-smoke.mjs` 新增 `/operations` 页面关键区域浏览器断言和截图。
- 本文档改为“新增特色功能闭环与版本记录”，明确本轮新增 12 个特色功能。

验收要求：

- 后端：`mvn -q -DskipTests package`
- 前端：`npm run build`
- 全链路：`tools/full-smoke-test.ps1`
- 浏览器：`npm run test:browser`
- Git：`git status --short` 确认只包含本轮相关文件后提交并推送。

### V2.1 多渠道真实接入

日期：2026-05-06

本轮完成：

- 扩展 `chat_session.channel`，支持 `WEB`、`APP`、`MINI_PROGRAM`、`ADMIN_TEST` 四类渠道。
- 新增数据库迁移脚本 `sql/migration-20260506-expand-chat-session-channel.sql`，同步更新 `sql/schema.sql` 约束。
- 后端会话查询支持 `channel` 筛选，`POST /chat-sessions` 创建会话时校验渠道合法性。
- `GET /system/enums` 新增 `chatChannels` 枚举，前端可从统一枚举扩展。
- 咨询工作台支持选择渠道创建会话，并支持按“全部渠道 / 网页 / App / 小程序 / 测试台”筛选。
- 运营指挥中心的多渠道会话分布现在可以统计真实渠道样本，不再只是预留展示。
- 全链路脚本新增 `APP` 渠道会话创建与筛选断言，浏览器脚本新增渠道控件断言。

验收要求：

- 数据库：在已有库执行 `sql/migration-20260506-expand-chat-session-channel.sql`
- 后端：`mvn -q -DskipTests package`
- 前端：`npm run build`
- 全链路：`tools/full-smoke-test.ps1`
- 浏览器：`npm run test:browser`、`npm run test:browser:roles`

### V2.2 会话证据报告导出

日期：2026-05-06

本轮完成：

- 后端新增 `GET /chat-sessions/{id}/evidence-report`，返回 `text/markdown` 证据报告。
- 报告聚合会话基础信息、订单上下文、完整对话、意图识别、知识命中、AI 调用、人工工单和处理轨迹。
- 前端咨询工作台新增“导出证据”按钮，可下载当前会话 Markdown 文件。
- 全链路脚本新增证据报告导出断言，验证报告包含知识命中、AI 调用、人工工单和处理轨迹章节。
- 浏览器脚本新增“导出证据”按钮可见性断言。

验收要求：

- 后端：`mvn -q -DskipTests package`
- 前端：`npm run build`
- 全链路：`tools/full-smoke-test.ps1`
- 浏览器：`npm run test:browser`

### V2.3 特色闭环中心与 14 个新增特色功能

日期：2026-05-06

本轮完成：

- 后端新增 `GET /feature-closures`，专门用于承载“新加 10 多个特色功能”的闭环验收，不再把旧功能简单计数。
- 前端新增 `/feature-closures` 特色闭环中心，侧边栏新增“特色闭环”入口。
- 页面采用 Apple-like 浅色玻璃质感、强留白、清晰层级和低圆角信息卡，展示指标、闭环功能、演示路线和参考项目落点。
- 每个新增特色功能都绑定：`code`、业务信号、诊断判断、下一步动作、页面入口、接口证据、借鉴来源、闭环分和 `closedLoop=true`。
- 全链路脚本新增接口断言，验证闭环功能数量不少于 14、全部闭环、演示路线和参考项目完整。
- 浏览器脚本新增 `/feature-closures` 页面断言，覆盖关键特色功能标题和参考项目落点。

本轮新加 14 个特色功能：

| 序号 | 功能 code | 新增特色功能 | 闭环落点 |
| ---: | --- | --- | --- |
| 1 | `SLA_GUARD` | SLA 自动预警台 | 基于工单状态和优先级识别待处理风险，跳转人工工单处置 |
| 2 | `SENTIMENT_RADAR` | 客户情绪温度计 | 基于投诉转人工意图识别负向情绪，回到咨询工作台触发样本 |
| 3 | `PRIORITY_ROUTER` | 智能优先级路由器 | 聚合 HIGH / URGENT 工单，辅助客服主管分派 |
| 4 | `COMPENSATION_ADVISOR` | 赔付方案推荐器 | 联动订单与售后记录，形成退款/补偿处置建议 |
| 5 | `KNOWLEDGE_GAP_MINER` | 知识缺口采矿器 | 检查知识库意图覆盖，反向推动 FAQ 补齐 |
| 6 | `ANSWER_QA_SCORECARD` | 回复质检评分卡 | 合并 AI 成功率、RAG 命中和流程轨迹形成质量解释 |
| 7 | `REFUND_TIMELINE` | 退款进度预测器 | 结合售后状态和多轮上下文解释退款进度 |
| 8 | `LOGISTICS_ESCALATION` | 物流异常处置流 | 识别物流异常订单，建议安抚与承运商同步 |
| 9 | `REPEAT_COMPLAINT_GUARD` | 重复投诉拦截器 | 按订单聚合工单，识别重复投诉和升级处理风险 |
| 10 | `EVIDENCE_CHAIN_CHECKER` | 证据链完整度检查器 | 对接会话证据报告，检查对话、知识、AI、工单、轨迹是否齐全 |
| 11 | `FALLBACK_DRILL` | AI 兜底演练面板 | 展示 AI 失败/跳过时本地业务规则仍可用 |
| 12 | `CHANNEL_ORCHESTRATOR` | 多渠道触达编排器 | 统一 Web、App、小程序、测试台会话渠道闭环 |
| 13 | `RAG_REVIEW_BOARD` | RAG 命中复盘板 | 回看知识命中、排序依据和检索日志 |
| 14 | `DEMO_SCRIPT_BUILDER` | 答辩演示编排器 | 串联登录、咨询、转人工、导出证据和运营复盘演示路线 |

参考项目与落地：

- LangChain4j RAG / Tools：借鉴 RAG、Tools、模型可替换；本项目落地为 Spring Boot 业务服务包裹 LangChain4j 增强层，并保留本地兜底。
- Spring Petclinic LangChain4j：借鉴自然语言调用业务工具；本项目落地为订单、知识、工单等工具调用必须经过业务服务。
- LILISHOP：借鉴电商订单、售后、后台运营完整度；本项目落地为订单、售后、工单、知识和运营闭环。

验收要求：

- 后端：`mvn -q -DskipTests package`
- 前端：`npm run build`
- 全链路：`tools/full-smoke-test.ps1`
- 浏览器：`npm run test:browser`
- 角色权限：`npm run test:browser:roles`
