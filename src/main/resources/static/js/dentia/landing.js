/**
 * DentIA - Landing Page & Acceso Clínico
 * Incluye atajo secreto administrativo (Ctrl + Shift + A), modal staff y reserva de citas interactiva en la misma página.
 */
document.addEventListener('DOMContentLoaded', () => {
    // 1. Atajo de teclado para acceso administrativo discreto
    window.addEventListener('keydown', (e) => {
        // Atajo: Ctrl + Shift + A o Alt + A
        if ((e.ctrlKey && e.shiftKey && (e.key === 'A' || e.key === 'a')) ||
            (e.altKey && (e.key === 'A' || e.key === 'a'))) {
            e.preventDefault();
            abrirModalStaff();
        }
    });

    // 2. Triple clic en el logo para invocar acceso staff
    const brandLogo = document.getElementById('dentiaBrandTrigger');
    if (brandLogo) {
        let clickCount = 0;
        let clickTimer = null;
        brandLogo.addEventListener('click', (e) => {
            clickCount++;
            if (clickCount === 3) {
                e.preventDefault();
                clickCount = 0;
                clearTimeout(clickTimer);
                abrirModalStaff();
            } else {
                clearTimeout(clickTimer);
                clickTimer = setTimeout(() => {
                    clickCount = 0;
                }, 600);
            }
        });
    }

    // 3. Función para abrir modal de staff y restaurar credenciales
    function restaurarCredencialesStaff() {
        const staffRemember = document.getElementById('staffRemember');
        const staffUserField = document.getElementById('staffUserField');
        const recordado = localStorage.getItem('dentia_staff_remember') === 'true';

        if (recordado && staffRemember) {
            staffRemember.checked = true;
            if (staffUserField && !staffUserField.value) {
                staffUserField.value = localStorage.getItem('dentia_staff_user') || '';
            }
        }
    }

    function abrirModalStaff() {
        const modalEl = document.getElementById('modalAccesoStaff');
        if (modalEl && window.bootstrap && window.bootstrap.Modal) {
            restaurarCredencialesStaff();
            const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
            modalInstance.show();
            setTimeout(() => {
                const userField = document.getElementById('staffUserField');
                const passField = document.getElementById('staffPassField');
                if (userField && !userField.value) userField.focus();
                else if (passField && !passField.value) passField.focus();
            }, 350);
        }
    }

    // Toggle de contraseña con "Ojito" 👁️ en Login de Staff
    const btnToggleStaffPass = document.getElementById('btnToggleStaffPassword');
    const staffPassInput = document.getElementById('staffPassField');
    if (btnToggleStaffPass && staffPassInput) {
        btnToggleStaffPass.addEventListener('click', (e) => {
            e.preventDefault();
            const icon = btnToggleStaffPass.querySelector('i');
            if (staffPassInput.type === 'password') {
                staffPassInput.type = 'text';
                if (icon) {
                    icon.classList.remove('bi-eye');
                    icon.classList.add('bi-eye-slash');
                }
                btnToggleStaffPass.setAttribute('title', 'Ocultar contraseña');
            } else {
                staffPassInput.type = 'password';
                if (icon) {
                    icon.classList.remove('bi-eye-slash');
                    icon.classList.add('bi-eye');
                }
                btnToggleStaffPass.setAttribute('title', 'Ver contraseña');
            }
        });
    }

    // Recordar solamente el usuario. La contraseña corresponde al gestor del navegador.
    const staffForm = document.querySelector('#modalAccesoStaff form');
    if (staffForm) {
        staffForm.addEventListener('submit', () => {
            const staffRemember = document.getElementById('staffRemember');
            const staffUserField = document.getElementById('staffUserField');
            if (staffRemember && staffRemember.checked) {
                localStorage.setItem('dentia_staff_remember', 'true');
                if (staffUserField) localStorage.setItem('dentia_staff_user', staffUserField.value);
            } else {
                localStorage.removeItem('dentia_staff_remember');
                localStorage.removeItem('dentia_staff_user');
            }
        });
    }

    // Borra una contraseña que pudiera haber quedado de versiones anteriores.
    localStorage.removeItem('dentia_staff_pass');

    // Inicializar credenciales guardadas
    restaurarCredencialesStaff();

    // Vincular botón discreto del footer al modal
    const footerStaffBtn = document.getElementById('btnStaffFooter');
    if (footerStaffBtn) {
        footerStaffBtn.addEventListener('click', (e) => {
            e.preventDefault();
            abrirModalStaff();
        });
    }

    // 4. Auto-abrir modal si la URL contiene parámetros de staff
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('staffLogin') || urlParams.has('staffError') ||
        urlParams.has('staffBloqueado') || urlParams.has('staffDisabled')) {
        setTimeout(abrirModalStaff, 250);
    }

    // 5. Smooth scrolling para navegación
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const targetId = this.getAttribute('href');
            if (targetId && targetId !== '#') {
                const targetElement = document.querySelector(targetId);
                if (targetElement) {
                    e.preventDefault();
                    targetElement.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            }
        });
    });

    // 6. SISTEMA DE RESERVA DE CITAS EN VIVO (Misma página)
    const selectTipo = document.getElementById('selectTipoAtencion');
    const selectProf = document.getElementById('selectProfesional');
    const inputFecha = document.getElementById('inputFecha');
    const contenedorSlots = document.getElementById('contenedorSlots');
    const slotStatusText = document.getElementById('slotStatusText');
    const inputHorario = document.getElementById('inputHorario');
    const inputIdProfesional = document.getElementById('inputIdProfesional');
    const btnConfirmar = document.getElementById('btnConfirmarCita');

    function cargarDisponibilidad() {
        if (!selectTipo || !inputFecha || !contenedorSlots) return;

        const idTipo = selectTipo.value;
        const fecha = inputFecha.value;
        const idProf = selectProf ? selectProf.value : '';

        if (!idTipo) {
            contenedorSlots.innerHTML = `
                <div class="text-muted small py-3">
                    <i class="bi bi-calendar-event fs-3 d-block mb-2 text-secondary"></i>
                    Selecciona un servicio para ver los turnos disponibles.
                </div>`;
            if (slotStatusText) slotStatusText.textContent = 'Selecciona un servicio y fecha';
            if (btnConfirmar) btnConfirmar.disabled = true;
            return;
        }

        if (!fecha) {
            contenedorSlots.innerHTML = `
                <div class="text-muted small py-3">
                    <i class="bi bi-calendar-date fs-3 d-block mb-2 text-secondary"></i>
                    Selecciona una fecha en el calendario.
                </div>`;
            if (slotStatusText) slotStatusText.textContent = 'Elige una fecha';
            if (btnConfirmar) btnConfirmar.disabled = true;
            return;
        }

        contenedorSlots.innerHTML = `
            <div class="py-4 text-center w-100">
                <div class="spinner-border text-teal" role="status" style="width: 2rem; height: 2rem; color: #0d9488;">
                    <span class="visually-hidden">Cargando disponibilidad...</span>
                </div>
                <div class="small text-muted mt-2">Buscando turnos disponibles...</div>
            </div>`;

        let url = `/disponibilidad?fecha=${encodeURIComponent(fecha)}&tipoAtencion=${encodeURIComponent(idTipo)}`;
        if (idProf) {
            url += `&profesional=${encodeURIComponent(idProf)}`;
        }

        fetch(url)
            .then(res => {
                if (!res.ok) throw new Error('Error al consultar turnos');
                return res.json();
            })
            .then(slots => {
                if (!slots || slots.length === 0) {
                    contenedorSlots.innerHTML = `
                        <div class="p-3 text-muted small text-center w-100">
                            <i class="bi bi-calendar-x fs-2 d-block mb-1 text-secondary"></i>
                            No hay turnos disponibles para esta fecha. Intenta seleccionando otro día o doctor.
                        </div>`;
                    if (slotStatusText) slotStatusText.textContent = 'Sin turnos disponibles';
                    if (btnConfirmar) btnConfirmar.disabled = true;
                    return;
                }

                if (slotStatusText) slotStatusText.textContent = `${slots.length} turnos disponibles`;

                let html = '';
                slots.forEach((slot, index) => {
                    const inicio = new Date(slot.inicio).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
                    const fin = new Date(slot.fin).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

                    html += `
                        <button type="button" class="slot-btn" 
                                data-horario="${slot.idHorario}" 
                                data-prof="${slot.idProfesional}"
                                data-prof-nombre="${slot.profesional}">
                            <div class="fw-bold">Turno ${index + 1} · ${inicio} – ${fin}</div>
                            <small class="text-muted d-block" style="font-size: .72rem;">${slot.profesional} · Orden de llegada</small>
                        </button>`;
                });

                contenedorSlots.innerHTML = html;

                // Agregar listeners a los botones de turnos
                contenedorSlots.querySelectorAll('.slot-btn').forEach(btn => {
                    btn.addEventListener('click', function() {
                        contenedorSlots.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('active'));
                        this.classList.add('active');

                        const horarioVal = this.getAttribute('data-horario');
                        const profIdVal = this.getAttribute('data-prof');

                        if (inputHorario) inputHorario.value = horarioVal;
                        if (inputIdProfesional) inputIdProfesional.value = profIdVal;

                        if (btnConfirmar) {
                            btnConfirmar.disabled = false;
                        }
                    });
                });
            })
            .catch(err => {
                contenedorSlots.innerHTML = `
                    <div class="p-3 text-danger small text-center w-100">
                        <i class="bi bi-exclamation-circle fs-3 d-block mb-1"></i>
                        No se pudo cargar la agenda en este momento. Intenta nuevamente.
                    </div>`;
                if (slotStatusText) slotStatusText.textContent = 'Error al cargar';
                if (btnConfirmar) btnConfirmar.disabled = true;
            });
    }

    if (selectTipo) selectTipo.addEventListener('change', cargarDisponibilidad);
    if (selectProf) selectProf.addEventListener('change', cargarDisponibilidad);
    if (inputFecha) inputFecha.addEventListener('change', cargarDisponibilidad);

    // Si ya viene algún tipo preseleccionado, cargar automáticamente
    if (selectTipo && selectTipo.value) {
        cargarDisponibilidad();
    }
});
