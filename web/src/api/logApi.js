import request from './request'

export const pageAiCallLogs = params => request.get('/ai-call-logs', { params })
export const pageRetrievalLogs = params => request.get('/retrieval-logs', { params })
