import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const noStoreHeaders = {
  'Cache-Control': 'no-store, no-cache, must-revalidate, max-age=0',
  Pragma: 'no-cache',
  'X-Robots-Tag': 'noindex, nofollow',
}

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    headers: noStoreHeaders,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    headers: noStoreHeaders,
  },
})
