import request from './request'

const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'

export const pageSessions = params => request.get('/chat-sessions', { params })
export const createSession = data => request.post('/chat-sessions', data)
export const getSession = id => request.get(`/chat-sessions/${id}`)
export const updateSession = (id, data) => request.put(`/chat-sessions/${id}`, data)
export const deleteSession = id => request.delete(`/chat-sessions/${id}`)
export const listMessages = id => request.get(`/chat-sessions/${id}/messages`)
export const sendMessage = (id, data) => request.post(`/chat-sessions/${id}/messages`, data)
export async function sendMessageStream(id, data, handlers = {}) {
  const token = localStorage.getItem('returns_assistant_token')
  const response = await fetch(`${apiBase}/chat-sessions/${id}/message-stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data)
  })
  if (!response.ok || !response.body) {
    throw new Error(`消息流请求失败：HTTP ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let finalData = null

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split('\n\n')
    buffer = chunks.pop() || ''
    for (const chunk of chunks) {
      const event = parseSseEvent(chunk)
      if (!event) continue
      if (event.name === 'progress') {
        handlers.onProgress?.(event.data)
      } else if (event.name === 'final') {
        finalData = event.data
        handlers.onFinal?.(event.data)
      } else if (event.name === 'error') {
        throw new Error(event.data?.message || '消息流处理失败')
      }
    }
  }

  if (!finalData) {
    throw new Error('消息流未返回最终结果')
  }
  return finalData
}

function parseSseEvent(chunk) {
  const lines = chunk.split('\n').filter(Boolean)
  let name = 'message'
  let dataText = ''
  for (const line of lines) {
    if (line.startsWith('event:')) {
      name = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataText += line.slice(5).trim()
    }
  }
  if (!dataText) {
    return null
  }
  try {
    return { name, data: JSON.parse(dataText) }
  } catch {
    return { name, data: dataText }
  }
}
export const listTraces = id => request.get(`/chat-sessions/${id}/process-traces`)
