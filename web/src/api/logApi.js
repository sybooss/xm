import request from './request'

export const getLogDiagnostics = () => request.get('/log-diagnostics')
export const pageAiCallLogs = params => request.get('/ai-call-logs', { params })
export const pageRetrievalLogs = params => request.get('/retrieval-logs', { params })
