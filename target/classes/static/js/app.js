// src/main/resources/static/js/app.js

document.addEventListener('DOMContentLoaded', () => {
  // Inicializar tooltips de Bootstrap en toda la app
  const tooltipTriggerList = Array.from(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
  tooltipTriggerList.forEach(el => new bootstrap.Tooltip(el));
});
