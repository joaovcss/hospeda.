import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// O proxy encaminha /api para o backend Spring (porta 8080). Para o navegador a chamada é de mesma origem,
// então o cabeçalho Origin é removido: assim o CORS do backend não bloqueia o painel aberto por 127.0.0.1 ou IP da rede.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'));
        },
      },
    },
  },
});
