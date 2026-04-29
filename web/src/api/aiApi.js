import request from './request'

export const testAi = data => request.post('/ai-tests', data)
