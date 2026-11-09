(() => {
    'use strict';

    const modalElement = document.getElementById('modalPaciente');
    const form = document.getElementById('formPaciente');
    if (!modalElement || !form || typeof bootstrap === 'undefined') return;

    // Configurar modal de Bootstrap para que no cierre por clic externo o escape
    const modal = bootstrap.Modal.getOrCreateInstance(modalElement, {
        backdrop: 'static',
        keyboard: false
    });

    const fields = {
        id: document.getElementById('pacienteId'),
        dni: document.getElementById('pacienteDni'),
        nombres: document.getElementById('pacienteNombres'),
        apellidos: document.getElementById('pacienteApellidos'),
        fecha: document.getElementById('pacienteFechaNacimiento'),
        direccion: document.getElementById('pacienteDireccion'),
        celular: document.getElementById('pacienteCelular'),
        correo: document.getElementById('pacienteCorreo')
    };

    const errors = {
        dni: document.getElementById('errorPacienteDni'),
        nombres: document.getElementById('errorPacienteNombres'),
        apellidos: document.getElementById('errorPacienteApellidos'),
        fecha: document.getElementById('errorPacienteFecha'),
        celular: document.getElementById('errorPacienteCelular'),
        correo: document.getElementById('errorPacienteCorreo')
    };

    const titulo = document.getElementById('modalPacienteTitulo');
    const guardar = document.getElementById('btnGuardarPaciente');
    const consultar = document.getElementById('btnConsultarDni');
    const estado = document.getElementById('estadoConsultaDni');
    let consultaEnCurso = false;

    const HOY_ISO = new Date().toISOString().split('T')[0];
    const ANIO_ACTUAL = new Date().getFullYear();

    // Establecer límites de fecha en el selector nativo
    if (fields.fecha) {
        fields.fecha.setAttribute('max', HOY_ISO);
        fields.fecha.setAttribute('min', '1900-01-01');
    }

    const valor = value => (value && value !== 'null') ? value.trim() : '';

    function estadoConsulta(mensaje, clase = '') {
        if (!estado) return;
        estado.textContent = mensaje;
        estado.className = `form-text ${clase}`.trim();
    }

    // ── GESTIÓN DE ERRORES IN SITU EN EL MODAL ─────────────────────────
    function mostrarError(campo, mensaje) {
        if (!campo) return;
        campo.classList.add('is-invalid');
        campo.classList.remove('is-valid');

        const errorDiv = campo === fields.fecha ? errors.fecha : errors[campo.id.replace('paciente', '').toLowerCase()];
        if (errorDiv) {
            errorDiv.textContent = mensaje;
            errorDiv.classList.add('d-block');
        }
    }

    function limpiarError(campo) {
        if (!campo) return;
        campo.classList.remove('is-invalid');
        campo.classList.toggle('is-valid', Boolean(campo.value && campo.value.trim()));

        const errorDiv = campo === fields.fecha ? errors.fecha : errors[campo.id.replace('paciente', '').toLowerCase()];
        if (errorDiv) {
            errorDiv.classList.remove('d-block');
        }
    }

    function limpiarTodosLosErrores() {
        Object.values(fields).forEach(f => { if (f) { limpiarError(f); f.classList.remove('is-valid'); } });
    }

    // ── VALIDACIÓN INDIVIDUAL DE CAMPOS ────────────────────────────────
    function validarDni() {
        const dni = fields.dni.value.trim();
        if (!dni) {
            mostrarError(fields.dni, 'El DNI es obligatorio.');
            return false;
        }
        if (!/^\d{8}$/.test(dni)) {
            mostrarError(fields.dni, 'El DNI debe tener exactamente 8 dígitos numéricos.');
            return false;
        }
        limpiarError(fields.dni);
        return true;
    }

    function validarNombres() {
        const nombres = fields.nombres.value.trim();
        if (!nombres) {
            mostrarError(fields.nombres, 'Los nombres son obligatorios.');
            return false;
        }
        if (nombres.length < 2) {
            mostrarError(fields.nombres, 'Ingrese al menos 2 caracteres.');
            return false;
        }
        if (!/^[A-ZÁÉÍÓÚÜÑ\s.'-]+$/i.test(nombres)) {
            mostrarError(fields.nombres, 'Solo se permiten letras y espacios (sin números ni símbolos especiales).');
            return false;
        }
        limpiarError(fields.nombres);
        return true;
    }

    function validarApellidos() {
        const apellidos = fields.apellidos.value.trim();
        if (!apellidos) {
            mostrarError(fields.apellidos, 'Los apellidos son obligatorios.');
            return false;
        }
        if (apellidos.length < 2) {
            mostrarError(fields.apellidos, 'Ingrese al menos 2 caracteres.');
            return false;
        }
        if (!/^[A-ZÁÉÍÓÚÜÑ\s.'-]+$/i.test(apellidos)) {
            mostrarError(fields.apellidos, 'Solo se permiten letras y espacios (sin números ni símbolos especiales).');
            return false;
        }
        limpiarError(fields.apellidos);
        return true;
    }

    function validarFechaNacimiento() {
        const fecha = fields.fecha.value.trim();
        if (!fecha) {
            mostrarError(fields.fecha, 'La fecha de nacimiento es obligatoria.');
            return false;
        }
        const partes = fecha.split('-');
        if (partes.length === 3) {
            const anio = parseInt(partes[0], 10);
            if (isNaN(anio) || anio < 1900 || anio > ANIO_ACTUAL) {
                mostrarError(fields.fecha, `Año inválido (${anio}). Debe ser entre 1900 y ${ANIO_ACTUAL}.`);
                return false;
            }
        }
        if (fecha > HOY_ISO) {
            mostrarError(fields.fecha, 'La fecha de nacimiento no puede ser futura.');
            return false;
        }
        limpiarError(fields.fecha);
        return true;
    }

    function validarCorreo() {
        const correo = fields.correo.value.trim();
        if (!correo) {
            limpiarError(fields.correo);
            return true;
        }
        const regexEmail = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!regexEmail.test(correo)) {
            mostrarError(fields.correo, 'Ingrese un formato de correo válido (ej: nombre@correo.com).');
            return false;
        }
        limpiarError(fields.correo);
        return true;
    }

    function validarCelular() {
        const celular = fields.celular.value.trim();
        if (!celular) {
            limpiarError(fields.celular);
            return true;
        }
        const digitos = celular.replace(/\D/g, '');
        if (digitos.length !== 9 || !digitos.startsWith('9')) {
            mostrarError(fields.celular, 'El celular en Perú debe tener 9 dígitos y empezar con 9.');
            return false;
        }
        limpiarError(fields.celular);
        return true;
    }

    // ── RESTRICCIÓN DE CARACTERES EN TIEMPO REAL Y MAYÚSCULAS ──────────

    // DNI: solo números
    fields.dni.addEventListener('input', event => {
        event.target.value = event.target.value.replace(/\D/g, '').slice(0, 8);
        if (event.target.value.length === 8) {
            validarDni();
        } else {
            fields.dni.classList.remove('is-valid');
            estadoConsulta('Ingresa los 8 dígitos para consultar.', '');
        }
    });

    // Nombres: MAYÚSCULAS en pantalla y en valor, sin números
    fields.nombres.addEventListener('input', event => {
        event.target.value = event.target.value.toUpperCase().replace(/[^A-ZÁÉÍÓÚÜÑ\s.'-]/g, '');
        validarNombres();
    });

    // Apellidos: MAYÚSCULAS en pantalla y en valor, sin números
    fields.apellidos.addEventListener('input', event => {
        event.target.value = event.target.value.toUpperCase().replace(/[^A-ZÁÉÍÓÚÜÑ\s.'-]/g, '');
        validarApellidos();
    });

    // Dirección: MAYÚSCULAS
    fields.direccion?.addEventListener('input', event => {
        event.target.value = event.target.value.toUpperCase();
        limpiarError(event.target);
    });

    // Celular: solo números máx 9
    fields.celular?.addEventListener('input', event => {
        event.target.value = event.target.value.replace(/\D/g, '').slice(0, 9);
        if (event.target.value.length > 0) validarCelular();
        else limpiarError(fields.celular);
    });

    // Fecha: validación en tiempo real
    fields.fecha?.addEventListener('change', validarFechaNacimiento);
    fields.fecha?.addEventListener('input', validarFechaNacimiento);

    // Correo: validación al tipear
    fields.correo?.addEventListener('input', () => {
        if (fields.correo.value.trim()) validarCorreo();
        else limpiarError(fields.correo);
    });

    // ── PREPARAR NUEVO / EDITAR ─────────────────────────────────────────
    function prepararNuevo() {
        form.reset();
        limpiarTodosLosErrores();
        fields.id.value = '';
        titulo.textContent = 'Nuevo paciente';
        guardar.disabled = false;
        guardar.innerHTML = '<i class="bi bi-check2-circle me-2"></i>Guardar paciente';
        estadoConsulta('Se completarán nombres, apellidos y ubicación cuando estén disponibles.');
        setTimeout(() => fields.dni.focus(), 250);
    }

    document.getElementById('btnNuevoPaciente')?.addEventListener('click', prepararNuevo);

    document.querySelectorAll('.btn-editar-paciente').forEach(button => {
        button.addEventListener('click', () => {
            limpiarTodosLosErrores();
            fields.id.value = valor(button.dataset.id);
            fields.dni.value = valor(button.dataset.dni);
            fields.nombres.value = valor(button.dataset.nombres).toUpperCase();
            fields.apellidos.value = valor(button.dataset.apellidos).toUpperCase();
            fields.fecha.value = valor(button.dataset.fecha);
            fields.direccion.value = valor(button.dataset.direccion).toUpperCase();
            fields.celular.value = valor(button.dataset.celular);
            fields.correo.value = valor(button.dataset.correo);
            if (fields.dni.value) validarDni();
            if (fields.nombres.value) validarNombres();
            if (fields.apellidos.value) validarApellidos();
            if (fields.fecha.value) validarFechaNacimiento();
            if (fields.direccion.value) limpiarError(fields.direccion);
            if (fields.celular.value) validarCelular();
            if (fields.correo.value) validarCorreo();
            titulo.textContent = 'Editar paciente';
            guardar.disabled = false;
            guardar.innerHTML = '<i class="bi bi-check2-circle me-2"></i>Guardar cambios';
            estadoConsulta('Puedes volver a consultar el DNI si deseas actualizar nombres y ubicación.');
        });
    });

    // ── CONSULTA EXTERNA RENIEC / DNI ──────────────────────────────────
    async function consultarDni() {
        const dni = fields.dni.value.replace(/\D/g, '').slice(0, 8);
        fields.dni.value = dni;
        if (dni.length !== 8 || consultaEnCurso) {
            estadoConsulta('El DNI debe contener exactamente 8 dígitos.', 'is-error');
            mostrarError(fields.dni, 'El DNI debe contener exactamente 8 dígitos.');
            return;
        }

        consultaEnCurso = true;
        consultar.disabled = true;
        estadoConsulta('Consultando datos del DNI…', 'is-loading');

        try {
            const response = await fetch(`/api/externo/dni/${encodeURIComponent(dni)}`, {
                headers: { Accept: 'application/json' }
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(payload.error || 'No fue posible consultar el DNI.');

            const data = payload.data || {};
            // Convertir a mayúsculas
            fields.nombres.value = valor(data.nombres).toUpperCase();
            fields.apellidos.value = valor(data.apellidos).toUpperCase();
            if (data.direccion) fields.direccion.value = data.direccion.toUpperCase();
            if (data.fecha_nacimiento && /^\d{4}-\d{2}-\d{2}$/.test(data.fecha_nacimiento)) {
                fields.fecha.value = data.fecha_nacimiento;
                limpiarError(fields.fecha);
            }

            validarDni();
            if (fields.nombres.value) validarNombres();
            if (fields.apellidos.value) validarApellidos();
            if (fields.fecha.value) validarFechaNacimiento();
            if (fields.direccion.value) limpiarError(fields.direccion);

            estadoConsulta(fields.fecha.value ? 'Datos encontrados. Completa el celular si corresponde.' : 'Datos encontrados. Completa la fecha de nacimiento y el celular si corresponde.', 'is-success');
            (fields.fecha.value ? fields.celular : fields.fecha).focus();
        } catch (error) {
            estadoConsulta(`${error.message} Puedes completar los datos manualmente.`, 'is-error');
        } finally {
            consultaEnCurso = false;
            consultar.disabled = false;
        }
    }

    consultar.addEventListener('click', consultarDni);

    fields.dni.addEventListener('blur', () => {
        if (!fields.id.value && fields.dni.value.length === 8) consultarDni();
    });

    fields.dni.addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            consultarDni();
        }
    });

    // ── VALIDACIÓN GLOBAL ANTES DE GUARDAR ─────────────────────────────
    form.addEventListener('submit', event => {
        // Forzar mayúsculas finales antes de enviar
        fields.nombres.value = fields.nombres.value.trim().toUpperCase();
        fields.apellidos.value = fields.apellidos.value.trim().toUpperCase();
        if (fields.direccion) fields.direccion.value = fields.direccion.value.trim().toUpperCase();

        const vDni = validarDni();
        const vNombres = validarNombres();
        const vApellidos = validarApellidos();
        const vFecha = validarFechaNacimiento();
        const vCorreo = validarCorreo();
        const vCelular = validarCelular();

        if (!vDni || !vNombres || !vApellidos || !vFecha || !vCorreo || !vCelular) {
            event.preventDefault();
            event.stopPropagation();

            // Enfocar el primer campo con error
            const primerInvalido = form.querySelector('.is-invalid');
            if (primerInvalido) primerInvalido.focus();
            return false;
        }

        guardar.disabled = true;
        guardar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Guardando…';
    });

    if (modalElement.dataset.autoOpen === 'true') modal.show();
})();
