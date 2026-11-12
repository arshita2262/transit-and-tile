import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,
    proxy: {
      // Any request from the React app to /api/* is forwarded to the Spring Boot
      // backend running on localhost:8081.  This avoids CORS issues during local
      // development without needing a permissive CORS policy on the backend.
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
