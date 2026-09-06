import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      "@xwms/ui": path.resolve(__dirname, "../../packages/ui/src"),
      "@xwms/api": path.resolve(__dirname, "../../packages/api/src"),
      "@xwms/shared": path.resolve(__dirname, "../../packages/shared/src"),
    },
  },
  server: {
    port: 3002,
    strictPort: true,
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          react: ["react", "react-dom", "react-router-dom"],
        },
      },
    },
  },
});
