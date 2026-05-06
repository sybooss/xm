# 真实双端系统改造进度文档

## 1. 当前目标

目标是把当前“AI 客服演示器”改造成真实可用的售后双端系统。顾客端围绕订单、售后申请、凭证、进度和评价；管理员端围绕审核队列、处理台、SLA、工单、客户画像和审计证据；AI 只作为辅助层，不能替代状态机和人工业务动作。

本进度文档用于记录每个版本的完成情况、验证命令、审视结论、遗留问题和下一轮优化方向。每次改造完成后必须更新本文件。

## 2. 版本进度总览

| 版本 | 状态 | 完成日期 | 主要交付 | 验证情况 | 审视结论 |
| --- | --- | --- | --- | --- | --- |
| V0.1 | 已完成 | 2026-05-06 | 建立 goal、补全版本化计划表、新增进度和审视机制 | UTF-8 文档检查、`git status --short`、提交并推送 `3cadb30` | 第一轮代码改造应从售后主对象、状态机和处理日志开始 |
| V1.0 | 已完成 | 2026-05-06 | 售后申请主对象、状态机、顾客/管理员基础接口 | 后端编译、schema/seed 执行、全链路冒烟、前端构建、浏览器测试均通过 | 新业务主流程已从旧演示 CRUD 中分离，下一步应补顾客端售后中心页面 |
| V1.1 | 已完成 | 2026-05-06 | 顾客端售后中心、申请弹窗、售后详情时间线和顾客默认入口 | `npm run build`、`npm run test:browser`、后端编译、全链路冒烟均通过 | 顾客端已从聊天入口独立出来，下一步应建设管理员审核工作台 |
| V1.2 | 已完成 | 2026-05-06 | 管理员审核队列、审核处理台、通过/驳回动作和浏览器主流程覆盖 | 后端编译、前端构建、全链路冒烟、浏览器测试均通过 | 管理员端已能在页面处理真实售后申请，下一步应补凭证和补材料证据链 |
| V2.0 | 已完成 | 2026-05-06 | 售后凭证表、顾客补充凭证、管理员要求补材料、双端证据链展示 | schema/seed、后端编译、前端构建、全链路冒烟、浏览器测试均通过 | 证据链已进入真实售后主流程，下一步应做 SLA 和优先级队列 |
| V2.1 | 已完成 | 2026-05-06 | SLA 任务接口、风险类型筛选、管理员 SLA 中心和跳转审核台 | 后端编译、前端构建、全链路冒烟、浏览器测试均通过 | SLA 风险队列已可用于管理员日常处理，下一步应关联工单和客服处理记录 |
| V3.0 | 已完成 | 2026-05-06 | 售后单关联客服工单、工单状态回写售后处理记录、管理端创建关联工单入口 | schema/seed、后端编译、前端构建、全链路冒烟、浏览器测试均通过 | 售后申请、订单、工单和处理日志已串成证据链，下一步应让 AI 作为副驾驶生成可审计建议 |
| V4.0 | 已完成 | 2026-05-06 | AI 副驾驶回复草稿、知识依据、风险标签、采纳/废弃审计 | schema/seed、后端编译、前端构建、全链路冒烟、浏览器测试均通过 | AI 已从占位摘要变成可审计建议层，下一步应补顾客评价和客户画像 |
| V5.0 | 已完成 | 2026-05-06 | 顾客服务评价、管理员客户画像、确认完成动作和风险聚合 | schema/seed、后端编译、前端构建、全链路冒烟、浏览器测试均通过 | 售后处理结果已形成反馈闭环，下一步应收口双端导航和权限边界 |
| V6.0 | 未开始 | - | 双端导航和权限边界收口 | - | - |
| V7.0 | 未开始 | - | 自动化验证和交付硬化 | - | - |
| V8.0 | 未开始 | - | 最终审视和可优化项清零 | - | - |

## 3. V0.1 计划基线记录

### 已完成

- 已建立长期 goal：完成从客服演示系统到真实双端售后系统的改造。
- 已确认项目当前主栈保持为 Spring Boot + Vue 3 + MySQL + LangChain4j。
- 已确认本轮默认只做程序、接口、页面、测试、稳定性和项目文档，不处理 PPT、论文、答辩材料。
- 已在 `docs/real-business-skeleton-refactor-plan.md` 中补充版本化改造计划表、每轮闭环和停止优化标准。
- 已新增本进度文档，后续每个版本完成后都必须更新。

### 待验证

- 文档改动完成后检查 Markdown 内容是否可读。
- 运行 `git status --short`，确认只包含本轮相关文件。
- 提交并推送 V0.1 计划基线。

### 本轮审视

当前计划已经从“想做哪些功能”升级为“按版本交付什么、怎么验证、怎么审视、什么时候停止”。下一步不能继续只写方案，应该进入 V1.0：建立真实售后申请主对象、状态机、处理日志和基础 REST 接口。

## 4. 当前下一轮候选改造

| 优先级 | 候选项 | 原因 | 建议版本 |
| --- | --- | --- | --- |
| P0 | 双端导航和权限边界收口 | 真实系统需要清晰区分顾客端和管理员端，避免客户看到管理入口或管理员误入客户流程 | V6.0 |
| P0 | 管理端关键动作权限与状态校验复查 | 当前功能增多后，需要集中复查审核、完成、草稿、评价等接口是否都在正确角色下 | V6.0 |
| P1 | 顾客端完成后可见客服回复与评价结果 | 评价已可提交，但顾客端还没有更完整的“处理结果说明”视图 | V6.0 |
| P1 | 自动化验证硬化 | 当前冒烟覆盖主链路，后续需要进一步拆分角色权限和失败场景测试 | V7.0 |

## V1.0 审视记录

- 完成日期：2026-05-06
- 本版改动文件：新增 `AfterSaleApplication`、`AfterSaleProcessLog` 等 POJO，新增 `AfterSaleApplicationMapper`、`AfterSaleProcessLogMapper` 和 XML，新增 `AfterSaleApplicationService` 及实现，新增 `/customer/after-sales` 与 `/admin/after-sales` REST 控制器，更新 `sql/schema.sql`、`sql/seed.sql`、`tools/full-smoke-test.ps1`。
- 验证命令和结果：`mvn -q -DskipTests package` 通过；`sql/schema.sql` 和 `sql/seed.sql` 已执行并确认新表可访问；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` 通过且 `FAILED_COUNT=0`；`npm run build` 通过；`npm run test:browser` 通过且 `FAILED_COUNT=0`。
- 业务价值：系统新增真实售后申请主对象 `after_sale_application` 和处理日志 `after_sale_process_log`，顾客可以提交售后申请，管理员可以在审核队列中通过或驳回，状态变更会留下处理时间线。
- 双端真实度审视：接口层已经区分顾客端 `/customer/after-sales` 和管理员端 `/admin/after-sales`，但页面层仍未形成顾客售后中心和管理员审核工作台，双端差异还没有在 UI 中完全体现。
- 状态机和权限审视：V1.0 已限制顾客只能提交自己的订单售后，管理员才能审核；审核动作只允许从 `SUBMITTED`、`UNDER_REVIEW` 流转。后续还需要补充取消、要求补充材料、确认完成、仓库收货、退款等动作。
- AI 辅助边界审视：本版没有让 AI 参与关键状态修改，符合“AI 只做辅助层”的边界。后续接入 AI 摘要和回复草稿时必须继续保持人工确认。
- 遗留问题：顾客端还没有售后中心页面；管理员端还没有审核队列页面；旧 `after_sale_record` 仍作为演示 CRUD 保留，后续需要决定是兼容展示还是逐步迁移；状态机只覆盖提交、通过、驳回三个动作。
- 下一轮优化：进入 V1.1，新增顾客端“我的售后中心”和申请/详情入口，让顾客不依赖聊天也能查看订单售后状态和处理日志。
- Git 提交：`feat: add real after-sale application workflow`。

## V1.1 审视记录

- 完成日期：2026-05-06
- 本版改动文件：新增 `web/src/api/customerAfterSaleApi.js`、`web/src/views/CustomerAfterSaleCenterView.vue`，更新 `web/src/router/index.js`、`web/src/components/layout/AppSidebar.vue`、`web/src/components/common/StatusTag.vue`、`web/src/views/LoginView.vue`、`web/browser-smoke.mjs`。
- 验证命令和结果：`npm run build` 通过；`npm run test:browser` 通过且覆盖顾客注册后默认进入“我的售后”、演示顾客提交售后申请和处理时间线展示；`mvn -q -DskipTests package` 通过；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` 通过且 `FAILED_COUNT=0`。
- 业务价值：顾客登录后不再默认进入聊天，而是进入“我的售后”；顾客能查看自己的订单、发起售后申请、查看售后单状态、下一步动作和处理时间线。
- 双端真实度审视：顾客端已经出现独立业务工作台，能完成“订单 -> 申请售后 -> 看进度”的第一版闭环。管理员端仍然停留在接口和旧看板层面，还缺真实审核队列页面。
- 状态机和权限审视：页面只调用顾客端 `/customer/after-sales` 接口，仍由后端校验订单归属和状态机。前端没有直接修改关键状态。
- AI 辅助边界审视：本版没有引入 AI 决策，保持售后申请是用户和状态机驱动，在线咨询只是辅助入口。
- 遗留问题：新注册客户默认没有订单，只能看到空订单/售后状态；顾客端还缺补充凭证、取消、确认完成和评价动作；管理员端审核队列仍未可视化。
- 下一轮优化：进入 V1.2，新增管理员售后审核队列和处理详情台，让管理员可以在页面上审核通过或驳回售后申请。
- Git 提交：`feat: add customer after-sale center`。

## V1.2 审视记录

- 完成日期：2026-05-06
- 本版改动文件：新增 `web/src/api/adminAfterSaleApi.js`、`web/src/views/AdminAfterSaleReviewView.vue`，更新管理员路由、侧边栏和 `web/browser-smoke.mjs`。
- 验证命令和结果：`mvn -q -DskipTests package` 通过；`npm run build` 通过；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` 通过且 `FAILED_COUNT=0`；`npm run test:browser` 通过且覆盖顾客提交售后申请、管理员审核台查询并审核通过。
- 业务价值：管理员端不再只能看旧看板或旧工单，已经有真实售后审核队列和处理台，可以查看申请原因、金额、风险等级、处理记录，并执行审核通过或驳回。
- 双端真实度审视：顾客端与管理员端已经形成第一版双端闭环：顾客提交申请，管理员在审核工作台处理，状态和日志回写。
- 状态机和权限审视：管理员页面调用 `/admin/after-sales/{id}/approve` 和 `/reject`，仍由后端限制可审核状态；驳回前端要求备注，后端也强制要求备注。
- AI 辅助边界审视：审核工作台展示 AI 摘要占位，但审核动作仍由管理员点击完成，AI 没有直接修改状态。
- 遗留问题：处理台还不能要求顾客补充材料，顾客也不能上传凭证；SLA 只是展示截止时间，尚未形成专门风险队列；工单与真实售后申请还未关联。
- 下一轮优化：进入 V2.0，新增凭证/补材料能力和证据链，让管理员可以要求补充材料、顾客可以提交文字或物流凭证。
- Git 提交：`feat: add admin after-sale review desk`。

## V2.0 审视记录

- 完成日期：2026-05-06
- 本版改动文件：新增 `AfterSaleEvidence`、`AfterSaleEvidenceRequest`、`AfterSaleEvidenceMapper` 和 XML，更新售后申请 Service/Controller、`sql/schema.sql`、`sql/seed.sql`、顾客售后中心、管理员审核工作台、API 封装、状态标签、全链路脚本和浏览器脚本。
- 验证命令和结果：执行 `sql/schema.sql` 和 `sql/seed.sql` 成功；`mvn -q -DskipTests package` 通过；`npm run build` 通过；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` 通过且覆盖管理员要求补材料、顾客补充物流凭证、管理员继续审核；`npm run test:browser` 通过且覆盖双端证据链展示。
- 业务价值：售后申请现在不再只是状态字段和备注，顾客可以补充文字或物流单号凭证，管理员可以要求补充材料并在处理台看到凭证列表。
- 双端真实度审视：顾客端和管理员端围绕同一售后单形成“要求补材料 -> 顾客补凭证 -> 管理员复核”的协作闭环，更接近真实售后系统。
- 状态机和权限审视：审视时发现 `NEED_MORE_EVIDENCE` 补完凭证后无法继续审核，已修正后端和前端可审核状态；顾客补凭证仍由后端校验只能操作自己的申请。
- AI 辅助边界审视：本版仍未让 AI 直接接触关键状态和凭证写入，AI 只保留摘要占位，符合辅助层定位。
- 遗留问题：当前凭证以文字/物流单号/链接为主，未实现真实图片文件上传；管理员还不能按 SLA 维度集中处理超时风险；凭证没有独立删除/审核动作。
- 下一轮优化：进入 V2.1，建设 SLA 和优先级队列，基于 `sla_deadline`、`priority` 和状态生成管理员风险队列。
- Git 提交：`feat: add after-sale evidence workflow`。

## V2.1 审视记录

- 完成日期：2026-05-06
- 本版改动文件：新增 `SlaTask`、`SlaTaskSearch`、`SlaTaskMapper`、`SlaTaskService`、`SlaTaskController` 和 XML，新增 `web/src/api/slaApi.js`、`web/src/views/SlaCenterView.vue`，更新路由、侧边栏、状态标签、管理员审核台 query 跳转、全链路脚本和浏览器脚本。
- 验证命令和结果：`mvn -q -DskipTests package` 通过；`npm run build` 通过；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` 通过且覆盖 `WAITING_CUSTOMER` SLA 队列；`npm run test:browser` 通过且覆盖管理员 SLA 中心筛选、看到待补材料风险、点击处理跳回审核台。
- 业务价值：管理员不再只能在审核列表里逐条找风险，已经可以按 SLA 风险类型、优先级和状态集中处理售后申请。
- 双端真实度审视：顾客端产生的待补材料状态会进入管理员 SLA 队列，管理员可从风险队列回到审核处理台，形成运营视角的任务闭环。
- 状态机和权限审视：SLA 中心只读风险，不自动改状态；所有处理动作仍回到审核工作台，由管理员显式执行。
- AI 辅助边界审视：SLA 风险完全基于业务字段计算，AI 不参与自动处置，符合辅助层定位。
- 遗留问题：目前没有后台定时扫描或通知机制；SLA 队列未和工单 `service_ticket` 直接合并；未生成处理绩效统计。
- 下一轮优化：进入 V3.0，打通真实售后申请与工单/客服处理记录，让投诉或异常申请可以形成工单并在处理记录中闭环。
- Git 提交：`feat: add after-sale SLA center`。

## V3.0 审视记录

- 完成日期：2026-05-06
- 本版改动文件：更新 `AfterSaleApplication`、`AfterSaleApplicationMapper` 和 XML，售后申请 Service/Controller 新增创建关联工单能力，工单更新接口回写售后处理记录，`sql/schema.sql` 新增 `ticket_id` 幂等迁移和 `CREATE_TICKET`、`UPDATE_TICKET` 日志动作约束；前端新增管理端创建关联工单按钮和状态标签；全链路脚本与浏览器脚本补充关联工单覆盖。
- 验证命令和结果：执行 `sql/schema.sql` 和 `sql/seed.sql` 成功；`mvn -q -DskipTests package` 通过；`npm run build` 通过；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` 通过且覆盖售后单创建关联工单、工单状态更新、售后处理日志出现 `CREATE_TICKET` 和 `UPDATE_TICKET`；`npm run test:browser` 通过且覆盖管理员审核台点击“创建关联工单”并看到“创建工单”日志。
- 业务价值：投诉、异常或复杂售后申请现在可以从售后审核台转成人工客服工单，工单后续处理状态会回写到同一售后单处理时间线，订单、售后、工单和日志可以串成一条证据链。
- 双端真实度审视：管理员端从“审核售后申请”进一步扩展为“审核后转人工继续处理”，更像真实后台任务台；顾客端本轮不新增入口，但其提交的售后申请已经能进入工单处理链路。
- 状态机和权限审视：创建工单仍由管理员接口触发；工单状态更新由 `@OperatorAnno` 管理端接口保护；售后状态本身不会因工单更新被自动改写，只追加审计日志，避免绕过售后状态机。
- AI 辅助边界审视：本版没有让 AI 自动创建工单或改状态，工单创建由管理员显式点击；现有 `aiSummary` 只随工单展示，仍是辅助信息。
- 遗留问题：售后单和工单目前是一对一软关联，未做多工单拆分；工单处理记录只回写状态更新摘要，还没有结构化处理备注、分派历史和客服绩效统计；顾客端尚不能看到关联工单的客服处理结果。
- 下一轮优化：进入 V4.0，建设 AI 副驾驶闭环，新增可审计的摘要、风险识别、知识推荐和回复草稿，要求 AI 建议必须能被管理员采纳、修改或废弃，且 AI 关闭后主业务仍可跑通。
- Git 提交：`feat: link after-sales with service tickets`。

## V4.0 审视记录

- 完成日期：2026-05-06
- 本版改动文件：新增 `reply_draft` 表、`ReplyDraft`/`ReplyDraftMapper`/`ReplyDraftService`/`AdminReplyDraftController`；更新 `sql/schema.sql`、管理员售后审核台、状态标签、`adminAfterSaleApi.js`、全链路脚本和浏览器脚本。
- 验证命令和结果：执行 `sql/schema.sql` 和 `sql/seed.sql` 成功；`mvn -q -DskipTests package` 通过；`npm run build` 通过；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` 通过且覆盖回复草稿生成、采纳、废弃和 `GENERATE_REPLY_DRAFT`、`USE_REPLY_DRAFT`、`DISCARD_REPLY_DRAFT` 日志；`npm run test:browser` 通过且覆盖管理员页面生成并采纳回复草稿。
- 业务价值：AI 不再只是售后单上的摘要占位，而是能结合售后单、凭证、处理日志和知识库生成给顾客看的回复草稿，并记录来源、模型、风险、知识依据和管理员处理结果。
- 双端真实度审视：管理员端现在有“AI 副驾驶回复草稿”面板，能在真实售后处理台中生成和确认建议；顾客端仍不直接接触 AI 草稿，避免把未审核内容展示给顾客。
- 状态机和权限审视：草稿生成、采纳、废弃均为管理员接口并写处理日志；这些动作只改变 `reply_draft` 状态，不会修改售后申请状态，避免绕过审核、退款、驳回等主状态机。
- AI 辅助边界审视：AI 失败或关闭时会生成本地模板草稿；AI 生成内容必须由管理员采纳才算进入业务记录；AI 不能直接创建退款、驳回或完成动作，符合副驾驶边界。
- 遗留问题：草稿采纳后还没有真正发送给顾客；顾客端还没有看到处理满意度和客服回复；客户画像与重复投诉风险尚未建设；AI 质量统计仍分散在日志中心，没有专门的草稿质量评估。
- 下一轮优化：进入 V5.0，新增顾客评价和客户画像，把已处理售后、工单、草稿采纳记录和顾客反馈汇总成管理员可查看的客户历史。
- Git 提交：`feat: add auditable AI reply drafts`。

## V5.0 审视记录

- 完成日期：2026-05-06
- 本版改动文件：新增 `service_review` 表、`ServiceReview`/`CustomerProfile` POJO、`ServiceReviewMapper`、`ServiceReviewService`、`ServiceReviewController`；售后申请新增管理员 `complete` 动作；更新 `ServiceTicketMapper` 用户聚合查询、`sql/schema.sql`、顾客售后中心、管理员客户画像页、路由、侧边栏、状态标签、全链路脚本和浏览器脚本。
- 验证命令和结果：执行 `sql/schema.sql` 和 `sql/seed.sql` 成功；`mvn -q -DskipTests package` 通过；`npm run build` 通过；`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` 通过且覆盖管理员确认完成、顾客提交评价、管理员客户画像聚合；`npm run test:browser` 通过且覆盖顾客评价弹窗、评价展示、管理员客户画像和评价内容可见。
- 业务价值：售后流程现在不仅能提交、审核、补凭证、转工单和生成草稿，还能由管理员确认完成，并让顾客对完成结果打分评价；管理员能在客户画像中看到订单、售后、工单、评价和风险等级。
- 双端真实度审视：顾客端有“已完成 -> 评价服务 -> 我的评价”的反馈闭环；管理员端有“客户画像”入口，可从客户维度复盘售后历史和满意度。
- 状态机和权限审视：评价只能由售后单所属顾客提交，且要求售后状态为 `COMPLETED`；完成动作只能由管理员触发，仍写入 `CONFIRM` 处理日志；客户画像接口受管理员权限保护。
- AI 辅助边界审视：本版没有新增 AI 自动决策，画像风险基于订单、售后、工单和评价数据计算；AI 草稿仍只是前一版的建议层，不会影响评价和完成状态。
- 遗留问题：顾客端的“处理结果说明”仍主要来自时间线和评价摘要，没有独立的客服最终回复；客户画像统计还偏轻量，未做时间窗口、复购率和低分原因分布；权限测试仍主要通过浏览器主链路间接覆盖。
- 下一轮优化：进入 V6.0，收口双端导航和权限边界，集中复查顾客/管理员入口、关键动作、状态机和接口角色限制。
- Git 提交：`feat: add service reviews and customer profiles`。

## 5. 每版审视模板

每完成一个版本，都在这里追加一段记录：

```md
## Vx.x 审视记录

- 完成日期：
- 本版改动文件：
- 验证命令和结果：
- 业务价值：
- 双端真实度审视：
- 状态机和权限审视：
- AI 辅助边界审视：
- 遗留问题：
- 下一轮优化：
- Git 提交：
```

## 6. 停止前最终检查清单

- [ ] 顾客端完整闭环：订单、申请、凭证、进度、补充、确认、评价。
- [ ] 管理员端完整闭环：审核、驳回、补材料、分派、处理、SLA、客户历史。
- [ ] AI 可关闭，关闭后主业务流程仍可跑通。
- [ ] 状态机禁止非法流转，并记录每次变更。
- [ ] 证据链完整串联订单、售后、工单、凭证、评价、日志。
- [ ] 自动化验证覆盖主流程。
- [ ] 进度文档无 P0/P1 未处理问题。
