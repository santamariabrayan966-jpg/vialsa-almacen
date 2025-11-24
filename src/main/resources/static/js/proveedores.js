// proveedores.js
document.addEventListener('DOMContentLoaded', function () {

    const modalEl = document.getElementById('modalProveedor');
    const modalProveedor = modalEl ? new bootstrap.Modal(modalEl) : null;

    const tituloModal = document.getElementById('titulo-modal-proveedor');
    const formProveedor = document.getElementById('form-proveedor');

    const inputId        = document.getElementById('prov-idProveedor');
    const inputDoc       = document.getElementById('prov-nroDocumento');
    const inputNombre    = document.getElementById('prov-nombreProveedor');
    const inputTelefono  = document.getElementById('prov-telefono');
    const inputCorreo    = document.getElementById('prov-correo');
    const inputDireccion = document.getElementById('prov-direccion');

    // ─────────────────────────────────────────────
    // 1. NUEVO PROVEEDOR → abre modal vacío
    // ─────────────────────────────────────────────
    const btnNuevo = document.getElementById('btn-nuevo-proveedor');

    if (btnNuevo && modalProveedor && formProveedor) {
        btnNuevo.addEventListener('click', () => {
            formProveedor.reset();
            if (inputId) inputId.value = '';
            if (tituloModal) {
                tituloModal.innerHTML = '<i class="bi bi-truck me-2"></i> Nuevo proveedor';
            }
            modalProveedor.show();
        });
    }

    // ─────────────────────────────────────────────
    // 2. EDITAR PROVEEDOR → carga datos por fetch
    // ─────────────────────────────────────────────
    document.querySelectorAll('.btn-editar-proveedor').forEach(btn => {
        btn.addEventListener('click', function () {
            if (!modalProveedor) return;

            const id = this.dataset.id;
            if (!id) return;

            fetch(`/proveedores/api/${id}`)
                .then(resp => {
                    if (!resp.ok) {
                        throw new Error('No se pudo obtener el proveedor');
                    }
                    return resp.json();
                })
                .then(p => {
                    if (inputId)        inputId.value        = p.idProveedor ?? '';
                    if (inputDoc)       inputDoc.value       = p.nroDocumento ?? '';
                    if (inputNombre)    inputNombre.value    = p.nombreProveedor ?? '';
                    if (inputTelefono)  inputTelefono.value  = p.telefono ?? '';
                    if (inputCorreo)    inputCorreo.value    = p.correo ?? '';
                    if (inputDireccion) inputDireccion.value = p.direccion ?? '';

                    if (tituloModal) {
                        tituloModal.innerHTML = '<i class="bi bi-truck me-2"></i> Editar proveedor';
                    }

                    modalProveedor.show();
                })
                .catch(err => {
                    console.error(err);
                    Swal.fire('Error', 'No se pudieron cargar los datos del proveedor', 'error');
                });
        });
    });

    // ─────────────────────────────────────────────
    // 3. API documento (DNI / RUC) para proveedores
    // ─────────────────────────────────────────────

async function buscarProveedorPorDocumento() {
    if (!inputDoc) return;

    const numero = inputDoc.value.trim();
    if (!numero) {
        Swal.fire('Atención', 'Ingrese un número de documento (DNI o RUC)', 'warning');
        return;
    }

    // 8 dígitos -> DNI, 11 dígitos -> RUC
    const tipo = numero.length === 8 ? 'dni' : 'ruc';

    try {
        const resp = await fetch(`/api/externo/${tipo}/${numero}`);
        if (!resp.ok) {
            const txt = await resp.text();
            console.error('❌ Respuesta no OK:', resp.status, txt);
            Swal.fire('Error', 'La API devolvió un error (' + resp.status + ').', 'error');
            return;
        }

        const data = await resp.json();
        console.log('✅ Respuesta API externo:', data);

        if (data.error) {
            Swal.fire('Error', data.error, 'error');
            return;
        }

        if (!data.data) {
            Swal.fire('Sin datos', 'No se encontraron datos para ese documento.', 'info');
            return;
        }

        const nombreCompleto = data.data.nombre_completo || '';
        const direccion      = data.data.direccion || '';

        if (inputNombre)    inputNombre.value    = nombreCompleto;
        if (inputDireccion) inputDireccion.value = direccion;

    } catch (err) {
        console.error('❌ Error al consultar API externo:', err);
        Swal.fire('Error', 'Error al buscar los datos del documento.', 'error');
    }
}

    // 👉 CONECTAR la función a un botón y al Enter

    // Botón de lupa / buscar (asegúrate que exista en el HTML)
    const btnBuscarDoc = document.getElementById('btn-buscar-doc-proveedor');
    if (btnBuscarDoc) {
        btnBuscarDoc.addEventListener('click', buscarProveedorPorDocumento);
    }

    // Enter en el input de documento
    if (inputDoc) {
        inputDoc.addEventListener('keypress', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                buscarProveedorPorDocumento();
            }
        });
    }

    // ─────────────────────────────────────────────
    // 4. SweetAlert para ELIMINAR proveedor
    // ─────────────────────────────────────────────
    document.querySelectorAll('.btn-eliminar-proveedor').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();

            const url = this.getAttribute('href');
            const nombre = this.dataset.nombre || 'este proveedor';

            Swal.fire({
                title: '¿Eliminar proveedor?',
                text: 'Proveedor: ' + nombre,
                icon: 'warning',
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
