document.addEventListener('DOMContentLoaded', () => {
    // Definición de las 4 secciones estructurales idénticas al menú lateral de DentIA
    const secciones = [
        {
            key: 'CONSULTORIO',
            nombre: 'CONSULTORIO',
            subtitulo: 'Operación diaria y pacientes',
            icono: 'bi-briefcase-fill',
            theme: 'theme-consultorio',
            modulos: [
                { key: 'dashboard', nombre: 'Inicio', icono: 'bi-house-door-fill', desc: 'Panel principal, KPIs operativos y métricas del día' },
                { key: 'pacientes', nombre: 'Pacientes', icono: 'bi-people-fill', desc: 'Directorio, registro y expedientes de pacientes' },
                { key: 'citas', nombre: 'Agenda y Citas', icono: 'bi-calendar2-week-fill', desc: 'Calendario clínico, programación de citas y turnos' }
            ]
        },
        {
            key: 'CLINICA',
            nombre: 'CLÍNICA',
            subtitulo: 'Expediente Clínico 360° y odontograma',
            icono: 'bi-journal-medical',
            theme: 'theme-clinica',
            modulos: [
                { key: 'historia_clinica', nombre: 'Historia clínica', icono: 'bi-journal-medical', desc: 'Antecedentes médicos, anamnesis y ficha clínica' },
                { key: 'odontograma', nombre: 'Acceso al odontograma', icono: 'bi-grid-3x3', desc: 'Visualización general y exploración dental' },
                { key: 'odontograma_2d', nombre: 'Ver odontograma 2D', icono: 'bi-eye-fill', desc: 'Odontograma interactivo bidimensional' },
                { key: 'odontograma_3d', nombre: 'Ver odontograma 3D', icono: 'bi-box', desc: 'Modelo tridimensional interactivo de arcadas dentales' },
                { key: 'diagnosticos', nombre: 'Diagnósticos', icono: 'bi-clipboard-check-fill', desc: 'Diagnósticos clínicos y catálogo CIE-10' },
                { key: 'tratamientos', nombre: 'Tratamientos', icono: 'bi-capsule', desc: 'Catálogo de procedimientos y tratamientos odontológicos' },
                { key: 'plan_tratamiento', nombre: 'Plan de tratamiento', icono: 'bi-list-stars', desc: 'Planes por fases terapéuticas, ortodoncia y presupuestos' },
                { key: 'atenciones', nombre: 'Atenciones realizadas', icono: 'bi-journal-check', desc: 'Registro de evoluciones y atenciones clínicas diarias' }
            ]
        },
        {
            key: 'FINANZAS',
            nombre: 'FINANZAS Y GESTIÓN',
            subtitulo: 'Cobranzas, documentos y reportes',
            icono: 'bi-cash-coin',
            theme: 'theme-finanzas',
            modulos: [
                { key: 'pagos', nombre: 'Pagos y Cobros', icono: 'bi-credit-card-fill', desc: 'Control de caja, pagos, cobranzas y comprobantes' },
                { key: 'reportes', nombre: 'Reportes', icono: 'bi-bar-chart-line-fill', desc: 'Reportes financieros, estadísticas y análisis asistenciales' },
                { key: 'presupuestos', nombre: 'Presupuestos', icono: 'bi-calculator-fill', desc: 'Cotizaciones y presupuestos odontológicos detallados' },
                { key: 'documentos', nombre: 'Documentos', icono: 'bi-file-earmark-medical-fill', desc: 'Consentimientos informados, recetas y certificados' },
                { key: 'seguimiento', nombre: 'Seguimiento', icono: 'bi-bell-fill', desc: 'Monitoreo de pacientes y avisos post-tratamiento' }
            ]
        },
        {
            key: 'ADMINISTRACION',
            nombre: 'ADMINISTRACIÓN',
            subtitulo: 'Personal, servicios y configuración',
            icono: 'bi-gear-fill',
            theme: 'theme-administracion',
            modulos: [
                { key: 'profesionales', nombre: 'Profesionales', icono: 'bi-person-vcard-fill', desc: 'Odontólogos, especialistas, colegiaturas y horarios' },
                { key: 'servicios', nombre: 'Servicios y precios', icono: 'bi-list-check', desc: 'Tipos de atención, tarifario clínico y tiempos estimados' },
                { key: 'usuarios', nombre: 'Usuarios', icono: 'bi-person-gear', desc: 'Cuentas de usuario del personal, credenciales y estados' },
                { key: 'roles', nombre: 'Roles y permisos', icono: 'bi-shield-check', desc: 'Gestión de roles y matriz de permisos del sistema' },
                { key: 'configuracion', nombre: 'Configuración', icono: 'bi-sliders', desc: 'Datos del consultorio, RUC, logo y parámetros generales' }
            ]
        }
    ];

    // Elementos del modal de Rol
    const rolId = document.querySelector('#rol-id');
    const rolNombre = document.querySelector('#rol-nombre');
    const rolDesc = document.querySelector('#rol-descripcion');

    document.querySelector('#btn-nuevo-rol')?.addEventListener('click', () => {
        if (rolId) rolId.value = '';
        if (rolNombre) rolNombre.value = '';
        if (rolDesc) rolDesc.value = '';
    });

    document.querySelectorAll('.btn-editar-rol').forEach(btn => {
        btn.addEventListener('click', () => {
            if (rolId) rolId.value = btn.dataset.idRol || '';
            if (rolNombre) rolNombre.value = btn.dataset.nombre || '';
            if (rolDesc) rolDesc.value = btn.dataset.descripcion || '';
        });
    });

    // Funciones auxiliares de cálculo de contadores y estados visuales
    function actualizarContadoresGlobales() {
        const totalChecks = document.querySelectorAll('.permiso-check');
        const activos = Array.from(totalChecks).filter(c => c.checked).length;
        const total = totalChecks.length;

        const lblTotal = document.querySelector('#lbl-total-activos');
        if (lblTotal) lblTotal.textContent = activos;

        // Actualizar estados por grupo
        secciones.forEach(sec => {
            const grupoChecks = document.querySelectorAll(`.permiso-check[data-grupo="${sec.key}"]`);
            const grupoTotal = grupoChecks.length;
            const grupoActivos = Array.from(grupoChecks).filter(c => c.checked).length;

            const badgeCount = document.querySelector(`.grupo-count-badge[data-grupo="${sec.key}"]`);
            if (badgeCount) {
                badgeCount.textContent = `${grupoActivos}/${grupoTotal}`;
                badgeCount.classList.toggle('all-active', grupoActivos === grupoTotal && grupoTotal > 0);
            }

            const masterCheck = document.querySelector(`.grupo-master-check[data-grupo="${sec.key}"]`);
            if (masterCheck) {
                masterCheck.checked = grupoTotal > 0 && grupoActivos === grupoTotal;
                masterCheck.indeterminate = grupoActivos > 0 && grupoActivos < grupoTotal;
            }
        });
    }

    // Modal de Permisos
    document.querySelectorAll('.btn-permisos').forEach(btn => {
        btn.addEventListener('click', async () => {
            const id = btn.dataset.id;
            const nombreRol = btn.dataset.nombre || 'ROL';
            const modalEl = document.querySelector('#modalPermisos');
            const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
            const box = document.querySelector('#contenedor-permisos');
            const inputIdRol = document.querySelector('#permiso-idRol');
            const badgeRol = document.querySelector('#badge-rol-nombre');

            if (inputIdRol) inputIdRol.value = id;
            if (badgeRol) badgeRol.textContent = nombreRol;

            box.innerHTML = `
                <div class="text-center py-5">
                    <div class="spinner-border text-success" role="status">
                        <span class="visually-hidden">Cargando...</span>
                    </div>
                    <p class="text-muted small mt-2 mb-0">Cargando permisos del rol <strong>${nombreRol}</strong>...</p>
                </div>
            `;
            modal.show();

            try {
                const res = await fetch(`/interno/roles/permisos/ajax/${id}`);
                if (!res.ok) {
                    box.innerHTML = '<div class="alert alert-danger mb-0">No se pudieron cargar los permisos del rol.</div>';
                    return;
                }

                const permisos = await res.json();
                const permisosActivos = new Set(
                    permisos.filter(p => p.puedeAcceder).map(p => p.modulo)
                );

                // Renderizar las 4 secciones con diseño profesional
                let html = '<div class="permisos-cards-grid">';

                secciones.forEach(sec => {
                    html += `
                        <div class="grupo-card">
                            <div class="grupo-header">
                                <div class="grupo-title-wrap">
                                    <div class="grupo-icon-pill ${sec.theme}">
                                        <i class="bi ${sec.icono}"></i>
                                    </div>
                                    <div>
                                        <h3 class="grupo-nombre">${sec.nombre}</h3>
                                        <p class="grupo-subtitle">${sec.subtitulo}</p>
                                    </div>
                                </div>
                                <div class="grupo-actions">
                                    <span class="grupo-count-badge" data-grupo="${sec.key}">0/${sec.modulos.length}</span>
                                    <label class="grupo-check-label">
                                        <input type="checkbox" class="form-check-input grupo-master-check" data-grupo="${sec.key}">
                                        <span>Todos</span>
                                    </label>
                                </div>
                            </div>
                            <div class="grupo-items-list">
                    `;

                    sec.modulos.forEach(mod => {
                        const estaActivo = permisosActivos.has(mod.key);
                        html += `
                            <div class="modulo-item-row ${estaActivo ? 'is-active' : ''}" data-key="${mod.key}" data-grupo="${sec.key}">
                                <div class="modulo-item-icon">
                                    <i class="bi ${mod.icono}"></i>
                                </div>
                                <div class="modulo-item-text">
                                    <div class="modulo-item-title">${mod.nombre}</div>
                                    <div class="modulo-item-desc">${mod.desc}</div>
                                </div>
                                <div class="form-check form-switch modulo-item-switch">
                                    <input class="form-check-input permiso-check" type="checkbox" role="switch"
                                           data-grupo="${sec.key}" value="${mod.key}" ${estaActivo ? 'checked' : ''}>
                                </div>
                            </div>
                        `;
                    });

                    html += `
                            </div>
                        </div>
                    `;
                });

                html += '</div>';
                box.innerHTML = html;

                // Eventos de interacción con cada fila de módulo
                box.querySelectorAll('.modulo-item-row').forEach(row => {
                    const chk = row.querySelector('.permiso-check');

                    row.addEventListener('click', (e) => {
                        // Si se hace clic en el switch directamente, ya se actualiza solo
                        if (e.target === chk) {
                            row.classList.toggle('is-active', chk.checked);
                            actualizarContadoresGlobales();
                            return;
                        }
                        // Si se hace clic en cualquier parte de la fila, alternar el switch
                        chk.checked = !chk.checked;
                        row.classList.toggle('is-active', chk.checked);
                        actualizarContadoresGlobales();
                    });

                    chk.addEventListener('change', () => {
                        row.classList.toggle('is-active', chk.checked);
                        actualizarContadoresGlobales();
                    });
                });

                // Eventos de los checks maestros por sección ("Todos")
                box.querySelectorAll('.grupo-master-check').forEach(master => {
                    const grupoKey = master.dataset.grupo;
                    master.addEventListener('change', () => {
                        const checks = box.querySelectorAll(`.permiso-check[data-grupo="${grupoKey}"]`);
                        checks.forEach(c => {
                            c.checked = master.checked;
                            const row = c.closest('.modulo-item-row');
                            if (row) row.classList.toggle('is-active', master.checked);
                        });
                        master.indeterminate = false;
                        actualizarContadoresGlobales();
                    });
                });

                // Inicializar contadores
                actualizarContadoresGlobales();

            } catch (err) {
                console.error('Error al cargar permisos:', err);
                box.innerHTML = '<div class="alert alert-danger mb-0">Ocurrió un error al cargar la información de permisos.</div>';
            }
        });
    });

    // Botones rápidos de la barra superior del modal: Marcar / Desmarcar todos
    document.querySelector('#btn-marcar-todos')?.addEventListener('click', () => {
        document.querySelectorAll('.permiso-check').forEach(c => {
            c.checked = true;
            const row = c.closest('.modulo-item-row');
            if (row) row.classList.add('is-active');
        });
        actualizarContadoresGlobales();
    });

    document.querySelector('#btn-desmarcar-todos')?.addEventListener('click', () => {
        document.querySelectorAll('.permiso-check').forEach(c => {
            c.checked = false;
            const row = c.closest('.modulo-item-row');
            if (row) row.classList.remove('is-active');
        });
        actualizarContadoresGlobales();
    });

    // Guardado de permisos vía AJAX
    document.querySelector('#form-permisos')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const idRol = document.querySelector('#permiso-idRol')?.value;
        const headerName = document.querySelector('#csrf-header')?.value;
        const token = document.querySelector('#csrf-token')?.value;
        const btnGuardar = document.querySelector('#btn-guardar-permisos');

        if (!idRol) return;

        const accesos = Array.from(document.querySelectorAll('.permiso-check:checked')).map(c => c.value);

        const textoOriginal = btnGuardar ? btnGuardar.innerHTML : '';
        if (btnGuardar) {
            btnGuardar.disabled = true;
            btnGuardar.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span> Guardando...';
        }

        try {
            const res = await fetch(`/interno/roles/permisos/guardar/${idRol}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [headerName]: token
                },
                body: JSON.stringify(accesos)
            });

            if (res.ok) {
                location.href = '/interno/roles?guardado=true';
            } else {
                const errorMsg = await res.text();
                alert('No se pudieron guardar los permisos: ' + (errorMsg || 'Error desconocido.'));
                if (btnGuardar) {
                    btnGuardar.disabled = false;
                    btnGuardar.innerHTML = textoOriginal;
                }
            }
        } catch (err) {
            console.error('Error al guardar permisos:', err);
            alert('Error de conexión al guardar los permisos.');
            if (btnGuardar) {
                btnGuardar.disabled = false;
                btnGuardar.innerHTML = textoOriginal;
            }
        }
    });
});
