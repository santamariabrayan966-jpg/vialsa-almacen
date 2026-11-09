document.addEventListener("DOMContentLoaded", () => {
    activarTabDesdeUrl();
    inicializarSeleccionTraslado();
    inicializarBuscadorHistorialTraslados();
    inicializarModalEvidenciaTraslado();
    inicializarModalIncidenciaTraslado();
    inicializarModalDetalleHistorialTraslado();
    inicializarAvisosCambiosTraslado();
});

function activarTabDesdeUrl() {
    const params = new URLSearchParams(window.location.search);
    const tab = params.get("tab");

    if (!tab) {
        return;
    }

    const mapaTabs = {
        pendientes: "#pendientes",
        proceso: "#proceso",
        recepcion: "#recepcion",
        historial: "#historial"
    };

    const destino = mapaTabs[tab];

    if (!destino) {
        return;
    }

    const boton = document.querySelector(`[data-bs-target="${destino}"]`);

    if (!boton) {
        return;
    }

    const instancia = new bootstrap.Tab(boton);
    instancia.show();

    setTimeout(() => {
        boton.scrollIntoView({
            behavior: "smooth",
            block: "center"
        });
    }, 150);
}
function inicializarSeleccionTraslado() {
    const items = document.querySelectorAll(".traslado-item");

    const codigo = document.getElementById("detalleCodigo");
    const origen = document.getElementById("detalleOrigen");
    const destino = document.getElementById("detalleDestino");
    const productos = document.getElementById("detalleProductos");
    const total = document.getElementById("detalleTotal");

    if (!items.length || !codigo || !origen || !destino || !productos || !total) {
        return;
    }

    items.forEach(item => {
        item.addEventListener("click", () => {
            items.forEach(i => i.classList.remove("active"));
            item.classList.add("active");

            codigo.textContent = item.dataset.codigo || "-";
            origen.textContent = item.dataset.origen || "-";
            destino.textContent = item.dataset.destino || "-";
            productos.textContent = item.dataset.productos || "0";
            total.textContent = item.dataset.total || "0";
        });
    });
}

function inicializarBuscadorHistorialTraslados() {
    const input = document.getElementById("buscadorHistorialTraslados");
    const filas = Array.from(document.querySelectorAll(".fila-historial-traslado"));

    const texto = document.getElementById("textoPaginacionHistorialTraslados");
    const indicador = document.getElementById("indicadorPaginaHistorialTraslados");
    const btnAnterior = document.getElementById("btnAnteriorHistorialTraslados");
    const btnSiguiente = document.getElementById("btnSiguienteHistorialTraslados");

    const filasPorPagina = 5;
    let paginaActual = 1;
    let filasFiltradas = [];

    if (!input || !filas.length || !texto || !indicador || !btnAnterior || !btnSiguiente) {
        return;
    }

    function aplicarFiltroHistorial() {
        const textoBuscado = normalizarTextoTraslado(input.value);

        filasFiltradas = filas.filter(fila => {
            const dataBusqueda = normalizarTextoTraslado(
                fila.dataset.busqueda || fila.textContent || ""
            );

            const palabras = textoBuscado
                .split(" ")
                .map(p => p.trim())
                .filter(p => p !== "");

            return palabras.length === 0
                || palabras.every(palabra => dataBusqueda.includes(palabra));
        });

        paginaActual = 1;
        pintarPaginaHistorial();
    }

    function pintarPaginaHistorial() {
        const total = filasFiltradas.length;
        const totalPaginas = Math.max(1, Math.ceil(total / filasPorPagina));

        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas;
        }

        const inicio = (paginaActual - 1) * filasPorPagina;
        const fin = inicio + filasPorPagina;

        filas.forEach(fila => {
            fila.classList.add("d-none");
        });

        filasFiltradas.slice(inicio, fin).forEach(fila => {
            fila.classList.remove("d-none");
        });

        const desde = total === 0 ? 0 : inicio + 1;
        const hasta = Math.min(fin, total);

        texto.textContent = total === 0
            ? "No hay traslados para la búsqueda ingresada."
            : `Mostrando ${desde} - ${hasta} de ${total} traslado(s)`;

        indicador.textContent = `Página ${paginaActual} de ${totalPaginas}`;

        btnAnterior.disabled = paginaActual <= 1;
        btnSiguiente.disabled = paginaActual >= totalPaginas;
    }

    input.addEventListener("input", aplicarFiltroHistorial);

    btnAnterior.addEventListener("click", () => {
        if (paginaActual > 1) {
            paginaActual--;
            pintarPaginaHistorial();
        }
    });

    btnSiguiente.addEventListener("click", () => {
        const totalPaginas = Math.max(1, Math.ceil(filasFiltradas.length / filasPorPagina));

        if (paginaActual < totalPaginas) {
            paginaActual++;
            pintarPaginaHistorial();
        }
    });

    aplicarFiltroHistorial();
}

function normalizarTextoTraslado(valor) {
    return String(valor || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}

function inicializarModalEvidenciaTraslado() {
    const modal = document.getElementById("modalEvidenciaTraslado");
    const form = document.getElementById("formEvidenciaTraslado");

    const titulo = document.getElementById("tituloModalEvidencia");
    const codigo = document.getElementById("codigoModalEvidencia");
    const descripcion = document.getElementById("descripcionModalEvidencia");

    const inputFoto = document.getElementById("fotoEvidenciaTraslado");
    const preview = document.getElementById("previewEvidenciaTraslado");
    const imgPreview = document.getElementById("imgPreviewEvidenciaTraslado");

    if (!modal || !form || !inputFoto) {
        return;
    }

    modal.addEventListener("show.bs.modal", function (event) {
        const boton = event.relatedTarget;

        if (!boton) {
            return;
        }

        const idTransferencia = boton.getAttribute("data-id");
        const accion = boton.getAttribute("data-accion");

        const codigoTransferencia = boton.getAttribute("data-codigo") || "TRF-00000";
        const tituloModal = boton.getAttribute("data-titulo") || "Evidencia del traslado";
        const descripcionModal = boton.getAttribute("data-descripcion") || "Sube una foto como evidencia antes de continuar.";

        if (!idTransferencia || !accion) {
            alert("No se pudo identificar el traslado. Cierra el modal y vuelve a intentarlo.");
            return;
        }

        form.action = `/interno/traslados-internos/${idTransferencia}/${accion}`;

        if (titulo) {
            titulo.textContent = tituloModal;
        }

        if (codigo) {
            codigo.textContent = codigoTransferencia;
        }

        if (descripcion) {
            descripcion.textContent = descripcionModal;
        }

        inputFoto.value = "";

        if (preview) {
            preview.classList.add("d-none");
        }

        if (imgPreview) {
            imgPreview.src = "";
        }
    });

    inputFoto.addEventListener("change", function () {
        const archivo = inputFoto.files && inputFoto.files[0];

        if (!archivo) {
            if (preview) {
                preview.classList.add("d-none");
            }

            if (imgPreview) {
                imgPreview.src = "";
            }

            return;
        }

        if (!archivo.type.startsWith("image/")) {
            alert("Solo se permite subir imágenes.");
            inputFoto.value = "";

            if (preview) {
                preview.classList.add("d-none");
            }

            if (imgPreview) {
                imgPreview.src = "";
            }

            return;
        }

        const urlTemporal = URL.createObjectURL(archivo);

        if (imgPreview) {
            imgPreview.src = urlTemporal;
        }

        if (preview) {
            preview.classList.remove("d-none");
        }
    });

    form.addEventListener("submit", function (e) {
        if (!form.action || !form.action.includes("/interno/traslados-internos/")) {
            e.preventDefault();
            alert("No se pudo preparar la acción del formulario. Cierra el modal y vuelve a abrirlo.");
            return;
        }

        if (!inputFoto.files || inputFoto.files.length === 0) {
            e.preventDefault();
            alert("Debes subir una foto antes de continuar.");
        }
    });
}
function inicializarModalIncidenciaTraslado() {
    const modal = document.getElementById("modalIncidenciaTraslado");
    const form = document.getElementById("formIncidenciaTraslado");
    const codigo = document.getElementById("codigoModalIncidencia");
    const motivo = document.getElementById("motivoIncidenciaTraslado");
    const descripcion = document.getElementById("descripcionIncidenciaTraslado");
    const foto = document.getElementById("fotoIncidenciaTraslado");
    const preview = document.getElementById("previewIncidenciaTraslado");
    const imgPreview = document.getElementById("imgPreviewIncidenciaTraslado");

    if (!modal || !form) {
        return;
    }

    modal.addEventListener("show.bs.modal", function (event) {
        const boton = event.relatedTarget;

        if (!boton) {
            return;
        }

        const idTransferencia = boton.getAttribute("data-id");
        const codigoTransferencia = boton.getAttribute("data-codigo") || "TRF-00000";

        if (!idTransferencia) {
            alert("No se pudo identificar el traslado.");
            return;
        }

        form.action = `/interno/traslados-internos/${idTransferencia}/reportar-incidencia`;

        if (codigo) {
            codigo.textContent = codigoTransferencia;
        }

        if (motivo) {
            motivo.value = "";
        }

        if (descripcion) {
            descripcion.value = "";
        }

        if (foto) {
            foto.value = "";
        }

        if (preview) {
            preview.classList.add("d-none");
        }

        if (imgPreview) {
            imgPreview.src = "";
        }
    });

    if (foto) {
        foto.addEventListener("change", function () {
            const archivo = foto.files && foto.files[0];

            if (!archivo) {
                if (preview) {
                    preview.classList.add("d-none");
                }

                if (imgPreview) {
                    imgPreview.src = "";
                }

                return;
            }

            if (!archivo.type.startsWith("image/")) {
                alert("Solo se permite subir imágenes.");
                foto.value = "";

                if (preview) {
                    preview.classList.add("d-none");
                }

                if (imgPreview) {
                    imgPreview.src = "";
                }

                return;
            }

            const urlTemporal = URL.createObjectURL(archivo);

            if (imgPreview) {
                imgPreview.src = urlTemporal;
            }

            if (preview) {
                preview.classList.remove("d-none");
            }
        });
    }

    form.addEventListener("submit", function (e) {
        if (!form.action || !form.action.includes("/reportar-incidencia")) {
            e.preventDefault();
            alert("No se pudo preparar el reporte de imprevisto.");
            return;
        }

        if (!motivo || !motivo.value || motivo.value.trim() === "") {
            e.preventDefault();
            alert("Debe seleccionar un motivo.");
            return;
        }

        if (!descripcion || !descripcion.value || descripcion.value.trim() === "") {
            e.preventDefault();
            alert("Debe ingresar una explicación.");
            return;
        }

        if (!foto || !foto.files || foto.files.length === 0) {
            e.preventDefault();
            alert("Debe subir una foto de evidencia.");
        }
    });
}

function inicializarModalDetalleHistorialTraslado() {
    const modal = document.getElementById("modalDetalleHistorialTraslado");
    const codigoSpan = document.getElementById("historialDetalleCodigo");
    const contenedor = document.getElementById("historialDetalleContenido");

    if (!modal || !codigoSpan || !contenedor) {
        return;
    }

    document.addEventListener("click", async function (e) {
        const btn = e.target.closest(".btnVerDetalleHistorialTraslado");

        if (!btn) {
            return;
        }

        const id = btn.dataset.id;
        const codigo = btn.dataset.codigo || "TRF-00000";

        codigoSpan.textContent = codigo;

        contenedor.innerHTML = `
            <div class="text-center text-muted py-5">
                <div class="spinner-border spinner-border-sm me-2"></div>
                Cargando detalle...
            </div>
        `;

        try {
            const response = await fetch(`/interno/inventario/transferencias/detalle/${id}`);

            if (!response.ok) {
                throw new Error("No se pudo cargar el detalle del traslado.");
            }

            const t = await response.json();

            contenedor.innerHTML = construirDetalleHistorialTraslado(t);

        } catch (error) {
            contenedor.innerHTML = `
                <div class="alert alert-danger rounded-4 mb-0">
                    ${escapeHtmlTraslado(error.message || "Error al cargar detalle.")}
                </div>
            `;
        }
    });
}

function construirDetalleHistorialTraslado(t) {
    return `
        <div class="row g-3 mb-4">

            <div class="col-md-3">
                <div class="border rounded-4 p-3 bg-light h-100">
                    <small class="text-muted">Estado final</small>
                    <div class="fw-bold">${escapeHtmlTraslado(formatearEstadoTraslado(t.estado))}</div>
                </div>
            </div>

            <div class="col-md-3">
                <div class="border rounded-4 p-3 bg-light h-100">
                    <small class="text-muted">Transportista</small>
                    <div class="fw-bold">${escapeHtmlTraslado(t.nombreTransportista || "Sin asignar")}</div>
                </div>
            </div>

            <div class="col-md-3">
                <div class="border rounded-4 p-3 bg-light h-100">
                    <small class="text-muted">Origen</small>
                    <div class="fw-bold">${escapeHtmlTraslado(t.nombreSedeOrigen || "-")}</div>
                </div>
            </div>

            <div class="col-md-3">
                <div class="border rounded-4 p-3 bg-light h-100">
                    <small class="text-muted">Destino</small>
                    <div class="fw-bold">${escapeHtmlTraslado(t.nombreSedeDestino || "-")}</div>
                </div>
            </div>

        </div>

        <div class="border rounded-4 p-3 bg-light mb-4">
            <h6 class="fw-bold mb-3">
                <i class="bi bi-diagram-3 me-1"></i>
                Línea de tiempo
            </h6>

            <div class="row g-2">
                ${construirPasoHistorial("Creada", t.fechaTransferencia, "bi-plus-circle")}
                ${construirPasoHistorial("Aceptada", t.fechaAceptacion, "bi-check2-circle")}
                ${construirPasoHistorial("Llegó a origen", t.fechaLlegadaOrigen, "bi-geo-alt")}
                ${construirPasoHistorial("Pedido listo", t.fechaPedidoListo, "bi-box-seam")}
                ${construirPasoHistorial("En ruta", t.fechaInicioRuta, "bi-truck")}
                ${construirPasoHistorial("Llegó a destino", t.fechaLlegadaDestino, "bi-flag")}
                ${construirPasoHistorial("Recepción", t.fechaRecepcion, "bi-clipboard-check")}
            </div>
        </div>

        <div class="border rounded-4 p-3 bg-light mb-4">
            <h6 class="fw-bold mb-3">
                <i class="bi bi-box-seam me-1"></i>
                Productos del traslado
            </h6>

            ${construirProductosHistorialTraslado(t.detalles || [])}
        </div>

        <div class="border rounded-4 p-3 bg-light mb-4">
            <h6 class="fw-bold mb-3">
                <i class="bi bi-images me-1"></i>
                Evidencias del traslado
            </h6>

            <div class="row g-3">
                ${construirFotoHistorial("Llegada a origen", t.fotoLlegadaOrigen)}
                ${construirFotoHistorial("Pedido listo / carga", t.fotoCarga)}
                ${construirFotoHistorial("Llegada a destino", t.fotoLlegada)}
            </div>
        </div>

        <div class="row g-3">
            <div class="col-md-6">
                <small class="text-muted">Observación inicial</small>
                <div class="border rounded-4 p-3 bg-light">
                    ${escapeHtmlTraslado(t.observacion || "Sin observación")}
                </div>
            </div>

            <div class="col-md-6">
                <small class="text-muted">Observación recepción</small>
                <div class="border rounded-4 p-3 bg-light">
                    ${escapeHtmlTraslado(t.observacionRecepcion || "Sin observación")}
                </div>
            </div>
        </div>
    `;
}

function construirPasoHistorial(titulo, fecha, icono) {
    const activo = fecha && String(fecha).trim() !== "";

    return `
        <div class="col-12 col-md-6 col-xl-3">
            <div class="border rounded-4 p-3 h-100 ${activo ? "bg-success-subtle text-success" : "bg-white text-muted"}">
                <div class="d-flex align-items-center gap-3">
                    <div class="rounded-circle d-flex align-items-center justify-content-center ${activo ? "bg-success text-white" : "bg-light border text-muted"}"
                         style="width:42px;height:42px;min-width:42px;">
                        <i class="bi ${icono}"></i>
                    </div>

                    <div>
                        <div class="fw-bold">${escapeHtmlTraslado(titulo)}</div>
                        <small>${escapeHtmlTraslado(formatearFechaTraslado(fecha))}</small>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function construirProductosHistorialTraslado(detalles) {
    if (!detalles.length) {
        return `<div class="text-muted text-center py-3">No hay productos registrados.</div>`;
    }

    return `
        <div class="table-responsive border rounded-4 overflow-hidden">
            <table class="table align-middle mb-0">
                <thead class="table-light">
                <tr>
                    <th>Producto</th>
                    <th class="text-center">Enviado</th>
                    <th class="text-center">Recibido</th>
                    <th class="text-center">Faltante</th>
                    <th>Observación</th>
                </tr>
                </thead>

                <tbody>
                ${detalles.map(d => {
                    const foto = d.fotoProducto
                        ? `/uploads/productos/${escapeHtmlTraslado(d.fotoProducto)}`
                        : "";

                    return `
                        <tr>
                            <td>
                                <div class="d-flex align-items-center gap-3">
                                    <div class="rounded-3 border bg-light d-flex align-items-center justify-content-center"
                                         style="width:50px;height:50px;min-width:50px;overflow:hidden;">
                                        ${
                                            foto
                                                ? `<img src="${foto}" alt="Producto" style="width:100%;height:100%;object-fit:cover;">`
                                                : `<i class="bi bi-box-seam text-primary fs-4"></i>`
                                        }
                                    </div>

                                    <div>
                                        <strong>${escapeHtmlTraslado(d.nombreProducto || "Producto")}</strong>
                                        <small class="text-muted d-block">${escapeHtmlTraslado(d.codigoProducto || "-")}</small>
                                    </div>
                                </div>
                            </td>

                            <td class="text-center fw-bold">${escapeHtmlTraslado(d.cantidadEnviada ?? d.cantidad ?? 0)}</td>
                            <td class="text-center fw-bold text-success">${escapeHtmlTraslado(d.cantidadRecibida ?? "-")}</td>
                            <td class="text-center fw-bold ${Number(d.cantidadFaltante || 0) > 0 ? "text-danger" : "text-muted"}">
                                ${escapeHtmlTraslado(d.cantidadFaltante ?? 0)}
                            </td>
                            <td>${escapeHtmlTraslado(d.observacionRecepcion || "-")}</td>
                        </tr>
                    `;
                }).join("")}
                </tbody>
            </table>
        </div>
    `;
}

function construirFotoHistorial(titulo, archivo) {
    const tieneFoto = archivo && String(archivo).trim() !== "";
    const ruta = tieneFoto ? `/uploads/traslados/${escapeHtmlTraslado(archivo)}` : "";

    return `
        <div class="col-12 col-md-4">
            <div class="border rounded-4 p-3 bg-white h-100">
                <h6 class="fw-bold mb-2">${escapeHtmlTraslado(titulo)}</h6>

                ${
                    tieneFoto
                        ? `
                            <img src="${ruta}"
                                 class="img-fluid rounded-4 border mb-2"
                                 style="height:160px;width:100%;object-fit:cover;">

                            <a href="${ruta}"
                               target="_blank"
                               class="btn btn-sm btn-outline-primary rounded-3 w-100">
                                <i class="bi bi-eye me-1"></i>
                                Ver foto
                            </a>
                        `
                        : `
                            <div class="text-center text-muted py-5 border rounded-4 bg-light">
                                <i class="bi bi-camera fs-2 d-block mb-2"></i>
                                Sin evidencia
                            </div>
                        `
                }
            </div>
        </div>
    `;
}

function formatearFechaTraslado(valor) {
    if (!valor) {
        return "-";
    }

    const fecha = new Date(valor);

    if (Number.isNaN(fecha.getTime())) {
        return String(valor);
    }

    return fecha.toLocaleString("es-PE", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function formatearEstadoTraslado(estado) {
    switch (estado) {
        case "COMPLETADA":
            return "Completada";
        case "COMPLETADA_CON_DIFERENCIAS":
            return "Completada con diferencias";
        case "CANCELADA":
            return "Cancelada";
        case "PENDIENTE_RECEPCION":
            return "Pendiente recepción";
        default:
            return estado || "Sin estado";
    }
}

function escapeHtmlTraslado(valor) {
    return String(valor ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function inicializarAvisosCambiosTraslado() {
    const traslados = Array.from(document.querySelectorAll(".traslado-vigilado"));
    const modalElement = document.getElementById("modalAvisoCambioTraslado");

    if (!traslados.length || !modalElement) {
        return;
    }

    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);

    const icono = document.getElementById("iconoAvisoCambioTraslado");
    const titulo = document.getElementById("tituloAvisoCambioTraslado");
    const mensaje = document.getElementById("mensajeAvisoCambioTraslado");
    const codigo = document.getElementById("codigoAvisoCambioTraslado");
    const btnEntendido = document.getElementById("btnEntendidoCambioTraslado");

    const ids = traslados
        .map(t => t.dataset.id)
        .filter(id => id && id.trim() !== "");

    if (!ids.length) {
        return;
    }

    let avisoMostradoEnPantalla = false;

    function obtenerEventosVistos() {
        try {
            return JSON.parse(localStorage.getItem("dentia_traslados_eventos_vistos") || "[]");
        } catch (e) {
            return [];
        }
    }

    function guardarEventoVisto(claveEvento) {
        const vistos = obtenerEventosVistos();

        if (!vistos.includes(claveEvento)) {
            vistos.push(claveEvento);
        }

        localStorage.setItem("dentia_traslados_eventos_vistos", JSON.stringify(vistos));
    }

    function eventoYaVisto(claveEvento) {
        return obtenerEventosVistos().includes(claveEvento);
    }

    async function consultarCambios() {
        if (avisoMostradoEnPantalla) {
            return;
        }

        try {
            const response = await fetch(`/interno/traslados-internos/notificaciones-cambios?ids=${ids.join(",")}`);

            if (!response.ok) {
                return;
            }

            const data = await response.json();

            if (!data || !data.hayCambios) {
                return;
            }

            const claveEvento = `TRASLADO_${data.idTransferencia}_${data.idEvento}_${data.tipo}`;

            if (eventoYaVisto(claveEvento)) {
                return;
            }

            avisoMostradoEnPantalla = true;

            // IMPORTANTE:
            // Se marca como visto ANTES de mostrar el modal,
            // así no vuelve a salir al recargar ni en el siguiente intervalo.
            guardarEventoVisto(claveEvento);

            mostrarAvisoCambio(data);

        } catch (error) {
            console.warn("No se pudo consultar cambios del traslado.", error);
        }
    }

    function mostrarAvisoCambio(data) {
        const tipo = data.tipo || "EDITADA";

        codigo.textContent = data.codigo || "TRF-00000";

        if (tipo === "CANCELADA") {
            icono.className = "bg-danger-subtle text-danger rounded-circle mx-auto mb-4 d-flex align-items-center justify-content-center";
            icono.innerHTML = `<i class="bi bi-x-circle fs-1"></i>`;

            titulo.textContent = "Traslado cancelado";
            mensaje.textContent = data.mensaje || "El administrador anuló este pedido. Ya no debes continuar con este traslado.";

            btnEntendido.className = "btn btn-danger btn-lg rounded-4 px-5 fw-semibold";
            btnEntendido.textContent = "Entendido, volver a la lista";

        } else {
            icono.className = "bg-warning-subtle text-warning rounded-circle mx-auto mb-4 d-flex align-items-center justify-content-center";
            icono.innerHTML = `<i class="bi bi-exclamation-triangle fs-1"></i>`;

            titulo.textContent = "Pedido actualizado";
            mensaje.textContent = data.mensaje || "El administrador editó este pedido. Verifica nuevamente los productos antes de continuar.";

            btnEntendido.className = "btn btn-primary btn-lg rounded-4 px-5 fw-semibold";
            btnEntendido.textContent = "Entendido, revisar pedido";
        }

        btnEntendido.onclick = () => {
            location.reload();
        };

        modal.show();
    }

    // No consultamos instantáneo para evitar que salga apenas entra por eventos antiguos.
    setTimeout(consultarCambios, 3000);

    // Luego revisa cada 8 segundos.
    setInterval(consultarCambios, 8000);
}
