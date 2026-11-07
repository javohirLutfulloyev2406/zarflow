import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  return {
    plugins: [react(), tailwindcss()],
    server: {
      proxy: {
        "/api": "http://localhost:8080",
        "/telegram": "http://localhost:8080",
      },
      allowedHosts: [env.VITE_DEV_ALLOWED_HOST].filter(
        (host): host is string => Boolean(host),
      ),
    },
    test: {
      environment: "jsdom",
      setupFiles: ["./src/test/setup.ts"],
      globals: false,
    },
  };
});
