import request from './request'

export const pageProductIssueInsights = params => request.get('/admin/product-issue-insights', { params })
export const getProductIssueInsightSummary = params => request.get('/admin/product-issue-insights/summary', { params })
export const refreshProductIssueInsights = data => request.post('/admin/product-issue-insights/refresh', data)
