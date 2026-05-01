import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import 'roboto-fontface/css/roboto/roboto-fontface.css'

import App from './App.vue'
import router from './router'
import { registerPlugins } from './plugins'
import './styles/main.css'

const chunkReloadStorageKey = 'story-weaver:chunk-reload-target'

function resolveChunkErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return ''
}

function isRecoverableChunkError(error: unknown) {
  const message = resolveChunkErrorMessage(error)
  return [
    'Failed to fetch dynamically imported module',
    'Importing a module script failed',
    'Unable to preload CSS',
  ].some((token) => message.includes(token))
}

function recoverFromChunkError(target?: string) {
  if (typeof window === 'undefined') {
    return false
  }

  const nextTarget = target || window.location.pathname + window.location.search + window.location.hash
  const previousTarget = window.sessionStorage.getItem(chunkReloadStorageKey)
  if (previousTarget === nextTarget) {
    window.sessionStorage.removeItem(chunkReloadStorageKey)
    return false
  }

  window.sessionStorage.setItem(chunkReloadStorageKey, nextTarget)
  window.location.replace(nextTarget)
  return true
}

window.addEventListener('vite:preloadError', (event) => {
  const preloadErrorEvent = event as Event & { payload?: unknown }
  if (!isRecoverableChunkError(preloadErrorEvent.payload)) {
    return
  }
  preloadErrorEvent.preventDefault()
  recoverFromChunkError()
})

router.onError((error, to) => {
  if (!isRecoverableChunkError(error)) {
    return
  }
  recoverFromChunkError(to?.fullPath)
})

const app = createApp(App)
const pinia = createPinia()
const vuetify = createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'storyWeaver',
    themes: {
      storyWeaver: {
        dark: false,
        colors: {
          primary: '#1e4d78',
          secondary: '#c66b3d',
          accent: '#2f7d6f',
          surface: '#ffffff',
          background: '#f4efe7',
          info: '#6b7a8f',
          success: '#4d8f5b',
          warning: '#c28d27',
          error: '#c45151',
        },
      },
    },
  },
  defaults: {
    VCard: {
      rounded: 'xl',
      elevation: 2,
    },
    VBtn: {
      rounded: 'lg',
    },
    VTextField: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VTextarea: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VSelect: {
      variant: 'outlined',
      density: 'comfortable',
    },
  },
})

app.use(pinia)
app.use(router)
app.use(vuetify)
registerPlugins()

app.mount('#app')
window.sessionStorage.removeItem(chunkReloadStorageKey)
