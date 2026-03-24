import http from './http'
import type { KnowledgeDocument } from '@/types'

export function getKnowledgeDocuments(projectId: number) {
  return http.get<never, KnowledgeDocument[]>(`/projects/${projectId}/knowledge/documents`)
}

export function getKnowledgeDocument(id: number) {
  return http.get<never, KnowledgeDocument>(`/knowledge/documents/${id}`)
}

export function createKnowledgeDocument(projectId: number, payload: Partial<KnowledgeDocument>) {
  return http.post<never, KnowledgeDocument>(`/projects/${projectId}/knowledge/documents`, payload)
}

export function updateKnowledgeDocument(id: number, payload: Partial<KnowledgeDocument>) {
  return http.put(`/knowledge/documents/${id}`, payload)
}

export function deleteKnowledgeDocument(id: number) {
  return http.delete(`/knowledge/documents/${id}`)
}

export function queryKnowledge(projectId: number, query: string) {
  return http.post<never, KnowledgeDocument[]>(`/projects/${projectId}/rag/query`, { query })
}

export function reindexKnowledge(projectId: number) {
  return http.post<never, { documentCount: number; status: string }>(`/projects/${projectId}/rag/reindex`)
}
