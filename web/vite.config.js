import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalized = id.replace(/\\/g, '/')
          if (normalized.includes('node_modules')) {
            if (normalized.includes('@element-plus/icons-vue')) {
              return 'vendor-element-icons'
            }
            if (normalized.includes('element-plus/es/')) {
              return 'vendor-element-plus'
            }
            if (normalized.includes('vue') || normalized.includes('pinia')) {
              return 'vendor-vue'
            }
            return 'vendor'
          }
          if (normalized.includes('/src/views/')) {
            return 'views'
          }
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      }
    }
  }
})
