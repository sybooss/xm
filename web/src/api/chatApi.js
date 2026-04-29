import request from './request'

export const pageSessions = params => request.get('/chat-sessions', { params })
export const createSession = data => request.post('/chat-sessions', data)
export const getSession = id => request.get(`/chat-sessions/${id}`)
export const updateSession = (id, data) => request.put(`/chat-sessions/${id}`, data)
export const deleteSession = id => request.delete(`/chat-sessions/${id}`)
export const listMessages = id => request.get(`/chat-sessions/${id}/messages`)
export const sendMessage = (id, data) => request.post(`/chat-sessions/${id}/messages`, data)
export const listTraces = id => request.get(`/chat-sessions/${id}/process-traces`)
