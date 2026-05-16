# 结项报告图表与数据库增强摘要

- 输出 DOCX：D:\复制软件系统\docs\结项报告_刘剑宇个人版_图表数据库增强版.docx
- 输出 Markdown：D:\复制软件系统\docs\结项报告_刘剑宇个人版_图表数据库增强版.md
- 工程图源目录：D:\复制软件系统\assets\report_figures
- 中文字符数：23434
- 新增工程图：7 张
- 第四章章节示意图：24 张
- 保留真实系统截图：11 张
- 新增/整理表格：10 张
- PDF 渲染页数：56
- PDF 文件：D:\复制软件系统\tmp\final-report-enhanced-render\结项报告_刘剑宇个人版_图表数据库增强版.pdf

## 新增工程图
- 系统总体技术架构图：fig_a_system_architecture.mmd / fig_a_system_architecture.png
- 数据库核心实体关系图：fig_b_database_er.mmd / fig_b_database_er.png
- 售后状态机图：fig_c_after_sale_state_machine.mmd / fig_c_after_sale_state_machine.png
- 顾客提交售后到管理员审核时序图：fig_d_submit_after_sale_sequence.mmd / fig_d_submit_after_sale_sequence.png
- AI 草稿生成与审计链路图：fig_e_reply_draft_audit_flow.mmd / fig_e_reply_draft_audit_flow.png
- 图片风险与 C2PA 预审数据流图：fig_f_image_risk_c2pa_flow.mmd / fig_f_image_risk_c2pa_flow.png
- SLA 与人工工单协同流程图：fig_g_sla_ticket_flow.mmd / fig_g_sla_ticket_flow.png

## 第四章章节示意图
- 4.1 登录注册与双端权限控制：fig_4_01_login_permission.png
- 4.2 顾客端我的售后：fig_4_02_customer_after_sale.png
- 4.3 管理员售后审核工作台：fig_4_03_admin_review.png
- 4.4 咨询工作台与多轮客服链路：fig_4_04_chat_workbench.png
- 4.5 知识库管理与 RAG 依据展示：fig_4_05_knowledge_rag.png
- 4.6 订单管理与售后上下文：fig_4_06_order_context.png
- 4.7 SLA 跟进与风险任务队列：fig_4_07_sla_queue.png
- 4.8 人工工单协同：fig_4_08_ticket_collaboration.png
- 4.9 AI 副驾驶可审计回复草稿：fig_4_09_audit_reply_draft.png
- 4.10 聊天图片风险扫描与 C2PA/图片可信度检测：fig_4_10_image_risk_c2pa.png
- 4.11 客户画像与服务评价：fig_4_11_customer_profile_review.png
- 4.12 商品质量问题聚合预警：fig_4_12_product_quality_alert.png
- 4.13 日志诊断与 AI 质检：fig_4_13_log_ai_quality.png
- 4.14 售后前置诊断、风险评估与凭证审核：fig_4_14_diagnosis_risk_evidence.png
- 4.15 运营首页与系统状态展示：fig_4_15_operation_status.png
- 4.16 真实双端售后业务闭环：fig_4_16_dual_after_sale_loop.png
- 4.17 SLA 跟进与人工工单协同：fig_4_17_sla_ticket_collaboration.png
- 4.18 AI 副驾驶回复草稿：fig_4_18_ai_reply_draft.png
- 4.19 图片风险扫描与 C2PA 可信度检测：fig_4_19_image_risk_detail.png
- 4.20 数据库实现与数据一致性验证：fig_4_20_database_consistency.png
- 4.21 测试与验证：fig_4_21_validation_matrix.png
- 4.22 项目特色与创新点：fig_4_22_project_innovation.png
- 4.23 个人负责模块与数据库支撑关系：fig_4_23_personal_database_mapping.png
- 4.24 个人负责模块补充说明：fig_4_24_personal_module_summary.png

## 真实性说明
- 数据库字段按 sql/schema.sql 校对：reason_text、assigned_to、from_status/to_status、uploaded_by 等字段采用真实名称。
- service_ticket 当前 SQL 未声明 application_id 外键；报告改写为 after_sale_application.ticket_id 的业务回指关系。
- AI 草稿没有写成自动业务决策，图片风险与 C2PA 只写成可信度预审信号。
- 第四章章节示意图围绕本项目 Spring Boot + Vue 3 + MySQL + LangChain4j、售后、工单、知识库、日志和图片风险预审等实际模块组织。