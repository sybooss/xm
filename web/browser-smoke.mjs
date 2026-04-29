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
  await expectText(page, '咨询工作台', 'home redirects to chat')
  await expectText(page, '退换货客服', 'layout brand visible')
  await page.screenshot({ path: path.join(artifactDir, '01-chat-initial.png'), fullPage: true })

  await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '系统总览', 'dashboard page visible')
  await expectText(page, 'gpt-4o-mini', 'dashboard model visible')
  await page.screenshot({ path: path.join(artifactDir, '02-dashboard.png'), fullPage: true })

  await page.goto(`${baseUrl}/ai-test`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, 'AI 测试', 'ai test page visible')
  const aiTextarea = page.locator('textarea').first()
  await aiTextarea.fill('请回复：前端浏览器自动化测试成功。')
  await page.getByRole('button', { name: /发送测试/ }).click()
  await page.getByText('成功', { exact: false }).first().waitFor({ timeout: 90000 })
  await expectText(page, 'gpt-4o-mini', 'ai test model visible')
  await page.screenshot({ path: path.join(artifactDir, '03-ai-test-success.png'), fullPage: true })
  record('ai test interaction', true, 'SUCCESS tag displayed')

  await page.goto(`${baseUrl}/chat`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '咨询工作台', 'chat page visible')
  await page.locator('textarea[placeholder*="输入售后问题"]').fill('这个订单能不能退货？')
  await page.getByRole('button', { name: /发送/ }).click()
  await page.getByText('AI 增强', { exact: false }).first().waitFor({ timeout: 120000 })
  await expectText(page, 'RETURN_APPLY', 'chat intent visible')
  await page.screenshot({ path: path.join(artifactDir, '04-chat-ai-enhanced.png'), fullPage: true })
  record('chat send interaction', true, 'AI_ENHANCED reply displayed')

  await page.goto(`${baseUrl}/knowledge`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '知识库', 'knowledge page visible')
  await expectText(page, '检索调试', 'knowledge search panel visible')
  await page.screenshot({ path: path.join(artifactDir, '05-knowledge.png'), fullPage: true })

  await page.goto(`${baseUrl}/orders`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '订单管理', 'orders page visible')
  await expectText(page, '订单详情', 'order detail visible')
  await page.screenshot({ path: path.join(artifactDir, '06-orders.png'), fullPage: true })

  await page.goto(`${baseUrl}/logs`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '日志中心', 'logs page visible')
  await expectText(page, 'AI 调用日志', 'ai logs tab visible')
  await page.screenshot({ path: path.join(artifactDir, '07-logs.png'), fullPage: true })
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
