import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const configureLocalGateway = (proxy) => {
  proxy.on('proxyReq', (proxyRequest) => {
    // 浏览器访问 Cloudflare 域名时会携带公网 Origin；后端实际只由本机网关访问。
    proxyRequest.setHeader('Origin', 'http://127.0.0.1:5173')
  })
}

const localProxy = {
  '/api': {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
    timeout: 0,
    configure: configureLocalGateway,
  },
  '/files': {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
    configure: configureLocalGateway,
  },
}

const frontendOnlyGuard = {
  name: 'frontend-only-preview-guard',
  configurePreviewServer(server) {
    server.middlewares.use((request, response, next) => {
      if (/^\/(api|files)(\/|\?|$)/i.test(request.url || '')) {
        response.statusCode = 403
        response.setHeader('Content-Type', 'application/json; charset=utf-8')
        response.end(JSON.stringify({ message: 'Public preview does not expose backend services.' }))
        return
      }
      next()
    })
  },
}

export default defineConfig(() => {
  const frontendOnly = process.env.PUBLIC_FRONTEND_ONLY === 'true'

  return {
    plugins: [vue(), ...(frontendOnly ? [frontendOnlyGuard] : [])],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: true,
      allowedHosts: true,
      proxy: localProxy,
    },
    preview: {
      host: true,
      allowedHosts: true,
      ...(frontendOnly ? {} : { proxy: localProxy }),
    },
  }
})
