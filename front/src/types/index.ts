export interface UserProfile {
  id: number
  username: string
  nickname: string
  email: string
  avatar?: string
  roleCode?: string
  status?: number
}

export interface AuthPublicConfig {
  registrationEnabled: boolean
  maxFailedAttempts: number
  lockMinutes: number
}

export interface ManagedUser {
  id: number
  username: string
  nickname: string
  email?: string
  avatar?: string
  status: number
  roleCode: string
  failedLoginAttempts: number
  locked: boolean
  lockedUntil?: string
  lastLoginAt?: string
  passwordChangedAt?: string
  createTime?: string
  updateTime?: string
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
  worldSettingIds?: number[]
  worldSettingNames?: string[]
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
  requiredCharacterIds?: number[]
  requiredCharacterNames?: string[]
  createTime?: string
  updateTime?: string
}

export interface Character {
  id: number
  projectId: number
  ownerUserId?: number
  name: string
  description?: string
  attributes?: string
  projectRole?: string
  projectIds?: number[]
  projectNames?: string[]
  inventoryItemCount?: number
  equippedItemCount?: number
  rareItemCount?: number
  createTime?: string
  updateTime?: string
}

export interface Item {
  id: number
  projectId: number
  ownerUserId?: number
  name: string
  description?: string
  category: string
  rarity: string
  stackable: boolean
  maxStack?: number
  usable: boolean
  equippable: boolean
  slotType?: string
  itemValue?: number
  weight?: number
  attributesJson?: string
  effectJson?: string
  tags?: string
  sourceType?: string
  createTime?: string
  updateTime?: string
}

export interface CharacterInventoryItem {
  id: number
  projectId: number
  characterId: number
  itemId: number
  quantity: number
  equipped: boolean
  durability?: number
  customName?: string
  notes?: string
  sortOrder?: number
  item: Item
  createTime?: string
  updateTime?: string
}

export interface ItemGenerationRequest {
  category?: string
  count?: number
  prompt?: string
  constraints?: string
  selectedProviderId?: number | null
  selectedModel?: string
}

export interface WorldSetting {
  id: number
  projectId: number
  ownerUserId?: number
  name?: string
  description?: string
  category?: string
  title?: string
  content?: string
  orderNum?: number
  associationCount?: number
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
  entryPoint?: string
}

export interface AIWritingStreamLogItem {
  id: string
  type: string
  stage?: string
  stageStatus?: string
  message?: string
  occurrenceCount?: number
  firstSeenAt?: number
  lastSeenAt?: number
  elapsedSeconds?: number
}

export interface AIWritingStreamEvent {
  type: 'meta' | 'stage' | 'log' | 'chunk' | 'replace' | 'complete' | 'error' | string
  delta?: string
  content?: string
  message?: string
  stage?: string
  stageStatus?: string
  writingType?: string
  selectedProviderId?: number | null
  selectedModel?: string
  maxTokens?: number | null
  record?: AIWritingRecord
}

export interface AIWritingStreamState {
  requestId?: string
  generating: boolean
  content: string
  error: string
  lastRecord: AIWritingRecord | null
  writingType?: string
  selectedProviderId?: number | null
  selectedModel?: string
  maxTokens?: number | null
  logs: AIWritingStreamLogItem[]
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
  directorDecisionId?: number | null
  status?: string
  createTime?: string
}

export interface AIDirectorDecisionRequest {
  chapterId: number
  currentContent?: string
  userInstruction?: string
  writingType?: string
  entryPoint?: string
  sourceType?: string
  forceRefresh?: boolean
  selectedProviderId?: number | null
  selectedModel?: string
}

export interface AIDirectorSelectedModule {
  module: string
  weight?: number | null
  required?: boolean
  topK?: number | null
  fields?: string[]
}

export interface AIDirectorDecisionPack {
  version?: string
  chapterId?: number | null
  projectId?: number | null
  entryPoint?: string
  stage?: string
  writingMode?: string
  targetWordCount?: number | null
  decisionSummary?: string
  selectedModules?: AIDirectorSelectedModule[]
  requiredFacts?: string[]
  prohibitedMoves?: string[]
  writerHints?: string[]
}

export interface AIDirectorToolTrace {
  id?: string
  name?: string
  argumentsJson?: string
  resultJson?: string
}

export interface AIDirectorDecision {
  id: number
  projectId?: number | null
  chapterId: number
  sourceType?: string
  entryPoint?: string
  stage?: string
  writingMode?: string
  targetWordCount?: number | null
  decisionSummary?: string
  decisionPack?: AIDirectorDecisionPack | null
  toolTrace?: AIDirectorToolTrace[] | null
  selectedProviderId?: number | null
  selectedModel?: string
  status?: string
  errorMessage?: string
  createTime?: string
}

export interface AIWritingChatMessage {
  id: number
  role: 'user' | 'assistant' | 'system' | string
  content: string
  segmentNo?: number
  pinnedToBackground?: boolean
  compressed?: boolean
  createTime?: string
}

export interface AIWritingChatSession {
  sessionId?: number
  projectId?: number
  chapterId: number
  activeSegmentNo?: number
  activeWindowChars?: number
  maxWindowChars?: number
  compressedSummary?: string
  messages: AIWritingChatMessage[]
}

export interface AIWritingChatMessageRequest {
  content: string
  selectedProviderId?: number | null
  selectedModel?: string
  entryPoint?: string
}

export interface AIWritingChatStreamEvent {
  type: 'meta' | 'chunk' | 'complete' | 'error' | string
  delta?: string
  message?: string
  selectedProviderId?: number | null
  selectedModel?: string
  session?: AIWritingChatSession
}

export interface MenuItem {
  title: string
  icon: string
  to: string
  subtitle?: string
  adminOnly?: boolean
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

export interface Outline {
  id: number
  projectId: number
  chapterId?: number | null
  title?: string
  summary?: string
  content?: string
  stageGoal?: string
  keyConflict?: string
  turningPoints?: string
  expectedEnding?: string
  focusCharacterIds?: number[]
  focusCharacterNames?: string[]
  relatedPlotIds?: number[]
  relatedPlotTitles?: string[]
  relatedCausalityIds?: number[]
  relatedCausalityNames?: string[]
  chapterTitle?: string
  status?: number
  orderNum?: number
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

export interface ProviderDiscoveryResult {
  success: boolean
  message: string
  models: string[]
  embeddingModels: string[]
}

export interface NameSuggestionResult {
  suggestions: string[]
  providerName?: string
  modelName?: string
}

export interface CharacterAttributeSuggestionResult {
  age?: string
  gender?: string
  identity?: string
  camp?: string
  goal?: string
  background?: string
  appearance?: string
  traits?: string[]
  talents?: string[]
  skills?: string[]
  weaknesses?: string[]
  equipment?: string[]
  tags?: string[]
  relations?: string[]
  notes?: string
  providerName?: string
  modelName?: string
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
