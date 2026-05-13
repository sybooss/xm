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
let seededReviewTicketSessionId = null
const productIssueOrderIds = []
const productIssueProductName = '浏览器聚合预警耳机'
let demoCustomerToken = ''
let lastOrderResponseSummary = ''

function record(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok), detail })
}

async function expectText(page, text, name) {
  await page.getByText(text, { exact: false }).first().waitFor({ timeout: 20000 })
  record(name, true, text)
}

async function expectVisibleBodyText(page, text, name) {
  try {
    await page.waitForFunction(expected => document.body.innerText.includes(expected), text, { timeout: 20000 })
  } catch (error) {
    const body = await page.locator('body').innerText().catch(() => '')
    throw new Error(`${name} missing ${text}. Last order response: ${lastOrderResponseSummary}. Body snapshot: ${body.slice(0, 1200)}`)
  }
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
page.on('response', async response => {
  if (response.url().includes('/api/orders') && response.url().includes('keyword=')) {
    const text = await response.text().catch(error => error.message)
    lastOrderResponseSummary = `${response.status()} ${response.url()} ${text.slice(0, 500)}`
  }
})

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
  const demoCustomer = await apiPost('/auth/login', { username: 'demo_customer', password: '123456' }, '')
  const browserCustomerToken = demoCustomer.token
  await page.evaluate(user => {
    localStorage.setItem('returns_assistant_token', user.token)
    localStorage.setItem('returns_assistant_user', JSON.stringify({
      userId: user.userId,
      username: user.username,
      displayName: user.displayName,
      role: user.role,
      expiresAt: user.expiresAt
    }))
  }, demoCustomer)
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
  const seededCustomerOrderPage = await apiGet(`/orders?page=1&pageSize=8&keyword=${orderNoForUiApply}`, browserCustomerToken)
  record('customer seeded order api visible', seededCustomerOrderPage.total === 1, `total=${seededCustomerOrderPage.total}, user=${demoCustomer.userId}`)
  await page.goto(`${baseUrl}/customer/after-sales`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '我的订单', 'customer after-sale center reloaded after seeding order')
  const customerOrderPanel = page.locator('.workspace-grid .panel').first()
  await customerOrderPanel.locator('input').first().fill(orderNoForUiApply)
  await Promise.all([
    page.waitForResponse(response => response.url().includes('/api/orders') && response.url().includes(`keyword=${orderNoForUiApply}`), { timeout: 30000 }),
    customerOrderPanel.locator('button').first().click()
  ])
  await expectVisibleBodyText(page, orderNoForUiApply, 'customer seeded order visible')
  await customerOrderPanel.locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: orderNoForUiApply })
    .first()
    .locator('button')
    .first()
    .click()
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
  await expectText(page, '售后审核工作台', 'admin login redirects to after-sale review')
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
  demoCustomerToken = demoCustomerAuth.token
  await apiPost('/customer/after-sales', {
    orderId: seededReviewOrderId,
    serviceType: 'RETURN',
    reasonCode: 'QUALITY_PROBLEM',
    reasonText: '浏览器自动化为管理员审核台创建待审核售后申请。',
    refundAmount: 188.80
  }, demoCustomerToken)
  const reviewApplicationPage = await apiGet(`/customer/after-sales?page=1&pageSize=1&keyword=${reviewOrderNo}`, demoCustomerToken)
  seededReviewApplicationId = reviewApplicationPage.rows?.[0]?.id
  for (let i = 0; i < 3; i += 1) {
    const issueOrderNo = `BISSUE${Date.now()}${i}`
    await apiPost('/orders', {
      orderNo: issueOrderNo,
      userId: demoCustomerAuth.userId,
      productName: productIssueProductName,
      skuName: `断连样本 ${i + 1}`,
      orderAmount: 329.00,
      payStatus: 'PAID',
      orderStatus: 'SIGNED',
      logisticsStatus: 'DELIVERED',
      afterSaleStatus: 'NONE',
      paidAt: '2026-04-23T10:00:00',
      shippedAt: '2026-04-24T10:00:00',
      signedAt: '2026-04-25T10:00:00'
    })
    const issueOrder = await apiGet(`/orders/no/${issueOrderNo}`)
    productIssueOrderIds.push(issueOrder.id)
    await apiPost('/customer/after-sales', {
      orderId: issueOrder.id,
      serviceType: 'EXCHANGE',
      reasonCode: 'QUALITY_PROBLEM',
      reasonText: '蓝牙耳机断连，左耳无声，申请换货检测。',
      refundAmount: 329.00
    }, demoCustomerToken)
  }
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
  const waitingCustomerTasks = await apiGet(`/admin/sla/tasks?page=1&pageSize=10&keyword=${reviewOrderNo}`)
  const waitingTask = waitingCustomerTasks.rows?.find(item => item.id === seededReviewApplicationId)
  record('admin SLA waiting customer task visible', waitingTask?.status === 'NEED_MORE_EVIDENCE', waitingTask?.status || waitingTask?.riskLabel || '')
  await page.getByRole('button', { name: '处理' }).first().click()
  await expectText(page, '审核处理台', 'admin SLA task jumps to review desk')
  await apiPost(`/customer/after-sales/${seededReviewApplicationId}/evidence`, {
    evidenceType: 'LOGISTICS_NO',
    content: 'BROWSER-EVIDENCE-' + Date.now()
  }, demoCustomerToken)
  await page.reload({ waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '凭证真实性审核', 'admin evidence audit panel visible')
  await expectText(page, '凭证材料', 'admin after-sale evidence list visible')
  await expectText(page, 'BROWSER-EVIDENCE-', 'admin after-sale evidence content visible')
  await page.getByRole('button', { name: '审核全部凭证' }).click()
  await expectText(page, '全部凭证已完成审核', 'admin evidence audit all toast')
  await expectText(page, /凭证通过|需补材料|风险较高|人工复核/, 'admin evidence audit result visible')
  await expectText(page, '售后风险识别', 'admin risk assessment panel visible')
  await page.getByRole('button', { name: '重新评估' }).click()
  await expectText(page, '售后风险评估已更新', 'admin risk assessment toast')
  await expectText(page, /标准风险|证据不足|SLA 临近|近期重复售后|高频售后用户/, 'admin risk assessment tags visible')
  await page.goto(`${baseUrl}/admin/sla`, { waitUntil: 'networkidle', timeout: 60000 })
  await page.getByPlaceholder('售后单号、订单号或商品名').fill(reviewOrderNo)
  await page.getByRole('button', { name: '查询' }).first().click()
  await expectText(page, '评估等级', 'admin SLA assessment level column visible')
  await expectText(page, '风险分', 'admin SLA risk score column visible')
  await expectText(page, /LOW|MEDIUM|HIGH|低|中|高/, 'admin SLA risk assessment value visible')
  await apiPost('/admin/product-issue-insights/refresh', { days: 7 })
  await page.goto(`${baseUrl}/admin/product-issues`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '商品质量预警', 'product issue insight page visible')
  await expectText(page, '开放预警', 'product issue summary visible')
  await expectText(page, '刷新预警', 'product issue refresh button visible')
  await page.getByPlaceholder('商品、关键词或建议动作').fill(productIssueProductName)
  await page.getByRole('button', { name: '查询' }).first().click()
  await expectText(page, productIssueProductName, 'product issue seeded product visible')
  await expectText(page, '断连', 'product issue keyword visible')
  await expectText(page, '运营建议', 'product issue suggested action visible')
  await expectText(page, /中|高|MEDIUM|HIGH/, 'product issue alert level visible')
  await page.screenshot({ path: path.join(artifactDir, '00-product-issues.png'), fullPage: true })
  await page.goto(`${baseUrl}/admin/after-sales/review`, { waitUntil: 'networkidle', timeout: 60000 })
  await page.getByPlaceholder('售后单号、订单号或商品名').fill(reviewOrderNo)
  await page.getByRole('button', { name: '查询' }).first().click()
  await expectText(page, '审核处理台', 'admin after-sale review detail after risk visible')
  await page.getByRole('button', { name: '创建关联工单' }).click()
  await expectText(page, '已创建关联工单', 'admin after-sale linked ticket toast')
  await expectText(page, '关联客服工单', 'admin after-sale linked ticket section visible')
  await expectText(page, '创建工单', 'admin after-sale create ticket log visible')
  const linkedAfterSaleDetail = await apiGet(`/admin/after-sales/${seededReviewApplicationId}`)
  seededReviewTicketSessionId = linkedAfterSaleDetail.ticketId
    ? (await apiGet(`/service-tickets/${linkedAfterSaleDetail.ticketId}`)).sessionId
    : null
  const draftResponse = page.waitForResponse(response =>
    response.url().includes(`/api/admin/after-sales/${seededReviewApplicationId}/reply-drafts`)
      && response.request().method() === 'POST',
  { timeout: 120000 })
  await page.getByRole('button', { name: '生成回复草稿' }).click()
  const draftResult = await (await draftResponse).json()
  record('admin AI copilot draft create response', draftResult.code === 1, draftResult?.data?.status || draftResult?.msg || '')
  await expectText(page, 'AI 副驾驶回复草稿', 'admin AI copilot panel visible')
  await expectText(page, '草稿', 'admin AI copilot draft status visible')
  await page.getByRole('button', { name: '采纳草稿' }).first().click()
  await expectText(page, '回复草稿已采纳', 'admin AI copilot use draft toast')
  await expectText(page, '已采纳', 'admin AI copilot used status visible')
  await page.getByRole('button', { name: '审核通过' }).click()
  await expectText(page, '售后申请已审核通过', 'admin after-sale approve toast')
  await expectText(page, '待买家寄回', 'admin after-sale approved status visible')
  await page.getByRole('button', { name: '确认完成' }).click()
  await expectText(page, '售后处理已确认完成', 'admin after-sale complete toast')
  await expectText(page, '已完成', 'admin after-sale completed status visible')
  record('admin after-sale review approve flow', true, 'admin approved a seeded after-sale application')
  await page.screenshot({ path: path.join(artifactDir, '00-admin-after-sale-review.png'), fullPage: true })

  await page.locator('header').getByRole('button', { name: /退出/ }).click()
  await expectText(page, '管理员登录', 'admin logout before customer review')
  await page.getByRole('button', { name: '客户' }).click()
  await page.locator('.login-form .login-button').click()
  await expectText(page, '我的售后', 'demo customer back to after-sales for review')
  await page.evaluate(user => {
    localStorage.setItem('returns_assistant_token', user.token)
    localStorage.setItem('returns_assistant_user', JSON.stringify({
      userId: user.userId,
      username: user.username,
      displayName: user.displayName,
      role: user.role,
      expiresAt: user.expiresAt
    }))
  }, demoCustomerAuth)
  const customerReviewApplication = await apiGet(`/customer/after-sales/${seededReviewApplicationId}`, demoCustomerAuth.token)
  record(
    'customer completed after-sale api visible',
    customerReviewApplication.orderNo === reviewOrderNo &&
      customerReviewApplication.status === 'COMPLETED' &&
      customerReviewApplication.customerResultSummary?.includes('处理完成') &&
      customerReviewApplication.customerNextAction?.includes('评价'),
    `status=${customerReviewApplication.status},order=${customerReviewApplication.orderNo},result=${customerReviewApplication.customerResultSummary}`
  )
  await page.goto(`${baseUrl}/customer/after-sales?focus=${seededReviewApplicationId}`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectVisibleBodyText(page, reviewOrderNo, 'customer completed after-sale visible')
  await expectText(page, '处理结果说明', 'customer result explanation visible')
  await expectText(page, '客服最终回复', 'customer final reply visible')
  await page.getByRole('button', { name: '评价服务' }).click()
  await expectText(page, '评价服务', 'customer review dialog visible')
  await page.getByPlaceholder('例如：响应快、处理清楚、还需跟进').fill('响应快,处理清楚')
  await page.locator('textarea[placeholder*="售后处理体验"]').fill('浏览器自动化评价：处理过程清楚，客服响应及时。')
  await page.getByRole('button', { name: '提交评价' }).click()
  await expectText(page, '评价已提交', 'customer review submit toast')
  await expectText(page, '我的评价', 'customer review summary visible')

  await page.locator('header').getByRole('button', { name: /退出/ }).click()
  await expectText(page, '管理员登录', 'customer logout before profile')
  await page.getByRole('button', { name: '管理员' }).click()
  await page.locator('.login-form .login-button').click()
  await expectText(page, '售后审核工作台', 'admin login before customer profile')
  await page.goto(`${baseUrl}/admin/customers/profile?userId=${demoCustomerAuth.userId}`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '客户画像', 'admin customer profile page visible')
  await expectText(page, '服务评价', 'admin customer profile reviews visible')
  await expectText(page, '浏览器自动化评价', 'admin customer profile review content visible')
  await expectText(page, '风险等级', 'admin customer profile risk visible')
  await expectText(page, '近30天售后', 'admin customer profile recent stats visible')
  await expectText(page, '投诉占比', 'admin customer profile complaint rate visible')
  await expectText(page, '低分原因', 'admin customer profile low rating reason visible')
  await page.goto(`${baseUrl}/showcase`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '闭环特色功能', 'showcase feature roadmap visible')
  await expectText(page, '流程', 'showcase flow visible')
  record('showcase removes version roadmap', !(await page.getByText('版本路线图', { exact: false }).count()), '版本路线图 absent')
  record('showcase removes metrics panel', !(await page.getByText('关键指标', { exact: false }).count()), '关键指标 absent')
  record('showcase removes operations card', !(await page.getByText('售后运营指挥中心', { exact: false }).count()), '售后运营指挥中心 absent')
  record('showcase removes display polish card', !(await page.getByText('Apple-like 展示中心', { exact: false }).count()), 'Apple-like 展示中心 absent')
  record('showcase removes multi-channel card', !(await page.getByText('多渠道接入', { exact: false }).count()), '多渠道接入 absent')
  await page.screenshot({ path: path.join(artifactDir, '01-showcase.png'), fullPage: true })

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
  await expectText(page, '测试台', 'chat admin test channel option visible')
  record('chat removes app channel option', !(await page.getByText('App', { exact: true }).count()), 'App absent')
  record('chat removes mini program channel option', !(await page.getByText('小程序', { exact: true }).count()), '小程序 absent')
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
  await expectText(page, '原始会话', 'service ticket conversation visible')
  await expectText(page, '人工接管回复工作台', 'service ticket manual workbench visible')
  await expectText(page, '发送人工回复', 'service ticket manual reply button visible')
  await expectText(page, '售后处理时间线', 'service ticket timeline visible')
  await expectText(page, '下一步动作', 'service ticket next action visible')
  await page.getByRole('button', { name: '接管会话' }).click()
  await expectText(page, '已接管会话', 'service ticket takeover toast')
  const browserManualReply = `浏览器自动化人工回复 ${Date.now()}：客服已接管，会继续核对订单和售后凭证。`
  await page.locator('textarea[placeholder*="人工客服最终回复"]').fill(browserManualReply)
  await page.getByRole('button', { name: '发送人工回复' }).click()
  await expectText(page, '人工回复已发送', 'service ticket manual reply toast')
  await expectVisibleBodyText(page, browserManualReply, 'service ticket manual reply visible')
  await expectText(page, '人工', 'service ticket manual source tag visible')
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
  for (const orderId of productIssueOrderIds) {
    await apiDelete(`/orders/${orderId}`).catch(error => {
      record('browser product issue order cleanup', false, error.message)
    })
  }
  if (seededReviewTicketSessionId) {
    await apiDelete(`/chat-sessions/${seededReviewTicketSessionId}`).catch(error => {
      record('browser linked ticket session cleanup', false, error.message)
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
