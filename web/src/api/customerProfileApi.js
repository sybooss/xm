import request from './request'

export const getCustomerProfile = userId => request.get(`/admin/customers/${userId}/profile`)
export const listCustomerReviews = userId => request.get(`/admin/customers/${userId}/reviews`)
