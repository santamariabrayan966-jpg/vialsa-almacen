function formatearMedidaFisica(valor) {
    const numero = Number(valor ?? 0);
    if (!Number.isFinite(numero)) return "0.000";
    return numero.toLocaleString("es-PE", { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

/* ==========================================================
   MODAL DETALLE DE INVENTARIO / EXISTENCIAS – DentIA
   ========================================================== */

document.addEventListener("DOMContentLoaded", () => {
    inicializarDetalleInventario();
});

function inicializarDetalleInventario() {
    const modalElement = document.getElementById("modalDetalleInventario");

    if (!modalElement) {
        return;
    }

    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);

    const loading = document.getElementById("detalleInventarioLoading");
    const errorBox = document.getElementById("detalleInventarioError");
    const contenido = document.getElementById("detalleInventarioContenido");

    document.querySelectorAll(".btn-ver-detalle-inventario").forEach((btn) => {
        btn.addEventListener("click", async () => {
            const idProducto = btn.dataset.idProducto;

            if (!idProducto) {
                mostrarErrorDetalleInventario("No se encontró el ID del producto.");
                modal.show();
                return;
            }

            limpiarModalDetalleInventario();
            modal.show();

            try {
                const response = await fetch(`/interno/inventario/existencias/detalle/${idProducto}`);

                if (!response.ok) {
                    const mensaje = await response.text();
                    throw new Error(mensaje || "No se pudo cargar el detalle del inventario.");
                }

                const data = await response.json();
                pintarDetalleInventario(data);

            } catch (error) {
                mostrarErrorDetalleInventario(error.message || "Ocurrió un error al cargar el detalle.");
            }
        });
    });

    function limpiarModalDetalleInventario() {
        loading.classList.remove("d-none");
        errorBox.classList.add("d-none");
        contenido.classList.add("d-none");
        errorBox.textContent = "";

        document.getElementById("detallePiezasTabla").innerHTML = "";
        document.getElementById("detallePiezasCards").innerHTML = "";


    }

    function mostrarErrorDetalleInventario(mensaje) {
        loading.classList.add("d-none");
        contenido.classList.add("d-none");
        errorBox.classList.remove("d-none");
        errorBox.textContent = mensaje;
    }
}

function pintarDetalleInventario(data) {
    const stock = data.stock || {};
    const partes = data.partes || [];
    const manejaPartes = partes.length > 0;
    const idProducto = stock.idProducto || data.idProducto;

    const btnKardex = document.getElementById("btnKardexProducto");
    const btnAjustar = document.getElementById("btnAjustarProducto");
    const btnTransferir = document.getElementById("btnTransferirProducto");
    const btnExportarDetalle = document.getElementById("btnExportarDetalleExistencia");

    if (idProducto) {
        if (btnKardex) {
            btnKardex.href = `/interno/inventario/kardex?idProducto=${idProducto}`;
        }

        if (btnAjustar) {
            btnAjustar.href = `/interno/inventario/ajustes?idProducto=${idProducto}`;
        }

        if (btnTransferir) {
            btnTransferir.href = `/interno/inventario/transferencias?idProducto=${idProducto}`;
        }

        if (btnExportarDetalle) {
            btnExportarDetalle.href = `/interno/inventario/existencias/${idProducto}/pdf`;
        }
    }

    document.getElementById("detalleNombreProducto").textContent = stock.nombreProducto || "Producto";
    document.getElementById("detalleCodigoProducto").textContent = stock.codigoProducto || "Sin código";
    document.getElementById("detalleCategoriaProducto").textContent = stock.categoria || "Sin categoría";
    document.getElementById("detalleSedeProducto").textContent = stock.nombreSede || "Sin sede";
    document.getElementById("detalleDireccionSede").textContent = stock.direccionSede || "Sin dirección";

    document.getElementById("detalleStockActual").textContent = formatoNumero(data.stockActual);
    document.getElementById("detalleCostoPromedio").textContent = formatoMoneda(data.costoPromedio);
    document.getElementById("detalleValorAprox").textContent = formatoMoneda(data.valorAprox);

    document.getElementById("detalleEquivalenteUnidadFisica").textContent =
        formatoNumero(data.equivalenteUnidadFisicaTotal, 6);

    document.getElementById("detallePartesCompletas").textContent = data.partesCompletas ?? 0;
    document.getElementById("detalleMediasPartes").textContent = data.mediasPartes ?? 0;
    document.getElementById("detalleRetazosCortes").textContent = calcularRetazosYCortes(partes);
    document.getElementById("detalleMedidaDisponible").textContent = formatearMedidaFisica(data.medidaDisponibleTotal);
    document.getElementById("detalleCostoDisponible").textContent = formatoMoneda(data.costoDisponibleTotal);

    actualizarEtiquetasDetalle(partes);

    const estado = Number(data.stockActual || 0) > Number(stock.stockMinimo || 0)
        ? "Activo"
        : "Bajo stock";

    const estadoBadge = document.getElementById("detalleEstadoProducto");
    estadoBadge.textContent = estado;
    estadoBadge.className = estado === "Activo"
        ? "badge bg-success-subtle text-success px-3 py-2"
        : "badge bg-danger-subtle text-danger px-3 py-2";

    pintarImagenProducto(stock.fotoProducto);
    const seccionComposicion = document.getElementById("seccionComposicionPartes");
    const seccionMedidaCosto = document.getElementById("seccionMedidaCostoPartes");
    const seccionTablaPartes = document.getElementById("seccionTablaPartes");
    const mensajeSinPartes = document.getElementById("mensajeSinPartes");

    if (manejaPartes) {
        seccionComposicion?.classList.remove("d-none");
        seccionMedidaCosto?.classList.remove("d-none");
        seccionTablaPartes?.classList.remove("d-none");
        mensajeSinPartes?.classList.add("d-none");
    } else {
        seccionComposicion?.classList.add("d-none");
        seccionMedidaCosto?.classList.add("d-none");
        seccionTablaPartes?.classList.add("d-none");
        mensajeSinPartes?.classList.remove("d-none");
    }
   inicializarPaginacionPartes(
       partes,
       data.largoCompletoMm,
       data.anchoCompletoMm
   );

    document.getElementById("detalleInventarioLoading").classList.add("d-none");
    document.getElementById("detalleInventarioError").classList.add("d-none");
    document.getElementById("detalleInventarioContenido").classList.remove("d-none");
}

function pintarImagenProducto(fotoProducto) {
    const wrapper = document.getElementById("detalleProductoImagenWrapper");

    if (fotoProducto) {
        wrapper.innerHTML = `
            <img src="/uploads/productos/${escapeHtml(fotoProducto)}"
                 alt="Producto"
                 style="width:100%;height:100%;object-fit:cover;">
        `;
    } else {
        wrapper.innerHTML = `<i class="bi bi-box-seam text-primary fs-1"></i>`;
    }
}

let paginaActualPartes = 1;
const partesPorPagina = 5;

function inicializarPaginacionPartes(
    partes,
    largoCompletoMm,
    anchoCompletoMm
) {
    paginaActualPartes = 1;

    renderizarPaginaPartes(
        partes,
        largoCompletoMm,
        anchoCompletoMm
    );
}

function renderizarPaginaPartes(
    partes,
    largoCompletoMm,
    anchoCompletoMm
) {
    const listaPartes = Array.isArray(partes)
        ? partes
        : [];

    const totalRegistros = listaPartes.length;

    const totalPaginas = Math.max(
        1,
        Math.ceil(totalRegistros / partesPorPagina)
    );

    if (paginaActualPartes > totalPaginas) {
        paginaActualPartes = totalPaginas;
    }

    const inicio =
        (paginaActualPartes - 1) * partesPorPagina;

    const fin =
        inicio + partesPorPagina;

    const partesPagina =
        listaPartes.slice(inicio, fin);

    pintarPartes(
        partesPagina,
        largoCompletoMm,
        anchoCompletoMm
    );

    pintarControlesPaginacionPartes(
        listaPartes,
        largoCompletoMm,
        anchoCompletoMm,
        totalPaginas
    );

    const desde =
        totalRegistros === 0
            ? 0
            : inicio + 1;

    const hasta =
        Math.min(fin, totalRegistros);

    const texto =
        `Mostrando ${desde} - ${hasta} de ${totalRegistros} registros`;

    const infoSuperior =
        document.getElementById("infoPaginacionPartes");

    const infoInferior =
        document.getElementById("infoPaginacionPartesInferior");

    if (infoSuperior) {
        infoSuperior.textContent = texto;
    }

    if (infoInferior) {
        infoInferior.textContent = texto;
    }

    const contenedor =
        document.getElementById("contenedorPaginacionPartes");

    if (contenedor) {
        contenedor.classList.toggle(
            "d-none",
            totalRegistros <= partesPorPagina
        );
    }
}

function pintarControlesPaginacionPartes(
    partes,
    largoCompletoMm,
    anchoCompletoMm,
    totalPaginas
) {
    const paginacion =
        document.getElementById("paginacionPartes");

    if (!paginacion) {
        return;
    }

    paginacion.innerHTML = "";

    const crearBoton = (
        texto,
        pagina,
        deshabilitado,
        activo = false
    ) => {
        const li =
            document.createElement("li");

        li.className =
            `page-item${deshabilitado ? " disabled" : ""}${activo ? " active" : ""}`;

        const boton =
            document.createElement("button");

        boton.type = "button";
        boton.className = "page-link";
        boton.textContent = texto;
        boton.disabled = deshabilitado;

        boton.addEventListener("click", () => {
            if (deshabilitado) {
                return;
            }

            paginaActualPartes = pagina;

            renderizarPaginaPartes(
                partes,
                largoCompletoMm,
                anchoCompletoMm
            );
        });

        li.appendChild(boton);
        paginacion.appendChild(li);
    };

    crearBoton(
        "Anterior",
        paginaActualPartes - 1,
        paginaActualPartes === 1
    );

    for (
        let pagina = 1;
        pagina <= totalPaginas;
        pagina++
    ) {
        crearBoton(
            String(pagina),
            pagina,
            false,
            pagina === paginaActualPartes
        );
    }

    crearBoton(
        "Siguiente",
        paginaActualPartes + 1,
        paginaActualPartes === totalPaginas
    );
}

function pintarPartes(partes, largoCompletoMm, anchoCompletoMm) {
    const tabla = document.getElementById("detallePiezasTabla");
    const cards = document.getElementById("detallePiezasCards");

    if (!partes.length) {
        tabla.innerHTML = `
            <tr>
                <td colspan="10" class="text-center text-muted py-4">
                    Este producto no maneja partes físicas detalladas.
                </td>
            </tr>
        `;

        cards.innerHTML = `
            <div class="text-center text-muted py-4">
                Este producto no maneja partes físicas detalladas.
            </div>
        `;
        return;
    }

    tabla.innerHTML = partes.map((p) => `
        <tr>
            <td>
                <span class="badge bg-primary-subtle text-primary">
                    ${escapeHtml(p.tipoParte || "-")}
                </span>
            </td>
            <td>${p.cantidadPartes ?? 1}</td>
            <td>${escapeHtml(p.tipoMedida || "-")}</td>
            <td>${escapeHtml(p.unidadMedida || "-")}</td>
            <td>${formatearMedidaFisica(p.medidaFisicaDisponible ?? p.medidaDisponible)}</td>
            <td>${p.medidaPorParte ?? 0}</td>
            <td>${formatoDimensionesFisicas(
                      p,
                      largoCompletoMm,
                      anchoCompletoMm
                  )}</td>
            <td>${formatoNumero(p.equivalenteUnidadFisica, 6)}</td>
            <td>S/ ${formatoMoneda(p.costoTotalDisponible)}</td>
            <td>
                <span class="badge bg-success-subtle text-success">
                    ${escapeHtml(p.estado || "-")}
                </span>
            </td>
        </tr>
    `).join("");

    cards.innerHTML = partes.map((p) => `
        <div class="border rounded-4 p-3 mb-2 bg-white">
            <div class="d-flex justify-content-between gap-2 mb-2">
                <strong>${escapeHtml(p.tipoParte || "Parte")}</strong>
                <span class="badge bg-success-subtle text-success">${escapeHtml(p.estado || "-")}</span>
            </div>

            <div class="small text-muted">Cantidad: <strong>${p.cantidadPartes ?? 1}</strong></div>
            <div class="small text-muted">Tipo medida: <strong>${escapeHtml(p.tipoMedida || "-")}</strong></div>
            <div class="small text-muted">Unidad: <strong>${escapeHtml(p.unidadMedida || "-")}</strong></div>
            <div class="small text-muted">Medida disponible: <strong>${formatearMedidaFisica(p.medidaFisicaDisponible ?? p.medidaDisponible)}</strong></div>
            <div class="small text-muted">Medida por parte: <strong>${p.medidaPorParte ?? 0}</strong></div>
            <div class="small text-muted">Dimensiones físicas: <strong>${formatoDimensionesFisicas(
                                                                             p,
                                                                             largoCompletoMm,
                                                                             anchoCompletoMm
                                                                         )}</strong></div>
            <div class="small text-muted">Equiv. unidad física: <strong>${formatoNumero(p.equivalenteUnidadFisica, 6)}</strong></div>
            <div class="small text-muted">Costo disponible: <strong>S/ ${formatoMoneda(p.costoTotalDisponible)}</strong></div>
        </div>
    `).join("");
}

function calcularRetazosYCortes(partes) {
    return partes.filter(p =>
        String(p.tipoParte || "").toUpperCase() === "RETAZO" ||
        String(p.tipoParte || "").toUpperCase() === "CORTE"
    ).length;
}

function actualizarEtiquetasDetalle(partes) {
    const primeraParte = partes && partes.length ? partes[0] : null;

    const tipoMedida = primeraParte?.tipoMedida
        ? String(primeraParte.tipoMedida).toUpperCase()
        : "";

    const unidadMedida = primeraParte?.unidadMedida
        ? String(primeraParte.unidadMedida).toUpperCase()
        : "";

    const esLongitud = tipoMedida === "LONGITUD";

    setTexto("lblEquivalenteUnidadFisica", esLongitud ? "Equiv. unidades físicas" : "Equiv. planchas");
    setTexto("lblPartesCompletas", esLongitud ? "Unidades completas" : "Planchas completas");
    setTexto("lblMediasPartes", esLongitud ? "Medias unidades" : "Medias planchas");
    setTexto("lblRetazosCortes", esLongitud ? "Cortes" : "Retazos");
    setTexto("lblMedidaDisponible", esLongitud ? `${unidadMedida || "METRO"} disponible total` : "Pie² vendible total");
    setTexto("tituloUltimasPartes", "Últimas partes disponibles");
}

function setTexto(id, texto) {
    const el = document.getElementById(id);
    if (el) {
        el.textContent = texto;
    }
}

function formatoNumero(valor, decimales = 2) {
    const numero = Number(valor || 0);

    return numero.toLocaleString("es-PE", {
        minimumFractionDigits: decimales,
        maximumFractionDigits: decimales
    });
}

function formatoMoneda(valor) {
    const numero = Number(valor || 0);

    return numero.toLocaleString("es-PE", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}


function formatoDimensionesFisicas(
    parte,
    largoCompletoMm,
    anchoCompletoMm
) {
    let largo = Number(parte?.largoDisponibleMm || 0);
    let ancho = Number(parte?.anchoDisponibleMm || 0);

    const tipoParte = String(parte?.tipoParte || "")
        .trim()
        .toUpperCase();

    /*
     * RETAZO y CORTE usan sus dimensiones reales guardadas.
     * COMPLETA y MEDIA usan las medidas base del producto.
     */
    if (largo <= 0 || ancho <= 0) {
        const largoBase = Number(largoCompletoMm || 0);
        const anchoBase = Number(anchoCompletoMm || 0);

        if (tipoParte === "COMPLETA") {
            largo = largoBase;
            ancho = anchoBase;
        } else if (tipoParte === "MEDIA") {
            largo = largoBase;
            ancho = anchoBase / 2;
        }
    }

    if (largo <= 0 || ancho <= 0) {
        return '<span class="text-muted">—</span>';
    }

    return `${formatoNumero(largo, 0)} × ${formatoNumero(ancho, 0)} mm`;
}

function formatoFecha(valor) {
    if (!valor) {
        return "-";
    }

    const fecha = new Date(valor);

    if (Number.isNaN(fecha.getTime())) {
        return valor;
    }

    return fecha.toLocaleString("es-PE", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function escapeHtml(valor) {
    return String(valor ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

/* ==========================================================
   BUSCADOR, FILTROS Y PAGINACIÓN DE EXISTENCIAS
   ========================================================== */

document.addEventListener("DOMContentLoaded", () => {
    inicializarTablaInventario();
});

let paginaInventario = 1;
const registrosPorPaginaInventario = 5;

function inicializarTablaInventario() {
    const inputBuscar = document.getElementById("buscarInventario");
    const filtroCategoria = document.getElementById("filtroCategoriaInventario");
    const filtroEstado = document.getElementById("filtroEstadoInventario");
    const btnActualizar = document.getElementById("btnActualizarInventario");
    const btnAnterior = document.getElementById("btnInventarioAnterior");
    const btnSiguiente = document.getElementById("btnInventarioSiguiente");

    if (!inputBuscar || !filtroCategoria || !filtroEstado) {
        return;
    }

    inputBuscar.addEventListener("input", () => {
        paginaInventario = 1;
        aplicarFiltrosInventario();
    });

    filtroCategoria.addEventListener("change", () => {
        paginaInventario = 1;
        aplicarFiltrosInventario();
    });

    filtroEstado.addEventListener("change", () => {
        paginaInventario = 1;
        aplicarFiltrosInventario();
    });

    if (btnActualizar) {
        btnActualizar.addEventListener("click", () => {
            window.location.reload();
        });
    }

    if (btnAnterior) {
        btnAnterior.addEventListener("click", () => {
            if (paginaInventario > 1) {
                paginaInventario--;
                aplicarFiltrosInventario();
            }
        });
    }

    if (btnSiguiente) {
        btnSiguiente.addEventListener("click", () => {
            const totalPaginas = obtenerTotalPaginasInventario();
            if (paginaInventario < totalPaginas) {
                paginaInventario++;
                aplicarFiltrosInventario();
            }
        });
    }

    aplicarFiltrosInventario();
}

function obtenerFilasInventario() {
    return Array.from(document.querySelectorAll(".fila-inventario"));
}

function obtenerFilasFiltradasInventario() {
    const textoBusqueda = normalizarTexto(document.getElementById("buscarInventario")?.value || "");
    const categoria = document.getElementById("filtroCategoriaInventario")?.value || "";
    const estado = document.getElementById("filtroEstadoInventario")?.value || "";

    return obtenerFilasInventario().filter((fila) => {
        const producto = normalizarTexto(fila.dataset.producto || "");
        const codigo = normalizarTexto(fila.dataset.codigo || "");
        const categoriaFila = fila.dataset.categoria || "";
        const estadoFila = fila.dataset.estado || "";

        const coincideBusqueda =
            !textoBusqueda ||
            producto.includes(textoBusqueda) ||
            codigo.includes(textoBusqueda);

        const coincideCategoria =
            !categoria ||
            categoriaFila === categoria;

        const coincideEstado =
            !estado ||
            estadoFila === estado;

        return coincideBusqueda && coincideCategoria && coincideEstado;
    });
}

function aplicarFiltrosInventario() {
    const filas = obtenerFilasInventario();
    const filtradas = obtenerFilasFiltradasInventario();

    filas.forEach((fila) => {
        fila.classList.add("d-none");
    });

    const totalRegistros = filtradas.length;
    const totalPaginas = Math.max(1, Math.ceil(totalRegistros / registrosPorPaginaInventario));

    if (paginaInventario > totalPaginas) {
        paginaInventario = totalPaginas;
    }

    const inicio = (paginaInventario - 1) * registrosPorPaginaInventario;
    const fin = inicio + registrosPorPaginaInventario;

    const visibles = filtradas.slice(inicio, fin);

    visibles.forEach((fila) => {
        fila.classList.remove("d-none");
    });

    actualizarContadoresInventario(totalRegistros, visibles.length, totalPaginas);
}

function obtenerTotalPaginasInventario() {
    const totalRegistros = obtenerFilasFiltradasInventario().length;
    return Math.max(1, Math.ceil(totalRegistros / registrosPorPaginaInventario));
}

function actualizarContadoresInventario(totalRegistros, mostrando, totalPaginas) {
    const contadorMostrando = document.getElementById("contadorInventarioMostrando");
    const contadorTotal = document.getElementById("contadorInventarioTotal");
    const paginaActual = document.getElementById("paginaInventarioActual");
    const paginaTotal = document.getElementById("paginaInventarioTotal");
    const btnAnterior = document.getElementById("btnInventarioAnterior");
    const btnSiguiente = document.getElementById("btnInventarioSiguiente");

    if (contadorMostrando) {
        contadorMostrando.textContent = mostrando;
    }

    if (contadorTotal) {
        contadorTotal.textContent = totalRegistros;
    }

    if (paginaActual) {
        paginaActual.textContent = paginaInventario;
    }

    if (paginaTotal) {
        paginaTotal.textContent = totalPaginas;
    }

    if (btnAnterior) {
        btnAnterior.disabled = paginaInventario <= 1;
    }

    if (btnSiguiente) {
        btnSiguiente.disabled = paginaInventario >= totalPaginas;
    }
}

function normalizarTexto(valor) {
    return String(valor || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}