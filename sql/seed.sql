USE test3;

INSERT IGNORE INTO user_account(username, display_name, role, phone, status)
VALUES
('demo_customer', '演示用户', 'CUSTOMER', '13800000000', 1),
('admin', '系统管理员', 'ADMIN', '13900000000', 1);

INSERT IGNORE INTO knowledge_category(category_code, category_name, sort_order, enabled)
VALUES
('RETURN_RULE', '退货规则', 10, 1),
('EXCHANGE_RULE', '换货规则', 20, 1),
('REFUND_RULE', '退款规则', 30, 1),
('LOGISTICS_RULE', '物流异常', 40, 1),
('COMPLAINT_RULE', '投诉与人工转接', 50, 1),
('GENERAL_FAQ', '通用 FAQ', 60, 1);

INSERT INTO knowledge_doc(category_id, title, doc_type, intent_code, scenario, question, answer, content, keywords, priority, status)
SELECT c.id, '七天无理由退货规则', 'POLICY', 'RETURN_APPLY', '已签收订单退货',
       '已签收订单还能退货吗？',
       '签收 7 天内且商品不影响二次销售，一般可以申请退货。',
       '用户签收商品后 7 天内，如商品完好、配件齐全且不影响二次销售，可申请七天无理由退货。部分特殊商品以平台规则为准。',
       '退货,七天无理由,签收', 100, 'ENABLED'
FROM knowledge_category c
WHERE c.category_code='RETURN_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='七天无理由退货规则' AND d.deleted=0);

INSERT INTO knowledge_doc(category_id, title, doc_type, intent_code, scenario, question, answer, content, keywords, priority, status)
SELECT c.id, '换货申请处理规则', 'POLICY', 'EXCHANGE_APPLY', '商品质量问题换货',
       '商品有问题可以换货吗？',
       '商品存在质量问题或规格不符时，可以提交换货申请。',
       '用户收到商品后发现质量问题、错发漏发或规格不符，可以在订单详情页提交换货申请，并上传照片或视频作为凭证。',
       '换货,质量问题,规格不符', 90, 'ENABLED'
FROM knowledge_category c
WHERE c.category_code='EXCHANGE_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='换货申请处理规则' AND d.deleted=0);

INSERT INTO knowledge_doc(category_id, title, doc_type, intent_code, scenario, question, answer, content, keywords, priority, status)
SELECT c.id, '退款到账时间说明', 'FAQ', 'REFUND_PROGRESS', '退款进度查询',
       '退款多久到账？',
       '退款审核通过后通常按原支付渠道退回，到账时间以支付渠道为准。',
       '商家确认收货或售后审核通过后，退款会按原支付渠道退回。银行卡、支付平台和优惠券等渠道到账时间可能不同。',
       '退款,到账,原路退回', 90, 'ENABLED'
FROM knowledge_category c
WHERE c.category_code='REFUND_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='退款到账时间说明' AND d.deleted=0);

INSERT INTO knowledge_doc(category_id, title, doc_type, intent_code, scenario, question, answer, content, keywords, priority, status)
SELECT c.id, '物流异常处理说明', 'FAQ', 'LOGISTICS_QUERY', '物流长时间不更新',
       '物流一直不动怎么办？',
       '可以先查看最新物流节点，长时间无更新时联系商家或申请平台介入。',
       '若物流超过 48 小时无更新，建议用户先联系商家核实包裹状态；如包裹疑似丢失或异常，可申请平台介入处理。',
       '物流,快递,不更新,异常', 85, 'ENABLED'
FROM knowledge_category c
WHERE c.category_code='LOGISTICS_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='物流异常处理说明' AND d.deleted=0);

INSERT INTO knowledge_doc(category_id, title, doc_type, intent_code, scenario, question, answer, content, keywords, priority, status)
SELECT c.id, '投诉与人工转接规则', 'SCRIPT', 'COMPLAINT_TRANSFER', '商家长时间不处理',
       '可以转人工或投诉吗？',
       '商家长时间不处理或处理结果不合理时，可以申请人工客服或平台介入。',
       '当用户表达投诉、平台介入、人工客服等诉求时，系统应优先安抚用户，并提示可提交凭证申请人工客服或平台介入。',
       '投诉,人工客服,平台介入', 80, 'ENABLED'
FROM knowledge_category c
WHERE c.category_code='COMPLAINT_RULE'
  AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.title='投诉与人工转接规则' AND d.deleted=0);

INSERT IGNORE INTO demo_order(order_no, user_id, product_name, sku_name, order_amount, pay_status, order_status, logistics_status, after_sale_status, paid_at, shipped_at, signed_at)
VALUES
('DD202604290001', 1, '无线蓝牙耳机', '白色 标准版', 199.00, 'PAID', 'SIGNED', 'DELIVERED', 'NONE', '2026-04-25 10:00:00', '2026-04-25 18:00:00', '2026-04-26 14:30:00'),
('DD202604290002', 1, '智能手表', '黑色 运动版', 399.00, 'PAID', 'SIGNED', 'DELIVERED', 'NONE', '2026-04-10 10:00:00', '2026-04-10 18:00:00', '2026-04-12 14:30:00'),
('DD202604290003', 1, '机械键盘', '青轴 RGB', 299.00, 'REFUNDING', 'COMPLETED', 'DELIVERED', 'REFUNDING', '2026-04-20 10:00:00', '2026-04-20 18:00:00', '2026-04-22 14:30:00'),
('DD202604290004', 1, '移动电源', '20000mAh', 129.00, 'PAID', 'SHIPPED', 'ABNORMAL', 'NONE', '2026-04-27 10:00:00', '2026-04-27 18:00:00', NULL);
