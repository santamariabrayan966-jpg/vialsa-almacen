// ======================================================
//  proveedores.js – versión CORREGIDA FINAL
// ======================================================

document.addEventListener("DOMContentLoaded", () => {

    // ===== CSRF (Spring Security) =====
    const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
    const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

    // ===== Tooltips =====
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => {
        new bootstrap.Tooltip(el);
    });

    // ===== Modal =====
    const modal = new bootstrap.Modal(document.getElementById("modalProveedor"));
    const form  = document.getElementById("form-proveedor");

    const inputId  = document.getElementById("prov-idProveedor");
    const inputDoc = document.getElementById("prov-nroDocumento");
    const inputNom = document.getElementById("prov-nombreProveedor");
    const inputTel = document.getElementById("prov-telefono");
    const inputCor = document.getElementById("prov-correo");
    const inputDir = document.getElementById("prov-direccion");

    // ===== Offcanvas =====
    const offcanvas = new bootstrap.Offcanvas(document.getElementById("offcanvasProveedor"));
    let proveedorActualId = null;

    const detNombre    = document.getElementById("det-nombre");
    const detDocumento = document.getElementById("det-documento");
    const detTelefono  = document.getElementById("det-telefono");
    const detCorreo    = document.getElementById("det-correo");
    const detDireccion = document.getElementById("det-direccion");
    const detActivoToggle = document.getElementById("det-activo-toggle");
    const detActivoLabel  = document.getElementById("det-activo-label");

    // =====================================================
    //   CLICK DELEGADO GLOBAL
    // =====================================================

    document.addEventListener("click", async (e) => {

        // =================== NUEVO ===================
        if (e.target.closest("#btn-nuevo-proveedor")) {
            form.reset();
            inputId.value = "";
            document.getElementById("titulo-modal-proveedor").textContent = "Nuevo proveedor";
            modal.show();
            return;
        }

        // =================== EDITAR ===================
        const btnEditar = e.target.closest(".btn-editar-proveedor");
        if (btnEditar) {
            const id = btnEditar.dataset.id;
            const resp = await fetch(`/proveedores/api/${id}`);
            const p = await resp.json();

            inputId.value  = p.idProveedor;
            inputDoc.value = p.nroDocumento;
            inputNom.value = p.nombreProveedor;
            inputTel.value = p.telefono;
            inputCor.value = p.correo;
            inputDir.value = p.direccion;

            document.getElementById("titulo-modal-proveedor").textContent = "Editar proveedor";
            modal.show();
            return;
        }

        // =================== ELIMINAR ===================
        const btnEliminar = e.target.closest(".btn-eliminar-proveedor");
        if (btnEliminar) {
            const nombre = btnEliminar.dataset.nombre;
            const url = btnEliminar.dataset.url;

            Swal.fire({
                title: "¿Eliminar proveedor?",
                text: nombre,
                icon: "warning",
                showCancelButton: true,
                confirmButtonColor: "#d33030",
                confirmButtonText: "Eliminar",
                cancelButtonText: "Cancelar"
            }).then(r => {
                if (r.isConfirmed) window.location.href = url;
            });
            return;
        }

        // =================== VER (OFFCANVAS) ===================
        const btnVer = e.target.closest(".btn-ver-proveedor");
        if (btnVer) {
            const id = btnVer.dataset.id;
            const resp = await fetch(`/proveedores/api/${id}`);
            const p = await resp.json();

            proveedorActualId = p.idProveedor;

            detNombre.textContent    = p.nombreProveedor;
            detDocumento.textContent = p.nroDocumento;
            detTelefono.textContent  = p.telefono || "-";
            detCorreo.textContent    = p.correo   || "-";
            detDireccion.textContent = p.direccion || "-";

            detActivoToggle.checked = !!p.activo;
            detActivoLabel.textContent = p.activo ? "Activo" : "Inactivo";

            offcanvas.show();
            return;
        }

        // =================== CAMBIO DE ESTADO ===================
        const btnEstado = e.target.closest(".btn-estado-proveedor");
        if (btnEstado) {
            const id = btnEstado.dataset.id;
            const nuevoEstado = btnEstado.dataset.activo !== "true";

            const headers = {};
            headers[CSRF_HEADER] = CSRF_TOKEN;

            await fetch(`/proveedores/cambiar-estado/${id}?activo=${nuevoEstado}`, {
                method: "POST",
                headers
            });

            window.location.reload();
            return;
        }

    });

    // =====================================================
    //  CAMBIO DE ESTADO DESDE OFFCANVAS
    // =====================================================

    if (detActivoToggle) {
        detActivoToggle.addEventListener("change", async () => {

            const headers = {};
            headers[CSRF_HEADER] = CSRF_TOKEN;

            await fetch(`/proveedores/cambiar-estado/${proveedorActualId}?activo=${detActivoToggle.checked}`, {
                method: "POST",
                headers
            });

            detActivoLabel.textContent = detActivoToggle.checked ? "Activo" : "Inactivo";
            setTimeout(() => location.reload(), 400);
        });
    }

    // =====================================================
    //  CONSULTA DNI / RUC USANDO TU API INTERNA
    // =====================================================

    const btnBuscarDoc = document.getElementById("btn-buscar-doc");
    if (btnBuscarDoc) {
        btnBuscarDoc.addEventListener("click", async () => {

            const doc = inputDoc.value.trim();

            if (doc.length !== 8 && doc.length !== 11) {
                return Swal.fire("Documento inválido", "DNI = 8 dígitos | RUC = 11 dígitos", "warning");
            }

            const tipo = doc.length === 8 ? "dni" : "ruc";

            try {
                const resp = await fetch(`/api/externo/${tipo}/${doc}`);
                if (!resp.ok) throw new Error();

                const json = await resp.json();
                const d = json.data;

                inputNom.value = d.nombre_completo || d.nombres || "";
                inputDir.value = d.direccion || "";

                Swal.fire("Éxito", "Datos cargados correctamente", "success");

            } catch (e) {
                Swal.fire("Error", "No se encontraron datos", "error");
            }
        });
    }

});
