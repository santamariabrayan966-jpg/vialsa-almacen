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
    // 1. BOTÓN "NUEVO USUARIO" → abre ventanita mini
    // ─────────────────────────────────────────────
    const btnNuevoUsuario = document.getElementById('btn-nuevo-usuario');
    const formNuevoUsuario = document.getElementById('form-nuevo-usuario');
    const nuevoFotoFile = document.getElementById('nuevo-fotoFile');
    const nuevoFotoPreview = document.getElementById('nuevo-foto-preview');

    if (btnNuevoUsuario && modalNuevoUsuario && formNuevoUsuario) {
        btnNuevoUsuario.addEventListener('click', () => {
            // Limpiar formulario
            formNuevoUsuario.reset();

            // Restaurar preview
            if (nuevoFotoPreview) {
                nuevoFotoPreview.src = '/images/default-user.png';
            }

            modalNuevoUsuario.show();
        });
    }

    // Vista previa foto en "Nuevo"
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
                    .then(response => {
                        if (!response.ok) {
                            throw new Error('No se pudo obtener el usuario');
                        }
                        return response.json();
                    })
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
                            if (usuario.foto) {
                                fotoPreviewEdit.src = `/uploads/usuarios/${usuario.foto}`;
                            } else {
                                fotoPreviewEdit.src = '/images/default-user.png';
                            }
                        }

                        modalEditarUsuario.show();
                    })
                    .catch(error => {
                        console.error(error);
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
                Swal.fire('Archivo inválido', 'Selecciona una imagen válida (JPG, PNG, etc.)', 'warning');
                this.value = '';
                return;
            }

            const reader = new FileReader();
            reader.onload = e => imgPreviewEdit.src = e.target.result;
            reader.readAsDataURL(file);
        });
    }

    // ─────────────────────────────────────────────
    // 3. FUNCIÓN GENÉRICA: API DNI/RUC
    // ─────────────────────────────────────────────
    async function buscarPersonaPorDocumentoGenerico(idInputDoc, idNombres, idApellidos) {
        const inputDoc = document.getElementById(idInputDoc);
        if (!inputDoc) return;

        const numero = inputDoc.value.trim();
        if (!numero) {
            Swal.fire('Atención', 'Ingrese un número de DNI o RUC', 'warning');
            return;
        }

        try {
            const tipo = numero.length === 8 ? 'dni' : 'ruc';
            const resp = await fetch(`/api/externo/${tipo}/${numero}`);

            if (!resp.ok) {
                throw new Error('Respuesta no OK de la API');
            }

            const data = await resp.json();

            const nombresInput   = document.getElementById(idNombres);
            const apellidosInput = document.getElementById(idApellidos);

            if (data.data) {
                const nombreCompleto = tipo === 'dni'
                    ? (data.data.nombre_completo || data.data.nombre || '')
                    : (data.data.nombre_o_razon_social || data.data.razon_social || '');

                const nombreLimpio = (nombreCompleto || '').trim();

                let nombres = nombreLimpio;
                let apellidos = '';

                const partes = nombreLimpio.split(/\s+/);
                if (partes.length >= 3) {
                    apellidos = partes[0] + ' ' + partes[1];
                    nombres = partes.slice(2).join(' ');
                }

                if (nombresInput)   nombresInput.value   = nombres;
                if (apellidosInput) apellidosInput.value = apellidos;
            } else {
                Swal.fire('Sin datos', 'No se encontraron datos para ese documento.', 'info');
                if (nombresInput)   nombresInput.value   = '';
                if (apellidosInput) apellidosInput.value = '';
            }
        } catch (err) {
            console.error('❌ Error al consultar API externo:', err);
            Swal.fire('Error', 'Error al buscar los datos del documento.', 'error');
        }
    }

    // Botón DNI en EDITAR
    const btnBuscarDniEdit = document.getElementById('btn-buscar-dni-edit');
    if (btnBuscarDniEdit) {
        btnBuscarDniEdit.addEventListener('click', () =>
            buscarPersonaPorDocumentoGenerico('edit-nroDocumento', 'edit-nombres', 'edit-apellidos')
        );
    }

    const inputDocEdit = document.getElementById('edit-nroDocumento');
    if (inputDocEdit) {
        inputDocEdit.addEventListener('keypress', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                buscarPersonaPorDocumentoGenerico('edit-nroDocumento', 'edit-nombres', 'edit-apellidos');
            }
        });
    }

    // Botón DNI en NUEVO
    const btnBuscarDniNuevo = document.getElementById('btn-buscar-dni-nuevo');
    if (btnBuscarDniNuevo) {
        btnBuscarDniNuevo.addEventListener('click', () =>
            buscarPersonaPorDocumentoGenerico('nuevo-nroDocumento', 'nuevo-nombres', 'nuevo-apellidos')
        );
    }

    const inputDocNuevo = document.getElementById('nuevo-nroDocumento');
    if (inputDocNuevo) {
        inputDocNuevo.addEventListener('keypress', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                buscarPersonaPorDocumentoGenerico('nuevo-nroDocumento', 'nuevo-nombres', 'nuevo-apellidos');
            }
        });
    }

    // ─────────────────────────────────────────────
    // 4. SweetAlert: Activar / Desactivar / Eliminar
    // ─────────────────────────────────────────────

    // ACTIVAR
    document.querySelectorAll('.btn-activar').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const url = this.dataset.url;
            const username = this.dataset.username || 'este usuario';

            Swal.fire({
                title: '¿Deseas ACTIVAR este usuario?',
                text: 'Usuario: ' + username,
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Sí, activar',
                cancelButtonText: 'Cancelar',
                confirmButtonColor: '#198754',
                cancelButtonColor: '#6c757d',
                backdrop: true
            }).then(result => {
                if (result.isConfirmed && url) {
                    window.location.href = url;
                }
            });
        });
    });

    // DESACTIVAR
    document.querySelectorAll('.btn-desactivar').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const url = this.dataset.url;
            const username = this.dataset.username || 'este usuario';

            Swal.fire({
                title: '¿Deseas DESACTIVAR este usuario?',
                text: 'Ya no podrá iniciar sesión.',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Sí, desactivar',
                cancelButtonText: 'Cancelar',
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                backdrop: true
            }).then(result => {
                if (result.isConfirmed && url) {
                    window.location.href = url;
                }
            });
        });
    });

    // ELIMINAR
    document.querySelectorAll('.btn-eliminar').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const url = this.dataset.url;
            const username = this.dataset.username || 'este usuario';

            Swal.fire({
                title: '¿Eliminar usuario?',
                text: 'Esta acción no se puede deshacer. Usuario: ' + username,
                icon: 'error',
                showCancelButton: true,
                confirmButtonText: 'Sí, eliminar',
                cancelButtonText: 'Cancelar',
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                backdrop: true
            }).then(result => {
                if (result.isConfirmed && url) {
                    window.location.href = url;
                }
            });
        });
    });

});
