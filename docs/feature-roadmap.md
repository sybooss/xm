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

计划：

- 将当前渠道统计从聚合展示升级为会话创建、筛选和页面筛选能力。
- 扩展 `chat_session.channel` 支持 `WEB`、`APP`、`MINI_PROGRAM`、`ADMIN_TEST`。
- 在运营指挥中心展示渠道趋势和渠道专属问题。

### V2.2 SLA 自动提醒与报告导出

计划：

- 为高优先级工单增加 SLA 阈值、自动提醒和处置建议。
- 将会话、工单、知识命中和日志证据导出为答辩附件。
