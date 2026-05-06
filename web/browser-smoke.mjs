import { chromium } from 'playwright'
import fs from 'node:fs/promises'
import path from 'node:path'

const baseUrl = 'http://localhost:5173'
const artifactDir = path.resolve('D:/复制软件系统/output/playwright')
await fs.mkdir(artifactDir, { recursive: true })

const results = []
const consoleErrors = []
const pageErrors = []
let adminToken = ''
let seededCustomerOrderId = null
let seededReviewOrderId = null
let seededReviewApplicationId = null

function record(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok), detail })
}

async function expectText(page, text, name) {
  await page.getByText(text, { exact: false }).first().waitFor({ timeout: 20000 })
  record(name, true, text)
}

async function apiPost(pathname, body, token = adminToken) {
  const response = await fetch(`http://localhost:8081${pathname}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(body)
  })
  const result = await response.json()
  if (result.code !== 1) {
    throw new Error(`${pathname} failed: ${result.msg}`)
  }
  return result.data
}

async function apiGet(pathname, token = adminToken) {
  const response = await fetch(`http://localhost:8081${pathname}`, {
    method: 'GET',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  const result = await response.json()
  if (result.code !== 1) {
    throw new Error(`${pathname} failed: ${result.msg}`)
  }
  return result.data
}

async function apiDelete(pathname, token = adminToken) {
  const response = await fetch(`http://localhost:8081${pathname}`, {
    method: 'DELETE',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  const result = await response.json()
  if (result.code !== 1) {
    throw new Error(`${pathname} failed: ${result.msg}`)
  }
  return result.data
}

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 920 } })

page.on('console', message => {
  if (message.type() === 'error') {
    consoleErrors.push(message.text())
  }
})
page.on('pageerror', error => pageErrors.push(error.message))

try {
  const adminAuth = await apiPost('/auth/login', { username: 'admin', password: '123456' }, '')
  adminToken = adminAuth.token

  await page.goto(baseUrl, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '管理员登录', 'home redirects to login')
  await page.getByRole('button', { name: '注册' }).click()
  const registerUsername = `browser_user_${Date.now()}`
  await page.getByPlaceholder('4-30位字母、数字或下划线').fill(registerUsername)
  await page.getByPlaceholder('用于页面展示，可不填').fill('浏览器注册用户')
  await page.getByPlaceholder('123456').fill('browser123')
  await page.getByPlaceholder('再次输入密码').fill('browser123')
  await page.getByRole('button', { name: '注册并登录' }).click()
  await expectText(page, '我的售后', 'register redirects to customer after-sales')
  await expectText(page, '我的订单', 'customer orders visible')
  await expectText(page, '我的售后', 'customer after-sales visible')
  await expectText(page, '客户', 'registered customer role visible')
  await page.locator('header').getByRole('button', { name: /退出/ }).click()
  await expectText(page, '管理员登录', 'registered customer logout returns login')
  await page.getByRole('button', { name: '客户' }).click()
  await page.locator('.login-form .login-button').click()
  await expectText(page, '我的售后', 'demo customer redirects to customer after-sales')
  const demoCustomer = await page.evaluate(() => JSON.parse(localStorage.getItem('returns_assistant_user') || '{}'))
  const orderNoForUiApply = 'BROWSER' + Date.now()
  await apiPost('/orders', {
    orderNo: orderNoForUiApply,
    userId: demoCustomer.userId,
    productName: '浏览器售后测试商品' + Date.now(),
    skuName: '标准版',
    orderAmount: 168.80,
    payStatus: 'PAID',
    orderStatus: 'SIGNED',
    logisticsStatus: 'DELIVERED',
    afterSaleStatus: 'NONE',
    paidAt: '2026-04-23T10:00:00',
    shippedAt: '2026-04-24T10:00:00',
    signedAt: '2026-04-25T10:00:00'
  })
  const seededOrder = await apiGet(`/orders/no/${orderNoForUiApply}`)
  seededCustomerOrderId = seededOrder?.id
  await page.getByPlaceholder('订单号或商品名').fill(orderNoForUiApply)
  await page.getByRole('button', { name: '查询' }).first().click()
  await expectText(page, orderNoForUiApply, 'customer seeded order visible')
  await page.getByRole('button', { name: '申请' }).nth(1).click()
  await expectText(page, '申请售后', 'customer after-sale apply dialog visible')
  await page.locator('textarea[placeholder*="请说明问题"]').fill('浏览器自动化提交售后申请：商品存在质量问题，需要退货退款。')
  await page.getByRole('button', { name: '提交申请' }).click()
  await expectText(page, '售后申请已提交', 'customer after-sale submit toast')
  await expectText(page, '处理时间线', 'customer after-sale timeline visible')
  await expectText(page, '提交申请', 'customer after-sale submit action visible')
  await page.screenshot({ path: path.join(artifactDir, '00-customer-after-sales.png'), fullPage: true })
  record('customer after-sale submit flow', true, 'demo customer application submitted and timeline displayed')
  await page.locator('header').getByRole('button', { name: /退出/ }).click()
  await expectText(page, '管理员登录', 'registered customer logout returns login')
  await page.locator('.login-form .login-button').click()
  await expectText(page, '答辩展示中心', 'login redirects to showcase')
  await expectText(page, '退换货客服', 'layout brand visible')
  const demoCustomerPage = await apiGet('/orders?page=1&pageSize=1')
  const demoCustomerId = demoCustomerPage.rows?.[0]?.userId || 1
  const reviewOrderNo = 'REVIEW' + Date.now()
  await apiPost('/orders', {
    orderNo: reviewOrderNo,
    userId: demoCustomerId,
    productName: '审核工作台测试商品' + Date.now(),
    skuName: '标准版',
    orderAmount: 288.80,
    payStatus: 'PAID',
    orderStatus: 'SIGNED',
    logisticsStatus: 'DELIVERED',
    afterSaleStatus: 'NONE',
    paidAt: '2026-04-23T10:00:00',
    shippedAt: '2026-04-24T10:00:00',
    signedAt: '2026-04-25T10:00:00'
  })
  const reviewOrder = await apiGet(`/orders/no/${reviewOrderNo}`)
  seededReviewOrderId = reviewOrder?.id
  const demoCustomerAuth = await apiPost('/auth/login', { username: 'demo_customer', password: '123456' }, '')
  await apiPost('/customer/after-sales', {
    orderId: seededReviewOrderId,
    serviceType: 'RETURN',
    reasonCode: 'QUALITY_PROBLEM',
    reasonText: '浏览器自动化为管理员审核台创建待审核售后申请。',
    refundAmount: 188.80
  }, demoCustomerAuth.token)
  const reviewApplicationPage = await apiGet(`/customer/after-sales?page=1&pageSize=1&keyword=${reviewOrderNo}`, demoCustomerAuth.token)
  seededReviewApplicationId = reviewApplicationPage.rows?.[0]?.id
  await page.goto(`${baseUrl}/admin/after-sales/review`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '售后审核工作台', 'admin after-sale review page visible')
  await page.getByPlaceholder('售后单号、订单号或商品名').fill(reviewOrderNo)
  await page.getByRole('button', { name: '查询' }).first().click()
  await expectText(page, reviewOrderNo, 'admin after-sale review seeded order visible')
  await expectText(page, '审核处理台', 'admin after-sale review detail visible')
  await page.getByRole('button', { name: '要求补材料' }).click()
  await expectText(page, '已要求顾客补充材料', 'admin after-sale request evidence toast')
  await expectText(page, '待补材料', 'admin after-sale need evidence status visible')
  await page.goto(`${baseUrl}/admin/sla`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, 'SLA 中心', 'admin SLA center page visible')
  await page.getByPlaceholder('售后单号、订单号或商品名').fill(reviewOrderNo)
  await page.getByRole('button', { name: '查询' }).first().click()
  await expectText(page, reviewOrderNo, 'admin SLA seeded task visible')
  await expectText(page, '待顾客补材料', 'admin SLA waiting customer risk visible')
  await page.getByRole('button', { name: '处理' }).first().click()
  await expectText(page, '审核处理台', 'admin SLA task jumps to review desk')
  await apiPost(`/customer/after-sales/${seededReviewApplicationId}/evidence`, {
    evidenceType: 'LOGISTICS_NO',
    content: 'BROWSER-EVIDENCE-' + Date.now()
  }, demoCustomerAuth.token)
  await page.reload({ waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '凭证材料', 'admin after-sale evidence list visible')
  await expectText(page, 'BROWSER-EVIDENCE-', 'admin after-sale evidence content visible')
  await page.getByRole('button', { name: '审核通过' }).click()
  await expectText(page, '售后申请已审核通过', 'admin after-sale approve toast')
  await expectText(page, '待买家寄回', 'admin after-sale approved status visible')
  record('admin after-sale review approve flow', true, 'admin approved a seeded after-sale application')
  await page.screenshot({ path: path.join(artifactDir, '00-admin-after-sale-review.png'), fullPage: true })
  await page.goto(`${baseUrl}/showcase`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '闭环特色功能', 'showcase feature roadmap visible')
  await expectText(page, '演示流程', 'showcase demo flow visible')
  await expectText(page, '版本路线图', 'showcase version roadmap visible')
  await page.screenshot({ path: path.join(artifactDir, '01-showcase.png'), fullPage: true })

  await page.goto(`${baseUrl}/operations`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '售后运营指挥中心', 'operations page visible')
  await expectText(page, '新增特色功能', 'operations new feature metric visible')
  await expectText(page, '答辩亮点矩阵', 'operations feature matrix visible')
  await expectText(page, '意图热力雷达', 'operations intent radar visible')
  await expectText(page, '工单 SLA 风险队列', 'operations ticket risk visible')
  await expectText(page, '多渠道会话分布', 'operations channel distribution visible')
  await expectText(page, '知识命中 Top 榜', 'operations knowledge ranking visible')
  await expectText(page, '订单风险扫描', 'operations order risk visible')
  await expectText(page, 'AI 运行质量摘要', 'operations ai quality visible')
  await expectText(page, '下一步动作清单', 'operations action items visible')
  await expectText(page, '版本里程碑面板', 'operations version milestones visible')
  await page.screenshot({ path: path.join(artifactDir, '02-operations.png'), fullPage: true })

  await page.goto(`${baseUrl}/feature-closures`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '特色闭环中心', 'feature closure page visible')
  await expectText(page, '10+ 新增特色功能', 'feature closure 10 plus visible')
  await expectText(page, 'SLA 自动预警台', 'feature closure SLA visible')
  await expectText(page, '证据链完整度检查器', 'feature closure evidence visible')
  await expectText(page, 'RAG 命中复盘板', 'feature closure RAG visible')
  await expectText(page, '答辩演示编排器', 'feature closure demo builder visible')
  await expectText(page, '参考项目落点', 'feature closure references visible')
  await page.screenshot({ path: path.join(artifactDir, '03-feature-closures.png'), fullPage: true })

  await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '系统总览', 'dashboard page visible')
  await expectText(page, 'gpt-4o-mini', 'dashboard model visible')
  await page.screenshot({ path: path.join(artifactDir, '03-dashboard.png'), fullPage: true })

  await page.goto(`${baseUrl}/ai-test`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, 'AI 测试', 'ai test page visible')
  const aiTextarea = page.locator('textarea').first()
  await aiTextarea.fill('请回复：前端浏览器自动化测试成功。')
  await page.getByRole('button', { name: /发送测试/ }).click()
  await page.getByText(/成功|跳过|本地兜底/, { exact: false }).first().waitFor({ timeout: 90000 })
  await expectText(page, 'gpt-4o-mini', 'ai test model visible')
  await page.screenshot({ path: path.join(artifactDir, '04-ai-test-success.png'), fullPage: true })
  record('ai test interaction', true, 'AI enabled or local fallback displayed')

  await page.goto(`${baseUrl}/chat`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '咨询工作台', 'chat page visible')
  await expectText(page, '渠道筛选', 'chat channel filter visible')
  await expectText(page, 'App', 'chat app channel option visible')
  await expectText(page, '小程序', 'chat mini program channel option visible')
  await page.locator('textarea[placeholder*="输入售后问题"]').fill('这个订单能不能退货？')
  await page.getByRole('button', { name: /发送/ }).click()
  await page.getByText(/AI 增强|本地兜底/, { exact: false }).first().waitFor({ timeout: 120000 })
  await expectText(page, 'RETURN_APPLY', 'chat intent visible')
  await expectText(page, 'AI 决策摘要', 'chat decision summary visible')
  await expectText(page, '导出证据', 'chat evidence export visible')
  await expectText(page, '业务工具', 'chat business tools visible')
  await expectText(page, '建议追问', 'chat suggested questions visible')
  await expectText(page, '回答过程', 'chat process flow visible')
  await expectText(page, '上下文承接', 'chat context panel visible')
  await page.screenshot({ path: path.join(artifactDir, '05-chat-ai-enhanced.png'), fullPage: true })
  record('chat send interaction', true, 'chat reply displayed with AI or fallback source')

  await page.locator('textarea[placeholder*="输入售后问题"]').fill('商家一直不处理可以转人工投诉吗？')
  await page.getByRole('button', { name: /发送/ }).click()
  await page.getByText('人工转接', { exact: false }).first().waitFor({ timeout: 120000 })
  await page.screenshot({ path: path.join(artifactDir, '06-chat-ticket.png'), fullPage: true })
  record('chat ticket handoff interaction', true, 'manual handoff displayed')

  await page.goto(`${baseUrl}/knowledge`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '知识库', 'knowledge page visible')
  await expectText(page, '检索调试', 'knowledge search panel visible')
  await page.getByPlaceholder('输入用户问题，例如：退货多久到账').fill('退款多久到账')
  await page.getByRole('button', { name: /^检索$/ }).click()
  await expectText(page, '命中文档', 'knowledge hit count visible')
  await expectText(page, '意图覆盖', 'knowledge intent coverage visible')
  await expectText(page, '命中解释', 'knowledge hit reason visible')
  await expectText(page, '排序依据', 'knowledge ranking reason visible')
  await page.screenshot({ path: path.join(artifactDir, '07-knowledge.png'), fullPage: true })

  await page.goto(`${baseUrl}/orders`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '订单管理', 'orders page visible')
  await expectText(page, '订单详情', 'order detail visible')
  await page.screenshot({ path: path.join(artifactDir, '08-orders.png'), fullPage: true })

  await page.goto(`${baseUrl}/service-tickets`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '人工工单', 'service ticket page visible')
  await expectText(page, '优先级', 'service ticket priority visible')
  await expectText(page, '状态', 'service ticket status field visible')
  await expectText(page, 'AI 摘要', 'service ticket AI summary visible')
  await expectText(page, '处理建议', 'service ticket detail visible')
  await expectText(page, 'SLA 风险', 'service ticket SLA risk visible')
  await expectText(page, '售后处理时间线', 'service ticket timeline visible')
  await expectText(page, '下一步动作', 'service ticket next action visible')
  await page.screenshot({ path: path.join(artifactDir, '09-service-tickets.png'), fullPage: true })

  await page.goto(`${baseUrl}/logs`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '日志诊断中心', 'logs page visible')
  await expectText(page, 'AI 稳定性', 'logs diagnostic hero visible')
  await expectText(page, '健康趋势与风险诊断', 'logs health panel visible')
  await expectText(page, '运行健康度', 'logs health level visible')
  await expectText(page, '风险信号', 'logs risk signals visible')
  await expectText(page, '处置建议', 'logs action items visible')
  await expectText(page, '诊断总览', 'logs diagnostic summary visible')
  await expectText(page, 'AI 调用日志', 'ai logs tab visible')
  await page.screenshot({ path: path.join(artifactDir, '10-logs.png'), fullPage: true })
} catch (error) {
  record('browser smoke exception', false, error.message)
} finally {
  if (seededCustomerOrderId) {
    await apiDelete(`/orders/${seededCustomerOrderId}`).catch(error => {
      record('browser seeded order cleanup', false, error.message)
    })
  }
  if (seededReviewOrderId) {
    await apiDelete(`/orders/${seededReviewOrderId}`).catch(error => {
      record('browser review order cleanup', false, error.message)
    })
  }
  await browser.close()
}

if (consoleErrors.length) {
  record('browser console errors', false, consoleErrors.slice(0, 5).join(' | '))
}
if (pageErrors.length) {
  record('browser page errors', false, pageErrors.slice(0, 5).join(' | '))
}

console.table(results)
const failed = results.filter(item => !item.ok)
console.log(`FAILED_COUNT=${failed.length}`)
if (failed.length) {
  process.exit(1)
}
