import request from './request'

export const getSystemStatus = () => request.get('/system/status')
export const getEnums = () => request.get('/system/enums')
export const getAiModels = () => request.get('/system/ai-models')
export const switchAiModel = modelName => request.put('/system/ai-models/current', { modelName })
