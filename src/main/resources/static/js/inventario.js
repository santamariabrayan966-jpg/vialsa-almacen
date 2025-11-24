// -------------------------------------------
// ABRIR MODAL Y CARGAR EL FORMULARIO
// -------------------------------------------
async function abrirModal(url) {
    const modal = new bootstrap.Modal(document.getElementById("modalInventario"));
    const cont = document.getElementById("modalContenido");
    const titulo = document.getElementById("modalTitulo");

    try {
        const resp = await fetch(url);
        const html = await resp.text();

        cont.innerHTML = html;
        titulo.textContent = "Registrar Movimiento";

        modal.show();

        inicializarBuscadorCliente();
        inicializarEnvioFormulario();

    } catch (e) {
        alert("Error cargando formulario.");
        console.error(e);
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

        const resp = await fetch(form.action, {
            method: "POST",
            body: data
        });

        if (resp.redirected) {
            location.reload();
        }
    });
}

// -------------------------------------------
// BÚSQUEDA DE CLIENTE
// -------------------------------------------
function inicializarBuscadorCliente() {

    const btn = document.getElementById("buscarCliente");
    const inputNumero = document.getElementById("documentoCliente");
    const inputNombre = document.getElementById("nombreCliente");

    if (!btn || !inputNumero || !inputNombre) return;

    btn.addEventListener("click", async () => {
        const numero = inputNumero.value.trim();

        if (!numero) return alert("Ingrese DNI o RUC");

        const tipo = numero.length === 8 ? "dni" : "ruc";

        inputNombre.value = "Buscando...";

        try {
            const resp = await fetch(`/api/externo/${tipo}/${numero}`);
            const data = await resp.json();

            if (!data.data) {
                inputNombre.value = "";
                return alert("No encontrado.");
            }

            inputNombre.value = (data.data.nombre_completo ||
                                 data.data.nombre ||
                                 data.data.nombre_o_razon_social ||
                                 "").trim();

        } catch (e) {
            console.error(e);
            alert("Error consultando.");
        }
    });
}
