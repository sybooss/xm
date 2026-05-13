import request from './request'

export const pageCustomerAfterSales = params => request.get('/customer/after-sales', { params })
export const getCustomerAfterSale = id => request.get(`/customer/after-sales/${id}`)
export const createCustomerAfterSale = data => request.post('/customer/after-sales', data)
export const createAfterSaleDiagnosis = data => request.post('/after-sale-diagnoses', data)
export const getAfterSaleDiagnosis = id => request.get(`/after-sale-diagnoses/${id}`)
export const addCustomerAfterSaleEvidence = (id, data) => request.post(`/customer/after-sales/${id}/evidence`, data)
export const getCustomerAfterSaleReview = id => request.get(`/customer/after-sales/${id}/reviews`)
export const createCustomerAfterSaleReview = (id, data) => request.post(`/customer/after-sales/${id}/reviews`, data)
