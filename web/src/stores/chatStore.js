import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { createSession, getSession, pageSessions, sendMessageStream } from '../api/chatApi'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const total = ref(0)
  const currentSession = ref(null)
  const messages = ref([])
  const insight = ref({})
  const loading = ref(false)
  const sending = ref(false)

  const currentSessionId = computed(() => currentSession.value?.id)
  const lastUserQuestion = computed(() => {
    const userMessages = messages.value.filter(item => item.role === 'USER')
    return userMessages.length ? userMessages[userMessages.length - 1].content : ''
  })

  async function loadSessions(params = {}) {
    loading.value = true
    try {
      const data = await pageSessions({ page: 1, pageSize: 20, ...params })
      sessions.value = data?.rows || []
      total.value = data?.total || 0
      return data
    } finally {
      loading.value = false
    }
  }

  async function startSession(payload = {}) {
    const data = await createSession({
      title: payload.title || '前端咨询会话',
      orderNo: payload.orderNo || undefined,
      channel: 'WEB'
    })
    currentSession.value = data
    await loadSession(data.id)
    await loadSessions()
    return data
  }

  async function loadSession(id) {
    const data = await getSession(id)
    currentSession.value = data
    messages.value = data?.messages || []
    return data
  }

  async function ask(content, orderNo, useAi = true) {
    if (!currentSessionId.value) {
      await startSession({ orderNo })
    }
    sending.value = true
    const stamp = Date.now()
    const tempUserId = `pending-user-${stamp}`
    const tempAssistantId = `pending-assistant-${stamp}`
    const lastSeqNo = messages.value.reduce((max, item) => Math.max(max, Number(item.seqNo) || 0), 0)
    messages.value = [
      ...messages.value,
      {
        id: tempUserId,
        role: 'USER',
        content,
        seqNo: lastSeqNo + 1,
        pending: true
      },
      {
        id: tempAssistantId,
        role: 'ASSISTANT',
        content: '正在分析订单、检索知识库并等待模型回复...',
        sourceType: 'THINKING',
        seqNo: lastSeqNo + 2,
        pending: true,
        thinking: true
      }
    ]
    insight.value = {
      assistantMessage: { sourceType: 'THINKING' },
      ai: { status: 'THINKING' }
    }
    try {
      const data = await sendMessageStream(
        currentSessionId.value,
        { content, orderNo, useAi },
        {
          onProgress: progress => {
            messages.value = messages.value.map(message => {
              if (message.id !== tempAssistantId) {
                return message
              }
              return {
                ...message,
                content: progress?.message || message.content,
                streamStage: progress?.stage || message.streamStage
              }
            })
            insight.value = {
              ...insight.value,
              stream: progress
            }
          }
        }
      )
      const persistedMessages = [data.userMessage, data.assistantMessage].filter(Boolean)
      messages.value = messages.value
        .filter(message => message.id !== tempUserId && message.id !== tempAssistantId)
        .concat(persistedMessages)
      insight.value = data
      await animateAssistant(data.assistantMessage?.id, data.assistantMessage?.content)
      await loadSession(currentSessionId.value)
      return data
    } catch (error) {
      messages.value = messages.value.map(message => {
        if (message.id !== tempAssistantId) {
          return message
        }
        return {
          ...message,
          content: '回复生成失败，请检查后端服务或稍后重试。',
          sourceType: 'FALLBACK',
          thinking: false,
          pending: false,
          error: true
        }
      })
      insight.value = {
        assistantMessage: { sourceType: 'FALLBACK' },
        ai: { status: 'FAILED' }
      }
      throw error
    } finally {
      sending.value = false
    }
  }

  function mergeTicket(ticket) {
    insight.value = {
      ...insight.value,
      ticket
    }
  }

  async function animateAssistant(messageId, fullText) {
    if (!messageId || !fullText) {
      return
    }
    const step = Math.max(2, Math.ceil(fullText.length / 36))
    for (let i = 0; i <= fullText.length; i += step) {
      const current = fullText.slice(0, i)
      messages.value = messages.value.map(message => {
        if (message.id !== messageId) {
          return message
        }
        return {
          ...message,
          content: current,
          streaming: i < fullText.length
        }
      })
      await sleep(18)
    }
    messages.value = messages.value.map(message => {
      if (message.id !== messageId) {
        return message
      }
      return {
        ...message,
        content: fullText,
        streaming: false
      }
    })
  }

  function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  return {
    sessions,
    total,
    currentSession,
    messages,
    insight,
    loading,
    sending,
    currentSessionId,
    lastUserQuestion,
    loadSessions,
    startSession,
    loadSession,
    ask,
    mergeTicket
  }
})
