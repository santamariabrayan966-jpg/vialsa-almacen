document.addEventListener('DOMContentLoaded', () => {
  // Bootstrap tooltips
  const list = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
  list.map(el => new bootstrap.Tooltip(el));
});