import { chromium } from 'playwright'

const baseUrl = 'http://localhost:5173'
const results = []

function record(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok), detail })
}

async function expectText(page, text, name) {
  await page.getByText(text, { exact: false }).first().waitFor({ timeout: 20000 })
  record(name, true, text)
}

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1366, height: 860 } })

try {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle', timeout: 60000 })
  await page.getByRole('button', { name: '客户' }).click()
  await page.getByRole('button', { name: /^登录$/ }).click()
  await expectText(page, '咨询工作台', 'customer lands on chat')
  await expectText(page, 'CUSTOMER', 'customer role tag visible')

  const adminMenuTexts = ['系统总览', '知识库', '订单管理', '人工工单', '日志中心', 'AI 测试']
  for (const text of adminMenuTexts) {
    const visible = await page.getByRole('menuitem', { name: text }).count()
    record(`customer menu hides ${text}`, visible === 0, `count=${visible}`)
  }

  for (const path of ['/dashboard', '/orders', '/service-tickets', '/logs', '/ai-test', '/knowledge']) {
    await page.goto(`${baseUrl}${path}`, { waitUntil: 'networkidle', timeout: 60000 })
    await page.waitForURL('**/chat', { timeout: 20000 })
    record(`customer blocked ${path}`, page.url().endsWith('/chat'), page.url())
  }

  await page.getByRole('button', { name: /我的订单/ }).click()
  await expectText(page, '申请售后', 'customer order panel visible')
} catch (error) {
  record('role browser smoke exception', false, error.message)
} finally {
  await browser.close()
}

console.table(results)
const failed = results.filter(item => !item.ok)
console.log(`FAILED_COUNT=${failed.length}`)
if (failed.length) {
  process.exit(1)
}
