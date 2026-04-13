import http from './http'
import type {
  SummaryWorkflowApplyResult,
  SummaryWorkflowChatMessage,
  SummaryWorkflowChatTurnResult,
  SummaryWorkflowIntent,
  SummaryWorkflowOperatorMode,
  SummaryWorkflowPreview,
  SummaryWorkflowProposal,
  SummaryWorkflowProposalResponse,
  SummaryWorkflowTargetType,
  SummaryWorkflowUnitRef,
} from '@/types'

export function createSummaryWorkflowProposal(payload: {
  targetType: SummaryWorkflowTargetType
  targetSourceId?: number
  projectId: number
  summaryText: string
  intent?: SummaryWorkflowIntent
  operatorMode?: SummaryWorkflowOperatorMode
}) {
  return http.post<never, SummaryWorkflowProposalResponse>('/summary-workflow/proposals', payload)
}

export function createSummaryWorkflowChatTurn(payload: {
  targetType: SummaryWorkflowTargetType
  targetSourceId?: number
  projectId: number
  title?: string
  existingSummary?: string
  currentDraftSummary?: string
  intent?: SummaryWorkflowIntent
  operatorMode?: SummaryWorkflowOperatorMode
  selectedProviderId?: number | null
  selectedModel?: string
  messages: SummaryWorkflowChatMessage[]
}) {
  return http.post<never, SummaryWorkflowChatTurnResult>('/summary-workflow/chat-turns', payload)
}

export function previewSummaryWorkflowProposal(payload: {
  proposalId?: string
  proposal?: SummaryWorkflowProposal
}) {
  return http.post<never, SummaryWorkflowPreview>('/summary-workflow/previews', payload)
}

export function applySummaryWorkflowProposal(payload: {
  proposalId: string
  confirmed?: boolean
  targetRef?: SummaryWorkflowUnitRef
}) {
  return http.post<never, SummaryWorkflowApplyResult>('/summary-workflow/apply', payload)
}
