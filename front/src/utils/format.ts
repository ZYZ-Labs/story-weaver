export function formatDateTime(value?: string | null) {
  if (!value) {
    return '暂无'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function compactText(value?: string | null, fallback = '暂无内容') {
  return value?.trim() || fallback
}
