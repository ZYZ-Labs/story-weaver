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
  summary?: string
  content?: string
  orderNum?: number
  status?: number
  chapterStatus?: string
  wordCount?: number
  outlineId?: number | null
  outlineTitle?: string
  storyBeatIds?: number[]
  storyBeatTitles?: string[]
  prevChapterId?: number | null
  nextChapterId?: number | null
  mainPovCharacterId?: number | null
  mainPovCharacterName?: string
  readingTimeMinutes?: number
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
  identity?: string
  coreGoal?: string
  growthArc?: string
  firstAppearanceChapterId?: number | null
  activeStage?: string
  isRetired?: boolean | number
  attributes?: string
  advancedProfileJson?: string
  projectRole?: string
  roleType?: string
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

export type SummaryWorkflowTargetType = 'CHARACTER' | 'WORLD_SETTING' | 'CHAPTER'
export type SummaryWorkflowIntent = 'CREATE' | 'UPDATE' | 'REFINE' | 'ENRICH'
export type SummaryWorkflowOperatorMode = 'DEFAULT' | 'EXPERT' | 'SYSTEM'

export interface SummaryWorkflowSummaryView {
  displayTitle?: string
  oneLineSummary?: string
  longSummary?: string
  stateSummary?: string
  relationSummary?: string
  changeSummary?: string
  pendingQuestions?: string[]
}

export interface SummaryWorkflowUnitRef {
  unitId: string
  unitKey: string
  unitType: SummaryWorkflowTargetType
}

export interface SummaryWorkflowPatchOperation {
  op: string
  path: string
  value?: string
}

export interface SummaryWorkflowProposal {
  proposalId: string
  targetRef: SummaryWorkflowUnitRef
  projectId: number
  inputIntent: SummaryWorkflowIntent
  operatorMode: SummaryWorkflowOperatorMode
  inputSummary: string
  proposalSummary: string
  affectedFacets: string[]
  riskNotes: string[]
  pendingQuestions: string[]
  requiresConfirmation: boolean
  patch: {
    patchId: string
    targetUnit: SummaryWorkflowUnitRef
    facetType: string
    operations: SummaryWorkflowPatchOperation[]
    summary: string
    status: string
  }
}

export interface SummaryWorkflowPreview {
  beforeSummary: SummaryWorkflowSummaryView
  afterSummary: SummaryWorkflowSummaryView
  changeSummary: string
  affectedFacets: string[]
  requiresConfirmation: boolean
  warnings: string[]
}

export interface SummaryWorkflowProposalResponse {
  proposal: SummaryWorkflowProposal
  preview: SummaryWorkflowPreview
}

export interface SummaryWorkflowApplyResult {
  applied: boolean
  updatedSummary: SummaryWorkflowSummaryView
  updatedUnitRef: SummaryWorkflowUnitRef
  warnings: string[]
}

export interface SummaryWorkflowChatMessage {
  role: 'assistant' | 'user' | 'system' | string
  content: string
}

export interface SummaryWorkflowChatTurnResult {
  assistantMessage: string
  draftSummary: string
  pendingQuestions: string[]
  readyForPreview: boolean
  selectedProviderId?: number | null
  selectedModel?: string
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

export interface AIWritingGenerationTrace {
  readiness?: {
    score?: number
    status?: string
    blockingIssues?: string[]
    warnings?: string[]
    recommendedModules?: string[]
  }
  anchors?: {
    chapterOutlineId?: number | null
    volumeOutlineId?: number | null
    mainPovCharacterId?: number | null
    mainPovCharacterName?: string
    requiredCharacterNames?: string[]
    storyBeatTitles?: string[]
    relatedWorldSettingNames?: string[]
    chapterSummary?: string
    anchorSources?: Record<string, string>
    anchorSummary?: string
  }
  readerReveal?: {
    openingMode?: string
    readerKnownFacts?: string[]
    revealTargets?: string[]
    forbiddenAssumptions?: string[]
  }
  director?: {
    decisionId?: number | null
    status?: string
    mode?: string
    model?: string
    decisionSummary?: string
    selectedAnchorSummary?: string
  }
  summaryTrace?: {
    promptSnapshotPreview?: string
    userInstructionPreview?: string
    chatParticipation?: {
      active?: boolean
      worldFactsCount?: number
      characterConstraintsCount?: number
      plotGuidanceCount?: number
      writingPreferencesCount?: number
      hardConstraintsCount?: number
    }
  }
  creationSuggestions?: StructuredCreationSuggestion[]
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
  generationTrace?: AIWritingGenerationTrace | null
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
  openingMode?: string
  readerRevealGoals?: string[]
  forbiddenReaderAssumptions?: string[]
}

export interface GenerationReadiness {
  score?: number
  status?: string
  blockingIssues?: string[]
  warnings?: string[]
  recommendedModules?: string[]
  resolvedAnchors?: ChapterAnchorBundle
}

export interface ChapterAnchorBundle {
  chapterId?: number
  projectId?: number
  chapterOutlineId?: number | null
  volumeOutlineId?: number | null
  mainPovCharacterId?: number | null
  mainPovCharacterName?: string
  requiredCharacterIds?: number[]
  requiredCharacterNames?: string[]
  storyBeatIds?: number[]
  storyBeatTitles?: string[]
  relatedWorldSettingIds?: number[]
  relatedWorldSettingNames?: string[]
  chapterSummary?: string
  chapterStatus?: string
  anchorSources?: Record<string, string>
}

export interface StructuredCreationSuggestion {
  entityType: string
  operation?: string
  summary?: string
  candidateFields?: Record<string, unknown>
  sourceExcerpt?: string
  sourceChapterId?: number | null
  requiresConfirmation?: boolean
}

export interface StructuredCreationApplyRequest {
  suggestion: StructuredCreationSuggestion
}

export interface StructuredCreationApplyResult {
  entityType: string
  createdId: number
  created: unknown
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
  mode?: string
  errorMessage?: string
  failureReason?: string
  selectedAnchorSummary?: string
  toolCallCount?: number
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
  storyBeatType?: string
  storyFunction?: string
  sequence?: number
  characters?: string
  locations?: string
  timeline?: string
  conflicts?: string
  resolutions?: string
  eventResult?: string
  prevBeatId?: number | null
  nextBeatId?: number | null
  outlinePriority?: number
  tags?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface Outline {
  id: number
  projectId: number
  outlineType?: string
  parentOutlineId?: number | null
  rootOutlineId?: number | null
  chapterId?: number | null
  generatedChapterId?: number | null
  title?: string
  summary?: string
  content?: string
  stageGoal?: string
  keyConflict?: string
  turningPoints?: string
  expectedEnding?: string
  focusCharacterIds?: number[]
  focusCharacterIdList?: number[]
  focusCharacterNames?: string[]
  relatedPlotIds?: number[]
  relatedPlotIdList?: number[]
  relatedPlotTitles?: string[]
  relatedCausalityIds?: number[]
  relatedCausalityIdList?: number[]
  relatedCausalityNames?: string[]
  relatedWorldSettingIds?: number[]
  relatedWorldSettingIdList?: number[]
  relatedWorldSettingNames?: string[]
  children?: Outline[]
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
  causalType?: string
  triggerMode?: string
  payoffStatus?: string
  upstreamCauseIdsJson?: string
  downstreamEffectIdsJson?: string
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
