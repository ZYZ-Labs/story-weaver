import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

function createManualChunk(id: string) {
  if (!id.includes('node_modules')) {
    return undefined
  }

  if (id.includes('vuetify')) {
    return 'vuetify'
  }

  if (id.includes('vue-router') || id.includes('pinia') || id.includes('/vue/')) {
    return 'vue-core'
  }

  if (id.includes('axios')) {
    return 'network'
  }

  return 'vendor'
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: env.VITE_API_PROXY_TARGET
        ? {
            '/api': {
              target: env.VITE_API_PROXY_TARGET,
              changeOrigin: true,
            },
          }
        : undefined,
    },
    build: {
      sourcemap: mode !== 'production',
      outDir: 'dist',
      rollupOptions: {
        output: {
          manualChunks: createManualChunk,
        },
      },
    },
  }
})
