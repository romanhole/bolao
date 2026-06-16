// Service Worker básico para PWA Pass-through
// Suficiente para o navegador reconhecer o app como instalável

self.addEventListener('install', event => {
    // Força a ativação imediata sem esperar
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    // Garante que a aba atual assuma o controle imediatamente
    event.waitUntil(clients.claim());
});

self.addEventListener('fetch', event => {
    // Pass-through: apenas repassa as requisições de rede
    // O cache pesado será feito pelo próprio compose/ktor se necessário
    event.respondWith(fetch(event.request));
});
