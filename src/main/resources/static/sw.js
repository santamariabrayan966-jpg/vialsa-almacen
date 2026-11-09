// Service Worker DentIA - Notificaciones de Citas y Alertas Clínicas
self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  // Passthrough to network
  event.respondWith(fetch(event.request));
});

// Manejo de clics en las notificaciones del sistema
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const data = event.notification.data || {};
  const notifId = data.id;
  const urlDestino = data.url || '/interno/citas';

  if (event.action === 'entendido') {
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
      clients.forEach((client) => {
        client.postMessage({ type: 'NOTIFICACION_ENTENDIDA', notificacionId: notifId });
      });
    });
  } else {
    event.waitUntil(
      self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
        for (let i = 0; i < clientList.length; i++) {
          const client = clientList[i];
          if (client.url && 'focus' in client) {
            client.focus();
            if (urlDestino && client.navigate) {
              return client.navigate(urlDestino);
            }
            return;
          }
        }
        if (self.clients.openWindow) {
          return self.clients.openWindow(urlDestino);
        }
      })
    );
  }
});
