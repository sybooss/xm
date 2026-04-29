import request from './request'

export const pageOrders = params => request.get('/orders', { params })
export const getOrder = id => request.get(`/orders/${id}`)
export const getOrderByNo = orderNo => request.get(`/orders/no/${orderNo}`)
export const createOrder = data => request.post('/orders', data)
export const updateOrder = (id, data) => request.put(`/orders/${id}`, data)
export const deleteOrder = id => request.delete(`/orders/${id}`)
export const listOrderAfterSales = id => request.get(`/orders/${id}/after-sale-records`)

export const pageAfterSaleRecords = params => request.get('/after-sale-records', { params })
export const createAfterSaleRecord = data => request.post('/after-sale-records', data)
export const updateAfterSaleRecord = (id, data) => request.put(`/after-sale-records/${id}`, data)
export const deleteAfterSaleRecord = id => request.delete(`/after-sale-records/${id}`)
