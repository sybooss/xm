import { chromium } from '../web/node_modules/playwright/index.mjs'
import fs from 'node:fs/promises'
import path from 'node:path'

const baseUrl = process.env.FRONTEND_URL || 'http://localhost:5173'
const backendUrl = process.env.BACKEND_URL || 'http://localhost:8081'
const artifactDir = path.resolve('D:/复制软件系统/output/final-report-screenshots')
await fs.mkdir(artifactDir, { recursive: true })

async function apiPost(pathname, body, token = '') {
  const response = await fetch(`${backendUrl}${pathname}`, {
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

async function apiGet(pathname, token = '') {
  const response = await fetch(`${backendUrl}${pathname}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  const result = await response.json()
  if (result.code !== 1) {
    throw new Error(`${pathname} failed: ${result.msg}`)
  }
  return result.data
}

async function waitVisibleText(page, text) {
  await page.getByText(text, { exact: false }).filter({ visible: true }).first().waitFor({ timeout: 30000 })
}

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 920 }, deviceScaleFactor: 1 })

try {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle', timeout: 60000 })
  await waitVisibleText(page, '管理员登录')
  await page.screenshot({ path: path.join(artifactDir, 'login.png'), fullPage: true })

  const admin = await apiPost('/auth/login', { username: 'admin', password: '123456' })
  const customer = await apiPost('/auth/login', { username: 'demo_customer', password: '123456' })
  await page.evaluate(user => {
    localStorage.setItem('returns_assistant_token', user.token)
    localStorage.setItem('returns_assistant_user', JSON.stringify({
      userId: user.userId,
      username: user.username,
      displayName: user.displayName,
      role: user.role,
      expiresAt: user.expiresAt
    }))
  }, admin)

  await page.goto(`${baseUrl}/admin/sla`, { waitUntil: 'networkidle', timeout: 60000 })
  await waitVisibleText(page, 'SLA 跟进')
  await page.screenshot({ path: path.join(artifactDir, 'sla-center.png'), fullPage: true })

  await page.goto(`${baseUrl}/admin/customers/profile?userId=${customer.userId}`, { waitUntil: 'networkidle', timeout: 60000 })
  await waitVisibleText(page, '客户画像')
  await page.screenshot({ path: path.join(artifactDir, 'customer-profile.png'), fullPage: true })

  await page.goto(`${baseUrl}/chat`, { waitUntil: 'networkidle', timeout: 60000 })
  await waitVisibleText(page, '咨询工作台')
  const riskTab = page.getByRole('tab', { name: '图片/凭证风险' })
  if (await riskTab.count()) {
    await riskTab.click()
    await page.waitForTimeout(600)
  }
  await page.screenshot({ path: path.join(artifactDir, 'chat-image-risk-tab.png'), fullPage: true })

  const status = await apiGet('/system/status', admin.token)
  await fs.writeFile(path.join(artifactDir, 'capture-metadata.json'), JSON.stringify({
    capturedAt: new Date().toISOString(),
    frontendUrl: baseUrl,
    backendUrl,
    model: status?.ai?.modelName || status?.ai?.activeModelName || null,
    database: status?.database?.status || null
  }, null, 2), 'utf8')
} finally {
  await browser.close()
}
