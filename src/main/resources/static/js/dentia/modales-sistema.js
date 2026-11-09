/**
 * DentIA - Control Universal de Modales y Validaciones del Sistema
 * 
 * Reglas aplicadas a nivel global en todos los módulos:
 * 1. Todos los modales del sistema se cierran ÚNICAMENTE con la 'X'.
 *    - Bloqueo de cierre por clic afuera (backdrop static).
 *    - Bloqueo de tecla Escape (keyboard false).
 *    - Conserva los controles de cierre de cada modal.
 * 2. Validación estricta de casilleros (no permite caracteres indebidos).
 * 3. Reporte de alertas en el modal directamente debajo del casillero errado.
 * 4. Nombres, apellidos y razones sociales SIEMPRE EN MAYÚSCULAS:
 *    - Se muestran en mayúsculas mientras se escribe (CSS text-transform).
 *    - Se convierten a mayúsculas en el valor del input al teclear.
 *    - Se envían y guardan en mayúsculas al servidor.
 */
(() => {
    'use strict';

    const HOY_ISO = new Date().toISOString().split('T')[0];
    const ANIO_ACTUAL = new Date().getFullYear();

    // ── 1. CONFIGURACIÓN UNIVERSAL DE BOOTSTRAP MODALS ──────────────────────
    if (typeof window !== 'undefined') {
        if (window.bootstrap && window.bootstrap.Modal) {
            window.bootstrap.Modal.Default = Object.assign(window.bootstrap.Modal.Default || {}, {
                backdrop: 'static',
                keyboard: false
            });
        }
    }

    /**
     * Ajusta un modal para que solo cierre con la 'X' y no por fuera
     */
    function configurarModal(modal) {
        if (!modal || modal.dataset.dentiaConfigured === 'true') return;
        modal.dataset.dentiaConfigured = 'true';

        // Forzar atributos static
        modal.setAttribute('data-bs-backdrop', 'static');
        modal.setAttribute('data-bs-keyboard', 'false');

        // Asegurar que la cabecera tenga la 'X' de cierre
        const header = modal.querySelector('.modal-header');
        if (header && !header.querySelector('[data-bs-dismiss]')) {
            const btnClose = document.createElement('button');
            btnClose.type = 'button';
            btnClose.className = 'btn-close';
            btnClose.setAttribute('data-bs-dismiss', 'modal');
            btnClose.setAttribute('aria-label', 'Cerrar');
            header.appendChild(btnClose);
        }


    }

    document.addEventListener('hidePrevented.bs.modal', event => {
        const close = event.target.querySelector('.modal-header [data-bs-dismiss="modal"]');
        if (!close) return;
        close.classList.remove('modal-close-attention');
        void close.offsetWidth;
        close.classList.add('modal-close-attention');
        close.focus();
    });

    // ── 2. VALIDACIONES Y FORMATEO DE CAMPOS ────────────────────────────────

    const REGEX_LETRAS = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s.'-]+$/;
    const REGEX_EMAIL = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    function esCampoBusquedaOFiltro(input) {
        if (!input) return false;
        if (input.type === 'search') return true;
        const id = (input.id || '').toLowerCase();
        const name = (input.name || '').toLowerCase();
        const cls = (input.className || '').toLowerCase();
        const placeholder = (input.placeholder || '').toLowerCase();
        const role = (input.getAttribute('role') || '').toLowerCase();

        return id.includes('filtro') || id.includes('buscar') || id.includes('search') ||
               name.includes('filtro') || name.includes('buscar') || name.includes('search') ||
               cls.includes('search') || cls.includes('filtro') ||
               placeholder.startsWith('buscar') || placeholder.includes('buscar') ||
               placeholder.includes('filtrar') ||
               input.hasAttribute('aria-autocomplete') || role === 'combobox' ||
               Boolean(input.closest('.patient-picker')) || Boolean(input.closest('[role="search"]'));
    }

    function esCampoNombre(input) {
        if (esCampoBusquedaOFiltro(input)) return false;
        const id = (input.id || '').toLowerCase();
        const name = (input.name || '').toLowerCase();
        const placeholder = (input.placeholder || '').toLowerCase();
        return id.includes('nombre') || name.includes('nombre') ||
               id.includes('apellido') || name.includes('apellido') ||
               id.includes('razon') || name.includes('razon') ||
               name.includes('titular') || id.includes('titular') ||
               placeholder.includes('nombre') || placeholder.includes('apellido');
    }

    function esCampoDni(input) {
        if (esCampoBusquedaOFiltro(input)) return false;
        const id = (input.id || '').toLowerCase();
        const name = (input.name || '').toLowerCase();
        const placeholder = (input.placeholder || '').toLowerCase();
        return id === 'dni' || name === 'dni' ||
               id.endsWith('dni') || name.endsWith('dni') ||
               id === 'nrodocumento' || name === 'nrodocumento' ||
               (placeholder.includes('8 dígitos') && !placeholder.includes('buscar'));
    }

    function esCampoRuc(input) {
        if (esCampoBusquedaOFiltro(input)) return false;
        const id = (input.id || '').toLowerCase();
        const name = (input.name || '').toLowerCase();
        return id.includes('ruc') || name.includes('ruc');
    }

    function esCampoCelular(input) {
        if (esCampoBusquedaOFiltro(input)) return false;
        const id = (input.id || '').toLowerCase();
        const name = (input.name || '').toLowerCase();
        return id.includes('celular') || name.includes('celular') ||
               id.includes('telefono') || name.includes('telefono') ||
               input.type === 'tel';
    }

    function esCampoFechaNacimiento(input) {
        const id = (input.id || '').toLowerCase();
        const name = (input.name || '').toLowerCase();
        return (id.includes('nacimiento') || name.includes('nacimiento')) && input.type === 'date';
    }

    /**
     * Muestra alerta de error directamente debajo del casillero errado
     */
    function mostrarErrorCampo(input, mensaje) {
        input.classList.add('is-invalid');
        input.classList.remove('is-valid');

        // Buscar o crear contenedor de feedback
        let parent = input.closest('.input-group') || input.parentElement;
        if (input.closest('.input-group')) {
            parent.classList.add('has-validation');
        }

        let feedback = parent.querySelector('.invalid-feedback');
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback d-block';
            parent.appendChild(feedback);
        } else {
            feedback.classList.add('d-block');
        }
        feedback.textContent = mensaje;
    }

    /**
     * Limpia la alerta de error del casillero
     */
    function limpiarErrorCampo(input) {
        input.classList.remove('is-invalid');
        const parent = input.closest('.input-group') || input.parentElement;
        const feedback = parent?.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.textContent = '';
            feedback.classList.remove('d-block');
        }
    }

    /**
     * Valida un campo específico y reporta ahí mismo si está mal
     */
    function validarCampo(input) {
        if (!input || input.type === 'hidden' || input.disabled) return true;
        if (esCampoBusquedaOFiltro(input)) return true;

        const val = input.value.trim();
        const isRequired = input.required || input.hasAttribute('required');

        // Campo requerido vacío
        if (isRequired && !val) {
            mostrarErrorCampo(input, 'Este campo es obligatorio.');
            return false;
        }

        // Si está vacío y no es requerido, es válido
        if (!val) {
            limpiarErrorCampo(input);
            return true;
        }

        // 1. Validación de DNI
        if (esCampoDni(input)) {
            if (!/^\d{8}$/.test(val)) {
                mostrarErrorCampo(input, 'El DNI debe contener exactamente 8 dígitos numéricos.');
                return false;
            }
        }

        // 2. Validación de RUC
        else if (esCampoRuc(input)) {
            if (!/^\d{11}$/.test(val)) {
                mostrarErrorCampo(input, 'El RUC debe contener exactamente 11 dígitos numéricos.');
                return false;
            }
        }

        // 3. Validación de Celular / Teléfono
        else if (esCampoCelular(input)) {
            const digits = val.replace(/\D/g, '');
            if (digits.length < 7 || digits.length > 15) {
                mostrarErrorCampo(input, 'Ingrese un número telefónico válido (mínimo 7 dígitos, celular 9 dígitos).');
                return false;
            }
            if (digits.length === 9 && !digits.startsWith('9')) {
                mostrarErrorCampo(input, 'Los celulares en Perú deben comenzar con el dígito 9.');
                return false;
            }
        }

        // 4. Validación de Nombres y Apellidos
        else if (esCampoNombre(input)) {
            if (val.length < 2) {
                mostrarErrorCampo(input, 'Debe contener al menos 2 caracteres.');
                return false;
            }
            if (!REGEX_LETRAS.test(val)) {
                mostrarErrorCampo(input, 'Solo se permiten letras y espacios (sin números ni símbolos especiales).');
                return false;
            }
        }

        // 5. Validación de Correo
        else if (input.type === 'email' || (input.name && input.name.toLowerCase().includes('correo'))) {
            if (!REGEX_EMAIL.test(val)) {
                mostrarErrorCampo(input, 'Ingrese un formato de correo electrónico válido (ej: usuario@dominio.com).');
                return false;
            }
        }

        // 6. Validación de Fecha de Nacimiento
        else if (esCampoFechaNacimiento(input)) {
            const partes = val.split('-');
            if (partes.length === 3) {
                const anio = parseInt(partes[0], 10);
                if (isNaN(anio) || anio < 1900 || anio > ANIO_ACTUAL) {
                    mostrarErrorCampo(input, `Año inválido (${anio}). Debe ser entre 1900 y ${ANIO_ACTUAL}.`);
                    return false;
                }
                if (val > HOY_ISO) {
                    mostrarErrorCampo(input, 'La fecha de nacimiento no puede ser una fecha futura.');
                    return false;
                }
            }
        }

        // Si pasó todas las reglas
        limpiarErrorCampo(input);
        return true;
    }

    /**
     * Aplica restricciones en vivo a los inputs
     */
    function configurarInput(input) {
        if (!input || input.dataset.dentiaInputBound === 'true') return;
        if (esCampoBusquedaOFiltro(input)) return;
        input.dataset.dentiaInputBound = 'true';

        // A. Nombres, apellidos, títulos, direcciones: MAYÚSCULAS en pantalla y en valor
        if (esCampoNombre(input) || (input.name && input.name.toLowerCase().includes('direccion'))) {
            input.style.textTransform = 'uppercase';

            input.addEventListener('input', function () {
                const inicio = this.selectionStart;
                const fin = this.selectionEnd;
                // Si es nombre/apellido, filtrar números
                if (esCampoNombre(this)) {
                    this.value = this.value.toUpperCase().replace(/[^A-ZÁÉÍÓÚÜÑ\s.'-]/g, '');
                } else {
                    this.value = this.value.toUpperCase();
                }
                if (inicio !== null && fin !== null) {
                    this.setSelectionRange(inicio, fin);
                }
                validarCampo(this);
            });

            input.addEventListener('blur', function () {
                this.value = this.value.trim().toUpperCase();
                validarCampo(this);
            });
        }

        // B. DNI: Solo números, máx 8 dígitos
        else if (esCampoDni(input)) {
            input.setAttribute('maxlength', '8');
            input.setAttribute('inputmode', 'numeric');

            input.addEventListener('input', function () {
                this.value = this.value.replace(/\D/g, '').slice(0, 8);
                validarCampo(this);
            });

            input.addEventListener('blur', function () {
                validarCampo(this);
            });
        }

        // C. RUC: Solo números, máx 11 dígitos
        else if (esCampoRuc(input)) {
            input.setAttribute('maxlength', '11');
            input.setAttribute('inputmode', 'numeric');

            input.addEventListener('input', function () {
                this.value = this.value.replace(/\D/g, '').slice(0, 11);
                validarCampo(this);
            });

            input.addEventListener('blur', function () {
                validarCampo(this);
            });
        }

        // D. Celular / Teléfono: Solo números
        else if (esCampoCelular(input)) {
            input.setAttribute('maxlength', '15');
            input.setAttribute('inputmode', 'tel');

            input.addEventListener('input', function () {
                this.value = this.value.replace(/\D/g, '').slice(0, 15);
                validarCampo(this);
            });

            input.addEventListener('blur', function () {
                validarCampo(this);
            });
        }

        // E. Fecha de nacimiento: Bloquear años futuros y límites 1900
        else if (esCampoFechaNacimiento(input)) {
            input.setAttribute('max', HOY_ISO);
            input.setAttribute('min', '1900-01-01');

            input.addEventListener('change', function () {
                validarCampo(this);
            });
            input.addEventListener('input', function () {
                validarCampo(this);
            });
        }

        // F. Correo
        else if (input.type === 'email' || (input.name && input.name.toLowerCase().includes('correo'))) {
            input.addEventListener('input', function () {
                if (this.value.trim()) validarCampo(this);
                else limpiarErrorCampo(this);
            });
            input.addEventListener('blur', function () {
                validarCampo(this);
            });
        }
    }

    /**
     * Intercepta formularios dentro de modales para validación y mayúsculas
     */
    function configurarFormulario(form) {
        if (!form || form.dataset.dentiaFormBound === 'true') return;
        form.dataset.dentiaFormBound = 'true';

        form.setAttribute('novalidate', 'true');

        form.addEventListener('submit', function (event) {
            const inputs = form.querySelectorAll('input, select, textarea');
            let formularioValido = true;
            let primerErroneo = null;

            // Validación específica si es el formulario de agendar cita
            if (form.id === 'formCita') {
                const pacienteInput = form.querySelector('#citaPaciente');
                const filtroInput = form.querySelector('#filtroPacienteCita');
                if (pacienteInput && (!pacienteInput.value || pacienteInput.value === '0')) {
                    if (filtroInput) {
                        mostrarErrorCampo(filtroInput, 'Debe seleccionar un paciente de la lista de sugerencias.');
                    }
                    formularioValido = false;
                    if (!primerErroneo) primerErroneo = filtroInput;
                }
            }

            inputs.forEach(input => {
                if (esCampoBusquedaOFiltro(input)) return;

                // Asegurar mayúsculas antes de enviar
                if (esCampoNombre(input) || (input.name && input.name.toLowerCase().includes('direccion'))) {
                    input.value = input.value.trim().toUpperCase();
                }

                const valido = validarCampo(input);
                if (!valido) {
                    formularioValido = false;
                    if (!primerErroneo) primerErroneo = input;
                }
            });

            if (!formularioValido) {
                event.preventDefault();
                event.stopPropagation();
                if (primerErroneo) {
                    primerErroneo.focus();
                }
                return false;
            }
        });
    }

    // ── 3. INICIALIZACIÓN Y MONITOREO DINÁMICO ──────────────────────────────

    function escanearTodo() {
        // Configurar todos los modales
        document.querySelectorAll('.modal').forEach(configurarModal);

        // Configurar inputs de modales
        document.querySelectorAll('.modal input, .modal select, .modal textarea').forEach(configurarInput);

        // Configurar formularios dentro de modales
        document.querySelectorAll('.modal form').forEach(configurarFormulario);

        // También aplicar mayúsculas a inputs fuera de modales si son de personas
        document.querySelectorAll('input').forEach(input => {
            if (esCampoNombre(input)) {
                configurarInput(input);
            }
        });
    }

    // Eventos de apertura de modal en Bootstrap
    document.addEventListener('show.bs.modal', event => {
        configurarModal(event.target);
        escanearTodo();
    });

    document.addEventListener('shown.bs.modal', event => {
        configurarModal(event.target);
        // Enfocar el primer input relevante
        const primerInput = event.target.querySelector('input:not([type="hidden"]):not([disabled]), select:not([disabled])');
        if (primerInput) setTimeout(() => primerInput.focus(), 150);
    });

    // Ejecutar al cargar la página
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', escanearTodo);
    } else {
        escanearTodo();
    }

    // Observador para elementos agregados dinámicamente
    const observer = new MutationObserver(() => {
        escanearTodo();
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });

})();
