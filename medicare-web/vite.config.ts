import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'

export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    viteMockServe({
      mockPath: 'src/mock',
      enable: mode !== 'production' && process.env.VITE_MOCK === 'true',
    }),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
}))
