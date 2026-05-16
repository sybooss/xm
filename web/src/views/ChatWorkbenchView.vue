<template>
  <section class="chat-page">
    <aside class="session-panel panel">
      <div class="panel-header">
        <h3 class="panel-title">会话</h3>
        <el-button type="primary" :icon="Plus" circle @click="newSession" />
      </div>
      <div class="session-create">
        <el-select v-model="selectedChannel" class="channel-select" placeholder="渠道">
          <el-option
            v-for="channel in channelOptions"
            :key="channel.code"
            :label="channel.name"
            :value="channel.code"
          />
        </el-select>
        <el-input v-model="orderNo" placeholder="订单号" clearable />
        <el-button :icon="Search" @click="newSession">绑定</el-button>
      </div>
      <div class="channel-filter">
        <div class="channel-filter-title">
          <span>渠道筛选</span>
          <small>网页 / 测试台</small>
        </div>
        <el-segmented v-model="channelFilter" :options="channelFilterOptions" @change="loadSessionsByChannel" />
      </div>
      <div v-if="chatStore.sessions.length" class="session-list">
        <div
          v-for="session in chatStore.sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === chatStore.currentSessionId }"
          @click="selectSession(session.id)"
        >
          <button class="session-main" type="button">
            <span class="session-title">{{ session.title || session.sessionNo }}</span>
            <span class="session-meta">{{ channelName(session.channel) }} · {{ session.currentIntent || '未识别意图' }}</span>
          </button>
          <el-button class="delete-session" :icon="Delete" text circle @click.stop="deleteChatSession(session)" />
        </div>
      </div>
      <EmptyState v-else text="暂无会话" />

      <div class="user-info-bar">
        <div class="user-text">
          <strong>{{ authStore.user?.displayName || authStore.user?.username }}</strong>
          <StatusTag :value="authStore.user?.role" />
        </div>
        <div class="user-actions">
          <el-button size="small" :icon="ShoppingBag" @click="openMyOrders">我的订单</el-button>
          <el-button size="small" :icon="SwitchButton" @click="logout">退出</el-button>
        </div>
      </div>
    </aside>

    <main class="message-panel panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">{{ showOrderPanel ? '我的订单' : (chatStore.currentSession?.title || '咨询工作台') }}</h3>
          <p class="panel-note">{{ showOrderPanel ? '选择订单可自动绑定到聊天，已申请售后的订单会避免重复提交' : (chatStore.currentSession?.sessionNo || '新建会话后开始咨询') }}</p>
        </div>
        <div class="header-actions">
          <el-button v-if="showOrderPanel" :icon="ArrowLeft" @click="backToChat">返回聊天</el-button>
          <template v-else>
            <el-button :icon="DocumentChecked" :disabled="!chatStore.currentSessionId" @click="exportEvidenceReport">
              导出证据
            </el-button>
            <StatusTag :value="lastSourceType" />
          </template>
        </div>
      </div>

      <CustomerOrderPanel v-if="showOrderPanel" @select="selectOrderFromPanel" />

      <div v-else ref="messageScrollRef" class="message-list">
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
            <div v-if="message.fileUrl" class="message-image-box">
              <img class="message-image" :src="imageUrl(message.fileUrl)" alt="聊天图片" />
              <span>{{ message.originalFilename || '聊天图片' }}</span>
              <ChatImageRiskPanel v-if="message.imageRisk" :risk="message.imageRisk" compact :show-signals="false" />
            </div>
          </div>
        </div>
        <EmptyState v-if="!chatStore.messages.length" text="选择会话或输入问题开始咨询" />
      </div>

      <div v-if="!showOrderPanel" class="quick-prompts">
        <el-button v-for="item in demoPrompts" :key="item" size="small" @click="draft = item">
          {{ item }}
        </el-button>
      </div>

      <div v-if="!showOrderPanel" class="composer">
        <div v-if="pendingImage.fileUrl" class="composer-image-preview">
          <img :src="imageUrl(pendingImage.fileUrl)" alt="待发送图片" />
          <div>
            <strong>{{ pendingImage.originalFilename || '待发送图片' }}</strong>
            <span>将随本轮消息发送给客服</span>
          </div>
          <el-button size="small" text type="danger" @click="clearPendingImage">移除</el-button>
        </div>
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入售后问题，例如：这个订单能不能退货？"
          @keydown.ctrl.enter="submit"
        />
        <div class="composer-actions">
          <div class="composer-options">
            <input ref="chatImageInputRef" class="hidden-file-input" type="file" accept="image/*" @change="uploadChatImageFile" />
            <el-button :icon="Picture" :loading="imageUploading" :disabled="chatStore.sending || imageUploading" @click="chooseChatImage">
              发图片
            </el-button>
            <el-switch v-model="useAi" active-text="AI" inactive-text="兜底" />
            <el-select
              v-if="authStore.isAdmin"
              v-model="selectedModel"
              class="composer-model-select"
              size="small"
              filterable
              allow-create
              default-first-option
              :reserve-keyword="false"
              :loading="systemStore.modelSwitching"
              @change="changeModel"
            >
              <el-option
                v-for="model in systemStore.modelOptions"
                :key="model"
                :label="model"
                :value="model"
              />
            </el-select>
          </div>
          <el-button type="primary" :icon="Promotion" :loading="chatStore.sending" :disabled="chatStore.sending" @click="submit">
            发送
          </el-button>
        </div>
      </div>
    </main>

    <aside class="insight-panel panel">
      <div class="panel-header insight-header">
        <div>
          <h3 class="panel-title">客服处理面板</h3>
          <p class="panel-note">当前客户、订单、建议和服务记录集中处理</p>
        </div>
        <StatusTag :value="chatStore.insight?.ai?.status" />
      </div>
      <div class="panel-body insight-body">
        <section class="service-summary">
          <article v-for="item in serviceSummaryCards" :key="item.label" class="service-summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.detail }}</p>
          </article>
        </section>

        <el-tabs v-model="activeInsightTab" class="insight-tabs">
          <el-tab-pane label="客户与订单" name="customer">
            <section class="insight-section">
              <h4>当前订单</h4>
              <el-descriptions v-if="chatStore.insight?.orderContext?.hasOrder" :column="1" size="small" border>
                <el-descriptions-item label="订单号">{{ chatStore.insight.orderContext.orderNo }}</el-descriptions-item>
                <el-descriptions-item label="商品">{{ chatStore.insight.orderContext.productName }}</el-descriptions-item>
                <el-descriptions-item label="订单状态">{{ chatStore.insight.orderContext.orderStatus }}</el-descriptions-item>
                <el-descriptions-item label="售后状态">{{ chatStore.insight.orderContext.afterSaleStatus }}</el-descriptions-item>
                <el-descriptions-item label="签收天数">{{ chatStore.insight.orderContext.signedDays }}</el-descriptions-item>
              </el-descriptions>
              <p v-else class="muted">暂未绑定订单，可在左侧输入订单号或打开“我的订单”选择。</p>
            </section>

            <section class="insight-section">
              <h4>上下文承接</h4>
              <el-descriptions v-if="chatStore.insight?.context" :column="1" size="small" border>
                <el-descriptions-item label="追问">{{ chatStore.insight.context.followUp ? '是' : '否' }}</el-descriptions-item>
                <el-descriptions-item label="上轮意图">{{ chatStore.insight.context.inheritedIntent || '-' }}</el-descriptions-item>
                <el-descriptions-item label="本轮意图">{{ chatStore.insight.context.resolvedIntent || '-' }}</el-descriptions-item>
                <el-descriptions-item label="摘要">{{ chatStore.insight.context.summary }}</el-descriptions-item>
              </el-descriptions>
              <p v-else class="muted">暂无上下文，系统会在多轮追问后自动汇总。</p>
            </section>

            <section class="insight-section">
              <ProductInsightPanel :insight="chatStore.insight?.productInsight" />
            </section>
          </el-tab-pane>

          <el-tab-pane label="AI 建议" name="assistant">
            <section class="decision-summary">
              <div class="decision-title">
                <div>
                  <span class="eyebrow">处理建议</span>
                  <h4>{{ decisionSummary.title }}</h4>
                </div>
                <StatusTag :value="decisionSummary.status" />
              </div>
              <p>{{ decisionSummary.detail }}</p>
              <div class="decision-metrics">
                <div>
                  <span>知识依据</span>
                  <strong>{{ decisionSummary.knowledgeCount }}</strong>
                </div>
                <div>
                  <span>流程步骤</span>
                  <strong>{{ decisionSummary.traceCount }}</strong>
                </div>
                <div>
                  <span>转人工</span>
                  <strong>{{ decisionSummary.ticketText }}</strong>
                </div>
              </div>
            </section>

            <section v-if="chatStore.insight?.stream" class="stream-card">
              <h4>实时进度</h4>
              <p>{{ chatStore.insight.stream.message || '正在处理当前咨询' }}</p>
            </section>

            <section class="insight-section">
              <h4>意图</h4>
              <el-tag v-if="chatStore.insight?.intent" type="primary">
                {{ chatStore.insight.intent.intentCode }} · {{ chatStore.insight.intent.intentName }}
              </el-tag>
              <span v-else class="muted">暂无</span>
            </section>

            <section v-if="businessTools.length" class="insight-section">
              <h4>业务工具</h4>
              <div class="tool-list">
                <div v-for="tool in businessTools" :key="tool" class="tool-item">{{ tool }}</div>
              </div>
            </section>

            <section v-if="suggestedQuestions.length" class="insight-section">
              <h4>建议追问</h4>
              <div class="suggestion-list">
                <button v-for="question in suggestedQuestions" :key="question" type="button" @click="draft = question">
                  {{ question }}
                </button>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="知识依据" name="knowledge">
            <section class="insight-section">
              <h4>知识命中</h4>
              <div v-if="chatStore.insight?.knowledgeHits?.length" class="hit-list">
                <div v-for="doc in chatStore.insight.knowledgeHits" :key="doc.id" class="hit-item">
                  <div class="hit-title">
                    <strong>{{ doc.title }}</strong>
                    <span>{{ doc.score ? Number(doc.score).toFixed(2) : '0.80' }}</span>
                  </div>
                  <p>{{ doc.contentPreview || doc.answer || doc.content }}</p>
                  <small>{{ doc.hitReason || '按标题、问题、正文或关键词召回' }}</small>
                </div>
              </div>
              <p v-else class="muted">暂无命中，客服可继续核实订单状态或补充知识库规则。</p>
            </section>
          </el-tab-pane>

          <el-tab-pane label="图片/凭证风险" name="risk">
            <section class="insight-section">
              <ChatImageRiskPanel v-if="chatStore.insight?.imageRisk" :risk="chatStore.insight.imageRisk" />
              <p v-else class="muted">当前会话暂无图片凭证风险。用户发送商品照片后，系统会在这里展示真实性预审和补证建议。</p>
            </section>
          </el-tab-pane>

          <el-tab-pane label="工单与流程" name="flow">
            <section class="insight-section">
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

            <section class="insight-section">
              <h4>回答过程</h4>
              <ProcessFlowPanel :traces="chatStore.insight?.trace || []" />
            </section>
          </el-tab-pane>
        </el-tabs>
      </div>
    </aside>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ArrowLeft, Delete, DocumentChecked, Picture, Plus, Promotion, Search, ShoppingBag, SwitchButton } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import CustomerOrderPanel from '../components/chat/CustomerOrderPanel.vue'
import ChatImageRiskPanel from '../components/chat/ChatImageRiskPanel.vue'
import ProcessFlowPanel from '../components/chat/ProcessFlowPanel.vue'
import ProductInsightPanel from '../components/chat/ProductInsightPanel.vue'
import TicketPanel from '../components/chat/TicketPanel.vue'
import EmptyState from '../components/common/EmptyState.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { useAuthStore } from '../stores/authStore'
import { useChatStore } from '../stores/chatStore'
import { useSystemStore } from '../stores/systemStore'
import { downloadEvidenceReport, uploadChatImage } from '../api/chatApi'

const router = useRouter()
const chatStore = useChatStore()
const authStore = useAuthStore()
const systemStore = useSystemStore()
const orderNo = ref('DD202604290001')
const selectedChannel = ref('WEB')
const channelFilter = ref('ALL')
const draft = ref('这个订单能不能退货？')
const useAi = ref(true)
const messageScrollRef = ref(null)
const chatImageInputRef = ref(null)
const showOrderPanel = ref(false)
const activeInsightTab = ref('customer')
const selectedModel = ref('')
const imageUploading = ref(false)
const pendingImage = ref({})
const maxChatImageSize = 5 * 1024 * 1024

const demoPrompts = ['这个订单能不能退货？', '退货后多久能退款？', '物流一直不更新怎么办？', '商家一直不处理可以投诉吗？']
const channelOptions = [
  { code: 'WEB', name: '网页' },
  { code: 'ADMIN_TEST', name: '测试台' }
]
const channelFilterOptions = [
  { label: '全部渠道', value: 'ALL' },
  ...channelOptions.map(item => ({ label: item.name, value: item.code }))
]
const lastSourceType = computed(() => chatStore.insight?.assistantMessage?.sourceType || (chatStore.sending ? 'THINKING' : 'FALLBACK'))
const businessTools = computed(() => {
  const tools = chatStore.insight?.businessTools?.tools
  if (Array.isArray(tools) && tools.length) {
    return tools
  }
  return ['queryOrderStatus', 'searchAfterSaleKnowledge', 'createServiceTicket']
})
const suggestedQuestions = computed(() => {
  const questions = chatStore.insight?.suggestedQuestions
  if (Array.isArray(questions) && questions.length) {
    return questions
  }
  return fallbackQuestions(chatStore.insight?.intent?.intentCode)
})
const serviceSummaryCards = computed(() => {
  const insight = chatStore.insight || {}
  const hasOrder = Boolean(insight.orderContext?.hasOrder)
  const ticketNeeded = Boolean(insight.ticket?.needed || insight.ticket?.id)
  const intentName = insight.intent?.intentName || insight.intent?.intentCode || '待识别'
  const source = insight.assistantMessage?.sourceType === 'AI_ENHANCED' ? 'AI 建议' : (chatStore.sending ? '处理中' : '本地兜底')
  return [
    {
      label: '当前客户',
      value: authStore.user?.displayName || authStore.user?.username || '-',
      detail: authStore.isAdmin ? '客服侧接待视角' : '顾客自助咨询'
    },
    {
      label: '当前订单',
      value: hasOrder ? insight.orderContext.orderNo : (orderNo.value || '未绑定'),
      detail: hasOrder ? (insight.orderContext.afterSaleStatus || '订单已关联') : '先绑定订单再处理售后'
    },
    {
      label: '处理建议',
      value: intentName,
      detail: source
    },
    {
      label: '下一步动作',
      value: ticketNeeded ? '转人工' : (insight.intent ? '按建议回复' : '等待问题'),
      detail: ticketNeeded ? '已进入工单处理' : '结合知识依据确认'
    }
  ]
})
const decisionSummary = computed(() => {
  const insight = chatStore.insight || {}
  if (chatStore.sending) {
    return {
      title: '正在生成处理方案',
      detail: insight.stream?.message || '系统正在解析上下文、检索知识库并生成回复。',
      status: 'THINKING',
      knowledgeCount: insight.knowledgeHits?.length || 0,
      traceCount: insight.trace?.length || 0,
      ticketText: '判定中'
    }
  }
  if (!insight.intent) {
    return {
      title: '等待用户问题',
      detail: '发送售后问题后，这里会汇总意图、知识依据、AI 状态和人工转接判断。',
      status: 'SKIPPED',
      knowledgeCount: 0,
      traceCount: 0,
      ticketText: '-'
    }
  }
  const source = insight.assistantMessage?.sourceType === 'AI_ENHANCED' ? 'AI 增强' : '本地兜底'
  const ticketNeeded = Boolean(insight.ticket?.needed || insight.ticket?.id)
  const orderText = insight.orderContext?.hasOrder ? `已绑定订单 ${insight.orderContext.orderNo}` : '未绑定订单'
  return {
    title: `${source} · ${insight.intent.intentName || insight.intent.intentCode}`,
    detail: `${orderText}，命中 ${insight.knowledgeHits?.length || 0} 条知识依据，${ticketNeeded ? '已进入人工转接链路' : '可由智能客服直接处理'}。`,
    status: insight.ai?.status || insight.assistantMessage?.sourceType || 'SUCCESS',
    knowledgeCount: insight.knowledgeHits?.length || 0,
    traceCount: insight.trace?.length || 0,
    ticketText: ticketNeeded ? '需要' : '无需'
  }
})

watch(
  () => systemStore.selectedModelName,
  value => {
    selectedModel.value = value || ''
  },
  { immediate: true }
)

async function newSession() {
  pendingImage.value = {}
  await chatStore.startSession({
    orderNo: orderNo.value,
    channel: selectedChannel.value,
    title: `${channelName(selectedChannel.value)}咨询会话`
  })
  channelFilter.value = 'ALL'
  await scrollBottom()
}

async function selectSession(id) {
  showOrderPanel.value = false
  pendingImage.value = {}
  await chatStore.loadSession(id)
  chatStore.insight = {}
  await scrollBottom()
}

async function deleteChatSession(session) {
  await ElMessageBox.confirm(`确定删除会话“${session.title || session.sessionNo}”吗？`, '删除会话', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await chatStore.removeSession(session.id)
  ElMessage.success('会话已删除')
  await scrollBottom()
}

async function submit() {
  if (chatStore.sending) {
    return
  }
  const content = draft.value.trim()
  if (!content && !pendingImage.value.fileUrl) {
    ElMessage.warning('请输入咨询内容或选择图片')
    return
  }
  draft.value = ''
  const attachment = pendingImage.value.fileUrl ? { ...pendingImage.value } : null
  pendingImage.value = {}
  const messageContent = content || (attachment ? `这是我拍的商品问题照片：${attachment.originalFilename || '聊天图片'}` : '')
  const task = chatStore.ask(messageContent, orderNo.value, useAi.value, attachment)
  await scrollBottom()
  try {
    await task
  } catch (error) {
    // request.js already shows the concrete network or backend error.
  }
  await scrollBottom()
}

function chooseChatImage() {
  chatImageInputRef.value?.click()
}

async function uploadChatImageFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }
  if (!file.type?.startsWith('image/')) {
    ElMessage.warning('只能发送图片')
    return
  }
  if (file.size > maxChatImageSize) {
    ElMessage.warning('图片不能超过 5MB，请压缩后再发送')
    return
  }
  imageUploading.value = true
  try {
    if (!chatStore.currentSessionId) {
      await chatStore.startSession({
        orderNo: orderNo.value,
        channel: selectedChannel.value,
        title: `${channelName(selectedChannel.value)}咨询会话`
      })
    }
    const uploaded = await uploadChatImage(chatStore.currentSessionId, file)
    pendingImage.value = uploaded
    if (!draft.value.trim()) {
      draft.value = `这是我拍的商品问题照片：${uploaded.originalFilename}`
    }
    ElMessage.success('图片已添加到聊天消息')
  } finally {
    imageUploading.value = false
  }
}

function clearPendingImage() {
  pendingImage.value = {}
}

function imageUrl(fileUrl) {
  if (!fileUrl) {
    return ''
  }
  if (/^https?:\/\//i.test(fileUrl)) {
    return fileUrl
  }
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
  if (fileUrl.startsWith('/uploads/')) {
    return `${apiBase}${fileUrl}`
  }
  return fileUrl
}

function openMyOrders() {
  showOrderPanel.value = true
}

function backToChat() {
  showOrderPanel.value = false
}

function selectOrderFromPanel(row) {
  orderNo.value = row.orderNo
  showOrderPanel.value = false
  ElMessage.success(`已绑定订单：${row.orderNo}`)
  scrollBottom()
}

async function exportEvidenceReport() {
  if (!chatStore.currentSessionId) {
    ElMessage.warning('请先选择会话')
    return
  }
  try {
    const blob = await downloadEvidenceReport(chatStore.currentSessionId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `session-${chatStore.currentSessionId}-evidence.md`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('证据报告已导出')
  } catch (error) {
    ElMessage.error(error.message || '证据报告导出失败')
  }
}

async function loadSessionsByChannel() {
  const params = channelFilter.value === 'ALL' ? {} : { channel: channelFilter.value }
  await chatStore.loadSessions(params)
  if (chatStore.sessions.length && !chatStore.sessions.some(item => item.id === chatStore.currentSessionId)) {
    await selectSession(chatStore.sessions[0].id)
  }
}

function channelName(code) {
  return channelOptions.find(item => item.code === code)?.name || '网页'
}

async function changeModel(modelName) {
  if (!modelName) return
  try {
    await systemStore.changeModel(modelName)
    ElMessage.success(`已切换模型：${modelName}`)
  } catch (error) {
    selectedModel.value = systemStore.selectedModelName
  }
}

async function logout() {
  await authStore.logout()
  await router.replace('/login')
}

function fallbackQuestions(intentCode) {
  const presets = {
    RETURN_APPLY: ['退货后多久能退款？', '退货需要自己承担运费吗？', '退货需要哪些照片？'],
    EXCHANGE_APPLY: ['换货需要多久？', '换货可以改成退货吗？', '需要上传什么凭证？'],
    REFUND_PROGRESS: ['退款会退到哪里？', '退款失败怎么办？', '能不能催一下退款？'],
    LOGISTICS_QUERY: ['物流不更新可以投诉吗？', '包裹丢失怎么办？', '需要转人工核实吗？'],
    COMPLAINT_TRANSFER: ['人工客服多久处理？', '还需要补充哪些材料？']
  }
  return presets[intentCode] || ['能不能帮我查订单？', '我想转人工客服']
}

async function scrollBottom() {
  await nextTick()
  if (messageScrollRef.value) {
    messageScrollRef.value.scrollTop = messageScrollRef.value.scrollHeight
  }
}

onMounted(async () => {
  if (authStore.isAdmin) {
    systemStore.loadStatus().catch(() => {})
  }
  await loadSessionsByChannel()
  if (chatStore.sessions.length) {
    await selectSession(chatStore.sessions[0].id)
  }
})
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 260px minmax(420px, 1fr) minmax(420px, 460px);
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

.session-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
}

.panel-note {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.session-create {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--line-soft);
}

.channel-select {
  width: 96px;
  flex: 0 0 auto;
}

.channel-filter {
  display: grid;
  gap: 8px;
  padding: 0 12px 10px;
  border-bottom: 1px solid var(--line-soft);
}

.channel-filter-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #1d1d1f;
  font-size: 12px;
  font-weight: 700;
}

.channel-filter-title small {
  min-width: 0;
  overflow: hidden;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.channel-filter :deep(.el-segmented) {
  width: 100%;
}

.session-list {
  min-height: 0;
  overflow: auto;
  padding: 8px;
}

.session-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  align-items: center;
  gap: 4px;
  width: 100%;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--text);
  text-align: left;
  cursor: pointer;
}

.session-main {
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.session-item.active {
  border-color: #bfdbfe;
  background: var(--brand-soft);
}

.delete-session {
  opacity: 0;
}

.session-item:hover .delete-session,
.session-item.active .delete-session {
  opacity: 1;
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

.user-info-bar {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--line-soft);
  background: white;
}

.user-text {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.user-text strong {
  overflow: hidden;
  color: var(--text);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.message-panel {
  display: grid;
  grid-template-rows: auto 1fr auto auto;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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

.message-image-box {
  display: grid;
  gap: 6px;
  margin-top: 10px;
}

.message-image {
  display: block;
  width: min(260px, 100%);
  max-height: 220px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  object-fit: contain;
  background: #fff;
}

.message-image-box span {
  color: var(--text-muted);
  font-size: 12px;
  word-break: break-all;
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

.composer-image-preview {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding: 8px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: #f8fafc;
}

.composer-image-preview img {
  width: 72px;
  height: 54px;
  border-radius: 4px;
  object-fit: cover;
  background: white;
}

.composer-image-preview strong,
.composer-image-preview span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.composer-image-preview strong {
  font-size: 13px;
}

.composer-image-preview span {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 12px;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.composer-options {
  display: flex;
  align-items: center;
  gap: 10px;
}

.composer-model-select {
  width: 172px;
}

.hidden-file-input {
  display: none;
}

.insight-header {
  align-items: flex-start;
}

.insight-header > div {
  min-width: 0;
}

.insight-body {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  height: calc(100% - 49px);
  overflow: hidden;
}

.service-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.service-summary-card {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #fff;
}

.service-summary-card span,
.service-summary-card p {
  color: var(--text-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.service-summary-card strong {
  display: block;
  margin-top: 4px;
  color: #1d1d1f;
  font-size: 14px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.service-summary-card p {
  margin: 4px 0 0;
  line-height: 1.45;
}

.insight-tabs {
  min-height: 0;
  overflow: hidden;
}

.insight-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
}

.insight-tabs :deep(.el-tabs__nav-wrap) {
  min-width: max-content;
}

.insight-tabs :deep(.el-tabs__nav-scroll) {
  overflow: visible;
}

.insight-tabs :deep(.el-tabs__item) {
  flex: 0 0 auto;
  min-width: max-content;
  padding: 0 16px;
}

.insight-tabs :deep(.el-tabs__content) {
  height: calc(100% - 48px);
  overflow: auto;
  padding-right: 2px;
}

.insight-tabs :deep(.el-tab-pane) {
  min-height: 100%;
  min-width: 0;
}

.insight-section {
  min-width: 0;
}

.insight-section :deep(.el-descriptions__cell),
.insight-section :deep(.el-descriptions__content),
.insight-section :deep(.el-descriptions__label) {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.insight-section + .insight-section,
.stream-card + .insight-section,
.decision-summary + .stream-card,
.decision-summary + .insight-section {
  margin-top: 14px;
}

.insight-section h4,
.stream-card h4,
.insight-body h4 {
  margin: 0 0 8px;
  font-size: 14px;
}

.decision-summary {
  padding: 14px;
  border: 1px solid rgb(37 99 235 / 18%);
  border-radius: var(--radius);
  background: linear-gradient(180deg, #f8fbff, #fff);
}

.decision-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.decision-title > div {
  min-width: 0;
}

.decision-title h4 {
  overflow-wrap: anywhere;
}

.eyebrow {
  display: block;
  margin-bottom: 4px;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.decision-summary p,
.stream-card p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.decision-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.decision-metrics div {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: #fff;
}

.decision-metrics span,
.decision-metrics strong {
  display: block;
}

.decision-metrics span {
  color: var(--text-muted);
  font-size: 12px;
}

.decision-metrics strong {
  margin-top: 5px;
  font-size: 18px;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.stream-card {
  padding: 12px;
  border: 1px dashed rgb(37 99 235 / 26%);
  border-radius: var(--radius);
  background: var(--brand-soft);
}

.hit-list {
  display: grid;
  gap: 8px;
}

.hit-item {
  min-width: 0;
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
  overflow-wrap: anywhere;
}

.hit-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.hit-title strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.hit-title span {
  flex: 0 0 auto;
  padding: 2px 6px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  font-weight: 700;
}

.hit-item small {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.tool-list,
.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.tool-item,
.suggestion-list button {
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: #fff;
  color: var(--text);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.tool-item {
  padding: 6px 10px;
}

.suggestion-list button {
  max-width: 100%;
  padding: 7px 10px;
  cursor: pointer;
}

.suggestion-list button:hover {
  border-color: #bfdbfe;
  background: var(--brand-soft);
  color: var(--brand);
}

@media (max-width: 1320px) {
  .chat-page {
    grid-template-columns: 240px 1fr;
    height: auto;
  }

  .insight-panel {
    grid-column: 1 / -1;
    min-height: 360px;
  }

  .insight-body {
    max-height: 640px;
  }
}

@media (max-width: 960px) {
  .service-summary {
    grid-template-columns: 1fr;
  }

  .decision-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .bubble {
    max-width: 92%;
  }

  .service-summary {
    grid-template-columns: 1fr;
  }
}
</style>
