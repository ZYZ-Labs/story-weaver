import type { AIProvider } from '@/types'

export type OutputLengthProfile = {
  min: number
  max: number
  step: number
  recommended: number
  description: string
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function roundToStep(value: number, step: number) {
  return Math.round(value / step) * step
}

function normalizeProviderLimit(provider?: AIProvider | null) {
  const value = provider?.maxTokens
  if (!value || value <= 0) {
    return null
  }
  return value
}

export function resolveOutputLengthProfile(
  provider?: AIProvider | null,
  modelName?: string | null,
): OutputLengthProfile {
  const normalized = `${modelName || provider?.modelName || ''}`.toLowerCase()

  const min = 200
  let max = 1800
  let step = 50
  let recommended = 700
  let description = '标准篇幅'

  if (/0\.5b|0\.6b|1\.5b|1b|2b|nano|tiny/.test(normalized)) {
    max = 900
    recommended = 420
    description = '轻量模型，适合较短段落'
  } else if (/3b|4b|mini|small/.test(normalized)) {
    max = 1400
    recommended = 700
    description = '中小模型，适合短到中等篇幅'
  } else if (/7b|8b|9b|11b/.test(normalized)) {
    max = 2200
    recommended = 1100
    step = 100
    description = '主力模型，适合大多数章节片段'
  } else if (/13b|14b|15b|16b|17b|18b|20b/.test(normalized)) {
    max = 3200
    recommended = 1600
    step = 100
    description = '较大模型，适合更完整的场景生成'
  } else if (/32b|34b|70b|72b|110b|405b|gpt-4\.1|gpt-4o|deepseek-chat/.test(normalized)) {
    max = 4200
    recommended = 2200
    step = 100
    description = '高容量模型，适合长段输出'
  }

  if (/reasoner|r1/.test(normalized)) {
    max = Math.min(max, 2400)
    recommended = Math.min(recommended, 1200)
    description = '推理模型，建议控制输出长度避免等待过久'
  }

  const providerLimit = normalizeProviderLimit(provider)
  if (providerLimit) {
    max = Math.min(max, providerLimit)
  }

  if (max <= min) {
    max = min + step
  }

  recommended = clamp(roundToStep(recommended, step), min, max)

  return {
    min,
    max,
    step,
    recommended,
    description,
  }
}
