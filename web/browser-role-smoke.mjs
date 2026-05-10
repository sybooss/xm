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

async function expectPath(page, expectedPath, name) {
  await page.waitForFunction(path => window.location.pathname === path, expectedPath, { timeout: 20000 })
  record(name, new URL(page.url()).pathname === expectedPath, page.url())
}

async function expectCustomerRedirect(page, path) {
  await expectPath(page, '/customer/after-sales', `customer blocked ${path}`)
  await expectText(page, '我的售后', `customer fallback visible for ${path}`)
}

async function expectAdminRedirect(page, path) {
  await expectPath(page, '/admin/after-sales/review', `admin blocked ${path}`)
  await expectText(page, '售后审核工作台', `admin fallback visible for ${path}`)
}

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1366, height: 860 } })

try {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle', timeout: 60000 })
  await page.getByRole('button', { name: '客户' }).click()
  await page.locator('.login-form .login-button').click()
  await expectPath(page, '/customer/after-sales', 'customer lands on after-sale center')
  await expectText(page, '我的售后', 'customer after-sale center visible')
  await expectText(page, '我的订单', 'customer order panel visible')
  await expectText(page, '客户', 'customer role tag visible')

  const customerMenuTexts = ['我的售后', '在线咨询']
  for (const text of customerMenuTexts) {
    const visible = await page.getByRole('menuitem', { name: text }).count()
    record(`customer menu shows ${text}`, visible >= 1, `count=${visible}`)
  }

  const adminMenuTexts = ['答辩展示', '系统总览', '售后审核', 'SLA 中心', '客户画像', '知识库', '订单管理', '人工工单', '日志中心', 'AI 测试']
  for (const text of adminMenuTexts) {
    const visible = await page.getByRole('menuitem', { name: text }).count()
    record(`customer menu hides ${text}`, visible === 0, `count=${visible}`)
  }

  for (const text of ['运营指挥', '特色闭环']) {
    const visible = await page.getByRole('menuitem', { name: text }).count()
    record(`customer menu has no removed ${text}`, visible === 0, `count=${visible}`)
  }

  for (const path of ['/dashboard', '/operations', '/feature-closures', '/admin/after-sales/review', '/admin/sla', '/admin/customers/profile', '/orders', '/service-tickets', '/logs', '/ai-test', '/knowledge', '/showcase']) {
    await page.goto(`${baseUrl}${path}`, { waitUntil: 'networkidle', timeout: 60000 })
    await expectCustomerRedirect(page, path)
  }

  await page.goto(`${baseUrl}/chat`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectText(page, '咨询工作台', 'customer shared chat visible')
  await expectText(page, '渠道筛选', 'customer channel filter visible')

  await page.locator('header').getByRole('button', { name: /退出/ }).click()
  await expectText(page, '管理员登录', 'customer logout returns login')

  await page.locator('.login-form .login-button').click()
  await expectPath(page, '/admin/after-sales/review', 'admin lands on after-sale review')
  await expectText(page, '售后审核工作台', 'admin review desk visible')
  await expectText(page, '管理员', 'admin role tag visible')

  for (const text of adminMenuTexts) {
    const visible = await page.getByRole('menuitem', { name: text }).count()
    record(`admin menu shows ${text}`, visible >= 1, `count=${visible}`)
  }

  for (const text of ['运营指挥', '特色闭环']) {
    const visible = await page.getByRole('menuitem', { name: text }).count()
    record(`admin menu has no removed ${text}`, visible === 0, `count=${visible}`)
  }

  for (const path of ['/operations', '/feature-closures']) {
    await page.goto(`${baseUrl}${path}`, { waitUntil: 'networkidle', timeout: 60000 })
    await expectPath(page, '/showcase', `admin removed route redirects ${path}`)
  }

  const adminHiddenCustomerMenu = await page.getByRole('menuitem', { name: '我的售后' }).count()
  record('admin menu hides 我的售后', adminHiddenCustomerMenu === 0, `count=${adminHiddenCustomerMenu}`)

  await page.goto(`${baseUrl}/customer/after-sales`, { waitUntil: 'networkidle', timeout: 60000 })
  await expectAdminRedirect(page, '/customer/after-sales')
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
