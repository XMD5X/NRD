import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Конфигурация Vite. В dev-режиме проксируем /api на backend (localhost:8080),
// чтобы не настраивать CORS отдельно. В продакшене /api проксирует nginx (см. nginx/nginx.conf).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
