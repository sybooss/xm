import request from './request'

export const listCategories = params => request.get('/knowledge-categories', { params })
export const createCategory = data => request.post('/knowledge-categories', data)
export const updateCategory = (id, data) => request.put(`/knowledge-categories/${id}`, data)
export const deleteCategory = id => request.delete(`/knowledge-categories/${id}`)

export const pageKnowledgeDocs = params => request.get('/knowledge-docs', { params })
export const createKnowledgeDoc = data => request.post('/knowledge-docs', data)
export const getKnowledgeDoc = id => request.get(`/knowledge-docs/${id}`)
export const updateKnowledgeDoc = (id, data) => request.put(`/knowledge-docs/${id}`, data)
export const deleteKnowledgeDoc = id => request.delete(`/knowledge-docs/${id}`)
export const searchKnowledgeDocs = params => request.get('/knowledge-docs/search', { params })
