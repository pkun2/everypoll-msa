import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/auth': {
        target: 'http://auth-service:8081',
        changeOrigin: true
      },
      '/api/polls': {
        target: 'http://poll-service:8082',
        changeOrigin: true
      },
      '/api/votes': {
        target: 'http://vote-service:8083',
        changeOrigin: true
      },
      // '/api/v1': {
      //   target: 'http://localhost:8002',
      //   changeOrigin: true
      // }
    }
  }
})