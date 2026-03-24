export interface UserProfile {
  id: number
  username: string
  nickname: string
  email: string
  avatar?: string
}

export interface Project {
  id: number
  name: string
  description?: string
  coverImage?: string
  userId?: number
  status?: number
  genre?: string
  tags?: string
  createTime?: string
  updateTime?: string
}

export interface Chapter {
  id: number
  projectId: number
  title: string
  content?: string
  orderNum?: number
  status?: number
  wordCount?: number
  createTime?: string
  updateTime?: string
}

export interface Character {
  id: number
  projectId: number
  name: string
  description?: string
  attributes?: string
  createTime?: string
  updateTime?: string
}

export interface WorldSetting {
  id: number
  projectId: number
  title?: string
  category?: string
  content?: string
  createTime?: string
  updateTime?: string
}

export interface AIWritingRequest {
  chapterId: number
  currentContent: string
  userInstruction?: string
  writingType: string
  maxTokens?: number
  selectedProviderId?: number | null
  selectedModel?: string
  promptSnapshot?: string
}

export interface AIWritingRecord {
  id: number
  chapterId: number
  originalContent: string
  generatedContent: string
  writingType: string
  userInstruction?: string
  selectedProviderId?: number | null
  selectedModel?: string
  promptSnapshot?: string
  status?: string
  createTime?: string
}

export interface MenuItem {
  title: string
  icon: string
  to: string
  subtitle?: string
}

export interface Plot {
  id: number
  projectId: number
  chapterId?: number | null
  title?: string
  description?: string
  content?: string
  plotType?: number
  sequence?: number
  characters?: string
  locations?: string
  timeline?: string
  conflicts?: string
  resolutions?: string
  tags?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface Causality {
  id: number
  projectId: number
  name?: string
  description?: string
  causeType?: string
  effectType?: string
  causeEntityId?: string
  effectEntityId?: string
  causeEntityType?: string
  effectEntityType?: string
  relationship?: string
  strength?: number
  conditions?: string
  tags?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface AIProvider {
  id: number
  name: string
  providerType: string
  baseUrl?: string
  apiKey?: string
  modelName?: string
  embeddingModel?: string
  temperature?: number
  topP?: number
  maxTokens?: number
  timeoutSeconds?: number
  enabled?: number
  isDefault?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface KnowledgeDocument {
  id: number
  projectId: number
  sourceType?: string
  sourceRefId?: string
  title: string
  contentText?: string
  summary?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export interface SystemConfig {
  id?: number
  configKey: string
  configValue: string
  description?: string
  createTime?: string
  updateTime?: string
}
