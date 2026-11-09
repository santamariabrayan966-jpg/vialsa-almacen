document.addEventListener("DOMContentLoaded", () => {
    inicializarPanelTransferencia();
    inicializarProductosTransferencia();
    inicializarDetalleTransferencia();
    inicializarEditarProductosTransferencia();
    inicializarAnularTransferencia();
    inicializarResumenTransferencias();
    inicializarFiltroYPaginacionTransferencias();
});

function inicializarPanelTransferencia() {
    const btnNuevo = document.getElementById("btnIrNuevaTransferencia");
    const btnOcultar = document.getElementById("btnOcultarNuevaTransferencia");
    const panel = document.getElementById("panelNuevaTransferencia");
    const colHistorial = document.getElementById("colHistorialTransferencias");

    if (!btnNuevo || !panel || !colHistorial) {
        return;
    }

    function mostrarPanel() {
        const esMovil = window.innerWidth < 768;
        panel.classList.remove("d-none");
        panel.setAttribute("aria-hidden", "false");
        panel.setAttribute("role", esMovil ? "dialog" : "region");
        if (esMovil) panel.setAttribute("aria-modal", "true");
        else panel.removeAttribute("aria-modal");

        colHistorial.classList.remove("col-xl-12");
        colHistorial.classList.add("col-xl-7");

        btnNuevo.classList.add("d-none");
        document.body.classList.toggle("inventario-panel-movil-abierto", esMovil);

        setTimeout(() => {
            if (esMovil) {
                btnOcultar?.focus({ preventScroll: true });
            } else {
                panel.scrollIntoView({ behavior: "smooth", block: "start" });
            }
        }, 100);
    }

    function ocultarPanel() {
        panel.classList.add("d-none");
        panel.setAttribute("aria-hidden", "true");
        panel.removeAttribute("aria-modal");
        document.body.classList.remove("inventario-panel-movil-abierto");

        colHistorial.classList.remove("col-xl-7");
        colHistorial.classList.add("col-xl-12");

        btnNuevo.classList.remove("d-none");
        btnNuevo.focus({ preventScroll: true });
    }

    btnNuevo.addEventListener("click", mostrarPanel);
    btnOcultar?.addEventListener("click", ocultarPanel);
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !panel.classList.contains("d-none")) {
            ocultarPanel();
        }
    });
}

function inicializarProductosTransferencia() {
    const contenedor = document.getElementById("contenedorProductosTransferencia");
    const btnAgregar = document.getElementById("btnAgregarProductoTransferencia");
    const form = document.getElementById("formTransferencia");

    if (!contenedor || !btnAgregar || !form) {
        return;
    }

    btnAgregar.addEventListener("click", () => {
        const primerItem = contenedor.querySelector(".producto-transferencia-item");

        if (!primerItem) {
            return;
        }

        const nuevoItem = primerItem.cloneNode(true);

        nuevoItem.querySelector(".producto-transferencia-id").value = "";

        const buscadorNuevo = nuevoItem.querySelector(".producto-transferencia-buscador");
        buscadorNuevo.value = "";
        buscadorNuevo.dataset.stock = "0";
        buscadorNuevo.removeAttribute("data-foto");

        nuevoItem.querySelector(".cantidad-transferencia-input").value = "";
        nuevoItem.querySelector(".error-producto-transferencia").textContent = "";
        nuevoItem.querySelector(".lista-productos-transferencia").classList.add("d-none");
        nuevoItem.querySelector(".btnEliminarProductoTransferencia").classList.remove("d-none");

        limpiarImagenProductoTransferencia(nuevoItem);

        contenedor.appendChild(nuevoItem);
        actualizarBotonesEliminarTransferencia();
    });

    contenedor.addEventListener("click", (e) => {
        const btnEliminar = e.target.closest(".btnEliminarProductoTransferencia");

        if (!btnEliminar) {
            return;
        }

        btnEliminar.closest(".producto-transferencia-item").remove();
        limpiarErroresTransferencia();
        actualizarBotonesEliminarTransferencia();
    });

    contenedor.addEventListener("click", (e) => {
        const btnAbrir = e.target.closest(".btnAbrirProductosTransferencia");
        const buscador = e.target.closest(".producto-transferencia-buscador");
        const itemProducto = e.target.closest(".item-producto-transferencia");

        if (btnAbrir || buscador) {
            const item = e.target.closest(".producto-transferencia-item");
            const lista = item.querySelector(".lista-productos-transferencia");
            const input = item.querySelector(".producto-transferencia-buscador");

            cerrarListasProductosTransferencia(lista);
            lista.classList.toggle("d-none");

            if (!lista.classList.contains("d-none")) {
                input.removeAttribute("readonly");
                input.focus();
                filtrarProductosTransferencia(item);
            }

            return;
        }

        if (itemProducto) {
            const item = itemProducto.closest(".producto-transferencia-item");

            const idInput = item.querySelector(".producto-transferencia-id");
            const buscadorInput = item.querySelector(".producto-transferencia-buscador");
            const lista = item.querySelector(".lista-productos-transferencia");

            idInput.value = itemProducto.dataset.id;
            buscadorInput.value = `${itemProducto.dataset.codigo} - ${itemProducto.dataset.nombre}`;
            buscadorInput.dataset.stock = itemProducto.dataset.stock || "0";
            buscadorInput.dataset.foto = itemProducto.dataset.foto || "";

            pintarImagenProductoTransferencia(item, itemProducto.dataset.foto);

            lista.classList.add("d-none");
            buscadorInput.setAttribute("readonly", "readonly");

            item.querySelector(".error-producto-transferencia").textContent = "";
        }
    });

    contenedor.addEventListener("input", (e) => {
        const buscador = e.target.closest(".producto-transferencia-buscador");

        if (!buscador) {
            return;
        }

        const item = buscador.closest(".producto-transferencia-item");
        item.querySelector(".producto-transferencia-id").value = "";
        filtrarProductosTransferencia(item);
    });

    form.addEventListener("submit", (e) => {
        limpiarErroresTransferencia();

        const items = Array.from(contenedor.querySelectorAll(".producto-transferencia-item"));
        const productosUsados = new Set();

        let hayError = false;

        items.forEach(item => {
            const idInput = item.querySelector(".producto-transferencia-id");
            const buscador = item.querySelector(".producto-transferencia-buscador");
            const cantidadInput = item.querySelector(".cantidad-transferencia-input");
            const error = item.querySelector(".error-producto-transferencia");

            const idProducto = idInput.value;
            const cantidad = Number(cantidadInput.value || 0);
            const stockDisponible = Number(buscador.dataset.stock || 0);

            if (!idProducto) {
                error.textContent = "Debe seleccionar un producto.";
                hayError = true;
                return;
            }

            if (productosUsados.has(idProducto)) {
                error.textContent = "Este producto ya fue agregado en la transferencia.";
                hayError = true;
                return;
            }

            productosUsados.add(idProducto);

            if (!cantidadInput.value || cantidad <= 0) {
                error.textContent = "La cantidad debe ser mayor a 0.";
                hayError = true;
                return;
            }

            if (!Number.isInteger(cantidad)) {
                error.textContent = "La transferencia por ahora debe ser en unidades completas.";
                hayError = true;
                return;
            }

            if (cantidad > stockDisponible) {
                error.textContent = `Stock insuficiente. Disponible: ${stockDisponible}.`;
                hayError = true;
            }
        });

        if (hayError) {
            e.preventDefault();

            const primerError = contenedor.querySelector(".error-producto-transferencia:not(:empty)");

            primerError?.scrollIntoView({
                behavior: "smooth",
                block: "center"
            });
        }
    });

    actualizarBotonesEliminarTransferencia();
}

function limpiarErroresTransferencia() {
    document.querySelectorAll(".error-producto-transferencia").forEach(error => {
        error.textContent = "";
    });
}

function actualizarBotonesEliminarTransferencia() {
    const items = document.querySelectorAll(".producto-transferencia-item");

    items.forEach((item) => {
        const btnEliminar = item.querySelector(".btnEliminarProductoTransferencia");

        if (!btnEliminar) {
            return;
        }

        btnEliminar.classList.toggle("d-none", items.length === 1);
    });
}

function inicializarDetalleTransferencia() {
    const botones = document.querySelectorAll(".btnDetalleTransferencia");

    botones.forEach(btn => {
        btn.addEventListener("click", async () => {
            const idTransferencia = btn.dataset.id;

            limpiarModalDetalleTransferencia();

            const btnPdf = document.getElementById("btnExportarDetalleTransferencia");

            if (btnPdf) {
                btnPdf.href = `/interno/inventario/transferencias/${idTransferencia}/pdf`;
            }

            try {
                const response = await fetch(`/interno/inventario/transferencias/detalle/${idTransferencia}`);

                if (!response.ok) {
                    const mensaje = await response.text();
                    throw new Error(mensaje || "No se pudo cargar el detalle de la transferencia.");
                }

                const transferencia = await response.json();

                pintarCabeceraDetalleTransferencia(transferencia);
                pintarTimelineTransferencia(transferencia);
                pintarFotosTransferencia(transferencia);
               pintarProductosDetalleCompletoTransferencia(
                   transferencia.detalles || [],
                   transferencia || {}
               );
                pintarEventosTransferencia(transferencia.eventos || []);

            } catch (error) {
                pintarErrorDetalleTransferencia(error.message || "Error al cargar el detalle.");
            }
        });
    });
}

function formatearEstadoTransferencia(estado) {
    switch (estado) {
        case "CREADA":
            return "Creada";
        case "ACEPTADA":
            return "Aceptada";
        case "EN_RECOJO":
            return "En recojo";
        case "LISTO_PARA_SALIR":
            return "Listo para salir";
        case "EN_RUTA":
            return "En ruta";
        case "PENDIENTE_RECEPCION":
            return "Pendiente recepción";
        case "COMPLETADA":
            return "Completada";
        case "COMPLETADA_CON_DIFERENCIAS":
            return "Completada con diferencias";
        case "CANCELADA":
            return "Cancelada";
        default:
            return estado || "Sin estado";
    }
}

function escapeHtmlTransferencia(valor) {
    return String(valor ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
function limpiarModalDetalleTransferencia() {
    const campos = {
        detalleCodigoTransferencia: "TRF-00000",
        detalleFechaTransferencia: "-",
        detalleUsuarioTransferencia: "-",
        detalleTransportistaTransferencia: "-",
        detalleOrigenTransferencia: "-",
        detalleDestinoTransferencia: "-",
        detalleObservacionTransferencia: "Sin observación",
        detalleObservacionRecepcionTransferencia: "Sin observación"
    };

    Object.entries(campos).forEach(([id, valor]) => {
        const el = document.getElementById(id);

        if (el) {
            el.textContent = valor;
        }
    });

    const estadoSpan = document.getElementById("detalleEstadoTransferencia");

    if (estadoSpan) {
        estadoSpan.textContent = "-";
        estadoSpan.className = "badge bg-secondary-subtle text-secondary px-3 py-2";
    }

    const timeline = document.getElementById("detalleTimelineTransferencia");
    const fotos = document.getElementById("detalleFotosTransferencia");
    const productos = document.getElementById("detalleProductosTransferencia");
    const eventos = document.getElementById("detalleEventosTransferencia");

    if (timeline) {
        timeline.innerHTML = `<div class="text-muted small">Cargando seguimiento...</div>`;
    }

    if (fotos) {
        fotos.innerHTML = `<div class="text-muted small">Cargando evidencias...</div>`;
    }

    if (productos) {
        productos.innerHTML = `<div class="text-muted small">Cargando productos...</div>`;
    }

    if (eventos) {
        eventos.innerHTML = `<div class="text-muted small">Cargando eventos...</div>`;
    }
}

function pintarCabeceraDetalleTransferencia(t) {
    document.getElementById("detalleCodigoTransferencia").textContent = t.codigo || "TRF-00000";
    document.getElementById("detalleFechaTransferencia").textContent = formatearFechaTransferencia(t.fechaTransferencia);
    document.getElementById("detalleUsuarioTransferencia").textContent = t.nombreUsuario || "Sistema";
    document.getElementById("detalleTransportistaTransferencia").textContent = t.nombreTransportista || "Sin asignar";
    document.getElementById("detalleOrigenTransferencia").textContent = t.nombreSedeOrigen || "-";
    document.getElementById("detalleDestinoTransferencia").textContent = t.nombreSedeDestino || "-";

    document.getElementById("detalleObservacionTransferencia").textContent =
        t.observacion && t.observacion.trim() !== "" ? t.observacion : "Sin observación";

    document.getElementById("detalleObservacionRecepcionTransferencia").textContent =
        t.observacionRecepcion && t.observacionRecepcion.trim() !== "" ? t.observacionRecepcion : "Sin observación";

    const estadoSpan = document.getElementById("detalleEstadoTransferencia");
    const estado = t.estado || "";

    if (estadoSpan) {
        estadoSpan.textContent = formatearEstadoTransferencia(estado);
        estadoSpan.className = "badge px-3 py-2";

        if (estado === "COMPLETADA") {
            estadoSpan.classList.add("bg-success-subtle", "text-success");
        } else if (estado === "PENDIENTE_RECEPCION") {
            estadoSpan.classList.add("bg-info-subtle", "text-info");
        } else if (estado === "CANCELADA") {
            estadoSpan.classList.add("bg-danger-subtle", "text-danger");
        } else if (["CREADA", "ACEPTADA", "EN_RECOJO", "LISTO_PARA_SALIR", "EN_RUTA"].includes(estado)) {
            estadoSpan.classList.add("bg-primary-subtle", "text-primary");
        } else {
            estadoSpan.classList.add("bg-secondary-subtle", "text-secondary");
        }
    }
}

function esTransferenciaInmediata(t = {}) {
    const tipo = String(t.tipoTransferencia || "").toUpperCase();
    const requiereTransporte = String(t.requiereTransporte).toLowerCase();

    return tipo === "INMEDIATA"
        || requiereTransporte === "false"
        || requiereTransporte === "0";
}

function pintarTimelineTransferencia(t) {
    const contenedor = document.getElementById("detalleTimelineTransferencia");

    if (!contenedor) {
        return;
    }

    if (esTransferenciaInmediata(t)) {
        contenedor.innerHTML = `
            <div class="col-12">
                <div class="border rounded-4 p-4 bg-success-subtle text-success">
                    <div class="d-flex align-items-center gap-3 flex-wrap">
                        <div class="bg-success text-white rounded-circle d-flex align-items-center justify-content-center"
                             style="width:54px;height:54px;min-width:54px;">
                            <i class="bi bi-lightning-charge fs-4"></i>
                        </div>

                        <div>
                            <h5 class="fw-bold mb-1">Transferencia inmediata</h5>
                            <p class="mb-0">
                                Esta transferencia fue completada directamente, sin proceso de transporte ni validaciones del chofer.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        `;
        return;
    }

    const pasos = [
        {
            titulo: "Creada",
            icono: "bi-plus-circle",
            fecha: t.fechaTransferencia,
            activo: true
        },
        {
            titulo: "Aceptada",
            icono: "bi-check2-circle",
            fecha: t.fechaAceptacion,
            activo: !!t.fechaAceptacion
        },
        {
            titulo: "Llegó a origen",
            icono: "bi-geo-alt",
            fecha: t.fechaLlegadaOrigen,
            activo: !!t.fechaLlegadaOrigen
        },
        {
            titulo: "Pedido listo",
            icono: "bi-box-seam",
            fecha: t.fechaPedidoListo,
            activo: !!t.fechaPedidoListo
        },
        {
            titulo: "En ruta",
            icono: "bi-truck",
            fecha: t.fechaInicioRuta,
            activo: !!t.fechaInicioRuta
        },
        {
            titulo: "Llegó a destino",
            icono: "bi-flag",
            fecha: t.fechaLlegadaDestino,
            activo: !!t.fechaLlegadaDestino
        },
        {
            titulo: "Recepción confirmada",
            icono: "bi-clipboard-check",
            fecha: t.fechaRecepcion,
            activo: !!t.fechaRecepcion || t.estado === "COMPLETADA"
        }
    ];

    contenedor.innerHTML = pasos.map(p => {
        const clase = p.activo
            ? "bg-success-subtle text-success border-success-subtle"
            : "bg-white text-muted border";

        const iconoClase = p.activo
            ? "bg-success text-white"
            : "bg-light text-muted border";

        return `
            <div class="col-12 col-md-6 col-xl-3">
                <div class="border rounded-4 p-3 h-100 ${clase}">
                    <div class="d-flex align-items-center gap-3">
                        <div class="rounded-circle d-flex align-items-center justify-content-center ${iconoClase}"
                             style="width:42px;height:42px;min-width:42px;">
                            <i class="bi ${p.icono}"></i>
                        </div>

                        <div>
                            <div class="fw-bold">${escapeHtmlTransferencia(p.titulo)}</div>
                            <small>${escapeHtmlTransferencia(formatearFechaTransferencia(p.fecha))}</small>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join("");
}

function pintarFotosTransferencia(t) {
    const contenedor = document.getElementById("detalleFotosTransferencia");

    if (!contenedor) {
        return;
    }

    if (esTransferenciaInmediata(t)) {
        contenedor.innerHTML = `
            <div class="col-12">
                <div class="border rounded-4 p-4 bg-light text-muted">
                    <div class="d-flex align-items-center gap-3 flex-wrap">
                        <div class="bg-white border rounded-circle d-flex align-items-center justify-content-center"
                             style="width:54px;height:54px;min-width:54px;">
                            <i class="bi bi-check2-circle fs-4 text-success"></i>
                        </div>

                        <div>
                            <h6 class="fw-bold mb-1 text-dark">Sin evidencias de transporte</h6>
                            <p class="mb-0">
                                Esta transferencia fue inmediata, por eso no registra fotos de llegada, carga o destino.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        `;
        return;
    }

    const fotos = [
        {
            titulo: "Llegada a origen",
            archivo: t.fotoLlegadaOrigen,
            fecha: t.fechaLlegadaOrigen
        },
        {
            titulo: "Pedido listo / carga",
            archivo: t.fotoCarga,
            fecha: t.fechaPedidoListo
        },
        {
            titulo: "Llegada a destino",
            archivo: t.fotoLlegada,
            fecha: t.fechaLlegadaDestino
        }
    ];

    contenedor.innerHTML = fotos.map(f => {
        const tieneFoto = f.archivo && String(f.archivo).trim() !== "";
        const ruta = tieneFoto ? `/uploads/traslados/${escapeHtmlTransferencia(f.archivo)}` : "";

        return `
            <div class="col-12 col-md-4">
                <div class="bg-white border rounded-4 p-3 h-100">
                    <div class="d-flex justify-content-between align-items-start gap-2 mb-2">
                        <div>
                            <div class="fw-bold">${escapeHtmlTransferencia(f.titulo)}</div>
                            <small class="text-muted">${escapeHtmlTransferencia(formatearFechaTransferencia(f.fecha))}</small>
                        </div>
                    </div>

                    ${
                        tieneFoto
                            ? `
                                <img src="${ruta}"
                                     alt="${escapeHtmlTransferencia(f.titulo)}"
                                     class="img-fluid rounded-4 border mb-2"
                                     style="height:170px;width:100%;object-fit:cover;">

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
                                    <small>Sin evidencia</small>
                                </div>
                            `
                    }
                </div>
            </div>
        `;
    }).join("");
}

function pintarProductosDetalleCompletoTransferencia(detalles, transferencia = {}) {
    const contenedor = document.getElementById("detalleProductosTransferencia");

    if (!contenedor) {
        return;
    }

    if (!detalles.length) {
        contenedor.innerHTML = `
            <div class="text-center text-muted py-3">
                No hay productos registrados en esta transferencia.
            </div>
        `;
        return;
    }

   if (transferencia && esTransferenciaInmediata(transferencia)) {
        contenedor.innerHTML = `
            <div class="row g-3">
                ${detalles.map(d => {
                    const cantidad = d.cantidadEnviada ?? d.cantidad ?? 0;

                    const foto = d.fotoProducto
                        ? `/uploads/productos/${escapeHtmlTransferencia(d.fotoProducto)}`
                        : null;

                    return `
                        <div class="col-12 col-md-6">
                            <div class="border rounded-4 p-3 bg-white h-100">
                                <div class="d-flex align-items-center gap-3">

                                    <div class="rounded-4 border bg-light d-flex align-items-center justify-content-center"
                                         style="width:70px;height:70px;min-width:70px;overflow:hidden;">
                                        ${
                                            foto
                                                ? `<img src="${foto}" alt="Producto" style="width:100%;height:100%;object-fit:cover;">`
                                                : `<i class="bi bi-box-seam text-primary fs-3"></i>`
                                        }
                                    </div>

                                    <div class="flex-grow-1">
                                        <h6 class="fw-bold mb-1">
                                            ${escapeHtmlTransferencia(d.nombreProducto || "Producto")}
                                        </h6>

                                        <small class="text-muted d-block mb-2">
                                            ${escapeHtmlTransferencia(d.codigoProducto || "-")}
                                        </small>

                                        <div class="d-flex align-items-center gap-2 flex-wrap">
                                            <span class="badge bg-primary-subtle text-primary px-3 py-2">
                                                Cantidad: ${escapeHtmlTransferencia(cantidad)}
                                            </span>

                                            <span class="badge bg-success-subtle text-success px-3 py-2">
                                                Aplicado directamente
                                            </span>
                                        </div>
                                    </div>

                                </div>
                            </div>
                        </div>
                    `;
                }).join("")}
            </div>
        `;

        return;
    }

    let html = `
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
    `;

    detalles.forEach(d => {
        const enviado = d.cantidadEnviada ?? d.cantidad ?? 0;
        const recibido = d.cantidadRecibida ?? "-";
        const faltante = d.cantidadFaltante ?? 0;
        const observacion = d.observacionRecepcion && d.observacionRecepcion.trim() !== ""
            ? d.observacionRecepcion
            : "-";

        const foto = d.fotoProducto
            ? `/uploads/productos/${escapeHtmlTransferencia(d.fotoProducto)}`
            : null;

        const claseFaltante = Number(faltante) > 0
            ? "text-danger fw-bold"
            : "text-muted";

        html += `
            <tr>
                <td>
                    <div class="d-flex align-items-center gap-3">
                        <div class="rounded-3 border bg-light d-flex align-items-center justify-content-center"
                             style="width:52px;height:52px;min-width:52px;overflow:hidden;">
                            ${
                                foto
                                    ? `<img src="${foto}" alt="Producto" style="width:100%;height:100%;object-fit:cover;">`
                                    : `<i class="bi bi-box-seam text-primary fs-4"></i>`
                            }
                        </div>

                        <div>
                            <strong>${escapeHtmlTransferencia(d.nombreProducto || "Producto")}</strong>
                            <small class="text-muted d-block">${escapeHtmlTransferencia(d.codigoProducto || "-")}</small>
                        </div>
                    </div>
                </td>

                <td class="text-center fw-bold">
                    ${escapeHtmlTransferencia(enviado)}
                </td>

                <td class="text-center fw-bold text-success">
                    ${escapeHtmlTransferencia(recibido)}
                </td>

                <td class="text-center ${claseFaltante}">
                    ${escapeHtmlTransferencia(faltante)}
                </td>

                <td>
                    ${escapeHtmlTransferencia(observacion)}
                </td>
            </tr>
        `;
    });

    html += `
                </tbody>
            </table>
        </div>
    `;

    contenedor.innerHTML = html;
}

function pintarEventosTransferencia(eventos) {
    const contenedor = document.getElementById("detalleEventosTransferencia");

    if (!contenedor) {
        return;
    }

    const eventosImportantes = eventos
        .filter(e => {
            const estadoNuevo = String(e.estadoNuevo || "").toUpperCase();
            const observacion = String(e.observacion || "").toLowerCase();

            const esEdicionAdmin =
                observacion.includes("administrador editó")
                || observacion.includes("administrador edito")
                || observacion.includes("editó los productos")
                || observacion.includes("edito los productos");

            const esAnulacion =
                estadoNuevo === "CANCELADA"
                || observacion.includes("anuló")
                || observacion.includes("anulo")
                || observacion.includes("anulada")
                || observacion.includes("cancelada");

            const esImprevisto =
                observacion.includes("imprevisto:")
                || observacion.includes("incidencia:")
                || observacion.includes("foto_evidencia=")
                || observacion.includes("accidente")
                || observacion.includes("demora")
                || observacion.includes("tráfico")
                || observacion.includes("trafico")
                || observacion.includes("problema");

            const esDiferencia =
                observacion.includes("diferencia")
                || observacion.includes("faltante")
                || observacion.includes("dañado")
                || observacion.includes("danado");

            return esEdicionAdmin || esAnulacion || esImprevisto || esDiferencia;
        })
        .filter((e, index, array) => {
            const claveActual = construirClaveEventoImportante(e);

            return array.findIndex(x => construirClaveEventoImportante(x) === claveActual) === index;
        });

    if (!eventosImportantes.length) {
        contenedor.innerHTML = `
            <div class="text-center text-muted py-3">
                No se registraron cambios administrativos ni imprevistos.
            </div>
        `;
        return;
    }

    contenedor.innerHTML = eventosImportantes.map(e => {
        const datos = separarObservacionEventoImportante(e.observacion || "");
        const titulo = obtenerTituloEventoImportante(e);
        const foto = datos.foto ? `/uploads/traslados/${escapeHtmlTransferencia(datos.foto)}` : "";

        return `
            <div class="bg-white border rounded-4 p-3 mb-3">

                <div class="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-2">
                    <div>
                        <div class="fw-bold">
                            ${escapeHtmlTransferencia(titulo)}
                        </div>

                        <small class="text-muted">
                            Usuario:
                            ${escapeHtmlTransferencia(e.nombreUsuario || "Sistema")}
                        </small>
                    </div>

                    <small class="text-muted">
                        ${escapeHtmlTransferencia(formatearFechaTransferencia(e.fechaEvento))}
                    </small>
                </div>

                <div class="small text-muted mb-3">
                    ${escapeHtmlTransferencia(datos.texto)}
                </div>

                ${
                    foto
                        ? `
                           <div class="border rounded-4 p-3 bg-light">
                               <div class="row g-3 align-items-center">

                                   <div class="col-12 col-md-4">
                                       <img src="${foto}"
                                            alt="Evidencia del imprevisto"
                                            class="img-fluid rounded-4 border"
                                            style="height:170px;width:100%;object-fit:cover;">
                                   </div>

                                   <div class="col-12 col-md-8">
                                       <div class="d-flex flex-column justify-content-center h-100">

                                           <small class="text-muted d-block mb-1">
                                               Evidencia del imprevisto
                                           </small>

                                           <h6 class="fw-bold mb-2">
                                               Foto registrada por el transportista
                                           </h6>

                                           <p class="text-muted small mb-3">
                                               Esta imagen respalda el imprevisto reportado durante el traslado.
                                           </p>

                                           <div>
                                               <a href="${foto}"
                                                  target="_blank"
                                                  class="btn btn-sm btn-outline-primary rounded-3 px-3">
                                                   <i class="bi bi-eye me-1"></i>
                                                   Ver foto completa
                                               </a>
                                           </div>

                                       </div>
                                   </div>

                               </div>
                           </div>
                        `
                        : ""
                }

            </div>
        `;
    }).join("");
}

function separarObservacionEventoImportante(observacion) {
    const textoOriginal = String(observacion || "").trim();

    const partes = textoOriginal.split("| FOTO_EVIDENCIA=");
    const texto = partes[0] ? partes[0].trim() : "Sin observación";
    const foto = partes[1] ? partes[1].trim() : "";

    return {
        texto,
        foto
    };
}

function construirClaveEventoImportante(e) {
    const observacion = String(e.observacion || "").trim().toLowerCase();
    const estadoAnterior = String(e.estadoAnterior || "").trim().toUpperCase();
    const estadoNuevo = String(e.estadoNuevo || "").trim().toUpperCase();

    let fecha = "";

    if (e.fechaEvento) {
        const f = new Date(e.fechaEvento);

        if (!Number.isNaN(f.getTime())) {
            fecha = `${f.getFullYear()}-${f.getMonth()}-${f.getDate()}-${f.getHours()}-${f.getMinutes()}`;
        } else {
            fecha = String(e.fechaEvento);
        }
    }

    return `${estadoAnterior}|${estadoNuevo}|${observacion}|${fecha}`;
}

function obtenerTituloEventoImportante(e) {
    const estadoNuevo = String(e.estadoNuevo || "").toUpperCase();
    const observacion = String(e.observacion || "").toLowerCase();

    if (
        observacion.includes("imprevisto:")
        || observacion.includes("incidencia:")
        || observacion.includes("foto_evidencia=")
    ) {
        return "Imprevisto reportado";
    }

    if (
        observacion.includes("administrador editó")
        || observacion.includes("administrador edito")
        || observacion.includes("editó los productos")
        || observacion.includes("edito los productos")
    ) {
        return "Edición administrativa";
    }

    if (
        estadoNuevo === "CANCELADA"
        || observacion.includes("anuló")
        || observacion.includes("anulo")
        || observacion.includes("anulada")
        || observacion.includes("cancelada")
    ) {
        return "Traslado anulado";
    }

    if (
        observacion.includes("diferencia")
        || observacion.includes("faltante")
        || observacion.includes("dañado")
        || observacion.includes("danado")
    ) {
        return "Recepción con diferencias";
    }

    return "Cambio importante";
}

function pintarErrorDetalleTransferencia(mensaje) {
    const productos = document.getElementById("detalleProductosTransferencia");
    const timeline = document.getElementById("detalleTimelineTransferencia");
    const fotos = document.getElementById("detalleFotosTransferencia");
    const eventos = document.getElementById("detalleEventosTransferencia");

    const html = `
        <div class="alert alert-danger rounded-4 mb-0">
            ${escapeHtmlTransferencia(mensaje)}
        </div>
    `;

    if (productos) productos.innerHTML = html;
    if (timeline) timeline.innerHTML = html;
    if (fotos) fotos.innerHTML = html;
    if (eventos) eventos.innerHTML = html;
}

function formatearFechaTransferencia(valor) {
    if (!valor) {
        return "-";
    }

    try {
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

    } catch (e) {
        return String(valor);
    }
}

function filtrarProductosTransferencia(item) {
    const buscador = item.querySelector(".producto-transferencia-buscador");
    const texto = normalizarTextoTransferencia(buscador.value || "");

    item.querySelectorAll(".item-producto-transferencia").forEach(btn => {
        const dataTexto = normalizarTextoTransferencia(btn.dataset.texto || "");
        btn.classList.toggle("d-none", texto && !dataTexto.includes(texto));
    });
}

function cerrarListasProductosTransferencia(excepto = null) {
    document.querySelectorAll(".lista-productos-transferencia").forEach(lista => {
        if (lista !== excepto) {
            lista.classList.add("d-none");
        }
    });
}

function normalizarTextoTransferencia(valor) {
    return String(valor || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}

document.addEventListener("click", (e) => {
    if (!e.target.closest(".producto-transferencia-item")) {
        cerrarListasProductosTransferencia();
    }
});

function pintarImagenProductoTransferencia(item, foto) {
    const contenedorImagen = item.querySelector(".producto-transferencia-imagen");

    if (!contenedorImagen) {
        return;
    }

    if (foto && foto.trim() !== "") {
        contenedorImagen.innerHTML = `
            <img src="/uploads/productos/${escapeHtmlTransferencia(foto)}"
                 alt="Producto"
                 style="width:100%;height:100%;object-fit:cover;">
        `;
    } else {
        contenedorImagen.innerHTML = `<i class="bi bi-box-seam text-primary"></i>`;
    }
}

function limpiarImagenProductoTransferencia(item) {
    const contenedorImagen = item.querySelector(".producto-transferencia-imagen");

    if (!contenedorImagen) {
        return;
    }

    contenedorImagen.innerHTML = `<i class="bi bi-box-seam text-primary"></i>`;
}

document.addEventListener("DOMContentLoaded", function () {
    const tipoInmediata = document.getElementById("tipoTransferenciaInmediata");
    const tipoTransporte = document.getElementById("tipoTransferenciaTransporte");
    const alertaTraslado = document.getElementById("alertaTrasladoInterno");

    if (!tipoInmediata || !tipoTransporte || !alertaTraslado) {
        return;
    }

    function actualizarAlertaTipoTransferencia() {
        if (tipoTransporte.checked) {
            alertaTraslado.classList.remove("d-none");
        } else {
            alertaTraslado.classList.add("d-none");
        }
    }

    tipoInmediata.addEventListener("change", actualizarAlertaTipoTransferencia);
    tipoTransporte.addEventListener("change", actualizarAlertaTipoTransferencia);

    actualizarAlertaTipoTransferencia();
});

document.addEventListener("DOMContentLoaded", function () {
    const botonesRecepcion = document.querySelectorAll(".btnConfirmarRecepcionTransferencia");
    const formRecepcion = document.getElementById("formConfirmarRecepcionTransferencia");
    const contenedor = document.getElementById("contenedorRecepcionTransferencia");
    const codigoSpan = document.getElementById("recepcionCodigoTransferencia");

    if (!botonesRecepcion.length || !formRecepcion || !contenedor || !codigoSpan) {
        return;
    }

    botonesRecepcion.forEach(btn => {
        btn.addEventListener("click", async function () {
            const id = this.dataset.id;
            const codigo = this.dataset.codigo;

            codigoSpan.textContent = codigo;
            formRecepcion.action = `/interno/inventario/transferencias/${id}/confirmar-recepcion`;

            contenedor.innerHTML = `
                <div class="text-center text-muted py-4">
                    <div class="spinner-border spinner-border-sm me-2"></div>
                    Cargando productos...
                </div>
            `;

            try {
                const response = await fetch(`/interno/inventario/transferencias/detalle/${id}`);

                if (!response.ok) {
                    throw new Error("No se pudo cargar el detalle.");
                }

                const transferencia = await response.json();

                if (!transferencia.detalles || transferencia.detalles.length === 0) {
                    contenedor.innerHTML = `
                        <div class="alert alert-warning border-0 rounded-4 mb-0">
                            No hay productos registrados en esta transferencia.
                        </div>
                    `;
                    return;
                }

                let html = `
                    <div class="table-responsive border rounded-4 overflow-hidden">
                        <table class="table align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Producto</th>
                                    <th class="text-center">Enviado</th>
                                    <th class="text-center">Recibido</th>
                                    <th>Observación</th>
                                </tr>
                            </thead>
                            <tbody>
                `;

                transferencia.detalles.forEach(d => {
                    const enviado = d.cantidadEnviada ?? d.cantidad ?? 0;
                    const foto = d.fotoProducto
                        ? `/uploads/productos/${escapeHtmlTransferencia(d.fotoProducto)}`
                        : null;

                    html += `
                        <tr>
                            <td>
                                <input type="hidden" name="idDetalleTransferencia" value="${escapeHtmlTransferencia(d.idDetalleTransferencia)}">

                                <div class="d-flex align-items-center gap-3">
                                    <div class="rounded-3 border bg-light d-flex align-items-center justify-content-center"
                                         style="width:48px;height:48px;min-width:48px;overflow:hidden;">
                                        ${
                                            foto
                                                ? `<img src="${foto}" alt="Producto" style="width:100%;height:100%;object-fit:cover;">`
                                                : `<i class="bi bi-box-seam text-primary fs-4"></i>`
                                        }
                                    </div>

                                    <div>
                                        <strong>${escapeHtmlTransferencia(d.nombreProducto ?? "Producto")}</strong>
                                        <small class="text-muted d-block">${escapeHtmlTransferencia(d.codigoProducto ?? "-")}</small>
                                    </div>
                                </div>
                            </td>

                            <td class="text-center fw-bold">${escapeHtmlTransferencia(enviado)}</td>

                            <td style="width:160px;">
                                <input type="number"
                                       name="cantidadRecibida"
                                       class="form-control rounded-3 text-center input-cantidad-recibida-transferencia"
                                       min="0"
                                       max="${escapeHtmlTransferencia(enviado)}"
                                       step="1"
                                       value="${escapeHtmlTransferencia(enviado)}"
                                       data-enviado="${escapeHtmlTransferencia(enviado)}"
                                       required>

                                <div class="text-danger small fw-semibold mt-1 error-cantidad-recibida-transferencia"></div>
                            </td>

                            <td>
                                <input type="text"
                                       name="observacionDetalle"
                                       class="form-control rounded-3"
                                       placeholder="Opcional: completo, faltante, dañado...">
                            </td>
                        </tr>
                    `;
                });

                html += `
                            </tbody>
                        </table>
                    </div>
                `;

                contenedor.innerHTML = html;

            } catch (error) {
                contenedor.innerHTML = `
                    <div class="alert alert-danger border-0 rounded-4 mb-0">
                        ${escapeHtmlTransferencia(error.message || "Error al cargar productos.")}
                    </div>
                `;
            }
        });
    });

    if (contenedor) {
        contenedor.addEventListener("input", function (e) {
            const input = e.target.closest(".input-cantidad-recibida-transferencia");

            if (!input) {
                return;
            }

            const enviado = Number(input.dataset.enviado || 0);
            let recibido = Number(input.value || 0);

            const error = input
                .closest("td")
                ?.querySelector(".error-cantidad-recibida-transferencia");

            if (recibido < 0) {
                input.value = 0;
                recibido = 0;
            }

            if (recibido > enviado) {
                input.value = enviado;
                recibido = enviado;

                if (error) {
                    error.textContent = `No puede recibir más de lo enviado. Máximo: ${enviado}.`;
                }

                return;
            }

            if (!Number.isInteger(recibido)) {
                if (error) {
                    error.textContent = "La cantidad recibida debe ser un número entero.";
                }

                return;
            }

            if (error) {
                error.textContent = "";
            }
        });
    }

    if (formRecepcion) {
        formRecepcion.addEventListener("submit", function (e) {
            const inputs = Array.from(
                formRecepcion.querySelectorAll(".input-cantidad-recibida-transferencia")
            );

            let hayError = false;
            let mensaje = "";

            inputs.forEach(input => {
                const enviado = Number(input.dataset.enviado || 0);
                const recibido = Number(input.value || 0);

                const error = input
                    .closest("td")
                    ?.querySelector(".error-cantidad-recibida-transferencia");

                if (input.value === "") {
                    hayError = true;
                    mensaje = "Debe ingresar la cantidad recibida.";
                    if (error) error.textContent = mensaje;
                    return;
                }

                if (recibido < 0) {
                    hayError = true;
                    mensaje = "La cantidad recibida no puede ser negativa.";
                    if (error) error.textContent = mensaje;
                    return;
                }

                if (recibido > enviado) {
                    hayError = true;
                    mensaje = `No puede recibir más de lo enviado. Máximo: ${enviado}.`;
                    if (error) error.textContent = mensaje;
                    return;
                }

                if (!Number.isInteger(recibido)) {
                    hayError = true;
                    mensaje = "La cantidad recibida debe ser un número entero.";
                    if (error) error.textContent = mensaje;
                    return;
                }

                if (error) {
                    error.textContent = "";
                }
            });

            if (hayError) {
                e.preventDefault();

                const primerError = formRecepcion.querySelector(".error-cantidad-recibida-transferencia:not(:empty)");

                primerError?.scrollIntoView({
                    behavior: "smooth",
                    block: "center"
                });
            }
        });
    }
});
function inicializarEditarProductosTransferencia() {
    const modal = document.getElementById("modalEditarProductosTransferencia");
    const form = document.getElementById("formEditarProductosTransferencia");
    const inputId = document.getElementById("editarIdTransferencia");
    const codigoSpan = document.getElementById("editarCodigoTransferencia");
    const contenedor = document.getElementById("contenedorEditarProductosTransferencia");
    const btnAgregar = document.getElementById("btnAgregarProductoEditarTransferencia");
    const errorGeneral = document.getElementById("errorEditarProductosTransferencia");

    if (!modal || !form || !inputId || !codigoSpan || !contenedor || !btnAgregar) {
        return;
    }

    document.addEventListener("click", async (e) => {
        const btn = e.target.closest(".btnEditarTransferencia");

        if (!btn) {
            return;
        }

        const id = btn.dataset.id;
        const codigo = btn.dataset.codigo || "TRF-00000";

        inputId.value = id;
        codigoSpan.textContent = codigo;
        errorGeneral.textContent = "";

        contenedor.innerHTML = `
            <div class="text-center text-muted py-4">
                <div class="spinner-border spinner-border-sm me-2"></div>
                Cargando productos...
            </div>
        `;

        try {
            const response = await fetch(`/interno/inventario/transferencias/detalle/${id}`);

            if (!response.ok) {
                throw new Error(await response.text());
            }

            const transferencia = await response.json();

            const estadosPermitidos = ["CREADA", "ACEPTADA", "EN_RECOJO"];

            if (!estadosPermitidos.includes(transferencia.estado)) {
                throw new Error("Esta transferencia ya no se puede editar porque el pedido ya fue preparado o está en ruta.");
            }

            contenedor.innerHTML = "";

            const detalles = transferencia.detalles || [];

            if (!detalles.length) {
                agregarFilaEditarProductoTransferencia();
                return;
            }

            detalles.forEach(d => {
                agregarFilaEditarProductoTransferencia(
                    d.idProducto,
                    d.cantidadEnviada ?? d.cantidad ?? 1
                );
            });

            actualizarBotonesEliminarEditarTransferencia();

        } catch (error) {
            contenedor.innerHTML = "";
            errorGeneral.textContent = error.message || "No se pudo cargar la transferencia.";
        }
    });

    btnAgregar.addEventListener("click", () => {
        agregarFilaEditarProductoTransferencia();
        actualizarBotonesEliminarEditarTransferencia();
    });

    contenedor.addEventListener("click", (e) => {
        const btnEliminar = e.target.closest(".btnEliminarEditarProductoTransferencia");

        if (!btnEliminar) {
            return;
        }

        btnEliminar.closest(".fila-editar-producto-transferencia").remove();
        actualizarBotonesEliminarEditarTransferencia();
    });

    contenedor.addEventListener("change", (e) => {
        const select = e.target.closest(".editar-producto-id");

        if (!select) {
            return;
        }

        const fila = select.closest(".fila-editar-producto-transferencia");
        const option = select.selectedOptions[0];

        const imagen = fila.querySelector(".editar-producto-imagen");
        const info = fila.querySelector(".editar-producto-info");

        const foto = option?.dataset.foto || "";
        const codigo = option?.dataset.codigo || "-";
        const nombre = option?.dataset.nombre || "Producto no seleccionado";
        const stock = option?.dataset.stock || "0";

        if (imagen) {
            if (foto.trim() !== "") {
                imagen.innerHTML = `
                    <img src="/uploads/productos/${escapeHtmlTransferencia(foto)}"
                         alt="Producto"
                         style="width:100%;height:100%;object-fit:cover;">
                `;
            } else {
                imagen.innerHTML = `<i class="bi bi-box-seam text-primary fs-2"></i>`;
            }
        }

        if (info) {
            info.textContent = `${codigo} - ${nombre} | Stock: ${stock}`;
        }
    });

    form.addEventListener("submit", (e) => {
        errorGeneral.textContent = "";

        const filas = Array.from(contenedor.querySelectorAll(".fila-editar-producto-transferencia"));
        const productosUsados = new Set();

        let hayError = false;
        let mensaje = "";

        if (!filas.length) {
            hayError = true;
            mensaje = "Debe agregar al menos un producto.";
        }

        filas.forEach(fila => {
            const select = fila.querySelector(".editar-producto-id");
            const cantidadInput = fila.querySelector(".editar-producto-cantidad");

            const idProducto = select.value;
            const cantidad = Number(cantidadInput.value || 0);
            const stock = Number(select.selectedOptions[0]?.dataset.stock || 0);

            if (!idProducto) {
                hayError = true;
                mensaje = "Debe seleccionar todos los productos.";
                return;
            }

            if (productosUsados.has(idProducto)) {
                hayError = true;
                mensaje = "No puedes repetir el mismo producto.";
                return;
            }

            productosUsados.add(idProducto);

            if (!cantidadInput.value || cantidad <= 0) {
                hayError = true;
                mensaje = "Todas las cantidades deben ser mayores a 0.";
                return;
            }

            if (!Number.isInteger(cantidad)) {
                hayError = true;
                mensaje = "Las cantidades deben ser números enteros.";
                return;
            }

            if (stock > 0 && cantidad > stock) {
                hayError = true;
                mensaje = `Stock insuficiente. Disponible: ${stock}.`;
            }
        });

        if (hayError) {
            e.preventDefault();
            errorGeneral.textContent = mensaje;
        }
    });

    function agregarFilaEditarProductoTransferencia(idProductoSeleccionado = "", cantidad = "") {
        const opciones = obtenerOpcionesProductosTransferencia(idProductoSeleccionado);
        const productoSeleccionado = obtenerProductoSeleccionadoTransferencia(idProductoSeleccionado);

        const foto = productoSeleccionado?.foto
            ? `/uploads/productos/${escapeHtmlTransferencia(productoSeleccionado.foto)}`
            : "";

        const codigo = productoSeleccionado?.codigo || "-";
        const nombre = productoSeleccionado?.nombre || "Producto no seleccionado";
        const stock = productoSeleccionado?.stock || "0";

        const div = document.createElement("div");
        div.className = "fila-editar-producto-transferencia border rounded-4 p-3 mb-2 bg-light";

        div.innerHTML = `
            <div class="row g-3 align-items-center">

                <div class="col-12 col-md-2">
                    <div class="rounded-4 border bg-white d-flex align-items-center justify-content-center editar-producto-imagen"
                         style="width:78px;height:78px;overflow:hidden;">
                        ${
                            foto
                                ? `<img src="${foto}" alt="Producto" style="width:100%;height:100%;object-fit:cover;">`
                                : `<i class="bi bi-box-seam text-primary fs-2"></i>`
                        }
                    </div>
                </div>

                <div class="col-12 col-md-5">
                    <label class="form-label small fw-semibold">Producto</label>

                    <select name="idProducto"
                            class="form-select rounded-3 editar-producto-id"
                            required>
                        ${opciones}
                    </select>

                    <small class="text-muted d-block mt-1 editar-producto-info">
                        ${escapeHtmlTransferencia(codigo)} - ${escapeHtmlTransferencia(nombre)} | Stock: ${escapeHtmlTransferencia(stock)}
                    </small>
                </div>

                <div class="col-8 col-md-3">
                    <label class="form-label small fw-semibold">Cantidad</label>
                    <input type="number"
                           name="cantidad"
                           class="form-control rounded-3 editar-producto-cantidad"
                           min="1"
                           step="1"
                           value="${escapeHtmlTransferencia(cantidad)}"
                           required>
                </div>

                <div class="col-4 col-md-2 d-grid">
                    <label class="form-label small fw-semibold">&nbsp;</label>
                    <button type="button"
                            class="btn btn-outline-danger rounded-3 btnEliminarEditarProductoTransferencia">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>
        `;

        contenedor.appendChild(div);
    }

    function obtenerOpcionesProductosTransferencia(idProductoSeleccionado) {
        const productos = Array.from(document.querySelectorAll(".item-producto-transferencia"));

        let html = `<option value="">Seleccionar producto</option>`;

        productos.forEach(p => {
            const id = p.dataset.id || "";
            const codigo = p.dataset.codigo || "";
            const nombre = p.dataset.nombre || "";
           const stock = p.dataset.stock || "0";
           const foto = p.dataset.foto || "";

           const selected = String(id) === String(idProductoSeleccionado) ? "selected" : "";

           html += `
               <option value="${escapeHtmlTransferencia(id)}"
                       data-stock="${escapeHtmlTransferencia(stock)}"
                       data-codigo="${escapeHtmlTransferencia(codigo)}"
                       data-nombre="${escapeHtmlTransferencia(nombre)}"
                       data-foto="${escapeHtmlTransferencia(foto)}"
                       ${selected}>
                   ${escapeHtmlTransferencia(codigo)} - ${escapeHtmlTransferencia(nombre)} | Stock: ${escapeHtmlTransferencia(stock)}
               </option>
           `;
        });

        return html;
    }

    function obtenerProductoSeleccionadoTransferencia(idProductoSeleccionado) {
        if (!idProductoSeleccionado) {
            return null;
        }

        const producto = Array.from(document.querySelectorAll(".item-producto-transferencia"))
            .find(p => String(p.dataset.id) === String(idProductoSeleccionado));

        if (!producto) {
            return null;
        }

        return {
            id: producto.dataset.id || "",
            codigo: producto.dataset.codigo || "",
            nombre: producto.dataset.nombre || "",
            stock: producto.dataset.stock || "0",
            foto: producto.dataset.foto || ""
        };
    }

    function actualizarBotonesEliminarEditarTransferencia() {
        const filas = contenedor.querySelectorAll(".fila-editar-producto-transferencia");

        filas.forEach(fila => {
            const btnEliminar = fila.querySelector(".btnEliminarEditarProductoTransferencia");

            if (btnEliminar) {
                btnEliminar.classList.toggle("d-none", filas.length === 1);
            }
        });
    }
}

function inicializarAnularTransferencia() {
    const form = document.getElementById("formAnularTransferencia");
    const codigoSpan = document.getElementById("anularCodigoTransferencia");

    if (!form || !codigoSpan) {
        return;
    }

    document.addEventListener("click", (e) => {
        const btn = e.target.closest(".btnAnularTransferencia");

        if (!btn) {
            return;
        }

        const id = btn.dataset.id;
        const codigo = btn.dataset.codigo || "TRF-00000";

        codigoSpan.textContent = codigo;
        form.action = `/interno/inventario/transferencias/${id}/anular`;
    });
}
document.addEventListener("shown.bs.collapse", function (e) {
    if (e.target.id !== "collapseHistorialCambiosTransferencia") {
        return;
    }

    const icono = document.querySelector(".icono-collapse-historial");

    if (icono) {
        icono.classList.remove("bi-chevron-down");
        icono.classList.add("bi-chevron-up");
    }
});

document.addEventListener("hidden.bs.collapse", function (e) {
    if (e.target.id !== "collapseHistorialCambiosTransferencia") {
        return;
    }

    const icono = document.querySelector(".icono-collapse-historial");

    if (icono) {
        icono.classList.remove("bi-chevron-up");
        icono.classList.add("bi-chevron-down");
    }
});
function inicializarFiltroYPaginacionTransferencias() {
    const filas = Array.from(document.querySelectorAll(".fila-transferencia-inventario"));
    const filtroEstado = document.getElementById("filtroEstadoTransferencias");
    const buscador = document.getElementById("buscadorTransferencias");

    const texto = document.getElementById("textoPaginacionTransferencias");
    const indicador = document.getElementById("indicadorPaginaTransferencias");
    const btnAnterior = document.getElementById("btnAnteriorTransferencias");
    const btnSiguiente = document.getElementById("btnSiguienteTransferencias");

    const filasPorPagina = 5;
    let paginaActual = 1;
    let filasFiltradas = [];

    if (!filas.length || !filtroEstado || !buscador || !texto || !indicador || !btnAnterior || !btnSiguiente) {
        return;
    }

    function obtenerGrupoEstado(estado) {
        estado = String(estado || "").toUpperCase();

        if (estado === "COMPLETADA" || estado === "COMPLETADA_CON_DIFERENCIAS") {
            return "COMPLETADA";
        }

        if (estado === "CANCELADA") {
            return "CANCELADA";
        }

        if (
            estado === "CREADA"
            || estado === "ACEPTADA"
            || estado === "EN_RECOJO"
            || estado === "EN_PREPARACION"
            || estado === "LISTO_PARA_SALIR"
            || estado === "EN_RUTA"
            || estado === "PENDIENTE_RECEPCION"
        ) {
            return "TRANSITO";
        }

        return "OTRO";
    }

    function normalizarBusquedaTransferencia(valor) {
        return String(valor || "")
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .trim();
    }

    function aplicarFiltro() {
        const estadoSeleccionado = filtroEstado.value || "TODOS";
        const textoBuscado = normalizarBusquedaTransferencia(buscador.value);

        filasFiltradas = filas.filter(fila => {
            const estadoFila = fila.dataset.estado || "";
            const grupoEstado = obtenerGrupoEstado(estadoFila);

            const coincideEstado =
                estadoSeleccionado === "TODOS"
                || grupoEstado === estadoSeleccionado;

            const dataBusqueda = normalizarBusquedaTransferencia(fila.dataset.busqueda || fila.textContent);

            const palabras = textoBuscado
                .split(" ")
                .map(p => p.trim())
                .filter(p => p !== "");

            const coincideBusqueda =
                palabras.length === 0
                || palabras.every(palabra => dataBusqueda.includes(palabra));

            return coincideEstado && coincideBusqueda;
        });

        paginaActual = 1;
        pintarPagina();
    }

    function pintarPagina() {
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
            ? "No hay transferencias para el filtro o búsqueda ingresada."
            : `Mostrando ${desde} - ${hasta} de ${total} transferencia(s)`;

        indicador.textContent = `Página ${paginaActual} de ${totalPaginas}`;

        btnAnterior.disabled = paginaActual <= 1;
        btnSiguiente.disabled = paginaActual >= totalPaginas;
    }

    filtroEstado.addEventListener("change", aplicarFiltro);

    buscador.addEventListener("input", () => {
        aplicarFiltro();
    });

    btnAnterior.addEventListener("click", () => {
        if (paginaActual > 1) {
            paginaActual--;
            pintarPagina();
        }
    });

    btnSiguiente.addEventListener("click", () => {
        const totalPaginas = Math.max(1, Math.ceil(filasFiltradas.length / filasPorPagina));

        if (paginaActual < totalPaginas) {
            paginaActual++;
            pintarPagina();
        }
    });

    aplicarFiltro();
}
function inicializarResumenTransferencias() {
    const filas = Array.from(document.querySelectorAll(".fila-transferencia-inventario"));

    const contadorCompletadas = document.getElementById("contadorCompletadasTransferencias");
    const contadorTransito = document.getElementById("contadorTransitoTransferencias");
    const contadorCanceladas = document.getElementById("contadorCanceladasTransferencias");

    if (!filas.length) {
        if (contadorCompletadas) contadorCompletadas.textContent = "0";
        if (contadorTransito) contadorTransito.textContent = "0";
        if (contadorCanceladas) contadorCanceladas.textContent = "0";
        return;
    }

    let completadas = 0;
    let transito = 0;
    let canceladas = 0;

    filas.forEach(fila => {
        const estado = String(fila.dataset.estado || "").toUpperCase();

        if (estado === "COMPLETADA" || estado === "COMPLETADA_CON_DIFERENCIAS") {
            completadas++;
            return;
        }

        if (estado === "CANCELADA") {
            canceladas++;
            return;
        }

        if (
            estado === "CREADA"
            || estado === "ACEPTADA"
            || estado === "EN_RECOJO"
            || estado === "EN_PREPARACION"
            || estado === "LISTO_PARA_SALIR"
            || estado === "EN_RUTA"
            || estado === "PENDIENTE_RECEPCION"
        ) {
            transito++;
        }
    });

    if (contadorCompletadas) {
        contadorCompletadas.textContent = completadas;
    }

    if (contadorTransito) {
        contadorTransito.textContent = transito;
    }

    if (contadorCanceladas) {
        contadorCanceladas.textContent = canceladas;
    }
}
