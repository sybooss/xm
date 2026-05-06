import request from './request'

export const pageCustomerAfterSales = params => request.get('/customer/after-sales', { params })
export const getCustomerAfterSale = id => request.get(`/customer/after-sales/${id}`)
export const createCustomerAfterSale = data => request.post('/customer/after-sales', data)
export const addCustomerAfterSaleEvidence = (id, data) => request.post(`/customer/after-sales/${id}/evidence`, data)
