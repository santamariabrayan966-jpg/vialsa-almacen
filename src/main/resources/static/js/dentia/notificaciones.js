/**
 * DentIA - Centro de Notificaciones y Alertas Clínicas
 * Campana interactiva, Web Audio API, Service Worker y Notificaciones de Escritorio.
 */
const NotificacionesService = (function () {
    const STORAGE_KEY_LEIDAS = 'dentia_notifs_leidas_';
    const STORAGE_KEY_ACTIVAS = 'dentia_notifs_activas';
    let ultimasAlertas = [];
    let pollingInterval = null;
    let permisosNotif = 'default';
    const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g,
        char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);

    function isActivas() {
        return localStorage.getItem(STORAGE_KEY_ACTIVAS) !== 'false';
    }

    function actualizarEstadoVisualToggle(activa) {
        const toggle = document.getElementById('toggleNotificacionesActivas');
        const icon = document.getElementById('notifsToggleIcon');
        const lblEstado = document.getElementById('lblNotifsEstado');
        const lblSub = document.getElementById('lblNotifsSub');

        if (toggle) toggle.checked = activa;
        if (icon) {
            icon.className = activa ? 'bi bi-bell-fill text-success' : 'bi bi-bell-slash text-secondary';
        }
        if (lblEstado) {
            lblEstado.textContent = activa ? 'Notificaciones activas' : 'Notificaciones en pausa';
        }
        if (lblSub) {
            lblSub.textContent = activa ? 'Sonido y avisos automáticos' : 'Silenciadas (clic para activar)';
        }
    }

    function setActivas(activa, reproducirPrueba = false) {
        localStorage.setItem(STORAGE_KEY_ACTIVAS, activa ? 'true' : 'false');
        actualizarEstadoVisualToggle(activa);

        if (activa) {
            if ('Notification' in window && Notification.permission === 'default') {
                Notification.requestPermission().then(p => { permisosNotif = p; });
            }
            if (reproducirPrueba) {
                reproducirSonidoCampana();
            }
            consultarAlertas(false);
        } else {
            const badgeBell = document.getElementById('notifsBadge');
            if (badgeBell) badgeBell.style.display = 'none';
        }
    }

    // Sonido sutil de campana armónica usando Web Audio API
    function reproducirSonidoCampana() {
        if (!isActivas()) return;
        try {
            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            if (!AudioCtx) return;
            const ctx = new AudioCtx();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();

            osc.type = 'sine';
            osc.frequency.setValueAtTime(880, ctx.currentTime); // Nota A5
            osc.frequency.exponentialRampToValueAtTime(1760, ctx.currentTime + 0.08);
            osc.frequency.exponentialRampToValueAtTime(440, ctx.currentTime + 0.32);

            gain.gain.setValueAtTime(0.2, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.38);

            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.38);
        } catch (e) {
            console.debug('AudioContext bloqueado o no disponible:', e);
        }
    }

    function getHoyString() {
        const d = new Date();
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    }

    function getLeidasHoy() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_LEIDAS + getHoyString());
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];
        }
    }

    function guardarLeida(id) {
        try {
            const leidas = getLeidasHoy();
            if (!leidas.includes(id)) {
                leidas.push(id);
                localStorage.setItem(STORAGE_KEY_LEIDAS + getHoyString(), JSON.stringify(leidas));
            }
        } catch (e) {}
    }

    function marcarTodasLeidas() {
        try {
            const leidas = ultimasAlertas.map(a => a.id);
            localStorage.setItem(STORAGE_KEY_LEIDAS + getHoyString(), JSON.stringify(leidas));
            renderDropdown([]);
            actualizarBadges(0, 0, 0, 0);
        } catch (e) {}
    }

    async function consultarAlertas(reproducirSonido = false) {
        try {
            const res = await fetch('/api/notificaciones/pendientes');
            if (!res.ok) return;

            const data = await res.json();
            const items = data.items || [];
            ultimasAlertas = items;

            const leidas = getLeidasHoy();
            const noLeidas = items.filter(a => !leidas.includes(a.id));

            const totalNoLeidas = noLeidas.length;
            const noAsistioCount = noLeidas.filter(a => a.tipo === 'NO_ASISTIO').length;
            const hoyCount = noLeidas.filter(a => a.tipo === 'HOY').length;
            const mananaCount = noLeidas.filter(a => a.tipo === 'MANANA').length;

            if (isActivas()) {
                actualizarBadges(totalNoLeidas, noAsistioCount, hoyCount, mananaCount);
            } else {
                actualizarBadges(0, 0, 0, 0);
            }
            renderDropdown(noLeidas);

            // Si hay nuevas alertas no leídas y las notificaciones están activadas
            if (isActivas() && reproducirSonido && totalNoLeidas > 0) {
                reproducirSonidoCampana();
                if (document.hidden && Notification.permission === 'granted') {
                    const primera = noLeidas[0];
                    new Notification('🦷 DentIA: ' + primera.titulo, {
                        body: primera.mensaje,
                        icon: '/favicon.ico',
                        tag: primera.id
                    });
                }
            }
        } catch (e) {
            console.debug('[DentIA Notificaciones] Error consultando alertas:', e);
        }
    }

    function actualizarBadges(total, noAsistio, hoy, manana) {
        const badgeBell = document.getElementById('notifsBadge');
        const countDropdown = document.getElementById('notifsDropdownCount');

        if (badgeBell) {
            if (total > 0) {
                badgeBell.textContent = total > 99 ? '99+' : total;
                badgeBell.style.display = 'inline-block';
                badgeBell.className = noAsistio > 0
                    ? 'position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-white pulse-badge'
                    : 'position-absolute top-0 start-100 translate-middle badge rounded-pill bg-warning text-dark border border-white';
            } else {
                badgeBell.style.display = 'none';
            }
        }

        if (countDropdown) {
            countDropdown.textContent = total;
            countDropdown.className = total > 0 ? 'badge bg-danger rounded-pill' : 'badge bg-secondary rounded-pill';
        }
    }

    function renderDropdown(alertas) {
        const contenedor = document.getElementById('notifsListaContenedor');
        if (!contenedor) return;

        if (alertas.length === 0) {
            contenedor.innerHTML = `
                <div class="text-center py-4 px-3 text-muted">
                    <div class="rounded-circle bg-success-subtle text-success d-inline-flex align-items-center justify-content-center mb-2" style="width: 44px; height: 44px;">
                        <i class="bi bi-check-circle fs-4"></i>
                    </div>
                    <div class="fw-semibold small text-dark">Todo al día en la agenda</div>
                    <small class="text-secondary">No hay citas pendientes ni inasistencias por gestionar.</small>
                </div>
            `;
            return;
        }

        contenedor.innerHTML = alertas.map(a => {
            const badgeClass = a.tipo === 'NO_ASISTIO' 
                ? 'bg-danger text-white' 
                : (a.tipo === 'HOY' ? 'bg-warning text-dark' : 'bg-info text-dark');
            
            const badgeText = a.tipo === 'NO_ASISTIO' 
                ? 'Inasistencia' 
                : (a.tipo === 'HOY' ? 'Cita de Hoy' : 'Mañana');

            const iconoTipo = a.tipo === 'NO_ASISTIO'
                ? 'bi-calendar-x text-danger'
                : (a.tipo === 'HOY' ? 'bi-clock-history text-warning' : 'bi-calendar-check text-info');

            const btnText = a.tipo === 'NO_ASISTIO' ? 'Reagendar' : 'Recordar';

            return `
                <div class="p-3 border-bottom notif-item position-relative" data-notif-id="${escapeHtml(a.id)}">
                    <div class="d-flex justify-content-between align-items-start gap-2 mb-1">
                        <div class="d-flex align-items-center gap-1.5">
                            <i class="bi ${iconoTipo} fs-6"></i>
                            <strong class="small text-dark" style="font-size: 12.5px;">${escapeHtml(a.titulo)}</strong>
                        </div>
                        <span class="badge ${badgeClass} rounded-pill" style="font-size: 10px; padding: 0.25em 0.6em;">${badgeText}</span>
                    </div>
                    <p class="text-secondary small mb-2" style="font-size: 12px; line-height: 1.35;">${escapeHtml(a.mensaje)}</p>
                    <div class="d-flex align-items-center justify-content-between gap-1 mt-2 pt-1 border-top border-light">
                        <div class="d-flex gap-1.5">
                            <!-- Botón WhatsApp Inteligente -->
                            <button type="button" class="btn btn-sm btn-success py-1 px-2.5 rounded-pill d-inline-flex align-items-center gap-1 btn-wa-smart"
                                data-wa-phone="${escapeHtml(a.pacienteCelular)}"
                                data-wa-message="${escapeHtml(encodeURIComponent(a.mensajeSugeridoWhatsApp || ''))}"
                                title="Enviar mensaje de WhatsApp al paciente" style="font-size: 11.5px;">
                                <i class="bi bi-whatsapp"></i>
                                <span>${btnText}</span>
                            </button>
                            <!-- Personalizar mensaje -->
                            <button type="button" class="btn btn-sm btn-light border py-1 px-2 rounded-pill small text-secondary btn-personalizar-notif"
                                data-paciente="${escapeHtml(a.pacienteNombre)}" data-tel="${escapeHtml(a.pacienteCelular)}"
                                data-msg="${escapeHtml(encodeURIComponent(a.mensajeSugeridoWhatsApp || ''))}"
                                data-tipo="${a.tipo === 'NO_ASISTIO' ? 'inasistencia' : 'recordatorio'}"
                                title="Editar mensaje antes de enviar" style="font-size: 11.5px;">
                                <i class="bi bi-pencil-square"></i>
                            </button>
                        </div>
                        <!-- Botón Entendido / Marcar Leído -->
                        <button type="button" class="btn btn-sm text-muted p-1 btn-marcar-leida" data-id="${escapeHtml(a.id)}" title="Marcar como leída">
                            <i class="bi bi-check2"></i>
                        </button>
                    </div>
                </div>
            `;
        }).join('');

        // Eventos de botones dentro del dropdown
        contenedor.querySelectorAll('.btn-marcar-leida').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const id = btn.getAttribute('data-id');
                guardarLeida(id);
                fetch('/api/notificaciones/marcar-leida', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json',
                        [document.getElementById('dentiaCsrf')?.dataset.header || 'X-CSRF-TOKEN']:
                            document.getElementById('dentiaCsrf')?.value || '' },
                    body: JSON.stringify({ notificacionId: id })
                }).catch(() => {});
                consultarAlertas(false);
            });
        });

        contenedor.querySelectorAll('.btn-personalizar-notif').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const paciente = btn.getAttribute('data-paciente');
                const tel = btn.getAttribute('data-tel');
                const msg = decodeURIComponent(btn.getAttribute('data-msg') || '');
                const tipo = btn.getAttribute('data-tipo') || 'recordatorio';
                if (window.WhatsAppSmartSender && window.WhatsAppSmartSender.abrirModalComunicar) {
                    window.WhatsAppSmartSender.abrirModalComunicar(paciente, tel, msg, tipo);
                }
            });
        });
    }

    function init() {
        if ('Notification' in window) {
            permisosNotif = Notification.permission;
            if (Notification.permission === 'default') {
                // Solicitar con el primer clic del usuario
                document.addEventListener('click', function pedir() {
                    Notification.requestPermission().then(p => { permisosNotif = p; });
                    document.removeEventListener('click', pedir);
                }, { once: true });
            }
        }

        if ('serviceWorker' in navigator) {
            window.addEventListener('load', () => {
                navigator.serviceWorker.register('/sw.js').catch(() => {});
            });
            navigator.serviceWorker.addEventListener('message', (e) => {
                if (e.data && e.data.type === 'NOTIFICACION_ENTENDIDA') {
                    consultarAlertas(false);
                }
            });
        }

        // Inicializar estado visual del toggle de notificaciones
        actualizarEstadoVisualToggle(isActivas());

        const toggle = document.getElementById('toggleNotificacionesActivas');
        if (toggle) {
            toggle.addEventListener('click', (e) => {
                e.stopPropagation();
            });
            toggle.addEventListener('change', (e) => {
                e.stopPropagation();
                setActivas(toggle.checked, true);
            });
        }

        const boxToggle = document.getElementById('boxToggleNotifs');
        if (boxToggle) {
            boxToggle.addEventListener('click', (e) => {
                e.stopPropagation();
                if (e.target !== toggle && toggle) {
                    toggle.checked = !toggle.checked;
                    setActivas(toggle.checked, true);
                }
            });
        }

        // Consultar inmediatamente
        consultarAlertas(false);

        // Polling cada 35 segundos
        if (!pollingInterval) {
            pollingInterval = setInterval(() => {
                if (!document.hidden && isActivas()) {
                    consultarAlertas(true);
                }
            }, 35000);
        }

        // Botón Marcar Todas como Leídas
        const btnMarcarTodas = document.getElementById('btnMarcarTodasNotifs');
        if (btnMarcarTodas) {
            btnMarcarTodas.addEventListener('click', (e) => {
                e.preventDefault();
                marcarTodasLeidas();
            });
        }
    }

    return {
        init: init,
        consultar: consultarAlertas,
        marcarTodasLeidas: marcarTodasLeidas,
        isActivas: isActivas,
        setActivas: setActivas
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    NotificacionesService.init();
});
