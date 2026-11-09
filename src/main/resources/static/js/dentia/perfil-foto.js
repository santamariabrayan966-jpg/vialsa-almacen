(() => {
    const input = document.getElementById('fotoFileInput');
    const image = document.getElementById('perfilFotoPreview');
    const placeholder = document.getElementById('perfilFotoPlaceholder');
    const remove = document.getElementById('removeProfilePhoto');
    const status = document.getElementById('profilePhotoStatus');
    const camera = document.querySelector('.avatar-camera-btn');
    const csrf = document.getElementById('dentiaCsrf')?.value;
    if (!input || !image || !placeholder || !remove || !status) return;

    async function send(url, body) {
        const headers = csrf ? { 'X-CSRF-TOKEN': csrf } : {};
        const response = await fetch(url, { method: 'POST', headers, body });
        if (response.redirected) throw new Error('La sesión ha expirado. Vuelve a iniciar sesión.');
        const result = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(result.error || 'No se pudo actualizar la foto.');
        return result;
    }

    function busy(value) {
        input.disabled = value;
        remove.disabled = value;
        camera?.setAttribute('aria-disabled', String(value));
    }

    input.addEventListener('change', async () => {
        const file = input.files?.[0];
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) {
            status.textContent = 'La imagen no puede superar 5 MB.';
            input.value = '';
            return;
        }
        const form = new FormData();
        form.append('fotoFile', file);
        busy(true);
        status.textContent = 'Guardando foto…';
        try {
            const result = await send('/interno/perfil/foto', form);
            image.src = result.url;
            image.classList.remove('d-none');
            placeholder.classList.add('d-none');
            remove.classList.remove('d-none');
            window.dispatchEvent(new CustomEvent('dentia-profile-photo', { detail: result.url }));
            try { localStorage.setItem('dentia-profile-photo-updated', String(Date.now())); } catch (_) {}
            status.textContent = 'Foto actualizada.';
        } catch (error) {
            status.textContent = error.message;
        } finally {
            input.value = '';
            busy(false);
        }
    });

    remove.addEventListener('click', async () => {
        busy(true);
        status.textContent = 'Eliminando foto…';
        try {
            await send('/interno/perfil/foto/eliminar');
            image.classList.add('d-none');
            image.removeAttribute('src');
            placeholder.classList.remove('d-none');
            remove.classList.add('d-none');
            window.dispatchEvent(new CustomEvent('dentia-profile-photo', { detail: '' }));
            try { localStorage.setItem('dentia-profile-photo-updated', String(Date.now())); } catch (_) {}
            status.textContent = 'Foto eliminada.';
        } catch (error) {
            status.textContent = error.message;
        } finally {
            busy(false);
        }
    });
})();
