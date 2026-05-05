<template>
  <section class="showcase-page">
    <div class="showcase-hero panel">
      <div class="hero-copy">
        <p class="hero-kicker">《复杂软件系统实践》高分展示入口</p>
        <h2>电商退换货 AI 客服系统</h2>
        <p>
          把售后咨询、知识依据、订单上下文、AI 增强、人工工单和日志追踪集中到一条可演示链路，
          答辩时先展示完整系统，再展开个人负责模块和技术取舍。
        </p>
        <div class="hero-actions">
          <el-button type="primary" :icon="ChatDotRound" @click="$router.push('/chat')">
            开始演示链路
          </el-button>
          <el-button :icon="DocumentChecked" @click="$router.push('/logs')">
            查看验证证据
          </el-button>
        </div>
      </div>
      <div class="hero-system">
        <div class="system-card">
          <span>数据库</span>
          <StatusTag :value="systemStore.database.status" />
        </div>
        <div class="system-card">
          <span>AI 状态</span>
          <StatusTag :value="systemStore.ai.status" />
        </div>
        <div class="system-card wide">
          <span>当前模型</span>
          <strong>{{ systemStore.ai.modelName || 'local-rule-template' }}</strong>
        </div>
        <div class="system-card wide">
          <span>兜底策略</span>
          <strong>{{ systemStore.ai.fallbackEnabled ? '已启用，可离线稳定演示' : '未开启' }}</strong>
        </div>
      </div>
    </div>

    <div class="showcase-layout">
      <main class="showcase-main">
        <section class="panel">
          <div class="panel-header">
            <h3 class="panel-title">演示流程</h3>
            <span class="panel-caption">按这个顺序讲，能把完整项目能力串成一条业务闭环。</span>
          </div>
          <div class="demo-flow">
            <article v-for="(step, index) in demoSteps" :key="step.title" class="flow-step">
              <span class="step-index">{{ index + 1 }}</span>
              <el-icon><component :is="step.icon" /></el-icon>
              <div>
                <h4>{{ step.title }}</h4>
                <p>{{ step.detail }}</p>
              </div>
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <h3 class="panel-title">系统核心亮点（已实现）</h3>
            <span class="panel-caption">每一项都有页面入口、接口或日志证据支撑。</span>
          </div>
          <div class="highlight-grid">
            <article v-for="item in highlights" :key="item.title" class="highlight-card">
              <div class="highlight-icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="highlight-content">
                <h4>{{ item.title }}</h4>
                <p>{{ item.detail }}</p>
                <span class="evidence">{{ item.evidence }}</span>
              </div>
              <StatusTag class="verified-tag" value="已验证" />
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <h3 class="panel-title">答辩讲解骨架</h3>
            <span class="panel-caption">讲实现时避免空泛，直接落到代码、接口、数据和验证。</span>
          </div>
          <div class="defense-grid">
            <article v-for="item in defensePoints" :key="item.title" class="defense-card">
              <h4>{{ item.title }}</h4>
              <p>{{ item.detail }}</p>
            </article>
          </div>
        </section>
      </main>

      <aside class="showcase-side">
        <section class="panel side-panel">
          <div class="panel-header">
            <h3 class="panel-title">答辩演示顺序</h3>
          </div>
          <div class="side-steps">
            <button
              v-for="(item, index) in sideLinks"
              :key="item.path"
              class="side-step"
              type="button"
              @click="$router.push(item.path)"
            >
              <span class="side-index">{{ index + 1 }}</span>
              <span>
                <strong>{{ item.title }}</strong>
                <small>{{ item.detail }}</small>
              </span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </section>

        <section class="panel side-panel">
          <div class="panel-header">
            <h3 class="panel-title">现场检查清单</h3>
          </div>
          <ul class="check-list">
            <li v-for="item in checklist" :key="item">{{ item }}</li>
          </ul>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import {
  ArrowRight,
  ChatDotRound,
  Collection,
  Connection,
  Cpu,
  DataAnalysis,
  Document,
  DocumentChecked,
  Finished,
  Lock,
  Operation,
  Search,
  Service,
  Tickets,
  TrendCharts
} from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import { useSystemStore } from '../stores/systemStore'

const systemStore = useSystemStore()

const demoSteps = [
  { title: '登录与权限', detail: '先展示注册客户，再切换管理员，证明客户侧和后台侧权限隔离。', icon: Lock },
  { title: '咨询工作台', detail: '绑定订单 DD202604290001，发送退货问题，观察流式处理进度。', icon: ChatDotRound },
  { title: '知识库依据', detail: '展示命中文档、问题关键词和规则内容，说明回答不是空生成。', icon: Collection },
  { title: '人工工单', detail: '投诉或物流异常自动升级工单，展示优先级、AI 摘要和处理建议。', icon: Service },
  { title: '日志与测试', detail: '用 AI 调用日志、检索日志、处理轨迹和 AI 测试页收尾。', icon: DocumentChecked }
]

const highlights = [
  {
    title: 'AI 流式客服',
    detail: '消息发送后立即进入处理状态，后端通过 SSE 返回进度，前端逐字展示最终回复。',
    evidence: 'POST /chat-sessions/{id}/message-stream',
    icon: ChatDotRound
  },
  {
    title: 'RAG 知识增强',
    detail: '回答前检索知识文档，右侧面板展示命中文档和依据摘要，日志记录检索过程。',
    evidence: 'GET /knowledge-docs/search',
    icon: Search
  },
  {
    title: '多轮上下文承接',
    detail: '追问退款、投诉等问题时，系统会读取会话摘要和上轮意图，避免每轮都从零判断。',
    evidence: 'CONTEXT_RESOLVE 处理轨迹',
    icon: Connection
  },
  {
    title: 'LangChain4j 工具调用',
    detail: '订单查询、知识检索、工单创建被封装为业务工具，AI 只增强流程，不绕过业务层。',
    evidence: 'AiBusinessToolService',
    icon: Operation
  },
  {
    title: '智能工单升级',
    detail: '投诉、人工客服、物流异常等场景可自动生成工单，保留会话、订单和处理建议。',
    evidence: 'GET /service-tickets',
    icon: Service
  },
  {
    title: '本地规则兜底',
    detail: '未配置模型或调用失败时仍可用本地规则模板回答，保证答辩现场稳定演示。',
    evidence: 'FALLBACK / SKIPPED 状态',
    icon: Finished
  },
  {
    title: '权限与安全控制',
    detail: '客户只能访问自己的会话和订单；管理端新增、修改、删除操作需要管理员身份。',
    evidence: 'AuthInterceptor + @OperatorAnno',
    icon: Lock
  },
  {
    title: '日志可追溯',
    detail: 'AI 调用、知识检索和处理轨迹可在页面查看，便于排障，也能作为答辩证据。',
    evidence: 'AI 调用日志 / 检索日志 / 轨迹',
    icon: Document
  }
]

const defensePoints = [
  {
    title: '完整业务闭环',
    detail: '从登录、订单绑定、售后咨询、知识依据、人工工单到日志追踪，覆盖真实售后客服主要流程。'
  },
  {
    title: '工程化设计',
    detail: '后端按 Controller、Service、Mapper 分层，接口遵循资源名词；前端按路由、Store、API、组件拆分。'
  },
  {
    title: 'AI 不是孤立功能',
    detail: 'LangChain4j 位于业务服务之后，只负责增强回复和工具编排；本地规则兜底保证基础业务可用。'
  },
  {
    title: '可验证可复现',
    detail: '构建、全链路接口烟测、浏览器烟测和文档脚本共同支撑，不把无法演示的内容写成亮点。'
  }
]

const sideLinks = [
  { title: '咨询工作台', detail: '发送退货、退款和投诉问题', path: '/chat' },
  { title: '知识库', detail: '查看规则文档和检索调试', path: '/knowledge' },
  { title: '人工工单', detail: '展示投诉升级和状态流转', path: '/service-tickets' },
  { title: '日志中心', detail: '查看 AI、检索和处理轨迹', path: '/logs' },
  { title: 'AI 测试', detail: '验证模型或本地兜底状态', path: '/ai-test' }
]

const checklist = [
  '后端 8081 和前端 5173 已启动',
  '管理员账号 admin / 123456 可登录',
  '演示订单 DD202604290001 可绑定',
  'AI 不可用时确认本地兜底仍可回答',
  '结束前打开日志中心展示证据'
]

onMounted(() => {
  systemStore.loadStatus().catch(() => {})
})
</script>

<style scoped>
.showcase-page {
  min-height: calc(100vh - var(--header-height));
  padding: 18px;
  background:
    linear-gradient(180deg, rgb(255 255 255 / 92%), rgb(246 248 251 / 92%)),
    radial-gradient(circle at 80% 0%, rgb(37 99 235 / 8%), transparent 34%);
}

.showcase-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
  gap: 24px;
  min-height: 238px;
  padding: 34px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 98%), rgb(248 251 255 / 96%)),
    repeating-linear-gradient(135deg, transparent 0 18px, rgb(37 99 235 / 3%) 18px 19px);
  box-shadow: 0 18px 60px rgb(15 23 42 / 8%);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-kicker {
  margin: 0 0 14px;
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

.hero-copy h2 {
  max-width: 720px;
  margin: 0;
  color: #0f172a;
  font-size: 38px;
  font-weight: 800;
  line-height: 1.14;
}

.hero-copy p:not(.hero-kicker) {
  max-width: 720px;
  margin: 18px 0 0;
  color: #4b5563;
  font-size: 15px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-system {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-content: center;
}

.system-card {
  display: grid;
  gap: 10px;
  min-height: 86px;
  padding: 16px;
  border: 1px solid rgb(213 219 228 / 82%);
  border-radius: 10px;
  background: rgb(255 255 255 / 86%);
  box-shadow: 0 10px 28px rgb(15 23 42 / 5%);
}

.system-card.wide {
  grid-column: span 2;
}

.system-card span {
  color: var(--text-muted);
  font-size: 13px;
}

.system-card strong {
  overflow: hidden;
  color: #111827;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.showcase-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  margin-top: 16px;
}

.showcase-main {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.panel-caption {
  color: var(--text-muted);
  font-size: 13px;
}

.demo-flow {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
}

.flow-step {
  position: relative;
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 12px;
  min-height: 168px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff, #f9fbff);
}

.step-index {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid #dbe5f4;
  border-radius: 50%;
  color: var(--brand);
  font-size: 13px;
  font-weight: 800;
}

.flow-step .el-icon {
  color: var(--brand);
  font-size: 26px;
}

.flow-step h4,
.highlight-card h4,
.defense-card h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  line-height: 1.35;
}

.flow-step p,
.highlight-card p,
.defense-card p {
  margin: 8px 0 0;
  color: #5f6b7a;
  font-size: 13px;
  line-height: 1.7;
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
}

.highlight-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 158px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: #ffffff;
}

.verified-tag {
  grid-column: 2;
  justify-self: start;
}

.highlight-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 1px solid #dbe8fb;
  border-radius: 10px;
  background: #f4f8ff;
  color: var(--brand);
}

.highlight-content {
  min-width: 0;
}

.evidence {
  display: block;
  margin-top: 10px;
  overflow: hidden;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.defense-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
}

.defense-card {
  min-height: 128px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: #fbfcff;
}

.showcase-side {
  display: grid;
  align-content: start;
  gap: 16px;
}

.side-panel {
  position: sticky;
  top: calc(var(--header-height) + 16px);
}

.side-panel + .side-panel {
  top: 434px;
}

.side-steps {
  display: grid;
  gap: 6px;
  padding: 12px;
}

.side-step {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.side-step:hover {
  border-color: #dbeafe;
  background: #f6f9ff;
}

.side-index {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #edf4ff;
  color: var(--brand);
  font-size: 12px;
  font-weight: 800;
}

.side-step strong,
.side-step small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-step strong {
  font-size: 14px;
}

.side-step small {
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 12px;
}

.check-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 14px 18px 18px 34px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 1260px) {
  .showcase-layout,
  .showcase-hero {
    grid-template-columns: 1fr;
  }

  .showcase-side,
  .side-panel,
  .side-panel + .side-panel {
    position: static;
  }

  .highlight-grid,
  .defense-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 940px) {
  .demo-flow {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .showcase-page {
    padding: 12px;
  }

  .showcase-hero {
    padding: 22px;
  }

  .hero-copy h2 {
    font-size: 28px;
  }

  .hero-system,
  .highlight-grid,
  .defense-grid {
    grid-template-columns: 1fr;
  }

  .system-card.wide {
    grid-column: auto;
  }

  .panel-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
