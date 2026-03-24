const env = import.meta.env

export const appEnv = {
  apiBaseUrl: env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  apiProxyTarget: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
  appName: 'Story Weaver',
}
