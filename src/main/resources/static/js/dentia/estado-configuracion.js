(() => {
    if (!/^\/interno\/(usuarios|roles)\/?$/.test(location.pathname)) return;

    function feedback(form, message, error) {
        let notice = form.parentElement.querySelector('.state-feedback');
        if (!notice) {
            notice = document.createElement('span');
            notice.className = 'state-feedback';
            notice.setAttribute('role', 'status');
            form.parentElement.appendChild(notice);
        }
        notice.classList.toggle('is-error', error);
        notice.textContent = message;
    }

    document.addEventListener('submit', async event => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;
        const action = new URL(form.action, location.href);
        const userMatch = action.pathname.match(/^\/interno\/usuarios\/(\d+)\/estado$/);
        const roleMatch = action.pathname.match(/^\/interno\/roles\/(activar|desactivar)\/(\d+)$/);
        if (!userMatch && !roleMatch) return;
        event.preventDefault();
        const button = form.querySelector('button[type="submit"], button:not([type])');
        if (button?.disabled) return;
        if (button) button.disabled = true;
        feedback(form, 'Actualizando…', false);
        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(form)),
                credentials: 'same-origin'
            });
            if (!response.ok || response.url.includes('/login')) throw new Error('No se pudo actualizar. Comprueba tu sesión.');
            const html = await response.text();
            const page = new DOMParser().parseFromString(html, 'text/html');
            const error = page.querySelector('main .alert-danger, main .alert-warning');
            if (error) throw new Error(error.textContent.trim());
            if (roleMatch && !new URL(response.url).searchParams.has('estadoCambiado')) throw new Error('No se pudo confirmar el cambio.');
            if (userMatch && !page.querySelector('main .alert-success')) throw new Error('No se pudo confirmar el cambio.');

            if (userMatch) {
                const next = form.elements.namedItem('estado').value;
                document.querySelectorAll('form[action]').forEach(other => {
                    if (new URL(other.action, location.href).pathname !== action.pathname) return;
                    other.elements.namedItem('estado').value = next === '1' ? '2' : '1';
                    const control = other.querySelector('button');
                    control.textContent = next === '1' ? 'Desactivar' : 'Activar';
                    control.classList.toggle('btn-outline-secondary', next === '1');
                    control.classList.toggle('btn-outline-success', next !== '1');
                    const card = other.closest('.user-card-item');
                    const row = other.closest('tr');
                    const badge = card?.querySelector('.card-header-clean .badge.rounded-pill') || row?.querySelector('td:nth-child(5) .badge');
                    if (badge) {
                        badge.textContent = next === '1' ? 'Activo' : 'Inactivo';
                        badge.classList.toggle('text-bg-success', next === '1');
                        badge.classList.toggle('text-bg-secondary', next !== '1');
                    }
                });
            } else {
                const active = roleMatch[1] === 'activar';
                form.action = '/interno/roles/' + (active ? 'desactivar/' : 'activar/') + roleMatch[2];
                button.textContent = active ? 'Desactivar' : 'Activar';
                const badge = form.closest('tr')?.querySelector('td[data-label="Estado"] .badge');
                if (badge) {
                    badge.textContent = active ? 'Activo' : 'Inactivo';
                    badge.classList.toggle('text-bg-success', active);
                    badge.classList.toggle('text-bg-secondary', !active);
                }
            }
            feedback(form, 'Estado actualizado.', false);
        } catch (error) {
            feedback(form, error.message || 'No se pudo actualizar el estado.', true);
        } finally {
            if (button) button.disabled = false;
        }
    });
})();
