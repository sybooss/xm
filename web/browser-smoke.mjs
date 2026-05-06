import { chromium } from 'playwright'
import fs from 'node:fs/promises'
import path from 'node:path'

const baseUrl = 'http://localhost:5173'
const artifactDir = path.resolve('D:/复制软件系统/output/playwright')
await fs.mkdir(artifactDir, { recursive: true })

const results = []
const consoleErrors = []
const pageErrors = []

function record(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok), detail })
}

async function expectText(page, text, name) {
  await page.getByText(text, { exact: false }).first().waitFor({ timeout: 20000 })
  record(name, true, text)
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
  await page.goto(baseUrl, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '管理员登录', 'home redirects to login')
  await page.getByRole('button', { name: '注册' }).click()
  const registerUsername = `browser_user_${Date.now()}`
  await page.getByPlaceholder('4-30位字母、数字或下划线').fill(registerUsername)
  await page.getByPlaceholder('用于页面展示，可不填').fill('浏览器注册用户')
  await page.getByPlaceholder('123456').fill('browser123')
  await page.getByPlaceholder('再次输入密码').fill('browser123')
  await page.getByRole('button', { name: '注册并登录' }).click()
  await expectText(page, '咨询工作台', 'register redirects to chat')
  await expectText(page, '客户', 'registered customer role visible')
  await page.locator('header').getByRole('button', { name: /退出/ }).click()
  await expectText(page, '管理员登录', 'registered customer logout returns login')
  await page.locator('.login-form .login-button').click()
  await expectText(page, '答辩展示中心', 'login redirects to showcase')
  await expectText(page, '退换货客服', 'layout brand visible')
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
  await page.locator('textarea[placeholder*="输入售后问题"]').fill('这个订单能不能退货？')
  await page.getByRole('button', { name: /发送/ }).click()
  await page.getByText(/AI 增强|本地兜底/, { exact: false }).first().waitFor({ timeout: 120000 })
  await expectText(page, 'RETURN_APPLY', 'chat intent visible')
  await expectText(page, 'AI 决策摘要', 'chat decision summary visible')
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
