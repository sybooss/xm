import request from './request'

export const getOperationInsights = () => request.get('/operation-insights')
export const getFeatureClosures = () => request.get('/feature-closures')
