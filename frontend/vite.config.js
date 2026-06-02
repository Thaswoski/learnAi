import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  server: {
    proxy: {
      '/api/ppt': {
        target: 'http://localhost:5050',
        changeOrigin: true
      },
      '/api/doc': {
        target: 'http://localhost:5050',
        changeOrigin: true
      },
      '/api/generate': {
        target: 'http://localhost:5050',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:6060',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:6060',
        changeOrigin: true
      }
    }
  }
})
