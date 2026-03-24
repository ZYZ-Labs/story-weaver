import { bootstrapAuth } from '@/stores/auth'

let bootstrapped = false

export function registerPlugins() {
  if (bootstrapped) {
    return
  }

  bootstrapAuth()
  bootstrapped = true
}
