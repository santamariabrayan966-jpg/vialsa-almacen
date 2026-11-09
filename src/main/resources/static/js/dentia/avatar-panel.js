(() => {
    const image = document.querySelector('.topbar-profile-image');
    const placeholder = document.querySelector('.topbar-profile-placeholder');
    if (!image || !placeholder) return;

    function update(url) {
        if (url) {
            image.src = url;
            image.classList.remove('d-none');
            placeholder.classList.add('d-none');
        } else {
            image.removeAttribute('src');
            image.classList.add('d-none');
            placeholder.classList.remove('d-none');
        }
    }

    image.addEventListener('error', () => update(''));
    function refresh() {
        fetch('/interno/perfil', { credentials: 'same-origin' })
            .then(response => response.ok && !response.redirected ? response.text() : '')
            .then(html => {
                if (!html) return;
                const photo = new DOMParser().parseFromString(html, 'text/html').getElementById('perfilFotoPreview');
                update(photo && !photo.classList.contains('d-none') ? photo.getAttribute('src') : '');
            })
            .catch(() => {});
    }
    const current = document.getElementById('perfilFotoPreview');
    if (current) update(current.classList.contains('d-none') ? '' : current.getAttribute('src'));
    else refresh();

    window.addEventListener('dentia-profile-photo', event => update(event.detail || ''));
    window.addEventListener('storage', event => {
        if (event.key === 'dentia-profile-photo-updated') refresh();
    });
})();
