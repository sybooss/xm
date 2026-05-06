import request from './request'

export const pageAdminAfterSales = params => request.get('/admin/after-sales', { params })
export const getAdminAfterSale = id => request.get(`/admin/after-sales/${id}`)
export const approveAfterSale = (id, data) => request.post(`/admin/after-sales/${id}/approve`, data)
export const rejectAfterSale = (id, data) => request.post(`/admin/after-sales/${id}/reject`, data)
export const requestAfterSaleEvidence = (id, data) => request.post(`/admin/after-sales/${id}/request-evidence`, data)
