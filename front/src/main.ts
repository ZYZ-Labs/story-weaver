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
