(() => {
    'use strict';

    const root = document.getElementById('clinica-workspace');
    if (!root) return;

    const initialModulo = root.dataset.modulo || 'historia-clinica';
    const initialPacienteId = root.dataset.pacienteId || '';

    const $ = selector => document.querySelector(selector);
    const $$ = selector => document.querySelectorAll(selector);
    const escape = val => String(val ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const money = val => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(Number(val || 0));
    const parseAnyDate = val => {
        if (!val) return null;
        if (val instanceof Date) return isNaN(val.getTime()) ? null : val;
        if (Array.isArray(val)) {
            const d = new Date(val[0], (val[1] || 1) - 1, val[2] || 1, val[3] || 0, val[4] || 0, val[5] || 0);
            return isNaN(d.getTime()) ? null : d;
        }
        const str = String(val).trim();
        if (str.length === 10 && /^\d{4}-\d{2}-\d{2}$/.test(str)) {
            return new Date(str + 'T12:00:00');
        }
        const d = new Date(str);
        return isNaN(d.getTime()) ? null : d;
    };
    const formatDate = val => {
        const d = parseAnyDate(val);
        return d ? new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }).format(d) : '—';
    };
    const formatDateTime = val => {
        const d = parseAnyDate(val);
        return d ? new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(d) : '—';
    };
    const formatTime = val => {
        const d = parseAnyDate(val);
        return d ? new Intl.DateTimeFormat('es-PE', { hour: '2-digit', minute: '2-digit', hour12: true }).format(d) : '—';
    };

    // Estado central de la aplicación clínica
    const state = {
        activeModulo: initialModulo,
        pacienteId: initialPacienteId ? Number(initialPacienteId) : null,
        citaId: root.dataset.citaId || null,
        expediente: null,
        odontoTool: 'CARIES',
        odontoDentition: 'TODOS',
        selectedPiece: 16,
        detallePieza: null,
        odontoPiecesMap: new Map(),
        odontoPending: new Map(),
        odontoSaving: false,
        activePlanId: null,
        carritoPlan: [],
        catalogoCache: [],
        descuentosActivos: false,
        modoDescuento: 'item', // 'item' | 'global'
        descuentoGlobalTipo: 'pct', // 'pct' | 'val'
        descuentoGlobalVal: 0,
        mobileBuilderView: 'catalogo' // 'catalogo' | 'cart'
    };

    // Helper para llamadas API seguras con CSRF
    async function api(path, options = {}) {
        if (options.method && options.method !== 'GET' && (!state.expediente || root.dataset.patientLoading === 'true'))
            throw new Error('Selecciona un paciente y espera a que cargue el expediente.');
        const headers = new Headers(options.headers || {});
        headers.set('Accept', 'application/json');
        if (options.body && !(options.body instanceof FormData)) {
            headers.set('Content-Type', 'application/json');
            options.body = JSON.stringify(options.body);
        }
        if (options.method && options.method !== 'GET') {
            const tokenMeta = $('meta[name="csrf-token"]');
            const headerMeta = $('meta[name="csrf-header"]');
            if (tokenMeta && headerMeta) {
                headers.set(headerMeta.content, tokenMeta.content);
            }
        }
        let res;
        try {
            res = await fetch(path, { credentials: 'same-origin', ...options, headers });
        } catch (fetchErr) {
            const error = new Error('No se pudo establecer conexión con el servidor. Compruebe su conexión a internet.');
            error.status = 0;
            throw error;
        }
        if (!res.ok) {
            let data = {};
            try {
                data = await res.json();
            } catch (e) {
                data = {};
            }
            const rawMsg = data.error || data.mensaje || data.message || `Error ${res.status}`;
            const error = new Error(formatearMensajeEspanol(rawMsg));
            error.status = res.status;
            error.detalles = data;
            throw error;
        }
        return res.json();
    }

    // Traductor / formateador para garantizar que todos los mensajes estén en español comprensible
    function formatearMensajeEspanol(msg) {
        if (!msg || typeof msg !== 'string') return 'Ocurrió un error inesperado al procesar la solicitud.';
        const m = msg.trim();
        if (m.includes('Failed to fetch') || m.includes('NetworkError') || m.includes('Network Error')) {
            return 'No se pudo conectar con el servidor. Compruebe su conexión a internet.';
        }
        if (m.includes('is required') || m.includes('must not be null') || m.includes('must not be blank')) {
            return 'Por favor, complete todos los campos obligatorios antes de continuar.';
        }
        if (m.startsWith('Error 400') || m === 'Bad Request') {
            return 'Los datos enviados contienen errores. Por favor, revise los campos del formulario.';
        }
        if (m.startsWith('Error 401') || m === 'Unauthorized') {
            return 'Su sesión ha vencido. Por favor, vuelva a iniciar sesión.';
        }
        if (m.startsWith('Error 403') || m === 'Forbidden') {
            return 'No tiene los permisos necesarios para realizar esta acción clínica.';
        }
        if (m.startsWith('Error 404') || m === 'Not Found') {
            return 'El registro o elemento solicitado no se encontró en el sistema.';
        }
        if (m.startsWith('Error 500') || m.includes('Internal Server Error')) {
            return 'Ocurrió un inconveniente temporal en el servidor. Por favor, intente nuevamente.';
        }
        return m;
    }

    // Sistema de Avisos Contextuales (Dentro de modales y en pantalla flotante visible)
    function mostrarAviso(mensaje, tipo = 'success', contexto = null) {
        mensaje = formatearMensajeEspanol(mensaje);

        // 1. Detectar si hay un modal activo o si se pasó contexto
        let modalAbierto = null;
        if (contexto && contexto instanceof Element) {
            modalAbierto = contexto.closest('.modal');
        }
        if (!modalAbierto) {
            modalAbierto = document.querySelector('.modal.show');
        }

        // Si hay un modal abierto, renderizar la alerta DIRECTAMENTE DENTRO del modal
        if (modalAbierto) {
            mostrarAlertaEnModal(modalAbierto, mensaje, tipo);
        }

        // 2. Renderizar también la alerta flotante siempre visible (Toast fijo en viewport)
        mostrarToastFlotante(mensaje, tipo);
    }

    function mostrarAlertaEnModal(modal, mensaje, tipo) {
        const modalBody = modal.querySelector('.modal-body') || modal.querySelector('.modal-content');
        if (!modalBody) return;

        let alertBox = modal.querySelector('.modal-local-alert');
        if (!alertBox) {
            alertBox = document.createElement('div');
            alertBox.setAttribute('role', 'alert');
            modalBody.insertBefore(alertBox, modalBody.firstChild);
        }

        const esError = tipo === 'error' || tipo === 'danger';
        const alertClass = esError ? 'alert-danger' : (tipo === 'warning' ? 'alert-warning' : (tipo === 'info' ? 'alert-info' : 'alert-success'));
        const iconClass = esError ? 'bi-exclamation-triangle-fill text-danger' : (tipo === 'warning' ? 'bi-exclamation-circle-fill text-warning' : (tipo === 'info' ? 'bi-info-circle-fill text-info' : 'bi-check-circle-fill text-success'));
        const prefix = esError ? 'Verifique los datos ingresados:' : (tipo === 'warning' ? 'Advertencia:' : (tipo === 'info' ? 'Aviso:' : 'Acción completada:'));

        alertBox.className = `modal-local-alert alert ${alertClass} alert-dismissible fade show d-flex align-items-start gap-2.5 mb-3 shadow-xs rounded-3 py-2.5 px-3`;
        alertBox.innerHTML = `
            <i class="bi ${iconClass} fs-5 mt-0.5 flex-shrink-0"></i>
            <div class="flex-grow-1 small">
                <strong class="d-block mb-0.5">${prefix}</strong>
                <span class="modal-local-alert-text"></span>
            </div>
            <button type="button" class="btn-close" aria-label="Cerrar aviso" style="padding: 0.75rem;"></button>
        `;
        alertBox.querySelector('.modal-local-alert-text').textContent = mensaje;
        alertBox.querySelector('.btn-close')?.addEventListener('click', () => alertBox.remove());

        // Desplazar el modal hacia arriba suavemente para enfocar la alerta en pantalla
        modalBody.scrollTo({ top: 0, behavior: 'smooth' });

        // Si es un error de campo específico, marcar y enfocar el campo en el formulario
        resaltarCampoConError(modal, mensaje);

        clearTimeout(alertBox._timer);
        if (!esError) {
            alertBox._timer = setTimeout(() => alertBox?.remove(), 6000);
        }
    }

    function resaltarCampoConError(modal, mensaje) {
        if (!mensaje || typeof mensaje !== 'string') return;
        const msgLower = mensaje.toLowerCase();

        const fieldRules = [
            { keywords: ['piezas tratadas', 'pieza tratada', 'piezas_tratadas', '100 caracteres'], name: 'piezasTratadas', tip: 'Las piezas tratadas no deben superar 100 caracteres. Indique solo números (ej: 16, 26, 48).' },
            { keywords: ['procedimiento', 'planprocedimiento', 'tratamiento efectuado'], name: 'planProcedimiento', tip: 'Por favor, detalle el procedimiento dental realizado.' },
            { keywords: ['importe', 'precio'], name: 'importeTratamiento', tip: 'Revisa el importe acordado (máximo dos decimales).' },
            { keywords: ['motivo', 'motivo de consulta'], name: 'motivoConsulta', tip: 'Indique el motivo de la consulta.' },
            { keywords: ['subjetivo', 'relato subjetivo'], name: 'subjetivo', tip: 'Revise el texto del relato subjetivo.' },
            { keywords: ['objetivo', 'hallazgos objetivos'], name: 'objetivo', tip: 'Revise los hallazgos objetivos.' },
            { keywords: ['evaluacion', 'evaluación'], name: 'evaluacion', tip: 'Revise la valoración o evaluación clínica.' },
            { keywords: ['receta', 'prescripción', 'prescripcion', 'indicaciones'], name: 'indicacionesReceta', tip: 'Revise las indicaciones o medicamentos de la receta.' }
        ];

        for (const rule of fieldRules) {
            if (rule.keywords.some(k => msgLower.includes(k))) {
                const input = modal.querySelector(`[name="${rule.name}"]`);
                if (input) {
                    input.classList.add('is-invalid');
                    let feedback = input.parentNode.querySelector('.invalid-feedback');
                    if (!feedback) {
                        feedback = document.createElement('div');
                        feedback.className = 'invalid-feedback small fw-semibold mt-1';
                        input.parentNode.appendChild(feedback);
                    }
                    feedback.textContent = rule.tip;
                    feedback.style.display = 'block';

                    setTimeout(() => input.focus(), 150);

                    const onInput = () => {
                        input.classList.remove('is-invalid');
                        if (feedback) feedback.style.display = 'none';
                        input.removeEventListener('input', onInput);
                    };
                    input.addEventListener('input', onInput);
                    break;
                }
            }
        }
    }

    function mostrarToastFlotante(mensaje, tipo = 'success') {
        let toast = $('#clinica-toast');
        if (!toast) return;

        const esError = tipo === 'error' || tipo === 'danger';
        const alertClass = esError ? 'alert-danger' : (tipo === 'warning' ? 'alert-warning' : (tipo === 'info' ? 'alert-info' : 'alert-success'));
        const iconClass = esError ? 'bi-exclamation-triangle-fill text-danger' : (tipo === 'warning' ? 'bi-exclamation-circle-fill text-warning' : (tipo === 'info' ? 'bi-info-circle-fill text-info' : 'bi-check-circle-fill text-success'));

        toast.className = `clinica-notice alert ${alertClass}`;
        let iconEl = toast.querySelector('.toast-icon');
        if (!iconEl) {
            iconEl = document.createElement('i');
            iconEl.className = `bi ${iconClass} fs-5 toast-icon flex-shrink-0`;
            toast.insertBefore(iconEl, toast.firstChild);
        } else {
            iconEl.className = `bi ${iconClass} fs-5 toast-icon flex-shrink-0`;
        }

        const msgEl = $('#clinica-toast-message');
        if (msgEl) msgEl.textContent = mensaje;

        toast.hidden = false;
        clearTimeout(toast._timeout);
        // Si no es error, ocultar en 5 segundos. Si es error, dar 10 segundos o cierre manual
        toast._timeout = setTimeout(() => {
            toast.hidden = true;
        }, esError ? 10000 : 5000);
    }

    // --- CARGA DE EXPEDIENTE DEL PACIENTE ---
    let patientRequest = 0;
    let atencionEditor = null;
    async function cargarPaciente(id) {
        if (!id) return;
        if (atencionEditor?.busy) { mostrarAviso('Espera a que termine el guardado de la atención.', 'error'); return; }
        const cambiarPaciente = atencionEditor && atencionEditor.pacienteId !== Number(id);
        if (cambiarPaciente && atencionEditor.isDirty(leerAtencion()) &&
                !window.confirm('Hay cambios de atención sin guardar o un cierre sin confirmar. ¿Quieres salir de este paciente?')) return;
        const request = ++patientRequest;
        root.dataset.patientLoading = 'true';
        try {
            const data = await api(`/interno/clinica/api/expediente/${Number(id)}`);
            const selectCatalogo = $('#sidebar-select-tratamiento');
            if (selectCatalogo && !selectCatalogo.dataset.loaded) {
                const catalogo = await api('/interno/clinica/api/tratamientos/catalogo');
                selectCatalogo.innerHTML = catalogo.map(t => `<option value="${escape(t.nombre)}" data-id="${Number(t.id)}" data-precio="${Number(t.precio)}">${escape(t.nombre)} (${money(t.precio)})</option>`).join('') + '<option value="OTRO" data-precio="0">Otro procedimiento</option>';
                selectCatalogo.dataset.loaded = 'true';
            }
            if (request !== patientRequest) return;
            if (state.pacienteId && Number(state.pacienteId) !== Number(id)) {
                state.odontoPending.clear();
                state.detallePieza = null;
                bootstrap.Modal.getInstance($('#modal-detalle-pieza'))?.hide();
                state.citaId = null; state.activePlanId = null;
                atencionEditor = null;
                delete workingDraftAtencion[state.pacienteId];
                actualizarEstadoAtencion();
                bootstrap.Modal.getInstance($('#modal-crear-atencion'))?.hide();
                $('#form-crear-atencion')?.reset(); $('#form-historia')?.reset();
                $$('.tooth-picker-chip').forEach(c => c.classList.remove('active'));
                const chkAll = $('#chk-todas-piezas'); if (chkAll) chkAll.checked = false;
                $('#img-panoramica-preview')?.removeAttribute('src');
                $('#contenedor-preview-panoramica')?.classList.add('d-none'); $('#dropzone-panoramica')?.classList.remove('d-none');
            }
            state.pacienteId = Number(id); state.expediente = data;
            state.odontoPiecesMap.clear();
            const primeraPat = (data.piezasOdontograma || []).find(p => p.condicion && p.condicion !== 'SANO');
            state.selectedPiece = primeraPat ? Number(primeraPat.numeroPieza) : (state.selectedPiece || 16);
            renderHeaderPaciente(data); renderModuloActual(); actualizarColoresStudio3D();
            const url = new URL(window.location);
            url.searchParams.set('pacienteId', id);
            if (state.citaId) url.searchParams.set('citaId', state.citaId); else url.searchParams.delete('citaId');
            window.history.replaceState({}, '', url);
            const draft = $('#btn-recuperar-atencion');
            if (draft) draft.hidden = !data.borradorAtencion?.contenido;
        } catch (error) { mostrarAviso(error.message, 'error'); }
        finally { if (request === patientRequest) root.dataset.patientLoading = 'false'; }
    }

    function proximaCita() {
        return (state.expediente?.citas || []).filter(c => ['RESERVADA', 'CONFIRMADA', 'EN_ESPERA', 'EN_ATENCION'].includes(c.estado) && new Date(c.fecha || c.inicio) >= new Date())
            .sort((a,b) => new Date(a.fecha || a.inicio) - new Date(b.fecha || b.inicio))[0];
    }

    // Render de cabecera fija con datos y alertas del paciente
    function renderHeaderPaciente(data) {
        const p = data.paciente;
        if (!p) return;
        const av = $('#paciente-avatar');
        if (av) av.textContent = (p.nombres?.[0] || 'C') + (p.apellidos?.[0] || 'M');
        const nom = $('#paciente-nombre');
        if (nom) nom.textContent = p.nombreCompleto;
        const metaId = $('#paciente-meta-id');
        if (metaId) metaId.textContent = `ID: 2026-${String(p.id).padStart(4, '0')} · ${p.dni ? 'DNI: ' + p.dni : 'Sin DNI'}`;
        const metaEdad = $('#paciente-meta-edad');
        if (metaEdad) metaEdad.textContent = (p.edad != null ? `${p.edad} años` : 'Edad no registrada');

        if ($('#paciente-meta-tel')) $('#paciente-meta-tel').textContent = p.celular || 'Sin teléfono';
        if ($('#paciente-meta-dni')) $('#paciente-meta-dni').textContent = p.dni ? 'DNI: ' + p.dni : 'Sin DNI';
        if ($('#widget-proxima-cita-fecha')) $('#widget-proxima-cita-fecha').textContent = proximaCita() ? formatDateTime(proximaCita().fecha) : 'Sin cita programada';

        // Render alertas médicas
        const alertsContainer = $('#paciente-alertas');
        if (alertsContainer) {
            if (data.alertas && data.alertas.length > 0) {
                alertsContainer.innerHTML = data.alertas.map(a => `
                    <div class="studio-alert-chip"><i class="bi bi-exclamation-triangle-fill"></i> ${escape(a)}</div>
                `).join('');
                alertsContainer.hidden = false;
            } else {
                alertsContainer.hidden = true;
            }
        }

        // Widget próxima cita
        if (data.citas && data.citas.length > 0) {
            const prox = proximaCita();
            if (prox) {
                const wFecha = $('#widget-proxima-cita-fecha');
                const wMotivo = $('#widget-proxima-cita-motivo');
                if (wFecha) wFecha.textContent = formatDateTime(prox.fecha || prox.inicio);
                if (wMotivo) wMotivo.textContent = `${prox.tipoAtencion || 'Cita dental'} · ${prox.profesional || ''}`;
            }
        }

        // Render Seguimiento de Ortodoncia / Brackets
        const orto = data.ortodoncia;
        const ortoCard = $('#ortho-tracker-card');
        if (ortoCard) {
            if (orto && orto.activo) {
                ortoCard.hidden = false;
                const oTitle = $('#ortho-card-title');
                if (oTitle) oTitle.textContent = orto.titulo || 'Ortodoncia con Brackets';
                const oTimer = $('#ortho-card-timer');
                if (oTimer) oTimer.textContent = `${orto.itemsRealizados || 0} de ${orto.totalItems || 0} procedimientos`;
                const estadoProgreso = $('#ortho-card-status');
                if (estadoProgreso) estadoProgreso.textContent = Number(orto.porcentajeAvance) === 100 ? 'Completado' : 'En curso';
                const oPct = $('#ortho-card-percent-label');
                if (oPct) oPct.textContent = `${orto.porcentajeAvance ?? 0}% completado`;
                const oBar = $('#ortho-card-bar');
                if (oBar) {
                    oBar.style.width = `${orto.porcentajeAvance ?? 0}%`;
                    oBar.setAttribute('aria-valuenow', orto.porcentajeAvance ?? 0);
                }
                const oFase = $('#ortho-card-fase');
                if (oFase) oFase.textContent = orto.faseActual || 'Sin fase registrada';
                const oNext = $('#ortho-card-next');
                if (oNext) oNext.textContent = orto.proximoControl || 'Consultar agenda';
            } else {
                ortoCard.hidden = true;
            }
        }

        // Render estado de Cita Activa / Vinculada en el Banner Superior
        const heroCitaContainer = $('#contenedor-cita-hero');
        if (heroCitaContainer) {
            const citas = data.citas || [];
            let citaActiva = state.citaId ? citas.find(c => String(c.id) === String(state.citaId)) : null;
            if (!citaActiva) {
                citaActiva = citas.find(c => c.estado === 'EN_ATENCION' || c.estado === 'EN_ESPERA');
                if (citaActiva) state.citaId = Number(citaActiva.id);
            }

            if (citaActiva) {
                heroCitaContainer.hidden = false;
                const estaAtendida = citaActiva.estado === 'ATENDIDA';
                heroCitaContainer.innerHTML = `
                    <span class="badge ${estaAtendida ? 'bg-success text-white' : 'bg-primary text-white'} shadow-sm" id="badge-cita-hero">
                        <i class="bi bi-${estaAtendida ? 'check2-all' : 'heart-pulse'} me-1"></i> Cita #${citaActiva.id} ${estaAtendida ? 'Atendida' : 'Activa'}
                    </span>
                    ${!estaAtendida ? `
                        <button type="button" class="btn btn-sm btn-success fw-bold shadow-xs py-1 px-2.5" id="btn-hero-finalizar-cita" title="Finalizar cita agendada y registrar atención médica">
                            <i class="bi bi-check2-circle me-1"></i> Finalizar Cita y Registrar Atención
                        </button>
                        <form method="post" action="/interno/citas/estado" class="d-inline" id="form-hero-regresar-espera">
                            <input type="hidden" name="idCita" value="${citaActiva.id}">
                            <input type="hidden" name="nuevoEstado" value="RESERVADA">
                            <input type="hidden" name="observacion" value="Atención revertida a espera por el profesional">
                            <button type="submit" class="btn btn-sm btn-outline-secondary fw-medium shadow-xs py-1 px-2.5" title="Regresar esta cita a espera" onclick="return confirm('¿Deseas regresar esta cita a espera? Podrás atenderla después o atender a otro paciente.');">
                                <i class="bi bi-arrow-counterclockwise me-1"></i> Regresar a Espera
                            </button>
                        </form>
                    ` : `
                        <button type="button" class="btn btn-sm btn-outline-teal fw-bold shadow-xs py-1 px-2.5 btn-ver-atencion-cita" data-cita-id="${citaActiva.id}" style="color: #0b7f86; border-color: #0b7f86;">
                            <i class="bi bi-journal-medical me-1"></i> Ver Atención Registrada
                        </button>
                        <button type="button" class="btn btn-sm btn-success fw-bold shadow-xs py-1 px-2.5 ms-1" id="btn-hero-nueva-atencion" title="Registrar nueva atención médica para este paciente">
                            <i class="bi bi-plus-circle me-1"></i> Nueva Atención
                        </button>
                    `}
                `;
                $('#btn-hero-finalizar-cita')?.addEventListener('click', abrirModalAtencion);
                $('#btn-hero-nueva-atencion')?.addEventListener('click', () => {
                    state.citaId = null;
                    delete root.dataset.citaId;
                    abrirModalAtencion();
                });
                heroCitaContainer.querySelector('.btn-ver-atencion-cita')?.addEventListener('click', () => abrirModalDetalleAtencionCita(citaActiva.id));
            } else {
                // PACIENTE SIN CITA ACTIVA: ATENCIÓN DIRECTA SIEMPRE DISPONIBLE
                heroCitaContainer.hidden = false;
                heroCitaContainer.innerHTML = `
                    <span class="badge bg-light text-secondary border shadow-xs" id="badge-cita-hero" title="Atención clínica directa sin cita previa">
                        <i class="bi bi-shield-check text-teal me-1"></i> Consulta directa
                    </span>
                    <button type="button" class="btn btn-sm btn-success fw-bold shadow-xs py-1 px-2.5" id="btn-hero-atencion-directa" title="Registrar atención médica o procedimiento para este paciente">
                        <i class="bi bi-journal-medical me-1"></i> Registrar Atención
                    </button>
                `;
                $('#btn-hero-atencion-directa')?.addEventListener('click', () => {
                    state.citaId = null;
                    delete root.dataset.citaId;
                    abrirModalAtencion();
                });
            }
        }
    }

    function renderModuloActual() {
        if (state.activeModulo === 'tratamientos') {
            state.activeModulo = 'plan-tratamiento';
        }
        const mod = state.activeModulo;

        // Pestañas superiores activas
        $$('.tab-pill-btn').forEach(item => {
            const isAct = item.dataset.modulo === mod;
            item.classList.toggle('active', isAct);
        });

        // Ocultar todos los paneles y mostrar el seleccionado
        $$('.clinica-view-panel').forEach(p => p.hidden = true);
        const activePanel = $(`#panel-${mod}`);
        if (activePanel) activePanel.hidden = false;

        if (mod === 'odontograma' && !$('#studio-3d-container')?.hidden) {
            setTimeout(initStudio3D, 80);
        }

        if (!state.expediente) {
            if (mod === 'odontograma') renderOdontograma();
            return;
        }

        switch (mod) {
            case 'historia-clinica':
                renderHistoriaClinica();
                break;
            case 'odontograma':
                renderOdontograma();
                break;
            case 'diagnosticos':
                renderDiagnosticos();
                break;
            case 'tratamientos':
            case 'plan-tratamiento':
                renderPlanesTratamiento();
                break;
            case 'atenciones':
                renderAtenciones();
                break;
            case 'cuentas-pagos':
                renderCuentasPagos();
                break;
            case 'historial-citas':
                renderHistorialCitas();
                break;
        }
    }

    // ============================================================
    // 1. HISTORIA CLÍNICA
    // ============================================================
    function renderHistoriaClinica() {
        const h = state.expediente?.historia || {};
        const form = $('#form-historia');
        if (!form) return;
        if (form.elements['alergias']) form.elements['alergias'].value = h.alergias || '';
        if (form.elements['enfermedadesSistemicas']) form.elements['enfermedadesSistemicas'].value = h.enfermedades_sistemicas || '';
        if (form.elements['medicacionActual']) form.elements['medicacionActual'].value = h.medicacion_actual || '';
        if (form.elements['hemorragias']) form.elements['hemorragias'].checked = h.hemorragias === 1 || h.hemorragias === true;
        if (form.elements['embarazo']) form.elements['embarazo'].checked = h.embarazo === 1 || h.embarazo === true;
        if (form.elements['antecedentesFamiliares']) form.elements['antecedentesFamiliares'].value = h.antecedentes_familiares || '';
        if (form.elements['habitos']) form.elements['habitos'].value = h.habitos || '';
        if (form.elements['motivoPrimeraConsulta']) form.elements['motivoPrimeraConsulta'].value = h.motivo_primera_consulta || '';
        if (form.elements['grupoSanguineo']) form.elements['grupoSanguineo'].value = h.grupo_sanguineo || '';
        if (form.elements['presionArterial']) form.elements['presionArterial'].value = h.presion_arterial || '';
        if (form.elements['observacionesMedicas']) form.elements['observacionesMedicas'].value = h.observaciones_medicas || '';
    }

    // ============================================================
    // 2. ODONTOGRAMA INTERACTIVO
    // ============================================================
    function renderOdontograma() {
        // Mapear piezas existentes
        state.odontoPiecesMap.clear();
        (state.expediente?.piezasOdontograma || []).forEach(p => {
            const pieza = Number(p.numeroPieza);
            if (!state.odontoPiecesMap.has(pieza)) state.odontoPiecesMap.set(pieza, []);
            state.odontoPiecesMap.get(pieza).push(p);
        });
        state.odontoPending.forEach((finding, key) => {
            if (finding.condicion === 'SANO') {
                const list = state.odontoPiecesMap.get(finding.pieza) || [];
                state.odontoPiecesMap.set(finding.pieza, list.filter(p => p.superficie !== finding.superficie));
            } else {
                const list = (state.odontoPiecesMap.get(finding.pieza) || []).filter(p => p.superficie !== finding.superficie);
                list.push({numeroPieza: finding.pieza, superficie: finding.superficie, condicion: finding.condicion});
                state.odontoPiecesMap.set(finding.pieza, list);
            }
        });

        // Vista unificada: Dientes permanentes y de leche integrados anatómicamente
        const cuadrantes = [
            ['odonto-q1', [18,17,16,15,14,13,12,11], 'SUP'],
            ['odonto-q2', [21,22,23,24,25,26,27,28], 'SUP'],
            ['odonto-q5', [55,54,53,52,51], 'SUP'],
            ['odonto-q6', [61,62,63,64,65], 'SUP'],
            ['odonto-q8', [85,84,83,82,81], 'INF'],
            ['odonto-q7', [71,72,73,74,75], 'INF'],
            ['odonto-q4', [48,47,46,45,44,43,42,41], 'INF'],
            ['odonto-q3', [31,32,33,34,35,36,37,38], 'INF']
        ];
        cuadrantes.forEach(([id, piezas, arcada]) => {
            const destino = $(`#${id}`);
            if (destino) destino.innerHTML = piezas.map(p => crearToothBox(p, arcada)).join('');
        });
        $$('.odonto-row-adulto').forEach(el => el.hidden = false);
        $$('.odonto-row-nino').forEach(el => el.hidden = false);
        actualizarBotonesDenticion();

        // Listeners en superficies dentales SVG
        $$('.odonto-surface').forEach(surf => {
            surf.addEventListener('click', onSurfaceClick);
            surf.addEventListener('pointerenter', event => {
                if (event.buttons === 1 && state.pacienteId) onSurfaceClick(event);
            });
        });

        // Listeners en cabeceras de pieza (para seleccionar y para limpiar)
        $$('.odonto-tooth-box').forEach(box => {
            box.addEventListener('click', () => {
                const pieza = Number(box.dataset.pieza);
                seleccionarDienteEnOdontograma(pieza);
            });
            box.addEventListener('contextmenu', e => {
                e.preventDefault();
                const pieza = Number(box.dataset.pieza);
                if (!state.pacienteId) return;
                ['MESIAL','DISTAL','VESTIBULAR','PALATINO','LINGUAL','OCLUSAL','INCISAL','GENERAL'].forEach(superficie => {
                    state.odontoPending.set(`${pieza}:${superficie}`, {pieza, superficie, condicion:'SANO'});
                });
                state.selectedPiece = pieza;
                renderOdontograma();
                void guardarOdontograma2D();
            });
        });
        $$('.odonto-tooth-open').forEach(button => {
            button.addEventListener('click', e => {
                e.stopPropagation();
                const pieza = Number(button.dataset.pieza);
                seleccionarDienteEnOdontograma(pieza);
            });
        });

        renderHallazgosLista();
        actualizarColoresStudio3D();
        if (state.selectedPiece) {
            actualizarSidebarOdontograma(state.selectedPiece);
        }
        const reintentar = $('#btn-guardar-odontograma-2d');
        if (reintentar) reintentar.hidden = state.odontoPending.size === 0 || state.odontoSaving;
        const status = $('#odonto-2d-status');
        if (status) status.textContent = state.pacienteId
            ? state.odontoPending.size > 0
                ? `${state.odontoPending.size} marca(s) pendientes de guardar.`
                : 'Sin cambios pendientes. Selecciona una herramienta y toca una cara.'
            : 'Selecciona un paciente para editar el odontograma.';
    }

    function crearToothBox(pieza, arcada) {
        const findings = state.odontoPiecesMap.get(pieza) || [];
        const isGeneralCondition = findings.find(f => f.superficie === 'GENERAL');
        const boxClass = isGeneralCondition ? isGeneralCondition.condicion.toLowerCase() : '';

        // Determinar clases de cada superficie
        const getSurfClass = s => {
            const f = findings.find(x => x.superficie === s);
            return f ? f.condicion.toLowerCase() : '';
        };

        // En arcada superior, la cara de arriba es vestibular; en inferior, lingual.
        const topSurf = arcada === 'SUP' ? 'VESTIBULAR' : 'LINGUAL';
        const bottomSurf = arcada === 'SUP' ? 'PALATINO' : 'VESTIBULAR';

        const patientRight = [1,4,5,8].includes(Math.floor(pieza / 10));
        const leftSurface = patientRight ? 'DISTAL' : 'MESIAL', rightSurface = patientRight ? 'MESIAL' : 'DISTAL';
        const bitingSurface = pieza % 10 <= 3 ? 'INCISAL' : 'OCLUSAL';
        const cajaAnotacion = `<button type="button" class="odonto-marker odonto-surface ${getSurfClass('GENERAL')}"
            data-pieza="${pieza}" data-superficie="GENERAL" aria-label="Marcar pieza ${pieza} completa"></button>`;
        const numero = `<span class="odonto-tooth-number">${pieza}</span>`;
        const diente = `<button type="button" class="odonto-tooth-open" data-pieza="${pieza}" aria-label="Seleccionar diente ${pieza}">${siluetaOdontologica(pieza)}</button>`;
        const caras = `
            <svg class="odonto-tooth-svg" viewBox="0 0 44 44" aria-label="Superficies de la pieza ${pieza}">
                <polygon class="odonto-surface ${getSurfClass(topSurf)}" data-pieza="${pieza}" data-superficie="${topSurf}" points="0,0 44,0 32,12 12,12"/>
                <polygon class="odonto-surface ${getSurfClass(leftSurface)}" data-pieza="${pieza}" data-superficie="${leftSurface}" points="0,0 12,12 12,32 0,44"/>
                <polygon class="odonto-surface ${getSurfClass(bitingSurface)}" data-pieza="${pieza}" data-superficie="${bitingSurface}" points="12,12 32,12 32,32 12,32"/>
                <polygon class="odonto-surface ${getSurfClass(rightSurface)}" data-pieza="${pieza}" data-superficie="${rightSurface}" points="44,0 44,44 32,32 32,12"/>
                <polygon class="odonto-surface ${getSurfClass(bottomSurf)}" data-pieza="${pieza}" data-superficie="${bottomSurf}" points="12,32 32,32 44,44 0,44"/>
            </svg>`;
        const isSelected = Number(state.selectedPiece) === Number(pieza) ? 'selected' : '';
        return `
            <div class="odonto-tooth-box ${boxClass} ${isSelected} ${state.pacienteId ? '' : 'readonly'}" data-pieza="${pieza}" data-arcada="${arcada}" title="Pieza ${pieza} (clic para seleccionar, clic derecho para limpiar)">
                ${arcada === 'SUP' ? cajaAnotacion + numero + diente + caras : caras + diente + numero + cajaAnotacion}
            </div>
        `;
    }

    function siluetaOdontologica(pieza) {
        const posicion = pieza % 10;
        const corona = posicion >= 6
            ? 'M6 39 Q10 34 14 38 Q19 33 23 37 Q28 33 33 38 Q38 34 42 39 L40 51 Q24 57 8 51 Z'
            : posicion >= 4
            ? 'M9 39 Q16 33 23 38 Q31 33 38 39 L36 52 Q23 57 11 52 Z'
            : posicion === 3
                ? 'M10 39 L23 32 L36 39 L34 53 Q23 57 12 53 Z'
                : 'M13 38 Q23 35 33 38 L33 53 Q23 57 13 53 Z';
        const raices = posicion >= 6
            ? 'M9 39 Q6 23 5 4 L18 33 L23 8 L28 33 L42 4 Q40 25 37 39 Z'
            : posicion >= 4
                ? 'M12 39 Q11 23 13 6 L23 31 L34 6 Q35 24 34 39 Z'
                : 'M17 39 Q18 20 23 3 Q29 22 29 39 Z';
        return `<svg class="odonto-tooth-shape" viewBox="0 0 48 60" aria-hidden="true">
            <defs><linearGradient id="esmalte-${pieza}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fffef8"/><stop offset=".6" stop-color="#fff5db"/><stop offset="1" stop-color="#ead8ae"/></linearGradient></defs>
            <path class="odonto-tooth-root" d="${raices}"/>
            <path class="odonto-tooth-crown" d="${corona}" style="fill:url(#esmalte-${pieza})"/>
            <path class="odonto-tooth-detail" d="M15 45 Q23 48 32 45 M23 39 L23 51"/>
        </svg>`;
    }

    function onSurfaceClick(e) {
        if (state.odontoSaving) return;
        e.stopPropagation();
        const surf = e.currentTarget;
        const pieza = Number(surf.dataset.pieza);
        const superficie = surf.dataset.superficie;
        const tool = state.odontoTool;
        if (!state.pacienteId || !state.expediente?.odontograma?.id) {
            mostrarAviso('Selecciona un paciente antes de marcar el odontograma.', 'error');
            return;
        }

        // Si la herramienta es de pieza entera (CORONA, ENDODONCIA, AUSENTE, IMPLANTE)
        const cara = ['CORONA', 'ENDODONCIA', 'AUSENTE', 'IMPLANTE', 'BRACKETS'].includes(tool) ? 'GENERAL' : superficie;
        state.odontoPending.set(`${pieza}:${cara}`, {
            pieza, superficie: cara, condicion: tool === 'LIMPIAR' ? 'SANO' : tool
        });
        state.selectedPiece = pieza;
        renderOdontograma();
        void guardarOdontograma2D();
    }

    async function guardarOdontograma2D() {
        if (!state.pacienteId || !state.expediente?.odontograma?.id) {
            mostrarAviso('Selecciona un paciente antes de guardar.', 'error');
            return;
        }
        if (state.odontoSaving) return;
        if (state.odontoPending.size === 0) {
            mostrarAviso('No hay marcas nuevas por guardar.');
            return;
        }
        state.odontoSaving = true;
        const button = $('#btn-guardar-odontograma-2d');
        if (button) button.disabled = true;
        try {
            for (const [key, marca] of state.odontoPending) {
                await api('/interno/clinica/api/odontograma/hallazgo', {
                    method: 'POST',
                    body: {
                        odontogramaId: state.expediente.odontograma.id,
                        version: state.expediente.versionOdontograma,
                        numeroPieza: marca.pieza,
                        superficie: marca.superficie,
                        condicion: marca.condicion,
                        estado: marca.condicion === 'SANO' ? 'SANO' :
                                ['CARIES','AUSENTE'].includes(marca.condicion) ? 'PATOLOGICO' : 'REALIZADO',
                        notas: 'Marca registrada en el odontograma 2D.'
                    }
                });
                state.expediente.versionOdontograma = Number(state.expediente.versionOdontograma || 0) + 1;
                state.odontoPending.delete(key);
            }
            await cargarPaciente(state.pacienteId);
            mostrarAviso('Odontograma 2D guardado en el expediente del paciente.');
        } catch (error) {
            mostrarAviso(error.message || 'No se pudieron guardar las marcas.', 'error');
        } finally {
            state.odontoSaving = false;
            if (button) button.disabled = false;
            renderOdontograma();
        }
    }

    function nombrePieza(pieza) {
        const numero = pieza % 10;
        const infantil = pieza >= 50;
        const tipo = infantil
            ? ['','Incisivo central','Incisivo lateral','Canino','Primer molar','Segundo molar'][numero]
            : ['','Incisivo central','Incisivo lateral','Canino','Primer premolar','Segundo premolar','Primer molar','Segundo molar','Tercer molar'][numero];
        const cuadrante = Math.floor(pieza / 10);
        const arcada = [1,2,5,6].includes(cuadrante) ? 'superior' : 'inferior';
        const lado = [1,4,5,8].includes(cuadrante) ? 'derecho' : 'izquierdo';
        return `${tipo || 'Diente'} ${arcada} ${lado}`;
    }

    /**
     * Extrae y limpia únicamente el procedimiento correspondiente a una pieza específica
     * a partir de descripciones compuestas o multidentales (ej: "BRACKETS en pieza 11; BRACKETS en pieza 15").
     */
    function extraerProcedimientoParaPieza(textoCompleto, pieza) {
        if (!textoCompleto || typeof textoCompleto !== 'string') return '';
        const piezaStr = String(pieza).trim();
        if (!piezaStr) return textoCompleto;

        // Si el texto no menciona piezas ni números de piezas de forma explícita, se devuelve tal cual
        const mencionaPiezasGlobal = /(?:piezas?|pzas?|dientes?|#)\s*\d{1,2}|\b[1-8][1-8]\b/i.test(textoCompleto);
        if (!mencionaPiezasGlobal) {
            return textoCompleto.trim();
        }

        // Dividir en frases o ítems por ;, saltos de línea o viñetas
        const segmentos = textoCompleto
            .split(/(?:[\r\n]+|[;•·]+)/)
            .map(s => s.trim())
            .filter(Boolean);

        // Regex para detectar mención específica de esta pieza (ej: "pieza 15", "pza 15", "#15", o 15 delimitado)
        const regexEstaPieza = new RegExp(`(?:(?:piezas?|pzas?|dientes?|#)\\s*${piezaStr}\\b|\\b${piezaStr}\\b)`, 'i');
        // Regex para detectar mención de cualquier otra pieza dental
        const regexCualquierPieza = /(?:piezas?|pzas?|dientes?|#)\s*([1-8][1-8])|\b([1-8][1-8])\b/i;

        const coincidentes = [];
        segmentos.forEach(seg => {
            if (regexEstaPieza.test(seg)) {
                // Limpiar la referencia redundante a "en pieza XX", "pieza XX:", etc.
                let limpio = seg
                    .replace(new RegExp(`(?:\\s*(?:en|de|para)?\\s*(?:la\\s*)?(?:piezas?|pzas?|dientes?|#)\\s*[\\d\\s,;yyeal-]*\\b${piezaStr}\\b[\\d\\s,;yyeal-]*)`, 'gi'), ' ')
                    .replace(new RegExp(`^[\\s•\\-\\*:>]*(?:piezas?|pzas?|dientes?|#)?\\s*${piezaStr}\\s*[:\\-]\\s*`, 'gi'), ' ')
                    .replace(new RegExp(`\\b(?:piezas?|pzas?|dientes?|#)\\s*${piezaStr}\\b`, 'gi'), ' ')
                    .replace(/\s{2,}/g, ' ')
                    .replace(/^[\s,;:\-•]+|[\s,;:\-•]+$/g, '')
                    .trim();
                if (!limpio) limpio = seg.trim();
                coincidentes.push(limpio);
            }
        });

        if (coincidentes.length > 0) {
            return Array.from(new Set(coincidentes)).join('; ');
        }

        // Rango (ej: piezas 11 al 15)
        const regexRango = /piezas?\s*(\d{2})\s*(?:al?|a|-)\s*(\d{2})/i;
        const matchRango = textoCompleto.match(regexRango);
        if (matchRango) {
            const desde = parseInt(matchRango[1], 10);
            const hasta = parseInt(matchRango[2], 10);
            const pNum = parseInt(piezaStr, 10);
            if (pNum >= Math.min(desde, hasta) && pNum <= Math.max(desde, hasta)) {
                return textoCompleto.replace(regexRango, '').replace(/\s{2,}/g, ' ').replace(/^[\s,;:\-•]+|[\s,;:\-•]+$/g, '').trim();
            }
        }

        // Si los segmentos mencionan exclusivamente otras piezas y no a esta, devolver los no ajenos si existen
        const noAjenos = segmentos.filter(seg => !regexCualquierPieza.test(seg));
        if (noAjenos.length > 0) {
            return noAjenos.join('; ');
        }

        // Si todos los segmentos mencionaban piezas y tenían el mismo nombre base de procedimiento (ej: "BRACKETS en pieza 11; BRACKETS en pieza 12...")
        const nombresProcedimientos = segmentos.map(s => s.replace(/(?:en|de)?\s*(?:la\s*)?(?:piezas?|pzas?|dientes?|#)\s*\d{1,2}/gi, '').trim());
        if (nombresProcedimientos.length > 0 && nombresProcedimientos.every(p => p.toLowerCase() === nombresProcedimientos[0].toLowerCase())) {
            return nombresProcedimientos[0];
        }

        return textoCompleto.trim();
    }

    function actualizarDetallePieza() {
        const pieza = state.detallePieza;
        if (!pieza) return;
        const numero = pieza % 10;
        const infantil = pieza >= 50;
        const cuadranteNum = Math.floor(pieza / 10);
        const arcadaStr = [1, 2, 5, 6].includes(cuadranteNum) ? 'Arcada Superior (Maxilar)' : 'Arcada Inferior (Mandibular)';

        const cuadrantesMap = {
            1: 'Cuadrante 1 (Superior derecho)',
            2: 'Cuadrante 2 (Superior izquierdo)',
            3: 'Cuadrante 3 (Inferior izquierdo)',
            4: 'Cuadrante 4 (Inferior derecho)',
            5: 'Cuadrante 5 (Superior derecho deciduo)',
            6: 'Cuadrante 6 (Superior izquierdo deciduo)',
            7: 'Cuadrante 7 (Inferior izquierdo deciduo)',
            8: 'Cuadrante 8 (Inferior derecho deciduo)'
        };

        const gruposMap = infantil
            ? ['', 'Incisivo central temporal', 'Incisivo lateral temporal', 'Canino temporal', 'Primer molar temporal', 'Segundo molar temporal']
            : ['', 'Incisivo central', 'Incisivo lateral', 'Canino', 'Primer premolar', 'Segundo premolar', 'Primer molar', 'Segundo molar', 'Tercer molar'];

        const funcionesMap = infantil
            ? {
                1: 'Corte de alimentos y estética de la dentición temporal.',
                2: 'Corte y guía incisal temporal.',
                3: 'Desgarre de alimentos y soporte canino.',
                4: 'Masticación y trituración temporal.',
                5: 'Masticación potente y mantenimiento de espacio.'
            }
            : {
                1: 'Corte de alimentos, estética facial y fonación anterior.',
                2: 'Corte de precisión y soporte estético lateral.',
                3: 'Desgarre de alimentos y soporte de la guía canina.',
                4: 'Trituración inicial de alimentos.',
                5: 'Trituración y transferencia hacia la zona molar.',
                6: 'Masticación y trituración principal, clave de oclusión.',
                7: 'Masticación potente y soporte oclusal posterior.',
                8: 'Molar distal / erupción de tercer molar.'
            };

        const caraOclusalIncisal = numero <= 3 ? 'Incisal' : 'Oclusal';
        const caraPalatinaLingual = [1, 2, 5, 6].includes(cuadranteNum) ? 'Palatina' : 'Lingual';
        const carasStr = `Vestibular · ${caraPalatinaLingual} · Mesial · Distal · ${caraOclusalIncisal}`;
        const carasArray = ['Vestibular', caraPalatinaLingual, 'Mesial', 'Distal', caraOclusalIncisal];

        // Cabecera del modal
        $('#detalle-pieza-titulo').textContent = `Diente ${pieza} · ${nombrePieza(pieza)}`;
        $('#detalle-pieza-denticion').textContent = infantil ? 'Diente de leche · Ficha infantil' : 'Diente permanente · Ficha adulta';
        if ($('#detalle-pieza-badge-num')) $('#detalle-pieza-badge-num').textContent = pieza;
        if ($('#detalle-pieza-tipo-badge')) $('#detalle-pieza-tipo-badge').textContent = infantil ? 'Dentición de leche' : 'Dentición permanente';
        if ($('#detalle-pieza-num-notice')) $('#detalle-pieza-num-notice').textContent = pieza;

        // 1. Sección: Información anatómica de este diente
        if ($('#detalle-pieza-arcada')) $('#detalle-pieza-arcada').textContent = arcadaStr;
        if ($('#detalle-pieza-cuadrante')) $('#detalle-pieza-cuadrante').textContent = cuadrantesMap[cuadranteNum] || `Cuadrante ${cuadranteNum}`;
        if ($('#detalle-pieza-grupo')) $('#detalle-pieza-grupo').textContent = gruposMap[numero] || 'Pieza dental';
        if ($('#detalle-pieza-funcion')) $('#detalle-pieza-funcion').textContent = funcionesMap[numero] || 'Función masticatoria y soporte.';
        if ($('#detalle-pieza-caras')) $('#detalle-pieza-caras').textContent = carasStr;

        const carasContainer = $('#detalle-pieza-caras-chips');
        if (carasContainer) {
            carasContainer.innerHTML = carasArray.map(c =>
                `<span class="odonto-cara-chip"><i class="bi bi-circle-fill me-1"></i>${escape(c)}</span>`
            ).join('');
        }

        // 2. Sección: Estado actual en que se encuentra la pieza
        const marcas = state.odontoPiecesMap.get(pieza) || [];
        const badgeColors = {
            'CARIES': 'bg-danger-subtle text-danger border-danger-subtle',
            'OBTURACION': 'bg-primary-subtle text-primary border-primary-subtle',
            'SELLANTE': 'bg-success-subtle text-success border-success-subtle',
            'CORONA': 'bg-warning-subtle text-warning-emphasis border-warning-subtle',
            'ENDODONCIA': 'bg-purple-subtle text-purple border-purple-subtle',
            'BRACKETS': 'bg-teal-subtle text-teal border-teal-subtle',
            'AUSENTE': 'bg-secondary-subtle text-secondary border-secondary-subtle',
            'IMPLANTE': 'bg-info-subtle text-info border-info-subtle'
        };

        const marcasContainer = $('#detalle-pieza-marcas');
        if (marcasContainer) {
            if (marcas.length > 0) {
                marcasContainer.innerHTML = marcas.map(m => {
                    const colorClass = badgeColors[m.condicion] || 'bg-light text-dark border';
                    return `<span class="badge ${colorClass} me-1 mb-1 fw-bold py-1.5 px-2.5 rounded-pill">${escape(m.condicion)} · ${escape(m.superficie || 'GENERAL')}</span>`;
                }).join('');
            } else {
                marcasContainer.innerHTML = `
                    <div class="alert alert-success-subtle border border-success-subtle text-success d-flex align-items-center gap-2 py-2 px-2.5 mb-0 rounded-3 small">
                        <i class="bi bi-check-circle-fill"></i>
                        <span>Pieza sana · Sin marcas ni afecciones registradas.</span>
                    </div>`;
            }
        }

        // Diagnósticos vigentes asociados a esta pieza
        const diagsPieza = (state.expediente?.diagnosticos || []).filter(d => Number(d.numeroPieza) === pieza);
        const diagsContainer = $('#detalle-pieza-diagnosticos');
        if (diagsContainer) {
            if (diagsPieza.length > 0) {
                diagsContainer.innerHTML = diagsPieza.map(d => `
                    <div class="p-2 mb-1.5 rounded-2 border bg-light-subtle d-flex align-items-start gap-2">
                        <i class="bi bi-clipboard2-pulse text-danger fs-6 mt-0.5"></i>
                        <div>
                            <strong class="text-dark small d-block">${escape(d.descripcion)}</strong>
                            <small class="text-muted" style="font-size:0.75rem;">CIE-10: ${escape(d.codigoCie10 || 'N/A')} · Estado: ${escape(d.estado || 'ACTIVO')}</small>
                        </div>
                    </div>
                `).join('');
            } else {
                diagsContainer.innerHTML = '<span class="text-secondary small fst-italic">Sin diagnósticos vigentes para esta pieza.</span>';
            }
        }

        // 3. Sección: Historial de tratamientos realizados (exclusivos de esta pieza, sin modelado 3D)
        const eventosPieza = [];

        // Atenciones clínicas donde se trató esta pieza
        const atencionesPieza = (state.expediente?.atenciones || []).filter(a => {
            const strPiezas = String(a.piezasTratadas || '');
            return strPiezas.split(/[\s,;]+/).some(p => p.trim() === String(pieza));
        });
        atencionesPieza.forEach(a => {
            const procLimpio = extraerProcedimientoParaPieza(a.planProcedimiento, pieza) || a.planProcedimiento || 'Atención clínica realizada';
            const detalleLimpio = extraerProcedimientoParaPieza(a.subjetivo || a.objetivo || '', pieza) || a.subjetivo || a.objetivo || '';
            eventosPieza.push({
                tipo: 'atencion',
                cardClass: '',
                iconBgClass: '',
                titulo: procLimpio,
                profesional: a.nombreProfesional || a.creadoPor || 'Especialista',
                detalle: detalleLimpio,
                receta: a.receta || '',
                fecha: a.fechaAtencion,
                icono: 'bi-journal-check',
                badge: 'Atención Realizada',
                badgeClass: 'bg-primary-subtle text-primary border border-primary-subtle'
            });
        });

        // Procedimientos en Planes de Tratamiento
        (state.expediente?.planes || []).forEach(p => {
            (p.items || []).filter(it => Number(it.numeroPieza) === pieza).forEach(it => {
                const procLimpio = extraerProcedimientoParaPieza(it.descripcion, pieza) || it.descripcion || 'Procedimiento en Plan';
                eventosPieza.push({
                    tipo: 'plan',
                    cardClass: 'card-plan',
                    iconBgClass: 'bg-plan',
                    titulo: procLimpio,
                    profesional: p.profesional || p.creadoPor || '',
                    detalle: `Plan: ${p.titulo || 'Plan Integral'} · Fase ${it.ordenFase || 1} · Costo: S/ ${Number(it.precio || 0).toFixed(2)}`,
                    receta: '',
                    fecha: p.creadoEn,
                    icono: 'bi-list-check',
                    badge: `Plan [${p.estado || 'REGISTRADO'}]`,
                    badgeClass: 'bg-success-subtle text-success border border-success-subtle'
                });
            });
        });

        // Notas clínicas reales escritas por el odontólogo (EXCLUYENDO totalmente cualquier ajuste de modelado 3D o auditoría técnica)
        (state.expediente?.historialOdontograma || []).filter(h => {
            if (Number(h.numeroPieza) !== pieza) return false;
            const accion = String(h.accion || '').toUpperCase();
            const condicion = String(h.condicion || '').toUpperCase();
            const notas = String(h.notas || '').toUpperCase();
            // Descartar cualquier registro técnico del modelado 3D
            if (accion.includes('3D') || condicion.includes('3D') || notas.includes('3D') ||
                notas.includes('INCLINACIÓN') || notas.includes('INCLINACION') || accion === 'AJUSTE_3D') {
                return false;
            }
            // Solo considerar notas clínicas de texto
            return accion === 'NOTA' && Boolean(h.notas && h.notas.trim());
        }).forEach(h => {
            eventosPieza.push({
                tipo: 'odonto_hist',
                cardClass: 'card-nota',
                iconBgClass: 'bg-nota',
                titulo: 'Nota clínica del odontólogo',
                profesional: h.usuario || '',
                detalle: h.notas,
                receta: '',
                fecha: h.fecha,
                icono: 'bi-sticky',
                badge: 'Nota Clínica',
                badgeClass: 'bg-info-subtle text-info-emphasis border border-info-subtle'
            });
        });

        // Ordenar cronológicamente (más reciente primero)
        eventosPieza.sort((a, b) => {
            const tA = a.fecha ? new Date(a.fecha).getTime() : 0;
            const tB = b.fecha ? new Date(b.fecha).getTime() : 0;
            return tB - tA;
        });

        const conteoHist = $('#detalle-pieza-conteo-historial');
        if (conteoHist) {
            conteoHist.textContent = `${eventosPieza.length} tratamiento(s)`;
        }

        const histContainer = $('#detalle-pieza-historial');
        if (histContainer) {
            if (eventosPieza.length === 0) {
                histContainer.innerHTML = `
                    <div class="text-center py-5 text-secondary">
                        <i class="bi bi-clock-history d-block fs-2 mb-2 text-muted opacity-50"></i>
                        <span class="small">Sin tratamientos ni atenciones clínicas registradas previamente en esta pieza.</span>
                    </div>`;
            } else {
                histContainer.innerHTML = eventosPieza.map(ev => `
                    <div class="odonto-detail-history-card ${ev.cardClass || ''}">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <span class="badge ${ev.badgeClass} small fw-bold px-2 py-1 rounded-pill">${escape(ev.badge)}</span>
                            ${ev.fecha ? `<span class="odonto-history-date"><i class="bi bi-calendar3 me-1 text-teal"></i>${formatDateTime(ev.fecha)}</span>` : ''}
                        </div>
                        <div class="d-flex align-items-start gap-2.5 mb-2">
                            <div class="odonto-history-icon-circle ${ev.iconBgClass || ''}">
                                <i class="bi ${ev.icono}"></i>
                            </div>
                            <div class="flex-grow-1">
                                <h5 class="odonto-history-title">${escape(ev.titulo)}</h5>
                                ${ev.profesional ? `
                                    <div class="odonto-history-doctor">
                                        <i class="bi bi-person-fill"></i>
                                        <span>Atendido por:</span>
                                        <strong>${escape(ev.profesional)}</strong>
                                    </div>` : ''}
                            </div>
                        </div>
                        ${ev.detalle ? `
                            <div class="odonto-history-detail-box">
                                <div class="odonto-history-detail-label"><i class="bi bi-chat-left-text me-1"></i>Detalle clínico:</div>
                                <div class="odonto-history-detail-content">${escape(ev.detalle)}</div>
                            </div>` : ''}
                        ${ev.receta ? `
                            <div class="odonto-history-rx-box">
                                <div class="odonto-history-rx-label"><i class="bi bi-prescription2 me-1"></i>Indicación / Receta:</div>
                                <div class="odonto-history-rx-content">${escape(ev.receta)}</div>
                            </div>` : ''}
                    </div>
                `).join('');
            }
        }
    }

    async function abrirDetallePieza(pieza) {
        state.detallePieza = pieza;
        seleccionarDienteEnOdontograma(pieza);
        actualizarDetallePieza();
        bootstrap.Modal.getOrCreateInstance($('#modal-detalle-pieza')).show();
    }

    // ============================================================
    // PANEL LATERAL UNIFICADO DEL ODONTOGRAMA (WORKFLOW CENTRAL)
    // ============================================================
    function actualizarBotonesDenticion() {
        const btnTodos = $('#btn-odonto-todos');
        const btnAdulto = $('#btn-odonto-adulto');
        const btnNino = $('#btn-odonto-nino');
        if (btnTodos) {
            btnTodos.classList.toggle('active', true);
            btnTodos.setAttribute('aria-pressed', 'true');
        }
        if (btnAdulto) btnAdulto.classList.remove('active');
        if (btnNino) btnNino.classList.remove('active');
    }

    function seleccionarDienteEnOdontograma(pieza) {
        if (!pieza) return;
        pieza = Number(pieza);
        state.selectedPiece = pieza;
        $$('.odonto-tooth-box').forEach(box => {
            if (Number(box.dataset.pieza) === pieza) {
                box.classList.add('selected');
            } else {
                box.classList.remove('selected');
            }
        });
        actualizarSidebarOdontograma(state.selectedPiece);

        // En pantallas móviles (< 992px), abrir el drawer/bottom-sheet de la pieza automáticamente
        if (window.innerWidth < 992) {
            const panel = $('#odonto-sidebar-panel');
            if (panel) panel.classList.add('mobile-open');
            const backdrop = $('#odonto-sidebar-backdrop');
            if (backdrop) backdrop.hidden = false;
        }
    }

    function actualizarSidebarOdontograma(pieza) {
        if (!pieza) return;
        pieza = Number(pieza);
        const infantil = pieza >= 50;
        const cuadranteNum = Math.floor(pieza / 10);
        const arcadaStr = [1, 2, 5, 6].includes(cuadranteNum) ? 'Arcada Superior (Maxilar)' : 'Arcada Inferior (Mandibular)';
        const cuadrantesMap = {
            1: 'Cuadrante 1 (Sup. Der.)',
            2: 'Cuadrante 2 (Sup. Izq.)',
            3: 'Cuadrante 3 (Inf. Izq.)',
            4: 'Cuadrante 4 (Inf. Der.)',
            5: 'Cuadrante 5 (Temporal Sup. Der.)',
            6: 'Cuadrante 6 (Temporal Sup. Izq.)',
            7: 'Cuadrante 7 (Temporal Inf. Izq.)',
            8: 'Cuadrante 8 (Temporal Inf. Der.)'
        };

        // 1. Cabecera de Pieza
        const badgeEl = $('#sidebar-pieza-badge');
        if (badgeEl) badgeEl.textContent = pieza;
        const nombreEl = $('#sidebar-pieza-nombre');
        if (nombreEl) nombreEl.textContent = nombrePieza(pieza);
        const metaEl = $('#sidebar-pieza-meta');
        if (metaEl) metaEl.textContent = `${arcadaStr} · ${cuadrantesMap[cuadranteNum] || ''}`;
        const tipoBadgeEl = $('#sidebar-pieza-tipo-badge');
        if (tipoBadgeEl) {
            tipoBadgeEl.textContent = infantil ? 'Diente de leche (Niño)' : 'Diente permanente (Adulto)';
            tipoBadgeEl.className = infantil
                ? 'badge bg-teal-soft text-teal border border-teal-subtle px-2 py-1'
                : 'badge bg-light text-secondary border px-2 py-1';
        }

        // 2. Estado Odontológico (Hallazgos en superficies)
        const hallazgosEl = $('#sidebar-pieza-hallazgos');
        const marcas = state.odontoPiecesMap.get(pieza) || [];
        const badgeColors = {
            'CARIES': 'bg-danger-subtle text-danger border border-danger-subtle',
            'OBTURACION': 'bg-primary-subtle text-primary border border-primary-subtle',
            'SELLANTE': 'bg-success-subtle text-success border border-success-subtle',
            'CORONA': 'bg-warning-subtle text-warning-emphasis border border-warning-subtle',
            'ENDODONCIA': 'bg-purple-subtle text-purple border border-purple-subtle',
            'BRACKETS': 'bg-teal-subtle text-teal border border-teal-subtle',
            'AUSENTE': 'bg-secondary-subtle text-secondary border border-secondary-subtle',
            'IMPLANTE': 'bg-info-subtle text-info border border-info-subtle'
        };

        if (hallazgosEl) {
            if (marcas.length > 0) {
                hallazgosEl.innerHTML = marcas.map(m => {
                    const colorClass = badgeColors[m.condicion] || 'bg-light text-dark border';
                    const icon = m.condicion === 'CARIES' ? 'bi-circle-fill text-danger' :
                                 m.condicion === 'OBTURACION' ? 'bi-circle-fill text-primary' :
                                 m.condicion === 'CORONA' ? 'bi-gem text-warning' :
                                 m.condicion === 'ENDODONCIA' ? 'bi-lightning-charge text-purple' :
                                 m.condicion === 'IMPLANTE' ? 'bi-shield-plus text-info' :
                                 m.condicion === 'AUSENTE' ? 'bi-x-circle text-secondary' : 'bi-pin-fill';
                    return `<span class="badge ${colorClass} py-1.5 px-2.5 rounded-pill fw-bold" style="font-size: 0.75rem;"><i class="bi ${icon} me-1" style="font-size: 0.65rem;"></i>${escape(m.condicion)} · ${escape(m.superficie || 'GENERAL')}</span>`;
                }).join('');
            } else {
                hallazgosEl.innerHTML = `
                    <div class="alert alert-success-subtle border border-success-subtle text-success py-1.5 px-2.5 rounded-2 small mb-0 w-100">
                        <i class="bi bi-check-circle-fill me-1"></i>Pieza sana · Sin marcas ni patologías registradas
                    </div>`;
            }
        }

        // Sugerir tratamiento adecuado en el selector de cotización si no se ha modificado
        const selectTrat = $('#sidebar-select-tratamiento');
        const inputPrecio = $('#sidebar-input-precio');
        const customTrat = $('#sidebar-custom-tratamiento');
        if (selectTrat && customTrat && customTrat.hidden) {
            const condicion = marcas.find(m => m.condicion !== 'SANO')?.condicion || '';
            const patrones = {CARIES:/resina/i, OBTURACION:/resina/i, ENDODONCIA:/endo|centrales|molares/i, CORONA:/corona|porcelana|circonio/i, SELLANTE:/sellante/i, BRACKETS:/instalación.*brackets|ortodoncia/i, AUSENTE:/implante/i, IMPLANTE:/implante/i};
            if (selectTrat.dataset.pieza !== String(pieza)) {
                const opcion = Array.from(selectTrat.options).find(o => patrones[condicion]?.test(o.value));
                if (opcion) selectTrat.value = opcion.value;
                selectTrat.dataset.pieza = String(pieza);
                if (inputPrecio) inputPrecio.value = selectTrat.selectedOptions[0]?.dataset.precio || '';
            }
        }

        // 3. Acción Directa: Atender Pieza Ahora
        const btnAtenderDirecto = $('#btn-sidebar-atender-directo');
        const txtAtenderDirecto = $('#btn-sidebar-atender-texto');
        if (txtAtenderDirecto) txtAtenderDirecto.textContent = `Atender Pieza ${pieza}`;
        if (btnAtenderDirecto) {
            btnAtenderDirecto.onclick = () => {
                const procedimiento = selectTrat?.value === 'OTRO' ? customTrat?.value?.trim() : selectTrat?.value;
                abrirModalAtencionConPieza(pieza, procedimiento || '', '', '', null);
                const importe = $('#atencion-importe');
                if (importe) importe.value = inputPrecio?.value || '';
            };
        }

        // 4. Expediente de la Pieza (Resumen compacto sin duplicar las listas de las pestañas superiores)
        const planItemsPieza = [];
        (state.expediente?.planes || []).forEach(plan => {
            (plan.items || []).forEach(it => {
                if (Number(it.numeroPieza) === pieza || String(it.descripcion || '').includes(String(pieza))) {
                    planItemsPieza.push({ item: it, plan: plan });
                }
            });
        });

        const atencionesPieza = (state.expediente?.atenciones || []).filter(a => {
            const strPiezas = String(a.piezasTratadas || '');
            return strPiezas.split(/[\s,;]+/).some(p => p.trim() === String(pieza));
        });

        const badgesEl = $('#sidebar-resumen-badges');
        if (badgesEl) {
            badgesEl.textContent = `${planItemsPieza.length} plan(es) · ${atencionesPieza.length} atención(es)`;
        }

        const resumenInfoEl = $('#sidebar-pieza-resumen-info');
        if (resumenInfoEl) {
            let htmlResumen = '';
            // Si hay un item aprobado en plan pendiente de atender
            const itemAprobado = planItemsPieza.find(p => p.plan.estado === 'APROBADO' && p.item.estado !== 'REALIZADO');
            const itemCotizado = planItemsPieza.find(p => p.plan.estado === 'BORRADOR' || p.plan.estado === 'PROPUESTO');

            if (itemAprobado) {
                const safeDesc = escape(itemAprobado.item.descripcion).replace(/'/g, "\\'");
                htmlResumen += `
                    <div class="p-2 mb-2 rounded-2 bg-success-subtle border border-success-subtle">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="badge bg-success text-white small fw-bold">Plan #${itemAprobado.plan.id} Aprobado</span>
                            <strong class="text-teal small">${money(itemAprobado.item.precio)}</strong>
                        </div>
                        <div class="fw-semibold text-dark small mb-1.5">${escape(itemAprobado.item.descripcion)}</div>
                        <button type="button" class="btn btn-xs btn-primary w-100 fw-bold shadow-xs py-1" onclick="window.DentiaClinica.atenderProcedimientoDesdeSidebar(${itemAprobado.item.id}, ${pieza}, '${safeDesc}')">
                            <i class="bi bi-heart-pulse-fill me-1"></i> Atender este ítem de Plan
                        </button>
                    </div>
                `;
            } else if (itemCotizado) {
                htmlResumen += `
                    <div class="p-2 mb-2 rounded-2 bg-warning-subtle border border-warning-subtle text-dark">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="badge bg-warning text-dark border border-warning-subtle small fw-bold">Cotizado en Plan #${itemCotizado.plan.id}</span>
                            <strong class="small">${money(itemCotizado.item.precio)}</strong>
                        </div>
                        <div class="small">${escape(itemCotizado.item.descripcion)}</div>
                    </div>
                `;
            }

            if (atencionesPieza.length > 0) {
                const ultAtencion = atencionesPieza[0];
                const procLimpio = extraerProcedimientoParaPieza(ultAtencion.planProcedimiento, pieza) || ultAtencion.planProcedimiento || 'Atención clínica';
                htmlResumen += `
                    <div class="d-flex align-items-start gap-1.5 mt-1">
                        <i class="bi bi-check-circle-fill text-success mt-0.5" style="font-size: 0.85rem;"></i>
                        <div>
                            <div class="fw-bold text-dark" style="font-size: 0.78rem;">Última atención: ${escape(procLimpio)}</div>
                            <small class="text-muted" style="font-size: 0.72rem;"><i class="bi bi-calendar3 me-1"></i>${formatDateTime(ultAtencion.fechaAtencion)} ${ultAtencion.nombreProfesional ? `· Dr(a). ${escape(ultAtencion.nombreProfesional)}` : ''}</small>
                        </div>
                    </div>
                `;
            }

            if (!htmlResumen) {
                htmlResumen = `<div class="text-muted fst-italic py-1"><i class="bi bi-info-circle me-1"></i>Sin planes ni atenciones previas en esta pieza.</div>`;
            }

            resumenInfoEl.innerHTML = htmlResumen;
        }

        // Conectar botones para ir a las pestañas oficiales superiores
        const btnPlanes = $('#btn-sidebar-ver-planes');
        if (btnPlanes) {
            btnPlanes.onclick = () => {
                state.activeModulo = 'plan-tratamiento';
                renderModuloActual();
            };
        }
        const btnAtenciones = $('#btn-sidebar-ver-atenciones');
        if (btnAtenciones) {
            btnAtenciones.onclick = () => {
                state.activeModulo = 'atenciones';
                renderModuloActual();
            };
        }
    }

    async function cotizarTratamientoEnPlanDesdeSidebar() {
        if (!state.pacienteId) {
            mostrarAviso('Selecciona un paciente antes de cotizar un tratamiento.', 'warning');
            return;
        }
        const pieza = state.selectedPiece || 16;
        const select = $('#sidebar-select-tratamiento');
        const customInput = $('#sidebar-custom-tratamiento');
        const precioInput = $('#sidebar-input-precio');
        const btn = $('#btn-sidebar-cotizar-plan');

        let tratamientoNombre = select?.value;
        if (tratamientoNombre === 'OTRO') {
            tratamientoNombre = customInput?.value?.trim();
            if (!tratamientoNombre) {
                mostrarAviso('Por favor escribe el nombre del procedimiento personalizado.', 'error');
                customInput?.focus();
                return;
            }
        }
        const precioVal = Number(precioInput?.value);
        if (!Number.isFinite(precioVal) || precioVal <= 0) {
            precioInput?.classList.add('is-invalid');
            mostrarAviso('Ingresa un precio mayor que cero para cotizar.', 'error');
            precioInput?.focus();
            return;
        }
        precioInput?.classList.remove('is-invalid');

        // Buscar si ya existe un plan en borrador o propuesto
        const planBorrador = (state.expediente?.planes || []).find(p => p.estado === 'PROPUESTO' || p.estado === 'BORRADOR');

        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span>Cotizando...';

        try {
            if (planBorrador) {
                // Agregar al plan existente
                const itemsActualizados = (planBorrador.items || []).map(it => ({
                    id: it.id,
                    tratamientoCatalogoId: it.tratamientoCatalogoId || null,
                    diagnosticoId: it.diagnosticoId || null,
                    numeroPieza: it.numeroPieza ? Number(it.numeroPieza) : null,
                    descripcion: it.descripcion,
                    precio: Number(it.precio || 0),
                    ordenFase: it.ordenFase || 1,
                    estado: it.estado || 'PENDIENTE'
                }));
                itemsActualizados.push({
                    tratamientoCatalogoId: null,
                    diagnosticoId: null,
                    numeroPieza: Number(pieza),
                    descripcion: `${tratamientoNombre} (Pieza ${pieza})`,
                    precio: precioVal,
                    ordenFase: 1,
                    estado: 'PENDIENTE'
                });

                await api(`/interno/clinica/api/planes?id=${planBorrador.id}`, {
                    method: 'POST',
                    body: {
                        pacienteId: state.pacienteId,
                        titulo: planBorrador.titulo || 'Plan de Tratamiento Integral',
                        estado: 'PROPUESTO',
                        notas: planBorrador.notas || '',
                        items: itemsActualizados
                    }
                });
            } else {
                // Crear nuevo plan propuesto
                await api('/interno/clinica/api/planes', {
                    method: 'POST',
                    body: {
                        pacienteId: state.pacienteId,
                        titulo: 'Plan de Tratamiento Integral',
                        estado: 'PROPUESTO',
                        notas: 'Cotización generada desde Odontograma.',
                        items: [{
                            tratamientoCatalogoId: null,
                            diagnosticoId: null,
                            numeroPieza: Number(pieza),
                            descripcion: `${tratamientoNombre} (Pieza ${pieza})`,
                            precio: precioVal,
                            ordenFase: 1,
                            estado: 'PENDIENTE'
                        }]
                    }
                });
            }

            await cargarPaciente(state.pacienteId);
            mostrarAviso(`"${tratamientoNombre}" cotizado en el Plan (estado PROPUESTO).`);
            seleccionarDienteEnOdontograma(pieza);
        } catch (e) {
            mostrarAviso(e.message || 'No se pudo agregar la cotización al plan.', 'error');
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-plus-circle me-1"></i> Cotizar en Plan';
            }
        }
    }

    async function aprobarPlanDesdeSidebar(planId) {
        if (!state.pacienteId) return;
        try {
            const deudRes = await api(`/interno/clinica/api/planes/${planId}/aprobar-deuda`, { method: 'POST' });
            await cargarPaciente(state.pacienteId);
            mostrarAviso(`¡Plan aprobado! Se ha generado la deuda de ${money(deudRes.total)} en la cuenta del paciente.`);
            if (state.selectedPiece) {
                seleccionarDienteEnOdontograma(state.selectedPiece);
            }
        } catch (e) {
            mostrarAviso(e.message || 'No se pudo aprobar el plan.', 'error');
        }
    }

    function atenderProcedimientoDesdeSidebar(itemId, pieza, descripcion) {
        abrirModalAtencionConPieza(pieza, descripcion, '', '', itemId);
    }

    // Nota: La evolución del odontograma tras registrar atención clínica se ejecuta
    // directamente y de manera atómica en el backend (AtencionClinicaService.java).


    async function aplicarCondicionPieza(pieza, superficie, condicion) {
        const odontoId = state.expediente.odontograma?.id;
        try {
            const estado = condicion === 'OBTURACION' || condicion === 'SELLANTE' ? 'REALIZADO' : 'PATOLOGICO';
            await api('/interno/clinica/api/odontograma/hallazgo', {
                method: 'POST',
                body: {
                    odontogramaId: odontoId,
                    version: state.expediente.versionOdontograma,
                    numeroPieza: pieza,
                    superficie: superficie,
                    condicion: condicion,
                    estado: estado,
                    notas: `Registrado desde odontograma interactivo.`
                }
            });

            // Recargar datos y refrescar
            await cargarPaciente(state.pacienteId);
            mostrarAviso(`Pieza ${pieza} (${superficie}) actualizada con: ${condicion}.`);

            // Si se marcó caries, sugerir creación de diagnóstico y tratamiento
            if (condicion === 'CARIES') {
                sugerirTratamientoDesdeCaries(pieza, superficie);
            }
        } catch (e) {
            mostrarAviso(e.message, 'error');
        }
    }

    async function limpiarPieza(pieza) {
        const odontoId = state.expediente.odontograma?.id;
        if (!odontoId) return;
        try {
            await api('/interno/clinica/api/odontograma/limpiar-pieza', {
                method: 'POST',
                body: { odontogramaId: odontoId, numeroPieza: pieza, version: state.expediente.versionOdontograma }
            });
            await cargarPaciente(state.pacienteId);
            mostrarAviso(`Pieza ${pieza} limpiada por completo.`);
        } catch (e) {
            mostrarAviso(e.message, 'error');
        }
    }

    function sugerirTratamientoDesdeCaries(pieza, superficie) {
        const banner = $('#odonto-sugerencia');
        if (!banner) return;
        banner.innerHTML = `
            <div class="alert alert-warning border shadow-sm d-flex align-items-center justify-content-between p-3 mt-3">
                <div class="d-flex align-items-center gap-2">
                    <i class="bi bi-magic fs-4 text-warning"></i>
                    <div>
                        <strong>¿Deseas agregar Diagnóstico y Plan para la Pieza ${pieza}?</strong>
                        <div class="small text-secondary">Se detectó caries en cara ${superficie.toLowerCase()}. Puedes derivarla en 1 click.</div>
                    </div>
                </div>
                <div class="d-flex gap-2">
                    <button class="btn btn-sm btn-light" onclick="this.closest('.alert').remove()">Descartar</button>
                    <button class="btn btn-sm btn-primary" id="btn-crear-diag-rapido">Crear Diagnóstico y Añadir a Plan</button>
                </div>
            </div>
        `;
        banner.hidden = false;
        $('#btn-crear-diag-rapido')?.addEventListener('click', async () => {
            try {
                // Registrar diagnóstico
                await api('/interno/clinica/api/diagnosticos', {
                    method: 'POST',
                    body: {
                        pacienteId: state.pacienteId,
                        codigoCie10: 'K02.1',
                        descripcion: `Caries de dentina en pieza ${pieza} (cara ${superficie.toLowerCase()})`,
                        numeroPieza: pieza,
                        gravedad: 'MODERADA',
                        estado: 'ACTIVO'
                    }
                });
                banner.remove();
                await cargarPaciente(state.pacienteId);
                mostrarAviso(`Diagnóstico vinculado a pieza ${pieza} registrado exitosamente.`);
            } catch (err) {
                mostrarAviso(err.message, 'error');
            }
        });
    }

    function renderHallazgosLista() {
        const tbody = $('#odonto-hallazgos-body');
        if (!tbody) return;
        const list = state.expediente?.piezasOdontograma || [];
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-secondary py-3">No hay afecciones marcadas en este odontograma. Haz clic en las piezas para registrarlas.</td></tr>';
            return;
        }
        tbody.innerHTML = list.map(h => `
            <tr>
                <td><strong>Pieza ${h.numeroPieza}</strong></td>
                <td><span class="badge text-bg-light border">${h.superficie}</span></td>
                <td><span class="badge ${h.condicion === 'CARIES' ? 'text-bg-danger' : (h.condicion === 'OBTURACION' ? 'text-bg-primary' : 'text-bg-warning')}">${h.condicion}</span></td>
                <td><span class="clinica-badge ${String(h.estado).toLowerCase()}">${h.estado}</span></td>
                <td class="text-end">
                    <button class="btn btn-light btn-sm btn-limpiar-pieza" data-pieza="${h.numeroPieza}" title="Limpiar pieza"><i class="bi bi-trash text-danger"></i></button>
                </td>
            </tr>
        `).join('');

        $$('.btn-limpiar-pieza').forEach(btn => {
            btn.addEventListener('click', () => limpiarPieza(Number(btn.dataset.pieza)));
        });
    }

    function obtenerHallazgosOdontogramaActual() {
        const piezasSet = new Set();
        const procedimientos = [];

        // 1. Revisar marcas pendientes de guardado en la sesión
        if (state.odontoPending && state.odontoPending.size > 0) {
            state.odontoPending.forEach((finding) => {
                if (finding.pieza && finding.condicion !== 'SANO') {
                    piezasSet.add(Number(finding.pieza));
                    const condNombre = finding.condicion === 'OBTURACION' ? 'Resina / Obturación' : finding.condicion;
                    procedimientos.push(`${condNombre} en pieza ${finding.pieza}${finding.superficie ? ' (' + finding.superficie + ')' : ''}`);
                }
            });
        }

        // 2. Si no hay marcas pendientes o ya fueron guardadas, leer marcas activas del mapa del odontograma
        if (piezasSet.size === 0 && state.odontoPiecesMap && state.odontoPiecesMap.size > 0) {
            state.odontoPiecesMap.forEach((marcas, pieza) => {
                marcas.forEach(m => {
                    if (m.condicion && m.condicion !== 'SANO') {
                        piezasSet.add(Number(pieza));
                        const condNombre = m.condicion === 'OBTURACION' ? 'Resina / Obturación' : m.condicion;
                        procedimientos.push(`${condNombre} en pieza ${pieza}${m.superficie ? ' (' + m.superficie + ')' : ''}`);
                    }
                });
            });
        }

        const piezasOrdenadas = Array.from(piezasSet).sort((a, b) => a - b);
        let piezasCadena = piezasOrdenadas.join(', ');
        if (piezasCadena.length > 95) {
            const reducidas = [];
            for (const p of piezasOrdenadas) {
                const candidato = reducidas.concat(p).join(', ');
                if (candidato.length > 95) break;
                reducidas.push(p);
            }
            piezasCadena = reducidas.join(', ');
        }
        return {
            piezasStr: piezasCadena,
            procedimientosStr: procedimientos.slice(0, 5).join('; ')
        };
    }

    function cargarDesdeOdontogramaEnFormulario() {
        const form = $('#form-crear-atencion');
        if (!form) return;
        const findings = obtenerHallazgosOdontogramaActual();
        if (!findings.piezasStr) {
            mostrarAviso('No se encontraron piezas con patologías en el odontograma. Marca las piezas tratadas en el recuadro.', 'info', form);
            return;
        }
        const inputPieza = form.elements['piezasTratadas'];
        if (inputPieza) {
            inputPieza.value = findings.piezasStr;
            inputPieza.classList.remove('is-invalid');
        }
        const inputProc = form.elements['planProcedimiento'];
        if (inputProc && findings.procedimientosStr) {
            const current = (inputProc.value || '').trim();
            if (!current) {
                inputProc.value = findings.procedimientosStr;
            } else if (!current.includes(findings.procedimientosStr)) {
                inputProc.value = `${current}\n• ${findings.procedimientosStr}`;
            }
        }
        sincronizarChipsDesdeInput();
        guardarBorradorEnMemoria();
        mostrarAviso(`Piezas cargadas: ${findings.piezasStr}`);
    }

    function abrirModalAtencionConPieza(pieza, procedimiento = '', notas = '', receta = '', itemPlanId = null) {
        abrirModalAtencion();
        const form = $('#form-crear-atencion');
        if (form) {
            let strPieza = '';
            let piezasArray = [];
            if (Array.isArray(pieza)) {
                piezasArray = pieza.map(Number).filter(Boolean);
                strPieza = piezasArray.join(', ');
            } else if (typeof pieza === 'string' && pieza.includes(',')) {
                piezasArray = (pieza.match(/\b(1[1-8]|2[1-8]|3[1-8]|4[1-8]|5[1-5]|6[1-5]|7[1-5]|8[1-5])\b/g) || []).map(Number);
                strPieza = pieza;
            } else if (pieza) {
                piezasArray = [Number(pieza)];
                strPieza = String(pieza);
            }

            const inputPieza = form.elements['piezasTratadas'];
            if (inputPieza) {
                inputPieza.value = strPieza;
            }

            const inputItemPlan = form.elements['itemPlanIdCompletar'];
            if (inputItemPlan) {
                inputItemPlan.value = itemPlanId ? String(itemPlanId) : '';
                const importePanel = $('#atencion-importe-panel');
                if (importePanel) importePanel.hidden = Boolean(itemPlanId);
                if (itemPlanId && form.elements.importeTratamiento) form.elements.importeTratamiento.value = '';
            }

            const planNotice = $('#atencion-modal-plan-notice');
            if (planNotice) {
                if (itemPlanId) {
                    planNotice.hidden = false;
                    const tit = $('#atencion-modal-plan-titulo');
                    if (tit) tit.textContent = procedimiento ? `Tratamiento: ${procedimiento}` : 'Tratamiento del Plan';
                } else {
                    planNotice.hidden = true;
                }
            }

            const titleEl = $('#modal-atencion-titulo');
            if (titleEl) {
                if (piezasArray.length === 1) {
                    titleEl.textContent = `Registrar Atención · Pieza ${piezasArray[0]}`;
                } else if (piezasArray.length > 1) {
                    titleEl.textContent = `Registrar Atención · ${piezasArray.length} Piezas Tratadas`;
                } else {
                    titleEl.textContent = 'Registrar Atención Clínica';
                }
            }

            const inputProc = form.elements['planProcedimiento'];
            if (inputProc) {
                if (procedimiento) {
                    inputProc.value = procedimiento;
                } else if (piezasArray.length === 1) {
                    const marcasPieza = state.odontoPiecesMap.get(piezasArray[0]) || [];
                    const condicionNoSana = marcasPieza.find(m => m.condicion && m.condicion !== 'SANO');
                    if (condicionNoSana) {
                        const nombreTrat = condicionNoSana.condicion === 'OBTURACION' ? 'Curación con resina' : `Tratamiento de ${condicionNoSana.condicion.toLowerCase()}`;
                        inputProc.value = `${nombreTrat} en pieza ${piezasArray[0]}`;
                    } else {
                        inputProc.value = '';
                    }
                } else {
                    inputProc.value = '';
                }
                setTimeout(() => inputProc.focus(), 250);
            }
            const inputSubj = form.elements['subjetivo'];
            if (inputSubj && notas) {
                inputSubj.value = notas;
            }
            const inputReceta = form.elements['indicacionesReceta'];
            if (inputReceta && receta) {
                inputReceta.value = receta;
            }
        }
        sincronizarChipsDesdeInput();
        guardarBorradorEnMemoria();
    }

    function renderDiagnosticos() {
        // 1. Hallazgos automáticos desde el Odontograma (Directo sin doble registro)
        const tbodyOdonto = $('#tabla-odonto-hallazgos-body');
        const countOdonto = $('#odonto-hallazgos-count');
        const chkMaster = $('#chk-hallazgos-master');
        const btnAtenderSel = $('#btn-atender-hallazgos-sel');
        const btnAtenderTodos = $('#btn-atender-todos-hallazgos');
        const countSelSpan = $('#count-hallazgos-sel');

        if (tbodyOdonto) {
            const hallazgos = [];
            state.odontoPiecesMap.forEach((marcas, pieza) => {
                marcas.forEach(m => {
                    if (m.condicion && m.condicion !== 'SANO') {
                        hallazgos.push({ pieza: Number(pieza), ...m });
                    }
                });
            });
            hallazgos.sort((a, b) => a.pieza - b.pieza);
            if (countOdonto) {
                countOdonto.textContent = `${hallazgos.length} hallazgo(s) detectado(s)`;
            }
            if (hallazgos.length === 0) {
                tbodyOdonto.innerHTML = '<tr><td colspan="6" class="text-center text-secondary py-3 small">Sin marcas de patologías registradas en el odontograma. Las condiciones que marques aparecerán aquí automáticamente.</td></tr>';
                if (chkMaster) { chkMaster.checked = false; chkMaster.indeterminate = false; }
                if (btnAtenderSel) btnAtenderSel.classList.add('d-none');
            } else {
                const getCondBadge = (cond) => {
                    switch (String(cond).toUpperCase()) {
                        case 'CARIES': return '<span class="badge text-bg-danger fw-bold"><i class="bi bi-circle-fill me-1"></i> CARIES</span>';
                        case 'FRACTURA': return '<span class="badge text-bg-warning text-dark fw-bold"><i class="bi bi-slash-circle me-1"></i> FRACTURA</span>';
                        case 'RESTAURACION': return '<span class="badge text-bg-primary fw-bold"><i class="bi bi-check2-circle me-1"></i> RESTAURACIÓN</span>';
                        case 'CORONA': return '<span class="badge text-bg-info text-dark fw-bold"><i class="bi bi-gem me-1"></i> CORONA</span>';
                        case 'ENDODONCIA': return '<span class="badge text-bg-secondary fw-bold"><i class="bi bi-lightning-charge me-1"></i> ENDODONCIA</span>';
                        case 'IMPLANTE': return '<span class="badge text-bg-success fw-bold"><i class="bi bi-shield-plus me-1"></i> IMPLANTE</span>';
                        case 'BRACKETS': return '<span class="badge bg-primary text-white fw-bold"><i class="bi bi-grid-3x3 me-1"></i> BRACKETS</span>';
                        case 'AUSENTE': return '<span class="badge text-bg-dark fw-bold"><i class="bi bi-x-circle me-1"></i> AUSENTE</span>';
                        default: return `<span class="badge bg-light text-dark border fw-bold">${escape(cond)}</span>`;
                    }
                };
                tbodyOdonto.innerHTML = hallazgos.map(h => `
                    <tr>
                        <td class="text-center">
                            <input type="checkbox" class="form-check-input chk-hallazgo-item" data-pieza="${h.pieza}" data-cond="${escape(h.condicion)}" style="cursor: pointer;">
                        </td>
                        <td><strong class="text-dark">Pieza ${h.pieza}</strong> <small class="text-secondary d-block" style="font-size:0.75rem;">${nombrePieza(h.pieza)}</small></td>
                        <td>${getCondBadge(h.condicion)}</td>
                        <td><span class="badge bg-light text-secondary border">${escape(h.superficie || 'GENERAL')}</span></td>
                        <td><small class="text-secondary"><i class="bi bi-grid-3x3 text-teal me-1"></i> Odontograma</small></td>
                        <td class="text-end">
                            <div class="d-inline-flex gap-1">
                                <button type="button" class="btn btn-xs btn-outline-teal btn-odonto-a-plan" data-pieza="${h.pieza}" data-cond="${escape(h.condicion)}" title="Cotizar en plan de tratamiento">
                                    <i class="bi bi-plus-circle me-1"></i> Cotizar en Plan
                                </button>
                                <button type="button" class="btn btn-xs btn-outline-primary btn-odonto-a-atender" data-pieza="${h.pieza}" data-cond="${escape(h.condicion)}" title="Registrar atención de hoy">
                                    <i class="bi bi-activity me-1"></i> Atender
                                </button>
                            </div>
                        </td>
                    </tr>
                `).join('');

                const actualizarSelState = () => {
                    const checkedBoxes = Array.from($$('.chk-hallazgo-item:checked'));
                    const totalBoxes = Array.from($$('.chk-hallazgo-item'));
                    if (chkMaster) {
                        chkMaster.checked = totalBoxes.length > 0 && checkedBoxes.length === totalBoxes.length;
                        chkMaster.indeterminate = checkedBoxes.length > 0 && checkedBoxes.length < totalBoxes.length;
                    }
                    if (btnAtenderSel) {
                        if (checkedBoxes.length > 0) {
                            btnAtenderSel.classList.remove('d-none');
                            if (countSelSpan) countSelSpan.textContent = checkedBoxes.length;
                        } else {
                            btnAtenderSel.classList.add('d-none');
                        }
                    }
                };

                if (chkMaster) {
                    chkMaster.onchange = () => {
                        $$('.chk-hallazgo-item').forEach(cb => { cb.checked = chkMaster.checked; });
                        actualizarSelState();
                    };
                }

                $$('.chk-hallazgo-item').forEach(cb => {
                    cb.onchange = actualizarSelState;
                });

                actualizarSelState();

                if (btnAtenderTodos) {
                    btnAtenderTodos.onclick = () => {
                        if (hallazgos.length === 0) {
                            mostrarAviso('No hay piezas marcadas con patologías en el odontograma.', 'info');
                            return;
                        }
                        const piezasSet = Array.from(new Set(hallazgos.map(h => h.pieza))).sort((a, b) => a - b);
                        const desc = hallazgos.slice(0, 4).map(h => {
                            const nombre = h.condicion === 'OBTURACION' ? 'Curación' : h.condicion;
                            return `${nombre} en pza ${h.pieza}`;
                        }).join('; ');
                        abrirModalAtencionConPieza(piezasSet, desc);
                    };
                }

                if (btnAtenderSel) {
                    btnAtenderSel.onclick = () => {
                        const checkedBoxes = Array.from($$('.chk-hallazgo-item:checked'));
                        if (checkedBoxes.length === 0) {
                            mostrarAviso('Selecciona al menos una pieza con el casillero para atender.', 'warning');
                            return;
                        }
                        const piezasSet = Array.from(new Set(checkedBoxes.map(cb => Number(cb.dataset.pieza)))).sort((a, b) => a - b);
                        const desc = checkedBoxes.slice(0, 4).map(cb => {
                            const cond = cb.dataset.cond === 'OBTURACION' ? 'Curación' : cb.dataset.cond;
                            return `${cond} en pza ${cb.dataset.pieza}`;
                        }).join('; ');
                        abrirModalAtencionConPieza(piezasSet, desc);
                    };
                }

                $$('.btn-odonto-a-plan').forEach(btn => {
                    btn.addEventListener('click', () => {
                        const pieza = Number(btn.dataset.pieza);
                        const cond = btn.dataset.cond;
                        abrirModalPlanConDiagnostico(null, `Tratamiento de ${cond} - Pieza ${pieza}`, pieza);
                    });
                });

                $$('.btn-odonto-a-atender').forEach(btn => {
                    btn.addEventListener('click', () => {
                        const pieza = btn.dataset.pieza;
                        const cond = btn.dataset.cond;
                        const nombre = cond === 'OBTURACION' ? 'Curación con resina' : `Tratamiento de ${cond.toLowerCase()}`;
                        abrirModalAtencionConPieza(pieza, `${nombre} en pieza ${pieza}`);
                    });
                });
            }
        }

        // 2. Diagnósticos Médicos Formales (CIE-10)
        const list = state.expediente.diagnosticos || [];
        const tbody = $('#tabla-diagnosticos-body');
        if (!tbody) return;
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-secondary py-4">Sin diagnósticos médicos CIE-10 registrados. Si requieres un código formal, pulsa "+ Agregar Diagnóstico CIE-10".</td></tr>';
            return;
        }
        tbody.innerHTML = list.map(d => `
            <tr>
                <td><span class="badge text-bg-secondary">${escape(d.codigoCie10 || 'CIE-10')}</span></td>
                <td><strong>${escape(d.descripcion)}</strong></td>
                <td>${d.numeroPieza ? `<span class="badge text-bg-info">Pieza ${d.numeroPieza}</span>` : '<span class="text-muted">General / Arcada</span>'}</td>
                <td><span class="badge ${d.gravedad === 'SEVERA' ? 'text-bg-danger' : 'text-bg-warning'}">${escape(d.gravedad || 'MODERADA')}</span></td>
                <td><span class="clinica-badge ${String(d.estado).toLowerCase()}">${escape(d.estado)}</span></td>
                <td class="text-end">
                    <div class="dropdown d-inline-block">
                        <button class="btn btn-light btn-sm" data-bs-toggle="dropdown"><i class="bi bi-three-dots-vertical"></i></button>
                        <ul class="dropdown-menu dropdown-menu-end">
                            <li><a class="dropdown-item btn-diag-a-atender" href="#" data-pieza="${d.numeroPieza || ''}" data-desc="${escape(d.descripcion)}"><i class="bi bi-activity text-teal"></i> Registrar Atención</a></li>
                            <li><hr class="dropdown-divider"></li>
                            <li><a class="dropdown-item btn-diag-estado" href="#" data-id="${d.id}" data-estado="EN_TRATAMIENTO"><i class="bi bi-play-circle text-primary"></i> En tratamiento</a></li>
                            <li><a class="dropdown-item btn-diag-estado" href="#" data-id="${d.id}" data-estado="RESUELTO"><i class="bi bi-check2-circle text-success"></i> Marcar resuelto</a></li>
                            <li><hr class="dropdown-divider"></li>
                            <li><a class="dropdown-item btn-diag-a-plan" href="#" data-id="${d.id}" data-desc="${escape(d.descripcion)}" data-pieza="${d.numeroPieza || ''}"><i class="bi bi-plus-square text-teal"></i> Añadir a Plan de Tratamiento</a></li>
                        </ul>
                    </div>
                </td>
            </tr>
        `).join('');

        $$('.btn-diag-a-atender').forEach(a => {
            a.addEventListener('click', e => {
                e.preventDefault();
                abrirModalAtencionConPieza(a.dataset.pieza, a.dataset.desc);
            });
        });

        $$('.btn-diag-estado').forEach(a => {
            a.addEventListener('click', async e => {
                e.preventDefault();
                try {
                    await api(`/interno/clinica/api/diagnosticos/${a.dataset.id}/estado`, {
                        method: 'POST',
                        body: { estado: a.dataset.estado }
                    });
                    await cargarPaciente(state.pacienteId);
                    mostrarAviso('Estado de diagnóstico actualizado.');
                } catch (err) {
                    mostrarAviso(err.message, 'error');
                }
            });
        });

        $$('.btn-diag-a-plan').forEach(a => {
            a.addEventListener('click', e => {
                e.preventDefault();
                abrirModalPlanConDiagnostico(a.dataset.id, a.dataset.desc, a.dataset.pieza);
            });
        });
    }

    // ============================================================
    // 4. TRATAMIENTOS (CATÁLOGO)
    // ============================================================
    async function renderTratamientos() {
        const tbody = $('#tabla-tratamientos-body');
        if (!tbody) return;
        try {
            const cat = $('#filtro-tratamiento-cat')?.value || '';
            const q = $('#filtro-tratamiento-q')?.value || '';
            const list = await api(`/interno/clinica/api/tratamientos/catalogo?categoria=${encodeURIComponent(cat)}&q=${encodeURIComponent(q)}`);
            actualizarOpcionesCatalogoEnPlan(list);
            if (!list || list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-secondary py-4">No se encontraron procedimientos en el catálogo.</td></tr>';
                return;
            }
            let grupoActual = '';
            tbody.innerHTML = list.map(t => {
                const grupo = `${t.categoria} · ${t.subcategoria || 'Otros procedimientos'}`;
                const encabezado = grupo === grupoActual ? '' : `<tr class="table-light"><th colspan="6" scope="rowgroup" class="text-teal">${escape(grupo)}</th></tr>`;
                grupoActual = grupo;
                return encabezado + `
                <tr>
                    <td><code>${escape(t.codigo || '—')}</code></td>
                    <td>
                        <strong class="text-dark">${escape(t.nombre)}</strong>
                        <div class="small text-secondary text-truncate" style="max-width: 320px;">${escape(t.descripcion || '')}</div>
                    </td>
                    <td><span class="badge text-bg-light border">${escape(t.subcategoria || 'Otros procedimientos')}</span></td>
                    <td><i class="bi bi-clock text-secondary me-1"></i>${t.duracion} min aprox.</td>
                    <td><strong class="text-teal">${Number(t.precio) > 0 ? money(t.precio) : 'Por definir'}</strong></td>
                    <td class="text-end">
                        <button type="button" class="btn btn-outline-primary btn-sm btn-anadir-a-plan fw-semibold shadow-sm"
                                data-id="${t.id}" data-codigo="${escape(t.codigo || '')}" data-nombre="${escape(t.nombre)}" data-precio="${t.precio}">
                            <i class="bi bi-plus-circle me-1"></i> Añadir a Plan
                        </button>
                    </td>
                </tr>
            `; }).join('');

            $$('.btn-anadir-a-plan').forEach(btn => {
                btn.addEventListener('click', () => {
                    agregarItemAlCarrito({
                        id: btn.dataset.id,
                        nombre: btn.dataset.nombre,
                        precio: btn.dataset.precio,
                        codigo: btn.dataset.codigo
                    });
                    state.activeModulo = 'plan-tratamiento';
                    renderModuloActual();
                });
            });
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-danger py-3">Error cargando catálogo: ${escape(e.message)}</td></tr>`;
        }
    }

    function actualizarOpcionesCatalogoEnPlan(list) {
        if (!list || list.length === 0) return;
        state.catalogoCache = list;
        const select = $('#select-catalogo-rapido');
        if (select) {
            select.innerHTML = '<option value="">+ Seleccionar del catálogo oficial...</option>' +
                list.map(t => `<option value="${t.id}" data-nombre="${escape(t.nombre)}" data-precio="${t.precio}">[${t.codigo || 'SRV'}] ${escape(t.nombre)} — ${Number(t.precio) > 0 ? 'S/ ' + Number(t.precio).toFixed(2) : 'Por definir'}</option>`).join('');
        }
        const datalist = $('#datalist-tratamientos-plan');
        if (datalist) {
            datalist.innerHTML = list.map(t => `<option value="${escape(t.nombre)}">[${t.codigo}] ${t.categoria} (S/ ${t.precio})</option>`).join('');
        }
    }

    // ============================================================
    // 5. PLAN DE TRATAMIENTO (CARRITO INTERACTIVO & COBRO DIRECTO)
    // ============================================================
    async function renderBuilderCatalogo(categoriaFiltro = '', busqueda = '') {
        const container = $('#builder-catalog-items');
        if (!container) return;

        let list = state.catalogoCache;
        if (!list || list.length === 0) {
            try {
                list = await api('/interno/clinica/api/tratamientos/catalogo');
                state.catalogoCache = list || [];
            } catch (e) {
                container.innerHTML = `<p class="text-danger small py-2">Error cargando catálogo: ${escape(e.message)}</p>`;
                return;
            }
        }

        const cat = (categoriaFiltro || '').toUpperCase().trim();
        const q = (busqueda || '').toLowerCase().trim();

        let filtrados = list.filter(t => {
            const matchesCat = !cat || (t.categoria && t.categoria.toUpperCase() === cat);
            const matchesQ = !q ||
                (t.nombre && t.nombre.toLowerCase().includes(q)) ||
                (t.codigo && t.codigo.toLowerCase().includes(q)) ||
                (t.subcategoria && t.subcategoria.toLowerCase().includes(q));
            return matchesCat && matchesQ;
        });

        const badgeCount = $('#builder-cat-count');
        if (badgeCount) badgeCount.textContent = `${filtrados.length} procedimientos`;

        if (filtrados.length === 0) {
            container.innerHTML = '<p class="text-secondary small text-center py-4">No se encontraron procedimientos con ese filtro.</p>';
            return;
        }

        container.innerHTML = filtrados.map(t => `
            <div class="builder-catalog-card">
                <div class="d-flex flex-column" style="min-width: 0;">
                    <div class="d-flex align-items-center gap-1 flex-wrap">
                        <strong class="text-dark small text-truncate" title="${escape(t.nombre)}">${escape(t.nombre)}</strong>
                        <span class="badge bg-light text-secondary border" style="font-size:0.68rem;">${escape(t.codigo || 'SRV')}</span>
                    </div>
                    <small class="text-secondary text-truncate" style="font-size:0.75rem;">${escape(t.subcategoria || t.categoria || 'General')} · ${t.duracion || 30} min</small>
                </div>
                <div class="d-flex align-items-center gap-2 flex-shrink-0">
                    <strong class="text-teal small" style="min-width:65px;text-align:right;">${Number(t.precio) > 0 ? money(t.precio) : 'S/ 0.00'}</strong>
                    <button type="button" class="btn-add-item btn-builder-add" data-id="${t.id}" data-nombre="${escape(t.nombre)}" data-precio="${t.precio}" data-codigo="${escape(t.codigo || '')}" data-categoria="${escape(t.categoria || '')}">
                        <i class="bi bi-plus-lg me-1"></i>Agregar
                    </button>
                </div>
            </div>
        `).join('');

        container.querySelectorAll('.btn-builder-add').forEach(btn => {
            btn.addEventListener('click', () => {
                agregarItemAlCarrito({
                    id: btn.dataset.id,
                    nombre: btn.dataset.nombre,
                    precio: btn.dataset.precio,
                    codigo: btn.dataset.codigo,
                    categoria: btn.dataset.categoria
                });
            });
        });
    }

    function switchMobileBuilderView(view) {
        state.mobileBuilderView = view;
        const colCat = $('#builder-col-catalogo');
        const colCart = $('#builder-col-cart');
        const btnCat = $('#mob-tab-btn-cat');
        const btnCart = $('#mob-tab-btn-cart');
        const stickyBar = $('#mobile-sticky-cart-bar');

        if (view === 'cart') {
            if (colCat) colCat.classList.add('is-mob-hidden');
            if (colCart) colCart.classList.remove('is-mob-hidden');
            if (btnCat) {
                btnCat.classList.remove('active');
                btnCat.classList.add('text-secondary');
            }
            if (btnCart) {
                btnCart.classList.add('active');
                btnCart.classList.remove('text-secondary');
            }
            if (stickyBar) stickyBar.hidden = true;
        } else {
            if (colCat) colCat.classList.remove('is-mob-hidden');
            if (colCart) colCart.classList.add('is-mob-hidden');
            if (btnCat) {
                btnCat.classList.add('active');
                btnCat.classList.remove('text-secondary');
            }
            if (btnCart) {
                btnCart.classList.remove('active');
                btnCart.classList.add('text-secondary');
            }
            if (stickyBar && state.carritoPlan.length > 0) stickyBar.hidden = false;
        }
    }

    // ============================================================
    // VALIDACIÓN Y GESTIÓN DE PIEZAS DENTALES (SISTEMAS FDI Y UNIVERSAL)
    // ============================================================
    const DENTAL_TEETH_SET = new Set([
        // Sistema Universal (1 al 32)
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
        // Sistema FDI permanentes (cuadrantes 1 al 4)
        33, 34, 35, 36, 37, 38,
        41, 42, 43, 44, 45, 46, 47, 48,
        // Sistema FDI deciduos / niños (cuadrantes 5 al 8)
        51, 52, 53, 54, 55,
        61, 62, 63, 64, 65,
        71, 72, 73, 74, 75,
        81, 82, 83, 84, 85
    ]);

    function parsearPiezasDentales(str) {
        if (!str || typeof str !== 'string') return { piezasValidas: [], piezasDuplicadas: [], piezasInvalidas: [] };
        const tokens = str.split(/[\s,;/]+/).map(s => s.trim()).filter(Boolean);
        const piezasValidas = [];
        const piezasDuplicadas = [];
        const piezasInvalidas = [];

        tokens.forEach(tok => {
            const num = parseInt(tok, 10);
            if (!isNaN(num) && DENTAL_TEETH_SET.has(num)) {
                if (piezasValidas.includes(num)) {
                    piezasDuplicadas.push(num);
                } else {
                    piezasValidas.push(num);
                }
            } else {
                piezasInvalidas.push(tok);
            }
        });
        return { piezasValidas, piezasDuplicadas, piezasInvalidas };
    }

    function agregarItemAlCarrito(trat) {
        if (!state.expediente) {
            mostrarAviso('Selecciona primero un paciente para agregar procedimientos al plan.', 'error');
            return;
        }
        const precioBase = trat.precio !== undefined && Number(trat.precio) > 0 ? Number(trat.precio) : 0;
        
        let piezaActual = '';
        if (trat.numeroPieza) {
            piezaActual = String(trat.numeroPieza);
        } else if (state.detallePieza) {
            piezaActual = typeof state.detallePieza === 'object' ? String(state.detallePieza.numero || '') : String(state.detallePieza);
        }

        const cat = (trat.categoria || '').toUpperCase();
        const nom = (trat.nombre || '').toLowerCase();
        const esGeneralPorDefecto = !piezaActual && (
            cat === 'PREVENTIVA' || cat === 'ORTODONCIA' || cat === 'ESTETICA' ||
            nom.includes('profilaxis') || nom.includes('limpieza') || nom.includes('blanqueamiento') ||
            nom.includes('consulta') || nom.includes('diagnostico') || nom.includes('diagnóstico') ||
            nom.includes('panoramica') || nom.includes('panorámica') || nom.includes('fluor')
        );

        const scope = esGeneralPorDefecto ? 'GENERAL' : 'PIEZA';

        // Buscar si ya existe este procedimiento en el carrito
        const existente = state.carritoPlan.find(x => String(x.tratId) === String(trat.id));

        if (existente) {
            if (existente.scope === 'PIEZA') {
                if (piezaActual) {
                    const parsed = parsearPiezasDentales(existente.pieza);
                    const numActual = parseInt(piezaActual, 10);
                    if (parsed.piezasValidas.includes(numActual)) {
                        mostrarAviso(`La pieza ${numActual} ya está en "${trat.nombre}". Cada diente solo se cobra 1 vez.`, 'info');
                        return;
                    } else {
                        const nuevas = [...parsed.piezasValidas, numActual];
                        existente.pieza = nuevas.join(', ');
                        existente.cantidad = nuevas.length;
                        renderCarritoPlan();
                        mostrarAviso(`Pieza ${numActual} añadida a "${trat.nombre}" (${existente.cantidad} piezas en total).`);
                        return;
                    }
                } else {
                    mostrarAviso(`"${trat.nombre}" ya está en el plan. Escribe las piezas a tratar en su casilla.`, 'info');
                    return;
                }
            } else {
                // Modo GENERAL: se incrementa la cantidad (sesiones / unidades)
                existente.cantidad = (Number(existente.cantidad) || 1) + 1;
                renderCarritoPlan();
                mostrarAviso(`"${trat.nombre}" cantidad aumentada a ${existente.cantidad}.`);
                return;
            }
        }

        // Crear nuevo item
        const uid = 'item_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);
        state.carritoPlan.push({
            uid: uid,
            tratId: trat.id,
            nombre: trat.nombre,
            codigo: trat.codigo || 'SRV',
            categoria: trat.categoria || '',
            precioBase: precioBase,
            scope: scope,
            pieza: piezaActual,
            cantidad: 1,
            fase: 1,
            tipoDesc: 'pct',
            valDesc: 0
        });
        renderCarritoPlan();
        mostrarAviso(`"${trat.nombre}" añadido al plan.`);
    }

    function renderCarritoPlan() {
        const container = $('#builder-cart-items');
        const badge = $('#builder-cart-badge');
        const mobBadge = $('#mob-cart-badge-count');
        const totalEl = $('#builder-cart-total');
        const subtotalEl = $('#builder-summary-subtotal');
        const discountRow = $('#builder-summary-discount-row');
        const discountValEl = $('#builder-summary-discount');
        const itemsCountEl = $('#builder-summary-items-count');

        // Sticky bar móvil
        const stickyBar = $('#mobile-sticky-cart-bar');
        const stickyTotal = $('#mobile-sticky-total');
        const stickyCount = $('#mobile-sticky-count');

        if (!container) return;

        const count = state.carritoPlan.length;
        let totalItemsQty = 0;
        let subtotalCatalogoTotal = 0;
        let totalDescuentoCalculado = 0;

        state.carritoPlan.forEach(it => {
            // Asegurar alcance: PIEZA vs GENERAL
            if (!it.scope) it.scope = it.pieza ? 'PIEZA' : 'GENERAL';

            // En modo PIEZA, la cantidad se sincroniza estrictamente con el conteo de piezas dentales
            if (it.scope === 'PIEZA') {
                if (it.pieza) {
                    const { piezasValidas } = parsearPiezasDentales(it.pieza);
                    it.cantidad = Math.max(1, piezasValidas.length);
                } else {
                    it.cantidad = 1;
                }
            } else {
                it.cantidad = Math.max(1, parseInt(it.cantidad) || 1);
            }

            const qty = it.cantidad;
            totalItemsQty += qty;
            const subCatalogoItem = it.precioBase * qty;
            subtotalCatalogoTotal += subCatalogoItem;

            if (state.descuentosActivos && state.modoDescuento === 'item') {
                const valDesc = Math.max(0, parseFloat(it.valDesc) || 0);
                let descUnit = 0;
                if (it.tipoDesc === 'pct') {
                    descUnit = it.precioBase * (Math.min(100, valDesc) / 100);
                } else {
                    descUnit = Math.min(it.precioBase, valDesc);
                }
                totalDescuentoCalculado += (descUnit * qty);
            }
        });

        // Descuento global
        if (state.descuentosActivos && state.modoDescuento === 'global') {
            const valGlobal = Math.max(0, parseFloat(state.descuentoGlobalVal) || 0);
            if (state.descuentoGlobalTipo === 'pct') {
                totalDescuentoCalculado = subtotalCatalogoTotal * (Math.min(100, valGlobal) / 100);
            } else {
                totalDescuentoCalculado = Math.min(subtotalCatalogoTotal, valGlobal);
            }
        }

        const totalFinal = Math.max(0, subtotalCatalogoTotal - (state.descuentosActivos ? totalDescuentoCalculado : 0));

        // Actualizar badges y totales
        if (badge) badge.textContent = `${count} ${count === 1 ? 'procedimiento' : 'procedimientos'}`;
        if (mobBadge) mobBadge.textContent = count;
        if (subtotalEl) subtotalEl.textContent = money(subtotalCatalogoTotal);
        if (itemsCountEl) itemsCountEl.textContent = `${count} ${count === 1 ? 'procedimiento' : 'procedimientos'} (${totalItemsQty} ${totalItemsQty === 1 ? 'unidad' : 'unidades'})`;
        if (totalEl) totalEl.textContent = money(totalFinal);

        // Descuento en resumen
        if (discountRow && discountValEl) {
            if (state.descuentosActivos && totalDescuentoCalculado > 0) {
                discountRow.hidden = false;
                discountValEl.textContent = `- ${money(totalDescuentoCalculado)}`;
            } else {
                discountRow.hidden = true;
            }
        }

        // Barra sticky móvil
        if (stickyBar) {
            if (count > 0 && state.mobileBuilderView === 'catalogo') {
                stickyBar.hidden = false;
                if (stickyTotal) stickyTotal.textContent = money(totalFinal);
                if (stickyCount) stickyCount.textContent = `${totalItemsQty} ${totalItemsQty === 1 ? 'unidad' : 'unidades'} en plan`;
            } else {
                stickyBar.hidden = true;
            }
        }

        // Si está vacío
        if (count === 0) {
            container.innerHTML = `
                <div class="text-center py-4 text-secondary small">
                    <i class="bi bi-bag-plus fs-2 text-teal opacity-50 d-block mb-1"></i>
                    El carrito está vacío.<br>Haz clic en <strong>[+ Agregar]</strong> en el catálogo para armar el plan.
                </div>
            `;
            return;
        }

        // Renderizar tarjetas (.cart-item-card)
        container.innerHTML = state.carritoPlan.map(it => {
            const qty = it.cantidad || 1;
            const subCatalogoItem = it.precioBase * qty;
            let subNetoItem = subCatalogoItem;
            let tieneDescItem = false;

            if (state.descuentosActivos && state.modoDescuento === 'item') {
                const valDesc = Math.max(0, parseFloat(it.valDesc) || 0);
                if (valDesc > 0) {
                    let descUnit = 0;
                    if (it.tipoDesc === 'pct') {
                        descUnit = it.precioBase * (Math.min(100, valDesc) / 100);
                    } else {
                        descUnit = Math.min(it.precioBase, valDesc);
                    }
                    const precioUnitNeto = Math.max(0, it.precioBase - descUnit);
                    subNetoItem = precioUnitNeto * qty;
                    tieneDescItem = descUnit > 0;
                }
            }

            return `
            <div class="cart-item-card" data-uid="${it.uid}">
                <div class="d-flex align-items-center justify-content-between mb-2 pb-1 border-bottom">
                    <div class="d-flex align-items-center gap-1.5 flex-wrap" style="min-width: 0;">
                        <span class="badge bg-teal-subtle text-teal fw-bold" style="font-size: 0.68rem;">${escape(it.codigo || 'SRV')}</span>
                        <strong class="text-dark small text-truncate" style="max-width: 210px;" title="${escape(it.nombre)}">${escape(it.nombre)}</strong>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <div class="text-end">
                            ${tieneDescItem ? `<span class="text-decoration-line-through text-muted small d-block" style="font-size: 0.7rem; line-height: 1;">${money(subCatalogoItem)}</span>` : ''}
                            <strong class="text-teal" style="font-size: 0.95rem;">${money(subNetoItem)}</strong>
                        </div>
                        <button type="button" class="btn btn-sm btn-light text-danger rounded-circle p-0 d-flex align-items-center justify-content-center btn-cart-remove" style="width: 24px; height: 24px;" title="Quitar">
                            <i class="bi bi-x-lg" style="font-size: 0.72rem;"></i>
                        </button>
                    </div>
                </div>

                <!-- Configuración: Por Pieza vs General + Cantidad -->
                <div class="d-flex align-items-center justify-content-between gap-2 flex-wrap">
                    <!-- Selector de Alcance -->
                    <div class="d-flex align-items-center gap-1">
                        <div class="btn-group btn-group-sm cart-scope-selector" role="group">
                            <button type="button" class="btn btn-xs ${it.scope === 'PIEZA' ? 'btn-teal text-white active' : 'btn-outline-secondary'} btn-scope-pieza" title="Asignar a pieza(s) dental(es)">
                                <i class="bi bi-pin-angle me-0.5"></i> Diente(s)
                            </button>
                            <button type="button" class="btn btn-xs ${it.scope === 'GENERAL' ? 'btn-teal text-white active' : 'btn-outline-secondary'} btn-scope-general" title="Sin pieza / Boca completa">
                                <i class="bi bi-globe me-0.5"></i> General
                            </button>
                        </div>
                    </div>

                    <!-- Detalle de Pieza o Cantidad -->
                    <div class="d-flex align-items-center gap-2 ms-auto flex-wrap">
                        ${it.scope === 'PIEZA' ? `
                        <div class="d-flex align-items-center gap-1.5">
                            <div class="input-group input-group-sm" style="width: 145px;">
                                <span class="input-group-text bg-light text-secondary border-end-0 px-1.5" style="font-size: 0.75rem;" title="Pieza(s) FDI"><i class="bi bi-pin-map"></i></span>
                                <input type="text" class="form-control form-control-sm border-start-0 cart-item-pieza fw-semibold" value="${escape(it.pieza || '')}" placeholder="ej. 18, 19" title="Ingresa números de pieza separados por coma (ej. 18, 19)">
                            </div>
                            <span class="badge bg-teal-subtle text-teal border-0 rounded-pill px-2 py-1 cart-piece-calc-badge" style="font-size: 0.72rem;" title="${qty} pieza(s) × ${money(it.precioBase)}">
                                ${qty} pza${qty > 1 ? 's' : ''} · ${money(it.precioBase)} c/u
                            </span>
                        </div>
                        ` : `
                        <div class="d-flex align-items-center gap-2">
                            <span class="text-secondary small" style="font-size: 0.75rem;">${money(it.precioBase)} c/u</span>
                            <div class="cart-qty-stepper">
                                <button type="button" class="btn-qty-step btn-qty-minus" title="Disminuir"><i class="bi bi-dash"></i></button>
                                <input type="number" min="1" max="99" class="cart-qty-input" value="${qty}" title="Cantidad de unidades o sesiones">
                                <button type="button" class="btn-qty-step btn-qty-plus" title="Aumentar"><i class="bi bi-plus"></i></button>
                            </div>
                        </div>
                        `}
                    </div>
                </div>

                <!-- Descuento Individual si está activo -->
                ${(state.descuentosActivos && state.modoDescuento === 'item') ? `
                <div class="d-flex align-items-center justify-content-between pt-1.5 mt-1.5 border-top border-light">
                    <span class="text-success small fw-semibold" style="font-size: 0.72rem;"><i class="bi bi-tag-fill me-1"></i>Descuento:</span>
                    <div class="d-flex align-items-center gap-1">
                        <div class="btn-group btn-group-sm" role="group">
                            <button type="button" class="btn btn-xs ${it.tipoDesc === 'pct' ? 'btn-teal active text-white' : 'btn-outline-secondary'} btn-item-desc-pct">%</button>
                            <button type="button" class="btn btn-xs ${it.tipoDesc === 'val' ? 'btn-teal active text-white' : 'btn-outline-secondary'} btn-item-desc-val">S/</button>
                        </div>
                        <input type="number" min="0" step="${it.tipoDesc === 'pct' ? '1' : '0.5'}" class="form-control form-control-sm text-end cart-item-discount-val fw-semibold text-teal" value="${it.valDesc || 0}" style="width: 58px; height: 26px; font-size: 0.8rem;">
                    </div>
                </div>
                ` : ''}
            </div>
            `;
        }).join('');

        // Wire Event Listeners de las tarjetas
        container.querySelectorAll('.cart-item-card').forEach(card => {
            const uid = card.dataset.uid;
            const item = state.carritoPlan.find(x => x.uid === uid);
            if (!item) return;

            // Quitar item
            card.querySelector('.btn-cart-remove')?.addEventListener('click', () => {
                state.carritoPlan = state.carritoPlan.filter(x => x.uid !== uid);
                renderCarritoPlan();
            });

            // Cambiar a modo Por Pieza
            card.querySelector('.btn-scope-pieza')?.addEventListener('click', () => {
                if (item.scope !== 'PIEZA') {
                    item.scope = 'PIEZA';
                    if (!item.pieza && state.detallePieza) {
                        item.pieza = typeof state.detallePieza === 'object' ? String(state.detallePieza.numero || '') : String(state.detallePieza);
                    }
                    const { piezasValidas } = parsearPiezasDentales(item.pieza);
                    item.cantidad = Math.max(1, piezasValidas.length);
                    renderCarritoPlan();
                }
            });

            // Cambiar a modo General
            card.querySelector('.btn-scope-general')?.addEventListener('click', () => {
                if (item.scope !== 'GENERAL') {
                    item.scope = 'GENERAL';
                    item.pieza = '';
                    item.cantidad = 1;
                    renderCarritoPlan();
                }
            });

            // Entrada y validación de piezas dentales
            card.querySelector('.cart-item-pieza')?.addEventListener('change', e => {
                const raw = e.target.value.trim();
                if (!raw) {
                    item.pieza = '';
                    item.cantidad = 1;
                    renderCarritoPlan();
                    return;
                }
                const { piezasValidas, piezasDuplicadas, piezasInvalidas } = parsearPiezasDentales(raw);
                if (piezasDuplicadas.length > 0) {
                    mostrarAviso(`Pieza(s) duplicadas ignoradas: ${piezasDuplicadas.join(', ')}. Cada diente se cobra una vez.`, 'info');
                }
                if (piezasInvalidas.length > 0) {
                    mostrarAviso(`Pieza(s) FDI no reconocidas: ${piezasInvalidas.join(', ')}. Ejemplo válido: 18, 28, 46.`, 'info');
                }
                if (piezasValidas.length > 0) {
                    item.pieza = piezasValidas.join(', ');
                    item.cantidad = piezasValidas.length;
                } else {
                    item.pieza = raw;
                    item.cantidad = 1;
                }
                renderCarritoPlan();
            });

            // Stepper minus (solo activo en General)
            card.querySelector('.btn-qty-minus')?.addEventListener('click', () => {
                if (item.cantidad > 1) {
                    item.cantidad--;
                    renderCarritoPlan();
                }
            });

            // Stepper plus (solo activo en General)
            card.querySelector('.btn-qty-plus')?.addEventListener('click', () => {
                item.cantidad = (Number(item.cantidad) || 1) + 1;
                renderCarritoPlan();
            });

            // Stepper direct input (solo activo en General)
            card.querySelector('.cart-qty-input')?.addEventListener('change', e => {
                const val = parseInt(e.target.value) || 1;
                item.cantidad = Math.max(1, Math.min(99, val));
                renderCarritoPlan();
            });

            // Descuentos individuales
            card.querySelector('.btn-item-desc-pct')?.addEventListener('click', () => {
                if (item.tipoDesc !== 'pct') {
                    item.tipoDesc = 'pct';
                    renderCarritoPlan();
                }
            });
            card.querySelector('.btn-item-desc-val')?.addEventListener('click', () => {
                if (item.tipoDesc !== 'val') {
                    item.tipoDesc = 'val';
                    renderCarritoPlan();
                }
            });
            card.querySelector('.cart-item-discount-val')?.addEventListener('input', e => {
                item.valDesc = parseFloat(e.target.value) || 0;
                renderCarritoPlan();
            });
        });
    }

    async function guardarPlanDesdeCarrito(aprobarInmediato) {
        if (!state.pacienteId) {
            mostrarAviso('Selecciona primero un paciente.', 'error');
            return;
        }
        if (!state.carritoPlan || state.carritoPlan.length === 0) {
            mostrarAviso('Agrega al menos un procedimiento al carrito con [+ Agregar].', 'error');
            return;
        }

        const titulo = $('#builder-plan-titulo')?.value?.trim() || 'Plan de Tratamiento Integral';
        const notas = $('#builder-plan-notas')?.value?.trim() || '';

        // Calcular subtotales y distribución de descuentos
        let subtotalCatalogoTotal = 0;
        state.carritoPlan.forEach(it => {
            subtotalCatalogoTotal += (it.precioBase * it.cantidad);
        });

        let totalDescuento = 0;
        if (state.descuentosActivos) {
            if (state.modoDescuento === 'global') {
                const valGlobal = Math.max(0, parseFloat(state.descuentoGlobalVal) || 0);
                if (state.descuentoGlobalTipo === 'pct') {
                    totalDescuento = subtotalCatalogoTotal * (Math.min(100, valGlobal) / 100);
                } else {
                    totalDescuento = Math.min(subtotalCatalogoTotal, valGlobal);
                }
            } else {
                state.carritoPlan.forEach(it => {
                    const valDesc = Math.max(0, parseFloat(it.valDesc) || 0);
                    let descUnit = 0;
                    if (it.tipoDesc === 'pct') {
                        descUnit = it.precioBase * (Math.min(100, valDesc) / 100);
                    } else {
                        descUnit = Math.min(it.precioBase, valDesc);
                    }
                    totalDescuento += (descUnit * it.cantidad);
                });
            }
        }

        const factorGlobal = (state.descuentosActivos && state.modoDescuento === 'global' && subtotalCatalogoTotal > 0)
            ? Math.max(0, subtotalCatalogoTotal - totalDescuento) / subtotalCatalogoTotal
            : 1;

        const items = state.carritoPlan.map(it => {
            const qty = Math.max(1, parseInt(it.cantidad) || 1);
            let precioNetoTotalLinea = it.precioBase * qty;

            let detalleDesc = '';
            if (state.descuentosActivos) {
                if (state.modoDescuento === 'item') {
                    const valDesc = Math.max(0, parseFloat(it.valDesc) || 0);
                    if (valDesc > 0) {
                        let descUnit = it.tipoDesc === 'pct' ? it.precioBase * (Math.min(100, valDesc) / 100) : Math.min(it.precioBase, valDesc);
                        precioNetoTotalLinea = Math.max(0, it.precioBase - descUnit) * qty;
                        detalleDesc = it.tipoDesc === 'pct' ? ` [Desc. ${valDesc}%]` : ` [Desc. S/ ${valDesc.toFixed(2)}]`;
                    }
                } else if (state.modoDescuento === 'global' && totalDescuento > 0) {
                    precioNetoTotalLinea = (it.precioBase * qty) * factorGlobal;
                    detalleDesc = ` [Desc. Global aplicado]`;
                }
            }

            // Formatear descripción con alcance, piezas y cantidades
            let descFinal = it.nombre;
            if (it.scope === 'PIEZA' && it.pieza) {
                descFinal += ` - Pza(s): ${it.pieza}`;
            } else if (it.scope === 'GENERAL') {
                descFinal += ` (General)`;
            }
            if (qty > 1 && it.scope === 'GENERAL') {
                descFinal += ` [x${qty}]`;
            }
            if (detalleDesc) {
                descFinal += detalleDesc;
            }

            // Parsear número de pieza si es una sola pieza FDI específica
            let pzaNum = null;
            if (it.scope === 'PIEZA' && it.pieza) {
                const { piezasValidas } = parsearPiezasDentales(it.pieza);
                if (piezasValidas.length === 1) {
                    pzaNum = piezasValidas[0];
                }
            }

            return {
                descripcion: descFinal,
                precio: Number(precioNetoTotalLinea.toFixed(2)) > 0 ? Number(precioNetoTotalLinea.toFixed(2)) : 0.01,
                ordenFase: Number(it.fase) || 1,
                numeroPieza: pzaNum,
                tratamientoCatalogoId: it.tratId ? parseInt(it.tratId) : null,
                estado: 'PENDIENTE'
            };
        });

        try {
            const estadoInicial = aprobarInmediato ? 'APROBADO' : 'PROPUESTO';
            const saveRes = await api('/interno/clinica/api/planes', {
                method: 'POST',
                body: {
                    pacienteId: state.pacienteId,
                    titulo: titulo,
                    estado: estadoInicial,
                    notas: notas,
                    items: items
                }
            });

            const planId = saveRes.id;

            if (aprobarInmediato) {
                const deudRes = await api(`/interno/clinica/api/planes/${planId}/aprobar-deuda`, { method: 'POST' });
                state.carritoPlan = [];
                renderCarritoPlan();
                await cargarPaciente(state.pacienteId);
                mostrarAviso(`¡Plan aprobado! Se ha generado la deuda de ${money(deudRes.total)} en la cuenta del paciente.`);
                abrirModalCobro(deudRes.presupuestoId, planId, deudRes.total);
            } else {
                state.carritoPlan = [];
                renderCarritoPlan();
                await cargarPaciente(state.pacienteId);
                mostrarAviso('Cotización guardada como borrador con éxito (sin deuda generada).');
            }
        } catch (err) {
            mostrarAviso(err.message, 'error');
        }
    }

    function abrirModalCobro(presupuestoId, planId, totalSugerido) {
        const modalEl = $('#modal-cobro-clinica');
        if (!modalEl) return;

        modalEl.dataset.token = crypto.randomUUID();
        $('#cobro-presupuesto-id').value = presupuestoId;
        $('#cobro-plan-id').value = planId || '';

        const presupuestos = state.expediente?.finanzas?.presupuestos || [];
        const pres = presupuestos.find(p => Number(p.id) === Number(presupuestoId));
        const saldo = pres ? Number(pres.saldo !== undefined ? pres.saldo : pres.total) : Number(totalSugerido || 0);

        $('#cobro-saldo-display').textContent = money(saldo);
        $('#cobro-paciente-display').textContent = `Paciente: ${state.expediente?.paciente?.nombreCompleto || 'Paciente'}`;
        $('#cobro-monto-input').value = saldo > 0 ? saldo.toFixed(2) : '';
        $('#cobro-fecha-input').value = new Date().toISOString().split('T')[0];
        $('#cobro-referencia-input').value = '';
        const errMsg = $('#cobro-error-msg');
        if (errMsg) errMsg.style.display = 'none';

        $('#cobro-metodo-input').value = 'YAPE';
        $$('.cobro-metodo-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.metodo === 'YAPE');
        });

        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    }

    function renderPlanesTratamiento() {
        renderBuilderCatalogo();
        renderCarritoPlan();

        const planes = state.expediente?.planes || [];
        const container = $('#contenedor-planes');
        if (!container) return;

        if (planes.length === 0) {
            container.innerHTML = `
                <div class="text-center py-4 bg-light rounded-4 border">
                    <i class="bi bi-clipboard-pulse text-secondary" style="font-size: 2.5rem;"></i>
                    <h3 class="h6 fw-bold mt-2 text-dark">Sin planes de tratamiento previos</h3>
                    <p class="text-secondary small mb-0">Usa el constructor interactivo arriba para agregar procedimientos con [+] y armar un plan o cotización.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = planes.map(p => {
            const totalItems = Number(p.totalItems || 0);
            const realizados = Number(p.itemsRealizados || 0);
            const pct = totalItems > 0 ? Math.round((realizados / totalItems) * 100) : 0;
            const estadoBadge = String(p.estado).toLowerCase();
            const tienePresupuesto = !!p.presupuestoId;

            let botonesAccion = '';
            if (tienePresupuesto) {
                botonesAccion = `
                    <button class="btn btn-teal btn-sm fw-bold text-white shadow-sm btn-cobrar-directo" style="background-color: #0b7f86;" data-presid="${p.presupuestoId}" data-planid="${p.id}" data-total="${p.total}" title="Cobrar o ver detalle del presupuesto">
                        <i class="bi bi-wallet2 me-1"></i> Cobrar / Presupuesto #${p.presupuestoId}
                    </button>
                    <button class="btn btn-outline-secondary btn-sm fw-semibold btn-pres-modal" data-presid="${p.presupuestoId}" data-planid="${p.id}" title="Ver presupuesto detallado">
                        <i class="bi bi-receipt"></i>
                    </button>
                `;
            } else {
                botonesAccion = `
                    <button class="btn btn-success btn-sm fw-bold shadow-sm btn-aprobar-plan-deuda" style="background-color: #0b7f86; border-color: #0b7f86;" data-id="${p.id}" title="Aprueba el plan y genera el presupuesto con su deuda">
                        <i class="bi bi-check-circle-fill me-1"></i>Aprobar Plan y Generar Deuda
                    </button>
                `;
            }

            return `
                <div class="plan-card" data-plan-id="${p.id}">
                    <div class="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
                        <div>
                            <div class="d-flex align-items-center gap-2">
                                <h3 class="h5 fw-bold mb-0">${escape(p.titulo)}</h3>
                                <span class="clinica-badge ${estadoBadge}">${escape(p.estado)}</span>
                            </div>
                            <small class="text-secondary">Creado el ${formatDate(p.creadoEn)} · ${escape(p.creadoPor || 'Personal')}</small>
                        </div>
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="text-end">
                                <span class="d-block text-secondary" style="font-size:.75rem;">Costo total</span>
                                <strong class="fs-5 text-teal">${money(p.total)}</strong>
                            </div>
                            ${botonesAccion}
                        </div>
                    </div>

                    <div class="plan-progress-track mb-1">
                        <div class="plan-progress-fill" style="width: ${pct}%;"></div>
                    </div>
                    <div class="d-flex justify-content-between text-secondary small mb-3">
                        <span>Avance: <strong>${realizados} de ${totalItems} procedimientos (${pct}%)</strong></span>
                        ${tienePresupuesto ? `<span class="text-success"><i class="bi bi-check-circle-fill"></i> Presupuesto vinculado (Deuda activa)</span>` : '<span class="text-muted">Cotización preliminar (Sin deuda)</span>'}
                    </div>

                    ${(p.items && p.items.length) ? `
                    <div class="plan-fases">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <span style="font-size:.75rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:#64748b;">Procedimientos y Fases del Plan</span>
                            <span class="badge bg-light text-secondary border" style="font-size:.72rem;">${p.items.length} ${p.items.length === 1 ? 'procedimiento' : 'procedimientos'}</span>
                        </div>
                        <div class="d-flex flex-column gap-1.5">
                            ${p.items.map((it, idx) => {
                                const estadoFase = String(it.estado || 'pendiente').toLowerCase();
                                const realizado = estadoFase === 'realizado' || estadoFase === 'completado';
                                const iconFase = realizado ? 'check-circle-fill text-success' : estadoFase === 'en_proceso' ? 'arrow-right-circle-fill text-primary' : 'circle text-secondary';
                                const numFase = it.ordenFase || it.fase || (idx + 1);
                                return `<div class="d-flex align-items-center justify-content-between p-2 rounded-2 ${realizado ? 'bg-light text-muted' : 'bg-white border'}" style="font-size:.84rem;">
                                    <div class="d-flex align-items-center gap-2">
                                        <i class="bi bi-${iconFase}" style="font-size:.95rem;flex-shrink:0;"></i>
                                        <span class="badge bg-secondary-subtle text-secondary border px-1.5 py-0.5" style="font-size:.68rem;">Fase ${numFase}</span>
                                        ${it.numeroPieza ? `<span class="badge bg-info-subtle text-info border border-info-subtle px-1.5 py-0.5" style="font-size:.68rem;">Pza ${it.numeroPieza}</span>` : ''}
                                        <span class="fw-medium text-dark ${realizado ? 'text-decoration-line-through text-muted' : ''}">${escape(it.descripcion || it.nombreTratamiento || 'Procedimiento ' + (idx+1))}</span>
                                    </div>
                                    <div class="d-flex align-items-center gap-2">
                                        <strong style="font-size:.84rem;color:#0b7f86;min-width:65px;text-align:right;">${money(it.precio)}</strong>
                                        <span class="clinica-badge ${estadoFase}" style="font-size:.68rem;">${escape(it.estado || 'Pendiente')}</span>
                                        ${!realizado ? `
                                            <button type="button" class="btn btn-xs btn-outline-teal btn-plan-item-atender py-0.5 px-2 fw-semibold" 
                                                data-item-id="${it.id || ''}" 
                                                data-pieza="${it.numeroPieza || ''}" 
                                                data-proc="${escape(it.descripcion || it.nombreTratamiento || '')}" 
                                                title="Registrar atención médica para este procedimiento">
                                                <i class="bi bi-activity me-1"></i> Atender
                                            </button>
                                        ` : ''}
                                    </div>
                                </div>`;
                            }).join('')}
                        </div>
                    </div>` : `
                    <div class="p-3 bg-light rounded-3 text-secondary text-center small mt-2">
                        <i class="bi bi-info-circle me-1"></i> Sin procedimientos detallados en este plan.
                    </div>`}
                </div>
            `;
        }).join('');

        container.querySelectorAll('.btn-plan-item-atender').forEach(btn => {
            btn.addEventListener('click', () => {
                const itemId = btn.dataset.itemId ? Number(btn.dataset.itemId) : null;
                const pieza = btn.dataset.pieza;
                const proc = btn.dataset.proc;
                abrirModalAtencionConPieza(pieza, proc, '', '', itemId);
            });
        });

        container.querySelectorAll('.btn-generar-presupuesto').forEach(btn => {
            btn.addEventListener('click', () => generarPresupuestoDesdePlan(btn.dataset.id));
        });

        container.querySelectorAll('.btn-pres-modal').forEach(btn => {
            btn.addEventListener('click', () => abrirModalPresupuesto(btn.dataset.presid, btn.dataset.planid));
        });

        container.querySelectorAll('.btn-cobrar-directo').forEach(btn => {
            btn.addEventListener('click', () => abrirModalCobro(btn.dataset.presid, btn.dataset.planid, btn.dataset.total));
        });

        container.querySelectorAll('.btn-aprobar-plan-deuda').forEach(btn => {
            btn.addEventListener('click', async () => {
                try {
                    const deudRes = await api(`/interno/clinica/api/planes/${btn.dataset.id}/aprobar-deuda`, { method: 'POST' });
                    await cargarPaciente(state.pacienteId);
                    mostrarAviso(`¡Plan aprobado! Se ha generado la deuda de ${money(deudRes.total)}.`);
                    abrirModalCobro(deudRes.presupuestoId, btn.dataset.id, deudRes.total);
                } catch (e) {
                    mostrarAviso(e.message, 'error');
                }
            });
        });
    }

    // ─── Modal de Presupuesto (directo, sin salir de la vista) ───────────────────
    const _pLabels = { BORRADOR: 'Borrador', APROBADO: 'Aprobado', RECHAZADO: 'Rechazado', ANULADO: 'Anulado' };
    const _pMoney  = v => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(Number(v || 0));
    const _pDate   = v => v ? new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(String(v).length === 10 ? v + 'T12:00:00' : v)) : '—';
    let _presModalInstance = null;
    let _presModalCurrentId = null;
    let _presModalCurrentVersion = null;

    function _csrfHeaders() {
        const t = document.querySelector('meta[name="csrf-token"]')?.content;
        const h = document.querySelector('meta[name="csrf-header"]')?.content;
        return (t && h) ? { 'Content-Type': 'application/json', 'Accept': 'application/json', [h]: t } : { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    }

    async function abrirModalPresupuesto(presId, planId) {
        const modalEl = document.getElementById('modalPresupuestoClinica');
        if (!modalEl) return;
        _presModalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
        // Reset
        document.getElementById('modalPresBody').innerHTML = '<div class="text-center py-4"><div class="spinner-border text-success"></div><p class="text-secondary mt-2">Cargando presupuesto…</p></div>';
        document.getElementById('modalPresFooter').innerHTML = '<button type="button" class="btn btn-light" data-bs-dismiss="modal">Cerrar</button>';
        document.getElementById('modalPresError').style.display = 'none';
        _presModalInstance.show();
        try {
            const res = await fetch(`/interno/gestion/presupuestos/${presId}`, { credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } });
            const d = res.headers.get('content-type')?.includes('json') ? await res.json() : null;
            if (!res.ok) throw new Error(d?.error || 'No se pudo cargar el presupuesto.');
            _presModalCurrentId = presId;
            _presModalCurrentVersion = d.version;
            poblarModalPresupuesto(d, planId);
        } catch (e) {
            document.getElementById('modalPresBody').innerHTML = `<div class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle fs-1"></i><p class="mt-2">${escape(e.message)}</p></div>`;
        }
    }

    function poblarModalPresupuesto(d, planId) {
        const estado = d.estado || 'BORRADOR';
        const esBorrador = estado === 'BORRADOR';
        const esAprobado = estado === 'APROBADO';
        const saldo = (Number(d.total || 0) - Number(d.pagado || 0)).toFixed(2);

        const clrMap = { BORRADOR: 'secondary', APROBADO: 'success', RECHAZADO: 'warning text-dark', ANULADO: 'secondary' };
        const badge = `<span class="badge text-bg-${(clrMap[estado] || 'secondary').split(' ')[0]}">${_pLabels[estado] || estado}</span>`;

        document.getElementById('modalPresTitle').innerHTML = `Presupuesto #${d.id} ${badge}`;
        document.getElementById('modalPresSubtitle').textContent = `${d.titulo || ''} · Vigente hasta ${_pDate(d.vence)} · Creado ${_pDate(d.fecha)}`;

        const items = (d.items || []).map(it => `
            <tr>
                <td class="py-2">${escape(it.descripcion)}</td>
                <td class="py-2 text-center">${it.cantidad}</td>
                <td class="py-2 text-end">${_pMoney(it.precio)}</td>
                <td class="py-2 text-end fw-bold">${_pMoney(Number(it.precio) * Number(it.cantidad))}</td>
            </tr>`).join('');

        document.getElementById('modalPresBody').innerHTML = `
            <div class="row g-3 mb-3">
                <div class="col-sm-4">
                    <div class="border rounded-3 p-3 text-center">
                        <div class="text-secondary" style="font-size:.72rem;font-weight:600;text-transform:uppercase;">Total presupuestado</div>
                        <div class="fw-bold fs-4 text-success">${_pMoney(d.total)}</div>
                    </div>
                </div>
                <div class="col-sm-4">
                    <div class="border rounded-3 p-3 text-center">
                        <div class="text-secondary" style="font-size:.72rem;font-weight:600;text-transform:uppercase;">Abonado</div>
                        <div class="fw-bold fs-5">${_pMoney(d.pagado)}</div>
                    </div>
                </div>
                <div class="col-sm-4">
                    <div class="border rounded-3 p-3 text-center" style="background:${Number(saldo) > 0 ? '#fef9c3' : '#f0fdf4'};">
                        <div class="text-secondary" style="font-size:.72rem;font-weight:600;text-transform:uppercase;">Saldo pendiente</div>
                        <div class="fw-bold fs-5 ${Number(saldo) > 0 ? 'text-warning-emphasis' : 'text-success'}">${_pMoney(saldo)}</div>
                    </div>
                </div>
            </div>
            ${d.notas ? `<div class="alert alert-light border mb-3 py-2 px-3" style="font-size:.87rem;"><i class="bi bi-chat-left-text me-1 text-secondary"></i>${escape(d.notas)}</div>` : ''}
            <table class="table table-sm table-hover">
                <thead class="table-light"><tr>
                    <th>Concepto</th><th class="text-center" style="width:70px;">Cant.</th>
                    <th class="text-end" style="width:100px;">P.Unit.</th><th class="text-end" style="width:110px;">Subtotal</th>
                </tr></thead>
                <tbody>${items}</tbody>
                <tfoot><tr class="table-light">
                    <td colspan="3" class="fw-bold text-end">TOTAL:</td>
                    <td class="fw-bold text-end text-success">${_pMoney(d.total)}</td>
                </tr></tfoot>
            </table>`;

        // Botones de acción en el footer
        const footer = document.getElementById('modalPresFooter');
        footer.innerHTML = '<button type="button" class="btn btn-light" data-bs-dismiss="modal">Cerrar</button>';

        if (esBorrador) {
            footer.innerHTML += `
                <button class="btn btn-outline-danger btn-sm" id="btnPresAnular"><i class="bi bi-slash-circle me-1"></i>Anular</button>
                <button class="btn btn-outline-warning btn-sm" id="btnPresRechazar"><i class="bi bi-x-lg me-1"></i>Rechazar</button>
                <button class="btn btn-success fw-semibold" id="btnPresAprobar"><i class="bi bi-check-lg me-1"></i>Aprobar presupuesto</button>`;
            document.getElementById('btnPresAprobar').onclick = () => accionPresModal('APROBADO', planId);
            document.getElementById('btnPresRechazar').onclick = () => accionPresModal('RECHAZADO', planId);
            document.getElementById('btnPresAnular').onclick = () => accionPresModal('ANULADO', planId);
        } else if (esAprobado) {
            footer.innerHTML += `
                <button class="btn btn-teal btn-sm fw-bold text-white shadow-sm me-auto" id="btnPresCobrarModal" style="background-color: #0b7f86;">
                    <i class="bi bi-wallet2 me-1"></i> Registrar Cobro Ahora
                </button>
                <button class="btn btn-outline-danger btn-sm" id="btnPresAnular"><i class="bi bi-slash-circle me-1"></i>Anular</button>`;
            document.getElementById('btnPresCobrarModal').onclick = () => {
                _presModalInstance?.hide();
                abrirModalCobro(d.id, planId, saldo);
            };
            document.getElementById('btnPresAnular').onclick = () => accionPresModal('ANULADO', planId);
        } else {
            footer.innerHTML += `<span class="text-muted small"><i class="bi bi-lock me-1"></i>Presupuesto ${(_pLabels[estado] || estado).toLowerCase()} — sin más acciones disponibles</span>`;
        }
    }

    async function accionPresModal(nuevoEstado, planId) {
        const etiquetas = { APROBADO: 'aprobar', RECHAZADO: 'rechazar', ANULADO: 'anular' };
        const necesitaMotivo = nuevoEstado !== 'APROBADO';
        let motivo = '';
        if (necesitaMotivo) {
            motivo = window.prompt(`Motivo para ${etiquetas[nuevoEstado]} (obligatorio):`) || '';
            if (!motivo.trim()) { mostrarAviso('El motivo es obligatorio para continuar.', 'error'); return; }
        }
        const errEl = document.getElementById('modalPresError');
        errEl.style.display = 'none';
        try {
            const res = await fetch(`/interno/gestion/presupuestos/${_presModalCurrentId}/estado`, {
                method: 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: _csrfHeaders(),
                body: JSON.stringify({ estado: nuevoEstado, motivo, version: _presModalCurrentVersion })
            });
            const j = res.headers.get('content-type')?.includes('json') ? await res.json() : {};
            if (!res.ok) throw new Error(j.error || 'Error al cambiar el estado.');
            // Recargar el plan para reflejar el nuevo estado
            await cargarPaciente(state.pacienteId);
            _presModalInstance?.hide();
            mostrarAviso(`Presupuesto ${etiquetas[nuevoEstado]}do correctamente.`);
        } catch (e) {
            errEl.textContent = e.message;
            errEl.style.display = 'block';
        }
    }

    async function generarPresupuestoDesdePlan(planId) {
        try {
            const res = await api(`/interno/clinica/api/planes/${planId}/generar-presupuesto`, { method: 'POST' });
            await cargarPaciente(state.pacienteId);
            mostrarAviso(res.mensaje || 'Presupuesto generado y enlazado con éxito.');
        } catch (e) {
            mostrarAviso(e.message, 'error');
        }
    }

    // ─── Presupuesto inline (sin salir de la vista) ───────────────────────────
    const _presCache = {};
    const _presLabels = { BORRADOR: 'Borrador', APROBADO: 'Aprobado', RECHAZADO: 'Rechazado', ANULADO: 'Anulado' };
    const _presClasses = { BORRADOR: 'secondary', APROBADO: 'success', RECHAZADO: 'warning', ANULADO: 'danger' };
    const _money = v => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(Number(v || 0));
    const _date = v => v ? new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(String(v).length === 10 ? v + 'T12:00:00' : v)) : '—';

    function csrfHeader() {
        const t = document.querySelector('meta[name="csrf-token"]')?.content;
        const h = document.querySelector('meta[name="csrf-header"]')?.content;
        return t && h ? { [h]: t } : {};
    }

    async function togglePresupuestoInline(presId, planId, btnEl) {
        const panel = document.getElementById('pres-inline-' + planId);
        if (!panel) return;
        if (panel.style.display !== 'none') { panel.style.display = 'none'; btnEl.innerHTML = `<i class="bi bi-receipt"></i> Ver Presupuesto #${presId}`; return; }
        btnEl.disabled = true;
        btnEl.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Cargando…';
        try {
            const d = await fetchPresupuesto(presId);
            renderPresupuestoInline(panel, d, planId);
            panel.style.display = 'block';
            btnEl.innerHTML = `<i class="bi bi-x-lg"></i> Cerrar presupuesto`;
        } catch (e) {
            mostrarAviso(e.message, 'error');
            btnEl.innerHTML = `<i class="bi bi-receipt"></i> Ver Presupuesto #${presId}`;
        } finally {
            btnEl.disabled = false;
        }
    }

    async function fetchPresupuesto(presId) {
        const res = await fetch(`/interno/gestion/presupuestos/${presId}`, { credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } });
        const json = res.headers.get('content-type')?.includes('json') ? await res.json() : null;
        if (!res.ok) throw new Error(json?.error || 'No se pudo cargar el presupuesto.');
        return json;
    }

    function renderPresupuestoInline(panel, d, planId) {
        const estado = d.estado || 'BORRADOR';
        const esBorrador = estado === 'BORRADOR';
        const esAprobado = estado === 'APROBADO';
        const saldo = (Number(d.total || 0) - Number(d.pagado || 0)).toFixed(2);
        const items = (d.items || []).map(it => `
            <tr>
                <td style="padding:.3rem .5rem;font-size:.84rem;">${escape(it.descripcion)}</td>
                <td style="padding:.3rem .5rem;font-size:.84rem;text-align:center;">${it.cantidad}</td>
                <td style="padding:.3rem .5rem;font-size:.84rem;text-align:right;">${_money(it.precio)}</td>
                <td style="padding:.3rem .5rem;font-size:.84rem;text-align:right;font-weight:700;">${_money(Number(it.precio) * Number(it.cantidad))}</td>
            </tr>`).join('');

        panel.innerHTML = `
            <div style="border:1.5px solid #e2e8f0;border-radius:.75rem;padding:1.25rem;margin-top:.75rem;background:#f8fafc;">
                <div style="display:flex;flex-wrap:wrap;gap:.75rem;align-items:flex-start;justify-content:space-between;margin-bottom:.75rem;">
                    <div>
                        <div style="font-size:.72rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:#94a3b8;margin-bottom:.2rem;">PRESUPUESTO #${escape(String(d.id))}</div>
                        <div style="font-weight:700;font-size:1rem;">${escape(d.titulo || '')}</div>
                        <div style="font-size:.82rem;color:#64748b;">Vigente hasta: ${_date(d.vence)} &nbsp;·&nbsp; Creado: ${_date(d.fecha)}</div>
                    </div>
                    <div style="text-align:right;">
                        <span style="display:inline-block;padding:.2rem .65rem;border-radius:2rem;font-size:.72rem;font-weight:700;text-transform:uppercase;background:var(--bs-${_presClasses[estado]}-bg-subtle,#e2e8f0);color:var(--bs-${_presClasses[estado]}-text-emphasis,#475569);">${_presLabels[estado] || estado}</span>
                        <div style="font-size:1.2rem;font-weight:800;color:#0f172a;margin-top:.2rem;">${_money(d.total)}</div>
                        <div style="font-size:.78rem;color:#64748b;">Saldo: <strong>${_money(saldo)}</strong> · Abonado: ${_money(d.pagado)}</div>
                    </div>
                </div>

                ${d.notas ? `<div style="background:#fff;border:1px solid #e2e8f0;border-radius:.5rem;padding:.6rem .85rem;font-size:.84rem;color:#475569;margin-bottom:.75rem;"><i class="bi bi-chat-left-text me-1"></i>${escape(d.notas)}</div>` : ''}

                <table style="width:100%;border-collapse:collapse;margin-bottom:.75rem;">
                    <thead><tr style="border-bottom:1.5px solid #e2e8f0;">
                        <th style="padding:.25rem .5rem;font-size:.75rem;color:#94a3b8;font-weight:600;">Concepto</th>
                        <th style="padding:.25rem .5rem;font-size:.75rem;color:#94a3b8;font-weight:600;text-align:center;">Cant.</th>
                        <th style="padding:.25rem .5rem;font-size:.75rem;color:#94a3b8;font-weight:600;text-align:right;">P.Unit.</th>
                        <th style="padding:.25rem .5rem;font-size:.75rem;color:#94a3b8;font-weight:600;text-align:right;">Subtotal</th>
                    </tr></thead>
                    <tbody>${items}</tbody>
                    <tfoot><tr style="border-top:2px solid #e2e8f0;">
                        <td colspan="3" style="padding:.4rem .5rem;font-weight:700;font-size:.9rem;text-align:right;">TOTAL:</td>
                        <td style="padding:.4rem .5rem;font-weight:800;font-size:1rem;text-align:right;color:#10b981;">${_money(d.total)}</td>
                    </tr></tfoot>
                </table>

                <div style="display:flex;gap:.5rem;flex-wrap:wrap;justify-content:flex-end;" id="pres-acciones-${planId}">
                    ${esBorrador ? `
                        <button class="btn btn-sm btn-outline-primary pres-inline-editar" data-presid="${d.id}" data-planid="${planId}" data-version="${d.version}"><i class="bi bi-pencil"></i> Editar</button>
                        <button class="btn btn-sm btn-success pres-inline-estado" data-presid="${d.id}" data-planid="${planId}" data-version="${d.version}" data-nuevo="APROBADO"><i class="bi bi-check-lg"></i> Aprobar</button>
                        <button class="btn btn-sm btn-outline-warning pres-inline-estado" data-presid="${d.id}" data-planid="${planId}" data-version="${d.version}" data-nuevo="RECHAZADO"><i class="bi bi-x-lg"></i> Rechazar</button>
                        <button class="btn btn-sm btn-outline-danger pres-inline-estado" data-presid="${d.id}" data-planid="${planId}" data-version="${d.version}" data-nuevo="ANULADO"><i class="bi bi-slash-circle"></i> Anular</button>
                    ` : ''}
                    ${esAprobado ? `
                        <span class="text-muted small d-flex align-items-center"><i class="bi bi-info-circle me-1"></i>Presupuesto aprobado. Gestiona los cobros en la sección <strong class="ms-1">Pagos y Cobros</strong>.</span>
                        <button class="btn btn-sm btn-outline-danger pres-inline-estado" data-presid="${d.id}" data-planid="${planId}" data-version="${d.version}" data-nuevo="ANULADO"><i class="bi bi-slash-circle"></i> Anular</button>
                    ` : ''}
                    ${(!esBorrador && !esAprobado) ? `<span class="text-muted small"><i class="bi bi-lock me-1"></i>Este presupuesto no admite más cambios.</span>` : ''}
                </div>
                <div class="pres-inline-error" id="pres-err-${planId}" style="display:none;background:#fee2e2;color:#991b1b;border-radius:.4rem;padding:.5rem .75rem;font-size:.83rem;margin-top:.5rem;"></div>
            </div>`;

        // Listeners de acciones inline
        panel.querySelectorAll('.pres-inline-estado').forEach(btn => {
            btn.addEventListener('click', () => accionEstadoInline(btn.dataset.presid, btn.dataset.planid, Number(btn.dataset.version), btn.dataset.nuevo));
        });
        panel.querySelectorAll('.pres-inline-editar').forEach(btn => {
            btn.addEventListener('click', () => abrirEditorPresupuestoInline(btn.dataset.presid, btn.dataset.planid));
        });
    }

    async function accionEstadoInline(presId, planId, version, nuevoEstado) {
        const etiquetas = { APROBADO: 'aprobar', RECHAZADO: 'rechazar', ANULADO: 'anular' };
        const necesitaMotivo = nuevoEstado !== 'APROBADO';
        let motivo = '';
        if (necesitaMotivo) {
            motivo = prompt(`Motivo para ${etiquetas[nuevoEstado]} el presupuesto (obligatorio):`, '') || '';
            if (!motivo.trim()) { mostrarAviso('Debes ingresar un motivo para continuar.', 'error'); return; }
        }
        const errEl = document.getElementById('pres-err-' + planId);
        try {
            await fetch(`/interno/gestion/presupuestos/${presId}/estado`, {
                method: 'POST',
                credentials: 'same-origin',
                cache: 'no-store',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json', ...csrfHeader() },
                body: JSON.stringify({ estado: nuevoEstado, motivo, version })
            }).then(async r => {
                const j = r.headers.get('content-type')?.includes('json') ? await r.json() : {};
                if (!r.ok) throw new Error(j.error || 'Error al cambiar el estado.');
            });
            // Recargar el panel
            const panel = document.getElementById('pres-inline-' + planId);
            const d = await fetchPresupuesto(presId);
            renderPresupuestoInline(panel, d, planId);
            mostrarAviso(`Presupuesto ${etiquetas[nuevoEstado]}do correctamente.`);
        } catch (e) {
            if (errEl) { errEl.textContent = e.message; errEl.style.display = 'block'; }
            else mostrarAviso(e.message, 'error');
        }
    }

    function abrirEditorPresupuestoInline(presId, planId) {
        // Redirige a la ficha del paciente en la pestaña de presupuestos para editar
        const pid = state.pacienteId;
        if (pid) window.location.href = `/interno/pacientes/${pid}#presupuestos`;
        else mostrarAviso('No se pudo identificar al paciente.', 'error');
    }

    // ============================================================
    // 6. EVOLUCIÓN CLÍNICA Y RECETA MÉDICA OFICIAL
    // ============================================================
    function abrirModalReceta(atencionId) {
        if (!state.expediente?.paciente) {
            mostrarAviso('Selecciona un paciente para emitir o consultar su receta médica.', 'warning');
            return;
        }
        const pac = state.expediente.paciente;
        const atenciones = state.expediente.atenciones || [];
        const atencion = atencionId ? atenciones.find(a => a.id === Number(atencionId)) : atenciones[0];

        const nombrePac = pac.nombreCompleto || `${pac.nombres || ''} ${pac.apellidos || ''}`.trim() || 'Paciente';
        const dniPac = pac.numeroDocumento || pac.dni || 'Sin documento';
        const fecha = atencion?.fechaAtencion ? formatDate(atencion.fechaAtencion) : formatDate(new Date());
        const doctor = atencion?.nombreProfesional || atencion?.creadoPor || 'Dra. María Elena Ramos Paredes';
        const proc = atencion?.planProcedimiento ? `${atencion.planProcedimiento}${atencion.piezasTratadas ? ' (Pieza ' + atencion.piezasTratadas + ')' : ''}` : 'Tratamiento Odontológico General';
        const recetaTexto = atencion?.receta || atencion?.indicacionesReceta || '1. Amoxicilina 500mg: Tomar 1 cápsula cada 8 horas por 7 días vía oral.\n2. Ibuprofeno 400mg: Tomar 1 tableta cada 8 horas por 3 días después de las comidas.\n3. Enjuague bucal antiséptico con Clorhexidina 0.12% tras cepillado suave.';

        $('#receta-fecha-emision').textContent = `Fecha: ${fecha}`;
        $('#receta-paciente-nombre').textContent = nombrePac;
        $('#receta-paciente-dni').textContent = dniPac;
        $('#receta-procedimiento').textContent = proc;
        $('#receta-doctor-nombre').textContent = doctor;
        $('#receta-firma-doctor').textContent = doctor;
        $('#receta-contenido-texto').textContent = recetaTexto;
        const ts = $('#receta-timestamp');
        if (ts) ts.textContent = `Documento emitido el ${formatDateTime(new Date())}`;

        // Guardar referencia activa para copiar / imprimir
        state.recetaActiva = { nombrePac, dniPac, fecha, doctor, proc, recetaTexto };

        bootstrap.Modal.getOrCreateInstance($('#modal-receta-medica')).show();
    }

    function renderAtenciones() {
        const list = state.expediente.atenciones || [];
        const container = $('#contenedor-atenciones');
        if (!container) return;

        if (list.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5">
                    <i class="bi bi-journal-medical text-teal" style="font-size: 3rem;"></i>
                    <h3 class="h5 fw-bold mt-3">Sin atenciones clínicas registradas</h3>
                    <p class="text-secondary small">Los procedimientos odontológicos registrados en consulta aparecerán agrupados por sesión y cita aquí.</p>
                </div>
            `;
            return;
        }

        // Agrupar atenciones por Cita o por Sesión de Fecha
        const sesiones = [];
        const sesionIndexMap = new Map();

        list.forEach(a => {
            const fechaDia = a.fechaAtencion ? String(a.fechaAtencion).slice(0, 10) : 'sin-fecha';
            const citaAsociada = a.citaId 
                ? (state.expediente?.citas || []).find(c => Number(c.id) === Number(a.citaId))
                : null;
            const claveSesion = a.citaId ? `cita_${a.citaId}` : `fecha_${fechaDia}_${a.profesionalId || a.creadoPor || 'prof'}`;

            if (!sesionIndexMap.has(claveSesion)) {
                sesionIndexMap.set(claveSesion, sesiones.length);
                sesiones.push({
                    clave: claveSesion,
                    citaId: a.citaId || null,
                    cita: citaAsociada,
                    fecha: a.fechaAtencion,
                    fechaDia: fechaDia,
                    profesional: a.nombreProfesional || citaAsociada?.profesional || a.creadoPor || 'Especialista',
                    tipoAtencion: citaAsociada?.tipoAtencion || (a.citaId ? 'Cita programada' : 'Atención en consultorio'),
                    procedimientos: [],
                    recetaTexto: null,
                    recetaAtencionId: null
                });
            }

            const ses = sesiones[sesionIndexMap.get(claveSesion)];
            ses.procedimientos.push(a);
            if (!ses.recetaTexto && (a.receta || a.indicacionesReceta)) {
                ses.recetaTexto = a.receta || a.indicacionesReceta;
                ses.recetaAtencionId = a.id;
            }
        });

        container.innerHTML = `
            <div class="timeline-atenciones-clean">
                ${sesiones.map(ses => `
                    <div class="clinica-evo-card mb-4 p-3.5 bg-white rounded-3 border shadow-xs">
                        <div class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom flex-wrap gap-2">
                            <div class="d-flex align-items-center gap-2 flex-wrap">
                                <span class="badge bg-teal text-white px-2.5 py-1.5 fw-bold" style="background-color: #0b7f86;">
                                    <i class="bi bi-calendar-event me-1"></i> ${formatDate(ses.fecha)}
                                </span>
                                ${ses.citaId ? `
                                    <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">
                                        <i class="bi bi-calendar-check me-1"></i> Cita #${ses.citaId} · ${escape(ses.tipoAtencion)}
                                    </span>
                                ` : `
                                    <span class="badge bg-light text-secondary border px-2 py-1">
                                        <i class="bi bi-heart-pulse me-1"></i> Atención directa en consultorio
                                    </span>
                                `}
                                <span class="badge bg-light text-dark border px-2 py-1">
                                    ${ses.procedimientos.length} ${ses.procedimientos.length === 1 ? 'procedimiento' : 'procedimientos'}
                                </span>
                            </div>
                            <span class="text-secondary small">
                                <i class="bi bi-person-badge text-teal me-1"></i> Odontólogo: <strong>${escape(ses.profesional)}</strong>
                            </span>
                        </div>

                        <div class="procedimientos-sesion-lista mb-3">
                            <label class="text-uppercase text-secondary fw-bold small mb-2 d-block" style="font-size: 0.72rem; letter-spacing: 0.05em;">
                                Procedimientos realizados en esta sesión:
                            </label>
                            ${ses.procedimientos.map((p, idx) => `
                                <div class="p-2.5 rounded-3 bg-light border mb-2">
                                    <div class="d-flex justify-content-between align-items-center mb-1 flex-wrap gap-1">
                                        <div class="d-flex align-items-center gap-2">
                                            <span class="badge bg-white text-teal border border-teal-subtle" style="font-size: 0.75rem; color: #0b7f86 !important;">
                                                Procedimiento ${idx + 1}
                                            </span>
                                            ${p.piezasTratadas ? `
                                                <span class="badge bg-teal-soft text-teal border border-teal-subtle">
                                                    <i class="bi bi-pin-angle me-0.5"></i> Pieza(s) ${escape(p.piezasTratadas)}
                                                </span>
                                            ` : ''}
                                        </div>
                                        <small class="text-muted"><i class="bi bi-clock me-1"></i>${formatTime(p.fechaAtencion)}</small>
                                    </div>
                                    <div class="fw-bold text-dark fs-6" style="color: #0b7f86 !important;">
                                        <i class="bi bi-check-circle-fill text-success me-1"></i> ${escape(p.planProcedimiento)}
                                    </div>
                                    ${p.subjetivo ? `
                                        <div class="small text-secondary mt-1">
                                            <strong class="text-dark"><i class="bi bi-chat-left-text me-1 text-teal"></i> Observaciones:</strong> ${escape(p.subjetivo)}
                                        </div>
                                    ` : ''}
                                    ${p.materiales ? `
                                        <div class="small text-muted mt-1"><i class="bi bi-box-seam me-1"></i> Materiales: ${escape(p.materiales)}</div>
                                    ` : ''}
                                </div>
                            `).join('')}
                        </div>

                        ${ses.recetaTexto ? `
                            <div class="alert alert-success border-success-subtle bg-success-subtle text-dark py-2.5 px-3 small mb-0 rounded-3">
                                <div class="d-flex justify-content-between align-items-center mb-1 flex-wrap gap-2">
                                    <strong class="text-success"><i class="bi bi-prescription2 me-1"></i> Prescripción / Receta médica de la sesión:</strong>
                                    <div class="d-flex gap-1">
                                        <button type="button" class="btn btn-xs btn-outline-teal bg-white btn-ver-receta-card" data-id="${ses.recetaAtencionId}">
                                            <i class="bi bi-printer-fill me-1"></i> Ver / Imprimir Receta
                                        </button>
                                        <button type="button" class="btn btn-xs btn-outline-success bg-white btn-wa-receta-card" data-id="${ses.recetaAtencionId}">
                                            <i class="bi bi-whatsapp me-1"></i> WhatsApp
                                        </button>
                                    </div>
                                </div>
                                <div style="white-space: pre-line;" class="mt-1">${escape(ses.recetaTexto)}</div>
                            </div>
                        ` : ''}
                    </div>
                `).join('')}
            </div>
        `;

        $$('.btn-ver-receta-card').forEach(btn => {
            btn.addEventListener('click', () => abrirModalReceta(btn.dataset.id));
        });

        $$('.btn-wa-receta-card').forEach(btn => {
            btn.addEventListener('click', () => {
                abrirModalReceta(btn.dataset.id);
                $('#btn-copiar-receta-wa')?.click();
            });
        });
    }


    

    // ============================================================
    // 7. CUENTAS, PAGOS Y DEUDAS DEL PACIENTE
    // ============================================================
    function renderCuentasPagos() {
        const fin = state.expediente?.finanzas || {};
        const total = Number(fin.totalPresupuestado || 0);
        const pagado = Number(fin.totalPagado || 0);
        const deuda = Number(fin.saldoDeudor || 0);

        const elTotal = $('#kpi-fin-total');
        if (elTotal) elTotal.textContent = money(total);
        const elPresCount = $('#kpi-fin-pres-count');
        if (elPresCount) elPresCount.textContent = `${(fin.presupuestos || []).length} presupuestos`;

        const elPagado = $('#kpi-fin-pagado');
        if (elPagado) elPagado.textContent = money(pagado);
        const elPagosCount = $('#kpi-fin-pagos-count');
        if (elPagosCount) elPagosCount.textContent = `${(fin.pagos || []).length} abonos registrados`;

        const elDeuda = $('#kpi-fin-deuda');
        if (elDeuda) elDeuda.textContent = money(deuda);
        const elDeudaStatus = $('#kpi-fin-deuda-status');
        const elDeudaCard = $('#kpi-fin-deuda-card');
        if (elDeudaStatus && elDeudaCard) {
            if (deuda > 0) {
                elDeudaStatus.textContent = '⚠️ Deuda pendiente por cobrar';
                elDeudaCard.className = 'kpi-financial-card p-3 rounded-4 border bg-danger-subtle border-danger-subtle shadow-sm';
            } else {
                elDeudaStatus.textContent = '✅ Al día (Sin deudas)';
                elDeudaCard.className = 'kpi-financial-card p-3 rounded-4 border bg-light border-success-subtle shadow-sm';
            }
        }

        // Habilitar botón de registrar abono rápido en cabecera
        const btnAbonoRapido = $('#btn-registrar-abono-rapido');
        const presupuestos = fin.presupuestos || [];
        const presConSaldo = presupuestos.find(p => Number(p.saldo !== undefined ? p.saldo : p.total) > 0 && p.estado === 'APROBADO') || presupuestos[0];
        if (btnAbonoRapido) {
            btnAbonoRapido.disabled = !presConSaldo;
            btnAbonoRapido.onclick = () => {
                if (presConSaldo) abrirModalCobro(presConSaldo.id, null, presConSaldo.saldo);
            };
        }

        // Render Presupuestos en contenedor-fin-presupuestos
        const contPres = $('#contenedor-fin-presupuestos');
        if (contPres) {
            if (presupuestos.length === 0) {
                contPres.innerHTML = '<p class="text-secondary text-center py-4 small">Sin presupuestos registrados para este paciente.</p>';
            } else {
                contPres.innerHTML = `
                    <div class="table-responsive">
                        <table class="table table-hover align-middle table-sm mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Presupuesto</th>
                                    <th>Total</th>
                                    <th>Abonado</th>
                                    <th>Deuda Pendiente</th>
                                    <th>Estado</th>
                                    <th class="text-end">Acción</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${presupuestos.map(p => {
                                    const saldoNum = Number(p.saldo !== undefined ? p.saldo : p.total);
                                    const tieneDeuda = saldoNum > 0 && p.estado === 'APROBADO';
                                    return `
                                    <tr>
                                        <td>
                                            <strong class="d-block text-dark">${escape(p.titulo || 'Presupuesto #' + p.id)}</strong>
                                            <small class="text-secondary">Emitido: ${formatDate(p.fecha)} · Vence: ${formatDate(p.vence)}</small>
                                        </td>
                                        <td class="fw-bold text-dark">${money(p.total)}</td>
                                        <td class="text-success fw-semibold">${money(p.pagado)}</td>
                                        <td class="fw-bold ${saldoNum > 0 ? 'text-danger' : 'text-secondary'}">${money(saldoNum)}</td>
                                        <td>
                                            <span class="badge ${p.estado === 'APROBADO' ? 'bg-success-subtle text-success' : 'bg-light text-secondary'} border">
                                                ${escape(p.estado || 'BORRADOR')}
                                            </span>
                                        </td>
                                        <td class="text-end">
                                            ${tieneDeuda ? `
                                                <button type="button" class="btn btn-sm btn-teal text-white fw-bold shadow-sm btn-cobrar-fila" style="background-color:#0b7f86;" data-presid="${p.id}" data-saldo="${saldoNum}">
                                                    <i class="bi bi-wallet2 me-1"></i>Cobrar
                                                </button>
                                            ` : p.estado === 'BORRADOR' ? `
                                                <button type="button" class="btn btn-sm btn-success fw-semibold btn-aprobar-pres-fila" data-presid="${p.id}">
                                                    <i class="bi bi-check-lg me-1"></i>Aprobar
                                                </button>
                                            ` : `
                                                <span class="badge bg-light text-muted border">Al día</span>
                                            `}
                                        </td>
                                    </tr>
                                    `;
                                }).join('')}
                            </tbody>
                        </table>
                    </div>
                `;

                contPres.querySelectorAll('.btn-cobrar-fila').forEach(btn => {
                    btn.addEventListener('click', () => {
                        abrirModalCobro(btn.dataset.presid, null, btn.dataset.saldo);
                    });
                });

                contPres.querySelectorAll('.btn-aprobar-pres-fila').forEach(btn => {
                    btn.addEventListener('click', async () => {
                        try {
                            await accionPresModal('APROBADO', null, btn.dataset.presid);
                        } catch (e) {
                            mostrarAviso(e.message, 'error');
                        }
                    });
                });
            }
        }

        // Render Pagos
        const tbodyPagos = $('#tabla-fin-pagos-body');
        if (tbodyPagos) {
            const pagos = fin.pagos || [];
            if (pagos.length === 0) {
                tbodyPagos.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3">No hay abonos registrados para este paciente.</td></tr>';
            } else {
                tbodyPagos.innerHTML = pagos.map(pg => `
                    <tr>
                        <td>
                            <strong class="d-block text-dark">${formatDate(pg.fecha)}</strong>
                        </td>
                        <td>
                            <span class="text-dark small">${escape(pg.presupuestoTitulo || 'Presupuesto')}</span>
                        </td>
                        <td>
                            <span class="badge bg-light text-dark border"><i class="bi bi-credit-card me-1"></i>${escape(pg.metodo || 'EFECTIVO')}</span>
                            ${pg.referencia ? `<small class="d-block text-muted" style="font-size:0.75rem;">Ref: ${escape(pg.referencia)}</small>` : ''}
                        </td>
                        <td class="fw-bold text-success text-end">${money(pg.monto)}</td>
                        <td>
                            <span class="badge ${pg.estado === 'REGISTRADO' ? 'bg-success-subtle text-success' : 'bg-danger-subtle text-danger'} border">
                                ${escape(pg.estado || 'REGISTRADO')}
                            </span>
                        </td>
                        <td class="text-end text-muted small">
                            ${escape(pg.creadoPor || '')}
                        </td>
                    </tr>
                `).join('');
            }
        }
    }

    // ============================================================
    // 8. HISTORIAL COMPLETO DE CITAS
    // ============================================================
    function renderHistorialCitas() {
        const citas = state.expediente?.citas || [];
        const atenciones = state.expediente?.atenciones || [];
        const tbody = $('#tabla-historial-citas-body');
        if (!tbody) return;

        if (citas.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No hay citas registradas en el historial de este paciente.</td></tr>';
            return;
        }

        const estadoBadge = est => {
            switch (est) {
                case 'ATENDIDA': return '<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-check2-all me-1"></i> Atendida</span>';
                case 'EN_ATENCION': return '<span class="badge bg-primary text-white"><i class="bi bi-person-workspace me-1"></i> En Atención</span>';
                case 'EN_ESPERA': return '<span class="badge bg-warning-subtle text-dark border"><i class="bi bi-person-walking me-1"></i> En Sala</span>';
                case 'CONFIRMADA': return '<span class="badge bg-info-subtle text-info border"><i class="bi bi-check-circle me-1"></i> Confirmada</span>';
                case 'RESERVADA': return '<span class="badge bg-light text-secondary border">Reservada</span>';
                case 'CANCELADA': return '<span class="badge bg-danger-subtle text-danger border">Cancelada</span>';
                case 'NO_ASISTIO': return '<span class="badge bg-secondary-subtle text-muted border">No asistió</span>';
                default: return `<span class="badge bg-light text-secondary border">${escape(est)}</span>`;
            }
        };

        tbody.innerHTML = citas.map(c => {
            const fechaCitaStr = String(c.fecha || c.inicio || '').slice(0, 10);
            const atencionesCita = atenciones.filter(a => {
                if (a.citaId && String(a.citaId) === String(c.id)) return true;
                if (!a.citaId && a.fechaAtencion && String(a.fechaAtencion).startsWith(fechaCitaStr)) return true;
                return false;
            });

            return `
            <tr>
                <td>
                    <strong class="d-block text-dark">${formatDateTime(c.fecha || c.inicio)}</strong>
                    <small class="text-secondary">${formatDate(c.fecha || c.inicio)}</small>
                </td>
                <td class="fw-semibold text-dark">${escape(c.profesional || 'Odontólogo asignado')}</td>
                <td class="text-secondary">${escape(c.especialidad || 'Odontología General')}</td>
                <td><span class="badge bg-light text-dark border">${escape(c.tipoAtencion || 'Consulta')}</span></td>
                <td><small class="text-secondary text-truncate d-inline-block" style="max-width: 200px;">${escape(c.motivo || 'Control de rutina')}</small></td>
                <td>${estadoBadge(c.estado)}</td>
                <td class="text-end">
                    ${c.estado === 'ATENDIDA' || atencionesCita.length > 0 ? `
                        <button type="button" class="btn btn-sm btn-outline-teal fw-bold d-inline-flex align-items-center gap-1 btn-ver-atencion-cita" data-cita-id="${c.id}" style="color: #0b7f86; border-color: #0b7f86;">
                            <i class="bi bi-journal-medical"></i> Ver Atención y Receta
                        </button>
                    ` : c.estado === 'EN_ATENCION' ? `
                        <button type="button" class="btn btn-sm btn-primary fw-bold text-white d-inline-flex align-items-center gap-1 btn-atender-cita-tabla" data-cita-id="${c.id}">
                            <i class="bi bi-person-workspace"></i> En Atención
                        </button>
                    ` : (c.estado === 'RESERVADA' || c.estado === 'EN_ESPERA' || c.estado === 'CONFIRMADA') ? `
                        <a href="/interno/citas/atender/${c.id}" class="btn btn-sm btn-outline-primary fw-medium d-inline-flex align-items-center gap-1">
                            <i class="bi bi-play-circle"></i> Atender
                        </a>
                    ` : `
                        <span class="text-muted small">—</span>
                    `}
                </td>
            </tr>
            `;
        }).join('');

        $$('.btn-ver-atencion-cita').forEach(btn => {
            btn.addEventListener('click', () => abrirModalDetalleAtencionCita(btn.dataset.citaId));
        });

        $$('.btn-atender-cita-tabla').forEach(btn => {
            btn.addEventListener('click', () => {
                state.citaId = btn.dataset.citaId;
                abrirModalAtencion();
            });
        });
    }

    function abrirModalDetalleAtencionCita(citaId) {
        const citas = state.expediente?.citas || [];
        const atenciones = state.expediente?.atenciones || [];
        const cita = citas.find(c => String(c.id) === String(citaId));

        if (!cita) {
            mostrarAviso('No se encontró información de la cita solicitada.', 'warning');
            return;
        }

        const fechaCitaStr = String(cita.fecha || cita.inicio || '').slice(0, 10);
        const atencionesCita = atenciones.filter(a => {
            if (a.citaId && String(a.citaId) === String(cita.id)) return true;
            if (!a.citaId && a.fechaAtencion && String(a.fechaAtencion).startsWith(fechaCitaStr)) return true;
            return false;
        });

        const elFecha = $('#det-cita-fecha-hora');
        if (elFecha) elFecha.textContent = formatDateTime(cita.fecha || cita.inicio);
        const elProf = $('#det-cita-profesional');
        if (elProf) elProf.textContent = cita.profesional || 'Odontólogo asignado';
        const elEsp = $('#det-cita-especialidad');
        if (elEsp) elEsp.textContent = cita.especialidad || 'Odontología General';
        const elTipo = $('#det-cita-tipo');
        if (elTipo) elTipo.textContent = cita.tipoAtencion || 'Consulta';
        const elMotivo = $('#det-cita-motivo');
        if (elMotivo) elMotivo.textContent = cita.motivo || 'Sin motivo especificado';
        const elTitle = $('#modal-detalle-cita-title');
        if (elTitle) elTitle.textContent = `Atención Clínica · Cita #${cita.id}`;
        const elSub = $('#modal-detalle-cita-sub');
        if (elSub) elSub.textContent = `${formatDate(cita.fecha || cita.inicio)} con ${cita.profesional || 'Especialista'}`;

        const badgeEl = $('#det-cita-estado-badge');
        if (badgeEl) {
            badgeEl.className = 'badge ' + (cita.estado === 'ATENDIDA' ? 'bg-success text-white' : 'bg-primary text-white');
            badgeEl.textContent = cita.estado;
        }

        // Procedimientos realizados
        const procListaEl = $('#det-cita-procedimientos-lista');
        if (procListaEl) {
            if (atencionesCita.length === 0) {
                procListaEl.innerHTML = `
                    <div class="text-center py-3 text-muted small">
                        <i class="bi bi-info-circle me-1"></i> Esta cita no cuenta con notas de procedimientos registradas.
                    </div>
                `;
            } else {
                procListaEl.innerHTML = atencionesCita.map((a, idx) => `
                    <div class="p-2.5 rounded-2 bg-light border mb-2">
                        <div class="d-flex justify-content-between align-items-center mb-1 flex-wrap gap-1">
                            <div class="d-flex align-items-center gap-2">
                                <span class="badge bg-teal text-white" style="background-color: #0b7f86; font-size: 0.72rem;">Procedimiento ${idx + 1}</span>
                                ${a.piezasTratadas ? `<span class="badge bg-info-subtle text-info border border-info-subtle"><i class="bi bi-pin-angle me-0.5"></i> Pieza(s) ${escape(a.piezasTratadas)}</span>` : ''}
                            </div>
                            <small class="text-muted"><i class="bi bi-clock me-1"></i>${formatTime(a.fechaAtencion)}</small>
                        </div>
                        <div class="fw-bold text-dark fs-6 mb-1" style="color: #0b7f86 !important;">
                            <i class="bi bi-check-circle-fill text-success me-1"></i> ${escape(a.planProcedimiento)}
                        </div>
                        ${a.subjetivo ? `<div class="small text-secondary mb-1"><strong class="text-dark">Observaciones:</strong> ${escape(a.subjetivo)}</div>` : ''}
                        ${a.materiales ? `<div class="small text-muted"><i class="bi bi-box-seam me-1"></i> Materiales: ${escape(a.materiales)}</div>` : ''}
                    </div>
                `).join('');
            }
        }

        // Receta médica de la cita
        const atencionConReceta = atencionesCita.find(a => (a.receta || a.indicacionesReceta));
        const recetaTexto = atencionConReceta ? (atencionConReceta.receta || atencionConReceta.indicacionesReceta) : null;
        const recetaBox = $('#det-cita-receta-texto');
        const recetaAcciones = $('#det-cita-receta-acciones');

        if (recetaBox) {
            if (recetaTexto) {
                recetaBox.textContent = recetaTexto;
                if (recetaAcciones) recetaAcciones.style.display = 'flex';
                const btnImp = $('#btn-det-cita-imprimir-receta');
                if (btnImp) btnImp.onclick = () => abrirModalReceta(atencionConReceta.id);
                const btnWa = $('#btn-det-cita-wa-receta');
                if (btnWa) btnWa.onclick = () => {
                    abrirModalReceta(atencionConReceta.id);
                    $('#btn-copiar-receta-wa')?.click();
                };
            } else {
                recetaBox.innerHTML = '<span class="text-muted fst-italic">No se prescribieron medicamentos para esta cita.</span>';
                if (recetaAcciones) recetaAcciones.style.display = 'none';
            }
        }

        bootstrap.Modal.getOrCreateInstance($('#modal-detalle-atencion-cita')).show();
    }


    

    // ============================================================
    // MODALES Y FORMULARIOS INTERACTIVOS
    // ============================================================
    function abrirModalCrearPlan() {
        const modalEl = $('#modal-crear-plan');
        if (!modalEl) return;
        const form = $('#form-crear-plan');
        if (form) form.reset();
        const tbody = $('#plan-items-lista');
        if (tbody) tbody.innerHTML = '';
        agregarFilaItemPlan();
        if (state.catalogoCache && state.catalogoCache.length > 0) {
            actualizarOpcionesCatalogoEnPlan(state.catalogoCache);
        } else {
            api('/interno/clinica/api/tratamientos/catalogo').then(list => actualizarOpcionesCatalogoEnPlan(list)).catch(() => {});
        }
        new bootstrap.Modal(modalEl).show();
    }

    function abrirModalPlanConTratamiento(nombre, precio, tratId) {
        const modalEl = $('#modal-crear-plan');
        const isOpen = modalEl && modalEl.classList.contains('show');
        if (!isOpen) {
            abrirModalCrearPlan();
            const primeraFila = $('#plan-items-lista tr:first-child');
            if (primeraFila && !primeraFila.querySelector('.item-desc')?.value) {
                primeraFila.remove();
            }
        }
        agregarFilaItemPlan({ nombre, precio, tratId });
        mostrarAviso(`"${nombre}" añadido al plan.`);
    }

    function abrirModalPlanConDiagnostico(diagId, desc, pieza) {
        const modalEl = $('#modal-crear-plan');
        const isOpen = modalEl && modalEl.classList.contains('show');
        if (!isOpen) {
            abrirModalCrearPlan();
            const primeraFila = $('#plan-items-lista tr:first-child');
            if (primeraFila && !primeraFila.querySelector('.item-desc')?.value) {
                primeraFila.remove();
            }
        }
        agregarFilaItemPlan({ nombre: `Tratamiento para: ${desc}`, pieza, diagId });
    }

    function agregarFilaItemPlan(datos = {}) {
        const tbody = $('#plan-items-lista');
        if (!tbody) return;
        const tr = document.createElement('tr');
        const descVal = datos.nombre || '';
        const precioVal = datos.precio !== undefined && Number(datos.precio) > 0 ? Number(datos.precio).toFixed(2) : (datos.precio !== undefined && datos.precio !== '' ? '0.00' : '');
        const tratVal = datos.tratId || '';
        const diagVal = datos.diagId || '';
        const piezaVal = datos.pieza || '';
        const faseVal = datos.fase || 1;

        tr.innerHTML = `
            <td><input type="number" class="form-control form-control-sm item-fase" value="${faseVal}" min="1" max="10" style="width: 75px;"></td>
            <td><input type="text" class="form-control form-control-sm item-pieza" value="${piezaVal}" placeholder="Pza" style="width: 80px;"></td>
            <td>
                <input type="text" list="datalist-tratamientos-plan" class="form-control form-control-sm item-desc" value="${escape(descVal)}" placeholder="Buscar en catálogo o escribir..." required>
                <input type="hidden" class="item-diag-id" value="${diagVal}">
                <input type="hidden" class="item-trat-id" value="${tratVal}">
            </td>
            <td><input type="number" step="0.01" min="0" class="form-control form-control-sm item-precio" value="${precioVal}" placeholder="Definir importe" required style="width: 120px;"></td>
            <td class="text-end"><button type="button" class="btn btn-light btn-sm text-danger" onclick="this.closest('tr').remove()"><i class="bi bi-trash"></i></button></td>
        `;
        tbody.appendChild(tr);

        // Si el usuario escribe o elige de la lista, auto-rellenar el precio del catálogo
        const inputDesc = tr.querySelector('.item-desc');
        const inputPrecio = tr.querySelector('.item-precio');
        const inputTrat = tr.querySelector('.item-trat-id');

        inputDesc.addEventListener('change', () => {
            const val = inputDesc.value.trim().toLowerCase();
            if (state.catalogoCache && state.catalogoCache.length > 0) {
                const match = state.catalogoCache.find(t => 
                    (t.nombre && t.nombre.toLowerCase() === val) || 
                    (t.codigo && t.codigo.toLowerCase() === val)
                );
                if (match) {
                    inputDesc.value = match.nombre;
                    inputPrecio.value = Number(match.precio) > 0 ? Number(match.precio).toFixed(2) : '0.00';
                    inputTrat.value = match.id;
                }
            }
        });

        return tr;
    }

    // Borrador en memoria por paciente para evitar pérdida de datos si el modal se cierra por error
    const workingDraftAtencion = {};

    function guardarBorradorEnMemoria() {
        if (!state.pacienteId) return;
        const form = $('#form-crear-atencion');
        if (!form) return;
        workingDraftAtencion[state.pacienteId] = {
            piezasTratadas: form.elements['piezasTratadas']?.value || '',
            planProcedimiento: form.elements['planProcedimiento']?.value || '',
            indicacionesReceta: form.elements['indicacionesReceta']?.value || '',
            subjetivo: form.elements['subjetivo']?.value || ''
        };
    }

    let toothPickerInitialized = false;
    function initToothPicker() {
        if (toothPickerInitialized) return;
        const rowSup = $('#tooth-picker-row-sup');
        const rowInf = $('#tooth-picker-row-inf');
        if (!rowSup || !rowInf) return;

        // Cuadrante 1 (18..11) y Cuadrante 2 (21..28)
        const q1 = [18, 17, 16, 15, 14, 13, 12, 11];
        const q2 = [21, 22, 23, 24, 25, 26, 27, 28];
        rowSup.innerHTML = q1.map(p => `<button type="button" class="tooth-picker-chip" data-piece="${p}" title="Pieza ${p}">${p}</button>`).join('')
            + `<div class="tooth-picker-divider"></div>`
            + q2.map(p => `<button type="button" class="tooth-picker-chip" data-piece="${p}" title="Pieza ${p}">${p}</button>`).join('');

        // Cuadrante 4 (48..41) y Cuadrante 3 (31..38)
        const q4 = [48, 47, 46, 45, 44, 43, 42, 41];
        const q3 = [31, 32, 33, 34, 35, 36, 37, 38];
        rowInf.innerHTML = q4.map(p => `<button type="button" class="tooth-picker-chip" data-piece="${p}" title="Pieza ${p}">${p}</button>`).join('')
            + `<div class="tooth-picker-divider"></div>`
            + q3.map(p => `<button type="button" class="tooth-picker-chip" data-piece="${p}" title="Pieza ${p}">${p}</button>`).join('');

        // Clic individual en pieza
        $$('.tooth-picker-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                chip.classList.toggle('active');
                actualizarInputDesdeChips();
            });
        });

        // Botón abrir/cerrar selector
        $('#btn-toggle-tooth-picker')?.addEventListener('click', () => {
            const picker = $('#atencion-tooth-picker');
            if (picker) picker.hidden = !picker.hidden;
        });

        // Checkbox "Todas las piezas"
        $('#chk-todas-piezas')?.addEventListener('change', e => {
            const isChecked = e.target.checked;
            $$('.tooth-picker-chip').forEach(c => c.classList.toggle('active', isChecked));
            const input = $('#atencion-pieza-input');
            if (input) {
                input.value = isChecked ? 'Todas las piezas (11-48)' : '';
                input.classList.remove('is-invalid');
            }
            guardarBorradorEnMemoria();
        });

        // Selección rápida Superior (18..28)
        $('#btn-quick-sel-sup')?.addEventListener('click', () => {
            const supPieces = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28];
            const allSupActive = supPieces.every(num => $(`#tooth-picker-row-sup [data-piece="${num}"]`)?.classList.contains('active'));
            supPieces.forEach(num => {
                const el = $(`#tooth-picker-row-sup [data-piece="${num}"]`);
                if (el) el.classList.toggle('active', !allSupActive);
            });
            actualizarInputDesdeChips();
        });

        // Selección rápida Inferior (48..38)
        $('#btn-quick-sel-inf')?.addEventListener('click', () => {
            const infPieces = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38];
            const allInfActive = infPieces.every(num => $(`#tooth-picker-row-inf [data-piece="${num}"]`)?.classList.contains('active'));
            infPieces.forEach(num => {
                const el = $(`#tooth-picker-row-inf [data-piece="${num}"]`);
                if (el) el.classList.toggle('active', !allInfActive);
            });
            actualizarInputDesdeChips();
        });

        // Limpiar piezas
        $('#btn-quick-sel-limpiar')?.addEventListener('click', () => {
            $$('.tooth-picker-chip').forEach(c => c.classList.remove('active'));
            const chkAll = $('#chk-todas-piezas');
            if (chkAll) chkAll.checked = false;
            const input = $('#atencion-pieza-input');
            if (input) input.value = '';
            guardarBorradorEnMemoria();
        });

        // Input manual
        $('#atencion-pieza-input')?.addEventListener('input', () => {
            sincronizarChipsDesdeInput();
            guardarBorradorEnMemoria();
        });

        toothPickerInitialized = true;
    }

    function actualizarInputDesdeChips() {
        const input = $('#atencion-pieza-input');
        const chkAll = $('#chk-todas-piezas');
        const labelTexto = $('#atencion-piezas-seleccionadas-texto');
        if (!input) return;
        const activeChips = Array.from($$('.tooth-picker-chip.active')).map(c => Number(c.dataset.piece));
        if (activeChips.length === 32) {
            input.value = 'Todas las piezas (11-48)';
            if (chkAll) chkAll.checked = true;
            if (labelTexto) labelTexto.innerHTML = '<span class="badge bg-teal text-white">Todas las piezas (11-48)</span>';
        } else if (activeChips.length > 0) {
            activeChips.sort((a, b) => a - b);
            const formatted = activeChips.join(', ');
            input.value = formatted.length > 95 ? formatted.slice(0, 95) : formatted;
            if (chkAll) chkAll.checked = false;
            if (labelTexto) {
                if (activeChips.length <= 6) {
                    labelTexto.innerHTML = activeChips.map(p => `<span class="badge bg-teal-subtle text-teal border border-teal-subtle me-0.5">${p}</span>`).join(' ');
                } else {
                    labelTexto.innerHTML = `<span class="badge bg-teal text-white">${activeChips.length} piezas seleccionadas</span>`;
                }
            }
        } else {
            input.value = '';
            if (chkAll) chkAll.checked = false;
            if (labelTexto) labelTexto.innerHTML = '<span class="text-muted">Ninguna (General)</span>';
        }
        input.classList.remove('is-invalid');
        guardarBorradorEnMemoria();
    }

    function sincronizarChipsDesdeInput() {
        const input = $('#atencion-pieza-input');
        const chkAll = $('#chk-todas-piezas');
        const labelTexto = $('#atencion-piezas-seleccionadas-texto');
        if (!input) return;
        const val = (input.value || '').trim().toLowerCase();
        if (val.includes('todas') || val.includes('completa') || val.includes('11-48')) {
            $$('.tooth-picker-chip').forEach(c => c.classList.add('active'));
            if (chkAll) chkAll.checked = true;
            if (labelTexto) labelTexto.innerHTML = '<span class="badge bg-teal text-white">Todas las piezas (11-48)</span>';
            return;
        }
        if (chkAll) chkAll.checked = false;
        const nums = (val.match(/\b(1[1-8]|2[1-8]|3[1-8]|4[1-8])\b/g) || []).map(Number);
        const set = new Set(nums);
        $$('.tooth-picker-chip').forEach(c => {
            c.classList.toggle('active', set.has(Number(c.dataset.piece)));
        });
        if (set.size === 32 && chkAll) {
            chkAll.checked = true;
        }
        if (labelTexto) {
            if (set.size === 0) {
                labelTexto.innerHTML = '<span class="text-muted">Ninguna (General)</span>';
            } else if (set.size <= 6) {
                const arr = Array.from(set).sort((a, b) => a - b);
                labelTexto.innerHTML = arr.map(p => `<span class="badge bg-teal-subtle text-teal border border-teal-subtle me-0.5">${p}</span>`).join(' ');
            } else {
                labelTexto.innerHTML = `<span class="badge bg-teal text-white">${set.size} piezas seleccionadas</span>`;
            }
        }
    }

    function abrirModalAtencion() {
        if (!state.expediente || root.dataset.patientLoading === 'true') { mostrarAviso('Selecciona un paciente y espera a que cargue.', 'error'); return; }
        const form = $('#form-crear-atencion');
        if (!form) return;

        // Validar si la cita vinculada ya cuenta con una atención previa (regla: 1 cita = 1 atención)
        if (state.citaId) {
            const citaYaAtendida = state.expediente?.atenciones?.some(a => String(a.citaId) === String(state.citaId));
            const citaObj = state.expediente?.citas?.find(c => String(c.id) === String(state.citaId));
            if (citaYaAtendida || (citaObj && citaObj.estado === 'ATENDIDA')) {
                state.citaId = null;
                delete root.dataset.citaId;
                if (form.elements.citaId) form.elements.citaId.value = '';
                const url = new URL(window.location);
                url.searchParams.delete('citaId');
                window.history.replaceState({}, '', url);
            }
        }

        if (!form.elements['itemPlanIdCompletar']?.value) {
            const planNotice = $('#atencion-modal-plan-notice');
            if (planNotice) planNotice.hidden = true;
        }

        initToothPicker();

        // Conservación automática de borrador en memoria mientras permanece en este paciente
        const draftEnMemoria = workingDraftAtencion[state.pacienteId];
        const serverDraft = state.expediente.borradorAtencion;
        if (!atencionEditor) {
            form.reset();
            if (form.elements.citaId) form.elements.citaId.value = state.citaId || '';
            if (draftEnMemoria) {
                Object.entries(draftEnMemoria).forEach(([key, value]) => {
                    if (form.elements[key]) form.elements[key].value = value ?? '';
                });
            } else if (serverDraft?.contenido) {
                Object.entries(serverDraft.contenido).forEach(([key, value]) => {
                    if (form.elements[key]) form.elements[key].value = value ?? '';
                });
            }
            atencionEditor = new DentiaAtencionSession(state.pacienteId, serverDraft?.version ?? 0, leerAtencion());
        } else {
            if (form.elements.citaId) form.elements.citaId.value = state.citaId || '';
        }

        // Profesional odontólogo tratante asignado (vinculado a la cita o en sesión)
        const profIdInput = $('#atencion-profesional-id');
        const profDisplay = $('#atencion-profesional-display');
        const profSub = $('#atencion-profesional-sub');
        if (state.citaId && state.expediente?.citas) {
            const cita = state.expediente.citas.find(c => String(c.id) === String(state.citaId));
            if (cita) {
                if (cita.profesional && profDisplay) profDisplay.textContent = cita.profesional;
                if (cita.profesionalId && profIdInput) profIdInput.value = cita.profesionalId;
                if (profSub) profSub.innerHTML = `<i class="bi bi-calendar-check text-primary me-1"></i>Asignado en Cita #${state.citaId}`;
            }
        } else {
            if (profSub) profSub.innerHTML = `<i class="bi bi-shield-check text-success me-1"></i>Odontólogo tratante (Consulta Directa)`;
        }

        // Título dinámico según si hay cita activa o atención directa
        const titleEl = $('#modal-atencion-titulo');
        if (titleEl) {
            titleEl.textContent = state.citaId 
                ? `Finalizar Cita #${state.citaId} · Registrar Atención`
                : 'Registrar Atención Clínica (Consulta Directa)';
        }
        const noticeEl = $('#atencion-modal-cita-notice');
        const noticeDirectoEl = $('#atencion-modal-directo-notice');
        if (state.citaId) {
            if (noticeEl) {
                noticeEl.hidden = false;
                const txt = $('#atencion-modal-cita-titulo');
                if (txt) txt.textContent = `Cita en curso #${state.citaId}`;
            }
            if (noticeDirectoEl) noticeDirectoEl.hidden = true;
        } else {
            if (noticeEl) noticeEl.hidden = true;
            if (noticeDirectoEl) noticeDirectoEl.hidden = false;
        }

        // Auto-sugerir piezas tratadas desde el odontograma si el campo está vacío
        const inputPieza = form.elements['piezasTratadas'];
        if (inputPieza && !inputPieza.value.trim()) {
            const findings = obtenerHallazgosOdontogramaActual();
            if (findings.piezasStr) {
                inputPieza.value = findings.piezasStr;
                const inputProc = form.elements['planProcedimiento'];
                if (inputProc && !inputProc.value.trim() && findings.procedimientosStr) {
                    inputProc.value = findings.procedimientosStr;
                }
            }
        }

        sincronizarChipsDesdeInput();
        actualizarEstadoAtencion();
        bootstrap.Modal.getOrCreateInstance($('#modal-crear-atencion')).show();
    }

    function leerAtencion() {
        const form = $('#form-crear-atencion');
        const values = Object.fromEntries(Array.from(form.elements).filter(el => el.name).map(el => [el.name, el.value]));
        return { ...values, importeTratamiento: values.importeTratamiento ? Number(values.importeTratamiento) : null, pacienteId: atencionEditor?.pacienteId ?? state.pacienteId,
            citaId: values.citaId ? Number(values.citaId) : null,
            profesionalId: values.profesionalId ? Number(values.profesionalId) : null,
            itemPlanIdCompletar: values.itemPlanIdCompletar ? Number(values.itemPlanIdCompletar) : null };
    }

    function actualizarEstadoAtencion() {
        const busy = !!atencionEditor?.busy, pending = !!atencionEditor?.pending;
        const conflicted = !!atencionEditor?.conflicted;
        $$('#form-crear-atencion input, #form-crear-atencion select, #form-crear-atencion textarea')
            .forEach(el => { el.disabled = busy || pending || conflicted; });
        const btnDraft = $('#btn-guardar-borrador');
        if (btnDraft) btnDraft.disabled = busy || pending || conflicted;
        const reload = $('#btn-recargar-atencion-conflicto');
        if (reload) reload.hidden = !conflicted;
        const submit = $('#btn-submit-atencion') || $('#form-crear-atencion button[type="submit"]');
        if (submit) {
            submit.disabled = busy || conflicted;
            submit.innerHTML = busy 
                ? '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Finalizando atención...'
                : pending 
                ? '<i class="bi bi-arrow-repeat me-1"></i> Reintentar finalización' 
                : `<i class="bi bi-check2-circle me-1"></i> ${state.citaId ? 'Finalizar Cita y Guardar Atención' : 'Finalizar atención'}`;
        }
        $$('#modal-crear-atencion [data-bs-dismiss="modal"]').forEach(el => { el.disabled = busy; });
        const estado = $('#estado-guardado-atencion');
        if (estado) estado.textContent = busy ? 'Guardando; no cierres esta ventana.' : conflicted
            ? 'Otro profesional actualizó esta atención. Recarga el borrador vigente para continuar sin sobrescribirlo.' : pending
            ? 'No se confirmó el cierre. Reintenta: no se creará una atención duplicada.'
            : 'Tus cambios se conservan automáticamente en este paciente.';
    }

    async function recargarAtencionTrasConflicto() {
        const editor = atencionEditor;
        if (!editor?.conflicted) return;
        const reload = $('#btn-recargar-atencion-conflicto');
        if (reload) reload.disabled = true;
        try {
            const data = await api(`/interno/clinica/api/expediente/${editor.pacienteId}`);
            if (atencionEditor !== editor) return;
            state.expediente = data;
            atencionEditor = null;
            $('#form-crear-atencion').reset();
            abrirModalAtencion();
            renderModuloActual();
            mostrarAviso('Se cargó el borrador más reciente. Revisa los cambios antes de continuar.');
        } catch (error) {
            mostrarAviso(error.message, 'error');
        } finally {
            if (reload) reload.disabled = false;
            actualizarEstadoAtencion();
        }
    }

    async function guardarAtencion(finalizar) {
        if (!atencionEditor || atencionEditor.busy) return;
        const editor = atencionEditor;
        const form = $('#form-crear-atencion');
        const modalAtencion = $('#modal-crear-atencion');
        const btnSubmit = $('#btn-submit-atencion') || $('#form-crear-atencion button[type="submit"]');

        // Validar si la cita ya cuenta con atención previa
        if (finalizar && state.citaId) {
            const yaAtendida = state.expediente?.atenciones?.some(a => String(a.citaId) === String(state.citaId));
            if (yaAtendida) {
                state.citaId = null;
                if (form?.elements.citaId) form.elements.citaId.value = '';
            }
        }

        // Validaciones preventivas inmediatas dentro del modal
        const inputPiezas = form?.elements['piezasTratadas'];
        if (inputPiezas && inputPiezas.value.trim().length > 100) {
            inputPiezas.classList.add('is-invalid');
            mostrarAviso(`El campo "Piezas tratadas" supera el límite permitido de 100 caracteres (${inputPiezas.value.trim().length} caracteres). Por favor, indique solo los números de piezas (ejemplo: 16, 26, 48).`, 'error', modalAtencion);
            inputPiezas.focus();
            return;
        }

        const inputProc = form?.elements['planProcedimiento'];
        if (finalizar && (!inputProc || !inputProc.value.trim())) {
            inputProc?.classList.add('is-invalid');
            mostrarAviso('El procedimiento dental realizado es obligatorio para registrar la atención.', 'error', modalAtencion);
            inputProc?.focus();
            return;
        }

        if (finalizar && btnSubmit) {
            btnSubmit.disabled = true;
            btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Finalizando atención...';
        }

        try {
            const request = editor.begin(leerAtencion(), finalizar);
            actualizarEstadoAtencion();
            const response = await api('/interno/clinica/api/atenciones' + (finalizar ? '' : '/borrador'), { method: 'POST', body: request });
            if (finalizar) {
                const teniaReceta = Boolean(request.contenido?.indicacionesReceta?.trim());
                editor.busy = false;
                delete workingDraftAtencion[editor.pacienteId];
                state.citaId = null;
                delete root.dataset.citaId;
                const url = new URL(window.location);
                url.searchParams.delete('citaId');
                window.history.replaceState({}, '', url);

                state.expediente.borradorAtencion = { version: editor.version + 1, contenido: null };
                atencionEditor = null;
                form?.reset();
                if (form?.elements['itemPlanIdCompletar']) form.elements['itemPlanIdCompletar'].value = '';
                const planNotice = $('#atencion-modal-plan-notice');
                if (planNotice) planNotice.hidden = true;
                $$('.tooth-picker-chip').forEach(c => c.classList.remove('active'));
                const chkAll = $('#chk-todas-piezas');
                if (chkAll) chkAll.checked = false;
                const labelTexto = $('#atencion-piezas-seleccionadas-texto');
                if (labelTexto) labelTexto.innerHTML = '<span class="text-muted">Ninguna (General)</span>';
                actualizarEstadoAtencion();
                bootstrap.Modal.getInstance($('#modal-crear-atencion'))?.hide();
                const btnRecup = $('#btn-recuperar-atencion');
                if (btnRecup) btnRecup.hidden = true;
                mostrarAviso('Atención clínica registrada exitosamente.');

                // Limpiar marcas pendientes locales en memoria para las piezas tratadas
                const strP = String(request.contenido?.piezasTratadas || '').trim();
                const piezasNums = (strP.match(/\b(1[1-8]|2[1-8]|3[1-8]|4[1-8]|5[1-5]|6[1-5]|7[1-5]|8[1-5])\b/g) || []).map(Number);
                if (piezasNums.length > 0) {
                    state.selectedPiece = piezasNums[0];
                    for (const [key] of state.odontoPending) {
                        if (piezasNums.some(p => key.startsWith(`${p}:`))) {
                            state.odontoPending.delete(key);
                        }
                    }
                }

                await cargarPaciente(editor.pacienteId);
                if (Number(request.contenido?.importeTratamiento) > 0 && !request.contenido?.itemPlanIdCompletar) {
                    mostrarAviso('Atención guardada y cargo pendiente generado. Registra el pago en Cuentas y Deudas.');
                }
                if (state.selectedPiece) {
                    seleccionarDienteEnOdontograma(state.selectedPiece);
                }

                // Si el doctor redactó receta, abrirla de inmediato para imprimir o enviar
                if (teniaReceta && state.expediente?.atenciones?.[0]) {
                    abrirModalReceta(state.expediente.atenciones[0].id);
                    mostrarAviso('Receta médica generada para esta atención. Lista para imprimir o enviar por WhatsApp.');
                }
            } else {
                editor.savedDraft(request.contenido, response.version);
                state.expediente.borradorAtencion = { contenido: request.contenido, version: response.version };
                const btnRecup = $('#btn-recuperar-atencion');
                if (btnRecup) btnRecup.hidden = false;
                mostrarAviso('Borrador guardado exitosamente. Puede continuar la atención más tarde.', 'success', modalAtencion);
            }
        } catch (error) {
            editor.failed(error.status);
            mostrarAviso(error.message, 'error', modalAtencion);
        } finally { actualizarEstadoAtencion(); }
    }

    // ============================================================
    // INICIALIZACIÓN DE EVENTOS
    // ============================================================
    function initEvents() {
        // Cierre de notificación toast flotante
        $('#clinica-toast-close')?.addEventListener('click', () => {
            const toast = $('#clinica-toast');
            if (toast) toast.hidden = true;
        });

        // Limpiar alertas locales y marcas de error al cerrar cualquier modal
        document.addEventListener('hidden.bs.modal', e => {
            const modal = e.target;
            if (modal) {
                modal.querySelectorAll('.modal-local-alert').forEach(el => el.remove());
                modal.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
                modal.querySelectorAll('.invalid-feedback').forEach(el => el.style.display = 'none');
            }
        });

        // Selector de Pestañas
        $$('.clinica-tab-link').forEach(link => {
            link.addEventListener('click', e => {
                e.preventDefault();
                state.activeModulo = link.dataset.modulo;
                renderModuloActual();
            });
        });

        // Buscador de Pacientes con Autocomplete
        const searchInput = $('#buscar-paciente');
        const searchResults = $('#resultados-pacientes');
        let searchTimeout;

        searchInput?.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            const q = searchInput.value.trim();
            if (q.length < 2) {
                searchResults.hidden = true;
                return;
            }
            searchTimeout = setTimeout(async () => {
                try {
                    const list = await api(`/interno/clinica/api/pacientes/buscar?q=${encodeURIComponent(q)}`);
                    if (list.length === 0) {
                        searchResults.innerHTML = '<li class="p-3 text-secondary text-center small">No se encontraron pacientes.</li>';
                    } else {
                        searchResults.innerHTML = list.map(p => `
                            <li class="clinica-search-item" data-id="${p.id}">
                                <div>
                                    <strong class="d-block">${escape(p.nombre)}</strong>
                                    <small class="text-secondary">${p.dni ? 'DNI ' + escape(p.dni) : 'Sin DNI'} · ${escape(p.celular || 'Sin celular')}</small>
                                </div>
                                ${p.alergias ? '<span class="badge text-bg-danger">Alergia</span>' : ''}
                            </li>
                        `).join('');
                        $$('.clinica-search-item').forEach(item => {
                            item.addEventListener('click', () => {
                                searchResults.hidden = true;
                                searchInput.value = '';
                                cargarPaciente(Number(item.dataset.id));
                            });
                        });
                    }
                    searchResults.hidden = false;
                } catch (e) {
                    console.error(e);
                }
            }, 250);
        });

        document.addEventListener('click', e => {
            if (!searchInput?.contains(e.target) && !searchResults?.contains(e.target)) {
                if (searchResults) searchResults.hidden = true;
            }
        });

        // Herramientas Odontograma
        $$('.odonto-tool-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                $$('.odonto-tool-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                state.odontoTool = btn.dataset.tool;
            });
        });
        $('#btn-guardar-odontograma-2d')?.addEventListener('click', guardarOdontograma2D);

        // Guardar Historia Clínica
        $('#form-historia')?.addEventListener('submit', async e => {
            e.preventDefault();
            if (!state.pacienteId) return;
            const form = e.target;
            try {
                await api('/interno/clinica/api/historia', {
                    method: 'POST',
                    body: {
                        pacienteId: state.pacienteId,
                        alergias: form.elements['alergias'].value,
                        enfermedadesSistemicas: form.elements['enfermedadesSistemicas'].value,
                        medicacionActual: form.elements['medicacionActual'].value,
                        hemorragias: form.elements['hemorragias'].checked,
                        embarazo: form.elements['embarazo'].checked,
                        antecedentesFamiliares: form.elements['antecedentesFamiliares'].value,
                        habitos: form.elements['habitos'].value,
                        motivoPrimeraConsulta: form.elements['motivoPrimeraConsulta'].value,
                        grupoSanguineo: form.elements['grupoSanguineo'].value,
                        presionArterial: form.elements['presionArterial'].value,
                        observacionesMedicas: form.elements['observacionesMedicas'].value
                    }
                });
                await cargarPaciente(state.pacienteId);
                mostrarAviso('Historia clínica guardada exitosamente.');
            } catch (err) {
                mostrarAviso(err.message, 'error');
            }
        });

        // Nuevo Diagnóstico Modal
        $('#btn-nuevo-diagnostico')?.addEventListener('click', () => {
            new bootstrap.Modal($('#modal-crear-diagnostico')).show();
        });

        $('#form-crear-diagnostico')?.addEventListener('submit', async e => {
            e.preventDefault();
            const form = e.target;
            try {
                await api('/interno/clinica/api/diagnosticos', {
                    method: 'POST',
                    body: {
                        pacienteId: state.pacienteId,
                        codigoCie10: form.elements['codigoCie10'].value,
                        descripcion: form.elements['descripcion'].value,
                        numeroPieza: form.elements['numeroPieza'].value ? Number(form.elements['numeroPieza'].value) : null,
                        gravedad: form.elements['gravedad'].value,
                        estado: 'ACTIVO',
                        observaciones: form.elements['observaciones'].value
                    }
                });
                bootstrap.Modal.getInstance($('#modal-crear-diagnostico')).hide();
                form.reset();
                await cargarPaciente(state.pacienteId);
                mostrarAviso('Diagnóstico registrado exitosamente.');
            } catch (err) {
                mostrarAviso(err.message, 'error');
            }
        });

        // Guardar Plan de Tratamiento
        $('#btn-agregar-fila-plan')?.addEventListener('click', () => agregarFilaItemPlan());

        $('#select-catalogo-rapido')?.addEventListener('change', e => {
            const id = e.target.value;
            if (!id) return;
            const opt = e.target.selectedOptions[0];
            const nombre = opt.dataset.nombre;
            const precio = opt.dataset.precio;
            const primera = $('#plan-items-lista tr:first-child');
            if (primera && !primera.querySelector('.item-desc')?.value) {
                primera.remove();
            }
            agregarFilaItemPlan({ nombre, precio, tratId: id });
            e.target.value = '';
        });

        // Limpiar o nuevo plan en el constructor (carrito)
        $('#btn-nuevo-plan')?.addEventListener('click', () => {
            state.carritoPlan = [];
            const inputTitulo = $('#builder-plan-titulo');
            if (inputTitulo) inputTitulo.value = 'Plan de Tratamiento Integral';
            const inputNotas = $('#builder-plan-notas');
            if (inputNotas) inputNotas.value = '';
            renderCarritoPlan();
            mostrarAviso('Carrito reiniciado para armar un nuevo plan.');
        });

        // Constructor de plan: Búsqueda y Filtros de Categorías
        $$('.builder-pill-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                $$('.builder-pill-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                renderBuilderCatalogo(btn.dataset.cat, $('#builder-buscar-tratamiento')?.value || '');
            });
        });

        $('#builder-buscar-tratamiento')?.addEventListener('input', () => {
            clearTimeout(window._builderSearchTimeout);
            window._builderSearchTimeout = setTimeout(() => {
                const activeCat = $('.builder-pill-btn.active')?.dataset.cat || '';
                renderBuilderCatalogo(activeCat, $('#builder-buscar-tratamiento')?.value || '');
            }, 250);
        });

        // Botones del Carrito
        $('#btn-builder-guardar-borrador')?.addEventListener('click', () => {
            guardarPlanDesdeCarrito(false);
        });

        $('#btn-builder-aprobar-deuda')?.addEventListener('click', () => {
            guardarPlanDesdeCarrito(true);
        });

        // Toggle de Descuentos
        $('#builder-toggle-descuentos')?.addEventListener('change', e => {
            state.descuentosActivos = e.target.checked;
            const modeSelector = $('#discount-mode-selector');
            const globalContainer = $('#global-discount-container');
            if (modeSelector) modeSelector.hidden = !state.descuentosActivos;
            if (globalContainer) globalContainer.hidden = !state.descuentosActivos || state.modoDescuento !== 'global';
            renderCarritoPlan();
        });

        // Radios de Modalidad de Descuento (item vs global)
        $$('input[name="discount-mode"]').forEach(radio => {
            radio.addEventListener('change', e => {
                state.modoDescuento = e.target.value;
                const globalContainer = $('#global-discount-container');
                if (globalContainer) globalContainer.hidden = !state.descuentosActivos || state.modoDescuento !== 'global';
                renderCarritoPlan();
            });
        });

        // Tipo de Descuento Global (% vs S/)
        $$('.btn-global-type').forEach(btn => {
            btn.addEventListener('click', () => {
                $$('.btn-global-type').forEach(b => b.classList.remove('active', 'btn-teal', 'text-white'));
                btn.classList.add('active', 'btn-teal', 'text-white');
                state.descuentoGlobalTipo = btn.dataset.type;
                renderCarritoPlan();
            });
        });

        // Valor de Descuento Global
        $('#input-global-discount-val')?.addEventListener('input', e => {
            state.descuentoGlobalVal = parseFloat(e.target.value) || 0;
            renderCarritoPlan();
        });

        // Pestañas de Vista Móvil en el Constructor (Catálogo vs Carrito)
        $('#mob-tab-btn-cat')?.addEventListener('click', () => {
            switchMobileBuilderView('catalogo');
        });
        $('#mob-tab-btn-cart')?.addEventListener('click', () => {
            switchMobileBuilderView('cart');
        });
        $('#btn-mobile-ver-carrito')?.addEventListener('click', () => {
            switchMobileBuilderView('cart');
        });

        // Selector de métodos con pills y submit de cobro rápido
        $$('.cobro-metodo-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                $$('.cobro-metodo-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                const metodo = btn.dataset.metodo;
                const inputMetodo = $('#cobro-metodo-input');
                if (inputMetodo) inputMetodo.value = metodo;
                const refInput = $('#cobro-referencia-input');
                if (refInput) {
                    if (metodo === 'EFECTIVO') {
                        refInput.placeholder = 'Opcional para efectivo (ej. Recepción)';
                        refInput.required = false;
                    } else {
                        refInput.placeholder = `N° Operación / Celular ${metodo} (obligatorio)`;
                        refInput.required = true;
                    }
                }
            });
        });

        $('#form-cobro-clinica')?.addEventListener('submit', async e => {
            e.preventDefault();
            const btnSubmit = $('#btn-confirmar-cobro');
            const errEl = $('#cobro-error-msg');
            if (errEl) errEl.style.display = 'none';

            const presId = Number($('#cobro-presupuesto-id')?.value);
            const monto = parseFloat($('#cobro-monto-input')?.value);
            const metodo = $('#cobro-metodo-input')?.value || 'YAPE';
            const fecha = $('#cobro-fecha-input')?.value;
            const referencia = $('#cobro-referencia-input')?.value?.trim() || (metodo === 'EFECTIVO' ? 'Pago en caja' : '');

            if (!presId) {
                if (errEl) { errEl.textContent = 'No se identificó el presupuesto a cobrar.'; errEl.style.display = 'block'; }
                return;
            }
            if (!monto || monto <= 0) {
                if (errEl) { errEl.textContent = 'Ingresa un monto válido mayor a 0.'; errEl.style.display = 'block'; }
                return;
            }
            if (metodo !== 'EFECTIVO' && !referencia) {
                if (errEl) { errEl.textContent = `Ingresa el número de operación o referencia para ${metodo}.`; errEl.style.display = 'block'; }
                return;
            }

            const token = $('#modal-cobro-clinica').dataset.token || ($('#modal-cobro-clinica').dataset.token = crypto.randomUUID());

            if (btnSubmit) {
                btnSubmit.disabled = true;
                btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Registrando…';
            }

            try {
                const res = await fetch('/interno/gestion/pagos', {
                    method: 'POST',
                    credentials: 'same-origin',
                    cache: 'no-store',
                    headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...csrfHeader() },
                    body: JSON.stringify({
                        presupuestoId: presId,
                        monto: monto,
                        fecha: fecha,
                        metodo: metodo,
                        referencia: referencia,
                        token: token
                    })
                });

                const data = res.headers.get('content-type')?.includes('json') ? await res.json() : {};
                if (!res.ok) throw new Error(data.error || data.mensaje || 'Error al registrar el cobro.');

                bootstrap.Modal.getInstance($('#modal-cobro-clinica'))?.hide();
                await cargarPaciente(state.pacienteId);
                mostrarAviso(`Cobro de ${money(monto)} registrado. Constancia #${data.id} disponible para imprimir.`);
                const aviso = $('#clinica-toast-message');
                if (aviso && data.id) {
                    const enlace = document.createElement('a');
                    enlace.href = `/interno/gestion/pagos?registro=${encodeURIComponent(data.id)}`;
                    enlace.className = 'btn btn-sm btn-outline-success ms-2';
                    enlace.textContent = 'Ver constancia';
                    aviso.appendChild(enlace);
                    clearTimeout($('#clinica-toast')._timeout);
                }
            } catch (err) {
                if (errEl) {
                    errEl.textContent = err.message;
                    errEl.style.display = 'block';
                } else {
                    mostrarAviso(err.message, 'error');
                }
            } finally {
                if (btnSubmit) {
                    btnSubmit.disabled = false;
                    btnSubmit.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Confirmar y Registrar Cobro';
                }
            }
        });

        // Formulario modal clásico de plan (fallback)
        $('#form-crear-plan')?.addEventListener('submit', async e => {
            e.preventDefault();
            const form = e.target;
            const items = [];
            $$('#plan-items-lista tr').forEach(tr => {
                const desc = tr.querySelector('.item-desc')?.value.trim();
                const precio = parseFloat(tr.querySelector('.item-precio')?.value) || 0;
                const fase = parseInt(tr.querySelector('.item-fase')?.value) || 1;
                const pza = tr.querySelector('.item-pieza')?.value ? parseInt(tr.querySelector('.item-pieza').value) : null;
                const diagId = tr.querySelector('.item-diag-id')?.value ? parseInt(tr.querySelector('.item-diag-id').value) : null;
                const tratId = tr.querySelector('.item-trat-id')?.value ? parseInt(tr.querySelector('.item-trat-id').value) : null;
                if (desc) {
                    items.push({
                        descripcion: desc,
                        precio: precio,
                        ordenFase: fase,
                        numeroPieza: pza,
                        diagnosticoId: diagId,
                        tratamientoCatalogoId: tratId,
                        estado: 'PENDIENTE'
                    });
                }
            });

            if (items.length === 0) {
                mostrarAviso('Agrega al menos un procedimiento al plan.', 'error');
                return;
            }

            try {
                await api('/interno/clinica/api/planes', {
                    method: 'POST',
                    body: {
                        pacienteId: state.pacienteId,
                        titulo: form.elements['titulo']?.value || 'Plan de Tratamiento Integral',
                        estado: form.elements['estado']?.value || 'PROPUESTO',
                        notas: form.elements['notas']?.value || '',
                        items: items
                    }
                });
                bootstrap.Modal.getInstance($('#modal-crear-plan'))?.hide();
                await cargarPaciente(state.pacienteId);
                mostrarAviso('Plan de tratamiento guardado con éxito.');
            } catch (err) {
                mostrarAviso(err.message, 'error');
            }
        });

        // Registrar Nuevo Procedimiento en Catálogo
        $('#btn-nuevo-procedimiento')?.addEventListener('click', () => {
            const modalEl = $('#modal-nuevo-tratamiento');
            if (modalEl) {
                $('#form-nuevo-tratamiento')?.reset();
                new bootstrap.Modal(modalEl).show();
            }
        });

        $('#form-nuevo-tratamiento')?.addEventListener('submit', async e => {
            e.preventDefault();
            const form = e.target;
            try {
                await api('/interno/clinica/api/tratamientos/catalogo', {
                    method: 'POST',
                    body: {
                        codigo: form.elements['codigo'].value.trim().toUpperCase(),
                        nombre: form.elements['nombre'].value.trim(),
                        categoria: form.elements['categoria'].value,
                        subcategoria: form.elements['subcategoria'].value.trim(),
                        descripcion: form.elements['descripcion'].value.trim(),
                        precioSugerido: parseFloat(form.elements['precio'].value),
                        duracionEstimadaMin: parseInt(form.elements['duracion'].value)
                    }
                });
                bootstrap.Modal.getInstance($('#modal-nuevo-tratamiento'))?.hide();
                mostrarAviso('Procedimiento agregado al catálogo exitosamente.');
                renderTratamientos();
            } catch (err) {
                mostrarAviso(err.message, 'error');
            }
        });

        // Guardar Atención / Evolución (SOEP)
        $('#btn-nueva-atencion')?.addEventListener('click', abrirModalAtencion);
        $('#btn-hero-finalizar-cita')?.addEventListener('click', abrirModalAtencion);
        $('#btn-hero-atencion-directa')?.addEventListener('click', () => {
            state.citaId = null;
            delete root.dataset.citaId;
            abrirModalAtencion();
        });
        $('#btn-banner-atender-cita')?.addEventListener('click', abrirModalAtencion);
        $('#btn-cargar-odonto-atencion')?.addEventListener('click', cargarDesdeOdontogramaEnFormulario);
        $('#btn-odonto-a-atencion')?.addEventListener('click', async () => {
            if (!state.pacienteId) {
                mostrarAviso('Selecciona un paciente antes de registrar su atención.', 'warning');
                return;
            }
            const prevFindings = obtenerHallazgosOdontogramaActual();
            if (state.odontoPending && state.odontoPending.size > 0) {
                await guardarMarcasOdontograma2D();
            }
            const findings = prevFindings.piezasStr ? prevFindings : obtenerHallazgosOdontogramaActual();
            abrirModalAtencion();
            if (findings.piezasStr) {
                const form = $('#form-crear-atencion');
                if (form) {
                    if (form.elements['piezasTratadas']) form.elements['piezasTratadas'].value = findings.piezasStr;
                    if (form.elements['planProcedimiento'] && !form.elements['planProcedimiento'].value.trim() && findings.procedimientosStr) {
                        form.elements['planProcedimiento'].value = findings.procedimientosStr;
                    }
                }
                sincronizarChipsDesdeInput();
            }
        });
        $('#btn-emitir-receta-directa')?.addEventListener('click', () => abrirModalReceta());

        // Botones de prescripción rápida (+ Amoxicilina, + Ibuprofeno, etc.)
        $$('.btn-quick-rx').forEach(btn => {
            btn.addEventListener('click', () => {
                const rx = btn.dataset.rx;
                const txt = $('#input-atencion-receta');
                if (txt && rx) {
                    txt.value = txt.value.trim() ? `${txt.value.trim()}\n• ${rx}` : `• ${rx}`;
                    txt.focus();
                }
            });
        });

        // Impresión y copiado de receta
        $('#btn-imprimir-receta')?.addEventListener('click', () => {
            window.print();
        });

        $('#btn-copiar-receta-wa')?.addEventListener('click', () => {
            const rx = state.recetaActiva;
            if (!rx) {
                mostrarAviso('No hay receta médica seleccionada.', 'warning');
                return;
            }
            const waText = `🦷 *RECETA MÉDICA ODONTOLÓGICA - DENTIA*\n` +
                `👤 *Paciente:* ${rx.nombrePac} (DNI: ${rx.dniPac})\n` +
                `📅 *Fecha:* ${rx.fecha}\n` +
                `👨‍⚕️ *Odontólogo:* ${rx.doctor}\n` +
                `🔬 *Procedimiento:* ${rx.proc}\n\n` +
                `💊 *Rp. Prescripción e Indicaciones:*\n${rx.recetaTexto}\n\n` +
                `⚠️ *Recomendación:* Cumplir con los horarios y dosis indicados. Si presenta molestia persistente comuníquese con DentIA.`;
            navigator.clipboard.writeText(waText).then(() => {
                mostrarAviso('¡Receta copiada en formato WhatsApp! Lista para enviar al paciente.');
            }).catch(() => {
                mostrarAviso('Copia el texto manualmente de la pantalla.', 'warning');
            });
        });

        // Registrar atención directa desde la ficha individual de la pieza
        $('#btn-detalle-pieza-atender')?.addEventListener('click', () => {
            const pieza = state.detallePieza;
            if (pieza) {
                bootstrap.Modal.getInstance($('#modal-detalle-pieza'))?.hide();
                abrirModalAtencionConPieza(pieza);
            }
        });

        $('#form-crear-atencion')?.addEventListener('input', guardarBorradorEnMemoria);
        $('#form-crear-atencion')?.addEventListener('change', guardarBorradorEnMemoria);
        $('#form-crear-atencion')?.addEventListener('submit', e => {
            e.preventDefault();
            guardarAtencion(true);
        });
        $('#modal-crear-atencion')?.addEventListener('hide.bs.modal', e => {
            if (atencionEditor?.busy) e.preventDefault();
        });
        window.addEventListener('beforeunload', e => {
            if (atencionEditor?.isDirty(leerAtencion())) { e.preventDefault(); e.returnValue = ''; }
        });

        // Filtros catálogo tratamientos
        $('#filtro-tratamiento-cat')?.addEventListener('change', renderTratamientos);
        $('#filtro-tratamiento-q')?.addEventListener('input', () => {
            clearTimeout(window._tTimeout);
            window._tTimeout = setTimeout(renderTratamientos, 300);
        });

        // Navegación de pestañas clínicas superiores
        $$('.tab-pill-btn[data-modulo]').forEach(item => {
            item.addEventListener('click', e => {
                e.preventDefault();
                state.activeModulo = item.dataset.modulo;
                renderModuloActual();
            });
        });

        $('#clinica-toast-close')?.addEventListener('click', () => {
            $('#clinica-toast').hidden = true;
            clearTimeout($('#clinica-toast')._timeout);
        });

        // Las vistas 2D y 3D tienen espacios de trabajo independientes.
        if (root.getAttribute('data-odonto-2d') !== 'true' && root.getAttribute('data-odonto-3d') === 'true') {
            $('#studio-3d-workspace').hidden = false;
            $('#studio-3d-container').hidden = false;
        }
        $('#btn-studio-3d')?.addEventListener('click', () => {
            $('#btn-studio-3d')?.classList.add('active');
            $('#btn-studio-2d')?.classList.remove('active');
            $('#btn-studio-3d')?.setAttribute('aria-pressed', 'true');
            $('#btn-studio-2d')?.setAttribute('aria-pressed', 'false');
            $('#studio-3d-workspace').hidden = false;
            $('#studio-3d-container').hidden = false;
            if ($('#studio-2d-container')) $('#studio-2d-container').hidden = true;
            setTimeout(initStudio3D, 60);
        });

        $('#btn-studio-2d')?.addEventListener('click', () => {
            $('#btn-studio-2d')?.classList.add('active');
            $('#btn-studio-3d')?.classList.remove('active');
            $('#btn-studio-2d')?.setAttribute('aria-pressed', 'true');
            $('#btn-studio-3d')?.setAttribute('aria-pressed', 'false');
            if ($('#studio-3d-workspace')) $('#studio-3d-workspace').hidden = true;
            if ($('#studio-3d-container')) $('#studio-3d-container').hidden = true;
            $('#studio-2d-container').hidden = false;
            renderOdontograma();
        });

        $('#btn-odonto-sidebar-close')?.addEventListener('click', () => {
            $('#odonto-sidebar-panel')?.classList.remove('mobile-open');
            const backdrop = $('#odonto-sidebar-backdrop');
            if (backdrop) backdrop.hidden = true;
        });
        $('#odonto-sidebar-backdrop')?.addEventListener('click', () => {
            $('#odonto-sidebar-panel')?.classList.remove('mobile-open');
            const backdrop = $('#odonto-sidebar-backdrop');
            if (backdrop) backdrop.hidden = true;
        });

        // Tabs rápidos de arcada para móviles
        $$('.odonto-arcada-tab').forEach(btn => {
            btn.addEventListener('click', () => {
                const arcada = btn.dataset.arcada;
                $$('.odonto-arcada-tab').forEach(b => b.classList.toggle('active', b === btn));
                const supRows = $$('#odonto-row-adulto-sup, #odonto-row-nino-sup');
                const infRows = $$('#odonto-row-nino-inf, #odonto-row-adulto-inf');
                const divider = $('.odonto-sheet-divider');
                if (arcada === 'SUP') {
                    supRows.forEach(r => r.hidden = false);
                    infRows.forEach(r => r.hidden = true);
                    if (divider) divider.hidden = true;
                } else if (arcada === 'INF') {
                    supRows.forEach(r => r.hidden = true);
                    infRows.forEach(r => r.hidden = false);
                    if (divider) divider.hidden = true;
                } else {
                    supRows.forEach(r => r.hidden = false);
                    infRows.forEach(r => r.hidden = false);
                    if (divider) divider.hidden = false;
                }
            });
        });

        // Sidebar del Odontograma Unificado (Cotizador y Selección)
        $('#sidebar-select-tratamiento')?.addEventListener('change', e => {
            const sel = e.target;
            const opt = sel.options[sel.selectedIndex];
            const precio = opt.dataset.precio;
            const custom = $('#sidebar-custom-tratamiento');
            const inputPrecio = $('#sidebar-input-precio');
            if (sel.value === 'OTRO') {
                if (custom) { custom.hidden = false; custom.focus(); }
                if (inputPrecio) inputPrecio.value = '0.00';
            } else {
                if (custom) custom.hidden = true;
                if (inputPrecio && precio !== undefined) inputPrecio.value = precio;
            }
        });
        $('#btn-sidebar-cotizar-plan')?.addEventListener('click', cotizarTratamientoEnPlanDesdeSidebar);


        // Botones del D-pad de navegación 3D
        $('#dpad-up')?.addEventListener('click', () => moverCamara3D(0, -3, 0));
        $('#dpad-down')?.addEventListener('click', () => moverCamara3D(0, 3, 0));
        $('#dpad-left')?.addEventListener('click', () => rotarCamara3D(0.25));
        $('#dpad-right')?.addEventListener('click', () => rotarCamara3D(-0.25));
        $('#dpad-center')?.addEventListener('click', resetCamara3D);
        $('#dpad-zoom-in')?.addEventListener('click', () => zoomCamara3D(-2.5));
        $('#dpad-zoom-out')?.addEventListener('click', () => zoomCamara3D(2.5));

        // Herramientas de visualización vertical 3D
        $('#tool-3d-teeth')?.addEventListener('click', () => {
            toggleHerramientaActiva('#tool-3d-teeth');
            resetCamara3D();
        });

        $('#tool-3d-gums')?.addEventListener('click', () => {
            const btn = $('#tool-3d-gums');
            btn.classList.toggle('active');
            studio3D.gumsVisible = !studio3D.gumsVisible;
            viewer?.setOptions({ gums: studio3D.gumsVisible });
            mostrarAviso(studio3D.gumsVisible ? 'Encías 3D visibles' : 'Encías 3D ocultas');
        });

        $('#tool-3d-roots')?.addEventListener('click', () => {
            const btn = $('#tool-3d-roots');
            btn.classList.toggle('active');
            studio3D.rootsVisible = !studio3D.rootsVisible;
            actualizarColoresStudio3D();
            mostrarAviso(studio3D.rootsVisible ? 'Raíces anatómicas visibles' : 'Raíces anatómicas ocultas (solo coronas)');
        });

        $('#tool-3d-wireframe')?.addEventListener('click', () => {
            const btn = $('#tool-3d-wireframe');
            btn.classList.toggle('active');
            studio3D.isWireframe = !studio3D.isWireframe;
            studio3D.teethMeshes.forEach(m => {
                m.traverse(c => { if (c.isMesh && c.material) c.material.wireframe = studio3D.isWireframe; });
            });
            mostrarAviso(studio3D.isWireframe ? 'Modo Rayos X / Alambre 3D' : 'Modo Anatómico 3D');
        });

        $('#tool-3d-occlusal')?.addEventListener('click', () => {
            toggleHerramientaActiva('#tool-3d-occlusal');
            vistaOclusalCenital();
        });

        // Controles de inclinación anatómica de la pieza seleccionada
        $('#btn-inclinar-mesial')?.addEventListener('click', () => {
            ajustarInclinacionDienteSeleccionado(-8);
        });

        $('#btn-inclinar-distal')?.addEventListener('click', () => {
            ajustarInclinacionDienteSeleccionado(8);
        });

        $('#btn-reset-inclinacion')?.addEventListener('click', () => {
            resetearInclinacionDienteSeleccionado();
        });

        $('#btn-marcar-ausente-3d')?.addEventListener('click', () => {
            cambiarEstadoRapidoDienteSeleccionado('AUSENTE');
        });

        $('#btn-marcar-retenido-3d')?.addEventListener('click', () => {
            cambiarEstadoRapidoDienteSeleccionado('RETENIDO');
        });

        $('#btn-marcar-erupcionado-3d')?.addEventListener('click', () => {
            cambiarEstadoRapidoDienteSeleccionado('SANO');
        });

        // Asistente de Radiografía Panorámica
        $('#btn-abrir-panoramica')?.addEventListener('click', abrirModalPanoramica);
        $('#btn-cargar-muestra-panoramica')?.addEventListener('click', cargarMuestraPanoramicaDemo);
        $('#btn-analizar-ia-panoramica')?.addEventListener('click', ejecutarEscaneoIAPanoramica);
        $('#btn-trigger-file-panoramica')?.addEventListener('click', () => $('#input-file-panoramica')?.click());
        $('#btn-cambiar-panoramica')?.addEventListener('click', () => $('#input-file-panoramica')?.click());
        $('#input-file-panoramica')?.addEventListener('change', procesarArchivoPanoramica);

        const dropzone = $('#dropzone-panoramica');
        if (dropzone) {
            ['dragenter', 'dragover'].forEach(eventName => {
                dropzone.addEventListener(eventName, e => {
                    e.preventDefault();
                    e.stopPropagation();
                    dropzone.classList.add('dragover');
                });
            });
            ['dragleave', 'drop'].forEach(eventName => {
                dropzone.addEventListener(eventName, e => {
                    e.preventDefault();
                    e.stopPropagation();
                    dropzone.classList.remove('dragover');
                });
            });
            dropzone.addEventListener('drop', e => {
                const files = e.dataTransfer?.files;
                if (files && files.length > 0) {
                    cargarImagenPanoramicaEnVisor(files[0]);
                }
            });
            dropzone.addEventListener('click', e => { if (!e.target.closest('button, input')) $('#input-file-panoramica')?.click(); });
        }

        $('#btn-preset-terceros-molares')?.addEventListener('click', () => {
            [18, 28, 38, 48].forEach(num => {
                const sel = $(`#calib-estado-${num}`);
                if (sel) sel.value = 'AUSENTE';
                const item = $(`#calib-item-${num}`);
                if (item) {
                    item.className = 'tooth-calib-item is-ausente';
                }
            });
            mostrarAviso('Cordales marcadas como ausentes en la plantilla.');
        });

        $('#btn-preset-todos-presentes')?.addEventListener('click', () => {
            $$('#lista-calibracion-dientes select').forEach(sel => sel.value = 'PRESENTE');
            $$('#lista-calibracion-dientes .tooth-calib-item').forEach(it => it.className = 'tooth-calib-item');
            mostrarAviso('Todas las piezas marcadas como presentes.');
        });

        $('#btn-aplicar-panoramica-3d')?.addEventListener('click', aplicarPanoramica3D);

        $('#studio-opening')?.addEventListener('input', e => { viewer?.setOpening(Number(e.target.value) / 100); $('#studio-opening-value').textContent = e.target.value + '%'; });
        $('#studio-select-tooth')?.addEventListener('change', e => seleccionarDienteStudio(Number(e.target.value)));
        $('#studio-arch')?.addEventListener('change', e => viewer?.setOptions({ arch: e.target.value }));
        $('#btn-recuperar-atencion')?.addEventListener('click', abrirModalAtencion);
        $('#btn-guardar-borrador')?.addEventListener('click', () => guardarAtencion(false));
        $('#btn-recargar-atencion-conflicto')?.addEventListener('click', recargarAtencionTrasConflicto);

        // Botón del Inspector para agregar tratamiento o nota
        $('#btn-inspector-agregar')?.addEventListener('click', () => {
            abrirModalAtencion();
            const input = $('#form-crear-atencion')?.elements['piezasTratadas'];
            if (input && !input.value && !atencionEditor?.pending && !atencionEditor?.busy) input.value = studio3D.selectedTooth;
        });

        $('#btn-rotar-diente')?.addEventListener('click', () => {
            if (isolatedTooth3D.mesh) {
                isolatedTooth3D.mesh.rotation.y += Math.PI / 2;
            }
        });
    }

    function toggleHerramientaActiva(selector) {
        $$('.studio-3d-tool-btn').forEach(b => b.classList.remove('active'));
        $(selector)?.classList.add('active');
    }

    // ============================================================
    // MOTOR 3D CLINICAL STUDIO (THREE.JS)
    // ============================================================
    const NOMBRES_PIEZAS = {
        18: "Tercer Molar Sup. Der.", 17: "Segundo Molar Sup. Der.", 16: "Primer Molar Sup. Der.",
        15: "Segundo Premolar Sup. Der.", 14: "Primer Premolar Sup. Der.", 13: "Canino Sup. Der.",
        12: "Incisivo Lateral Sup. Der.", 11: "Incisivo Central Sup. Der.",
        21: "Incisivo Central Sup. Izq.", 22: "Incisivo Lateral Sup. Izq.", 23: "Canino Sup. Izq.",
        24: "Primer Premolar Sup. Izq.", 25: "Segundo Premolar Sup. Izq.", 26: "Primer Molar Sup. Izq.",
        27: "Segundo Molar Sup. Izq.", 28: "Tercer Molar Sup. Izq.",
        48: "Tercer Molar Inf. Der.", 47: "Segundo Molar Inf. Der.", 46: "Primer Molar Inf. Der.",
        45: "Segundo Premolar Inf. Der.", 44: "Primer Premolar Inf. Der.", 43: "Canino Inf. Der.",
        42: "Incisivo Lateral Inf. Der.", 41: "Incisivo Central Inf. Der.",
        31: "Incisivo Central Inf. Izq.", 32: "Incisivo Lateral Inf. Izq.", 33: "Canino Inf. Izq.",
        34: "Primer Premolar Inf. Izq.", 35: "Segundo Premolar Inf. Izq.", 36: "Primer Molar Inf. Izq.",
        37: "Segundo Molar Inf. Izq.", 38: "Tercer Molar Inf. Izq."
    };

    let studio3D = {
        initialized: false,
        scene: null,
        camera: null,
        renderer: null,
        controls: null,
        teethMeshes: new Map(),
        toothRotations: new Map(),
        toothStates: new Map(),
        selectedTooth: 26,
        selectedGroup: null,
        gumsGroup: null,
        gumsVisible: true,
        rootsVisible: true,
        isWireframe: false
    };

    let isolatedTooth3D = {
        initialized: false,
        scene: null,
        camera: null,
        renderer: null,
        mesh: null
    };

    let viewer = null;
    let viewerPromise = null;
    let geometrySaving = false;

    async function initStudio3D() {
        if (root.getAttribute('data-odonto-3d') !== 'true') return;
        if (viewerPromise) { viewer?.resize(); actualizarColoresStudio3D(); return viewerPromise; }
        const canvas = $('#canvas-odontograma-3d');
        if (!canvas) return;
        try {
            viewer = new DentiaDentalViewer({
                canvas, container: $('#studio-3d-viewport'),
                onSelect: seleccionarDienteStudio,
                onStatus: (text, status) => {
                    const label = $('#studio-model-status');
                    if (label) { label.textContent = text; label.dataset.status = status; }
                }
            });
            viewerPromise = viewer.ready.then(() => {
                studio3D.initialized = true; studio3D.teethMeshes = viewer.teeth;
                studio3D.camera = viewer.camera; studio3D.controls = viewer.controls;
                initIsolatedTooth3D(); actualizarColoresStudio3D(); seleccionarDienteStudio(studio3D.selectedTooth);
            }).catch(error => { console.error(error); mostrarAviso('No se pudo cargar el visor. Usa Caras 2D.', 'error'); });
            return viewerPromise;
        } catch (error) { mostrarAviso(error.message, 'error'); }
    }

    function initIsolatedTooth3D() {
        const canvas = $('#canvas-tooth-isolated');
        if (!canvas || isolatedTooth3D.initialized) return;
        const scene = new THREE.Scene(), camera = new THREE.PerspectiveCamera(38, 1, 0.01, 100);
        camera.position.set(0, 0, 4.2); camera.lookAt(0, 0, 0);
        const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
        renderer.outputEncoding = THREE.sRGBEncoding; renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        scene.add(new THREE.HemisphereLight(0xffffff, 0x819099, 1.3));
        const light = new THREE.DirectionalLight(0xffffff, 1); light.position.set(2, 3, 4); scene.add(light);
        const group = new THREE.Group(); scene.add(group);
        Object.assign(isolatedTooth3D, { initialized: true, scene, camera, renderer, mesh: group });
        new ResizeObserver(() => {
            const w = canvas.parentElement.clientWidth, h = canvas.parentElement.clientHeight;
            if (w && h) { camera.aspect = w / h; camera.updateProjectionMatrix(); renderer.setSize(w, h, false); }
        }).observe(canvas.parentElement);
        renderer.setAnimationLoop(() => {
            if (!document.hidden && canvas.getClientRects().length) renderer.render(scene, camera);
        });
    }

    function actualizarDienteAislado(num) {
        if (!isolatedTooth3D.mesh || !viewer) return;
        isolatedTooth3D.mesh.traverse(mesh => {
            if (mesh.isMesh) (Array.isArray(mesh.material) ? mesh.material : [mesh.material]).forEach(m => m.dispose());
        });
        viewer.isolate(num, isolatedTooth3D.mesh);
    }

    function seleccionarDienteStudio(num) {
        studio3D.selectedTooth = Number(num); viewer?.select(Number(num), false);
        const set = (selector, value) => { const element = $(selector); if (element) element.textContent = value; };
        set('#callout-numero-pieza', num);
        set('#inspector-pieza-titulo', `Diente ${num} · ${NOMBRES_PIEZAS[num] || 'Pieza dental'}`);
        set('#inspector-angulacion-val', `${studio3D.toothRotations.get(Number(num)) || 0}°`);
        actualizarDienteAislado(Number(num));
        const findings = (state.expediente?.piezasOdontograma || []).filter(f => Number(f.numeroPieza) === Number(num));
        const history = (state.expediente?.historialOdontograma || []).filter(f => Number(f.numeroPieza) === Number(num));
        const encounters = (state.expediente?.atenciones || []).filter(a => String(a.piezasTratadas || '').split(/\D+/).includes(String(num)));
        const last = encounters[0];
        set('#inspector-estado-badge', findings.length ? [...new Set(findings.map(f => f.condicion))].join(' · ') : 'Sin hallazgos registrados');
        set('#spec-fecha', last ? formatDate(last.fechaAtencion) : 'Sin atención registrada');
        set('#spec-tratamiento', last?.planProcedimiento || 'Sin procedimiento registrado');
        set('#spec-material', last?.materiales || 'No registrado');
        set('#spec-color', 'No registrado');
        set('#spec-observacion', findings.map(f => `${f.superficie}: ${f.condicion}`).join(' · ') || 'Selecciona una superficie en 2D para registrar un hallazgo.');
        const next = proximaCita();
        set('#spec-proxima-cita', next ? formatDateTime(next.fecha || next.inicio) : 'Sin cita programada');
        const timeline = $('#inspector-history-nodes');
        if (timeline) timeline.innerHTML = history.length ? history.map(item => `
            <div class="history-light-node"><span class="node-date">${escape(formatDateTime(item.fecha))}</span>
            <strong class="d-block">${escape(item.condicion || item.accion)} · ${escape(item.superficie)}</strong>
            <small>${escape(item.notas || '')} · ${escape(item.usuario)}</small></div>`).join('')
            : '<p class="small text-secondary">Sin cambios registrados para esta pieza.</p>';
        const select = $('#studio-select-tooth'); if (select) select.value = num;
    }

    async function guardarGeometria(piezas) {
        if (geometrySaving) return false;
        if (!state.expediente?.odontograma) { mostrarAviso('Selecciona un paciente antes de guardar.', 'error'); return false; }
        geometrySaving = true;
        try {
            await api('/interno/clinica/api/odontograma/configuracion', { method: 'POST', body: {
                odontogramaId: state.expediente.odontograma.id, version: state.expediente.versionOdontograma, piezas
            }});
            await cargarPaciente(state.pacienteId);
            mostrarAviso('Cambios del odontograma guardados.');
            return true;
        } catch (error) { mostrarAviso(error.message, 'error'); return false; }
        finally { geometrySaving = false; }
    }

    function ajustarInclinacionDienteSeleccionado(deltaGrados) {
        const num = studio3D.selectedTooth;
        return guardarGeometria([{ numeroPieza: num, inclinacion: Math.max(-90, Math.min(90, (studio3D.toothRotations.get(num) || 0) + deltaGrados)) }]);
    }

    function resetearInclinacionDienteSeleccionado() {
        return guardarGeometria([{ numeroPieza: studio3D.selectedTooth, inclinacion: 0 }]);
    }

    function cambiarEstadoRapidoDienteSeleccionado(estado) {
        const num = studio3D.selectedTooth;
        return guardarGeometria([{ numeroPieza: num, inclinacion: studio3D.toothRotations.get(num) || 0, presencia: estado === 'SANO' ? 'PRESENTE' : estado }]);
    }

    function abrirModalPanoramica() {
        const modalEl = $('#modal-asistente-panoramica');
        if (!modalEl) return;
        renderizarCalibracionPanoramica();
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }

    function renderizarCalibracionPanoramica() {
        const container = $('#lista-calibracion-dientes');
        if (!container) return;

        const cuadrantes = [
            { titulo: 'Cuadrante 1 (Superior Derecho)', piezas: [18, 17, 16, 15, 14, 13, 12, 11] },
            { titulo: 'Cuadrante 2 (Superior Izquierdo)', piezas: [21, 22, 23, 24, 25, 26, 27, 28] },
            { titulo: 'Cuadrante 4 (Inferior Derecho)', piezas: [48, 47, 46, 45, 44, 43, 42, 41] },
            { titulo: 'Cuadrante 3 (Inferior Izquierdo)', piezas: [31, 32, 33, 34, 35, 36, 37, 38] }
        ];

        let html = '';
        cuadrantes.forEach(q => {
            html += `<div class="tooth-calib-quadrant-header">${q.titulo}</div>`;
            q.piezas.forEach(num => {
                const estadoActual = studio3D.toothStates.get(num) || 'PRESENTE';
                const claseCss = estadoActual === 'AUSENTE' ? 'is-ausente' : (estadoActual === 'RETENIDO' ? 'is-retenido' : '');
                html += `
                    <div class="tooth-calib-item ${claseCss}" id="calib-item-${num}">
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-dark text-white fw-bold" style="width: 32px;">${num}</span>
                            <span class="small fw-semibold text-truncate" style="max-width: 130px;">${NOMBRES_PIEZAS[num] || ''}</span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <select class="form-select form-select-sm py-0" style="width: 105px; font-size: 0.75rem;" id="calib-estado-${num}" onchange="cambiarEstadoCalibItem(${num}, this.value)">
                                <option value="PRESENTE" ${estadoActual === 'PRESENTE' ? 'selected' : ''}>Presente</option>
                                <option value="AUSENTE" ${estadoActual === 'AUSENTE' ? 'selected' : ''}>Ausente</option>
                                <option value="RETENIDO" ${estadoActual === 'RETENIDO' ? 'selected' : ''}>Retenido</option>
                            </select>
                        </div>
                    </div>
                `;
            });
        });

        container.innerHTML = html;
    }

    window.cambiarEstadoCalibItem = function(num, val) {
        const item = $(`#calib-item-${num}`);
        if (!item) return;
        item.classList.remove('is-ausente', 'is-retenido', 'is-angulado');
        if (val === 'AUSENTE') item.classList.add('is-ausente');
        if (val === 'RETENIDO') item.classList.add('is-retenido');
    };

    function procesarArchivoPanoramica(e) {
        const file = e.target?.files?.[0];
        if (file) cargarImagenPanoramicaEnVisor(file);
    }

    function cargarImagenPanoramicaEnVisor(file) {
        if (!file || !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 10 * 1024 * 1024) {
            mostrarAviso('Selecciona JPG, PNG o WEBP de hasta 10 MB.', 'error');
            return;
        }

        const reader = new FileReader();
        reader.onload = ev => {
            const imgPreview = $('#img-panoramica-preview');
            const dropzone = $('#dropzone-panoramica');
            const contenedorPreview = $('#contenedor-preview-panoramica');
            const badgeStatus = $('#badge-panoramica-status');

            if (imgPreview) imgPreview.src = ev.target.result;
            if (dropzone) dropzone.classList.add('d-none');
            if (contenedorPreview) contenedorPreview.classList.remove('d-none');
            if (badgeStatus) {
                badgeStatus.className = 'badge bg-success-subtle text-success border border-success-subtle';
                badgeStatus.textContent = 'Radiografía Panorámica Cargada';
            }
            mostrarAviso('Radiografía panorámica cargada. Revisa los estados dentales.');
        };
        reader.readAsDataURL(file);
    }

    function cargarMuestraPanoramicaDemo() {
        const imgPreview = $('#img-panoramica-preview');
        const dropzone = $('#dropzone-panoramica');
        const contenedorPreview = $('#contenedor-preview-panoramica');
        const badgeStatus = $('#badge-panoramica-status');

        if (imgPreview) imgPreview.src = '/img/demo/panoramica_demo.jpg';
        if (dropzone) dropzone.classList.add('d-none');
        if (contenedorPreview) contenedorPreview.classList.remove('d-none');
        if (badgeStatus) {
            badgeStatus.className = 'badge bg-teal-soft text-teal border';
            badgeStatus.textContent = 'Muestra Clínica Demo Cargada';
        }

        mostrarAviso('Imagen de ejemplo cargada para revisión visual manual.');
    }

    function ejecutarEscaneoIAPanoramica() {
        const status = $('#badge-panoramica-status');
        if (status) status.textContent = 'Revisión manual · marca las piezas observadas';
        $('#lista-calibracion-dientes')?.scrollIntoView({ block: 'nearest' });
    }

    async function aplicarPanoramica3D() {
        const piezas = [...$$('#lista-calibracion-dientes select')].map(select => {
            const numeroPieza = Number(select.id.replace('calib-estado-', ''));
            return { numeroPieza, presencia: select.value, inclinacion: studio3D.toothRotations.get(numeroPieza) || 0 };
        });
        if (await guardarGeometria(piezas)) bootstrap.Modal.getInstance($('#modal-asistente-panoramica'))?.hide();
    }

    function moverCamara3D(dx, dy) { viewer?.orbit(0, dy > 0 ? 0.16 : -0.16); }
    function rotarCamara3D(rad) { viewer?.orbit(rad); }
    function zoomCamara3D(delta) { viewer?.zoom(delta < 0 ? 0.85 : 1.18); }
    function resetCamara3D() { viewer?.setView('three-quarter'); }
    function vistaOclusalCenital() { viewer?.setOpening(1); viewer?.setView('lower'); }
    function actualizarColoresStudio3D() {
        studio3D.toothRotations = new Map((state.expediente?.posicionesOdontograma || []).map(p => [Number(p.numeroPieza), Number(p.inclinacion)]));
        studio3D.toothStates = new Map((state.expediente?.piezasOdontograma || []).filter(p => ['AUSENTE', 'RETENIDO'].includes(p.condicion)).map(p => [Number(p.numeroPieza), p.condicion]));
        viewer?.setData(state.expediente?.piezasOdontograma || [], state.expediente?.posicionesOdontograma || []);
        viewer?.setOptions({ roots: studio3D.rootsVisible, gums: studio3D.gumsVisible, wireframe: studio3D.isWireframe });
        if (studio3D.initialized) seleccionarDienteStudio(studio3D.selectedTooth);
    }

    // Exponer API del Odontograma Unificado a ventana global para interacción directa
    window.DentiaClinica = {
        aprobarPlanDesdeSidebar,
        atenderProcedimientoDesdeSidebar,
        seleccionarDienteEnOdontograma,
        cotizarTratamientoEnPlanDesdeSidebar,
        abrirModalReceta,
        irAModulo: (mod) => {
            state.activeModulo = mod;
            renderModuloActual();
        }
    };

    async function init() {
        initEvents();
        if (state.pacienteId) await cargarPaciente(state.pacienteId);
        else renderModuloActual();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
