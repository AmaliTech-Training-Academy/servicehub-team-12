import { defineConfig } from 'vite';
import tailwindcss from "@tailwindcss/vite";
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
const __dirname = path.dirname(fileURLToPath(import.meta.url));
// Removes the index.html Vite emits into static/ —
// Spring Boot + Thymeleaf owns all HTML serving.
function deleteStaticIndexHtml() {
  return {
    name: 'delete-static-index-html',
    closeBundle() {
      const file = path.resolve(__dirname, '../backend/src/main/resources/static/index.html');
      if (fs.existsSync(file)) {
        fs.unlinkSync(file);
      }
    },
  };
}
export default defineConfig({
  plugins: [deleteStaticIndexHtml(), tailwindcss()],
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
    rollupOptions: {
      input: 'src/main.ts',
      output: {
        // Stable, hash-free filenames so Thymeleaf templates can reference them directly.
        entryFileNames: 'assets/main.js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name].[ext]',
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
