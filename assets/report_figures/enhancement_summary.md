# 结项报告图表与数据库增强摘要

- 输出 DOCX：D:\复制软件系统\docs\结项报告_刘剑宇个人版_图表数据库增强版.docx
- 输出 Markdown：D:\复制软件系统\docs\结项报告_刘剑宇个人版_图表数据库增强版.md
- 工程图源目录：D:\复制软件系统\assets\report_figures
- 中文字符数：23434
- 新增工程图：7 张
- 保留真实系统截图：11 张
- 新增/整理表格：10 张
- PDF 渲染页数：43
- PDF 文件：D:\复制软件系统\tmp\final-report-enhanced-render\结项报告_刘剑宇个人版_图表数据库增强版.pdf

## 新增工程图
- 系统总体技术架构图：fig_a_system_architecture.mmd / fig_a_system_architecture.png
- 数据库核心实体关系图：fig_b_database_er.mmd / fig_b_database_er.png
- 售后状态机图：fig_c_after_sale_state_machine.mmd / fig_c_after_sale_state_machine.png
- 顾客提交售后到管理员审核时序图：fig_d_submit_after_sale_sequence.mmd / fig_d_submit_after_sale_sequence.png
- AI 草稿生成与审计链路图：fig_e_reply_draft_audit_flow.mmd / fig_e_reply_draft_audit_flow.png
- 图片风险与 C2PA 预审数据流图：fig_f_image_risk_c2pa_flow.mmd / fig_f_image_risk_c2pa_flow.png
- SLA 与人工工单协同流程图：fig_g_sla_ticket_flow.mmd / fig_g_sla_ticket_flow.png

## 真实性说明
- 数据库字段按 sql/schema.sql 校对：reason_text、assigned_to、from_status/to_status、uploaded_by 等字段采用真实名称。
- service_ticket 当前 SQL 未声明 application_id 外键；报告改写为 after_sale_application.ticket_id 的业务回指关系。
- AI 草稿没有写成自动业务决策，图片风险与 C2PA 只写成可信度预审信号。