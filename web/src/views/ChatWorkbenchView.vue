<template>
  <section class="chat-page">
    <aside class="session-panel panel">
      <div class="panel-header">
        <h3 class="panel-title">会话</h3>
        <el-button type="primary" :icon="Plus" circle @click="newSession" />
      </div>
      <div class="session-create">
        <el-input v-model="orderNo" placeholder="订单号" clearable />
        <el-button :icon="Search" @click="newSession">绑定</el-button>
      </div>
      <div v-if="chatStore.sessions.length" class="session-list">
        <button
          v-for="session in chatStore.sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === chatStore.currentSessionId }"
          @click="selectSession(session.id)"
        >
          <span class="session-title">{{ session.title || session.sessionNo }}</span>
          <span class="session-meta">{{ session.currentIntent || '未识别意图' }}</span>
        </button>
      </div>
      <EmptyState v-else text="暂无会话" />
    </aside>

    <main class="message-panel panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">{{ chatStore.currentSession?.title || '咨询工作台' }}</h3>
          <p class="panel-note">{{ chatStore.currentSession?.sessionNo || '新建会话后开始咨询' }}</p>
        </div>
        <StatusTag :value="lastSourceType" />
      </div>

      <div ref="messageScrollRef" class="message-list">
        <div
          v-for="message in chatStore.messages"
          :key="message.id || `${message.role}-${message.seqNo}`"
          class="message"
          :class="[message.role === 'USER' ? 'user' : 'assistant', { pending: message.pending, error: message.error }]"
        >
          <div class="bubble">
            <div class="message-role">
              {{ message.role === 'USER' ? '用户' : '智能客服' }}
              <StatusTag v-if="message.sourceType" :value="message.sourceType" />
            </div>
            <div class="message-content" :class="{ 'thinking-content': message.thinking }">
              <template v-if="message.thinking">
                <span class="thinking-dots" aria-hidden="true">
                  <i></i>
                  <i></i>
                  <i></i>
                </span>
                <span>{{ message.content }}</span>
              </template>
              <template v-else>{{ message.content }}</template>
            </div>
          </div>
        </div>
        <EmptyState v-if="!chatStore.messages.length" text="选择会话或输入问题开始咨询" />
      </div>

      <div class="quick-prompts">
        <el-button v-for="item in demoPrompts" :key="item" size="small" @click="draft = item">
          {{ item }}
        </el-button>
      </div>

      <div class="composer">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入售后问题，例如：这个订单能不能退货？"
          @keydown.ctrl.enter="submit"
        />
        <div class="composer-actions">
          <el-switch v-model="useAi" active-text="AI" inactive-text="兜底" />
          <el-button type="primary" :icon="Promotion" :loading="chatStore.sending" :disabled="chatStore.sending" @click="submit">
            发送
          </el-button>
        </div>
      </div>
    </main>

    <aside class="insight-panel panel">
      <div class="panel-header">
        <h3 class="panel-title">处理洞察</h3>
        <StatusTag :value="chatStore.insight?.ai?.status" />
      </div>
      <div class="panel-body insight-body">
        <section>
          <h4>意图</h4>
          <el-tag v-if="chatStore.insight?.intent" type="primary">
            {{ chatStore.insight.intent.intentCode }} · {{ chatStore.insight.intent.intentName }}
          </el-tag>
          <span v-else class="muted">暂无</span>
        </section>

        <section>
          <h4>上下文承接</h4>
          <el-descriptions v-if="chatStore.insight?.context" :column="1" size="small" border>
            <el-descriptions-item label="追问">{{ chatStore.insight.context.followUp ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="上轮意图">{{ chatStore.insight.context.inheritedIntent || '-' }}</el-descriptions-item>
            <el-descriptions-item label="本轮意图">{{ chatStore.insight.context.resolvedIntent || '-' }}</el-descriptions-item>
            <el-descriptions-item label="摘要">{{ chatStore.insight.context.summary }}</el-descriptions-item>
          </el-descriptions>
          <p v-else class="muted">暂无上下文</p>
        </section>

        <section>
          <h4>订单上下文</h4>
          <el-descriptions v-if="chatStore.insight?.orderContext?.hasOrder" :column="1" size="small" border>
            <el-descriptions-item label="订单号">{{ chatStore.insight.orderContext.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="商品">{{ chatStore.insight.orderContext.productName }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">{{ chatStore.insight.orderContext.orderStatus }}</el-descriptions-item>
            <el-descriptions-item label="售后状态">{{ chatStore.insight.orderContext.afterSaleStatus }}</el-descriptions-item>
            <el-descriptions-item label="签收天数">{{ chatStore.insight.orderContext.signedDays }}</el-descriptions-item>
          </el-descriptions>
          <p v-else class="muted">暂未绑定订单</p>
        </section>

        <section>
          <h4>知识命中</h4>
          <div v-if="chatStore.insight?.knowledgeHits?.length" class="hit-list">
            <div v-for="doc in chatStore.insight.knowledgeHits" :key="doc.id" class="hit-item">
              <strong>{{ doc.title }}</strong>
              <p>{{ doc.contentPreview || doc.answer || doc.content }}</p>
            </div>
          </div>
          <p v-else class="muted">暂无命中</p>
        </section>

        <section>
          <h4>人工转接</h4>
          <TicketPanel
            :ticket="chatStore.insight?.ticket"
            :session-id="chatStore.currentSessionId"
            :order-no="orderNo"
            :last-question="chatStore.lastUserQuestion"
            :intent-code="chatStore.insight?.intent?.intentCode"
            @created="chatStore.mergeTicket"
          />
        </section>

        <section>
          <h4>回答过程</h4>
          <ProcessFlowPanel :traces="chatStore.insight?.trace || []" />
        </section>
      </div>
    </aside>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Promotion, Search } from '@element-plus/icons-vue'
import ProcessFlowPanel from '../components/chat/ProcessFlowPanel.vue'
import TicketPanel from '../components/chat/TicketPanel.vue'
import EmptyState from '../components/common/EmptyState.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { useChatStore } from '../stores/chatStore'

const chatStore = useChatStore()
const orderNo = ref('DD202604290001')
const draft = ref('这个订单能不能退货？')
const useAi = ref(true)
const messageScrollRef = ref(null)

const demoPrompts = ['这个订单能不能退货？', '退货后多久能退款？', '物流一直不更新怎么办？', '商家一直不处理可以投诉吗？']
const lastSourceType = computed(() => chatStore.insight?.assistantMessage?.sourceType || (chatStore.sending ? 'THINKING' : 'FALLBACK'))

async function newSession() {
  await chatStore.startSession({ orderNo: orderNo.value, title: '前端咨询会话' })
  await scrollBottom()
}

async function selectSession(id) {
  await chatStore.loadSession(id)
  chatStore.insight = {}
  await scrollBottom()
}

async function submit() {
  if (chatStore.sending) {
    return
  }
  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning('请输入咨询内容')
    return
  }
  draft.value = ''
  const task = chatStore.ask(content, orderNo.value, useAi.value)
  await scrollBottom()
  try {
    await task
  } catch (error) {
    // request.js already shows the concrete network or backend error.
  }
  await scrollBottom()
}

async function scrollBottom() {
  await nextTick()
  if (messageScrollRef.value) {
    messageScrollRef.value.scrollTop = messageScrollRef.value.scrollHeight
  }
}

onMounted(async () => {
  await chatStore.loadSessions()
  if (chatStore.sessions.length) {
    await selectSession(chatStore.sessions[0].id)
  }
})
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 260px minmax(420px, 1fr) 360px;
  gap: 12px;
  height: calc(100vh - var(--header-height));
  padding: 12px;
}

.session-panel,
.message-panel,
.insight-panel {
  min-height: 0;
  overflow: hidden;
}

.panel-note {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.session-create {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--line-soft);
}

.session-list {
  height: calc(100% - 112px);
  overflow: auto;
  padding: 8px;
}

.session-item {
  display: block;
  width: 100%;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--text);
  text-align: left;
  cursor: pointer;
}

.session-item.active {
  border-color: #bfdbfe;
  background: var(--brand-soft);
}

.session-title,
.session-meta {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.message-panel {
  display: grid;
  grid-template-rows: auto 1fr auto auto;
}

.message-list {
  min-height: 0;
  overflow: auto;
  padding: 18px;
  background: #f8fafc;
}

.message {
  display: flex;
  margin-bottom: 12px;
}

.message.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 76%;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: white;
}

.message.user .bubble {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.message.pending .bubble {
  border-style: dashed;
}

.message.error .bubble {
  border-color: #fecaca;
  background: #fef2f2;
}

.message-role {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.message-content {
  line-height: 1.75;
  white-space: pre-wrap;
}

.thinking-content {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
}

.thinking-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 16px;
}

.thinking-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #409eff;
  animation: thinking-bounce 1s infinite ease-in-out;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes thinking-bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.35;
  }

  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--line-soft);
}

.composer {
  padding: 12px;
  border-top: 1px solid var(--line-soft);
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.insight-body {
  height: calc(100% - 49px);
  overflow: auto;
}

.insight-body section + section {
  margin-top: 18px;
}

.insight-body h4 {
  margin: 0 0 8px;
  font-size: 14px;
}

.hit-list {
  display: grid;
  gap: 8px;
}

.hit-item {
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: var(--surface-soft);
}

.hit-item p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .chat-page {
    grid-template-columns: 240px 1fr;
    height: auto;
  }

  .insight-panel {
    grid-column: 1 / -1;
    min-height: 360px;
  }
}

@media (max-width: 820px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .bubble {
    max-width: 92%;
  }
}
</style>
