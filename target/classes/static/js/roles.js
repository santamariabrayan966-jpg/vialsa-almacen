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
// =============================
// ABRIR MODAL DE PERMISOS
// =============================
document.addEventListener("click", async (e) => {
    if (e.target.closest(".btn-permisos")) {

        const idRol = e.target.closest(".btn-permisos").dataset.id;
        const nombreRol = e.target.closest(".btn-permisos").dataset.nombre;

        document.querySelector("#permiso-idRol").value = idRol;

        // Cambiar título
        document.querySelector("#modalPermisos .modal-title").innerHTML =
            `<i class="bi bi-key"></i> Permisos de ${nombreRol}`;

        // Llamada AJAX para obtener permisos
        const response = await fetch(`/roles/permisos/ajax/${idRol}`);
        const permisos = await response.json();

        let html = `
            <table class="table table-bordered text-center align-middle">
                <thead class="table-dark">
                    <tr>
                        <th>Módulo</th>
                        <th>Acceso</th>
                    </tr>
                </thead>
                <tbody>
        `;

        permisos.forEach(p => {
            html += `
                <tr>
                    <td class="text-start">${p.modulo.toUpperCase()}</td>
                    <td>
                        <input type="checkbox" class="form-check-input permiso-check"
                            value="${p.modulo}" ${p.puedeAcceder ? "checked" : ""}>
                    </td>
                </tr>
            `;
        });

        html += "</tbody></table>";

        document.querySelector("#contenedor-permisos").innerHTML = html;

        // Mostrar modal
        const modal = new bootstrap.Modal(document.querySelector("#modalPermisos"));
        modal.show();
    }
});

// =============================
// GUARDAR PERMISOS (AJAX)
// =============================
document.querySelector("#form-permisos").addEventListener("submit", async (e) => {
    e.preventDefault();

    const idRol = document.querySelector("#permiso-idRol").value;

    const accesos = [...document.querySelectorAll(".permiso-check:checked")]
        .map(chk => chk.value);

    // 🔥 CSRF CORRECTO
    const csrfHeader = document.querySelector("#csrf").name;
    const csrfToken = document.querySelector("#csrf").value;

    await fetch(`/roles/permisos/guardar/${idRol}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken   // <<--- AHORA SI ENVÍA EL TOKEN
        },
        body: JSON.stringify(accesos)
    });

    Swal.fire({
        title: "Guardado",
        text: "Los permisos se actualizaron correctamente.",
        icon: "success",
        timer: 1500,
        showConfirmButton: false
    });

    bootstrap.Modal.getInstance(document.querySelector("#modalPermisos")).hide();
});
