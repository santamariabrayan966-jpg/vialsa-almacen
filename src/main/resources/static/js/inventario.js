/* ==========================================================
   MODAL DE MOVIMIENTO DE INVENTARIO – VERSIÓN PRO (VIALSA)
   ========================================================== */

// -------------------------------------------
// ABRIR MODAL Y CARGAR EL FORMULARIO
// -------------------------------------------
async function abrirModal(url) {
    const modal = new bootstrap.Modal(document.getElementById("modalInventario"));
    const cont = document.getElementById("modalContenido");
    const titulo = document.getElementById("modalTitulo");

    // Indicador de carga mientras trae el HTML
    cont.innerHTML = `
        <div class="text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-3 text-muted">Cargando formulario...</p>
        </div>
    `;

    try {
        const resp = await fetch(url);
        if (!resp.ok) throw new Error("Error cargando formulario");

        const html = await resp.text();

        cont.innerHTML = html;
        titulo.textContent = "Registrar Movimiento";

        modal.show();

        inicializarBuscadorCliente();
        inicializarEnvioFormulario();

    } catch (e) {
        console.error(e);
        Swal.fire({
            icon: "error",
            title: "Error",
            text: "No se pudo cargar el formulario.",
            confirmButtonColor: "#dc3545"
        });
    }
}


// -------------------------------------------
// ENVÍO AJAX DEL FORMULARIO
// -------------------------------------------
function inicializarEnvioFormulario() {
    const form = document.getElementById("formMovimiento");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const data = new FormData(form);

        Swal.fire({
            title: "Procesando...",
            text: "Guardando movimiento",
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
        });

        try {
            const resp = await fetch(form.action, {
                method: "POST",
                body: data
            });

            if (resp.redirected) {
                Swal.close();
                window.location.reload();
            } else {
                Swal.fire({
                    icon: "error",
                    title: "Error",
                    text: "No se pudo guardar el movimiento."
                });
            }

        } catch (e) {
            console.error(e);
            Swal.fire({
                icon: "error",
                title: "Error inesperado",
                text: "Intente nuevamente."
            });
        }
    });
}


// -------------------------------------------
// BÚSQUEDA DE CLIENTE (API DNI / RUC)
// -------------------------------------------
function inicializarBuscadorCliente() {

    const btn = document.getElementById("buscarCliente");
    const inputNumero = document.getElementById("documentoCliente");
    const inputNombre = document.getElementById("nombreCliente");

    if (!btn || !inputNumero || !inputNombre) return;

    btn.addEventListener("click", async () => {

        const numero = inputNumero.value.trim();

        if (!numero) {
            Swal.fire({
                icon: "warning",
                title: "Atención",
                text: "Ingrese un número de documento"
            });
            return;
        }

        const tipo = numero.length === 8 ? "dni" : "ruc";

        inputNombre.value = "Buscando...";

        try {
            const resp = await fetch(`/api/externo/${tipo}/${numero}`);
            if (!resp.ok) throw new Error("Error en API");

            const data = await resp.json();

            if (!data.data) {
                inputNombre.value = "";
                Swal.fire({
                    icon: "info",
                    title: "No encontrado",
                    text: "No se encontraron datos para este documento."
                });
                return;
            }

            inputNombre.value =
                (data.data.nombre_completo ||
                 data.data.nombre ||
                 data.data.nombre_o_razon_social ||
                 "").trim();

        } catch (e) {
            console.error(e);
            inputNombre.value = "";
            Swal.fire({
                icon: "error",
                title: "Error",
                text: "No se pudo consultar el documento."
            });
        }
    });
}
