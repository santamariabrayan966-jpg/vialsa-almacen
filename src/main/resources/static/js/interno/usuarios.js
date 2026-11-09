/**
 * DentIA - Gestión de Usuarios Internos
 * Soporta modal de creación y edición, previsualización de foto, validación completa de campos,
 * toggle con ojito de contraseñas y alternador entre vista de Tabla y Tarjetas (Cards).
 */
document.addEventListener('DOMContentLoaded', function () {

    // ─────────────────────────────────────────────
    // 1. ALTERNADOR DE VISTA (TABLA vs TARJETAS)
    // ─────────────────────────────────────────────
    const btnVistaTabla = document.getElementById('btnVistaTabla');
    const btnVistaCards = document.getElementById('btnVistaCards');
    const vistaTablaContainer = document.getElementById('vistaTablaContainer');
    const vistaCardsContainer = document.getElementById('vistaCardsContainer');

    function aplicarVista(vista) {
        if (!vistaTablaContainer || !vistaCardsContainer) return;

        if (vista === 'cards') {
            vistaTablaContainer.classList.add('d-none');
            vistaCardsContainer.classList.remove('d-none');
            if (btnVistaCards) btnVistaCards.classList.add('active');
            if (btnVistaTabla) btnVistaTabla.classList.remove('active');
            localStorage.setItem('dentia_usuarios_vista', 'cards');
        } else {
            vistaCardsContainer.classList.add('d-none');
            vistaTablaContainer.classList.remove('d-none');
            if (btnVistaTabla) btnVistaTabla.classList.add('active');
            if (btnVistaCards) btnVistaCards.classList.remove('active');
            localStorage.setItem('dentia_usuarios_vista', 'tabla');
        }
    }

    if (btnVistaTabla) {
        btnVistaTabla.addEventListener('click', () => aplicarVista('tabla'));
    }
    if (btnVistaCards) {
        btnVistaCards.addEventListener('click', () => aplicarVista('cards'));
    }

    // Restaurar vista guardada
    const vistaGuardada = localStorage.getItem('dentia_usuarios_vista') || 'tabla';
    aplicarVista(vistaGuardada);

    // ─────────────────────────────────────────────
    // 2. REFERENCIAS AL MODAL DE USUARIO
    // ─────────────────────────────────────────────
    const modalEl = document.getElementById('modalUsuario');
    const modalUsuario = modalEl ? bootstrap.Modal.getOrCreateInstance(modalEl) : null;
    const formUsuario = document.getElementById('formUsuarioModal');

    const modalTitulo = document.getElementById('modalUsuarioTitulo');
    const inputId = document.getElementById('modalIdUsuario');
    const inputEliminarFoto = document.getElementById('modalEliminarFoto');
    const inputNombres = document.getElementById('modalNombres');
    const inputApellidos = document.getElementById('modalApellidos');
    const inputDni = document.getElementById('modalDni');
    const inputTelefono = document.getElementById('modalTelefono');
    const inputDireccion = document.getElementById('modalDireccion');
    const inputCorreo = document.getElementById('modalCorreo');
    const btnConsultarDni = document.getElementById('btnConsultarDniUsuario');
    const estadoDni = document.getElementById('estadoDniUsuario');
    const inputUsername = document.getElementById('modalUsername');
    const selectRol = document.getElementById('modalRol');
    const inputPassword = document.getElementById('modalPassword');
    const inputFoto = document.getElementById('modalFotoInput');
    const imgPreview = document.getElementById('modalFotoPreview');
    const imgPlaceholder = document.getElementById('modalFotoPlaceholder');
    const editPasswordHint = document.getElementById('modalEditPasswordHint');
    const passwordLabel = document.getElementById('modalPasswordLabel');
    const btnQuitarFoto = document.getElementById('btnQuitarFotoUsuario');

    function actualizarRequisitosPassword() {
        const value = inputPassword?.value || '';
        const rules = {
            length: value.length >= 8 && value.length <= 80,
            upper: /[A-Z]/.test(value),
            lower: /[a-z]/.test(value),
            number: /[0-9]/.test(value),
            symbol: /[^\w\s]/.test(value)
        };
        document.querySelectorAll('#usuarioPasswordRequirements [data-rule]').forEach(item => {
            item.classList.toggle('is-met', rules[item.dataset.rule]);
        });
    }
    inputPassword?.addEventListener('input', actualizarRequisitosPassword);

    // ─────────────────────────────────────────────
    // 3. BOTÓN NUEVO USUARIO (MODAL)
    // ─────────────────────────────────────────────
    const btnNuevoUsuario = document.getElementById('btnNuevoUsuarioModal');
    if (btnNuevoUsuario && modalUsuario && formUsuario) {
        btnNuevoUsuario.addEventListener('click', () => {
            limpiarValidaciones();
            formUsuario.reset();
            inputId.value = '';
            if (inputEliminarFoto) inputEliminarFoto.value = 'false';
            btnQuitarFoto?.classList.add('d-none');

            if (modalTitulo) modalTitulo.textContent = 'Nuevo usuario interno';
            if (passwordLabel) passwordLabel.innerHTML = 'Contraseña <span class="text-danger">*</span>';
            if (inputPassword) {
                inputPassword.required = true;
                inputPassword.value = '';
            }
            if (editPasswordHint) editPasswordHint.classList.add('d-none');
            if (estadoDni) estadoDni.textContent = 'Consulta para completar nombres, apellidos y dirección.';

            // Resetear avatar
            if (imgPreview) {
                imgPreview.src = '';
                imgPreview.classList.add('d-none');
            }
            if (imgPlaceholder) {
                imgPlaceholder.classList.remove('d-none');
            }

            actualizarRequisitosPassword();
            modalUsuario.show();
        });
    }

    // ─────────────────────────────────────────────
    // 4. BOTONES EDITAR USUARIO (MODAL)
    // ─────────────────────────────────────────────
    function vincularBotonesEditar() {
        document.querySelectorAll('.btn-editar-usuario').forEach(btn => {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                limpiarValidaciones();
                if (formUsuario) formUsuario.reset();

                const id = this.getAttribute('data-id');
                const nombres = this.getAttribute('data-nombres') || '';
                const apellidos = this.getAttribute('data-apellidos') || '';
                const dni = this.getAttribute('data-dni') || '';
                const telefono = this.getAttribute('data-telefono') || '';
                const direccion = this.getAttribute('data-direccion') || '';
                const correo = this.getAttribute('data-correo') || '';
                const username = this.getAttribute('data-username') || '';
                const rol = this.getAttribute('data-rol') || '';
                const foto = this.getAttribute('data-foto') || '';

                if (modalTitulo) modalTitulo.textContent = `Editar usuario: ${nombres} ${apellidos}`;
                if (inputId) inputId.value = id;
                if (inputEliminarFoto) inputEliminarFoto.value = 'false';
                if (btnQuitarFoto) btnQuitarFoto.classList.toggle('d-none', !foto || foto === 'null');
                if (inputNombres) inputNombres.value = nombres;
                if (inputApellidos) inputApellidos.value = apellidos;
                if (inputDni) inputDni.value = dni;
                if (inputTelefono) inputTelefono.value = telefono;
                if (inputDireccion) inputDireccion.value = direccion;
                if (inputCorreo) inputCorreo.value = correo;
                if (inputUsername) inputUsername.value = username;
                if (selectRol) selectRol.value = rol;

                if (passwordLabel) passwordLabel.textContent = 'Nueva contraseña (opcional)';
                if (inputPassword) {
                    inputPassword.required = false;
                    inputPassword.value = '';
                }
                if (editPasswordHint) editPasswordHint.classList.remove('d-none');

                // Foto previa
                if (foto && foto !== 'null' && imgPreview && imgPlaceholder) {
                    imgPreview.src = `/uploads/usuarios/${foto}`;
                    imgPreview.classList.remove('d-none');
                    imgPlaceholder.classList.add('d-none');
                } else if (imgPreview && imgPlaceholder) {
                    imgPreview.src = '';
                    imgPreview.classList.add('d-none');
                    imgPlaceholder.classList.remove('d-none');
                }

                actualizarRequisitosPassword();
                if (modalUsuario) modalUsuario.show();
            });
        });
    }
    vincularBotonesEditar();

    btnQuitarFoto?.addEventListener('click', () => {
        if (inputEliminarFoto) inputEliminarFoto.value = 'true';
        if (inputFoto) inputFoto.value = '';
        imgPreview?.classList.add('d-none');
        imgPlaceholder?.classList.remove('d-none');
        btnQuitarFoto.classList.add('d-none');
    });

    async function consultarDniUsuario() {
        const dni = (inputDni?.value || '').trim();
        if (!/^\d{8}$/.test(dni)) {
            mostrarError(inputDni, 'El DNI debe contener exactamente 8 dígitos numéricos.');
            return;
        }
        if (estadoDni) estadoDni.textContent = 'Consultando DNI…';
        if (btnConsultarDni) btnConsultarDni.disabled = true;
        try {
            const response = await fetch(`/api/externo/dni/${encodeURIComponent(dni)}`, {headers: {'Accept': 'application/json'}});
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.error || 'No se encontraron datos para ese DNI.');
            }
            const data = payload.data || {};
            if (!data.nombres && !data.apellidos) throw new Error('No se encontraron datos para ese DNI.');
            if (data.nombres) inputNombres.value = data.nombres.toUpperCase();
            if (data.apellidos) inputApellidos.value = data.apellidos.toUpperCase();
            if (data.direccion && inputDireccion) inputDireccion.value = data.direccion.toUpperCase();
            marcarValido(inputDni);
            marcarValido(inputNombres);
            marcarValido(inputApellidos);
            if (estadoDni) estadoDni.textContent = 'Datos encontrados. Puedes editarlos antes de guardar.';
        } catch (error) {
            if (estadoDni) estadoDni.textContent = error.message || 'No se pudo consultar el DNI.';
            mostrarError(inputDni, error.message || 'No se pudo consultar el DNI.');
        } finally {
            if (btnConsultarDni) btnConsultarDni.disabled = false;
        }
    }

    btnConsultarDni?.addEventListener('click', consultarDniUsuario);
    inputDni?.addEventListener('blur', () => {
        if (/^\d{8}$/.test(inputDni.value.trim())) consultarDniUsuario();
    });

    // ─────────────────────────────────────────────
    // 5. PREVISUALIZACIÓN DE FOTO EN VIVO
    // ─────────────────────────────────────────────
    if (inputFoto && imgPreview && imgPlaceholder) {
        inputFoto.addEventListener('change', function () {
            const file = this.files && this.files[0];
            if (!file) return;

            if (!file.type.startsWith('image/')) {
                alert('Por favor selecciona un archivo de imagen válido (JPG, PNG, WebP).');
                this.value = '';
                return;
            }

            if (file.size > 5 * 1024 * 1024) {
                alert('La imagen no debe superar los 5 MB.');
                this.value = '';
                return;
            }

            if (inputEliminarFoto) inputEliminarFoto.value = 'false';
            btnQuitarFoto?.classList.remove('d-none');
            const reader = new FileReader();
            reader.onload = e => {
                imgPreview.src = e.target.result;
                imgPreview.classList.remove('d-none');
                imgPlaceholder.classList.add('d-none');
            };
            reader.readAsDataURL(file);
        });
    }

    // ─────────────────────────────────────────────
    // 6. TOGGLE DE CONTRASEÑA CON "OJITO" 👁️
    // ─────────────────────────────────────────────
    document.querySelectorAll('.btn-toggle-password').forEach(btn => {
        btn.addEventListener('click', function () {
            const targetId = this.getAttribute('data-target');
            const targetInput = document.getElementById(targetId);
            const icon = this.querySelector('i');

            if (targetInput) {
                if (targetInput.type === 'password') {
                    targetInput.type = 'text';
                    if (icon) {
                        icon.classList.remove('bi-eye');
                        icon.classList.add('bi-eye-slash');
                    }
                    this.setAttribute('title', 'Ocultar contraseña');
                } else {
                    targetInput.type = 'password';
                    if (icon) {
                        icon.classList.remove('bi-eye-slash');
                        icon.classList.add('bi-eye');
                    }
                    this.setAttribute('title', 'Ver contraseña');
                }
            }
        });
    });

    // ─────────────────────────────────────────────
    // 7. VALIDACIÓN COMPLETA DE TODOS LOS CAMPOS
    // ─────────────────────────────────────────────
    function limpiarValidaciones() {
        if (!formUsuario) return;
        formUsuario.querySelectorAll('.is-invalid, .is-valid').forEach(el => {
            el.classList.remove('is-invalid', 'is-valid');
        });
    }

    function mostrarError(campo, mensaje) {
        if (!campo) return;
        campo.classList.remove('is-valid');
        campo.classList.add('is-invalid');
        const feedback = campo.parentNode.querySelector('.invalid-feedback') ||
                         campo.parentNode.parentNode.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.textContent = mensaje;
        }
    }

    function marcarValido(campo) {
        if (!campo) return;
        campo.classList.remove('is-invalid');
        campo.classList.add('is-valid');
    }

    // Validadores individuales en tiempo real
    if (inputNombres) {
        inputNombres.addEventListener('input', () => {
            inputNombres.value = inputNombres.value.toUpperCase().replace(/[^A-ZÁÉÍÓÚÜÑ\s]/g, '');
            const val = inputNombres.value.trim();
            if (val.length < 2) {
                mostrarError(inputNombres, 'Ingresa al menos 2 caracteres.');
            } else if (!/^[A-ZÁÉÍÓÚÜÑ\s]+$/i.test(val)) {
                mostrarError(inputNombres, 'Los nombres solo deben contener letras.');
            } else {
                marcarValido(inputNombres);
            }
        });
    }

    if (inputApellidos) {
        inputApellidos.addEventListener('input', () => {
            inputApellidos.value = inputApellidos.value.toUpperCase().replace(/[^A-ZÁÉÍÓÚÜÑ\s]/g, '');
            const val = inputApellidos.value.trim();
            if (val.length < 2) {
                mostrarError(inputApellidos, 'Ingresa al menos 2 caracteres.');
            } else if (!/^[A-ZÁÉÍÓÚÜÑ\s]+$/i.test(val)) {
                mostrarError(inputApellidos, 'Los apellidos solo deben contener letras.');
            } else {
                marcarValido(inputApellidos);
            }
        });
    }

    if (inputDni) {
        inputDni.addEventListener('input', () => {
            const val = inputDni.value.trim();
            if (val.length > 0 && !/^\d{8}$/.test(val)) {
                mostrarError(inputDni, 'El DNI debe contener exactamente 8 dígitos numéricos.');
            } else {
                marcarValido(inputDni);
            }
        });
    }

    if (inputTelefono) {
        inputTelefono.addEventListener('input', () => {
            const val = inputTelefono.value.trim();
            if (val.length > 0 && !/^9\d{8}$|^\d{7,12}$/.test(val)) {
                mostrarError(inputTelefono, 'Ingresa un número telefónico válido (ej: 987654321).');
            } else {
                marcarValido(inputTelefono);
            }
        });
    }

    if (inputCorreo) {
        inputCorreo.addEventListener('input', () => {
            const val = inputCorreo.value.trim();
            if (val.length > 0 && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
                mostrarError(inputCorreo, 'Ingresa un correo electrónico válido (ej: doctor@dentia.pe).');
            } else {
                marcarValido(inputCorreo);
            }
        });
    }

    if (inputUsername) {
        inputUsername.addEventListener('input', () => {
            const val = inputUsername.value.trim();
            if (val.length < 4) {
                mostrarError(inputUsername, 'El usuario debe tener al menos 4 caracteres.');
            } else if (!/^[a-zA-Z0-9._-]+$/.test(val)) {
                mostrarError(inputUsername, 'Solo letras, números, puntos y guiones (sin espacios).');
            } else {
                marcarValido(inputUsername);
            }
        });
    }

    if (selectRol) {
        selectRol.addEventListener('change', () => {
            if (!selectRol.value) {
                mostrarError(selectRol, 'Selecciona un rol válido.');
            } else {
                marcarValido(selectRol);
            }
        });
    }

    if (inputPassword) {
        inputPassword.addEventListener('input', () => {
            const val = inputPassword.value;
            const esNuevo = !inputId.value;
            if (esNuevo || val.length > 0) {
                if (val.length < 8) {
                    mostrarError(inputPassword, 'La contraseña debe tener mínimo 8 caracteres.');
                } else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s])/.test(val)) {
                    mostrarError(inputPassword, 'Debe incluir mayúscula, minúscula, número y símbolo.');
                } else {
                    marcarValido(inputPassword);
                }
            } else {
                marcarValido(inputPassword);
            }
        });
    }

    // Validación final al enviar formulario
    if (formUsuario) {
        formUsuario.addEventListener('submit', function (e) {
            let esValido = true;

            // 1. Nombres
            if (!inputNombres || inputNombres.value.trim().length < 2) {
                mostrarError(inputNombres, 'El nombre es obligatorio (mínimo 2 caracteres).');
                esValido = false;
            }

            // 2. Apellidos
            if (!inputApellidos || inputApellidos.value.trim().length < 2) {
                mostrarError(inputApellidos, 'El apellido es obligatorio (mínimo 2 caracteres).');
                esValido = false;
            }

            // 3. DNI
            if (inputDni && inputDni.value.trim().length > 0 && !/^\d{8}$/.test(inputDni.value.trim())) {
                mostrarError(inputDni, 'El DNI debe tener exactamente 8 dígitos.');
                esValido = false;
            }

            // 4. Teléfono
            if (inputTelefono && inputTelefono.value.trim().length > 0 && !/^9\d{8}$|^\d{7,12}$/.test(inputTelefono.value.trim())) {
                mostrarError(inputTelefono, 'Ingresa un número telefónico válido.');
                esValido = false;
            }

            // 5. Correo
            if (inputCorreo && inputCorreo.value.trim().length > 0 && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(inputCorreo.value.trim())) {
                mostrarError(inputCorreo, 'El correo electrónico no tiene un formato válido.');
                esValido = false;
            }

            // 6. Username
            if (!inputUsername || inputUsername.value.trim().length < 4) {
                mostrarError(inputUsername, 'El nombre de usuario es obligatorio (mínimo 4 caracteres).');
                esValido = false;
            }

            // 7. Rol
            if (!selectRol || !selectRol.value) {
                mostrarError(selectRol, 'Debes seleccionar un rol para el usuario.');
                esValido = false;
            }

            // 8. Contraseña
            const esNuevo = !inputId.value;
            const passVal = inputPassword ? inputPassword.value : '';
            if (esNuevo && (!passVal || passVal.length < 8)) {
                mostrarError(inputPassword, 'La contraseña es obligatoria (mínimo 8 caracteres).');
                esValido = false;
            } else if (passVal && passVal.length > 0) {
                if (passVal.length < 8 || !/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s])/.test(passVal)) {
                    mostrarError(inputPassword, 'La contraseña debe tener mínimo 8 caracteres con mayúscula, minúscula, número y símbolo.');
                    esValido = false;
                }
            }

            if (!esValido) {
                e.preventDefault();
                const primerInvalido = formUsuario.querySelector('.is-invalid');
                if (primerInvalido) primerInvalido.focus();
            }
        });
    }
});
