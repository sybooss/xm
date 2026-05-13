# 商品质量问题聚合预警功能计划

## 1. 功能定位

商品质量问题聚合预警是第 5 个功能，目标是把系统从“处理单个售后单”推进到“发现商品集中质量风险”。它不代替售后审核，也不自动判定商品有质量缺陷，而是给管理员和运营人员一个可追踪的预警视图：哪些商品在最近 7 天或 30 天集中出现了退换货、投诉、低分评价或人工工单，问题关键词是什么，应该优先检查什么。

这个功能必须复用当前项目已有数据链路：

| 数据来源 | 使用方式 |
| --- | --- |
| `after_sale_application` | 统计同商品售后申请数、投诉数、退款金额、售后原因文本 |
| `demo_order` | 关联商品名、SKU、订单金额 |
| `service_ticket` | 补充人工工单中的用户原始问题 |
| `service_review` | 统计低分评价和评价标签 |
| `after_sale_risk_assessment` | 给风险识别提供“商品集中问题”信号 |
| `after_sale_process_log` | 记录预警刷新和运营处置留痕 |

## 2. 业务边界

第一版做“运营预警辅助”，不做自动下架、自动拦截退款、自动处罚商家或用户。所有预警只作为客服和运营的参考。

需要体现真实世界可用感：

- 能按时间窗口刷新，例如最近 7 天、30 天。
- 能识别关键词，不只是按商品名计数。
- 能展示样本售后单，管理员可以跳回审核工作台查看证据。
- 能给出运营建议，例如检查批次、补充 FAQ、通知仓库、暂停自动退款。
- 能被风险评估模块读取，售后单如果属于近期集中问题商品，应提高风险/优先级提示。

## 3. 数据模型

新增表 `product_issue_alert`，保存每次刷新后的预警快照。选择落表而不是纯实时查询，是为了让答辩演示时能展示“系统发现、刷新、留痕、复查”的运营闭环，也便于后续接入处理状态。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `alert_no` | 预警编号 |
| `product_name` | 商品名称 |
| `issue_keyword` | 命中的问题关键词 |
| `issue_count` | 关键词命中次数 |
| `application_count` | 售后申请数 |
| `ticket_count` | 关联人工工单数 |
| `low_rating_count` | 低分评价数 |
| `refund_amount` | 统计窗口内申请退款金额合计 |
| `time_window_days` | 统计窗口，例如 7 或 30 |
| `alert_level` | `LOW` / `MEDIUM` / `HIGH` |
| `trend_score` | 0-100，综合热度分 |
| `sample_application_ids` | 样本售后单 ID，逗号分隔 |
| `sample_reasons` | 样本文本摘要 |
| `suggested_action` | 建议动作 |
| `status` | `OPEN` / `WATCHING` / `RESOLVED` |
| `created_at`、`updated_at` | 时间字段 |

幂等策略：

- 同一 `product_name + issue_keyword + time_window_days + status=OPEN` 只保留一条最新开放预警。
- 刷新时若已有开放预警则更新统计值；没有则新增。
- 第一版不做复杂关闭流程，但保留 `status` 字段，方便第 6 个工单工作台后续接入。

## 4. 关键词规则

第一版用本地规则抽取关键词，AI 只作为后续增强，不作为必需路径。关键词来自售后原因、工单问题和评价标签。

| 品类或场景 | 关键词 |
| --- | --- |
| 蓝牙耳机 | 无声、单耳、断连、降噪、破音、续航、充电 |
| 机械键盘 | 连击、失灵、灯光、轴体、蓝牙、按键 |
| 智能手表 | 表带、充电、定位、屏幕、续航、心率 |
| 物流与包装 | 破损、少件、丢件、延迟、外包装、漏发 |
| 通用质量 | 故障、质量问题、无法使用、异响、发热 |

如果没有命中关键词，但售后类型是 `COMPLAINT` 或低分评价较多，则归为“体验投诉”；如果是退款/退货集中但文本很散，则归为“退货集中”。

## 5. 评分与等级

`trend_score` 采用可解释规则，便于答辩说明：

| 信号 | 分值 |
| --- | --- |
| 同商品同关键词每 1 个售后申请 | +12，最多 +48 |
| 同商品同关键词人工工单每 1 个 | +10，最多 +30 |
| 低分评价每 1 个 | +8，最多 +24 |
| 投诉类售后每 1 个 | +10，最多 +30 |
| 申请退款金额超过 500 | +8 |
| 申请退款金额超过 1000 | +15 |
| 7 天窗口内售后数达到 3 | +15 |

等级：

- `HIGH`：`trend_score >= 65`，建议运营立即检查批次或暂停自动化处理。
- `MEDIUM`：`trend_score >= 35`，建议客服重点关注并补充 FAQ。
- `LOW`：低于 35，只做观察记录。

## 6. 后端设计

新增文件：

- `ProductIssueAlert`
- `ProductIssueInsightSearch`
- `ProductIssueInsightSummary`
- `ProductIssueAlertMapper`
- `ProductIssueAlertMapper.xml`
- `ProductIssueInsightService`
- `ProductIssueInsightServiceImpl`
- `ProductIssueInsightController`

新增接口：

```http
GET  /admin/product-issue-insights?days=7&page=1&pageSize=10&alertLevel=HIGH&keyword=耳机
POST /admin/product-issue-insights/refresh
GET  /admin/product-issue-insights/summary?days=7
PATCH /admin/product-issue-insights/{id}/status
```

第一版必须实现前三个接口。`PATCH` 可以先留在计划，不强制实现，避免在本轮引入复杂运营状态流。

`POST /admin/product-issue-insights/refresh` 请求示例：

```json
{
  "days": 7
}
```

返回示例：

```json
{
  "refreshedCount": 4,
  "highCount": 1,
  "mediumCount": 2,
  "topAlert": {
    "productName": "无线蓝牙耳机 Pro",
    "issueKeyword": "断连",
    "issueCount": 5,
    "alertLevel": "HIGH"
  }
}
```

## 7. 聚合查询设计

Mapper 提供两类查询：

1. 从售后、工单、评价中拉取候选明细。
2. 查询已经落表的预警列表和汇总指标。

候选明细字段：

| 字段 | 说明 |
| --- | --- |
| `application_id` | 售后单 ID |
| `application_no` | 售后单号 |
| `product_name` | 商品名 |
| `service_type` | 售后类型 |
| `reason_text` | 售后原因 |
| `customer_issue` | 工单问题 |
| `review_tags`、`review_comment` | 评价内容 |
| `rating` | 评分 |
| `refund_amount` | 退款金额 |
| `created_at` | 售后创建时间 |

Service 层按商品和关键词分组，计算分数、等级、样本 ID 和建议动作后写入 `product_issue_alert`。

## 8. 前端设计

新增后台页面 `ProductIssueInsightView.vue`，路由建议：

```text
/admin/product-issues
```

页面结构：

- 顶部指标：开放预警数、高风险数、涉及商品数、近 7 天样本数。
- 筛选区：时间窗口、预警等级、关键词。
- 主表格：商品名、关键词、等级、趋势分、售后数、工单数、低分评价数、退款金额、建议动作。
- 右侧或展开行：样本售后单 ID、样本原因、更新时间。
- 操作：刷新预警、跳转售后审核工作台并带关键词。

导航入口：

- `DashboardView.vue` 的演示入口增加“商品预警”。
- 如有侧边栏导航，也补充“商品预警”入口。

## 9. 与已完成功能的联动

| 已有功能 | 联动方式 |
| --- | --- |
| 产品智能顾问 | 商品档案可解释“该商品为什么容易出现某些问题” |
| 售后前置诊断 | 诊断页可提示“该商品近期同类问题增多” |
| 凭证真实性审核 | 聚合时不直接使用真假结论，但可在建议中提醒加强凭证要求 |
| 售后风险识别 | 风险评估读取开放预警，命中同商品同关键词则加分并添加“商品集中问题”标签 |
| 人工接管工作台 | 后续客服可从预警样本跳到工单/会话进行人工回复 |

## 10. 实施顺序

1. 写入 `product_issue_alert` 表和兼容性建表脚本。
2. 新增 POJO、Mapper、Service、Controller。
3. 在 Service 中实现关键词抽取、评分、等级和建议动作。
4. 修改售后风险评估，读取开放预警并加入商品集中问题信号。
5. 新增前端 API、路由和 `ProductIssueInsightView.vue`。
6. 修改 `DashboardView.vue` 增加入口。
7. 扩展冒烟脚本和浏览器测试，覆盖刷新接口和页面展示。

## 11. 自审与修订

### 初版风险

1. 如果只实时聚合不落表，演示时缺少“预警被系统发现并持续跟踪”的真实运营感。
2. 如果关键词规则写得过细，容易变成硬编码堆砌；第一版应控制在常见退换货问题。
3. 如果与售后风险识别重复，价值会变弱；本功能应聚焦“商品维度”，风险识别聚焦“单个售后单”。
4. 如果只展示排行榜，用户看不到证据来源；必须展示样本售后单和原因摘要。
5. 如果引入 AI 总结作为必需步骤，会增加不稳定性；第一版保持本地规则闭环。

### 修订结论

计划采用“落表快照 + 本地关键词规则 + 样本证据 + 风险评估联动”的方式。这样既不会把第 5 个功能做成孤立报表，也不会和第 4 个功能重复；同时符合当前 Spring Boot、MyBatis、Vue 3 的项目框架，便于验证和答辩演示。

## 12. 验证标准

后端：

```powershell
mvn -q -DskipTests package
```

前端：

```powershell
npm run build
```

数据库：

```powershell
Get-Content -Path sql\schema.sql -Raw -Encoding UTF8 | mysql -uroot -p1234 --default-character-set=utf8mb4
```

接口冒烟：

- `POST /admin/product-issue-insights/refresh`
- `GET /admin/product-issue-insights?days=7`
- `GET /admin/product-issue-insights/summary?days=7`
- 售后风险评估命中商品预警信号

浏览器：

- `npm run test:browser`
- 后台能进入商品预警页，刷新后看到预警列表和建议动作。
