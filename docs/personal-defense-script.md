# 个人答辩讲解稿

## 1. 个人负责内容概述

我在本项目中主要围绕“电商退换货智能客服系统”的核心链路进行设计和实现，重点包括后端业务编排、AI 增强链路、知识库检索、工单升级、日志追踪、前端展示入口和自动化验证。

我的讲解主线可以概括为：

```text
完整售后业务流程
-> AI 和本地规则协同
-> 可解释的知识依据与日志
-> 工单闭环
-> 前端答辩展示和验证证据
```

我不会把项目讲成单纯调用大模型的聊天页面，而是强调它是一个包含订单、售后、知识库、AI、人工客服和日志追踪的完整业务系统。

## 2. 核心模块一：聊天主链路

核心代码位置：

- `server/src/main/java/com/user/returnsassistant/service/impl/ChatServiceImpl.java`
- `server/src/main/java/com/user/returnsassistant/controller/ChatSessionController.java`
- `web/src/views/ChatWorkbenchView.vue`
- `web/src/stores/chatStore.js`

讲解要点：

聊天主链路由 `POST /chat-sessions/{id}/message-stream` 或 `POST /chat-sessions/{id}/messages` 触发。前端发送用户问题后，后端不是直接把问题丢给 AI，而是先按业务流程处理：

1. 保存用户消息。
2. 解析多轮上下文。
3. 识别售后意图。
4. 查询订单和售后状态。
5. 检索知识库规则。
6. 生成本地规则回复。
7. 调用 LangChain4j 进行 AI 增强。
8. 失败时回退本地规则。
9. 判断是否需要人工工单。
10. 保存 AI 日志、检索日志和处理轨迹。

答辩时可以打开咨询工作台，输入“这个订单能不能退货？”，展示右侧意图、订单上下文、知识命中和回答过程。继续追问“那退款一般多久到账？”，展示系统能承接上一轮上下文。

## 3. 核心模块二：多轮上下文与意图识别

核心代码位置：

- `ChatServiceImpl.buildConversationContext`
- `ChatServiceImpl.recognizeIntent`
- `ChatServiceImpl.contextualIntent`

讲解要点：

系统需要处理真实客服对话中的省略式追问。例如用户先问“这个订单能不能退货？”，下一轮只问“那多久到账？”。如果只看当前文本，“那”指代不清楚，系统可能不知道用户在问什么。因此我在后端保存会话摘要、读取最近消息和上轮意图，并用规则判断当前问题是否属于追问。

判断追问的信号包括：

- “那、这个、刚才、上面、继续”等指代词。
- “多久到账、怎么办、可以吗、需要什么”等短问题。
- 当前问题缺少明确独立意图词，但会话中已有上一轮意图。

这样设计的原因是稳定性更高：多轮承接不完全依赖大模型，模型不可用时仍能完成常见追问解析。

## 4. 核心模块三：RAG 知识依据

核心代码位置：

- `server/src/main/java/com/user/returnsassistant/service/impl/ChatServiceImpl.java`
- `server/src/main/resources/mapper/KnowledgeDocMapper.xml`
- `server/src/main/resources/mapper/RetrievalLogMapper.xml`
- `web/src/views/KnowledgeDocView.vue`
- `web/src/views/LogCenterView.vue`

讲解要点：

系统将退货、换货、退款、物流异常和投诉转人工规则维护在 `knowledge_doc` 表中。聊天时会根据用户问题和识别出的意图检索知识库，命中文档会展示在前端右侧，也会写入 `retrieval_log`。

这部分的价值是让 AI 回复可解释。老师如果问“怎么证明模型不是乱说”，可以打开知识库和日志诊断中心，先看知识命中和高频命中文档摘要，再切到检索日志展示命中的规则文档。

## 5. 核心模块四：LangChain4j 与本地兜底

核心代码位置：

- `server/src/main/java/com/user/returnsassistant/service/impl/AiServiceImpl.java`
- `server/src/main/java/com/user/returnsassistant/service/impl/AiBusinessToolServiceImpl.java`
- `server/src/main/resources/application.yml`

讲解要点：

LangChain4j 在系统中只作为 AI 增强层，不替代 Spring Boot 业务逻辑。订单是否可退、是否已有售后、是否要转人工，这些都由后端业务服务判断。AI 接收到的是已经整理好的业务上下文和知识依据，用来生成更自然的客服回复。

为了保证答辩稳定性，我保留了本地规则兜底：

- 没有配置 `OPENAI_API_KEY` 时，系统返回 `SKIPPED` 并使用本地回复。
- 模型调用失败时，系统记录 `FAILED` 并返回本地回复。
- 聊天主链路不因为 AI 失败而中断。

这体现了工程取舍：AI 提升体验，但基础业务必须可靠。

## 6. 核心模块五：智能工单升级

核心代码位置：

- `server/src/main/java/com/user/returnsassistant/service/impl/ServiceTicketServiceImpl.java`
- `server/src/main/java/com/user/returnsassistant/controller/ServiceTicketController.java`
- `web/src/views/ServiceTicketView.vue`
- `web/src/components/chat/TicketPanel.vue`

讲解要点：

当用户表达“投诉”“人工客服”“商家不处理”“平台介入”等诉求，系统会自动创建或复用人工客服工单。工单记录用户问题、订单、会话、意图、AI 摘要、处理建议、优先级和状态。

这让系统从“问答系统”升级为“客服业务系统”。答辩时可以在聊天工作台输入“商家一直不处理可以转人工投诉吗？”，然后切到人工工单页面展示工单号、优先级、AI 摘要和处理状态。

## 7. 核心模块六：权限与角色隔离

核心代码位置：

- `server/src/main/java/com/user/returnsassistant/config/AuthInterceptor.java`
- `server/src/main/java/com/user/returnsassistant/service/impl/AuthServiceImpl.java`
- `web/src/router/index.js`
- `web/browser-role-smoke.mjs`

讲解要点：

系统支持管理员和客户两个角色。客户注册后默认只能进入咨询工作台，不能访问后台管理页。后端对管理操作使用 `@OperatorAnno` 做管理员校验，前端路由也用 `adminOnly` 限制菜单和访问。

验证证据是 `npm.cmd run test:browser:roles`，该脚本确认客户看不到“答辩展示、系统总览、知识库、订单管理、人工工单、日志中心、AI 测试”等后台菜单，并且直接访问对应路由会被重定向回 `/chat`。

## 8. 核心模块七：答辩展示中心

核心代码位置：

- `web/src/views/ShowcaseView.vue`
- `web/src/router/index.js`
- `web/src/components/layout/AppSidebar.vue`
- `docs/demo-script.md`

讲解要点：

为了让老师快速理解系统完整度，我新增了 `/showcase` 答辩展示中心。管理员登录后默认进入该页面。页面集中展示系统主题、数据库和 AI 状态、兜底策略、演示流程、8 个已实现亮点、答辩讲解骨架和现场检查清单。

这个页面的作用不是替代业务页面，而是作为高分展示入口，让老师先看到完整项目结构，再按页面链接进入咨询工作台、知识库、工单、日志诊断和 AI 测试。

## 9. 验证证据

最近一次完整验证包括：

| 验证项 | 命令 | 结果 |
| --- | --- | --- |
| 后端打包 | `cd server; mvn -q -DskipTests package` | 通过 |
| 前端构建 | `cd web; npm.cmd run build` | 通过 |
| 全链路接口烟测 | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\full-smoke-test.ps1` | `FAILED_COUNT=0` |
| 浏览器主流程烟测 | `cd web; npm.cmd run test:browser` | `FAILED_COUNT=0` |
| 浏览器角色权限烟测 | `cd web; npm.cmd run test:browser:roles` | `FAILED_COUNT=0` |

浏览器截图保存在 `output/playwright/`，其中 `01-showcase.png` 是答辩展示中心截图，`09-logs.png` 是日志诊断中心截图。由于 `output/` 是运行产物目录，不提交到 Git，但可以作为本机答辩证据使用。

配套个人答辩 PPT 位于 `docs/personal-defense-slides.pptx`，内容按“完整链路、RAG 依据、LangChain4j 取舍、工单闭环、前端展示、日志诊断、验证证据、追问回答”组织，适合三到五分钟个人答辩。

## 10. 三分钟口头版本

老师好，我做的是一个电商退换货智能客服系统。它不是单纯的聊天框，而是把登录权限、订单售后、知识库、AI 回复、人工工单和日志追踪串成了一条完整业务链路。

系统前端用 Vue 3，后端用 Spring Boot，数据库是 MySQL，AI 增强层使用 LangChain4j。用户在咨询工作台输入问题后，后端会先做多轮上下文解析和意图识别，再查询订单状态、检索知识库规则，生成本地业务判断，然后再调用模型做自然语言增强。如果模型失败或没有配置 key，系统仍然可以用本地规则兜底，所以答辩现场不会因为网络或模型问题导致核心流程不可用。

我的重点工作包括聊天主链路、多轮追问、RAG 知识依据、LangChain4j 工具调用、本地兜底、自动工单和日志追踪。比如用户先问“这个订单能不能退货”，再问“那多久到账”，系统能承接上文识别为退款进度；如果用户说商家一直不处理，系统会自动生成工单，并记录 AI 摘要和处理建议。

为了方便答辩，我还做了答辩展示中心，把 8 个已实现亮点、演示顺序和验证入口集中起来。验证方面，我跑过后端打包、前端构建、全链路接口烟测、浏览器主流程烟测和角色权限烟测，结果都是通过。

## 11. 可能被追问的个人贡献回答

如果老师问“你主要做了什么”，可以回答：

我主要负责把智能客服从单点问答扩展成完整售后业务闭环，包括聊天主流程、多轮上下文、RAG 知识依据、AI 增强与本地兜底、人工工单、日志追踪和答辩展示入口。我的重点不是只调通模型，而是让模型在 Spring Boot 业务规则之后增强回复，同时保证没有模型时系统也能稳定演示。

如果老师问“难点是什么”，可以回答：

难点是 AI 生成和业务规则之间的边界。售后判断不能交给模型自由发挥，所以我把订单状态、售后状态、知识库命中和工单规则放在后端业务层处理，AI 只拿整理好的上下文生成更自然的回复。这样既能展示 AI 特色，又不会破坏业务可靠性。
