USE test3;
SET NAMES utf8mb4;

UPDATE user_account
SET display_name='演示用户'
WHERE username='demo_customer';

UPDATE user_account
SET display_name='系统管理员'
WHERE username='admin';

UPDATE knowledge_category SET category_name='退货规则' WHERE category_code='RETURN_RULE';
UPDATE knowledge_category SET category_name='换货规则' WHERE category_code='EXCHANGE_RULE';
UPDATE knowledge_category SET category_name='退款规则' WHERE category_code='REFUND_RULE';
UPDATE knowledge_category SET category_name='物流异常' WHERE category_code='LOGISTICS_RULE';
UPDATE knowledge_category SET category_name='投诉与人工转接' WHERE category_code='COMPLAINT_RULE';
UPDATE knowledge_category SET category_name='通用 FAQ' WHERE category_code='GENERAL_FAQ';

UPDATE knowledge_doc
SET title='七天无理由退货规则',
    scenario='已签收订单退货',
    question='已签收订单还能退货吗？',
    answer='签收 7 天内且商品不影响二次销售，一般可以申请退货。',
    content='用户签收商品后 7 天内，如商品完好、配件齐全且不影响二次销售，可申请七天无理由退货。部分特殊商品以平台规则为准。',
    keywords='退货,七天无理由,签收'
WHERE intent_code='RETURN_APPLY' AND deleted=0 AND id=1;

UPDATE knowledge_doc
SET title='换货申请处理规则',
    scenario='商品质量问题换货',
    question='商品有问题可以换货吗？',
    answer='商品存在质量问题或规格不符时，可以提交换货申请。',
    content='用户收到商品后发现质量问题、错发漏发或规格不符，可以在订单详情页提交换货申请，并上传照片或视频作为凭证。',
    keywords='换货,质量问题,规格不符'
WHERE intent_code='EXCHANGE_APPLY' AND deleted=0 AND id=2;

UPDATE knowledge_doc
SET title='退款到账时间说明',
    scenario='退款进度查询',
    question='退款多久到账？',
    answer='退款审核通过后通常按原支付渠道退回，到账时间以支付渠道为准。',
    content='商家确认收货或售后审核通过后，退款会按原支付渠道退回。银行卡、支付平台和优惠券等渠道到账时间可能不同。',
    keywords='退款,到账,原路退回'
WHERE intent_code='REFUND_PROGRESS' AND deleted=0 AND id=3;

UPDATE knowledge_doc
SET title='物流异常处理说明',
    scenario='物流长时间不更新',
    question='物流一直不动怎么办？',
    answer='可以先查看最新物流节点，长时间无更新时联系商家或申请平台介入。',
    content='若物流超过 48 小时无更新，建议用户先联系商家核实包裹状态；如包裹疑似丢失或异常，可申请平台介入处理。',
    keywords='物流,快递,不更新,异常'
WHERE intent_code='LOGISTICS_QUERY' AND deleted=0 AND id=4;

UPDATE knowledge_doc
SET title='投诉与人工转接规则',
    scenario='商家长时间不处理',
    question='可以转人工或投诉吗？',
    answer='商家长时间不处理或处理结果不合理时，可以申请人工客服或平台介入。',
    content='当用户表达投诉、平台介入、人工客服等诉求时，系统应优先安抚用户，并提示可提交凭证申请人工客服或平台介入。',
    keywords='投诉,人工客服,平台介入'
WHERE intent_code='COMPLAINT_TRANSFER' AND deleted=0 AND id=5;

UPDATE demo_order
SET product_name='无线蓝牙耳机',
    sku_name='白色 标准版'
WHERE order_no='DD202604290001';

UPDATE demo_order
SET product_name='智能手表',
    sku_name='黑色 运动版'
WHERE order_no='DD202604290002';

UPDATE demo_order
SET product_name='机械键盘',
    sku_name='青轴 RGB'
WHERE order_no='DD202604290003';

UPDATE demo_order
SET product_name='移动电源',
    sku_name='20000mAh'
WHERE order_no='DD202604290004';

UPDATE after_sale_application
SET reason_text='耳机左耳无声音，申请退货退款。',
    ai_summary='用户反馈左耳无声，需要人工审核照片或检测凭证。'
WHERE application_no='ASA202605060001';

UPDATE after_sale_process_log l
JOIN after_sale_application a ON l.application_id=a.id
JOIN user_account u ON a.user_id=u.id
SET l.operator_name=u.display_name,
    l.remark='顾客提交退货退款申请。'
WHERE a.application_no='ASA202605060001'
  AND l.action='SUBMIT';

UPDATE after_sale_evidence e
JOIN after_sale_application a ON e.application_id=a.id
SET e.content='左耳无声音，已尝试重新配对仍无法恢复。'
WHERE a.application_no='ASA202605060001'
  AND e.evidence_type='TEXT';
