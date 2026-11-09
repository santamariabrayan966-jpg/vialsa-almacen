/* ============================
   CSRF SPRING SECURITY
============================ */
function csrfHeaders() {

    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;

    return token && header
        ? { [header]: token }
        : {};
}

function escaparHtml(valor) {
    return String(valor ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function texto(valor) {
    return valor === null || valor === undefined || String(valor).trim() === "" ? "—" : String(valor);
}

function formatearMonto(valor) {
    return new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" }).format(Number(valor || 0));
}

function formatearFecha(valor) {
    if (!valor) return "—";
    const fecha = new Date(valor);
    return Number.isNaN(fecha.getTime()) ? "—" : new Intl.DateTimeFormat("es-PE", {
        dateStyle: "short", timeStyle: "short"
    }).format(fecha);
}

const estadoListadoClientes = { pagina: 0, tamano: 10, buscar: "", filtro: "TODOS", vista: "tabla" };
let temporizadorBusquedaClientes = null;

function sincronizarEstadoListadoClientes() {
    const listado = document.getElementById("listadoClientes");
    if (!listado) return;
    estadoListadoClientes.pagina = Number(listado.dataset.pagina || 0);
    estadoListadoClientes.tamano = Number(listado.dataset.tamano || 10);
    estadoListadoClientes.filtro = listado.dataset.filtro || "TODOS";
    estadoListadoClientes.vista = listado.dataset.vista || "tabla";
    estadoListadoClientes.buscar = document.getElementById("buscarCliente")?.value.trim() || "";
}

function reemplazarListadoClientes(html) {
    const documento = new DOMParser().parseFromString(html, "text/html");
    const recibido = documento.querySelector("#listadoClientes");
    const actual = document.getElementById("listadoClientes");
    if (!actual || !recibido) throw new Error("No se pudo actualizar el listado de clientes.");
    actual.replaceWith(recibido);
    sincronizarEstadoListadoClientes();
    inicializarTooltipsClientes(recibido);
}

async function cargarListadoClientes(pagina = estadoListadoClientes.pagina) {
    const params = new URLSearchParams({
        pagina: String(Math.max(0, pagina)),
        tamano: String(estadoListadoClientes.tamano),
        buscar: estadoListadoClientes.buscar,
        filtro: estadoListadoClientes.filtro,
        vista: estadoListadoClientes.vista
    });
    const respuesta = await fetch(`/interno/clientes/pagina?${params}`, { headers: { "X-Requested-With": "XMLHttpRequest" } });
    if (!respuesta.ok) throw new Error("No se pudo cargar la página de clientes.");
    reemplazarListadoClientes(await respuesta.text());
    const url = new URL(location.href);
    [...params.entries()].forEach(([clave, valor]) => url.searchParams.set(clave, valor));
    history.replaceState(null, "", url);
}

function inicializarTooltipsClientes(contenedor = document) {
    if (!window.bootstrap?.Tooltip || !contenedor) return;
    contenedor.querySelectorAll(".btn-action[title], .cliente-card-actions [title]").forEach(elemento => {
        bootstrap.Tooltip.getOrCreateInstance(elemento, { container: "body", trigger: "hover focus" });
    });
}

function mostrarAvisoClientes(titulo, mensaje, tipo = "info", alOcultar = null) {
    const toastElement = document.getElementById("avisoClientes");
    if (!toastElement) return;
    const iconos = { success: "check-circle-fill", danger: "exclamation-triangle-fill", warning: "exclamation-circle-fill", info: "info-circle-fill" };
    toastElement.dataset.tipo = tipo;
    document.getElementById("avisoClientesTitulo").textContent = titulo;
    document.getElementById("avisoClientesTexto").textContent = mensaje;
    toastElement.querySelector(".cliente-toast-icono i").className = `bi bi-${iconos[tipo] || iconos.info}`;
    if (alOcultar) toastElement.addEventListener("hidden.bs.toast", alOcultar, { once: true });
    bootstrap.Toast.getOrCreateInstance(toastElement, { delay: 3500 }).show();
}

async function mensajeErrorAccion(respuesta) {
    const mensajes = {
        403: "No tienes permiso para realizar esta acción.",
        404: "El cliente ya no existe.",
        409: "La acción entra en conflicto con el estado actual del cliente.",
        500: "Ocurrió un error interno al actualizar el cliente."
    };
    try {
        const contenido = await respuesta.json();
        return contenido.error || contenido.message || mensajes[respuesta.status] || "No se pudo actualizar el cliente.";
    } catch (_) {
        return mensajes[respuesta.status] || "No se pudo actualizar el cliente.";
    }
}

async function ejecutarAccionCliente(boton, url, mensajeExito) {
    if (boton.dataset.ejecutando === "true") return;
    const contenidoOriginal = boton.innerHTML;
    const etiquetaOriginal = boton.getAttribute("aria-label");
    boton.dataset.ejecutando = "true";
    boton.disabled = true;
    boton.setAttribute("aria-busy", "true");
    boton.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span>';
    bootstrap.Tooltip.getInstance(boton)?.hide();
    try {
        const respuesta = await fetch(url, { method: "POST", headers: csrfHeaders() });
        if (!respuesta.ok) throw new Error(await mensajeErrorAccion(respuesta));
        try {
            await cargarListadoClientes(estadoListadoClientes.pagina);
        } catch (_) {
            mostrarAvisoClientes("Acción completada", `${mensajeExito} Se recargará el listado para reflejar el cambio.`, "warning");
            setTimeout(() => location.reload(), 900);
            return;
        }
        mostrarAvisoClientes("Cliente actualizado", mensajeExito, "success");
    } catch (error) {
        boton.disabled = false;
        boton.dataset.ejecutando = "false";
        boton.removeAttribute("aria-busy");
        boton.innerHTML = contenidoOriginal;
        if (etiquetaOriginal) boton.setAttribute("aria-label", etiquetaOriginal);
        mostrarAvisoClientes("No se completó la acción", error.message, "danger");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    sincronizarEstadoListadoClientes();
    inicializarTooltipsClientes();
});
/* ============================
   BÚSQUEDA EN VIVO
============================ */
const inputBuscar = document.getElementById("buscarCliente");

if (inputBuscar) {
    inputBuscar.addEventListener("input", () => {
        estadoListadoClientes.buscar = inputBuscar.value.trim();
        clearTimeout(temporizadorBusquedaClientes);
        temporizadorBusquedaClientes = setTimeout(() => cargarListadoClientes(0).catch(error => {
            mostrarAvisoClientes("No se actualizó la búsqueda", error.message, "danger");
        }), 300);
    });
}

document.getElementById("tamanoPaginaClientes")?.addEventListener("change", event => {
    estadoListadoClientes.tamano = Number(event.target.value || 10);
    cargarListadoClientes(0).catch(error => mostrarAvisoClientes("No se actualizó el listado", error.message, "danger"));
});

document.addEventListener("click", event => {
    const botonPagina = event.target instanceof Element ? event.target.closest("#paginacionClientes button[data-pagina]") : null;
    if (!botonPagina || botonPagina.closest(".disabled")) return;
    cargarListadoClientes(Number(botonPagina.dataset.pagina)).catch(error => mostrarAvisoClientes("No se cambió de página", error.message, "danger"));
});


/* ============================
   FILTROS AVANZADOS
============================ */
function filtrar(tipo) {
    estadoListadoClientes.filtro = String(tipo || "TODOS").toUpperCase();
    cargarListadoClientes(0)
        .then(() => bootstrap.Modal.getInstance(document.getElementById("modalFiltros"))?.hide())
        .catch(error => mostrarAvisoClientes("No se aplicó el filtro", error.message, "danger"));
}


/* ============================
   VIP
============================ */
function marcarVip(btn) {
    let id = btn.getAttribute("data-id");
    ejecutarAccionCliente(btn, `/interno/clientes/vip/${id}`, "El cliente fue marcado como VIP.");
}

function quitarVip(btn) {
    let id = btn.getAttribute("data-id");

    ejecutarAccionCliente(btn, `/interno/clientes/vip/quitar/${id}`, "Se retiró la condición VIP.");
}


/* ============================
   MOROSO
============================ */
function marcarMoroso(btn) {
    let id = btn.getAttribute("data-id");

    ejecutarAccionCliente(btn, `/interno/clientes/moroso/${id}`, "El cliente fue marcado como moroso.");
}

function quitarMoroso(btn) {
    let id = btn.getAttribute("data-id");

    ejecutarAccionCliente(btn, `/interno/clientes/moroso/quitar/${id}`, "Se retiró la condición de morosidad.");
}


/* ============================
   ACTIVAR / DESACTIVAR CLIENTE
============================ */
function activarCliente(btn) {
    let id = btn.getAttribute("data-id");

    ejecutarAccionCliente(btn, `/interno/clientes/activar/${id}`, "El cliente fue activado.");
}

function desactivarCliente(btn) {
    let id = btn.getAttribute("data-id");

    ejecutarAccionCliente(btn, `/interno/clientes/desactivar/${id}`, "El cliente fue desactivado; sus ventas y comprobantes se conservaron.");
}


/* ============================
   PERFIL DEL CLIENTE
============================ */
let modalPerfil = document.getElementById("modalPerfil");

if (modalPerfil) {
    modalPerfil.addEventListener("show.bs.modal", function (event) {
        const id = event.relatedTarget?.getAttribute("data-id");
        const contenedor = document.getElementById("perfilContenido");
        contenedor.innerHTML = '<div class="text-center py-5"><span class="spinner-border text-primary" aria-hidden="true"></span><p class="text-muted mt-2">Cargando detalle...</p></div>';
        fetch(`/interno/clientes/api/${id}/detalle`, { headers: { Accept: "application/json" } })
            .then(async respuesta => {
                if (!respuesta.ok) throw new Error(respuesta.status === 403 ? "No tienes permiso para consultar este cliente." : respuesta.status === 404 ? "El cliente no existe." : "No se pudo cargar el detalle.");
                return respuesta.json();
            })
            .then(renderizarDetalleCliente)
            .catch(error => {
                contenedor.innerHTML = `<div class="alert alert-danger mb-0">${escaparHtml(error.message)}</div>`;
            });
    });
}

function renderizarDetalleCliente(cliente) {
    const contenedor = document.getElementById("perfilContenido");
    const foto = cliente.foto
        ? (String(cliente.foto).startsWith("http") ? cliente.foto : `/uploads/clientes/${encodeURIComponent(cliente.foto)}`)
        : null;
    contenedor.innerHTML = `
        <section class="cliente-detalle">
            <header class="cliente-detalle-header">
                ${foto ? `<img src="${escaparHtml(foto)}" alt="Foto de ${escaparHtml(cliente.nombre)}">` : '<div class="cliente-detalle-avatar"><i class="bi bi-person"></i></div>'}
                <div><h4>${escaparHtml(cliente.nombre)}</h4><p>${escaparHtml(texto(cliente.tipoDocumento))}: ${escaparHtml(texto(cliente.documento))}</p></div>
                <div class="cliente-detalle-badges">
                    <span class="badge text-bg-${cliente.activo ? "success" : "secondary"}">${cliente.activo ? "Activo" : "Inactivo"}</span>
                    <span class="badge cliente-tipo-${String(cliente.clasificacion).toLowerCase().replace(" ", "-")}">${escaparHtml(cliente.clasificacion)}</span>
                    ${cliente.vip ? '<span class="badge text-bg-warning">VIP</span>' : ''}
                    ${cliente.moroso ? '<span class="badge text-bg-danger">Moroso</span>' : ''}
                </div>
            </header>
            <div class="cliente-detalle-grid">
                ${datoDetalle("ID", cliente.id)}${datoDetalle("Teléfono", cliente.telefono)}${datoDetalle("Correo", cliente.correo)}
                ${datoDetalle("Dirección", cliente.direccion, true)}${datoDetalle("Fecha de registro", formatearFecha(cliente.fechaRegistro))}
            </div>
            <h6 class="cliente-detalle-subtitulo">Resumen comercial</h6>
            <div class="cliente-comercial-grid">
                ${datoComercial("Compras válidas", cliente.cantidadCompras)}${datoComercial("Total histórico", formatearMonto(cliente.totalHistorico))}
                ${datoComercial("Ticket promedio", formatearMonto(cliente.ticketPromedio))}${datoComercial("Primera compra", formatearFecha(cliente.primeraCompra))}
                ${datoComercial("Última compra", formatearFecha(cliente.ultimaCompra))}${datoComercial("Sede principal", texto(cliente.sedePrincipal))}
                ${datoComercial("Canal principal", texto(cliente.canalPrincipal))}${datoComercial("Pago más usado", texto(cliente.metodoPagoPrincipal))}
            </div>
        </section>`;
}

function datoDetalle(etiqueta, valor, ancho = false) {
    return `<div class="cliente-detalle-dato${ancho ? " cliente-detalle-dato-ancho" : ""}"><small>${escaparHtml(etiqueta)}</small><span>${escaparHtml(texto(valor))}</span></div>`;
}

function datoComercial(etiqueta, valor) {
    return `<div><small>${escaparHtml(etiqueta)}</small><strong>${escaparHtml(texto(valor))}</strong></div>`;
}


/* ============================
   OBSERVACIONES ADMINISTRATIVAS
============================ */
let modalNotas = document.getElementById("modalNotas");

if (modalNotas) {
    modalNotas.addEventListener("show.bs.modal", function (event) {
        const id = event.relatedTarget?.getAttribute("data-id");
        modalNotas.setAttribute("data-id", id || "");
        document.getElementById("notasClienteNombre").textContent = event.relatedTarget?.getAttribute("data-cliente") || "Cliente";
        document.getElementById("nuevaNota").value = "";
        actualizarContadorNota();
        cargarObservacionesCliente(id);
    });
}

async function cargarObservacionesCliente(id) {
    const contenedor = document.getElementById("listaNotas");
    contenedor.innerHTML = '<div class="cliente-notas-estado"><span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Cargando observaciones...</div>';
    try {
        const respuesta = await fetch(`/interno/clientes/notas/${id}`, { headers: { Accept: "application/json" } });
        if (!respuesta.ok) throw new Error(respuesta.status === 403 ? "No tienes permiso para consultar estas observaciones." : "No se pudieron cargar las observaciones.");
        const lista = await respuesta.json();
        contenedor.innerHTML = lista.length
            ? lista.map(nota => `<article class="cliente-nota"><i class="bi bi-chat-left-text" aria-hidden="true"></i><p>${escaparHtml(nota)}</p></article>`).join("")
            : '<div class="cliente-notas-estado"><i class="bi bi-journal" aria-hidden="true"></i><span>Aún no hay observaciones administrativas.</span></div>';
    } catch (error) {
        contenedor.innerHTML = `<div class="alert alert-danger mb-0">${escaparHtml(error.message)}</div>`;
    }
}

function actualizarContadorNota() {
    const campo = document.getElementById("nuevaNota");
    const contador = document.getElementById("contadorNota");
    if (campo && contador) contador.textContent = `${campo.value.length} / 1000`;
}

document.getElementById("nuevaNota")?.addEventListener("input", actualizarContadorNota);

async function guardarNota() {
    const id = modalNotas.getAttribute("data-id");
    const campo = document.getElementById("nuevaNota");
    const boton = document.getElementById("btnGuardarNota");
    const nota = campo.value.trim();

    if (nota.length === 0) {
        campo.focus();
        mostrarAvisoClientes("Observación requerida", "Escribe una observación antes de guardarla.", "warning");
        return;
    }

    boton.disabled = true;
    try {
        const respuesta = await fetch(`/interno/clientes/notas/agregar/${id}`, {
            method: "POST",
            body: nota,
            headers: { "Content-Type": "text/plain", ...csrfHeaders() }
        });
        if (!respuesta.ok) throw new Error(respuesta.status === 403 ? "No tienes permiso para guardar observaciones." : "No se pudo guardar la observación.");
        campo.value = "";
        actualizarContadorNota();
        await cargarObservacionesCliente(id);
        mostrarAvisoClientes("Observación guardada", "La observación administrativa se registró correctamente.", "success");
    } catch (error) {
        mostrarAvisoClientes("No se guardó la observación", error.message, "danger");
    } finally {
        boton.disabled = false;
    }
}


/* ============================
   HISTORIAL
============================ */
let modalHistorial = document.getElementById("modalHistorial");
let clienteHistorialId = null;
let paginaHistorial = 0;

if (modalHistorial) {
    modalHistorial.addEventListener("show.bs.modal", function (event) {
        clienteHistorialId = event.relatedTarget?.getAttribute("data-id");
        paginaHistorial = 0;
        document.getElementById("historialClienteNombre").textContent = event.relatedTarget?.getAttribute("data-cliente") || "Cliente";
        document.getElementById("filtroHistorialClientes")?.reset();
        actualizarEstadoFiltrosFechaHistorial();
        cargarHistorialCompras();
    });

    document.getElementById("filtroHistorialClientes")?.addEventListener("submit", event => {
        event.preventDefault();
        paginaHistorial = 0;
        cargarHistorialCompras();
    });

    modalHistorial.addEventListener("hide.bs.modal", () => {
        if (modalHistorial.contains(document.activeElement)) document.activeElement.blur();
    });
}

const formularioHistorial = document.getElementById("filtroHistorialClientes");
const fechaExactaHistorial = document.getElementById("historialFechaExacta");

function formatearFechaExactaVisual(valor) {
    if (!valor) return "Selecciona un día";
    const [anio, mes, dia] = valor.split("-");
    return `${dia}/${mes}/${anio}`;
}

function actualizarEstadoFiltrosFechaHistorial() {
    if (!formularioHistorial) return;
    const fechaExacta = formularioHistorial.elements.fecha;
    const controlesAlternativos = [formularioHistorial.elements.mes, formularioHistorial.elements.anio,
        formularioHistorial.elements.desde, formularioHistorial.elements.hasta];
    const hayFechaExacta = Boolean(fechaExacta?.value);
    controlesAlternativos.forEach(control => { if (control) control.disabled = hayFechaExacta; });
    document.getElementById("historialFechaExactaTexto").textContent = formatearFechaExactaVisual(fechaExacta?.value);
}

fechaExactaHistorial?.addEventListener("change", () => {
    if (fechaExactaHistorial.value) {
        ["mes", "anio", "desde", "hasta"].forEach(nombre => { if (formularioHistorial.elements[nombre]) formularioHistorial.elements[nombre].value = ""; });
    }
    actualizarEstadoFiltrosFechaHistorial();
    paginaHistorial = 0;
});

["mes", "anio", "desde", "hasta"].forEach(nombre => {
    formularioHistorial?.elements[nombre]?.addEventListener("change", event => {
        if (event.target.value && fechaExactaHistorial) fechaExactaHistorial.value = "";
        actualizarEstadoFiltrosFechaHistorial();
        paginaHistorial = 0;
    });
});

document.getElementById("limpiarFechasHistorial")?.addEventListener("click", () => {
    ["fecha", "mes", "anio", "desde", "hasta"].forEach(nombre => { if (formularioHistorial.elements[nombre]) formularioHistorial.elements[nombre].value = ""; });
    actualizarEstadoFiltrosFechaHistorial();
    paginaHistorial = 0;
    cargarHistorialCompras(0);
});

async function cargarHistorialCompras(pagina = paginaHistorial) {
    if (!clienteHistorialId) return;
    paginaHistorial = pagina;
    const contenedor = document.getElementById("historialContenido");
    contenedor.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm"></span> Cargando...</div>';
    const params = new URLSearchParams(new FormData(document.getElementById("filtroHistorialClientes")));
    [...params.entries()].forEach(([clave, valor]) => { if (!valor) params.delete(clave); });
    params.set("pagina", String(pagina));
    params.set("tamano", "10");
    try {
        const respuesta = await fetch(`/interno/clientes/api/${clienteHistorialId}/compras?${params}`, { headers: { Accept: "application/json" } });
        if (!respuesta.ok) throw new Error(respuesta.status === 403 ? "No tienes permiso para consultar el historial." : respuesta.status === 404 ? "El cliente no existe." : "No se pudo cargar el historial.");
        const datos = await respuesta.json();
        renderizarResumenHistorial(datos);
        contenedor.innerHTML = datos.compras.length ? datos.compras.map(renderizarCompraCliente).join("") : '<div class="cliente-vacio"><i class="bi bi-receipt"></i><span>No hay compras válidas para estos filtros.</span></div>';
        renderizarPaginacionHistorial(datos);
        modalHistorial.querySelector(".cliente-historial-modal-body")?.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        contenedor.innerHTML = `<div class="alert alert-danger">${escaparHtml(error.message)}</div>`;
        document.getElementById("historialRangoPagina").textContent = "No se pudo cargar la página";
        document.getElementById("historialPaginaActual").textContent = "";
        document.getElementById("historialPaginacion").innerHTML = "";
    }
}

function renderizarResumenHistorial(datos) {
    document.getElementById("historialTotalCompras").textContent = datos.comprasFiltradas;
    document.getElementById("historialTotalMonto").textContent = formatearMonto(datos.totalFiltrado);
    document.getElementById("historialTicketPromedio").textContent = formatearMonto(datos.ticketPromedioFiltrado);
    document.getElementById("historialUltimaCompra").textContent = formatearFecha(datos.ultimaCompra);
    document.getElementById("historialCanales").textContent = datos.canalesUtilizados;
    document.getElementById("historialSedes").textContent = datos.sedesUtilizadas;
}

function renderizarCompraCliente(compra) {
    const comprobante = compra.comprobanteDisponible
        ? `<a class="btn btn-outline-danger btn-sm" href="${escaparHtml(compra.comprobanteUrl)}" target="_blank" rel="noopener"><i class="bi bi-file-earmark-pdf"></i> Comprobante</a>`
        : '<span class="text-muted small">Sin comprobante disponible</span>';
    return `<article class="cliente-compra-card">
        <header><div><small>Comprobante</small><strong>${escaparHtml(texto(compra.numeroComprobante || compra.codigo))}</strong></div><div class="d-flex flex-wrap gap-1"><span class="badge text-bg-primary">${escaparHtml(texto(compra.canal))}</span><span class="badge text-bg-success">${escaparHtml(texto(compra.estadoVenta))}</span></div></header>
        <div class="cliente-compra-grid">${datoCompra("Fecha", formatearFecha(compra.fecha))}${datoCompra("Sede", compra.sede)}${datoCompra("Vendedor", compra.vendedor)}${datoCompra("Método de pago", compra.metodoPago)}${datoCompra("Entrega", compra.estadoEntrega)}${datoCompra("Total", formatearMonto(compra.total), true)}</div>
        <footer><a class="btn btn-outline-primary btn-sm" href="${escaparHtml(compra.detalleUrl)}" target="_blank" rel="noopener"><i class="bi bi-eye"></i> Ver venta</a>${comprobante}</footer>
    </article>`;
}

function datoCompra(etiqueta, valor, destacado = false) {
    return `<div${destacado ? ' class="cliente-compra-total"' : ""}><small>${escaparHtml(etiqueta)}</small><strong>${escaparHtml(texto(valor))}</strong></div>`;
}

function renderizarPaginacionHistorial(datos) {
    const lista = document.getElementById("historialPaginacion");
    const desde = datos.totalElementos === 0 ? 0 : datos.pagina * datos.tamano + 1;
    const hasta = Math.min(datos.totalElementos, (datos.pagina + 1) * datos.tamano);
    document.getElementById("historialRangoPagina").textContent = `Mostrando ${desde} a ${hasta} de ${datos.totalElementos} compras`;
    document.getElementById("historialPaginaActual").textContent = datos.totalPaginas === 0 ? "Página 0 de 0" : `Página ${datos.pagina + 1} de ${datos.totalPaginas}`;
    if (datos.totalPaginas <= 1) { lista.innerHTML = ""; return; }
    const botones = [];
    botones.push(`<li class="page-item ${datos.pagina === 0 ? "disabled" : ""}"><button class="page-link" type="button" data-pagina="${datos.pagina - 1}">Anterior</button></li>`);
    const inicio = Math.max(0, datos.pagina - 2);
    const fin = Math.min(datos.totalPaginas - 1, datos.pagina + 2);
    for (let i = inicio; i <= fin; i++) botones.push(`<li class="page-item ${i === datos.pagina ? "active" : ""}"><button class="page-link" type="button" data-pagina="${i}" aria-label="Página ${i + 1}">${i + 1}</button></li>`);
    botones.push(`<li class="page-item ${datos.pagina + 1 >= datos.totalPaginas ? "disabled" : ""}"><button class="page-link" type="button" data-pagina="${datos.pagina + 1}">Siguiente</button></li>`);
    lista.innerHTML = botones.join("");
    lista.querySelectorAll("button[data-pagina]").forEach(boton => boton.addEventListener("click", () => {
        if (!boton.closest(".disabled")) cargarHistorialCompras(Number(boton.dataset.pagina));
    }));
}

const modalRanking = document.getElementById("modalRanking");
if (modalRanking) {
    modalRanking.addEventListener("show.bs.modal", () => cargarRankingClientes());
    document.getElementById("filtroRankingClientes")?.addEventListener("submit", event => {
        event.preventDefault();
        cargarRankingClientes();
    });
}

async function cargarRankingClientes() {
    const contenedor = document.getElementById("rankingClientesContenido");
    const params = new URLSearchParams(new FormData(document.getElementById("filtroRankingClientes")));
    [...params.entries()].forEach(([clave, valor]) => { if (!valor) params.delete(clave); });
    contenedor.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm"></span> Calculando ranking...</div>';
    try {
        const respuesta = await fetch(`/interno/clientes/api/ranking?${params}`, { headers: { Accept: "application/json" } });
        if (!respuesta.ok) throw new Error(respuesta.status === 403 ? "No tienes permiso para consultar el ranking." : "No se pudo calcular el ranking.");
        const ranking = await respuesta.json();
        contenedor.innerHTML = ranking.length ? ranking.map(item => `
            <article class="cliente-ranking-card">
                <div class="cliente-ranking-posicion">${item.posicion}</div>
                <div class="cliente-ranking-identidad"><strong>${escaparHtml(texto(item.cliente))}</strong><span>${escaparHtml(texto(item.documento))}</span><span class="badge cliente-tipo-${String(item.clasificacion).toLowerCase().replace(" ", "-")}">${escaparHtml(item.clasificacion)}</span></div>
                <div><small>Compras</small><strong>${item.cantidadCompras}</strong></div>
                <div><small>Total</small><strong>${escaparHtml(formatearMonto(item.totalComprado))}</strong></div>
                <div><small>Ticket promedio</small><strong>${escaparHtml(formatearMonto(item.ticketPromedio))}</strong></div>
                <div><small>Última compra</small><strong>${escaparHtml(formatearFecha(item.ultimaCompra))}</strong></div>
                <div><small>Sede principal</small><strong>${escaparHtml(texto(item.sedePrincipal))}</strong></div>
                <div><small>Canal principal</small><strong>${escaparHtml(texto(item.canalPrincipal))}</strong></div>
            </article>`).join("") : '<div class="cliente-vacio"><i class="bi bi-trophy"></i><span>No hay compras válidas para estos filtros.</span></div>';
    } catch (error) {
        contenedor.innerHTML = `<div class="alert alert-danger">${escaparHtml(error.message)}</div>`;
    }
}

/* ============================
   IMPORTAR CLIENTES
============================ */
function importarClientes() {
    let file = document.getElementById("archivoImportar").files[0];
    const botonImportar = document.getElementById("btnImportarClientes");

    if (!file) {
        mostrarAvisoClientes("Archivo requerido", "Seleccione un archivo antes de importar.", "warning");
        return;
    }

    if (!/\.(csv|xlsx)$/i.test(file.name)) {
        mostrarAvisoClientes("Formato no válido", "Seleccione un archivo CSV o XLSX válido.", "warning");
        return;
    }

    if (file.size > 5 * 1024 * 1024) {
        mostrarAvisoClientes("Archivo demasiado grande", "El archivo no puede superar 5 MB.", "warning");
        return;
    }

    let formData = new FormData();
    formData.append("archivo", file);  // <--- NOMBRE CORRECTO
    botonImportar.disabled = true;

    fetch("/interno/clientes/importar", {
        method: "POST",
        headers: csrfHeaders(),
        body: formData
    })
        .then(r => r.text())
        .then(resp => {
            if (!resp.toUpperCase().startsWith("ERROR") && !resp.toUpperCase().startsWith("FORMATO")) {
                mostrarAvisoClientes("Importación completada", resp, "success", () => location.reload());
            } else {
                botonImportar.disabled = false;
                mostrarAvisoClientes("No se completó la importación", resp, "danger");
            }
        })
        .catch(() => {
            botonImportar.disabled = false;
            mostrarAvisoClientes("No se completó la importación", "No fue posible comunicarse con el servidor.", "danger");
        });
}


/* ============================
   EXPORTAR CLIENTES
============================ */
function exportar(tipo) {
    window.location.href = `/interno/clientes/exportar/${tipo}`;
}


/* ============================
   CAMBIO DE VISTA
============================ */
function vistaTabla() {
    estadoListadoClientes.vista = "tabla";
    cargarListadoClientes(0)
        .then(() => bootstrap.Modal.getInstance(document.getElementById("modalVista"))?.hide())
        .catch(error => mostrarAvisoClientes("No se cambió la vista", error.message, "danger"));
}

function vistaCards() {
    estadoListadoClientes.vista = "cards";
    cargarListadoClientes(0)
        .then(() => bootstrap.Modal.getInstance(document.getElementById("modalVista"))?.hide())
        .catch(error => mostrarAvisoClientes("No se cambió la vista", error.message, "danger"));
}


/* ============================
   MODAL ELIMINAR PRO
============================ */
/* ============================
    NUEVO CLIENTE
============================ */
function abrirModalNuevoCliente() {

    document.getElementById("tituloModalCliente").innerText = "Nuevo Cliente";

    fetch("/interno/clientes/nuevo?fragmento=true")
        .then(r => r.text())
        .then(html => {
            document.getElementById("contenidoFormCliente").innerHTML = html;
            prepararRetornoFormularioCliente();
            bootstrap.Modal.getOrCreateInstance(document.getElementById("modalFormCliente")).show();
        });
}


/* ============================
    EDITAR CLIENTE
============================ */
function abrirModalEditarCliente(id) {

    document.getElementById("tituloModalCliente").innerText = "Editar Cliente";

    fetch(`/interno/clientes/editar/${id}?fragmento=true`)
        .then(r => r.text())
        .then(html => {
            document.getElementById("contenidoFormCliente").innerHTML = html;
            prepararRetornoFormularioCliente();
            bootstrap.Modal.getOrCreateInstance(document.getElementById("modalFormCliente")).show();
        });
}

function prepararRetornoFormularioCliente() {
    const valores = {
        clientePaginaRetorno: estadoListadoClientes.pagina,
        clienteTamanoRetorno: estadoListadoClientes.tamano,
        clienteBuscarRetorno: estadoListadoClientes.buscar,
        clienteFiltroRetorno: estadoListadoClientes.filtro,
        clienteVistaRetorno: estadoListadoClientes.vista
    };
    Object.entries(valores).forEach(([id, valor]) => {
        const campo = document.getElementById(id);
        if (campo) campo.value = String(valor ?? "");
    });
}

document.addEventListener("change", function (e) {
    if (e.target.id === "fotoFile") {
        let file = e.target.files[0];
        if (!file) return;

        let reader = new FileReader();
       reader.onload = ev => {

           const img = document.getElementById("foto-preview");
           const placeholder = document.getElementById("foto-placeholder");

           if (img) {
               img.src = ev.target.result;
               img.classList.remove("d-none");
               img.style.display = "inline-block";
           }

           if (placeholder) {
               placeholder.classList.add("d-none");
               placeholder.style.display = "none";
           }
       };
        reader.readAsDataURL(file);
    }
});
// ============================
//  API DNI CLIENTE (miapi.cloud)
// ============================
document.addEventListener("click", async function (e) {

    // Botón dentro del formulario del modal
    const botonBuscarDni = e.target instanceof Element ? e.target.closest("#btnBuscarDniCliente") : null;
    if (botonBuscarDni) {

        const dniInput = document.getElementById("dniCliente");
        if (!dniInput) return;

        const dni = dniInput.value.trim();
        if (!dni) {
            mostrarAvisoClientes("DNI requerido", "Ingrese un DNI antes de realizar la búsqueda.", "warning");
            dniInput.focus();
            return;
        }

        try {
            // 👈 URL CORRECTA SEGÚN TU CONTROLADOR
            const resp = await fetch(`/api/externo/dni/${dni}`);
            const json = await resp.json();

            if (json.error) {
                mostrarAvisoClientes("Consulta de DNI", json.error || "No se encontraron datos para ese DNI.", "warning");
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
            mostrarAvisoClientes("Consulta no disponible", "No se pudo consultar la información del DNI.", "danger");
        }
    }
});

