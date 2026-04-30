import request from './request'

export const pageTickets = params => request.get('/service-tickets', { params })
export const createTicket = data => request.post('/service-tickets', data)
export const getTicket = id => request.get(`/service-tickets/${id}`)
export const updateTicket = (id, data) => request.put(`/service-tickets/${id}`, data)
export const deleteTicket = id => request.delete(`/service-tickets/${id}`)
export const listSessionTickets = sessionId => request.get(`/chat-sessions/${sessionId}/service-tickets`)
export const createSessionTicket = (sessionId, data) => request.post(`/chat-sessions/${sessionId}/service-tickets`, data)
