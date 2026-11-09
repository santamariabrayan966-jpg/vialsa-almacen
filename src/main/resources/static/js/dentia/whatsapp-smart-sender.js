/**
 * WhatsAppSmartSender - Módulo unificado para envíos oficiales de WhatsApp en DentIA
 * Abre conversaciones individuales en WhatsApp Web o la aplicación.
 * Permite redactar, modificar o aplicar plantillas antes de abrir el chat oficial con el mensaje listo.
 */
const WhatsAppSmartSender = {
    init() {
        this.bindSmartButtons();
        this.bindModalMensajeEvents();
    },

    /**
     * Sanitiza y formatea el número de celular (añade prefijo 51 si es celular peruano de 9 dígitos)
     */
    formatearTelefono(phone) {
        if (!phone) return '';
        let cleaned = String(phone).replace(/\D/g, '');
        if (cleaned.length === 9 && cleaned.startsWith('9')) {
            cleaned = '51' + cleaned;
        }
        return cleaned;
    },

    /**
     * Envía o abre el chat oficial de WhatsApp con el número y mensaje proporcionados.
     */
    enviar({ phone, message, btnElement, onSuccess, onError }) {
        if (!phone) {
            this.mostrarToast('El paciente no tiene un número telefónico registrado.', 'bi-exclamation-triangle text-danger');
            if (onError) onError(new Error('Teléfono vacío'));
            return;
        }

        const cleanedPhone = this.formatearTelefono(phone);
        const textoMensaje = (message || '').trim();
        const originalContent = btnElement ? btnElement.innerHTML : null;

        if (btnElement) {
            btnElement.disabled = true;
            btnElement.innerHTML = `<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Abriendo...`;
        }

        this.abrirEnlaceWeb(cleanedPhone, textoMensaje);

        this.mostrarToast('Abriendo WhatsApp Oficial... Puedes revisar o modificar el mensaje antes de presionar Enviar.', 'bi-whatsapp text-success');

        if (btnElement) {
            btnElement.classList.remove('btn-outline-success', 'btn-teal');
            btnElement.classList.add('btn-success');
            btnElement.innerHTML = `<i class="bi bi-check2 me-1"></i> Abierto`;
            setTimeout(() => {
                btnElement.disabled = false;
                if (originalContent) btnElement.innerHTML = originalContent;
            }, 3000);
        }

        if (onSuccess) onSuccess({ mode: 'web_direct', phone: cleanedPhone });
    },

    /**
     * Abre de manera oficial y legal el esquema de WhatsApp Web / App
     */
    abrirEnlaceWeb(phone, message) {
        const url = `https://api.whatsapp.com/send?phone=${phone}&text=${encodeURIComponent(message || '')}`;
        window.open(url, '_blank');
    },

    /**
     * Toast de notificación moderno y flotante
     */
    mostrarToast(mensaje, icono = 'bi-info-circle text-primary') {
        let container = document.getElementById('waSmartToastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'waSmartToastContainer';
            container.style.cssText = 'position: fixed; bottom: 20px; right: 20px; z-index: 99999; display: flex; flex-direction: column; gap: 8px; max-width: 380px; pointer-events: none;';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = 'alert alert-light border shadow-lg py-2.5 px-3 rounded-4 small d-flex align-items-center gap-2.5';
        toast.style.cssText = 'pointer-events: auto; background: #ffffff; animation: fadeIn 0.25s ease-out; margin: 0;';
        toast.innerHTML = `
            <i class="bi ${icono} fs-5 flex-shrink-0"></i>
            <span class="text-dark fw-medium" style="font-size: 13px; line-height: 1.35;">${mensaje}</span>
        `;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity 0.4s ease';
            setTimeout(() => toast.remove(), 450);
        }, 4500);
    },

    /**
     * Intercepta botones de envío directo con .btn-wa-smart o [data-wa-phone]
     */
    bindSmartButtons() {
        document.addEventListener('click', (e) => {
            const btn = e.target.closest('.btn-wa-smart, [data-wa-phone]');
            if (!btn) return;

            // Si el botón está destinado a abrir el modal de comunicar, no disparar envío directo
            if (btn.hasAttribute('data-bs-toggle') && btn.getAttribute('data-bs-target') === '#modalComunicarPaciente') {
                return;
            }

            let phone = btn.getAttribute('data-wa-phone');
            let message = btn.getAttribute('data-wa-message');

            if (!phone && btn.href) {
                try {
                    const url = new URL(btn.href);
                    if (url.hostname.includes('whatsapp.com') || url.hostname.includes('wa.me')) {
                        if (url.searchParams.has('phone')) {
                            phone = url.searchParams.get('phone');
                        } else if (url.pathname.length > 1) {
                            phone = url.pathname.replace('/', '');
                        }
                        if (url.searchParams.has('text')) {
                            message = url.searchParams.get('text');
                        }
                    }
                } catch (ignored) {}
            }

            if (phone) {
                e.preventDefault();
                e.stopPropagation();

                // Si el mensaje viene codificado como URL component, decodificarlo
                let decodedMessage = message || '';
                try {
                    if (decodedMessage.includes('%')) {
                        decodedMessage = decodeURIComponent(decodedMessage);
                    }
                } catch (ignored) {}

                this.enviar({
                    phone: phone,
                    message: decodedMessage,
                    btnElement: btn
                });
            }
        });
    },

    /**
     * Control del Modal de Mensajería: búsqueda de paciente, edición de teléfono,
     * selección de plantilla y edición libre del mensaje.
     */
    bindModalMensajeEvents() {
        const modalEl = document.getElementById('modalComunicarPaciente');
        if (!modalEl) return;

        const inputSearch = document.getElementById('waSearchPacienteInput');
        const searchResults = document.getElementById('waSearchPacienteResults');
        const lblSelNombre = document.getElementById('waSelPacienteNombre');
        const inputTel = document.getElementById('waPacienteTelefono');
        const selectPlantilla = document.getElementById('waSelectPlantilla');
        const textareaMsg = document.getElementById('waMensajeTexto');
        const charCount = document.getElementById('waCharCountIndividual');
        const status = document.getElementById('waFormStatus');
        const closeButton = modalEl.querySelector('.wa-close');

        modalEl.addEventListener('hidePrevented.bs.modal', () => {
            closeButton.classList.remove('wa-close-hint');
            void closeButton.offsetWidth;
            closeButton.classList.add('wa-close-hint');
            closeButton.focus();
            status.textContent = 'Para cerrar, usa la X de la parte superior.';
        });
        modalEl.addEventListener('hidden.bs.modal', () => { status.textContent = ''; });
        const btnEnviarModal = document.getElementById('btnEnviarWaModal');

        let pacienteSeleccionado = null;

        // Actualizar contador de caracteres
        textareaMsg?.addEventListener('input', () => {
            if (charCount) {
                charCount.textContent = `${textareaMsg.value.length} caracteres`;
            }
        });

        // Búsqueda en vivo de pacientes
        let timeoutSearch = null;
        let searchVersion = 0;
        inputSearch?.addEventListener('input', () => {
            clearTimeout(timeoutSearch);
            const q = inputSearch.value.trim();
            const version = ++searchVersion;
            pacienteSeleccionado = null;
            lblSelNombre.textContent = 'Número manual';
            if (q.length < 2) {
                if (searchResults) searchResults.innerHTML = '';
                return;
            }

            timeoutSearch = setTimeout(async () => {
                try {
                    const res = await fetch(`/api/notificaciones/pacientes-rapido?q=${encodeURIComponent(q)}`);
                    if (res.ok) {
                        const lista = await res.json();
                        if (version !== searchVersion) return;
                        if (!searchResults) return;
                        if (lista.length === 0) {
                            searchResults.innerHTML = '<div class="p-2 text-muted small">No se encontraron pacientes activos.</div>';
                            return;
                        }
                        searchResults.replaceChildren();
                        lista.forEach(p => {
                            const btn = document.createElement('button');
                            btn.type = 'button';
                            btn.className = 'wa-result';
                            const name = document.createElement('strong');
                            name.textContent = p.nombreCompleto;
                            const detail = document.createElement('small');
                            detail.textContent = [p.dni ? 'DNI ' + p.dni : '', p.celular || 'Sin número'].filter(Boolean).join(' · ');
                            btn.append(name, detail);
                            btn.addEventListener('click', () => {
                                pacienteSeleccionado = { id: p.idPaciente, nombre: p.nombreCompleto, soloNombre: p.nombres, tel: p.celular };
                                lblSelNombre.textContent = p.nombreCompleto;
                                inputTel.value = p.celular || '';
                                inputSearch.value = p.nombreCompleto;
                                searchResults.replaceChildren();
                                aplicarPlantilla();
                            });
                            searchResults.appendChild(btn);
                        });
                    }
                } catch (e) {
                    console.debug('Error buscando pacientes:', e);
                }
            }, 250);
        });

        // Cambio dinámico de plantilla odontológica
        function aplicarPlantilla() {
            const tipo = selectPlantilla?.value || 'personalizado';
            const nombre = (pacienteSeleccionado?.soloNombre || pacienteSeleccionado?.nombre || (lblSelNombre?.textContent === 'Número manual' ? '' : lblSelNombre?.textContent) || 'estimado(a) paciente').trim();

            let txt = '';
            switch (tipo) {
                case 'recordatorio':
                    txt = `Hola ${nombre}, te saludamos de la clínica odontológica DentIA 🦷\nTe recordamos que tienes una cita médica programada próximamente.\nPor favor confírmanos tu asistencia respondiendo a este mensaje. ¡Te esperamos!`;
                    break;
                case 'inasistencia':
                    txt = `Hola ${nombre}, te saludamos de DentIA 🦷\nNotamos que no pudiste acudir a tu cita odontológica reciente. ¿Te gustaría que te ayudemos a reprogramarla para una fecha y hora que te sea más conveniente?`;
                    break;
                case 'post_operatorio':
                    txt = `Hola ${nombre}, ¿cómo estás? Te escribimos de DentIA para consultar cómo te sientes tras tu atención dental de hoy 🦷\nRecuerda seguir las indicaciones brindadas y evitar alimentos duros o muy calientes. Cualquier duda estamos para atenderte.`;
                    break;
                case 'presupuesto':
                    txt = `Hola ${nombre}, te informamos desde DentIA que tu presupuesto y plan de tratamiento odontológico ya están listos en clínica 🦷\nPuedes escribirnos para coordinar el inicio de tus atenciones.`;
                    break;
                case 'higiene':
                    txt = `Hola ${nombre}, en DentIA nos preocupamos por la salud de tu sonrisa 🦷\nRecuerda que se recomienda realizar una profilaxis y limpieza dental preventiva cada 6 meses. Si deseas agendar un turno, con gusto te atendemos.`;
                    break;
                default:
                    txt = '';
                    break;
            }

            if (txt && textareaMsg) {
                textareaMsg.value = txt;
                textareaMsg.dispatchEvent(new Event('input'));
            }
        }

        selectPlantilla?.addEventListener('change', aplicarPlantilla);

        // Envío directo oficial desde el Modal
        btnEnviarModal?.addEventListener('click', () => {
            const tel = inputTel?.value?.trim();
            const msg = textareaMsg?.value?.trim();

            const cleaned = WhatsAppSmartSender.formatearTelefono(tel);
            if (!/^[1-9]\d{7,14}$/.test(cleaned)) {
                status.textContent = 'Ingresa un número válido con código de país o un celular peruano de 9 dígitos.';
                inputTel?.focus();
                return;
            }
            if (!msg) {
                status.textContent = 'Escribe un mensaje para continuar.';
                textareaMsg?.focus();
                return;
            }

            status.textContent = '';
            WhatsAppSmartSender.enviar({
                phone: tel,
                message: msg,
                btnElement: btnEnviarModal,
                onSuccess: () => {
                    status.textContent = 'Se abrió WhatsApp. Revisa el mensaje antes de enviarlo.';
                }
            });
        });
    },

    /**
     * Abre el modal de comunicación pre-llenado para que el usuario pueda
     * revisar o modificar el mensaje antes de enviarlo por WhatsApp.
     */
    abrirModalComunicar(pacienteNombre, pacienteTelefono, mensajeSugerido, plantillaKey = 'personalizado') {
        const modalEl = document.getElementById('modalComunicarPaciente');
        if (!modalEl) return;

        const tabIndBtn = document.getElementById('tabWaIndividualBtn');
        if (tabIndBtn) {
            const tabInstance = bootstrap.Tab.getOrCreateInstance(tabIndBtn);
            tabInstance.show();
        }

        const lblSelNombre = document.getElementById('waSelPacienteNombre');
        const inputTel = document.getElementById('waPacienteTelefono');
        const inputSearch = document.getElementById('waSearchPacienteInput');
        const textareaMsg = document.getElementById('waMensajeTexto');
        const selectPlantilla = document.getElementById('waSelectPlantilla');

        if (lblSelNombre) lblSelNombre.textContent = pacienteNombre || 'Paciente seleccionado';
        if (inputTel) inputTel.value = pacienteTelefono || '';
        if (inputSearch) inputSearch.value = pacienteNombre || '';
        if (textareaMsg && mensajeSugerido) {
            let msg = mensajeSugerido;
            try {
                if (msg.includes('%')) msg = decodeURIComponent(msg);
            } catch (ignored) {}
            textareaMsg.value = msg;
            textareaMsg.dispatchEvent(new Event('input'));
        }
        if (selectPlantilla && plantillaKey) selectPlantilla.value = plantillaKey;

        const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
        modalInstance.show();
    }
};

document.addEventListener('DOMContentLoaded', () => {
    window.WhatsAppSmartSender = WhatsAppSmartSender;
    WhatsAppSmartSender.init();
});
