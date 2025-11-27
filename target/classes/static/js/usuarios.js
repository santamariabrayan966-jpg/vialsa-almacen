// usuarios.js
document.addEventListener('DOMContentLoaded', function () {

    // ─────────────────────────────────────────────
    // 0. REFERENCIAS A LOS MODALES
    // ─────────────────────────────────────────────
    const modalNuevoEl = document.getElementById('modalNuevoUsuario');
    const modalEditarEl = document.getElementById('modalEditarUsuario');

    const modalNuevoUsuario  = modalNuevoEl  ? new bootstrap.Modal(modalNuevoEl)  : null;
    const modalEditarUsuario = modalEditarEl ? new bootstrap.Modal(modalEditarEl) : null;

    // ─────────────────────────────────────────────
    // 1. BOTÓN "NUEVO USUARIO"
    // ─────────────────────────────────────────────
    const btnNuevoUsuario   = document.getElementById('btn-nuevo-usuario');
    const formNuevoUsuario  = document.getElementById('form-nuevo-usuario');
    const nuevoFotoFile     = document.getElementById('nuevo-fotoFile');
    const nuevoFotoPreview  = document.getElementById('nuevo-foto-preview');
    const nuevoSelectRol    = document.getElementById('nuevo-idRol');

    if (btnNuevoUsuario && modalNuevoUsuario && formNuevoUsuario) {
        btnNuevoUsuario.addEventListener('click', () => {
            formNuevoUsuario.reset();

            if (nuevoFotoPreview) {
                nuevoFotoPreview.src = '/images/default-user.png';
            }

            if (nuevoSelectRol) {
                const placeholder = nuevoSelectRol.querySelector('option[value=""]');
                if (placeholder) nuevoSelectRol.value = "";
                else nuevoSelectRol.selectedIndex = 0;
            }

            modalNuevoUsuario.show();
        });
    }

    // Vista previa en "Nuevo"
    if (nuevoFotoFile && nuevoFotoPreview) {
        nuevoFotoFile.addEventListener('change', function () {
            const file = this.files && this.files[0];
            if (!file) return;

            if (!file.type.startsWith('image/')) {
                Swal.fire('Archivo inválido', 'Selecciona una imagen válida (JPG, PNG, etc.)', 'warning');
                this.value = '';
                return;
            }

            const reader = new FileReader();
            reader.onload = e => nuevoFotoPreview.src = e.target.result;
            reader.readAsDataURL(file);
        });
    }

    // ─────────────────────────────────────────────
    // 2. MODAL EDITAR USUARIO
    // ─────────────────────────────────────────────
    const btnsEditar = document.querySelectorAll('.btn-editar-usuario');

    if (modalEditarUsuario && btnsEditar.length > 0) {
        btnsEditar.forEach(btn => {
            btn.addEventListener('click', function () {
                const idUsuario = this.getAttribute('data-id-usuario');
                if (!idUsuario) return;

                fetch(`/usuarios/${idUsuario}`)
                    .then(r => r.json())
                    .then(usuario => {

                        document.getElementById('edit-idUsuario').value      = usuario.idUsuario;
                        document.getElementById('edit-nombreUsuario').value  = usuario.nombreUsuario || '';
                        document.getElementById('edit-nroDocumento').value   = usuario.nroDocumento || '';
                        document.getElementById('edit-nombres').value        = usuario.nombres || '';
                        document.getElementById('edit-apellidos').value      = usuario.apellidos || '';
                        document.getElementById('edit-correo').value         = usuario.correo || '';
                        document.getElementById('edit-telefono').value       = usuario.telefono || '';
                        document.getElementById('edit-idRol').value          = usuario.idRol || '';
                        document.getElementById('edit-activo').value         = usuario.activo ? 'true' : 'false';

                        const fotoPreviewEdit = document.getElementById('edit-foto-preview');
                        if (fotoPreviewEdit) {
                            fotoPreviewEdit.src = usuario.foto
                                ? `/uploads/usuarios/${usuario.foto}`
                                : '/images/default-user.png';
                        }

                        modalEditarUsuario.show();
                    })
                    .catch(() => {
                        Swal.fire('Error', 'No se pudieron cargar los datos del usuario', 'error');
                    });
            });
        });
    }

    // Vista previa foto en "Editar"
    const inputFotoEdit = document.getElementById('edit-fotoFile');
    const imgPreviewEdit = document.getElementById('edit-foto-preview');
    if (inputFotoEdit && imgPreviewEdit) {
        inputFotoEdit.addEventListener('change', function () {
            const file = this.files && this.files[0];
            if (!file) return;

            if (!file.type.startsWith('image/')) {
                Swal.fire('Archivo inválido', 'Selecciona una imagen válida', 'warning');
                this.value = '';
                return;
            }

            const reader = new FileReader();
            reader.onload = e => imgPreviewEdit.src = e.target.result;
            reader.readAsDataURL(file);
        });
    }

    // ─────────────────────────────────────────────
    // 3. API DNI/RUC
    // ─────────────────────────────────────────────
 async function buscarPersonaPorDocumentoGenerico(idDoc, idNom, idApe) {

     const dni = document.getElementById(idDoc).value.trim();
     if (!dni) {
         Swal.fire("Atención", "Ingrese un número de documento", "warning");
         return;
     }

     try {
         const resp = await fetch(`/api/usuario/${dni}`);
         const data = await resp.json();

         if (data.error) {
             Swal.fire("No encontrado", data.error, "info");
             return;
         }

         document.getElementById(idNom).value = data.nombres || "";
         document.getElementById(idApe).value = data.apellidos || "";

     } catch (e) {
         console.log(e);
         Swal.fire("Error", "No se pudo consultar el DNI", "error");
     }
 }


    // ─────────────────────────────────────────────
    // BOTONES DNI — (Deben estar aquí ADENTRO)
    // ─────────────────────────────────────────────
    const btnBuscarNuevo = document.getElementById('btn-buscar-dni-nuevo');
    if (btnBuscarNuevo) {
        btnBuscarNuevo.addEventListener('click', () => {
            buscarPersonaPorDocumentoGenerico(
                'nuevo-nroDocumento',
                'nuevo-nombres',
                'nuevo-apellidos'
            );
        });
    }

    const btnBuscarEdit = document.getElementById('btn-buscar-dni-edit');
    if (btnBuscarEdit) {
        btnBuscarEdit.addEventListener('click', () => {
            buscarPersonaPorDocumentoGenerico(
                'edit-nroDocumento',
                'edit-nombres',
                'edit-apellidos'
            );
        });
    }

    // ─────────────────────────────────────────────
    // 4. Confirmaciones SweetAlert2
    // ─────────────────────────────────────────────
    function confirmarAccion(btnSelector, opciones) {
        document.querySelectorAll(btnSelector).forEach(btn => {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                const url = this.dataset.url;
                const username = this.dataset.username || 'este usuario';

                Swal.fire({
                    title: opciones.title,
                    text: (opciones.showUsername ? 'Usuario: ' + username : opciones.text) || '',
                    icon: opciones.icon,
                    showCancelButton: true,
                    confirmButtonText: opciones.confirmText,
                    cancelButtonText: 'Cancelar',
                    confirmButtonColor: opciones.confirmColor,
                    cancelButtonColor: '#6c757d',
                }).then(result => {
                    if (result.isConfirmed && url) {
                        window.location.href = url;
                    }
                });
            });
        });
    }

    confirmarAccion('.btn-activar', {
        title: '¿Deseas ACTIVAR este usuario?',
        showUsername: true,
        icon: 'question',
        confirmText: 'Sí, activar',
        confirmColor: '#198754'
    });

    confirmarAccion('.btn-desactivar', {
        title: '¿Deseas DESACTIVAR este usuario?',
        text: 'Ya no podrá iniciar sesión.',
        icon: 'warning',
        confirmText: 'Sí, desactivar',
        confirmColor: '#dc3545'
    });

    confirmarAccion('.btn-eliminar', {
        title: '¿Eliminar usuario?',
        showUsername: true,
        text: 'Esta acción no se puede deshacer.',
        icon: 'error',
        confirmText: 'Sí, eliminar',
        confirmColor: '#dc3545'
    });

    // ─────────────────────────────────────────────
    // 5. Validación de contraseña
    // ─────────────────────────────────────────────
    const formEditar = document.getElementById('form-editar-usuario');
    const nuevaPassInput = document.getElementById('edit-nuevaContrasena');
    const confirmarPassInput = document.getElementById('edit-confirmarContrasena');
    const errorPassLabel = document.getElementById('edit-password-error');

    if (formEditar) {
        formEditar.addEventListener('submit', function (e) {
            const nueva = nuevaPassInput.value.trim();
            const conf = confirmarPassInput.value.trim();

            if (nueva === '' && conf === '') {
                errorPassLabel.classList.add('d-none');
                return;
            }

            if (nueva !== conf) {
                e.preventDefault();
                errorPassLabel.textContent = 'Las contraseñas no coinciden.';
                errorPassLabel.classList.remove('d-none');
                return;
            }

            const strongRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,}$/;

            if (!strongRegex.test(nueva)) {
                e.preventDefault();
                errorPassLabel.textContent = 'La contraseña es débil.';
                errorPassLabel.classList.remove('d-none');
                return;
            }

            errorPassLabel.classList.add('d-none');
        });
    }

    // ─────────────────────────────────────────────
    // 6. Mostrar / ocultar contraseña
    // ─────────────────────────────────────────────
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('.toggle-password-btn');
        if (!btn) return;

        const group = btn.closest('.input-group');
        const input = group.querySelector('.password-toggle-input');
        const icon = btn.querySelector('i');

        if (input.type === 'password') {
            input.type = 'text';
            icon.classList.replace('bi-eye', 'bi-eye-slash');
        } else {
            input.type = 'password';
            icon.classList.replace('bi-eye-slash', 'bi-eye');
        }
    });

    // ─────────────────────────────────────────────
    // 7. Enviar formulario NUEVO USUARIO por AJAX
    // ─────────────────────────────────────────────
    const formNuevo = document.getElementById("form-nuevo-usuario");

    if (formNuevo) {
        formNuevo.addEventListener("submit", async function (e) {
            e.preventDefault(); // ⛔ No recargar la página

            const url = formNuevo.getAttribute("action");
            const formData = new FormData(formNuevo);

            try {
                const resp = await fetch(url, {
                    method: "POST",
                    body: formData
                });

                const data = await resp.json();

                // ❌ Si hubo errores del lado del backend (validación)
                if (data.status === "error") {

                    let mensaje = "<ul>";

                    data.errors.forEach(err => {
                        mensaje += `<li>${err.defaultMessage}</li>`;
                    });

                    mensaje += "</ul>";

                    Swal.fire({
                        icon: "error",
                        title: "Errores en el formulario",
                        html: mensaje
                    });

                    return;
                }

                // ✔ Usuario creado correctamente
                Swal.fire({
                    icon: "success",
                    title: "Usuario registrado",
                    text: "El usuario fue guardado correctamente.",
                    timer: 1500,
                    showConfirmButton: false
                }).then(() => {
                    window.location.reload(); // refresca tabla
                });

            } catch (error) {
                Swal.fire("Error", "No se pudo guardar el usuario", "error");
            }
        });
    }



}); // ← NADA DEBE IR DESPUÉS DE ESTO
