import request from './request'

export const pageAdminAfterSales = params => request.get('/admin/after-sales', { params })
export const getAdminAfterSale = id => request.get(`/admin/after-sales/${id}`)
export const approveAfterSale = (id, data) => request.post(`/admin/after-sales/${id}/approve`, data)
export const rejectAfterSale = (id, data) => request.post(`/admin/after-sales/${id}/reject`, data)
export const requestAfterSaleEvidence = (id, data) => request.post(`/admin/after-sales/${id}/request-evidence`, data)
export const createAfterSaleTicket = (id, data) => request.post(`/admin/after-sales/${id}/tickets`, data)
export const listReplyDrafts = id => request.get(`/admin/after-sales/${id}/reply-drafts`)
export const generateReplyDraft = (id, data) => request.post(`/admin/after-sales/${id}/reply-drafts`, data)
export const useReplyDraft = (id, draftId, data) => request.post(`/admin/after-sales/${id}/reply-drafts/${draftId}/use`, data)
export const discardReplyDraft = (id, draftId, data) => request.post(`/admin/after-sales/${id}/reply-drafts/${draftId}/discard`, data)
