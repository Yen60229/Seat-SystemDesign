import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 開發期將 API 轉發到 Spring Boot
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    // 建置產物直接放進 Spring Boot 靜態資源目錄，單一服務即可展示
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
});
