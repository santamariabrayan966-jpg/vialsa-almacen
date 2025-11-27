/* ============================
   BÚSQUEDA EN VIVO
============================ */
const inputBuscar = document.getElementById("buscarCliente");

if (inputBuscar) {
    inputBuscar.addEventListener("input", () => {
        const q = inputBuscar.value.trim();

        fetch(`/clientes/buscar?q=${encodeURIComponent(q)}`)
            .then(r => r.text())
            .then(html => {
                document.getElementById("tablaClientes").innerHTML = html;
            });
    });
}


/* ============================
   FILTROS AVANZADOS
============================ */
function filtrar(tipo) {

    fetch(`/clientes/filtro/${tipo}`)
        .then(r => r.text())
        .then(html => {
            document.getElementById("tablaClientes").innerHTML = html;
        });
}


/* ============================
   VIP
============================ */
function marcarVip(btn) {
    let id = btn.getAttribute("data-id");

    fetch(`/clientes/vip/${id}`, { method: "POST" })
        .then(() => location.reload());
}

function quitarVip(btn) {
    let id = btn.getAttribute("data-id");

    fetch(`/clientes/vip/quitar/${id}`, { method: "POST" })
        .then(() => location.reload());
}


/* ============================
   MOROSO
============================ */
function marcarMoroso(btn) {
    let id = btn.getAttribute("data-id");

    fetch(`/clientes/moroso/${id}`, { method: "POST" })
        .then(() => location.reload());
}

function quitarMoroso(btn) {
    let id = btn.getAttribute("data-id");

    fetch(`/clientes/moroso/quitar/${id}`, { method: "POST" })
        .then(() => location.reload());
}


/* ============================
   ACTIVAR / DESACTIVAR CLIENTE
============================ */
function activarCliente(btn) {
    let id = btn.getAttribute("data-id");

    fetch(`/clientes/activar/${id}`, { method: "POST" })
        .then(() => location.reload());
}

function desactivarCliente(btn) {
    let id = btn.getAttribute("data-id");

    fetch(`/clientes/desactivar/${id}`, { method: "POST" })
        .then(() => location.reload());
}


/* ============================
   PERFIL DEL CLIENTE
============================ */
let modalPerfil = document.getElementById("modalPerfil");

modalPerfil.addEventListener("show.bs.modal", function (event) {
    let id = event.relatedTarget.getAttribute("data-id");

    fetch(`/clientes/perfil/${id}`)
        .then(r => r.text())
        .then(html => {
            document.getElementById("perfilContenido").innerHTML = html;
        });
});


/* ============================
   NOTAS INTERNAS
============================ */
let modalNotas = document.getElementById("modalNotas");

modalNotas.addEventListener("show.bs.modal", function (event) {
    let id = event.relatedTarget.getAttribute("data-id");

    modalNotas.setAttribute("data-id", id);

    fetch(`/clientes/notas/${id}`)
        .then(r => r.json())
        .then(lista => {
            let cont = document.getElementById("listaNotas");
            cont.innerHTML = "";

            if (lista.length === 0) {
                cont.innerHTML = "<p class='text-muted'>Sin notas.</p>";
            } else {
                lista.forEach(n => {
                    cont.innerHTML += `<div class="alert alert-secondary p-2 mb-2">${n}</div>`;
                });
            }
        });
});

function guardarNota() {
    let id = modalNotas.getAttribute("data-id");
    let nota = document.getElementById("nuevaNota").value.trim();

    if (nota.length === 0) return;

    fetch(`/clientes/notas/agregar/${id}`, {
        method: "POST",
        body: nota,
        headers: { "Content-Type": "text/plain" }
    }).then(() => location.reload());
}


/* ============================
   HISTORIAL
============================ */
let modalHistorial = document.getElementById("modalHistorial");

modalHistorial.addEventListener("show.bs.modal", function (event) {
    let id = event.relatedTarget.getAttribute("data-id");

    fetch(`/clientes/historial/${id}`)
        .then(r => r.json())
        .then(lista => {
            let cont = document.getElementById("historialContenido");
            cont.innerHTML = "";

            if (lista.length === 0) {
                cont.innerHTML = "<p class='text-muted text-center'>Sin historial.</p>";
            } else {
                lista.forEach(item => {
                    cont.innerHTML += `<div class="alert alert-warning p-2 mb-2">${item}</div>`;
                });
            }
        });
});


/* ============================
   IMPORTAR CLIENTES
============================ */
function importarClientes() {
    let file = document.getElementById("archivoImportar").files[0];

    if (!file) {
        alert("Seleccione un archivo primero.");
        return;
    }

    let formData = new FormData();
    formData.append("archivo", file);  // <--- NOMBRE CORRECTO

    fetch("/clientes/importar", {
        method: "POST",
        body: formData
    })
        .then(r => r.text())
        .then(resp => {
            alert(resp);
            location.reload();
        });
}


/* ============================
   EXPORTAR CLIENTES
============================ */
function exportar(tipo) {
    window.location.href = `/clientes/exportar/${tipo}`;
}


/* ============================
   CAMBIO DE VISTA
============================ */
function vistaTabla() {
    document.getElementById("tablaClientes").classList.remove("d-none");
}

function vistaCards() {
    fetch("/clientes/cards")
        .then(r => r.text())
        .then(html => {
            document.getElementById("tablaClientes").innerHTML = html;
        });
}


/* ============================
   MODAL ELIMINAR PRO
============================ */
let modalEliminar = document.getElementById("modalEliminar");

modalEliminar.addEventListener("show.bs.modal", function (event) {
    let boton = event.relatedTarget;
    let id = boton.getAttribute("data-id");
    let nombre = boton.getAttribute("data-nombre");

    document.getElementById("clienteNombre").innerText = nombre;
    document.getElementById("btnConfirmarEliminar").href = `/clientes/eliminar/${id}`;
});
/* ============================
    NUEVO CLIENTE
============================ */
function abrirModalNuevoCliente() {

    document.getElementById("tituloModalCliente").innerText = "Nuevo Cliente";

    fetch("/clientes/nuevo?fragmento=true")
        .then(r => r.text())
        .then(html => {
            document.getElementById("contenidoFormCliente").innerHTML = html;
            new bootstrap.Modal(document.getElementById("modalFormCliente")).show();
        });
}


/* ============================
    EDITAR CLIENTE
============================ */
function abrirModalEditarCliente(id) {

    document.getElementById("tituloModalCliente").innerText = "Editar Cliente";

    fetch(`/clientes/editar/${id}?fragmento=true`)
        .then(r => r.text())
        .then(html => {
            document.getElementById("contenidoFormCliente").innerHTML = html;
            new bootstrap.Modal(document.getElementById("modalFormCliente")).show();
        });
}

document.addEventListener("change", function (e) {
    if (e.target.id === "fotoFile") {
        let file = e.target.files[0];
        if (!file) return;

        let reader = new FileReader();
        reader.onload = ev => document.getElementById("foto-preview").src = ev.target.result;
        reader.readAsDataURL(file);
    }
});
// ============================
//  API DNI CLIENTE (miapi.cloud)
// ============================
document.addEventListener("click", async function (e) {

    // Botón dentro del formulario del modal
    if (e.target && e.target.id === "btnBuscarDniCliente") {

        const dniInput = document.getElementById("dniCliente");
        if (!dniInput) return;

        const dni = dniInput.value.trim();
        if (!dni) {
            alert("Ingrese un DNI primero");
            return;
        }

        try {
            // 👈 URL CORRECTA SEGÚN TU CONTROLADOR
            const resp = await fetch(`/api/externo/dni/${dni}`);
            const json = await resp.json();

            if (json.error) {
                alert(json.error || "No se encontraron datos para ese DNI");
                return;
            }

            const data = json.data || {};

            // Rellenar campos del formulario
            const nombresInput   = document.getElementById("nombresCliente");
            const apellidosInput = document.getElementById("apellidosCliente");
            const direccionInput = document.getElementById("direccionCliente");

            if (nombresInput)   nombresInput.value   = data.nombres   || "";
            if (apellidosInput) apellidosInput.value = data.apellidos || "";
            if (direccionInput) direccionInput.value = data.direccion || "";

        } catch (err) {
            console.error(err);
            alert("Error al consultar la API de DNI");
        }
    }
});

