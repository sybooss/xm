import request from './request'

export const pageSlaTasks = params => request.get('/admin/sla/tasks', { params })
