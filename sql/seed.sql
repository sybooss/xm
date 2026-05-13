USE test3;
SET NAMES utf8mb4;

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

INSERT INTO product_profile(product_name, product_alias, category, positioning, spec_json, selling_points, usage_scenarios, common_issues, troubleshooting_steps, comparison_text, retention_script, after_sale_advice, enabled)
VALUES
('无线蓝牙耳机',
 '蓝牙耳机,无线耳机,降噪耳机,耳机',
 'EARPHONE',
 '面向日常通勤、网课会议和轻运动的入门到中端无线蓝牙耳机',
 JSON_OBJECT('bluetooth','5.3','batteryLife','单次约 6 小时，配合充电盒约 24 小时','noiseControl','支持基础环境降噪和通话降噪','waterResistance','日常防汗','charging','Type-C 充电盒'),
 '连接方便、佩戴轻便、通话清晰，适合日常通勤、在线会议和宿舍学习。',
 '通勤路上、宿舍学习、线上会议、轻运动。',
 '降噪效果受耳塞尺寸、佩戴密封性、环境模式和噪声类型影响；单耳无声可能与配对状态、电量或硬件异常有关；频繁断连可能与蓝牙环境、距离或系统兼容性有关。',
 '更换耳塞尺寸并确认佩戴密封；切换降噪/通透模式；将耳机放回充电盒复位后重新配对；分别测试左右耳和通话麦克风；在另一台手机上测试连接稳定性。',
 '该商品更偏日常使用和性价比，不能等同于专业头戴式强降噪耳机；同价位优势是轻便、连接方便和通话场景覆盖。',
 '如果主要是不熟悉降噪模式，建议先按排查步骤测试；如仍不满意且商品完好，可继续按七天无理由退货规则处理。',
 '体验不满意且不影响二次销售时可引导退货；单耳无声、频繁断连、充不进电时更适合换货或检测。',
 1),
('智能手表',
 '运动手表,智能腕表,手表,定位手表',
 'WATCH',
 '面向运动记录、健康监测和日常消息提醒的入门智能手表',
 JSON_OBJECT('screen','1.8 英寸触控屏','batteryLife','典型使用约 5 天','location','支持 GPS/手机辅助定位','health','支持心率、睡眠和运动记录','waterResistance','生活防水'),
 '运动记录、消息提醒和健康数据查看方便，适合日常运动和通勤佩戴。',
 '跑步记录、日常消息提醒、睡眠监测、学生或上班族运动打卡。',
 '定位偏差常见于室内、楼宇密集区域或未授权定位权限；充电异常可能与触点污渍、充电线或适配器有关；表带不适可能与佩戴松紧和材质敏感有关。',
 '开启定位和后台权限；在室外空旷区域重新测试 GPS；更新手表固件和手机 App；清洁充电触点并更换适配器测试；调整表带松紧。',
 '该商品偏日常运动和健康提醒，不等同于专业户外运动表；优势是上手简单、功能覆盖均衡。',
 '定位问题建议先区分室内环境和权限设置；持续异常时可补充定位截图或测试视频申请检测。',
 '设置排查后仍定位异常、无法充电或屏幕异常时，建议上传图片/视频后走换货检测；单纯不适应佩戴且商品完好可按退货规则处理。',
 1),
('机械键盘',
 '键盘,机械键盘,青轴键盘,RGB键盘',
 'KEYBOARD',
 '面向办公打字和轻度游戏的机械键盘',
 JSON_OBJECT('switch','青轴','layout','全尺寸键位','connection','USB 有线连接','lighting','RGB 背光','keycap','可拆卸键帽'),
 '段落感明显、输入反馈清晰，适合长文本输入和桌面外设升级。',
 '办公输入、宿舍学习、轻度游戏、桌面外设搭配。',
 '按键连击可能来自轴体异常、键帽卡滞、进灰或宏设置；灯光异常可能与驱动配置或 USB 供电有关；部分用户可能不适应青轴声音。',
 '更换 USB 接口或电脑测试；拔下键帽清理轴体周围灰尘；关闭驱动宏和连发设置；录制连击或失灵视频；确认是否进水或外力损伤。',
 '青轴反馈清晰但声音较明显；如果更重视安静办公，同类茶轴或静音轴更适合。',
 '如果只是声音或手感不适应，可说明青轴特性并引导按退货规则判断；如果是连击或失灵，建议补充故障视频后换货检测。',
 '连击、失灵、灯光异常等质量问题优先建议换货或检测；外观完好且不影响二次销售时可按规则申请退货。',
 1),
('移动电源',
 '充电宝,移动电源,充电器,备用电源',
 'POWER_BANK',
 '面向手机和小型数码设备应急补电的 20000mAh 移动电源',
 JSON_OBJECT('capacity','20000mAh 标称容量','input','Type-C 输入','output','双 USB 输出','power','常规快充档位','protection','过流和温控保护'),
 '容量较大、适合外出补电，可同时给两台小型设备应急充电。',
 '通勤、旅行、考试周、短途出行、应急备用电源。',
 '容量体验会受转换损耗、手机电池容量和快充协议影响；发热可能出现在高功率输出或边充边放场景；无法充电可能与线材、接口或保护模式有关。',
 '更换数据线和适配器测试；短按电源键查看剩余电量；避免边充边放；静置 10 分钟后重新连接；测试不同输出口和不同手机。',
 '该商品偏大容量和应急补电，不等同于高功率笔记本充电宝；优势是容量和多设备兼容。',
 '如果用户认为容量不符，应解释转换损耗并建议按完整充放电测试；若无法充电或异常发热，应优先安全停用并申请检测。',
 '无法充电、异常发热、接口松动等问题建议换货或检测；容量体验争议建议补充充放电测试记录后人工复核。',
 1)
ON DUPLICATE KEY UPDATE
  product_alias=VALUES(product_alias),
  category=VALUES(category),
  positioning=VALUES(positioning),
  spec_json=VALUES(spec_json),
  selling_points=VALUES(selling_points),
  usage_scenarios=VALUES(usage_scenarios),
  common_issues=VALUES(common_issues),
  troubleshooting_steps=VALUES(troubleshooting_steps),
  comparison_text=VALUES(comparison_text),
  retention_script=VALUES(retention_script),
  after_sale_advice=VALUES(after_sale_advice),
  enabled=VALUES(enabled);

INSERT INTO after_sale_application(application_no, order_id, user_id, service_type, reason_code, reason_text, status, refund_amount, approved_amount, priority, sla_deadline, assigned_to, ai_summary, risk_level, created_at, updated_at)
SELECT 'ASA202605060001', o.id, o.user_id, 'RETURN', 'QUALITY_PROBLEM', '耳机左耳无声音，申请退货退款。', 'SUBMITTED', 199.00, NULL, 'NORMAL',
       DATE_ADD(NOW(), INTERVAL 48 HOUR), NULL, '用户反馈左耳无声，需要人工审核照片或检测凭证。', 'LOW', NOW(), NOW()
FROM demo_order o
WHERE o.order_no='DD202604290001'
  AND NOT EXISTS (SELECT 1 FROM after_sale_application a WHERE a.application_no='ASA202605060001');

INSERT INTO after_sale_process_log(application_id, operator_id, operator_name, operator_role, action, from_status, to_status, remark)
SELECT a.id, u.id, u.display_name, 'CUSTOMER', 'SUBMIT', NULL, 'SUBMITTED', '顾客提交退货退款申请。'
FROM after_sale_application a
LEFT JOIN user_account u ON a.user_id=u.id
WHERE a.application_no='ASA202605060001'
  AND NOT EXISTS (
      SELECT 1 FROM after_sale_process_log l
      WHERE l.application_id=a.id AND l.action='SUBMIT'
  );

INSERT INTO after_sale_evidence(application_id, evidence_type, content, uploaded_by)
SELECT a.id, 'TEXT', '左耳无声音，已尝试重新配对仍无法恢复。', a.user_id
FROM after_sale_application a
WHERE a.application_no='ASA202605060001'
  AND NOT EXISTS (
      SELECT 1 FROM after_sale_evidence e
      WHERE e.application_id=a.id AND e.content='左耳无声音，已尝试重新配对仍无法恢复。'
  );
