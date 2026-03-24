export const storageKeys = {
  token: 'story-weaver:token',
  user: 'story-weaver:user',
  projectId: 'story-weaver:project-id',
}

export function readStorage<T>(key: string, fallback: T): T {
  const rawValue = localStorage.getItem(key)
  if (!rawValue) {
    return fallback
  }

  try {
    return JSON.parse(rawValue) as T
  } catch {
    return fallback
  }
}

export function writeStorage<T>(key: string, value: T) {
  localStorage.setItem(key, JSON.stringify(value))
}

export function clearStorage(key: string) {
  localStorage.removeItem(key)
}
