import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [
    uni()
  ],
  server: {
    proxy: {
      '/wx': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
