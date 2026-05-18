USE test3;
SET NAMES utf8mb4;

START TRANSACTION;

DELETE FROM evidence_audit
WHERE application_id IN (
  SELECT id FROM after_sale_application WHERE application_no LIKE 'OPSASA%'
)
   OR evidence_id IN (
  SELECT e.id
  FROM after_sale_evidence e
  JOIN after_sale_application a ON e.application_id=a.id
  WHERE a.application_no LIKE 'OPSASA%'
);

DELETE FROM after_sale_risk_assessment
WHERE application_id IN (
  SELECT id FROM after_sale_application WHERE application_no LIKE 'OPSASA%'
);

DELETE FROM service_review
WHERE application_id IN (
  SELECT id FROM after_sale_application WHERE application_no LIKE 'OPSASA%'
);

DELETE FROM reply_draft
WHERE application_id IN (
  SELECT id FROM after_sale_application WHERE application_no LIKE 'OPSASA%'
)
   OR ticket_id IN (
  SELECT id FROM service_ticket WHERE ticket_no LIKE 'OPSTK%'
);

DELETE FROM after_sale_evidence
WHERE application_id IN (
  SELECT id FROM after_sale_application WHERE application_no LIKE 'OPSASA%'
);

DELETE FROM after_sale_process_log
WHERE application_id IN (
  SELECT id FROM after_sale_application WHERE application_no LIKE 'OPSASA%'
);

DELETE FROM after_sale_record
WHERE after_sale_no LIKE 'OPSASR%'
   OR order_id IN (
  SELECT id FROM demo_order WHERE order_no LIKE 'OPS2026%'
);

UPDATE after_sale_application
SET ticket_id=NULL
WHERE ticket_id IN (SELECT id FROM service_ticket);

UPDATE reply_draft
SET ticket_id=NULL
WHERE ticket_id IN (SELECT id FROM service_ticket);

DELETE FROM service_ticket;

DELETE FROM intent_record;

DELETE FROM retrieval_log;

DELETE FROM ai_call_log;

DELETE FROM process_trace;

DELETE FROM chat_message;

DELETE FROM chat_session;

UPDATE knowledge_doc
SET deleted=1,
    status='DISABLED',
    updated_at=NOW()
WHERE deleted=0
  AND (
    title='测试'
    OR title LIKE '%测试%'
    OR title LIKE '%浏览器%'
    OR LOWER(title) LIKE '%browser%'
    OR LOWER(title) LIKE '%auto test%'
    OR LOWER(title) LIKE '%upload debug%'
    OR question LIKE '%测试%'
    OR content LIKE '%浏览器测试%'
    OR LOWER(content) LIKE '%browser test%'
  );

DELETE FROM after_sale_application
WHERE application_no LIKE 'OPSASA%';

DELETE FROM product_issue_alert;

DELETE FROM demo_order
WHERE order_no LIKE 'OPS2026%';

DELETE FROM user_account
WHERE username IN (
  'ops_admin_lina',
  'ops_admin_chen',
  'ops_customer_yan',
  'ops_customer_luo',
  'ops_customer_wu',
  'ops_customer_he',
  'ops_customer_qin',
  'ops_customer_fang',
  'ops_customer_tang',
  'ops_customer_song',
  'ops_customer_zhao',
  'ops_customer_han'
);

INSERT INTO user_account(username, display_name, role, phone, password_hash, status, created_at, updated_at)
VALUES
('ops_admin_lina', '李娜（售后主管）', 'ADMIN', '13910001001', NULL, 1, '2026-04-01 09:10:00', '2026-05-12 09:10:00'),
('ops_admin_chen', '陈磊（客服专员）', 'ADMIN', '13910001002', NULL, 1, '2026-04-01 09:18:00', '2026-05-12 09:18:00'),
('ops_customer_yan', '严小雪', 'CUSTOMER', '13810002001', NULL, 1, '2026-04-03 10:22:00', '2026-05-12 08:40:00'),
('ops_customer_luo', '罗浩然', 'CUSTOMER', '13810002002', NULL, 1, '2026-04-04 11:04:00', '2026-05-11 21:05:00'),
('ops_customer_wu', '吴嘉怡', 'CUSTOMER', '13810002003', NULL, 1, '2026-04-06 14:32:00', '2026-05-12 10:20:00'),
('ops_customer_he', '何明轩', 'CUSTOMER', '13810002004', NULL, 1, '2026-04-08 09:45:00', '2026-05-10 19:12:00'),
('ops_customer_qin', '秦雨晴', 'CUSTOMER', '13810002005', NULL, 1, '2026-04-09 15:26:00', '2026-05-12 11:08:00'),
('ops_customer_fang', '方子涵', 'CUSTOMER', '13810002006', NULL, 1, '2026-04-12 13:17:00', '2026-05-09 17:36:00'),
('ops_customer_tang', '唐亦辰', 'CUSTOMER', '13810002007', NULL, 1, '2026-04-15 16:43:00', '2026-05-12 12:04:00'),
('ops_customer_song', '宋佳宁', 'CUSTOMER', '13810002008', NULL, 1, '2026-04-17 08:55:00', '2026-05-11 13:28:00'),
('ops_customer_zhao', '赵启航', 'CUSTOMER', '13810002009', NULL, 1, '2026-04-20 18:20:00', '2026-05-12 14:18:00'),
('ops_customer_han', '韩沐阳', 'CUSTOMER', '13810002010', NULL, 1, '2026-04-23 12:05:00', '2026-05-12 15:46:00');

INSERT INTO demo_order(order_no, user_id, product_name, sku_name, order_amount, pay_status, order_status, logistics_status, after_sale_status, paid_at, shipped_at, signed_at, created_at, updated_at)
SELECT 'OPS202605120001', u.id, '智能恒温水壶', '白色 1.7L', 159.00, 'PAID', 'COMPLETED', 'DELIVERED', 'NONE', '2026-04-22 09:12:00', '2026-04-22 17:20:00', '2026-04-24 11:06:00', '2026-04-22 09:10:00', '2026-05-12 09:20:00' FROM user_account u WHERE u.username='ops_customer_yan'
UNION ALL SELECT 'OPS202605120002', u.id, '无线蓝牙耳机 Pro', '米白色 降噪版', 329.00, 'PAID', 'SIGNED', 'DELIVERED', 'EXCHANGE_APPLYING', '2026-05-01 10:02:00', '2026-05-01 19:05:00', '2026-05-03 14:16:00', '2026-05-01 10:00:00', '2026-05-12 11:12:00' FROM user_account u WHERE u.username='ops_customer_yan'
UNION ALL SELECT 'OPS202605120003', u.id, '家用空气炸锅', '绿色 5L', 268.80, 'REFUNDED', 'COMPLETED', 'DELIVERED', 'FINISHED', '2026-04-18 13:50:00', '2026-04-18 20:10:00', '2026-04-20 12:30:00', '2026-04-18 13:48:00', '2026-05-09 16:50:00' FROM user_account u WHERE u.username='ops_customer_luo'
UNION ALL SELECT 'OPS202605120004', u.id, '机械键盘', '青轴 RGB', 299.00, 'REFUNDING', 'SIGNED', 'DELIVERED', 'REFUNDING', '2026-04-28 20:11:00', '2026-04-29 11:20:00', '2026-05-01 09:44:00', '2026-04-28 20:10:00', '2026-05-12 08:55:00' FROM user_account u WHERE u.username='ops_customer_luo'
UNION ALL SELECT 'OPS202605120005', u.id, '护眼台灯', '银灰色 标准款', 189.90, 'PAID', 'SHIPPED', 'IN_TRANSIT', 'NONE', '2026-05-09 08:30:00', '2026-05-09 18:02:00', NULL, '2026-05-09 08:29:00', '2026-05-12 10:05:00' FROM user_account u WHERE u.username='ops_customer_wu'
UNION ALL SELECT 'OPS202605120006', u.id, '运动手表', '黑色 GPS版', 459.00, 'PAID', 'SIGNED', 'DELIVERED', 'RETURN_APPLYING', '2026-05-02 15:30:00', '2026-05-02 22:08:00', '2026-05-04 16:12:00', '2026-05-02 15:28:00', '2026-05-12 12:18:00' FROM user_account u WHERE u.username='ops_customer_wu'
UNION ALL SELECT 'OPS202605120007', u.id, '轻薄笔记本支架', '铝合金 折叠款', 99.00, 'PAID', 'SIGNED', 'DELIVERED', 'RETURNING', '2026-04-25 12:16:00', '2026-04-25 19:30:00', '2026-04-27 10:10:00', '2026-04-25 12:14:00', '2026-05-11 18:40:00' FROM user_account u WHERE u.username='ops_customer_he'
UNION ALL SELECT 'OPS202605120008', u.id, '便携榨汁杯', '粉色 双杯盖', 119.00, 'PAID', 'COMPLETED', 'DELIVERED', 'NONE', '2026-04-19 09:58:00', '2026-04-19 16:21:00', '2026-04-21 13:28:00', '2026-04-19 09:55:00', '2026-05-08 10:20:00' FROM user_account u WHERE u.username='ops_customer_he'
UNION ALL SELECT 'OPS202605120009', u.id, '旅行收纳箱', '24寸 湖蓝色', 239.00, 'PAID', 'SIGNED', 'DELIVERED', 'NONE', '2026-05-06 18:18:00', '2026-05-07 09:10:00', '2026-05-09 17:32:00', '2026-05-06 18:15:00', '2026-05-12 13:16:00' FROM user_account u WHERE u.username='ops_customer_qin'
UNION ALL SELECT 'OPS202605120010', u.id, '洗地机配件包', '滚刷+滤芯组合', 89.90, 'PAID', 'SIGNED', 'DELIVERED', 'EXCHANGE_APPLYING', '2026-05-03 11:45:00', '2026-05-03 18:12:00', '2026-05-05 15:08:00', '2026-05-03 11:42:00', '2026-05-12 09:45:00' FROM user_account u WHERE u.username='ops_customer_qin'
UNION ALL SELECT 'OPS202605120011', u.id, '4K显示器', '27英寸 Type-C', 1299.00, 'PAID', 'SHIPPED', 'ABNORMAL', 'NONE', '2026-05-07 20:02:00', '2026-05-08 11:28:00', NULL, '2026-05-07 20:00:00', '2026-05-12 16:35:00' FROM user_account u WHERE u.username='ops_customer_fang'
UNION ALL SELECT 'OPS202605120012', u.id, '儿童学习平板', '护眼版 64G', 799.00, 'PAID', 'SIGNED', 'DELIVERED', 'REJECTED', '2026-04-30 10:10:00', '2026-04-30 18:33:00', '2026-05-02 10:26:00', '2026-04-30 10:08:00', '2026-05-10 12:36:00' FROM user_account u WHERE u.username='ops_customer_fang'
UNION ALL SELECT 'OPS202605120013', u.id, '即热饮水机', '米色 3L', 369.00, 'PAID', 'COMPLETED', 'DELIVERED', 'NONE', '2026-04-24 14:15:00', '2026-04-24 21:05:00', '2026-04-26 09:50:00', '2026-04-24 14:12:00', '2026-05-07 09:15:00' FROM user_account u WHERE u.username='ops_customer_tang'
UNION ALL SELECT 'OPS202605120014', u.id, '家用路由器', 'AX3000 双频', 229.00, 'PAID', 'SIGNED', 'DELIVERED', 'NONE', '2026-05-10 10:10:00', '2026-05-10 18:30:00', '2026-05-12 09:28:00', '2026-05-10 10:08:00', '2026-05-12 09:28:00' FROM user_account u WHERE u.username='ops_customer_tang'
UNION ALL SELECT 'OPS202605120015', u.id, '电动牙刷', '蓝色 双刷头', 169.00, 'REFUNDED', 'COMPLETED', 'DELIVERED', 'FINISHED', '2026-04-16 08:36:00', '2026-04-16 16:06:00', '2026-04-18 12:20:00', '2026-04-16 08:35:00', '2026-05-05 18:26:00' FROM user_account u WHERE u.username='ops_customer_song'
UNION ALL SELECT 'OPS202605120016', u.id, '智能门锁', '深空灰 指纹款', 899.00, 'PAID', 'PAID', 'NOT_SHIPPED', 'NONE', '2026-05-12 08:15:00', NULL, NULL, '2026-05-12 08:13:00', '2026-05-12 08:15:00' FROM user_account u WHERE u.username='ops_customer_song'
UNION ALL SELECT 'OPS202605120017', u.id, '加湿器', '白色 4L 静音款', 139.00, 'PAID', 'COMPLETED', 'DELIVERED', 'NONE', '2026-04-21 11:06:00', '2026-04-21 19:44:00', '2026-04-23 15:10:00', '2026-04-21 11:02:00', '2026-05-06 16:44:00' FROM user_account u WHERE u.username='ops_customer_zhao'
UNION ALL SELECT 'OPS202605120018', u.id, '电饭煲', '3L 智能预约', 259.00, 'PAID', 'SIGNED', 'DELIVERED', 'RETURN_APPLYING', '2026-05-04 09:21:00', '2026-05-04 17:50:00', '2026-05-06 11:09:00', '2026-05-04 09:18:00', '2026-05-12 15:20:00' FROM user_account u WHERE u.username='ops_customer_zhao'
UNION ALL SELECT 'OPS202605120019', u.id, '高清摄像头', '1080P 带麦克风', 149.00, 'PAID', 'COMPLETED', 'DELIVERED', 'NONE', '2026-04-14 13:22:00', '2026-04-14 20:05:00', '2026-04-16 10:00:00', '2026-04-14 13:19:00', '2026-05-03 10:40:00' FROM user_account u WHERE u.username='ops_customer_han'
UNION ALL SELECT 'OPS202605120020', u.id, '便携投影仪', '标准版 1080P', 699.00, 'PAID', 'SIGNED', 'DELIVERED', 'RETURNING', '2026-05-05 16:40:00', '2026-05-05 23:10:00', '2026-05-07 18:30:00', '2026-05-05 16:36:00', '2026-05-12 16:10:00' FROM user_account u WHERE u.username='ops_customer_han'
UNION ALL SELECT 'OPS202605120021', u.id, '除螨仪', '紫外线加强版', 209.00, 'PAID', 'COMPLETED', 'DELIVERED', 'NONE', '2026-04-26 18:45:00', '2026-04-27 09:40:00', '2026-04-29 10:28:00', '2026-04-26 18:42:00', '2026-05-11 12:50:00' FROM user_account u WHERE u.username='ops_customer_yan'
UNION ALL SELECT 'OPS202605120022', u.id, '蓝牙音箱', '黑色 低音增强', 199.00, 'PAID', 'SIGNED', 'DELIVERED', 'EXCHANGE_APPLYING', '2026-05-08 12:10:00', '2026-05-08 19:22:00', '2026-05-10 15:18:00', '2026-05-08 12:08:00', '2026-05-12 17:05:00' FROM user_account u WHERE u.username='ops_customer_luo'
UNION ALL SELECT 'OPS202605120023', u.id, '保温杯', '咖色 500ml', 79.00, 'PAID', 'COMPLETED', 'DELIVERED', 'NONE', '2026-04-27 10:35:00', '2026-04-27 18:20:00', '2026-04-29 11:12:00', '2026-04-27 10:33:00', '2026-05-04 13:38:00' FROM user_account u WHERE u.username='ops_customer_wu'
UNION ALL SELECT 'OPS202605120024', u.id, '空气净化器滤芯', '一年装', 188.00, 'PAID', 'SIGNED', 'DELIVERED', 'NONE', '2026-05-09 21:08:00', '2026-05-10 10:15:00', '2026-05-12 10:55:00', '2026-05-09 21:05:00', '2026-05-12 14:50:00' FROM user_account u WHERE u.username='ops_customer_qin';

INSERT INTO after_sale_record(after_sale_no, order_id, service_type, reason, status, refund_amount, apply_at, handle_at, remark, created_at, updated_at)
SELECT 'OPSASR202605120001', o.id, 'RETURN', '商品外壳有明显划痕，用户要求退货', 'FINISHED', 268.80, '2026-04-21 10:20:00', '2026-05-09 16:30:00', '已完成退款，用户确认处理结果', '2026-04-21 10:20:00', '2026-05-09 16:50:00' FROM demo_order o WHERE o.order_no='OPS202605120003'
UNION ALL SELECT 'OPSASR202605120002', o.id, 'REFUND', '键盘部分按键连击，申请仅退款', 'REFUNDING', 299.00, '2026-05-02 10:40:00', '2026-05-12 08:40:00', '已进入原路退款流程', '2026-05-02 10:40:00', '2026-05-12 08:55:00' FROM demo_order o WHERE o.order_no='OPS202605120004'
UNION ALL SELECT 'OPSASR202605120003', o.id, 'RETURN', '支架连接处松动，已同意退货', 'WAIT_BUYER_SEND', 99.00, '2026-05-10 14:30:00', '2026-05-11 18:30:00', '等待用户寄回商品', '2026-05-10 14:30:00', '2026-05-11 18:40:00' FROM demo_order o WHERE o.order_no='OPS202605120007'
UNION ALL SELECT 'OPSASR202605120004', o.id, 'EXCHANGE', '配件包少发滤芯，优先安排换货', 'APPROVED', 89.90, '2026-05-11 09:08:00', '2026-05-12 09:40:00', '已通过审核，仓库准备补发', '2026-05-11 09:08:00', '2026-05-12 09:45:00' FROM demo_order o WHERE o.order_no='OPS202605120010'
UNION ALL SELECT 'OPSASR202605120005', o.id, 'RETURN', '学习平板已激活且缺少包装，退货被拒绝', 'REJECTED', 799.00, '2026-05-03 16:12:00', '2026-05-10 12:30:00', '已说明平台规则并建议走维修检测', '2026-05-03 16:12:00', '2026-05-10 12:36:00' FROM demo_order o WHERE o.order_no='OPS202605120012'
UNION ALL SELECT 'OPSASR202605120006', o.id, 'REFUND', '牙刷充电异常，已退回并退款', 'FINISHED', 169.00, '2026-04-19 11:10:00', '2026-05-05 18:20:00', '用户评价满意', '2026-04-19 11:10:00', '2026-05-05 18:26:00' FROM demo_order o WHERE o.order_no='OPS202605120015';

INSERT INTO after_sale_application(application_no, order_id, user_id, service_type, reason_code, reason_text, status, refund_amount, approved_amount, priority, sla_deadline, assigned_to, ai_summary, risk_level, created_at, updated_at, closed_at)
SELECT 'OPSASA202605120001', o.id, o.user_id, 'EXCHANGE', 'MISSING_PARTS', '耳机右耳偶发断连，用户希望优先换新，不接受长时间维修。', 'UNDER_REVIEW', 329.00, NULL, 'HIGH', '2026-05-12 20:00:00', a.id, '用户描述右耳断连且刚签收，建议核验证据后走换货，优先级高。', 'MEDIUM', '2026-05-11 20:12:00', '2026-05-12 11:12:00', NULL FROM demo_order o JOIN user_account a ON a.username='ops_admin_chen' WHERE o.order_no='OPS202605120002'
UNION ALL SELECT 'OPSASA202605120002', o.id, o.user_id, 'RETURN', 'QUALITY_PROBLEM', '空气炸锅内胆掉漆，用户上传照片后申请退货退款。', 'COMPLETED', 268.80, 268.80, 'NORMAL', '2026-05-08 18:00:00', a.id, '质量问题证据充分，已完成退货退款闭环。', 'LOW', '2026-04-21 10:20:00', '2026-05-09 16:50:00', '2026-05-09 16:50:00' FROM demo_order o JOIN user_account a ON a.username='ops_admin_lina' WHERE o.order_no='OPS202605120003'
UNION ALL SELECT 'OPSASA202605120003', o.id, o.user_id, 'REFUND', 'QUALITY_PROBLEM', '机械键盘空格键连击，客户申请仅退款并愿意补充检测视频。', 'REFUNDING', 299.00, 299.00, 'URGENT', '2026-05-12 18:00:00', a.id, '金额中等但用户多次催促，已审核通过并进入退款。', 'HIGH', '2026-05-02 10:40:00', '2026-05-12 08:55:00', NULL FROM demo_order o JOIN user_account a ON a.username='ops_admin_lina' WHERE o.order_no='OPS202605120004'
UNION ALL SELECT 'OPSASA202605120004', o.id, o.user_id, 'COMPLAINT', 'LOGISTICS_ABNORMAL', '手表签收后用户反馈表带磨损，同时对前次沟通等待时间不满意。', 'NEED_MORE_EVIDENCE', 459.00, NULL, 'HIGH', '2026-05-11 18:00:00', a.id, '包含质量与服务体验双重诉求，需用户补充表带近照和包装照。', 'HIGH', '2026-05-08 16:18:00', '2026-05-12 12:18:00', NULL FROM demo_order o JOIN user_account a ON a.username='ops_admin_chen' WHERE o.order_no='OPS202605120006'
UNION ALL SELECT 'OPSASA202605120005', o.id, o.user_id, 'RETURN', 'QUALITY_PROBLEM', '笔记本支架连接处松动，客服审核通过后等待买家寄回。', 'WAIT_BUYER_SEND', 99.00, 99.00, 'NORMAL', '2026-05-13 10:00:00', a.id, '证据清晰，已提示用户填写退货物流单号。', 'LOW', '2026-05-10 14:30:00', '2026-05-11 18:40:00', NULL FROM demo_order o JOIN user_account a ON a.username='ops_admin_chen' WHERE o.order_no='OPS202605120007'
UNION ALL SELECT 'OPSASA202605120006', o.id, o.user_id, 'EXCHANGE', 'MISSING_PARTS', '洗地机配件包少发滤芯，用户要求补发或换新。', 'EXCHANGING', 89.90, 89.90, 'NORMAL', '2026-05-13 12:00:00', a.id, '少件诉求明确，仓库已创建补发任务，进入换货处理中。', 'LOW', '2026-05-11 09:08:00', '2026-05-12 09:45:00', NULL FROM demo_order o JOIN user_account a ON a.username='ops_admin_chen' WHERE o.order_no='OPS202605120010'
UNION ALL SELECT 'OPSASA202605120007', o.id, o.user_id, 'RETURN', 'NOT_SUPPORT', '学习平板已激活且外包装不完整，用户要求七天无理由退货。', 'REJECTED', 799.00, 0.00, 'NORMAL', '2026-05-06 16:00:00', a.id, '商品已激活且包装缺失，规则不支持无理由退货，建议维修检测。', 'MEDIUM', '2026-05-03 16:12:00', '2026-05-10 12:36:00', '2026-05-10 12:36:00' FROM demo_order o JOIN user_account a ON a.username='ops_admin_lina' WHERE o.order_no='OPS202605120012'
UNION ALL SELECT 'OPSASA202605120008', o.id, o.user_id, 'REFUND', 'QUALITY_PROBLEM', '电动牙刷无法充电，退回检测后确认故障。', 'COMPLETED', 169.00, 169.00, 'LOW', '2026-05-04 18:00:00', a.id, '检测通过，退款完成，用户评价较好。', 'LOW', '2026-04-19 11:10:00', '2026-05-05 18:26:00', '2026-05-05 18:26:00' FROM demo_order o JOIN user_account a ON a.username='ops_admin_chen' WHERE o.order_no='OPS202605120015'
UNION ALL SELECT 'OPSASA202605120009', o.id, o.user_id, 'COMPLAINT', 'SERVICE_DELAY', '电饭煲售后咨询等待时间较长，用户要求人工介入并给出明确处理时限。', 'UNDER_REVIEW', 259.00, NULL, 'URGENT', '2026-05-12 17:30:00', a.id, '投诉类工单且临近 SLA，建议主管优先跟进。', 'HIGH', '2026-05-12 09:50:00', '2026-05-12 15:20:00', NULL FROM demo_order o JOIN user_account a ON a.username='ops_admin_lina' WHERE o.order_no='OPS202605120018'
UNION ALL SELECT 'OPSASA202605120010', o.id, o.user_id, 'RETURN', 'QUALITY_PROBLEM', '投影仪亮度异常，用户已寄回，等待仓库确认收货。', 'WAIT_SELLER_RECEIVE', 699.00, 699.00, 'HIGH', '2026-05-12 23:30:00', a.id, '大额退货已寄回，需仓库尽快确认收货避免超时。', 'MEDIUM', '2026-05-08 18:40:00', '2026-05-12 16:10:00', NULL FROM demo_order o JOIN user_account a ON a.username='ops_admin_chen' WHERE o.order_no='OPS202605120020'
UNION ALL SELECT 'OPSASA202605120011', o.id, o.user_id, 'EXCHANGE', 'QUALITY_PROBLEM', '蓝牙音箱低音破音，用户申请换新，证据已通过初审。', 'APPROVED', 199.00, 199.00, 'NORMAL', '2026-05-13 18:00:00', a.id, '故障视频可见破音，审核通过，待系统生成换货指引。', 'LOW', '2026-05-11 17:30:00', '2026-05-12 17:05:00', NULL FROM demo_order o JOIN user_account a ON a.username='ops_admin_chen' WHERE o.order_no='OPS202605120022'
UNION ALL SELECT 'OPSASA202605120012', o.id, o.user_id, 'REFUND', 'CUSTOMER_CANCEL', '滤芯订单签收后用户误以为型号不匹配，后确认可用并取消申请。', 'CANCELLED', 188.00, NULL, 'LOW', '2026-05-13 14:00:00', NULL, '用户自行取消，无需继续处理。', 'LOW', '2026-05-12 11:20:00', '2026-05-12 14:50:00', '2026-05-12 14:50:00' FROM demo_order o WHERE o.order_no='OPS202605120024';

INSERT INTO chat_session(session_no, user_id, order_id, title, channel, status, current_intent, summary, created_at, updated_at, closed_at)
SELECT 'OPSCS202605120001', o.user_id, o.id, '图片凭证风险预审-耳机断连换货', 'WEB', 'ACTIVE', 'EXCHANGE_APPLY', '主线演示会话：耳机断连、AI 图片风险预审、C2PA 信号、补证建议和工单联动。', '2026-05-11 20:10:00', '2026-05-18 09:30:00', NULL FROM demo_order o WHERE o.order_no='OPS202605120002'
UNION ALL SELECT 'OPSCS202605120002', o.user_id, o.id, '退款多久到账-多轮规则命中', 'WEB', 'CLOSED', 'REFUND_PROGRESS', '用户追问退货退款进度，系统承接订单语境并命中退款到账知识库。', '2026-05-09 15:50:00', '2026-05-18 09:20:00', '2026-05-09 16:40:00' FROM demo_order o WHERE o.order_no='OPS202605120003'
UNION ALL SELECT 'OPSCS202605120003', o.user_id, o.id, '退款催促-高优先级工单', 'WEB', 'ACTIVE', 'REFUND_PROGRESS', '用户催促键盘退款，AI 总结为高优先级退款进度跟进并生成工单。', '2026-05-12 08:30:00', '2026-05-18 09:10:00', NULL FROM demo_order o WHERE o.order_no='OPS202605120004'
UNION ALL SELECT 'OPSCS202605120004', o.user_id, o.id, '投诉转人工-SLA 跟进', 'WEB', 'ACTIVE', 'COMPLAINT_TRANSFER', '用户对沟通等待时间不满意，工单转人工并进入 SLA 高优先级跟进。', '2026-05-12 11:40:00', '2026-05-18 09:00:00', NULL FROM demo_order o WHERE o.order_no='OPS202605120006'
UNION ALL SELECT 'OPSCS202605120005', o.user_id, o.id, '物流 48 小时未更新-人工核实', 'WEB', 'ACTIVE', 'LOGISTICS_QUERY', '包裹超过 48 小时未更新，系统提示客服核实物流并记录异常。', '2026-05-12 16:00:00', '2026-05-18 08:50:00', NULL FROM demo_order o WHERE o.order_no='OPS202605120011'
UNION ALL SELECT 'OPSCS202605120006', o.user_id, o.id, '激活商品退货规则-本地兜底', 'WEB', 'CLOSED', 'RULE_EXPLAIN', '用户咨询激活商品退货规则，客服解释规则并给出维修建议，本地规则可兜底。', '2026-05-10 10:20:00', '2026-05-18 08:40:00', '2026-05-10 12:30:00' FROM demo_order o WHERE o.order_no='OPS202605120012';

INSERT INTO chat_message(session_id, role, content, message_type, file_url, original_filename, content_type, file_size, seq_no, reply_to_id, intent_code, source_type, created_at)
SELECT s.id, 'USER', '这个耳机右耳总是断连，可以直接换新吗？', 'TEXT', NULL, NULL, NULL, NULL, 1, NULL, 'EXCHANGE_APPLY', NULL, '2026-05-11 20:12:00' FROM chat_session s WHERE s.session_no='OPSCS202605120001'
UNION ALL SELECT s.id, 'ASSISTANT', '可以先为您按换货申请处理。请上传右耳断连的视频或检测截图，客服会优先核验。', 'TEXT', NULL, NULL, NULL, NULL, 2, NULL, 'EXCHANGE_APPLY', 'AI_ENHANCED', '2026-05-11 20:12:08' FROM chat_session s WHERE s.session_no='OPSCS202605120001'
UNION ALL SELECT s.id, 'USER', '我上传一张截图，右下角能看到“豆包AI生成”水印，请帮我判断能不能作为售后凭证。', 'IMAGE', '/uploads/demo/ai-risk-evidence.png', '豆包AI生成-售后凭证演示.png', 'image/png', 128432, 3, NULL, 'EXCHANGE_APPLY', NULL, '2026-05-11 20:13:00' FROM chat_session s WHERE s.session_no='OPSCS202605120001'
UNION ALL SELECT s.id, 'ASSISTANT', '这张图片存在 AI 生成水印和来源不清风险，只能作为沟通材料，建议补充原始实拍照片、故障视频和序列号照片后由客服人工复核。', 'TEXT', NULL, NULL, NULL, NULL, 4, NULL, 'EXCHANGE_APPLY', 'AI_ENHANCED', '2026-05-11 20:13:12' FROM chat_session s WHERE s.session_no='OPSCS202605120001'
UNION ALL SELECT s.id, 'USER', '我明天要出差，能不能快一点处理？', 'TEXT', NULL, NULL, NULL, NULL, 5, NULL, 'COMPLAINT_TRANSFER', NULL, '2026-05-12 11:03:00' FROM chat_session s WHERE s.session_no='OPSCS202605120001'
UNION ALL SELECT s.id, 'ASSISTANT', '已为您标记高优先级，并生成待审核记录，客服会在今天内给出换货结果。', 'TEXT', NULL, NULL, NULL, NULL, 6, NULL, 'COMPLAINT_TRANSFER', 'AI_ENHANCED', '2026-05-12 11:03:10' FROM chat_session s WHERE s.session_no='OPSCS202605120001'
UNION ALL SELECT s.id, 'USER', '空气炸锅退货已经寄回了，退款什么时候到账？', 'TEXT', NULL, NULL, NULL, NULL, 1, NULL, 'REFUND_PROGRESS', NULL, '2026-05-09 15:58:00' FROM chat_session s WHERE s.session_no='OPSCS202605120002'
UNION ALL SELECT s.id, 'ASSISTANT', '仓库已经确认收货，退款会按原支付渠道退回，通常 1-3 个工作日到账。', 'TEXT', NULL, NULL, NULL, NULL, 2, NULL, 'REFUND_PROGRESS', 'AI_ENHANCED', '2026-05-09 15:58:08' FROM chat_session s WHERE s.session_no='OPSCS202605120002'
UNION ALL SELECT s.id, 'USER', '我刚看到已经退回了，谢谢。', 'TEXT', NULL, NULL, NULL, NULL, 3, NULL, 'REFUND_PROGRESS', NULL, '2026-05-09 16:38:00' FROM chat_session s WHERE s.session_no='OPSCS202605120002'
UNION ALL SELECT s.id, 'ASSISTANT', '不客气，售后单已完成，如后续还有问题可以继续联系我们。', 'TEXT', NULL, NULL, NULL, NULL, 4, NULL, 'REFUND_PROGRESS', 'RULE_TEMPLATE', '2026-05-09 16:38:07' FROM chat_session s WHERE s.session_no='OPSCS202605120002'
UNION ALL SELECT s.id, 'USER', '键盘退款为什么还没到账？', 'TEXT', NULL, NULL, NULL, NULL, 1, NULL, 'REFUND_PROGRESS', NULL, '2026-05-12 08:32:00' FROM chat_session s WHERE s.session_no='OPSCS202605120003'
UNION ALL SELECT s.id, 'ASSISTANT', '您的退款申请已审核通过，目前处于原路退款处理中，我会同步给人工客服跟进。', 'TEXT', NULL, NULL, NULL, NULL, 2, NULL, 'REFUND_PROGRESS', 'AI_ENHANCED', '2026-05-12 08:32:09' FROM chat_session s WHERE s.session_no='OPSCS202605120003'
UNION ALL SELECT s.id, 'USER', '今天必须给我结果。', 'TEXT', NULL, NULL, NULL, NULL, 3, NULL, 'COMPLAINT_TRANSFER', NULL, '2026-05-12 08:48:00' FROM chat_session s WHERE s.session_no='OPSCS202605120003'
UNION ALL SELECT s.id, 'ASSISTANT', '已升级为紧急退款工单，客服主管会优先核对退款通道状态。', 'TEXT', NULL, NULL, NULL, NULL, 4, NULL, 'COMPLAINT_TRANSFER', 'AI_ENHANCED', '2026-05-12 08:48:10' FROM chat_session s WHERE s.session_no='OPSCS202605120003'
UNION ALL SELECT s.id, 'USER', '手表表带有磨损，而且之前客服一直没人回。', 'TEXT', NULL, NULL, NULL, NULL, 1, NULL, 'COMPLAINT_TRANSFER', NULL, '2026-05-12 11:45:00' FROM chat_session s WHERE s.session_no='OPSCS202605120004'
UNION ALL SELECT s.id, 'ASSISTANT', '抱歉给您带来不好的体验。请补充表带近照和包装照，我会同步创建人工跟进。', 'TEXT', NULL, NULL, NULL, NULL, 2, NULL, 'COMPLAINT_TRANSFER', 'AI_ENHANCED', '2026-05-12 11:45:12' FROM chat_session s WHERE s.session_no='OPSCS202605120004'
UNION ALL SELECT s.id, 'USER', '物流三天不动了，显示器会不会丢件？', 'TEXT', NULL, NULL, NULL, NULL, 1, NULL, 'LOGISTICS_QUERY', NULL, '2026-05-12 16:02:00' FROM chat_session s WHERE s.session_no='OPSCS202605120005'
UNION ALL SELECT s.id, 'ASSISTANT', '物流超过 48 小时未更新，建议先由客服联系承运商核实，如疑似丢件可申请平台介入。', 'TEXT', NULL, NULL, NULL, NULL, 2, NULL, 'LOGISTICS_QUERY', 'AI_ENHANCED', '2026-05-12 16:02:08' FROM chat_session s WHERE s.session_no='OPSCS202605120005'
UNION ALL SELECT s.id, 'USER', '平板已经激活还能七天无理由退吗？', 'TEXT', NULL, NULL, NULL, NULL, 1, NULL, 'RULE_EXPLAIN', NULL, '2026-05-10 10:22:00' FROM chat_session s WHERE s.session_no='OPSCS202605120006'
UNION ALL SELECT s.id, 'ASSISTANT', '已激活且影响二次销售的商品通常不支持无理由退货，但如果存在质量问题可以走检测维修。当前模型不可用时，这类规则解释会走本地模板兜底。', 'TEXT', NULL, NULL, NULL, NULL, 2, NULL, 'RULE_EXPLAIN', 'RULE_TEMPLATE', '2026-05-10 10:22:10' FROM chat_session s WHERE s.session_no='OPSCS202605120006'
UNION ALL SELECT s.id, 'USER', '那如果确实有质量问题，我需要准备什么材料？', 'TEXT', NULL, NULL, NULL, NULL, 3, NULL, 'EXCHANGE_APPLY', NULL, '2026-05-10 10:25:00' FROM chat_session s WHERE s.session_no='OPSCS202605120006'
UNION ALL SELECT s.id, 'ASSISTANT', '可以补充故障照片、连续故障视频、商品序列号和包装配件照片，客服会按质量问题流程重新复核。', 'TEXT', NULL, NULL, NULL, NULL, 4, NULL, 'EXCHANGE_APPLY', 'RULE_TEMPLATE', '2026-05-10 10:25:10' FROM chat_session s WHERE s.session_no='OPSCS202605120006';

INSERT INTO intent_record(session_id, message_id, intent_code, intent_name, confidence, method, slots_json, created_at)
SELECT s.id, m.id, m.intent_code,
       CASE m.intent_code
         WHEN 'RETURN_APPLY' THEN '退货申请'
         WHEN 'EXCHANGE_APPLY' THEN '换货申请'
         WHEN 'REFUND_PROGRESS' THEN '退款进度'
         WHEN 'LOGISTICS_QUERY' THEN '物流查询'
         WHEN 'RULE_EXPLAIN' THEN '规则解释'
         ELSE '投诉与人工转接'
       END,
       0.9100, 'HYBRID', JSON_OBJECT('orderNo', o.order_no, 'source', 'demo-usage-data'), m.created_at
from chat_message m
join chat_session s on m.session_id=s.id
left join demo_order o on s.order_id=o.id
where s.session_no like 'OPSCS%'
  and m.role='USER';

INSERT INTO retrieval_log(session_id, message_id, query_text, doc_id, rank_no, score, hit_reason, doc_title_snapshot, doc_content_snapshot, created_at)
SELECT s.id, m.id, m.content, d.id, 1, 0.9200, '意图匹配 + 关键词命中',
       d.title, LEFT(d.content, 300), DATE_ADD(m.created_at, INTERVAL 1 SECOND)
from chat_message m
join chat_session s on m.session_id=s.id
left join knowledge_doc d on d.intent_code=m.intent_code and d.deleted=0
where s.session_no like 'OPSCS%'
  and m.role='ASSISTANT'
  and d.id = (
    select min(d2.id)
    from knowledge_doc d2
    where d2.intent_code=m.intent_code and d2.deleted=0
  );

INSERT INTO ai_call_log(session_id, message_id, provider, model_name, request_summary, response_summary, status, prompt_tokens, completion_tokens, latency_ms, error_message, created_at)
SELECT s.id, m.id, 'openai-compatible', 'gpt-4o-mini',
       CONCAT('识别意图并结合订单 ', coalesce(o.order_no, '无订单'), ' 生成客服回复'),
       LEFT(m.content, 300), 'SUCCESS', 560 + m.seq_no * 9, 120 + m.seq_no * 5, 820 + m.seq_no * 33, NULL,
       DATE_ADD(m.created_at, INTERVAL 2 SECOND)
from chat_message m
join chat_session s on m.session_id=s.id
left join demo_order o on s.order_id=o.id
where s.session_no like 'OPSCS%'
  and m.role='ASSISTANT'
  and m.source_type='AI_ENHANCED';

INSERT INTO ai_call_log(session_id, message_id, provider, model_name, request_summary, response_summary, status, prompt_tokens, completion_tokens, latency_ms, error_message, created_at)
SELECT s.id, m.id, 'local-fallback', 'rule-template',
       '演示样例：模型未启用时仍按本地规则模板回复退款进度',
       LEFT(m.content, 300), 'SKIPPED', 0, 0, 18, '模型链路跳过，本地规则兜底生成稳定回复。',
       DATE_ADD(m.created_at, INTERVAL 2 SECOND)
from chat_message m
join chat_session s on m.session_id=s.id
where s.session_no='OPSCS202605120002'
  and m.seq_no=4
UNION ALL SELECT s.id, m.id, 'openai-compatible', 'gpt-4o-mini',
       '演示样例：投诉场景模型网关超时，系统记录失败并转人工工单',
       LEFT(m.content, 300), 'FAILED', 620, 0, 3000, '模型网关超时，已切换为本地兜底话术并生成工单。',
       DATE_ADD(m.created_at, INTERVAL 2 SECOND)
from chat_message m
join chat_session s on m.session_id=s.id
where s.session_no='OPSCS202605120004'
  and m.seq_no=2;

INSERT INTO process_trace(session_id, message_id, step_name, step_status, detail_json, created_at)
SELECT s.id, m.id, 'INTENT_RECOGNIZE', 'SUCCESS', JSON_OBJECT('intentCode', m.intent_code, 'confidence', 0.91), DATE_ADD(m.created_at, INTERVAL 1 SECOND)
from chat_message m join chat_session s on m.session_id=s.id
where s.session_no like 'OPSCS%' and m.role='ASSISTANT'
UNION ALL
SELECT s.id, m.id, 'KNOWLEDGE_RETRIEVAL', 'SUCCESS', JSON_OBJECT('topK', 3, 'hit', true), DATE_ADD(m.created_at, INTERVAL 2 SECOND)
from chat_message m join chat_session s on m.session_id=s.id
where s.session_no like 'OPSCS%' and m.role='ASSISTANT'
UNION ALL
SELECT s.id, m.id, 'REPLY_GENERATE', 'SUCCESS', JSON_OBJECT('sourceType', coalesce(m.source_type, 'RULE_TEMPLATE')), DATE_ADD(m.created_at, INTERVAL 3 SECOND)
from chat_message m join chat_session s on m.session_id=s.id
where s.session_no like 'OPSCS%' and m.role='ASSISTANT'
UNION ALL
SELECT s.id, m.id, 'BUSINESS_TOOL_CALLS', 'SUCCESS', JSON_OBJECT('orderBound', s.order_id is not null), DATE_ADD(m.created_at, INTERVAL 4 SECOND)
from chat_message m join chat_session s on m.session_id=s.id
where s.session_no like 'OPSCS%' and m.role='ASSISTANT';

INSERT INTO process_trace(session_id, message_id, step_name, step_status, detail_json, created_at)
SELECT s.id, m.id, 'CHAT_IMAGE_RISK_SCAN', 'SUCCESS',
       JSON_OBJECT(
         'title', '聊天图片风险预审',
         'summary', '聊天图片存在明显 AI 生成水印和来源不清风险，建议要求用户补充原始实拍照片、故障视频和序列号照片。',
         'auditStatus', 'RISKY',
         'sufficiencyLevel', 'PARTIAL',
         'authenticityRisk', 'HIGH',
         'aiGeneratedRisk', 'HIGH',
         'tamperRisk', 'MEDIUM',
         'c2paStatus', 'NOT_FOUND',
         'c2paProvider', NULL,
         'c2paGenerator', NULL,
         'visionStatus', 'SUCCESS',
         'visionModel', 'gpt-4o-mini',
         'imageRisk', JSON_OBJECT(
           'auditStatus', 'RISKY',
           'sufficiencyLevel', 'PARTIAL',
           'authenticityRisk', 'HIGH',
           'aiGeneratedRisk', 'HIGH',
           'tamperRisk', 'MEDIUM',
           'metadataSignal', 'C2PA 内容凭证检测：未发现可用内容凭证，不能据此证明图片来源；图片文件名和说明包含 AI 生成来源信号。',
           'visualSignal', '视觉模型预审：画面右下角出现“豆包AI生成”水印，图片只能作为沟通材料，不能直接作为最终售后凭证。',
           'watermarkSignal', '聊天图片可见“豆包AI生成”水印，按疑似 AIGC 来源处理。',
           'c2paStatus', 'NOT_FOUND',
           'c2paProvider', NULL,
           'c2paGenerator', NULL,
           'c2paSignal', '未发现 C2PA 内容凭证；缺失凭证不能证明图片真实或虚假，只能作为来源缺失信号。',
           'visionStatus', 'SUCCESS',
           'visionModel', 'gpt-4o-mini',
           'visionSignal', '识别到 AI 平台水印和合成图风格，建议要求原始实拍材料。',
           'requiredEvidence', '补充原始实拍照片、故障连续视频、商品序列号照片和包装配件照片；必要时人工复核。',
           'requiredEvidenceList', JSON_ARRAY('补充原始实拍照片', '补充故障连续视频', '补充商品序列号照片', '补充包装配件照片'),
           'summary', '聊天图片存在明显 AI 生成水印和来源不清风险，建议要求用户补充原始实拍材料并人工复核。'
         )
       ),
       DATE_ADD(m.created_at, INTERVAL 1 SECOND)
from chat_message m
join chat_session s on m.session_id=s.id
where s.session_no='OPSCS202605120001'
  and m.message_type='IMAGE'
  and m.file_url='/uploads/demo/ai-risk-evidence.png';

INSERT INTO service_ticket(ticket_no, session_id, message_id, order_id, user_id, intent_code, priority, status, customer_issue, ai_summary, suggested_action, assigned_to, created_at, updated_at, resolved_at, deleted)
SELECT 'OPSTK202605120001', s.id, NULL, s.order_id, s.user_id, 'EXCHANGE_APPLY', 'HIGH', 'PROCESSING', '耳机右耳断连，客户要求今天内给出换货结果。', 'AI 判断为高优先级换货诉求，证据待核验。', '核对视频凭证，若故障成立则创建换新单。', '陈磊（客服专员）', '2026-05-12 11:05:00', '2026-05-12 11:12:00', NULL, 0 FROM chat_session s WHERE s.session_no='OPSCS202605120001'
UNION ALL SELECT 'OPSTK202605120002', s.id, NULL, s.order_id, s.user_id, 'REFUND_PROGRESS', 'NORMAL', 'RESOLVED', '空气炸锅退款进度咨询。', '仓库已确认收货，退款已完成。', '确认用户是否收到退款到账通知。', '李娜（售后主管）', '2026-05-09 16:00:00', '2026-05-09 16:40:00', '2026-05-09 16:40:00', 0 FROM chat_session s WHERE s.session_no='OPSCS202605120002'
UNION ALL SELECT 'OPSTK202605120003', s.id, NULL, s.order_id, s.user_id, 'REFUND_PROGRESS', 'URGENT', 'PROCESSING', '机械键盘退款催促，用户要求当天反馈。', '退款已进入处理链路，用户情绪较急。', '联系支付通道确认退款批次，必要时主管回电安抚。', '李娜（售后主管）', '2026-05-12 08:50:00', '2026-05-12 09:10:00', NULL, 0 FROM chat_session s WHERE s.session_no='OPSCS202605120003'
UNION ALL SELECT 'OPSTK202605120004', s.id, NULL, s.order_id, s.user_id, 'COMPLAINT_TRANSFER', 'HIGH', 'PENDING', '运动手表表带磨损并投诉等待时间过长。', '需补充表带照片，同时处理服务体验投诉。', '要求用户补充图片，客服主管在 SLA 内跟进。', '陈磊（客服专员）', '2026-05-12 12:10:00', '2026-05-12 12:18:00', NULL, 0 FROM chat_session s WHERE s.session_no='OPSCS202605120004'
UNION ALL SELECT 'OPSTK202605120005', s.id, NULL, s.order_id, s.user_id, 'LOGISTICS_QUERY', 'HIGH', 'PENDING', '4K 显示器物流超过 48 小时未更新。', '物流异常，存在延误或丢件风险。', '联系承运商核实轨迹，必要时补发或赔付。', '陈磊（客服专员）', '2026-05-12 16:20:00', '2026-05-12 16:35:00', NULL, 0 FROM chat_session s WHERE s.session_no='OPSCS202605120005'
UNION ALL SELECT 'OPSTK202605120006', s.id, NULL, s.order_id, s.user_id, 'RULE_EXPLAIN', 'NORMAL', 'CLOSED', '学习平板激活后无理由退货争议。', '规则不支持无理由退货，已给出维修检测建议。', '保留拒绝说明，若用户补充质量证据则重新评估。', '李娜（售后主管）', '2026-05-10 10:40:00', '2026-05-10 12:30:00', '2026-05-10 12:30:00', 0 FROM chat_session s WHERE s.session_no='OPSCS202605120006';

UPDATE after_sale_application a
JOIN demo_order o ON a.order_id=o.id
LEFT JOIN service_ticket t ON t.order_id=o.id AND t.ticket_no LIKE 'OPSTK%'
SET a.ticket_id=t.id
WHERE a.application_no LIKE 'OPSASA%'
  AND t.id IS NOT NULL;

INSERT INTO after_sale_process_log(application_id, operator_id, operator_name, operator_role, action, from_status, to_status, remark, created_at)
SELECT a.id, a.user_id, u.display_name, 'CUSTOMER', 'SUBMIT', NULL, 'SUBMITTED', '客户提交售后申请，系统已生成受理记录。', a.created_at
from after_sale_application a join user_account u on a.user_id=u.id
where a.application_no like 'OPSASA%'
UNION ALL SELECT a.id, a.assigned_to, u.display_name, 'ADMIN', 'REQUEST_MORE_EVIDENCE', 'UNDER_REVIEW', 'NEED_MORE_EVIDENCE', '需要补充商品近照、包装照片或物流凭证。', '2026-05-12 12:16:00'
from after_sale_application a join user_account u on a.assigned_to=u.id
where a.application_no='OPSASA202605120004'
UNION ALL SELECT a.id, a.assigned_to, u.display_name, 'ADMIN', 'APPROVE', 'UNDER_REVIEW', a.status, '证据充分，审核通过并给出后续处理指引。', DATE_SUB(a.updated_at, INTERVAL 20 MINUTE)
from after_sale_application a join user_account u on a.assigned_to=u.id
where a.application_no in ('OPSASA202605120002','OPSASA202605120003','OPSASA202605120005','OPSASA202605120006','OPSASA202605120008','OPSASA202605120010','OPSASA202605120011')
UNION ALL SELECT a.id, a.assigned_to, u.display_name, 'ADMIN', 'REJECT', 'UNDER_REVIEW', 'REJECTED', '商品已激活且包装缺失，不满足无理由退货规则。', '2026-05-10 12:30:00'
from after_sale_application a join user_account u on a.assigned_to=u.id
where a.application_no='OPSASA202605120007'
UNION ALL SELECT a.id, a.user_id, u.display_name, 'CUSTOMER', 'CONFIRM', 'WAIT_BUYER_SEND', 'WAIT_SELLER_RECEIVE', '客户补充寄回物流单号，等待商家确认收货。', '2026-05-12 15:25:00'
from after_sale_application a join user_account u on a.user_id=u.id
where a.application_no='OPSASA202605120010'
UNION ALL SELECT a.id, a.assigned_to, u.display_name, 'ADMIN', 'SYSTEM_MARK', 'REFUNDING', 'COMPLETED', '退款完成，售后单关闭。', a.closed_at
from after_sale_application a join user_account u on a.assigned_to=u.id
where a.application_no in ('OPSASA202605120002','OPSASA202605120008')
UNION ALL SELECT a.id, a.user_id, u.display_name, 'CUSTOMER', 'CANCEL', 'SUBMITTED', 'CANCELLED', '客户确认型号可用后自行取消申请。', a.closed_at
from after_sale_application a join user_account u on a.user_id=u.id
where a.application_no='OPSASA202605120012'
UNION ALL SELECT a.id, a.assigned_to, u.display_name, 'ADMIN', 'CREATE_TICKET', a.status, a.status, concat('创建关联客服工单：', t.ticket_no), t.created_at
from after_sale_application a
join service_ticket t on a.ticket_id=t.id
join user_account u on a.assigned_to=u.id
where a.application_no like 'OPSASA%';

INSERT INTO after_sale_evidence(application_id, evidence_type, file_url, content, uploaded_by, created_at)
SELECT a.id, 'IMAGE', '/uploads/demo/ai-risk-evidence.png', '耳机断连凭证截图，图片右下角可见“豆包AI生成”水印，需要补充原始实拍和连续故障视频。', a.user_id, '2026-05-11 20:18:00' from after_sale_application a where a.application_no='OPSASA202605120001'
UNION ALL SELECT a.id, 'IMAGE', '/uploads/demo/air-fryer-scratch.png', '空气炸锅内胆掉漆照片，影响正常使用。', a.user_id, '2026-04-21 10:25:00' from after_sale_application a where a.application_no='OPSASA202605120002'
UNION ALL SELECT a.id, 'VIDEO', NULL, '键盘空格键连续触发的视频凭证，现场演示保留为文字记录，避免把视频路径误渲染成图片。', a.user_id, '2026-05-02 10:48:00' from after_sale_application a where a.application_no='OPSASA202605120003'
UNION ALL SELECT a.id, 'TEXT', NULL, '用户称表带有磨损，客服要求补充近照和包装照。', a.user_id, '2026-05-12 11:50:00' from after_sale_application a where a.application_no='OPSASA202605120004'
UNION ALL SELECT a.id, 'LOGISTICS_NO', NULL, 'YT7382916405', a.user_id, '2026-05-11 18:35:00' from after_sale_application a where a.application_no='OPSASA202605120005'
UNION ALL SELECT a.id, 'IMAGE', '/uploads/demo/missing-filter.png', '配件包实拍，缺少滤芯。', a.user_id, '2026-05-11 09:20:00' from after_sale_application a where a.application_no='OPSASA202605120006'
UNION ALL SELECT a.id, 'TEXT', NULL, '学习平板已激活，包装缺少封签。', a.user_id, '2026-05-03 16:20:00' from after_sale_application a where a.application_no='OPSASA202605120007'
UNION ALL SELECT a.id, 'VIDEO', NULL, '蓝牙音箱低音破音视频，声音失真明显；视频凭证由审核记录说明，不在图片预览区展示。', a.user_id, '2026-05-12 16:45:00' from after_sale_application a where a.application_no='OPSASA202605120011';

INSERT INTO evidence_audit(audit_no, application_id, evidence_id, audit_status, sufficiency_level, authenticity_risk, ai_generated_risk, tamper_risk, metadata_signal, visual_signal, watermark_signal, required_evidence, audit_detail_json, ai_summary, ai_status, ai_error_message, created_at)
SELECT 'OPSEAUD202605120001', a.id, e.id, 'RISKY', 'PARTIAL', 'HIGH', 'HIGH', 'MEDIUM',
       'C2PA 内容凭证检测未发现可用来源凭证；文件名和凭证描述包含 AI 生成来源信号。',
       '凭证能说明用户反馈的问题，但图片右下角存在明显“豆包AI生成”水印，不能直接作为最终审核依据。',
       '可见“豆包AI生成”水印，按疑似 AIGC 来源处理。',
       '补充原始实拍照片；补充连续故障视频；补充商品序列号照片；补充包装配件照片',
       JSON_OBJECT('ruleVersion','demo-evidence-audit-v1','c2paStatus','NOT_FOUND','riskScore',88,'source','demo-usage-data'),
       '系统识别到 AI 生成水印和来源不清风险，建议只作为沟通材料，要求用户补充原始实拍与故障视频后人工复核。',
       'SKIPPED', '演示库预置审核结果，本地规则兜底生成。', '2026-05-12 11:14:00'
from after_sale_application a
join after_sale_evidence e on e.application_id=a.id
where a.application_no='OPSASA202605120001'
  and e.file_url='/uploads/demo/ai-risk-evidence.png'
UNION ALL SELECT 'OPSEAUD202605120002', a.id, e.id, 'MANUAL_REVIEW', 'PARTIAL', 'MEDIUM', 'LOW', 'LOW',
       '未发现明确元数据异常，但缺少近距离实拍角度。',
       '表带磨损诉求与文字说明一致，仍需补充近照和包装照。',
       '未发现明确水印或生成平台来源信号。',
       '补充表带近照；补充包装照片；补充购买后首次发现问题的时间说明',
       JSON_OBJECT('ruleVersion','demo-evidence-audit-v1','riskScore',45,'source','demo-usage-data'),
       '该投诉类凭证建议进入人工复核，客服需结合表带近照、包装和等待时长一起处理。',
       'SKIPPED', '演示库预置审核结果，本地规则兜底生成。', '2026-05-12 12:14:00'
from after_sale_application a
join after_sale_evidence e on e.application_id=a.id
where a.application_no='OPSASA202605120004'
  and e.evidence_type='TEXT'
UNION ALL SELECT 'OPSEAUD202605120003', a.id, e.id, 'PASS', 'SUFFICIENT', 'LOW', 'LOW', 'LOW',
       '凭证描述和文件类型完整，未发现明显元数据异常信号。',
       '视频凭证与低音破音诉求一致，可作为换货审核的初步依据。',
       '未发现明确水印或生成平台来源信号。',
       '当前凭证基本充分，客服仍需结合订单规则复核。',
       JSON_OBJECT('ruleVersion','demo-evidence-audit-v1','riskScore',12,'source','demo-usage-data'),
       '当前凭证较充分，暂未发现明显 AI 生成或篡改风险信号，可进入标准换货审核。',
       'SKIPPED', '演示库预置审核结果，本地规则兜底生成。', '2026-05-12 16:48:00'
from after_sale_application a
join after_sale_evidence e on e.application_id=a.id
where a.application_no='OPSASA202605120011'
  and e.evidence_type='VIDEO';

INSERT INTO after_sale_risk_assessment(assessment_no, application_id, risk_level, risk_score, risk_tags, risk_reasons, suggested_action, rule_detail_json, ai_summary, ai_status, ai_error_message, created_at, updated_at)
SELECT 'OPSRISK202605120001', a.id, 'HIGH', 82, '疑似 AI 凭证，证据不足，SLA 临近，商品集中问题',
       '凭证图片存在 AI 生成水印；用户期望当天处理；同类耳机断连诉求集中出现；距离 SLA 截止较近',
       '建议资深客服人工复核，先要求原始实拍和连续故障视频，不直接退款或驳回。',
       JSON_OBJECT('ruleVersion','after-sale-risk-v1','refundAmount',329.00,'evidenceAuditCount',1,'topProductIssueAlert','OPSPIA202605120001','source','demo-usage-data'),
       '系统评估为高风险：图片来源存疑、证据不足且用户要求快速处理，应由客服人工复核后再推进换货。',
       'SKIPPED', '演示库预置风险评估，本地规则兜底生成。', '2026-05-12 11:16:00', '2026-05-12 11:16:00'
from after_sale_application a where a.application_no='OPSASA202605120001'
UNION ALL SELECT 'OPSRISK202605120002', a.id, 'HIGH', 76, '投诉风险，证据不足，SLA 已超时，低满意度历史',
       '当前为投诉类售后；处于待补材料状态；SLA 截止时间已过；需要主管优先跟进',
       '建议主管先安抚并承诺处理时限，同时要求用户补充表带近照和包装照。',
       JSON_OBJECT('ruleVersion','after-sale-risk-v1','refundAmount',459.00,'status','NEED_MORE_EVIDENCE','source','demo-usage-data'),
       '系统评估为高风险：投诉、补证不足和 SLA 超时叠加，建议主管优先人工处理。',
       'SKIPPED', '演示库预置风险评估，本地规则兜底生成。', '2026-05-12 12:20:00', '2026-05-12 12:20:00'
from after_sale_application a where a.application_no='OPSASA202605120004'
UNION ALL SELECT 'OPSRISK202605120003', a.id, 'MEDIUM', 48, '高金额，SLA 临近，大额退货',
       '投影仪退货金额较高；等待仓库确认收货；需要避免退款确认超时',
       '建议催促仓库扫描退回包裹，确认收货后再触发退款。',
       JSON_OBJECT('ruleVersion','after-sale-risk-v1','refundAmount',699.00,'status','WAIT_SELLER_RECEIVE','source','demo-usage-data'),
       '系统评估为中风险：金额较高且处于仓库确认节点，需要跟进但不应跳过收货确认。',
       'SKIPPED', '演示库预置风险评估，本地规则兜底生成。', '2026-05-12 16:12:00', '2026-05-12 16:12:00'
from after_sale_application a where a.application_no='OPSASA202605120010';

UPDATE after_sale_application a
JOIN after_sale_risk_assessment r ON r.application_id=a.id
SET a.risk_level=r.risk_level
WHERE a.application_no IN ('OPSASA202605120001','OPSASA202605120004','OPSASA202605120010');

INSERT INTO product_issue_alert(alert_no, product_name, issue_keyword, issue_count, application_count, ticket_count, low_rating_count, refund_amount, time_window_days, alert_level, trend_score, sample_application_ids, sample_reasons, suggested_action, status, created_at, updated_at)
SELECT 'OPSPIA202605120001', '无线蓝牙耳机 Pro', '断连', 5, 1, 1, 0, 329.00, 7, 'HIGH', 88,
       CAST(a.id AS CHAR),
       '耳机右耳偶发断连；聊天图片凭证存在“豆包AI生成”水印；客户要求当天处理，需人工复核',
       '近期同款耳机集中出现断连反馈，建议运营检查批次和质检记录，客服侧暂停直接通过可疑凭证，要求补充原始实拍和连续故障视频。',
       'OPEN', '2026-05-12 11:18:00', '2026-05-12 11:18:00'
from after_sale_application a
join demo_order o on a.order_id=o.id
where a.application_no='OPSASA202605120001'
UNION ALL SELECT 'OPSPIA202605120002', '机械键盘', '连击', 3, 1, 1, 0, 299.00, 7, 'MEDIUM', 58,
       CAST(a.id AS CHAR),
       '空格键连续触发；退款催促；建议抽查同批次轴体和售后检测视频',
       '建议客服补充键盘按键检测话术，运营抽查同批次青轴键盘的出库质检记录。',
       'OPEN', '2026-05-12 09:12:00', '2026-05-12 09:12:00'
from after_sale_application a
where a.application_no='OPSASA202605120003'
UNION ALL SELECT 'OPSPIA202605120003', '蓝牙音箱', '破音', 2, 1, 0, 0, 199.00, 7, 'MEDIUM', 46,
       CAST(a.id AS CHAR),
       '低音破音视频较充分；同类音频质量问题需要继续观察',
       '建议运营继续观察音箱低音破音反馈，客服审核通过后同步记录故障频次。',
       'WATCHING', '2026-05-12 17:08:00', '2026-05-12 17:08:00'
from after_sale_application a
where a.application_no='OPSASA202605120011';

INSERT INTO after_sale_process_log(application_id, operator_id, operator_name, operator_role, action, from_status, to_status, remark, created_at)
SELECT a.id, a.assigned_to, u.display_name, 'ADMIN', 'EVIDENCE_AUDIT', a.status, a.status,
       CONCAT('凭证审计 ', ea.audit_no, '：', ea.audit_status, '，AI生成风险 ', ea.ai_generated_risk, '，建议 ', ea.required_evidence),
       ea.created_at
from evidence_audit ea
join after_sale_application a on ea.application_id=a.id
left join user_account u on a.assigned_to=u.id
where ea.audit_no like 'OPSEAUD%'
UNION ALL SELECT a.id, a.assigned_to, u.display_name, 'ADMIN', 'RISK_ASSESSMENT', a.status, a.status,
       CONCAT('售后风险评估 ', r.assessment_no, '：', r.risk_level, ' / ', r.risk_score, ' 分；', r.risk_tags),
       r.created_at
from after_sale_risk_assessment r
join after_sale_application a on r.application_id=a.id
left join user_account u on a.assigned_to=u.id
where r.assessment_no like 'OPSRISK%'
UNION ALL SELECT a.id, a.assigned_to, u.display_name, 'ADMIN', 'PRODUCT_ISSUE_ALERT', a.status, a.status,
       CONCAT('商品质量预警 ', p.alert_no, '：', p.product_name, ' / ', p.issue_keyword, '，等级 ', p.alert_level, '，趋势分 ', p.trend_score),
       p.created_at
from product_issue_alert p
join after_sale_application a on find_in_set(CAST(a.id AS CHAR), p.sample_application_ids)
left join user_account u on a.assigned_to=u.id
where p.alert_no like 'OPSPIA%';

INSERT INTO reply_draft(application_id, ticket_id, draft_content, source_type, status, risk_level, knowledge_refs, ai_status, ai_provider, ai_model_name, audit_remark, created_by, used_at, created_at, updated_at)
SELECT a.id, a.ticket_id, '您好，已收到您关于耳机右耳断连的反馈。请您保留商品与包装，并上传断连视频，我们将在今天内完成审核；若故障属实，将优先为您安排同款换新。', 'AI', 'DRAFT', 'MEDIUM', '换货处理规则;质量问题凭证要求', 'SUCCESS', 'openai-compatible', 'gpt-4o-mini', '待客服核验凭证后发送', a.assigned_to, NULL, '2026-05-12 11:10:00', '2026-05-12 11:10:00'
from after_sale_application a where a.application_no='OPSASA202605120001'
UNION ALL SELECT a.id, a.ticket_id, '您好，退款申请已审核通过，目前正在按原支付渠道退款。我们会持续跟进支付通道状态，预计今天内给您同步最新结果。', 'AI', 'USED', 'HIGH', '退款到账说明;投诉安抚话术', 'SUCCESS', 'openai-compatible', 'gpt-4o-mini', '主管已采纳并用于回复', a.assigned_to, '2026-05-12 09:05:00', '2026-05-12 08:58:00', '2026-05-12 09:05:00'
from after_sale_application a where a.application_no='OPSASA202605120003'
UNION ALL SELECT a.id, a.ticket_id, '您好，关于表带磨损和等待时间的问题我们已升级人工处理。请补充表带近照和包装照，我们会在 SLA 时限内完成复核。', 'TEMPLATE', 'USED', 'HIGH', '投诉转人工规则;补充凭证模板', 'SKIPPED', 'local-fallback', 'local-rule-template', '本地兜底模板已采用', a.assigned_to, '2026-05-12 12:16:00', '2026-05-12 12:12:00', '2026-05-12 12:16:00'
from after_sale_application a where a.application_no='OPSASA202605120004'
UNION ALL SELECT a.id, a.ticket_id, '您好，您寄回的投影仪包裹已经进入仓库确认队列，我们会催促仓库优先扫描，确认后立即为您推进退款。', 'AI', 'DRAFT', 'MEDIUM', '退货寄回确认;大额订单优先处理', 'SUCCESS', 'openai-compatible', 'gpt-4o-mini', '待仓库确认后发送', a.assigned_to, NULL, '2026-05-12 16:00:00', '2026-05-12 16:00:00'
from after_sale_application a where a.application_no='OPSASA202605120010'
UNION ALL SELECT a.id, a.ticket_id, '您好，图片已足以证明耳机存在问题，我们可以直接为您退款。', 'AI', 'DISCARDED', 'HIGH', '换货处理规则;凭证真实性审核', 'SUCCESS', 'openai-compatible', 'gpt-4o-mini', '客服废弃：忽略了 AI 生成水印和补证要求，不能直接发送给客户。', a.assigned_to, NULL, '2026-05-12 11:22:00', '2026-05-12 11:26:00'
from after_sale_application a where a.application_no='OPSASA202605120001';

INSERT INTO after_sale_process_log(application_id, operator_id, operator_name, operator_role, action, from_status, to_status, remark, created_at)
SELECT a.id, d.created_by, u.display_name, 'AI', 'GENERATE_REPLY_DRAFT', a.status, a.status,
       CONCAT('生成回复草稿：', LEFT(d.draft_content, 120)),
       d.created_at
from reply_draft d
join after_sale_application a on d.application_id=a.id
left join user_account u on d.created_by=u.id
where a.application_no like 'OPSASA%'
UNION ALL SELECT a.id, d.created_by, u.display_name, 'ADMIN', 'USE_REPLY_DRAFT', a.status, a.status,
       CONCAT('采纳回复草稿：', coalesce(d.audit_remark, '客服确认后发送')),
       d.used_at
from reply_draft d
join after_sale_application a on d.application_id=a.id
left join user_account u on d.created_by=u.id
where a.application_no like 'OPSASA%'
  and d.status='USED'
UNION ALL SELECT a.id, d.created_by, u.display_name, 'ADMIN', 'DISCARD_REPLY_DRAFT', a.status, a.status,
       CONCAT('废弃回复草稿：', coalesce(d.audit_remark, '客服确认不采用')),
       d.updated_at
from reply_draft d
join after_sale_application a on d.application_id=a.id
left join user_account u on d.created_by=u.id
where a.application_no like 'OPSASA%'
  and d.status='DISCARDED';

INSERT INTO service_review(application_id, user_id, rating, tags, comment, created_at)
SELECT a.id, a.user_id, 5, '响应及时,退款清晰', '客服说明很清楚，退款也按时到账。', '2026-05-09 17:20:00' from after_sale_application a where a.application_no='OPSASA202605120002'
UNION ALL SELECT a.id, a.user_id, 4, '处理规范,还可更快', '整体满意，如果检测反馈能再快一点会更好。', '2026-05-05 19:10:00' from after_sale_application a where a.application_no='OPSASA202605120008'
UNION ALL SELECT a.id, a.user_id, 2, '规则解释不足,体验一般', '虽然理解规则，但希望一开始就说明激活商品限制。', '2026-05-10 13:20:00' from after_sale_application a where a.application_no='OPSASA202605120007';

UPDATE demo_order o
JOIN user_account u ON u.username='demo_customer'
SET o.user_id=u.id
WHERE o.order_no IN (
  'OPS202605120002',
  'OPS202605120003',
  'OPS202605120004',
  'OPS202605120006',
  'OPS202605120011',
  'OPS202605120012'
);

UPDATE after_sale_application a
JOIN demo_order o ON a.order_id=o.id
SET a.user_id=o.user_id
WHERE o.order_no IN (
  'OPS202605120002',
  'OPS202605120003',
  'OPS202605120004',
  'OPS202605120006',
  'OPS202605120011',
  'OPS202605120012'
);

UPDATE after_sale_record r
JOIN demo_order o ON r.order_id=o.id
SET r.updated_at=GREATEST(r.updated_at, '2026-05-18 08:00:00')
WHERE o.order_no IN ('OPS202605120003','OPS202605120004');

UPDATE chat_session s
JOIN demo_order o ON s.order_id=o.id
SET s.user_id=o.user_id
WHERE s.session_no LIKE 'OPSCS20260512000_';

UPDATE chat_session
SET user_id=(SELECT id FROM user_account WHERE username='demo_customer')
WHERE session_no IN (
  'OPSCS202605120001',
  'OPSCS202605120002',
  'OPSCS202605120003',
  'OPSCS202605120004',
  'OPSCS202605120005',
  'OPSCS202605120006'
);

UPDATE service_ticket t
JOIN chat_session s ON t.session_id=s.id
SET t.user_id=s.user_id
WHERE t.ticket_no LIKE 'OPSTK%';

UPDATE after_sale_evidence e
JOIN after_sale_application a ON e.application_id=a.id
SET e.uploaded_by=a.user_id
WHERE a.application_no IN (
  'OPSASA202605120001',
  'OPSASA202605120002',
  'OPSASA202605120003',
  'OPSASA202605120004',
  'OPSASA202605120007'
);

UPDATE service_review r
JOIN after_sale_application a ON r.application_id=a.id
SET r.user_id=a.user_id
WHERE a.application_no IN (
  'OPSASA202605120002',
  'OPSASA202605120007'
);

INSERT INTO service_review(application_id, user_id, rating, tags, comment, created_at)
SELECT a.id, a.user_id, 3, '催促较多,需要更清晰时限', '退款过程能查到进度，但希望客服主动说明预计到账时间。', '2026-05-12 18:20:00'
from after_sale_application a
where a.application_no='OPSASA202605120003'
  and not exists (select 1 from service_review r where r.application_id=a.id);

INSERT INTO knowledge_doc(category_id, title, doc_type, intent_code, scenario, question, answer, content, keywords, priority, status, created_by, updated_by, created_at, updated_at, deleted)
SELECT c.id, '质量问题换货凭证要求', 'POLICY', 'EXCHANGE_APPLY', '用户申请换货但需要核验证据', '商品有质量问题换货需要什么凭证？', '建议上传故障照片、视频、检测截图或物流签收记录，客服会结合订单状态审核。', '当用户因质量问题申请换货时，系统需要提示用户上传能证明故障的照片或视频，客服审核通过后可创建换新或补发流程。', '换货,质量问题,凭证,视频,照片', 96, 'ENABLED', a.id, a.id, '2026-05-01 09:00:00', '2026-05-12 09:00:00', 0
FROM knowledge_category c JOIN user_account a ON a.username='ops_admin_lina'
WHERE c.category_code='EXCHANGE_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='质量问题换货凭证要求' AND d.deleted=0)
UNION ALL
SELECT c.id, '物流超过 48 小时未更新处理 SOP', 'FAQ', 'LOGISTICS_QUERY', '物流长时间无更新', '物流两天不更新怎么办？', '先核实承运商轨迹，超过 48 小时无更新可创建物流异常工单并安抚用户。', '物流超过 48 小时没有新节点时，客服应查询承运商轨迹、记录异常时间、告知用户预计反馈时间，必要时转人工工单。', '物流,48小时,异常,丢件,承运商', 94, 'ENABLED', a.id, a.id, '2026-05-02 09:00:00', '2026-05-12 09:00:00', 0
FROM knowledge_category c JOIN user_account a ON a.username='ops_admin_lina'
WHERE c.category_code='LOGISTICS_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='物流超过 48 小时未更新处理 SOP' AND d.deleted=0)
UNION ALL
SELECT c.id, '退款原路返回与到账时效说明', 'FAQ', 'REFUND_PROGRESS', '用户追问退款到账', '退款审核通过后多久到账？', '退款通常按原支付渠道返回，不同支付方式到账时间不同，客服应说明当前节点和预计时间。', '退款审核通过后，系统应展示当前状态、原路返回渠道、预计到账区间和异常处理入口。若用户多次催促，应创建工单跟进。', '退款,到账,原路返回,催促', 93, 'ENABLED', a.id, a.id, '2026-05-03 09:00:00', '2026-05-12 09:00:00', 0
FROM knowledge_category c JOIN user_account a ON a.username='ops_admin_lina'
WHERE c.category_code='REFUND_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='退款原路返回与到账时效说明' AND d.deleted=0)
UNION ALL
SELECT c.id, '投诉转人工安抚话术', 'SCRIPT', 'COMPLAINT_TRANSFER', '用户情绪激动或要求投诉', '用户要求投诉时怎么回复？', '先表达歉意，再确认问题、承诺处理时限，并创建人工工单。', '用户明确表达投诉、人工介入、等待过久等诉求时，应避免机械解释规则，优先安抚并承诺下一次反馈时间。', '投诉,人工,安抚,SLA,主管', 92, 'ENABLED', a.id, a.id, '2026-05-04 09:00:00', '2026-05-12 09:00:00', 0
FROM knowledge_category c JOIN user_account a ON a.username='ops_admin_lina'
WHERE c.category_code='COMPLAINT_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='投诉转人工安抚话术' AND d.deleted=0);

COMMIT;
