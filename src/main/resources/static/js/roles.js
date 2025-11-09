// /static/js/roles.js
document.addEventListener('DOMContentLoaded', () => {

    // Nuevo Rol
    const btnNuevo = document.getElementById('btn-nuevo-rol');
    const modalElement = document.getElementById('modalRol');
    const modalRol = modalElement ? new bootstrap.Modal(modalElement) : null;

    const inputId = document.getElementById('rol-id');
    const inputNombre = document.getElementById('rol-nombre');
    const tituloModal = document.getElementById('modal-rol-title');

    if (btnNuevo && modalRol) {
        btnNuevo.addEventListener('click', () => {
            // modo nuevo
            inputId.value = 0;
            inputNombre.value = '';
            if (tituloModal) {
                tituloModal.innerHTML = `<i class="bi bi-shield-plus me-2"></i> Nuevo Rol`;
            }
            modalRol.show();
        });
    }

    // Editar Rol
    document.querySelectorAll('.btn-editar-rol').forEach(btn => {
        btn.addEventListener('click', () => {
            const idRol = btn.getAttribute('data-id-rol');
            const nombre = btn.getAttribute('data-nombre');

            if (!idRol || !modalRol) return;

            inputId.value = idRol;
            inputNombre.value = nombre || '';

            if (tituloModal) {
                tituloModal.innerHTML = `<i class="bi bi-pencil-square me-2"></i> Editar Rol`;
            }
            modalRol.show();
        });
    });

    // Eliminar Rol
    document.querySelectorAll('.btn-eliminar-rol').forEach(btn => {
        btn.addEventListener('click', () => {
            const url = btn.getAttribute('data-url');
            const nombre = btn.getAttribute('data-nombre') || 'este rol';

            Swal.fire({
                title: '¿Eliminar rol?',
                text: `Se desactivará el rol y los usuarios asociados. Rol: ${nombre}`,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Sí, eliminar',
                cancelButtonText: 'Cancelar',
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d'
            }).then(result => {
                if (result.isConfirmed && url) {
                    window.location.href = url;
                }
            });
        });
    });

    // Desactivar Rol
    document.querySelectorAll('.btn-desactivar-rol').forEach(btn => {
        btn.addEventListener('click', () => {
            const url = btn.getAttribute('data-url');
            const nombre = btn.getAttribute('data-nombre') || 'este rol';

            Swal.fire({
                title: '¿Desactivar rol?',
                text: `Los usuarios con este rol también serán desactivados. Rol: ${nombre}`,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Sí, desactivar',
                cancelButtonText: 'Cancelar',
                confirmButtonColor: '#ffc107',
                cancelButtonColor: '#6c757d'
            }).then(result => {
                if (result.isConfirmed && url) {
                    window.location.href = url;
                }
            });
        });
    });

    // Activar Rol
    document.querySelectorAll('.btn-activar-rol').forEach(btn => {
        btn.addEventListener('click', () => {
            const url = btn.getAttribute('data-url');
            const nombre = btn.getAttribute('data-nombre') || 'este rol';

            Swal.fire({
                title: '¿Activar rol?',
                text: `Rol: ${nombre}`,
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Sí, activar',
                cancelButtonText: 'Cancelar',
                confirmButtonColor: '#198754',
                cancelButtonColor: '#6c757d'
            }).then(result => {
                if (result.isConfirmed && url) {
                    window.location.href = url;
                }
            });
        });
    });

});
