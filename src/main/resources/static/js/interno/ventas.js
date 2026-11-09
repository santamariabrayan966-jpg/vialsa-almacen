/* ============================================================================
   ventas.js – DentIA ERP (VERSIÓN FINAL PROFESIONAL)
============================================================================ */

/* ============================================================================
   CSRF SPRING SECURITY
============================================================================ */
const CSRF_TOKEN  = document.querySelector("meta[name='_csrf']")?.content;
const CSRF_HEADER = document.querySelector("meta[name='_csrf_header']")?.content;

/* ============================================================================
   UTILIDADES
============================================================================ */
const f2 = n => parseFloat(Number(n || 0).toFixed(2));

/* ============================================================================
   FORMULARIO PRINCIPAL
============================================================================ */
document.addEventListener("DOMContentLoaded", () => {

    /* ----------------------------------------------------------------------
       ELEMENTOS DEL FORMULARIO
    ---------------------------------------------------------------------- */
    const tablaBody      = document.querySelector("#detalleBody");
    const btnAgregar     = document.getElementById("agregarProducto");
    const btnAgregarMedidas = document.getElementById("agregarPorMedidas");

    const formaPago      = document.getElementById("formaPago");
    const bloqueCredito  = document.getElementById("bloqueCredito");
    const pagoInicial    = document.getElementById("pagoInicial");
    const deudaVenta     = document.getElementById("deudaVenta");
    const numeroCuotas = document.getElementById("numeroCuotas");
    const frecuenciaCuotas = document.getElementById("frecuenciaCuotas");
    const fechaPrimeraCuota = document.getElementById("fechaPrimeraCuota");
    const btnGenerarCuotas = document.getElementById("btnGenerarCuotas");
    const cuotasPreviewBody = document.getElementById("cuotasPreviewBody");

    const totalVentaInput = document.getElementById("totalVentaInput");
    const lblTotal        = document.getElementById("lblTotal");

    // CLIENTE
    const inputDoc   = document.getElementById("cliDocumento");
    const inputNom   = document.getElementById("cliNombre");
    const inputTel   = document.getElementById("cliTelefono");
    const inputIdCli = document.getElementById("idCliente");
    const btnBuscarCliente = document.getElementById("btnBuscarCliente");
    const inputCorreo = document.getElementById("cliCorreo");
    const inputDireccion = document.getElementById("cliDireccion");
    const inputOrigenCliente = document.getElementById("clienteOrigen");
    const inputClienteNombres = document.getElementById("clienteNombres");
    const inputClienteApellidos = document.getElementById("clienteApellidos");
    const inputClienteRazonSocial = document.getElementById("clienteRazonSocial");
    const clienteConsultaEstado = document.getElementById("clienteConsultaEstado");
    const clienteConsultaBadge = document.getElementById("clienteConsultaBadge");
    const clienteConsultaMensaje = document.getElementById("clienteConsultaMensaje");
    let consultaClienteEnCurso = false;
    let documentoClienteConsultado = inputIdCli?.value && inputDoc?.value ? inputDoc.value.trim() : "";

    // COMPROBANTE
    const tipoComp = document.getElementById("tipoComprobante");
    const nroInput = document.getElementById("nroComprobante");

    const formVenta = document.getElementById("ventaForm");

    const tipoEntrega = document.getElementById("tipoEntrega");
    const bloqueRecojoProgramado = document.getElementById("bloqueRecojoProgramado");
    const fechaRecojoProgramadaInput = document.getElementById("fechaRecojoProgramadaInput");

    const fechaRecojoProgramadaFecha = document.getElementById("fechaRecojoProgramadaFecha");
    const horariosRecojoContainer = document.getElementById("horariosRecojoContainer");
    const alertaRecojoProgramado = document.getElementById("alertaRecojoProgramado");
    const mensajeRecojoProgramado = document.getElementById("mensajeRecojoProgramado");
    const resumenRecojoProgramado = document.getElementById("resumenRecojoProgramado");
    const textoRecojoSeleccionado = document.getElementById("textoRecojoSeleccionado");
    // PREPARACIÓN / CORTE
    const modalPreparacionElement = document.getElementById("modalPreparacionProducto");
    const modalPreparacion = modalPreparacionElement
        ? bootstrap.Modal.getOrCreateInstance(modalPreparacionElement)
        : null;
    let modalPreparacionEnApertura = false;
    modalPreparacionElement?.addEventListener("shown.bs.modal", () => {
        modalPreparacionEnApertura = false;
    });
    modalPreparacionElement?.addEventListener("hidden.bs.modal", () => {
        modalPreparacionEnApertura = false;

        filaPreparacionActual = null;

        prepSeleccionProductoPanel?.classList.add("d-none");
        prepContenidoTecnico?.classList.remove("d-none");
        if (prepSeleccionAyuda) prepSeleccionAyuda.textContent = "";
        if (prepPresentacionSelect) {
            prepPresentacionSelect.innerHTML = '<option value="">Selecciona primero un producto</option>';
            prepPresentacionSelect.disabled = true;
        }
        if (prepProductoSelect && window.jQuery) {
            try { $(prepProductoSelect).val(null).trigger("change"); } catch (e) { /* sin acción */ }
        }
    });

    const prepFilaIndex = document.getElementById("prepFilaIndex");
    const prepSeleccionProductoPanel = document.getElementById("prepSeleccionProductoPanel");
    const prepProductoSelect = document.getElementById("prepProductoSelect");
    const prepPresentacionSelect = document.getElementById("prepPresentacionSelect");
    const prepSeleccionAyuda = document.getElementById("prepSeleccionAyuda");
    const prepContenidoTecnico = document.getElementById("prepContenidoTecnico");
    const prepTipo = document.getElementById("prepTipo");
    const prepEntregarSobrante = document.getElementById("prepEntregarSobrante");
    const prepObservacion = document.getElementById("prepObservacion");
    const prepCortesBody = document.getElementById("prepCortesBody");
    const prepCroquisPreview = document.getElementById("prepCroquisPreview");
    const btnAgregarCorte = document.getElementById("btnAgregarCorte");
    const btnGuardarPreparacion = document.getElementById("btnGuardarPreparacion");

    const prepResumenProducto = document.getElementById("prepResumenProducto");
    const prepPiezaActivaInput = document.getElementById("prepPiezaActiva");
    const prepPiezasTabs = document.getElementById("prepPiezasTabs");
    const prepTituloPieza = document.getElementById("prepTituloPieza");
    const prepSubtituloPieza = document.getElementById("prepSubtituloPieza");
    const prepMedidaBaseTexto = document.getElementById("prepMedidaBaseTexto");
    const prepBadgeEstado = document.getElementById("prepBadgeEstado");
    const prepErroresPieza = document.getElementById("prepErroresPieza");
    const prepAlertaInline = document.getElementById("prepAlertaInline");
    const prepAlertaTexto = document.getElementById("prepAlertaTexto");
    const prepTextoAyuda = document.getElementById("prepTextoAyuda");
    const prepResumenMedidas = document.getElementById("prepResumenMedidas");
    const btnVerCroquisGrande = document.getElementById("btnVerCroquisGrande");
    const croquisGrandeContenido = document.getElementById("croquisGrandeContenido");
    const croquisGrandeSubtitulo = document.getElementById("croquisGrandeSubtitulo");
    const modalCroquisGrandeElement = document.getElementById("modalCroquisGrande");
    const modalCroquisGrande = modalCroquisGrandeElement
        ? bootstrap.Modal.getOrCreateInstance(modalCroquisGrandeElement)
        : null;

    let filaPreparacionActual = null;
    let piezaActiva = 1;

    function limpiarRecojoProgramado() {
        if (fechaRecojoProgramadaInput) {
            fechaRecojoProgramadaInput.value = "";
        }

        if (resumenRecojoProgramado) {
            resumenRecojoProgramado.classList.add("d-none");
        }

        if (textoRecojoSeleccionado) {
            textoRecojoSeleccionado.textContent = "";
        }

        if (alertaRecojoProgramado) {
            alertaRecojoProgramado.classList.add("d-none");
        }
    }

    function mostrarAlertaRecojo(mensaje) {
        if (mensajeRecojoProgramado) {
            mensajeRecojoProgramado.textContent = mensaje;
        }

        if (alertaRecojoProgramado) {
            alertaRecojoProgramado.classList.remove("d-none");
        }
    }

    function pintarMensajeHorarios(mensaje, tipo = "muted") {
        if (!horariosRecojoContainer) return;

        horariosRecojoContainer.innerHTML = `
            <div class="text-${tipo} small border rounded-3 px-3 py-2 bg-white">
                ${mensaje}
            </div>
        `;
    }

    function actualizarTipoEntrega() {
        if (!tipoEntrega || !bloqueRecojoProgramado) return;

        if (tipoEntrega.value === "PROGRAMADA") {
            bloqueRecojoProgramado.classList.remove("d-none");

            if (fechaRecojoProgramadaInput) {
                fechaRecojoProgramadaInput.setAttribute("required", "required");
            }

            const hoy = new Date().toISOString().split("T")[0];

            if (fechaRecojoProgramadaFecha) {
                fechaRecojoProgramadaFecha.min = hoy;

                if (!fechaRecojoProgramadaFecha.value) {
                    fechaRecojoProgramadaFecha.value = hoy;
                }

                cargarHorariosRecojoDisponibles();
            }

        } else {
            bloqueRecojoProgramado.classList.add("d-none");

            if (fechaRecojoProgramadaInput) {
                fechaRecojoProgramadaInput.removeAttribute("required");
            }

            limpiarRecojoProgramado();
            pintarMensajeHorarios("Selecciona una fecha para cargar los horarios.");
        }
    }

    async function cargarHorariosRecojoDisponibles() {
        if (!fechaRecojoProgramadaFecha || !horariosRecojoContainer) return;

        const fecha = fechaRecojoProgramadaFecha.value;

        limpiarRecojoProgramado();

        if (!fecha) {
            pintarMensajeHorarios("Selecciona una fecha para cargar los horarios.");
            return;
        }

        pintarMensajeHorarios("Cargando horarios disponibles...", "primary");

        try {
            const resp = await fetch(`/interno/ventas/horarios-disponibles?fecha=${fecha}`);

            if (!resp.ok) {
                pintarMensajeHorarios("No se pudieron cargar los horarios. Intenta nuevamente.", "danger");
                return;
            }

            const horarios = await resp.json();

            if (!horarios || horarios.length === 0) {
                pintarMensajeHorarios("No hay horarios disponibles para esta fecha.", "warning");
                return;
            }

            horariosRecojoContainer.innerHTML = "";

            horarios.forEach(h => {
                const hora = h.horaTexto || h.hora;
                const cupos = h.cuposDisponibles ?? 0;
                const capacidad = h.capacidad ?? 0;

                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "btn btn-outline-primary btn-sm rounded-pill px-3 horario-recojo-btn";
                btn.dataset.hora = hora;

                btn.innerHTML = `
                    <i class="bi bi-clock me-1"></i>
                    ${hora}
                    <span class="badge text-bg-light ms-1">${cupos}/${capacidad}</span>
                `;

                btn.addEventListener("click", () => {
                    document.querySelectorAll(".horario-recojo-btn").forEach(b => {
                        b.classList.remove("btn-primary", "text-white");
                        b.classList.add("btn-outline-primary");
                    });

                    btn.classList.remove("btn-outline-primary");
                    btn.classList.add("btn-primary", "text-white");

                    if (fechaRecojoProgramadaInput) {
                        fechaRecojoProgramadaInput.value = `${fecha}T${hora}`;
                    }

                    if (textoRecojoSeleccionado) {
                        textoRecojoSeleccionado.textContent = `${fecha} a las ${hora}`;
                    }

                    if (resumenRecojoProgramado) {
                        resumenRecojoProgramado.classList.remove("d-none");
                    }

                    if (alertaRecojoProgramado) {
                        alertaRecojoProgramado.classList.add("d-none");
                    }
                });

                horariosRecojoContainer.appendChild(btn);
            });

        } catch (e) {
            console.error("Error cargando horarios de recojo:", e);
            pintarMensajeHorarios("Error al cargar horarios disponibles.", "danger");
        }
    }

    tipoEntrega?.addEventListener("change", actualizarTipoEntrega);
    fechaRecojoProgramadaFecha?.addEventListener("change", cargarHorariosRecojoDisponibles);

    actualizarTipoEntrega();

    if (!tablaBody) return;

/* ============================================================================
   PREPARACIÓN / CORTE DE PRODUCTOS
============================================================================ */
function obtenerTipoMedidaFila(tr) {
    const tipoMedidaProducto = (tr.dataset.tipoMedidaProducto || "").toUpperCase();

    if (tipoMedidaProducto === "AREA" || tipoMedidaProducto === "LONGITUD") {
        return tipoMedidaProducto;
    }

    const presentacionSelect = tr.querySelector(".presentacion-select");
    const opt = presentacionSelect?.options[presentacionSelect.selectedIndex];

    const textoPresentacion = (opt?.textContent || "").toLowerCase();
    const textoProducto = (
        tr.querySelector(".seleccionar-producto option:checked")?.textContent ||
        tr.querySelector(".seleccionar-producto")?.textContent ||
        ""
    ).toLowerCase();

    const unidad = (
        tr.dataset.unidadVentaNombre ||
        tr.dataset.unidadVentaAbrev ||
        ""
    ).toLowerCase();

    const texto = `${unidad} ${textoPresentacion} ${textoProducto}`;

    if (
        texto.includes("pie") ||
        texto.includes("pie²") ||
        texto.includes("m2") ||
        texto.includes("m²") ||
        texto.includes("metro cuadrado") ||
        texto.includes("vidrio") ||
        texto.includes("plancha")
    ) {
        return "AREA";
    }

    if (
        texto.includes("metro lineal") ||
        texto.includes("lineal") ||
        texto.includes("metro") ||
        texto.includes("ml") ||
        texto.includes("varilla") ||
        texto.includes("aluminio") ||
        texto.includes("perfil") ||
        texto.includes("regla")
    ) {
        return "LONGITUD";
    }

    return "CANTIDAD";
}

function obtenerNombrePiezaFisica(tipoMedida) {
    if (tipoMedida === "AREA") return "Plancha";
    if (tipoMedida === "LONGITUD") return "Varilla";
    return "Pieza";
}

function obtenerCantidadPiezasFisicas(tr) {
    if (!tr) return 1;

    const calculadas = Number(tr.dataset.unidadesFisicasCalculadas || 0);
    if (Number.isInteger(calculadas) && calculadas > 0) return calculadas;

    const cantidadVendida = Number(tr.querySelector(".cantidad")?.value || 1);
    const cantidadIncluida = Number(tr.dataset.cantidadIncluida || 1);
    const medidaTotalBase = Number(tr.dataset.medidaTotalBase || 0);
    const factor = Number(tr.querySelector(".factor-stock-hidden")?.value || 1);
    const tipoCalculoVenta = (tr.dataset.tipoCalculoVenta || "").toUpperCase();

    if (
        tipoCalculoVenta === "METRO_LINEAL" ||
        (factor < 1 && medidaTotalBase === 1)
    ) {
        return 1;
    }

    return Math.max(1, Math.round(cantidadVendida * cantidadIncluida));
}

function obtenerMaximoPorPieza(tr, tipoMedida) {
    const cantidadVendida = Number(tr.querySelector(".cantidad")?.value || 1);
    const medidaTotalBase = Number(tr.dataset.medidaTotalBase || 0);
    const cantidadPiezas = obtenerCantidadPiezasFisicas(tr);

    if (tipoMedida === "AREA" && esVentaPorAreaSuelta(tr)) {
        return cantidadVendida * medidaTotalBase;
    }

    if (cantidadPiezas > 1 && medidaTotalBase > 0) {
        return medidaTotalBase;
    }

    if (medidaTotalBase > 0) {
        return cantidadVendida * medidaTotalBase;
    }

    return cantidadVendida;
}

function nombreUnidadValidacion(tipoMedida) {
    if (tipoMedida === "LONGITUD") return "m";
    if (tipoMedida === "AREA") return "pie²";
    return "unidades";
}

function calcularAreaPie2(cortes) {
    let totalM2 = 0;

    cortes.forEach(c => {
        const anchoM = Number(c.anchoCm || 0) / 100;
        const altoM = Number(c.altoCm || 0) / 100;
        const cantidad = Number(c.cantidad || 0);

        totalM2 += anchoM * altoM * cantidad;
    });

    const pie2 = totalM2 * 10.7639;

    return {
        m2: Number(totalM2.toFixed(4)),
        pie2: Number(pie2.toFixed(2))
    };
}

function obtenerCortesDesdeModal() {
    const cortes = [];

    const tipoMedida = filaPreparacionActual
        ? obtenerTipoMedidaFila(filaPreparacionActual)
        : "AREA";

    prepCortesBody?.querySelectorAll(".prep-corte-card").forEach(card => {
        const cantidad = Number(card.querySelector(".corte-cantidad")?.value || 0);
        const piezaOrigen = Number(card.dataset.piezaOrigen || piezaActiva || 1);

        if (tipoMedida === "LONGITUD") {
            const longitud = Number(card.querySelector(".corte-longitud")?.value || 0);

            if (longitud > 0 && cantidad > 0) {
                cortes.push({
                    tipo: "LONGITUD",
                    piezaOrigen,
                    longitudM: longitud,
                    cantidad
                });
            }

            return;
        }

        const ancho = Number(card.querySelector(".corte-ancho")?.value || 0);
        const alto = Number(card.querySelector(".corte-alto")?.value || 0);

        if (ancho > 0 && alto > 0 && cantidad > 0) {
            cortes.push({
                tipo: "AREA",
                piezaOrigen,
                anchoCm: ancho,
                altoCm: alto,
                cantidad
            });
        }
    });

    return cortes;
}

function obtenerTodosLosCortesGuardados() {
    const cortesActuales = obtenerCortesDesdeModal();

    const cortesAnteriores = filaPreparacionActual
        ? filaPreparacionActual.querySelector(".cortes-json-hidden")?.value || ""
        : "";

    let cortes = [];

    if (cortesAnteriores) {
        try {
            cortes = JSON.parse(cortesAnteriores);
        } catch (e) {
            cortes = [];
        }
    }

    cortes = cortes.filter(c => Number(c.piezaOrigen || 1) !== Number(piezaActiva));

    return [...cortes, ...cortesActuales];
}

function cortesDePieza(cortes, pieza) {
    return (cortes || []).filter(c => Number(c.piezaOrigen || 1) === Number(pieza));
}

function calcularUsoPorPieza(cortes, tipoMedida) {
    if (tipoMedida === "LONGITUD") {
        return cortes.reduce((acc, c) => {
            return acc + (Number(c.longitudM || 0) * Number(c.cantidad || 0));
        }, 0);
    }

    if (tipoMedida === "AREA") {
        return calcularAreaPie2(cortes).pie2;
    }

    return cortes.reduce((acc, c) => acc + Number(c.cantidad || 0), 0);
}

function validarCortesPorPiezaFisica(tr, cortes, tipoMedida) {
    const cantidadPiezas = obtenerCantidadPiezasFisicas(tr);
    const maximoPorPieza = obtenerMaximoPorPieza(tr, tipoMedida);
    const nombrePieza = obtenerNombrePiezaFisica(tipoMedida);
    const unidad = nombreUnidadValidacion(tipoMedida);

    const acumulado = {};

    for (const c of cortes) {
        const pieza = Number(c.piezaOrigen || 1);

        if (pieza < 1 || pieza > cantidadPiezas) {
            return {
                ok: false,
                pieza,
                mensaje: `${nombrePieza} ${pieza} no existe para esta presentación.`
            };
        }

        if (!acumulado[pieza]) {
            acumulado[pieza] = 0;
        }

        if (tipoMedida === "LONGITUD") {
            acumulado[pieza] += Number(c.longitudM || 0) * Number(c.cantidad || 0);
        } else if (tipoMedida === "AREA") {
            acumulado[pieza] += calcularAreaPie2([c]).pie2;
        } else {
            acumulado[pieza] += Number(c.cantidad || 0);
        }
    }

    for (const pieza in acumulado) {
        if (acumulado[pieza] > maximoPorPieza) {
            return {
                ok: false,
                pieza: Number(pieza),
                mensaje: `${nombrePieza} ${pieza} supera su medida real. Disponible: ${maximoPorPieza.toFixed(2)} ${unidad}. Usado: ${acumulado[pieza].toFixed(2)} ${unidad}.`
            };
        }
    }

    return {
        ok: true,
        pieza: null,
        mensaje: ""
    };
}

function validarMedidasIndividuales(tr, cortes, tipoMedida) {
    const largoBase = Number(tr.dataset.largoBase || 0);
    const anchoBase = Number(tr.dataset.anchoBase || 0);
    const medidaTotalBase = Number(tr.dataset.medidaTotalBase || 0);

    if (tipoMedida === "AREA" && largoBase > 0 && anchoBase > 0) {
        for (const c of cortes) {
            const ancho = Number(c.anchoCm || 0);
            const alto = Number(c.altoCm || 0);

            const cabeNormal = ancho <= largoBase && alto <= anchoBase;
            const cabeGirado = ancho <= anchoBase && alto <= largoBase;

            if (!cabeNormal && !cabeGirado) {
                return {
                    ok: false,
                    pieza: Number(c.piezaOrigen || 1),
                    mensaje: `El corte ${ancho} x ${alto} cm no cabe en la medida base ${largoBase} x ${anchoBase} cm.`
                };
            }
        }
    }

    if (tipoMedida === "LONGITUD" && medidaTotalBase > 0) {
        for (const c of cortes) {
            const longitud = Number(c.longitudM || 0);

            if (longitud > medidaTotalBase) {
                return {
                    ok: false,
                    pieza: Number(c.piezaOrigen || 1),
                    mensaje: `El corte de ${longitud} m supera la medida base de ${medidaTotalBase} m.`
                };
            }
        }
    }

    return {
        ok: true,
        pieza: null,
        mensaje: ""
    };
}

function pintarTabsPiezas(cortes = []) {
    if (!prepPiezasTabs || !filaPreparacionActual) return;

    const tipoMedida = obtenerTipoMedidaFila(filaPreparacionActual);
    const nombrePieza = obtenerNombrePiezaFisica(tipoMedida);
    const cantidadPiezas = obtenerCantidadPiezasFisicas(filaPreparacionActual);

    prepPiezasTabs.innerHTML = "";

    const validacion = validarCortesPorPiezaFisica(filaPreparacionActual, cortes, tipoMedida);
    const piezaError = validacion.ok ? null : validacion.pieza;

    for (let i = 1; i <= cantidadPiezas; i++) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "prep-pieza-tab";
        btn.dataset.pieza = i;
        btn.textContent = `${nombrePieza} ${i}`;

        if (i === piezaActiva) {
            btn.classList.add("active");
        }

        if (piezaError === i) {
            btn.classList.add("error");
            btn.innerHTML = `<i class="bi bi-exclamation-triangle me-1"></i>${nombrePieza} ${i}`;
        }

        btn.addEventListener("click", () => {
            guardarCortesPiezaActualEnHidden();
            piezaActiva = i;

            if (prepPiezaActivaInput) {
                prepPiezaActivaInput.value = i;
            }

            pintarModalDesdeHidden();
        });

        prepPiezasTabs.appendChild(btn);
    }
}

function guardarCortesPiezaActualEnHidden() {
    if (!filaPreparacionActual) return;

    const hidden = filaPreparacionActual.querySelector(".cortes-json-hidden");
    if (!hidden) return;

    const actuales = obtenerCortesDesdeModal();

    let anteriores = [];

    if (hidden.value) {
        try {
            anteriores = JSON.parse(hidden.value);
        } catch (e) {
            anteriores = [];
        }
    }

    anteriores = anteriores.filter(c => Number(c.piezaOrigen || 1) !== Number(piezaActiva));

    hidden.value = JSON.stringify([...anteriores, ...actuales]);
}

function pintarCortesDePieza(cortes = []) {
    if (!prepCortesBody || !filaPreparacionActual) return;

    const tipoMedida = obtenerTipoMedidaFila(filaPreparacionActual);
    prepCortesBody.innerHTML = "";

    const cortesActuales = cortesDePieza(cortes, piezaActiva);

    if (!cortesActuales || cortesActuales.length === 0) {
        prepCortesBody.innerHTML = `
            <div class="text-center text-muted small py-4">
                Sin cortes registrados para esta pieza.
            </div>
        `;
        return;
    }

    cortesActuales.forEach((c, index) => {
        agregarFilaCorte({
            index: index + 1,
            tipoMedida,
            piezaOrigen: piezaActiva,
            anchoCm: c.anchoCm || "",
            altoCm: c.altoCm || "",
            longitudM: c.longitudM || "",
            cantidad: c.cantidad || 1,
            actualizar: false
        });
    });
}

function agregarFilaCorte({
    index = null,
    tipoMedida = null,
    piezaOrigen = null,
    anchoCm = "",
    altoCm = "",
    longitudM = "",
    cantidad = 1,
    actualizar = true
} = {}) {
    if (!prepCortesBody || !filaPreparacionActual) return;

    tipoMedida = tipoMedida || obtenerTipoMedidaFila(filaPreparacionActual);
    piezaOrigen = piezaOrigen || piezaActiva;

    const vacio = prepCortesBody.querySelector(".text-muted");
    if (vacio) {
        prepCortesBody.innerHTML = "";
    }

    const card = document.createElement("div");
    card.className = "prep-corte-card";
    card.dataset.piezaOrigen = piezaOrigen;

    const numero = index || (prepCortesBody.querySelectorAll(".prep-corte-card").length + 1);

    if (tipoMedida === "LONGITUD") {
        card.innerHTML = `
            <div class="d-flex justify-content-between align-items-center mb-2">
                <strong class="small">Corte ${numero}</strong>
                <button type="button" class="btn btn-outline-danger btn-sm btn-quitar-corte">
                    <i class="bi bi-trash"></i>
                </button>
            </div>

            <div class="prep-corte-grid-longitud">
                <div>
                    <label class="form-label small mb-1">Longitud (m)</label>
                    <input type="number"
                           class="form-control form-control-sm corte-longitud"
                           min="0.01"
                           step="0.01"
                           value="${longitudM}"
                           placeholder="Ej: 1.20">
                </div>

                <div>
                    <label class="form-label small mb-1">Cant.</label>
                    <input type="number"
                          class="form-control form-control-sm corte-cantidad"
                          min="1"
                          max="99"
                          step="1"
                           value="${cantidad || 1}">
                </div>

                <div></div>
            </div>
        `;
    } else {
        card.innerHTML = `
            <div class="d-flex justify-content-between align-items-center mb-2">
                <strong class="small">Corte ${numero}</strong>
                <button type="button" class="btn btn-outline-danger btn-sm btn-quitar-corte">
                    <i class="bi bi-trash"></i>
                </button>
            </div>

            <div class="prep-corte-grid-area">
                <div>
                    <label class="form-label small mb-1">Ancho cm</label>
                    <input type="number"
                           class="form-control form-control-sm corte-ancho"
                           min="1"
                           step="0.01"
                           value="${anchoCm}"
                           placeholder="Ej: 30">
                </div>

                <div>
                    <label class="form-label small mb-1">Alto cm</label>
                    <input type="number"
                           class="form-control form-control-sm corte-alto"
                           min="1"
                           step="0.01"
                           value="${altoCm}"
                           placeholder="Ej: 40">
                </div>

                <div>
                    <label class="form-label small mb-1">Cant.</label>
                    <input type="number"
                          class="form-control form-control-sm corte-cantidad"
                          min="1"
                          max="99"
                          step="1"
                           value="${cantidad || 1}">
                </div>

                <div></div>
            </div>
        `;
    }

    prepCortesBody.appendChild(card);

  card.querySelectorAll("input").forEach(input => {

      input.addEventListener("input", () => {

          if (input.classList.contains("corte-cantidad")) {

              // Permite borrar para escribir 20, 30, etc.
              if (input.value === "") {
                  guardarCortesPiezaActualEnHidden();
                  actualizarVistaPreparacion();
                  return;
              }

              let valor = Number(input.value);

              if (valor > 99) {
                  valor = 99;
                  input.value = valor;
              }
          }

          guardarCortesPiezaActualEnHidden();
          actualizarVistaPreparacion();
      });

      input.addEventListener("blur", () => {

          if (input.classList.contains("corte-cantidad")) {

              let valor = Number(input.value || 1);

              if (valor < 1) valor = 1;
              if (valor > 99) valor = 99;

              input.value = valor;
          }

          guardarCortesPiezaActualEnHidden();
          actualizarVistaPreparacion();
      });

  });

    card.querySelector(".btn-quitar-corte")?.addEventListener("click", () => {
        card.remove();
        guardarCortesPiezaActualEnHidden();
        pintarModalDesdeHidden();
    });

    if (actualizar) {
        guardarCortesPiezaActualEnHidden();
        actualizarVistaPreparacion();
    }
}

function actualizarTextosCabecera() {
    if (!filaPreparacionActual) return;

    const tipoMedida = obtenerTipoMedidaFila(filaPreparacionActual);
    const nombrePieza = obtenerNombrePiezaFisica(tipoMedida);
    const cantidadPiezas = obtenerCantidadPiezasFisicas(filaPreparacionActual);

    const productoTexto =
        filaPreparacionActual.querySelector(".seleccionar-producto option:checked")?.textContent ||
        "Producto seleccionado";

    const presentacionTexto =
        filaPreparacionActual.querySelector(".presentacion-select option:checked")?.textContent ||
        "Presentación";

    const largo = Number(filaPreparacionActual.dataset.largoBase || 0);
    const ancho = Number(filaPreparacionActual.dataset.anchoBase || 0);
    const medida = Number(filaPreparacionActual.dataset.medidaTotalBase || 0);

    if (prepResumenProducto) {
        prepResumenProducto.textContent = `${productoTexto} · ${presentacionTexto}`;
    }

   if (prepMedidaBaseTexto) {
       if (tipoMedida === "AREA") {
           if (esVentaPorAreaSuelta(filaPreparacionActual)) {
               const areaVenta = obtenerAreaDisponibleVenta(filaPreparacionActual);
               const lienzo = obtenerLienzoAreaSueltaCm(filaPreparacionActual);

               prepMedidaBaseTexto.textContent =
                   `Área vendida: ${areaVenta.toFixed(2)} pie² · Lienzo: ${lienzo.anchoCm} x ${lienzo.altoCm} cm · Base física: ${largo} x ${ancho} cm`;
           } else {
               prepMedidaBaseTexto.textContent =
                   `Medida base: ${largo} x ${ancho} cm · ${medida.toFixed(2)} pie²`;
           }
       } else if (tipoMedida === "LONGITUD") {
            prepMedidaBaseTexto.textContent = `Medida base: ${medida.toFixed(2)} m por ${nombrePieza.toLowerCase()}`;
        } else {
            prepMedidaBaseTexto.textContent = "Producto por cantidad.";
        }
    }

    if (prepTituloPieza) {
        if (tipoMedida === "AREA" && esVentaPorAreaSuelta(filaPreparacionActual)) {
            prepTituloPieza.textContent =
                prepTipo?.value === "TAL_CUAL"
                    ? "Pieza sugerida"
                    : "Cortes solicitados";
        } else {
            prepTituloPieza.textContent = `${nombrePieza} ${piezaActiva}`;
        }
    }

 if (prepSubtituloPieza) {
     if (tipoMedida === "AREA" && esVentaPorAreaSuelta(filaPreparacionActual)) {
         const areaVenta = obtenerAreaDisponibleVenta(filaPreparacionActual);

         if (prepTipo?.value === "TAL_CUAL") {
             const sugerida = obtenerPiezaUnicaSugeridaCm(filaPreparacionActual);
             prepSubtituloPieza.textContent =
                 `Preparar 1 pieza sugerida de ${sugerida.anchoCm} x ${sugerida.altoCm} cm aprox.`;
         } else {
             prepSubtituloPieza.textContent =
                 `Área disponible para cortes personalizados: ${areaVenta.toFixed(2)} pie².`;
         }
     } else {
         prepSubtituloPieza.textContent =
             `${cantidadPiezas} ${nombrePieza.toLowerCase()}(s) disponible(s) para esta presentación.`;
     }
 }

   if (prepTextoAyuda) {
       if (tipoMedida === "AREA" && esVentaPorAreaSuelta(filaPreparacionActual)) {
           prepTextoAyuda.textContent = prepTipo?.value === "TAL_CUAL"
               ? "Se preparará una sola pieza rectangular equivalente al área comprada."
               : "Registra las medidas exactas solicitadas por el cliente.";
       } else if (tipoMedida === "AREA") {
           prepTextoAyuda.textContent =
               "Registra los cortes personalizados que pidió el cliente.";
       } else {
           prepTextoAyuda.textContent =
               "Selecciona una varilla y registra cortes en metros.";
       }
   }
}

function validarDistribucionAreaSimple(cortesPieza) {
    const largo = Number(filaPreparacionActual?.dataset.largoBase || 0);
    const ancho = Number(filaPreparacionActual?.dataset.anchoBase || 0);

    if (!largo || !ancho) {
        return {
            ok: false,
            mensaje: "No hay medidas reales para validar la plancha."
        };
    }

    let xCursor = 0;
    let yCursor = 0;
    let filaAlto = 0;
    let contador = 1;

    for (const c of cortesPieza) {
        const cantidad = Number(c.cantidad || 1);

        for (let i = 0; i < cantidad; i++) {
            const w = Number(c.anchoCm || 0);
            const h = Number(c.altoCm || 0);

            if (w <= 0 || h <= 0) {
                return {
                    ok: false,
                    mensaje: `La pieza ${contador} tiene medidas inválidas.`
                };
            }

            if (w > largo || h > ancho) {
                return {
                    ok: false,
                    mensaje: `La pieza ${contador} (${w} x ${h} cm) no cabe en la plancha ${largo} x ${ancho} cm.`
                };
            }

            if (xCursor + w > largo) {
                xCursor = 0;
                yCursor += filaAlto;
                filaAlto = 0;
            }

            if (yCursor + h > ancho) {
                return {
                    ok: false,
                    mensaje: `La pieza ${contador} ya no entra en la plancha. Reduce la cantidad o cambia las medidas.`
                };
            }

            xCursor += w;
            filaAlto = Math.max(filaAlto, h);
            contador++;
        }
    }

    return {
        ok: true,
        mensaje: ""
    };
}
function esVentaPorAreaSuelta(tr) {
    if (!tr) return false;

    const tipoCalculo = (tr.dataset.tipoCalculoVenta || "").toUpperCase();
    const medidaTotalBase = Number(tr.dataset.medidaTotalBase || 0);

    const presentacionTexto = (
        tr.querySelector(".presentacion-select option:checked")?.textContent || ""
    ).toLowerCase();

    const unidadTexto = (
        tr.dataset.unidadVentaNombre ||
        tr.dataset.unidadVentaAbrev ||
        ""
    ).toLowerCase();

    return (
        medidaTotalBase === 1 ||
        tipoCalculo === "PIE2" ||
        tipoCalculo === "M2" ||
        tipoCalculo === "METRO_CUADRADO" ||
        presentacionTexto.includes("pie") ||
        presentacionTexto.includes("m2") ||
        presentacionTexto.includes("m²") ||
        unidadTexto.includes("pie") ||
        unidadTexto.includes("m2") ||
        unidadTexto.includes("m²") ||
        unidadTexto.includes("metro cuadrado")
    );
}

function obtenerAreaDisponibleVenta(tr) {
    const cantidadVendida = Number(tr.querySelector(".cantidad")?.value || 0);
    const medidaTotalBase = Number(tr.dataset.medidaTotalBase || 0);

    if (esVentaPorAreaSuelta(tr)) {
        return cantidadVendida * medidaTotalBase;
    }

    return obtenerMaximoPorPieza(tr, "AREA");
}

function obtenerPiezaUnicaSugeridaCm(tr) {
    const areaPie2 = obtenerAreaDisponibleVenta(tr);
    const largoReal = Number(tr?.dataset.largoBase || 0);
    const anchoReal = Number(tr?.dataset.anchoBase || 0);

    // 1 pie² = 929.0304 cm²
    const areaCm2 = areaPie2 * 929.0304;

    if (!areaPie2 || areaPie2 <= 0) {
        return {
            anchoCm: 0,
            altoCm: 0,
            areaPie2Real: 0,
            areaPie2Vendida: areaPie2
        };
    }

    const relacion =
        largoReal > 0 && anchoReal > 0
            ? largoReal / anchoReal
            : 1.5;

    let anchoSugerido = Math.round(Math.sqrt(areaCm2 * relacion));

    if (largoReal > 0 && anchoSugerido > largoReal) {
        anchoSugerido = Math.floor(largoReal);
    }

    let altoSugerido = Math.floor(areaCm2 / anchoSugerido);

    if (anchoReal > 0 && altoSugerido > anchoReal) {
        altoSugerido = Math.floor(anchoReal);
        anchoSugerido = Math.floor(areaCm2 / altoSugerido);
    }

    const areaPie2Real = (anchoSugerido * altoSugerido) / 929.0304;

    return {
        anchoCm: anchoSugerido,
        altoCm: altoSugerido,
        areaPie2Real: Number(areaPie2Real.toFixed(2)),
        areaPie2Vendida: Number(areaPie2.toFixed(2))
    };
}

function generarCroquisPiezaUnicaAreaSuelta() {
    const sugerida = obtenerPiezaUnicaSugeridaCm(filaPreparacionActual);
    const largoReal = Number(filaPreparacionActual?.dataset.largoBase || 0);
    const anchoReal = Number(filaPreparacionActual?.dataset.anchoBase || 0);

    const viewW = 720;
    const viewH = 430;
    const margen = 70;

    if (!sugerida.anchoCm || !sugerida.altoCm) {
        return `<span class="text-muted small">No se pudo calcular la pieza sugerida.</span>`;
    }

    const escala = Math.min(
        (viewW - margen * 2) / sugerida.anchoCm,
        (viewH - 170) / sugerida.altoCm
    );

    const piezaW = sugerida.anchoCm * escala;
    const piezaH = sugerida.altoCm * escala;

    const x0 = (viewW - piezaW) / 2;
    const y0 = 145;

    return `
        <svg xmlns="http://www.w3.org/2000/svg"
             viewBox="0 0 ${viewW} ${viewH}"
             width="100%"
             height="auto">

            <rect width="100%" height="100%" fill="#f8fafc"></rect>

            <text x="${viewW / 2}" y="30"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="18"
                  font-weight="bold"
                  fill="#0f172a">
                Entregar tal cual por área
            </text>

            <text x="${viewW / 2}" y="55"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="14"
                  fill="#475569">
                Área vendida: ${sugerida.areaPie2Vendida.toFixed(2)} pie² · Pieza sugerida: ${sugerida.areaPie2Real.toFixed(2)} pie²
            </text>

            <text x="${viewW / 2}" y="78"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="12"
                  fill="#64748b">
                Base física referencial: ${largoReal} x ${anchoReal} cm
            </text>

            <text x="${viewW / 2}" y="100"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="12"
                  fill="#64748b">
                En entregas, el sistema asignará retazo, media plancha o plancha completa.
            </text>

            <rect x="${x0}" y="${y0}" width="${piezaW}" height="${piezaH}"
                  fill="#dbeafe"
                  stroke="#1d4ed8"
                  stroke-width="3"
                  rx="6"></rect>

            <text x="${x0 + piezaW / 2}" y="${y0 + piezaH / 2 - 6}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="18"
                  font-weight="bold"
                  fill="#0f172a">
                1 pieza
            </text>

            <text x="${x0 + piezaW / 2}" y="${y0 + piezaH / 2 + 18}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="15"
                  fill="#0f172a">
                ${sugerida.anchoCm} x ${sugerida.altoCm} cm aprox.
            </text>

            <text x="${x0 + piezaW / 2}" y="${y0 - 10}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  fill="#111827">
                ${sugerida.anchoCm} cm
            </text>

            <text x="${x0 - 18}" y="${y0 + piezaH / 2}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  fill="#111827"
                  transform="rotate(-90 ${x0 - 18} ${y0 + piezaH / 2})">
                ${sugerida.altoCm} cm
            </text>
        </svg>
    `;
}


function obtenerLienzoAreaSueltaCm(tr) {
    const areaDisponiblePie2 = obtenerAreaDisponibleVenta(tr);
    const largoReal = Number(tr?.dataset.largoBase || 0);
    const anchoReal = Number(tr?.dataset.anchoBase || 0);

    // 1 pie² = 929.0304 cm²
    const areaCm2 = areaDisponiblePie2 * 929.0304;

    if (!areaDisponiblePie2 || areaDisponiblePie2 <= 0) {
        return {
            anchoCm: 30.48,
            altoCm: 30.48,
            etiqueta: "Área referencial"
        };
    }

    // Si es exactamente 1 pie², dibuja el cuadrado clásico
    if (Math.abs(areaDisponiblePie2 - 1) < 0.0001) {
        return {
            anchoCm: 30.48,
            altoCm: 30.48,
            etiqueta: "1 pie² referencial"
        };
    }

    // Si hay base física real, usamos su proporción
    const relacion =
        largoReal > 0 && anchoReal > 0
            ? largoReal / anchoReal
            : 1;

    let anchoCm = Math.sqrt(areaCm2 * relacion);
    let altoCm = areaCm2 / anchoCm;

    anchoCm = Number(anchoCm.toFixed(2));
    altoCm = Number(altoCm.toFixed(2));

    return {
        anchoCm,
        altoCm,
        etiqueta: `${areaDisponiblePie2.toFixed(2)} pie² referenciales`
    };
}

function generarCroquisAreaSuelta(cortesPieza) {
    const areaDisponible = obtenerAreaDisponibleVenta(filaPreparacionActual);
    const areaDisponibleCm2 = areaDisponible * 929.0304;

    const largoReal = Number(filaPreparacionActual?.dataset.largoBase || 0);
    const anchoReal = Number(filaPreparacionActual?.dataset.anchoBase || 0);

    const usado = calcularAreaPie2(cortesPieza).pie2;
    const usadoCm2 = usado * 929.0304;
    const sobrante = Math.max(areaDisponible - usado, 0);
    const excedeArea = usado > areaDisponible;

    const viewW = 720;
    const margen = 55;

    const piezas = [];

    cortesPieza.forEach(c => {
        const cantidad = Number(c.cantidad || 1);

        for (let i = 0; i < cantidad; i++) {
            const anchoCm = Number(c.anchoCm || 0);
            const altoCm = Number(c.altoCm || 0);

            if (anchoCm > 0 && altoCm > 0) {
                piezas.push({
                    anchoCm,
                    altoCm,
                    areaCm2: anchoCm * altoCm
                });
            }
        }
    });

    if (piezas.length === 0) {
        return `
            <svg xmlns="http://www.w3.org/2000/svg"
                 viewBox="0 0 ${viewW} 360"
                 width="100%"
                 height="auto">
                <rect width="100%" height="100%" fill="#f8fafc"></rect>

                <text x="${viewW / 2}" y="34"
                      text-anchor="middle"
                      font-family="Arial"
                      font-size="18"
                      font-weight="bold"
                      fill="#0f172a">
                    Corte por Pie²
                </text>

                <text x="${viewW / 2}" y="62"
                      text-anchor="middle"
                      font-family="Arial"
                      font-size="13"
                      fill="#475569">
                    Disponible: ${areaDisponible.toFixed(2)} pie²
                </text>

                <text x="${viewW / 2}" y="180"
                      text-anchor="middle"
                      font-family="Arial"
                      font-size="15"
                      fill="#64748b">
                    Agrega medidas para visualizar el acomodo inteligente.
                </text>
            </svg>
        `;
    }

    function construirLayout(columnas) {
        const filas = [];
        let filaActual = [];

        piezas.forEach(p => {
            filaActual.push(p);

            if (filaActual.length === columnas) {
                filas.push(filaActual);
                filaActual = [];
            }
        });

        if (filaActual.length > 0) {
            filas.push(filaActual);
        }

        let anchoTotal = 0;
        let altoTotal = 0;
        const filasCalculadas = [];

        filas.forEach(fila => {
            const anchoFila = fila.reduce((acc, p) => acc + p.anchoCm, 0);
            const altoFila = Math.max(...fila.map(p => p.altoCm));

            anchoTotal = Math.max(anchoTotal, anchoFila);
            altoTotal += altoFila;

            filasCalculadas.push({
                piezas: fila,
                anchoFila,
                altoFila
            });
        });

        const areaBloqueCm2 = anchoTotal * altoTotal;
        const areaSobranteAcomodoCm2 = Math.max(areaBloqueCm2 - usadoCm2, 0);

        const cabeEnBaseNormal =
            largoReal <= 0 ||
            anchoReal <= 0 ||
            (anchoTotal <= largoReal && altoTotal <= anchoReal);

        const cabeEnBaseGirado =
            largoReal <= 0 ||
            anchoReal <= 0 ||
            (anchoTotal <= anchoReal && altoTotal <= largoReal);

        const cabeEnBase = cabeEnBaseNormal || cabeEnBaseGirado;
        const cabeEnAreaVendida = areaBloqueCm2 <= areaDisponibleCm2 + 0.5;

        const proporcionBase =
            largoReal > 0 && anchoReal > 0
                ? largoReal / anchoReal
                : 1.6;

        const proporcionLayout =
            altoTotal > 0
                ? anchoTotal / altoTotal
                : 1;

        const diferenciaProporcion = Math.abs(proporcionLayout - proporcionBase);

        return {
            columnas,
            filas: filasCalculadas,
            anchoTotal,
            altoTotal,
            areaBloqueCm2,
            areaSobranteAcomodoCm2,
            cabeEnBase,
            cabeEnAreaVendida,
            diferenciaProporcion
        };
    }

    let layouts = [];

    for (let columnas = 1; columnas <= piezas.length; columnas++) {
        layouts.push(construirLayout(columnas));
    }

    layouts.sort((a, b) => {
        if (a.cabeEnBase !== b.cabeEnBase) {
            return a.cabeEnBase ? -1 : 1;
        }

        if (a.cabeEnAreaVendida !== b.cabeEnAreaVendida) {
            return a.cabeEnAreaVendida ? -1 : 1;
        }

        const excesoA = Math.max(a.areaBloqueCm2 - areaDisponibleCm2, 0);
        const excesoB = Math.max(b.areaBloqueCm2 - areaDisponibleCm2, 0);

        if (excesoA !== excesoB) {
            return excesoA - excesoB;
        }

        if (a.areaSobranteAcomodoCm2 !== b.areaSobranteAcomodoCm2) {
            return a.areaSobranteAcomodoCm2 - b.areaSobranteAcomodoCm2;
        }

        return a.diferenciaProporcion - b.diferenciaProporcion;
    });

    const layout = layouts[0];

    const areaBloquePie2 = layout.areaBloqueCm2 / 929.0304;
    const sobranteAcomodoPie2 = layout.areaSobranteAcomodoCm2 / 929.0304;

    const escala = Math.min(
        (viewW - margen * 2) / layout.anchoTotal,
        260 / layout.altoTotal
    );

    const bloqueW = layout.anchoTotal * escala;
    const bloqueH = layout.altoTotal * escala;

    const x0 = (viewW - bloqueW) / 2;
    const y0 = 130;

    let contenido = "";
    let yCursor = y0;
    let contador = 1;

    layout.filas.forEach(fila => {
        let xCursor = x0;

        fila.piezas.forEach(p => {
            const w = p.anchoCm * escala;
            const h = p.altoCm * escala;

            contenido += `
                <g>
                    <rect x="${xCursor}" y="${yCursor}" width="${w}" height="${h}"
                          fill="${excedeArea ? '#fee2e2' : '#dbeafe'}"
                          stroke="${excedeArea ? '#dc2626' : '#1d4ed8'}"
                          stroke-width="2"></rect>

                    <text x="${xCursor + w / 2}" y="${yCursor + h / 2 - 5}"
                          text-anchor="middle"
                          font-size="14"
                          font-family="Arial"
                          font-weight="bold"
                          fill="#0f172a">
                        P${contador}
                    </text>

                    <text x="${xCursor + w / 2}" y="${yCursor + h / 2 + 13}"
                          text-anchor="middle"
                          font-size="11"
                          font-family="Arial"
                          fill="#0f172a">
                        ${p.anchoCm} x ${p.altoCm} cm
                    </text>
                </g>
            `;

            xCursor += w;
            contador++;
        });

        const sobranteFilaCm = layout.anchoTotal - fila.anchoFila;

        if (sobranteFilaCm > 0.01) {
            const sobranteW = sobranteFilaCm * escala;
            const sobranteH = fila.altoFila * escala;

            contenido += `
                <g>
                    <rect x="${xCursor}" y="${yCursor}" width="${sobranteW}" height="${sobranteH}"
                          fill="#f1f5f9"
                          stroke="#94a3b8"
                          stroke-dasharray="5 4"
                          stroke-width="2"></rect>

                    <text x="${xCursor + sobranteW / 2}" y="${yCursor + sobranteH / 2}"
                          text-anchor="middle"
                          font-size="11"
                          font-family="Arial"
                          fill="#64748b">
                        Sobrante
                    </text>
                </g>
            `;
        }

        yCursor += fila.altoFila * escala;
    });

    const viewH = Math.max(430, y0 + bloqueH + 85);

    let mensajeEstado = "";

    if (excedeArea) {
        mensajeEstado = `
            <text x="${viewW / 2}" y="${viewH - 28}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  font-weight="bold"
                  fill="#dc2626">
                Los cortes superan los pie² vendidos. Aumenta la cantidad vendida o reduce cortes.
            </text>
        `;
    } else if (!layout.cabeEnAreaVendida) {
        mensajeEstado = `
            <text x="${viewW / 2}" y="${viewH - 28}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  font-weight="bold"
                  fill="#f97316">
                Para este acomodo limpio se requiere más área que la vendida.
            </text>
        `;
    } else {
        mensajeEstado = `
            <text x="${viewW / 2}" y="${viewH - 28}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  font-weight="bold"
                  fill="#16a34a">
                Acomodo inteligente dentro de los pie² vendidos.
            </text>
        `;
    }

    return `
        <svg xmlns="http://www.w3.org/2000/svg"
             viewBox="0 0 ${viewW} ${viewH}"
             width="100%"
             height="auto">
            <rect width="100%" height="100%" fill="#f8fafc"></rect>

            <text x="${viewW / 2}" y="28"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="18"
                  font-weight="bold"
                  fill="#0f172a">
                Acomodo inteligente por Pie²
            </text>

            <text x="${viewW / 2}" y="52"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  fill="#475569">
                Disponible: ${areaDisponible.toFixed(2)} pie² · Usado en piezas: ${usado.toFixed(2)} pie² · Sobrante vendido: ${sobrante.toFixed(2)} pie²
            </text>

            <text x="${viewW / 2}" y="74"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="12"
                  fill="#64748b">
                Bloque sugerido: ${layout.anchoTotal.toFixed(2)} x ${layout.altoTotal.toFixed(2)} cm · Área bloque: ${areaBloquePie2.toFixed(2)} pie²
            </text>

            <text x="${viewW / 2}" y="96"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="12"
                  fill="#64748b">
                Base física referencial: ${largoReal} x ${anchoReal} cm · Sobrante por acomodo: ${sobranteAcomodoPie2.toFixed(2)} pie²
            </text>

            <rect x="${x0}" y="${y0}" width="${bloqueW}" height="${bloqueH}"
                  fill="#ffffff"
                  stroke="#111827"
                  stroke-width="3"></rect>

            <text x="${x0 + bloqueW / 2}" y="${y0 - 10}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  fill="#111827">
                ${layout.anchoTotal.toFixed(2)} cm
            </text>

            <text x="${x0 - 16}" y="${y0 + bloqueH / 2}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  fill="#111827"
                  transform="rotate(-90 ${x0 - 16} ${y0 + bloqueH / 2})">
                ${layout.altoTotal.toFixed(2)} cm
            </text>

            ${contenido}
            ${mensajeEstado}
        </svg>
    `;
}

function generarCroquisArea(cortesPieza) {
    const largo = Number(filaPreparacionActual?.dataset.largoBase || 0);
    const ancho = Number(filaPreparacionActual?.dataset.anchoBase || 0);
    if (esVentaPorAreaSuelta(filaPreparacionActual)) {
        return generarCroquisAreaSuelta(cortesPieza);
    }

    if (!largo || !ancho) {
        return `<span class="text-muted small">No hay medidas reales para dibujar la plancha.</span>`;
    }

    const viewW = 720;
    const viewH = 520;
    const margen = 60;

    const escala = Math.min(
        (viewW - margen * 2) / largo,
        (viewH - margen * 2) / ancho
    );

    const planchaW = largo * escala;
    const planchaH = ancho * escala;
    const x0 = (viewW - planchaW) / 2;
    const y0 = (viewH - planchaH) / 2;

    let xCursor = x0;
    let yCursor = y0;
    let filaAlto = 0;
    let contenido = "";
    let contador = 1;
    let hayExceso = false;

   for (const c of cortesPieza) {
       if (hayExceso) break;

       const cantidad = Number(c.cantidad || 1);

       for (let i = 0; i < cantidad; i++) {
           const anchoCm = Number(c.anchoCm || 0);
           const altoCm = Number(c.altoCm || 0);

           const w = anchoCm * escala;
           const h = altoCm * escala;

           if (xCursor + w > x0 + planchaW) {
               xCursor = x0;
               yCursor += filaAlto;
               filaAlto = 0;
           }

           const fuera = yCursor + h > y0 + planchaH;

           if (fuera) {
               hayExceso = true;
               break;
           }

           contenido += `
               <g>
                   <rect x="${xCursor}" y="${yCursor}" width="${w}" height="${h}"
                         fill="#dbeafe"
                         stroke="#1d4ed8"
                         stroke-width="2"></rect>

                   <text x="${xCursor + w / 2}" y="${yCursor + h / 2 - 5}"
                         text-anchor="middle" font-size="16"
                         font-family="Arial" font-weight="bold"
                         fill="#0f172a">P${contador}</text>

                   <text x="${xCursor + w / 2}" y="${yCursor + h / 2 + 14}"
                         text-anchor="middle" font-size="13"
                         font-family="Arial"
                         fill="#0f172a">${anchoCm} x ${altoCm} cm</text>
               </g>
           `;

           xCursor += w;
           filaAlto = Math.max(filaAlto, h);
           contador++;
       }
   }

    const alerta = hayExceso
        ? `
            <text x="${viewW / 2}" y="${viewH - 18}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="15"
                  font-weight="bold"
                  fill="#dc2626">
                Hay piezas que ya no entran en la plancha
            </text>
        `
        : "";

    return `
        <svg xmlns="http://www.w3.org/2000/svg"
             viewBox="0 0 ${viewW} ${viewH}"
             width="100%"
             height="auto">
            <rect width="100%" height="100%" fill="#f8fafc"></rect>

            <text x="${viewW / 2}" y="28"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="18"
                  font-weight="bold"
                  fill="#0f172a">
                Plancha ${piezaActiva} - ${largo} x ${ancho} cm
            </text>

            <rect x="${x0}" y="${y0}" width="${planchaW}" height="${planchaH}"
                  fill="#ffffff"
                  stroke="#111827"
                  stroke-width="3"></rect>

            <text x="${x0 + planchaW / 2}" y="${y0 - 10}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="14"
                  fill="#111827">${largo} cm</text>

            <text x="${x0 - 18}" y="${y0 + planchaH / 2}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="14"
                  fill="#111827"
                  transform="rotate(-90 ${x0 - 18} ${y0 + planchaH / 2})">${ancho} cm</text>

            ${contenido}
            ${alerta}
        </svg>
    `;
}

function generarCroquisLongitud(cortesPieza) {
    const medida = Number(filaPreparacionActual?.dataset.medidaTotalBase || 0);
    const viewW = 720;
    const viewH = 220;
    const x0 = 55;
    const y0 = 90;
    const barW = 610;
    const barH = 34;

    let xCursor = x0;
    let contenido = "";

    let usado = 0;
    let contador = 1;

    cortesPieza.forEach(c => {
        const cantidad = Number(c.cantidad || 1);

        for (let i = 0; i < cantidad; i++) {
            const longitud = Number(c.longitudM || 0);
            const w = medida > 0 ? (longitud / medida) * barW : 0;

            contenido += `
                <g>
                    <rect x="${xCursor}" y="${y0}" width="${w}" height="${barH}"
                          fill="#dbeafe" stroke="#1d4ed8" stroke-width="2"></rect>
                    <text x="${xCursor + w / 2}" y="${y0 + 22}"
                          text-anchor="middle"
                          font-family="Arial"
                          font-size="13"
                          fill="#0f172a">P${contador} · ${longitud} m</text>
                </g>
            `;

            xCursor += w;
            usado += longitud;
            contador++;
        }
    });

    const sobrante = Math.max(medida - usado, 0);
    const sobranteW = medida > 0 ? (sobrante / medida) * barW : 0;

    if (sobranteW > 0) {
        contenido += `
            <rect x="${xCursor}" y="${y0}" width="${sobranteW}" height="${barH}"
                  fill="#f1f5f9" stroke="#94a3b8" stroke-width="2"></rect>
            <text x="${xCursor + sobranteW / 2}" y="${y0 + 22}"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="13"
                  fill="#475569">Sobrante ${sobrante.toFixed(2)} m</text>
        `;
    }

    return `
        <svg xmlns="http://www.w3.org/2000/svg"
             viewBox="0 0 ${viewW} ${viewH}"
             width="100%"
             height="auto">
            <rect width="100%" height="100%" fill="#f8fafc"></rect>

            <text x="${viewW / 2}" y="35"
                  text-anchor="middle"
                  font-family="Arial"
                  font-size="18"
                  font-weight="bold"
                  fill="#0f172a">
                Varilla ${piezaActiva} - ${medida.toFixed(2)} m
            </text>

            <rect x="${x0}" y="${y0}" width="${barW}" height="${barH}"
                  fill="#ffffff"
                  stroke="#111827"
                  stroke-width="3"></rect>

            ${contenido}
        </svg>
    `;
}

function actualizarVistaPreparacion() {
    if (!filaPreparacionActual) return;

    const tipoMedida = obtenerTipoMedidaFila(filaPreparacionActual);
    const hidden = filaPreparacionActual.querySelector(".cortes-json-hidden");

    let cortes = [];

    if (hidden?.value) {
        try {
            cortes = JSON.parse(hidden.value);
        } catch (e) {
            cortes = [];
        }
    }

    const cortesActuales = obtenerCortesDesdeModal();
    cortes = cortes.filter(c => Number(c.piezaOrigen || 1) !== Number(piezaActiva));
    cortes = [...cortes, ...cortesActuales];

    const cortesPieza = cortesDePieza(cortes, piezaActiva);
    const usado = calcularUsoPorPieza(cortesPieza, tipoMedida);
    const maximo = obtenerMaximoPorPieza(filaPreparacionActual, tipoMedida);
    const sobrante = Math.max(maximo - usado, 0);
    const unidad = nombreUnidadValidacion(tipoMedida);

    let calculoVista = null;
    let mensajeError = "";
    if (cortes.length > 0 && (tipoMedida === "AREA" || tipoMedida === "LONGITUD")) {
        try {
            calculoVista = calcularYDistribuirMedidas(filaPreparacionActual, cortes);
        } catch (error) {
            mensajeError = error.message;
        }
    }
    const hayError = mensajeError !== "";

    if (prepErroresPieza) {
        prepErroresPieza.classList.toggle("d-none", !hayError);
        const span = prepErroresPieza.querySelector("span");
        if (span) span.textContent = mensajeError;
    }

    if (prepAlertaInline) {
        prepAlertaInline.classList.toggle("d-none", !hayError);
    }

    if (prepAlertaTexto) {
        prepAlertaTexto.textContent = mensajeError;
    }

    if (prepBadgeEstado) {
        prepBadgeEstado.className = hayError
            ? "badge rounded-pill text-bg-danger"
            : (cortesPieza.length > 0 ? "badge rounded-pill text-bg-success" : "badge rounded-pill text-bg-secondary");
        prepBadgeEstado.textContent = hayError
            ? "Con error"
            : (cortesPieza.length > 0 ? "Correcto" : "Sin cortes");
    }

    if (prepResumenMedidas) {
        if (
            prepTipo?.value === "TAL_CUAL" &&
            tipoMedida === "AREA" &&
            esVentaPorAreaSuelta(filaPreparacionActual)
        ) {
            const sugerida = obtenerPiezaUnicaSugeridaCm(filaPreparacionActual);

            prepResumenMedidas.innerHTML = `
                <span class="badge text-bg-light border">Vendido: ${sugerida.areaPie2Vendida.toFixed(2)} pie²</span>
                <span class="badge text-bg-light border">Pieza sugerida: ${sugerida.areaPie2Real.toFixed(2)} pie²</span>
                <span class="badge text-bg-light border">Medida: ${sugerida.anchoCm} x ${sugerida.altoCm} cm</span>
            `;
        } else if (calculoVista) {
            const factor = Number(filaPreparacionActual.querySelector(".factor-stock-hidden")?.value || 1);
            const stockNecesario = calculoVista.cantidadComercial * factor;
            prepResumenMedidas.innerHTML = `
                <span class="badge text-bg-light border">Solicitado: ${calculoVista.medidaTotal.toFixed(2)} ${unidad}</span>
                <span class="badge text-bg-light border">Piezas físicas: ${calculoVista.unidadesFisicas}</span>
                <span class="badge text-bg-light border">Stock: ${stockNecesario.toFixed(4)}</span>
                <span class="badge text-bg-light border">Sobrante: ${calculoVista.sobrante.toFixed(2)} ${unidad}</span>
            `;
        } else {
            prepResumenMedidas.innerHTML = `
                <span class="badge text-bg-light border">Disponible: ${maximo.toFixed(2)} ${unidad}</span>
                <span class="badge text-bg-light border">Usado: ${usado.toFixed(2)} ${unidad}</span>
                <span class="badge text-bg-light border">Sobrante: ${sobrante.toFixed(2)} ${unidad}</span>
            `;
        }
    }

    if (prepCroquisPreview) {
        if (prepTipo?.value === "TAL_CUAL") {
            if (tipoMedida === "AREA" && esVentaPorAreaSuelta(filaPreparacionActual)) {
                prepCroquisPreview.innerHTML = generarCroquisPiezaUnicaAreaSuelta();
            } else {
                prepCroquisPreview.innerHTML = `
                    <span class="text-muted small">
                        Este producto se entregará tal cual, sin cortes.
                    </span>
                `;
            }
        } else if (tipoMedida === "AREA") {
            prepCroquisPreview.innerHTML = generarCroquisArea(cortesPieza);
        } else if (tipoMedida === "LONGITUD") {
            prepCroquisPreview.innerHTML = generarCroquisLongitud(cortesPieza);
        }
    }

    pintarTabsPiezas(cortes);
}

function pintarModalDesdeHidden() {
    if (!filaPreparacionActual) return;

    let cortes = [];
    const hidden = filaPreparacionActual.querySelector(".cortes-json-hidden");

    if (hidden?.value) {
        try {
            cortes = JSON.parse(hidden.value);
        } catch (e) {
            cortes = [];
        }
    }

    actualizarTextosCabecera();
    pintarTabsPiezas(cortes);
    pintarCortesDePieza(cortes);
    actualizarVistaPreparacion();
}

let secuenciaAyudaPresentacion = 0;

function obtenerAyudaPresentacion(tr) {
    let ayuda = tr.querySelector(".presentacion-ayuda");
    if (!ayuda) {
        ayuda = document.createElement("div");
        ayuda.className = "presentacion-ayuda";
        ayuda.setAttribute("role", "status");
        ayuda.setAttribute("aria-live", "polite");
        tr.querySelector(".presentacion-select")?.insertAdjacentElement("afterend", ayuda);
    }
    if (!ayuda.id) ayuda.id = `presentacion-ayuda-${++secuenciaAyudaPresentacion}`;
    tr.querySelector(".presentacion-select")?.setAttribute("aria-describedby", ayuda.id);
    return ayuda;
}

function mostrarAyudaPresentacion(tr, mensaje = "", esError = false) {
    const ayuda = obtenerAyudaPresentacion(tr);
    ayuda.textContent = mensaje;
    ayuda.classList.toggle("presentacion-ayuda--error", esError);
    ayuda.classList.toggle("is-visible", Boolean(mensaje));
}

function limpiarPreparacionFila(tr) {
    tr.querySelector(".requiere-preparacion-hidden").value = "0";
    tr.querySelector(".tipo-preparacion-hidden").value = "TAL_CUAL";
    tr.querySelector(".entregar-sobrante-hidden").value = "0";
    tr.querySelector(".observacion-preparacion-hidden").value = "";
    tr.querySelector(".cortes-json-hidden").value = "";
    tr.querySelector(".croquis-svg-hidden").value = "";
    ["unidadesFisicasCalculadas", "medidaTotalCalculada", "sobranteCalculado", "stockCalculado"]
        .forEach(clave => delete tr.dataset[clave]);
}

function limpiarDatosPresentacionFila(tr) {
    tr.querySelector(".unidad-hidden").value = "0";
    tr.querySelector(".factor-stock-hidden").value = "1";
    ["unidadVentaNombre", "unidadVentaAbrev", "tipoMedidaPreparacion", "tipoCalculoVenta",
        "tipoCalculoVentaOriginal", "cantidadIncluida", "largoBase", "anchoBase",
        "medidaTotalBase", "productoTieneMedida", "tipoMedidaProducto"]
        .forEach(clave => delete tr.dataset[clave]);
}

function enfocarCampoFila(campo) {
    if (!campo || campo.disabled) return;
    campo.focus({ preventScroll: false });
    if (campo.classList.contains("seleccionar-producto") && window.jQuery) {
        try { $(campo).select2("open"); } catch (e) { /* El enfoque nativo es suficiente. */ }
    }
}

function abrirModalPreparacion(tr, opciones = {}) {
    if (!modalPreparacion || !tr) return;

    const productoSelect = tr.querySelector(".seleccionar-producto");
    const presentacionSelect = tr.querySelector(".presentacion-select");
    const producto = Number(productoSelect?.value || 0);
    const presentacion = Number(presentacionSelect?.value || 0);

    if (producto <= 0) {
        mostrarAyudaPresentacion(tr, "Selecciona primero un producto.", true);
        enfocarCampoFila(productoSelect);
        return;
    }
    if (presentacion <= 0) {
        mostrarAyudaPresentacion(tr, "Selecciona una presentación para continuar.", true);
        enfocarCampoFila(presentacionSelect);
        return;
    }

    const opcion = presentacionSelect.options[presentacionSelect.selectedIndex];
    if (tr.dataset.estadoPresentaciones !== "LISTA" || opcion?.dataset.datosTecnicosCargados !== "1") {
        mostrarAyudaPresentacion(tr, "Espera a que termine la carga de la presentación.", true);
        enfocarCampoFila(presentacionSelect);
        return;
    }

    filaPreparacionActual = tr;
    piezaActiva = 1;

    if (prepPiezaActivaInput) {
        prepPiezaActivaInput.value = "1";
    }

    const tipoMedida = obtenerTipoMedidaFila(tr);
    const tieneMedida = tr.dataset.productoTieneMedida === "1";
    const dimensionesListas = tipoMedida === "AREA"
        ? Number(tr.dataset.largoBase || 0) > 0 && Number(tr.dataset.anchoBase || 0) > 0
        : Number(tr.dataset.medidaTotalBase || 0) > 0;

    if (tipoMedida !== "AREA" && tipoMedida !== "LONGITUD") {
        mostrarAyudaPresentacion(tr, "Esta presentación se vende por cantidad y no requiere ingresar medidas.");
        enfocarCampoFila(tr.querySelector(".cantidad"));
        return;
    }

    if (!tieneMedida || !dimensionesListas) {
        mostrarAyudaPresentacion(tr, "La presentación no tiene datos técnicos de medida disponibles.", true);
        enfocarCampoFila(presentacionSelect);
        return;
    }

    const modalYaVisible = modalPreparacionElement?.classList.contains("show");
    if (!opciones.forzar && (modalPreparacionEnApertura || modalYaVisible)) return;
    if (!modalYaVisible) modalPreparacionEnApertura = true;
    mostrarAyudaPresentacion(tr);

    const requiere = tr.querySelector(".requiere-preparacion-hidden")?.value === "1";
    const tipo = tr.querySelector(".tipo-preparacion-hidden")?.value || "TAL_CUAL";
    const entregar = tr.querySelector(".entregar-sobrante-hidden")?.value || "0";
    const obs = tr.querySelector(".observacion-preparacion-hidden")?.value || "";

   if (prepTipo) {
       prepTipo.value = requiere ? tipo : ((tipoMedida === "AREA" || tipoMedida === "LONGITUD") ? "CORTE" : "TAL_CUAL");
   }

   document.querySelectorAll("input[name='prepTipoRadio']").forEach(r => {
       r.checked = r.value === prepTipo.value;
   });

    if (prepEntregarSobrante) {
        prepEntregarSobrante.value = entregar;
    }

    if (prepObservacion) {
        prepObservacion.value = obs;
    }

    pintarModalDesdeHidden();
    actualizarEstadoCortesPorModo();

    prepContenidoTecnico?.classList.remove("d-none");
    if (!modalYaVisible) modalPreparacion.show();
}

function actualizarBotonPreparacion(tr) {
    const btn = tr.querySelector(".btn-config-preparacion");
    if (!btn) return;

    if (obtenerGrupoMedidaFila(tr)) {
        btn.classList.add("d-none");
        btn.disabled = true;
        btn.setAttribute("aria-disabled", "true");
        return;
    }

    const requiere = tr.querySelector(".requiere-preparacion-hidden")?.value === "1";
    const esLineaMedidas = tr.dataset.origenLinea === "MEDIDAS" || requiere;

    btn.classList.toggle("d-none", !esLineaMedidas);
    btn.disabled = !esLineaMedidas;
    btn.setAttribute("aria-disabled", String(!esLineaMedidas));
    btn.classList.toggle("btn-primary", esLineaMedidas);
    btn.classList.toggle("btn-outline-primary", !esLineaMedidas);
    btn.innerHTML = `<i class="bi bi-pencil-square me-1"></i> Editar medidas`;
    btn.title = esLineaMedidas ? "Editar las medidas y recalcular la línea" : "";
}

function validarPreparacionAntesDeGuardar() {
    const tipo = prepTipo?.value || "TAL_CUAL";

    if (tipo === "TAL_CUAL") {
        return true;
    }

    guardarCortesPiezaActualEnHidden();

    const hidden = filaPreparacionActual.querySelector(".cortes-json-hidden");
    let cortes = [];

    if (hidden?.value) {
        try {
            cortes = JSON.parse(hidden.value);
        } catch (e) {
            cortes = [];
        }
    }

    const tipoMedida = obtenerTipoMedidaFila(filaPreparacionActual);

    if (!Array.isArray(cortes) || cortes.length === 0) {
        if (prepAlertaInline) prepAlertaInline.classList.remove("d-none");
        if (prepAlertaTexto) prepAlertaTexto.textContent = "Agrega al menos un corte o cambia el modo a entregar tal cual.";
        return false;
    }

    try {
        const calculo = calcularYDistribuirMedidas(filaPreparacionActual, cortes);
        cortes = calculo.cortes;
        hidden.value = JSON.stringify(cortes);
        filaPreparacionActual.dataset.unidadesFisicasCalculadas = String(calculo.unidadesFisicas);
        filaPreparacionActual.dataset.medidaTotalCalculada = String(calculo.medidaTotal);
        filaPreparacionActual.dataset.sobranteCalculado = String(calculo.sobrante);
        const cantidadCalculada = filaPreparacionActual.querySelector(".cantidad");
        cantidadCalculada.value = String(calculo.cantidadComercial);
        cantidadCalculada.readOnly = true;
    } catch (error) {
        if (prepAlertaInline) prepAlertaInline.classList.remove("d-none");
        if (prepAlertaTexto) prepAlertaTexto.textContent = error.message;
        return false;
    }

    const invalidas = cortes.some(c => {
        if (tipoMedida === "LONGITUD") {
            return Number(c.longitudM || 0) <= 0 || Number(c.cantidad || 0) <= 0;
        }

        return Number(c.anchoCm || 0) <= 0 ||
            Number(c.altoCm || 0) <= 0 ||
            Number(c.cantidad || 0) <= 0;
    });

    if (invalidas) {
        if (prepAlertaInline) prepAlertaInline.classList.remove("d-none");
        if (prepAlertaTexto) prepAlertaTexto.textContent = "Hay cortes con medidas inválidas.";
        return false;
    }

    const validacionPieza = validarCortesPorPiezaFisica(filaPreparacionActual, cortes, tipoMedida);
    const validacionMedida = validarMedidasIndividuales(filaPreparacionActual, cortes, tipoMedida);
   const validacionDistribucion = { ok: true, mensaje: "", pieza: null };

   if (!validacionPieza.ok || !validacionMedida.ok || !validacionDistribucion.ok) {
       const error = !validacionPieza.ok
           ? validacionPieza
           : (!validacionMedida.ok ? validacionMedida : validacionDistribucion);

       piezaActiva = error.pieza || piezaActiva || 1;

       if (prepPiezaActivaInput) {
           prepPiezaActivaInput.value = piezaActiva;
       }

       if (prepAlertaInline) {
           prepAlertaInline.classList.remove("d-none");
       }

       if (prepAlertaTexto) {
           prepAlertaTexto.textContent = error.mensaje;
       }

       pintarModalDesdeHidden();
       return false;
   }

    return true;
}

function actualizarEstadoCortesPorModo() {
    if (!prepTipo) return;

    const esTalCual = prepTipo.value === "TAL_CUAL";

    if (btnAgregarCorte) {
        btnAgregarCorte.disabled = esTalCual;
        btnAgregarCorte.classList.toggle("disabled", esTalCual);
    }

    if (esTalCual) {
        if (filaPreparacionActual) {
            filaPreparacionActual.querySelector(".cortes-json-hidden").value = "";
        }

        if (prepCortesBody) {
            if (
                filaPreparacionActual &&
                obtenerTipoMedidaFila(filaPreparacionActual) === "AREA" &&
                esVentaPorAreaSuelta(filaPreparacionActual)
            ) {
                const sugerida = obtenerPiezaUnicaSugeridaCm(filaPreparacionActual);

                prepCortesBody.innerHTML = `
                    <div class="text-center text-muted small py-4">
                        <i class="bi bi-square d-block fs-4 mb-2 text-primary"></i>
                        Se preparará <strong>1 pieza rectangular</strong><br>
                        Medida sugerida:
                        <strong>${sugerida.anchoCm} x ${sugerida.altoCm} cm aprox.</strong>
                    </div>
                `;
            } else {
                prepCortesBody.innerHTML = `
                    <div class="text-center text-muted small py-4">
                        No requiere cortes.
                    </div>
                `;
            }
        }
    }

    actualizarTextosCabecera();
    actualizarVistaPreparacion();
}

document.querySelectorAll("input[name='prepTipoRadio']").forEach(radio => {
    radio.addEventListener("change", () => {
        if (prepTipo) {
            prepTipo.value = radio.value;
        }

        actualizarEstadoCortesPorModo();
    });
});

prepTipo?.addEventListener("change", () => {
    document.querySelectorAll("input[name='prepTipoRadio']").forEach(r => {
        r.checked = r.value === prepTipo.value;
    });

    actualizarEstadoCortesPorModo();
});

btnAgregarCorte?.addEventListener("click", () => {
    if (prepTipo?.value === "TAL_CUAL") {
        if (prepAlertaInline) prepAlertaInline.classList.remove("d-none");
        if (prepAlertaTexto) prepAlertaTexto.textContent = "Cambia el modo a 'Requiere corte' para agregar medidas.";
        return;
    }

    agregarFilaCorte();
});

btnGuardarPreparacion?.addEventListener("click", async () => {
    if (!filaPreparacionActual) return;

    if (!validarPreparacionAntesDeGuardar()) {
        return;
    }

   const tipo = prepTipo?.value || "TAL_CUAL";
   const requiere = tipo === "CORTE";

    let cortes = [];
    const hidden = filaPreparacionActual.querySelector(".cortes-json-hidden");

    if (hidden?.value) {
        try {
            cortes = JSON.parse(hidden.value);
        } catch (e) {
            cortes = [];
        }
    }

   if (!requiere) {
       cortes = [];
       const cantidadTalCual = filaPreparacionActual.querySelector(".cantidad");
       cantidadTalCual.value = "1";
       cantidadTalCual.readOnly = ["AREA", "LONGITUD"].includes(obtenerTipoMedidaFila(filaPreparacionActual));
       filaPreparacionActual.dataset.unidadesFisicasCalculadas = "1";
   }

    const tipoMedida = obtenerTipoMedidaFila(filaPreparacionActual);

    let croquis = "";

   if (requiere) {
       if (tipoMedida === "LONGITUD") {
           croquis = generarCroquisLongitud(cortesDePieza(cortes, 1));
       } else {
           croquis = generarCroquisArea(cortesDePieza(cortes, 1));
       }
   } else if (tipoMedida === "AREA" && esVentaPorAreaSuelta(filaPreparacionActual)) {
       croquis = generarCroquisPiezaUnicaAreaSuelta();
   }

    filaPreparacionActual.querySelector(".requiere-preparacion-hidden").value = requiere ? "1" : "0";
    filaPreparacionActual.querySelector(".tipo-preparacion-hidden").value = tipo;
    filaPreparacionActual.querySelector(".entregar-sobrante-hidden").value = prepEntregarSobrante?.value || "0";
    filaPreparacionActual.querySelector(".observacion-preparacion-hidden").value = prepObservacion?.value || "";
    filaPreparacionActual.querySelector(".cortes-json-hidden").value = requiere ? JSON.stringify(cortes) : "";
    filaPreparacionActual.querySelector(".croquis-svg-hidden").value = croquis;

    try {
        await validarStockCalculadoAntesAgregar(filaPreparacionActual);
    } catch (error) {
        if (prepAlertaInline) prepAlertaInline.classList.remove("d-none");
        if (prepAlertaTexto) prepAlertaTexto.textContent = error.message;
        return;
    }

    actualizarBotonPreparacion(filaPreparacionActual);
    recalcularFila(filaPreparacionActual);

    modalPreparacion?.hide();

    Swal.fire({
        icon: "success",
        title: "Medidas aplicadas a la venta",
        timer: 1200,
        showConfirmButton: false
    });
});

btnVerCroquisGrande?.addEventListener("click", () => {
    if (!prepCroquisPreview || !modalCroquisGrande) return;

    if (croquisGrandeContenido) {
        croquisGrandeContenido.innerHTML = prepCroquisPreview.innerHTML;
    }

    if (croquisGrandeSubtitulo && filaPreparacionActual) {
        const tipoMedida = obtenerTipoMedidaFila(filaPreparacionActual);
        const nombrePieza = obtenerNombrePiezaFisica(tipoMedida);
        croquisGrandeSubtitulo.textContent = `${nombrePieza} ${piezaActiva} - vista ampliada`;
    }

    modalCroquisGrande.show();
});

    /* ============================================================================
       SELECT2 – MÓDULO DE PRODUCTOS
============================================================================ */
async function aplicarProductoSeleccionadoAFila(tr, sel, data) {
    if (!tr || !sel || !data?.id) return;

    sel.value = data.id;

   /*
    * Siempre consulta la sede activa porque el stock que llega
    * desde Select2 no contiene completas, medias ni retazos.
    */
   await cargarStockSedeActiva(
       tr,
       data.id
   );

    const selectPresentacion = tr.querySelector(".presentacion-select");
    selectPresentacion.innerHTML = `<option value="">Seleccione presentación</option>`;
    selectPresentacion.disabled = true;
    delete selectPresentacion.dataset.selected;

    limpiarPreparacionFila(tr);
    limpiarDatosPresentacionFila(tr);
    tr.dataset.estadoPresentaciones = "CARGANDO";

    const cantidad = tr.querySelector(".cantidad");
    cantidad.value = tr.dataset.origenLinea === "MEDIDAS" ? "0" : "1";
    cantidad.readOnly = tr.dataset.origenLinea === "MEDIDAS";

    const precio = tr.querySelector(".precio");
    precio.value = "0.00";
    precio.readOnly = true;

    const desc = tr.querySelector(".descuento");
    desc.value = "0.00";
    desc.dataset.maximo = 0;
    desc.max = 0;
    desc.readOnly = true;

    actualizarBotonPreparacion(tr);
    recalcularFila(tr);

    await cargarPresentaciones(tr, data.id);

    const img = tr.querySelector(".producto-img-mini");
    const icon = tr.querySelector(".producto-icon-mini");
    const fotoProducto = data.img || data.foto || "";

    if (fotoProducto.trim() !== "") {
        img.src = "/uploads/productos/" + fotoProducto;
        img.classList.remove("d-none");
        icon.classList.add("d-none");
    } else {
        img.removeAttribute("src");
        img.classList.add("d-none");
        icon.classList.remove("d-none");
    }

    recalcularFila(tr);
}

function activarSelect2(sel) {

    // ⚠️ Evita inicializar 2 veces el mismo select
    if ($(sel).hasClass("select2-hidden-accessible")) {
        $(sel).select2("destroy");
    }

    $(sel).select2({
        theme: "bootstrap-5",
        width: "resolve",     // ✔ responsivo automático
        dropdownAutoWidth: true,
        closeOnSelect: true,  // ✔ evita doble evento
        minimumInputLength: 1,

        ajax: {
            url: "/interno/productos/buscar-vendibles",
            dataType: "json",
            delay: 200,
            data: params => ({ term: params.term }),
          processResults: data => ({
              results: data.map(p => ({
                  id: p.id,
                  text: p.text,
                  img: p.img || p.foto || "",
                  precio: p.precio,
                  stock: p.stock,
                  unidadNombre: p.unidadNombre,
                  idUnidad: p.idUnidad,
                  descuentoMaximo: p.descuentoMaximo
              }))
          })
        },

        placeholder: "Seleccione producto",
        allowClear: false,

        templateResult: formatoProductoMini,
        templateSelection: item => item.text || "",
        escapeMarkup: m => m
    });

    // 🔥 evitar doble ejecución
    $(sel).off("select2:select").on("select2:select", async function (e) {
        const tr = sel.closest("tr");
        await aplicarProductoSeleccionadoAFila(tr, sel, e.params.data);
    });

}



function formatoProductoMini(p) {
    if (!p.id) return p.text;

    const tieneImagen = p.img && p.img.trim() !== "";

    return $(`
        <div class="prod-item-mini">
            ${
                tieneImagen
                ? `<img src="/uploads/productos/${p.img}" class="prod-img-mini">`
                : `<div class="prod-img-mini" style="
                        width:45px;
                        height:45px;
                        border-radius:6px;
                        background:#e9ecef;
                        display:flex;
                        align-items:center;
                        justify-content:center;
                        color:#6c757d;
                        font-size:0.7rem;
                    ">SIN IMG</div>`
            }
            <div>
                <strong>${p.text}</strong><br>
                <small>Stock disponible: ${p.stock}</small>
            </div>
        </div>
    `);
}



    function formatoProducto(p) {
        if (!p.id) return p.text;

        return $(`
            <div class="d-flex align-items-center">
                <img src="${p.img
                    ? '/uploads/productos/' + p.img
                    : '/img/tienda/sin-imagen.jpg'}"
                class="prod-img">
                <div>
                    <strong>${p.text}</strong><br>
                    <small>${p.codigo} | Stock: ${p.stock} | S/ ${p.precio}</small>
                </div>
            </div>
        `);
    }


    /* ============================================================================
       FILAS DE PRODUCTOS
============================================================================ */
   function nuevaFila(origen = "NORMAL", ocultar = false) {

       const tr = document.createElement("tr");
       tr.dataset.origenLinea = origen;
       if (ocultar) tr.classList.add("d-none", "fila-medidas-borrador");

       tr.innerHTML = `
          <td>

              <div class="producto-box">

                 <div class="producto-icon-mini">
                     <i class="bi bi-box-seam"></i>
                 </div>

                 <img src=""
                      class="producto-img-mini d-none">

                  <select class="form-select seleccionar-producto"
                          name="idProducto"
                          required>
                  </select>

              </div>

          </td>

          <td>
              <select class="form-select presentacion-select"
                      name="idPrecioPresentacion"
                      required>
                  <option value="">Seleccione presentación</option>
              </select>

              <div class="presentacion-ayuda" role="status" aria-live="polite"></div>

              <input type="hidden"
                     name="factorConversionStock"
                     class="factor-stock-hidden"
                     value="1">
          </td>

        <input type="hidden" name="idUnidad" class="unidad-hidden" value="0">

          <td>
              <div class="cell-input-wrap">
                  <input type="number" class="form-control cantidad"
                         name="cantidad"
                         value="0"
                         step="1"
                         min="1"
                         readonly
                         required>
              </div>
          </td>

          <td>
              <div class="cell-input-wrap">
                 <input type="number" class="form-control precio"
                        name="precioUnitario" value="0.00" step="0.01" readonly required>
              </div>
          </td>

          <td>
              <div class="cell-input-wrap">
                  <input type="number" class="form-control descuento"
                         name="descuento" value="0.00" step="0.01" readonly required>
              </div>
          </td>

          <td class="subtotal-cell text-end">0.00</td>

          <td class="text-center">
              <button type="button"
                      class="btn btn-outline-primary btn-sm rounded-pill btn-config-preparacion d-none"
                      disabled aria-disabled="true"
                      title="Editar las medidas calculadas">
                  <i class="bi bi-pencil-square me-1"></i>
                  Editar medidas
              </button>

              <input type="hidden" name="requierePreparacion" class="requiere-preparacion-hidden" value="0">
              <input type="hidden" name="tipoPreparacion" class="tipo-preparacion-hidden" value="TAL_CUAL">
              <input type="hidden" name="entregarSobrante" class="entregar-sobrante-hidden" value="0">
              <input type="hidden" name="observacionPreparacion" class="observacion-preparacion-hidden" value="">
              <input type="hidden" name="cortesJson" class="cortes-json-hidden" value="">
              <input type="hidden" name="croquisSvg" class="croquis-svg-hidden" value="">
              <input type="hidden" name="grupoMedidaUuid" class="grupo-medida-uuid-hidden" value="">
              <input type="hidden" name="lineaPrincipalMedida" class="linea-principal-medida-hidden" value="0">
              <input type="hidden" class="linea-medida-clave-hidden" value="">
          </td>

          <td class="text-center">
              <button type="button" class="btn btn-danger btn-sm btn-eliminar-fila">
                  <i class="bi bi-trash"></i>
              </button>
          </td>
       `;

       tablaBody.appendChild(tr);

       activarSelect2(tr.querySelector(".seleccionar-producto"));
       bindFilaEvents(tr);
       recalcularFila(tr);
       return tr;
   }

  async function cargarStockSedeActiva(tr, idProducto) {
      try {
          const resp = await fetch(
              `/interno/ventas/disponibilidad-producto/${idProducto}`
          );

          if (!resp.ok) {
              tr.dataset.stock = "0";
              tr.dataset.completasDisponibles = "0";
              tr.dataset.mediasDirectas = "0";
              tr.dataset.mediasPotenciales = "0";
              tr.dataset.medidaVendibleTotal = "0";
              return 0;
          }

          const data = await resp.json();

          if (data.ok === false) {
              throw new Error(
                  data.mensaje ||
                  "No se pudo consultar la disponibilidad física."
              );
          }

          const stock = Number(data.stockDisponible || 0);

          tr.dataset.stock =
              String(stock);

          tr.dataset.completasDisponibles =
              String(Number(data.completasDisponibles || 0));

          tr.dataset.mediasDirectas =
              String(Number(data.mediasDirectas || 0));

          tr.dataset.mediasPotenciales =
              String(Number(data.mediasPotenciales || 0));

          tr.dataset.retazosDisponibles =
              String(Number(data.retazosDisponibles || 0));

          tr.dataset.medidaVendibleTotal =
              String(Number(data.medidaVendibleTotal || 0));

          tr.dataset.unidadMedidaFisica =
              String(data.unidadMedida || "").toUpperCase();

          tr.dataset.tipoMedidaFisica =
              String(data.tipoMedida || "").toUpperCase();

          return stock;

      } catch (e) {
          console.error(
              "Error cargando disponibilidad real:",
              e
          );

          tr.dataset.stock = "0";
          tr.dataset.completasDisponibles = "0";
          tr.dataset.mediasDirectas = "0";
          tr.dataset.mediasPotenciales = "0";
          tr.dataset.medidaVendibleTotal = "0";

          return 0;
      }
  }

  function obtenerDisponibilidadPresentacionFila(tr, presentacion) {
      if (!tr || !presentacion) {
          return {
              disponible: 0,
              tipo: "DESCONOCIDA"
          };
      }

      const tipoCalculo = String(
          presentacion.tipoCalculoVenta || ""
      ).trim().toUpperCase();

      const nombre = String(
          presentacion.nombrePresentacion || ""
      ).trim().toUpperCase();

      const esCompleta =
          tipoCalculo === "COMPLETA"
          || tipoCalculo === "UNIDAD_COMPLETA"
          || nombre.includes("PLANCHA COMPLETA")
          || nombre === "PLANCHA"
          || nombre.includes("VARILLA COMPLETA")
          || nombre === "VARILLA"
          || nombre.includes("ROLLO COMPLETO");

      const esMedia =
          tipoCalculo === "MEDIA"
          || tipoCalculo === "MEDIA_UNIDAD"
          || nombre.includes("MEDIA PLANCHA")
          || nombre === "MEDIA"
          || nombre.includes("1/2 PLANCHA");

      const esAreaSuelta =
          tipoCalculo === "PIE2"
          || tipoCalculo === "PIE_2"
          || tipoCalculo === "M2"
          || tipoCalculo === "METRO_CUADRADO"
          || tipoCalculo === "AREA"
          || nombre.includes("PIE²")
          || nombre.includes("PIE2")
          || nombre.includes("PIE CUADRADO")
          || nombre.includes("METRO CUADRADO");

      const esLongitudSuelta =
          tipoCalculo === "METRO_LINEAL"
          || tipoCalculo === "LONGITUD"
          || tipoCalculo === "METRO"
          || nombre.includes("METRO LINEAL");

      if (esCompleta) {
          return {
              disponible: Math.max(
                  0,
                  Number(tr.dataset.completasDisponibles || 0)
              ),
              tipo: "COMPLETA"
          };
      }

      if (esMedia) {
          return {
              disponible: Math.max(
                  0,
                  Number(tr.dataset.mediasPotenciales || 0)
              ),
              tipo: "MEDIA"
          };
      }

      if (esAreaSuelta || esLongitudSuelta) {
          return {
              disponible: Math.max(
                  0,
                  Number(tr.dataset.medidaVendibleTotal || 0)
              ),
              tipo: esAreaSuelta
                  ? "MEDIDA_AREA"
                  : "MEDIDA_LONGITUD"
          };
      }

      /*
       * Respaldo para productos normales que no manejan partes.
       */
      const stock = Math.max(
          0,
          Number(tr.dataset.stock || 0)
      );

      const factor = Number(
          presentacion.factorConversionStock || 0
      );

      return {
          disponible:
              factor > 0
                  ? Math.floor(stock / factor)
                  : 0,
          tipo: "EQUIVALENTE"
      };
  }

   async function cargarPresentaciones(tr, idProducto) {

       const selectPresentacion = tr.querySelector(".presentacion-select");
       const unidadTexto = tr.querySelector(".unidad-input");
       const unidadHidden = tr.querySelector(".unidad-hidden");
       const precio = tr.querySelector(".precio");
       const desc = tr.querySelector(".descuento");
       const factorHidden = tr.querySelector(".factor-stock-hidden");

       selectPresentacion.innerHTML = `<option value="">Cargando...</option>`;
       selectPresentacion.disabled = true;
       tr.dataset.estadoPresentaciones = "CARGANDO";
       actualizarBotonPreparacion(tr);

       precio.value = "0.00";
       desc.value = "0.00";
       desc.dataset.maximo = 0;
       desc.max = 0;
       factorHidden.value = 1;

       try {
           const resp = await fetch(`/interno/productos/${idProducto}/presentaciones-venta`);
           if (!resp.ok) throw new Error("Respuesta inválida al cargar presentaciones");
           const data = await resp.json();

           if (tr.querySelector(".seleccionar-producto")?.value !== String(idProducto)) return;

           selectPresentacion.innerHTML = `<option value="">Seleccione presentación</option>`;

         if (!data || data.length === 0) {
             selectPresentacion.innerHTML = `<option value="">Sin presentaciones activas</option>`;
             tr.dataset.estadoPresentaciones = "SIN_DATOS";
             selectPresentacion.disabled = true;
             actualizarBotonPreparacion(tr);
             return;
         }

         const presentacionesVendibles = data.filter(p => {
             const disponibilidad =
                 obtenerDisponibilidadPresentacionFila(
                     tr,
                     p
                 );

             return disponibilidad.disponible > 0;
         });

        if (presentacionesVendibles.length === 0) {
            selectPresentacion.innerHTML =
                `<option value="">Sin stock físico disponible</option>`;

            tr.dataset.estadoPresentaciones =
                "SIN_STOCK";

            selectPresentacion.disabled =
                true;

            mostrarAyudaPresentacion(
                tr,
                "No hay completas, medias ni medida vendible disponible para este producto.",
                true
            );

            actualizarBotonPreparacion(tr);
            return;
        }

         presentacionesVendibles.forEach(p => {
               const opt = document.createElement("option");
               const disponibilidadPresentacion =
                   obtenerDisponibilidadPresentacionFila(
                       tr,
                       p
                   );

               opt.value = p.idPrecioPresentacion;
              opt.textContent = p.nombrePresentacion;

               opt.dataset.precio = p.precioVenta || 0;
               opt.dataset.descuentoMaximo = p.descuentoMaximo || 0;
               opt.dataset.idUnidadVenta = p.idUnidadVenta || 0;
               opt.dataset.nombreUnidadVenta = p.nombreUnidadVenta || "";
               opt.dataset.abreviaturaUnidadVenta = p.abreviaturaUnidadVenta || "";
               opt.dataset.factorConversionStock = p.factorConversionStock || 1;
               opt.dataset.tipoCalculoVenta = p.tipoCalculoVenta || "";
               opt.dataset.tipoCalculoVentaOriginal = p.tipoCalculoVentaOriginal || p.tipoCalculoVenta || "";
               opt.dataset.cantidadIncluida = p.cantidadIncluida || 0;
               opt.dataset.largoBase = p.largoBase || 0;
               opt.dataset.anchoBase = p.anchoBase || 0;
               opt.dataset.medidaTotalBase = p.medidaTotalBase || 0;
               opt.dataset.productoTieneMedida = p.productoTieneMedida ? "1" : "0";
               opt.dataset.tipoMedidaProducto = p.tipoMedidaProducto || "";
               opt.dataset.datosTecnicosCargados = "1";
               opt.dataset.disponibilidadFisica =
                   String(disponibilidadPresentacion.disponible);

               opt.dataset.tipoDisponibilidadFisica =
                   disponibilidadPresentacion.tipo;

               selectPresentacion.appendChild(opt);
           });

           const selectedId = selectPresentacion.dataset.selected;

           if (selectedId) {
               selectPresentacion.value = selectedId;

               const optSeleccionada = selectPresentacion.options[selectPresentacion.selectedIndex];

               if (optSeleccionada && optSeleccionada.value) {
                   precio.value = Number(optSeleccionada.dataset.precio || 0).toFixed(2);

                   desc.value = desc.value || "0.00";
                   desc.max = Number(optSeleccionada.dataset.descuentoMaximo || 0);
                   desc.dataset.maximo = Number(optSeleccionada.dataset.descuentoMaximo || 0);
                   desc.readOnly = false;

                   unidadHidden.value = optSeleccionada.dataset.idUnidadVenta || 0;
                   factorHidden.value = optSeleccionada.dataset.factorConversionStock || 1;
                   tr.dataset.unidadVentaNombre = optSeleccionada.dataset.nombreUnidadVenta || "";
                   tr.dataset.unidadVentaAbrev = optSeleccionada.dataset.abreviaturaUnidadVenta || "";
                   tr.dataset.tipoCalculoVenta = optSeleccionada.dataset.tipoCalculoVenta || "";
                   tr.dataset.tipoCalculoVentaOriginal = optSeleccionada.dataset.tipoCalculoVentaOriginal || "";
                   tr.dataset.cantidadIncluida = optSeleccionada.dataset.cantidadIncluida || 0;
                   tr.dataset.largoBase = optSeleccionada.dataset.largoBase || 0;
                   tr.dataset.anchoBase = optSeleccionada.dataset.anchoBase || 0;
                   tr.dataset.medidaTotalBase = optSeleccionada.dataset.medidaTotalBase || 0;
                   tr.dataset.productoTieneMedida = optSeleccionada.dataset.productoTieneMedida || "0";
                   tr.dataset.tipoMedidaProducto = optSeleccionada.dataset.tipoMedidaProducto || "";
                   tr.dataset.tipoMedidaPreparacion = obtenerTipoMedidaFila(tr);
               }
           }

           selectPresentacion.disabled = false;
           tr.dataset.estadoPresentaciones = "LISTA";
           actualizarBotonPreparacion(tr);

       } catch (e) {
           console.error(e);
           selectPresentacion.innerHTML = `<option value="">Error al cargar</option>`;
           selectPresentacion.disabled = true;
           tr.dataset.estadoPresentaciones = "ERROR";
           actualizarBotonPreparacion(tr);
       }
   }


function bindFilaEvents(tr) {

    const cantInput = tr.querySelector(".cantidad");
    const precioInput = tr.querySelector(".precio");
    const descInput = tr.querySelector(".descuento");
    const presentacionSelect = tr.querySelector(".presentacion-select");


    // 🔥 Evitar números negativos
  cantInput.addEventListener("input", () => {

      let valor = cantInput.value.replace(/\D/g, "");

      if (valor === "") {
          cantInput.value = "";
          recalcularFila(tr);
          return;
      }

      cantInput.value = valor;

      validarStock(tr);
      recalcularFila(tr);

  });

  cantInput.addEventListener("blur", () => {

      if (!cantInput.value || Number(cantInput.value) < 1) {
          cantInput.value = 1;
      }

      validarStock(tr);
      recalcularFila(tr);

  });

    precioInput.addEventListener("input", () => {

        if (precioInput.value < 1) {
            precioInput.value = 1.00;
        }

        recalcularFila(tr);

    });

    descInput.addEventListener("input", () => {

        if (descInput.value < 0) descInput.value = 0;

        const maximo = Number(descInput.dataset.maximo || 0);
        const cantidad = Number(tr.querySelector(".cantidad").value || 0);
        const precio = Number(tr.querySelector(".precio").value || 0);
        const subtotalFila = cantidad * precio;

        if (maximo > 0 && Number(descInput.value) > maximo) {
            descInput.value = maximo.toFixed(2);
            Swal.fire("Descuento no permitido", "Supera el descuento máximo del producto.", "warning");
        }

        if (Number(descInput.value) >= subtotalFila) {
            descInput.value = Math.max(subtotalFila - 0.01, 0).toFixed(2);
            Swal.fire("Descuento inválido", "El descuento no puede ser igual o mayor al subtotal.", "warning");
        }

        recalcularFila(tr);
    });

    presentacionSelect?.addEventListener("change", () => {

        const opt = presentacionSelect.options[presentacionSelect.selectedIndex];
        limpiarPreparacionFila(tr);
        limpiarDatosPresentacionFila(tr);

        if (!opt || !opt.value) {
            tr.querySelector(".precio").value = "0.00";
            cantInput.value = "0";
            cantInput.readOnly = true;

            const desc = tr.querySelector(".descuento");
            desc.value = "0.00";
            desc.dataset.maximo = 0;
            desc.max = 0;
            desc.readOnly = true;

            recalcularFila(tr);
            actualizarBotonPreparacion(tr);
            return;
        }

        const precio = tr.querySelector(".precio");
        const desc = tr.querySelector(".descuento");

        precio.value = Number(opt.dataset.precio || 0).toFixed(2);
        precio.readOnly = true;

        desc.value = "0.00";
        desc.max = Number(opt.dataset.descuentoMaximo || 0);
        desc.dataset.maximo = Number(opt.dataset.descuentoMaximo || 0);
        desc.readOnly = false;
        desc.placeholder = "Máx: " + Number(opt.dataset.descuentoMaximo || 0).toFixed(2);
        desc.title = "Descuento máximo permitido: S/ " + Number(opt.dataset.descuentoMaximo || 0).toFixed(2);



        tr.querySelector(".unidad-hidden").value =
            opt.dataset.idUnidadVenta || 0;

        tr.querySelector(".factor-stock-hidden").value =
            opt.dataset.factorConversionStock || 1;

            tr.dataset.unidadVentaNombre = opt.dataset.nombreUnidadVenta || "";
            tr.dataset.unidadVentaAbrev = opt.dataset.abreviaturaUnidadVenta || "";
            tr.dataset.tipoCalculoVenta = opt.dataset.tipoCalculoVenta || "";
            tr.dataset.tipoCalculoVentaOriginal = opt.dataset.tipoCalculoVentaOriginal || "";
            tr.dataset.cantidadIncluida = opt.dataset.cantidadIncluida || 0;
            tr.dataset.largoBase = opt.dataset.largoBase || 0;
            tr.dataset.anchoBase = opt.dataset.anchoBase || 0;
            tr.dataset.medidaTotalBase = opt.dataset.medidaTotalBase || 0;
            tr.dataset.productoTieneMedida = opt.dataset.productoTieneMedida || "0";
            tr.dataset.tipoMedidaProducto = opt.dataset.tipoMedidaProducto || "";
            tr.dataset.tipoMedidaPreparacion = obtenerTipoMedidaFila(tr);

            const esLineaMedidas = tr.dataset.origenLinea === "MEDIDAS";
            cantInput.value = esLineaMedidas ? "0" : "1";
            cantInput.readOnly = esLineaMedidas;

            actualizarBotonPreparacion(tr);

        validarStock(tr);
        recalcularFila(tr);
    });

    tr.querySelector(".btn-eliminar-fila").addEventListener("click", () => {
        const grupo = obtenerGrupoMedidaFila(tr);
        if (grupo) {
            eliminarLineaGrupoMedidas(tr);
            return;
        }
        tr.remove();
        recalcularTotales();
    });
    tr.querySelector(".btn-config-preparacion")?.addEventListener("click", () => {
        if (obtenerGrupoMedidaFila(tr)) {
            iniciarEdicionGrupoMedidas(tr);
            return;
        }
        abrirModalPreparacion(tr);
    });

    actualizarBotonPreparacion(tr);
    configurarAccionesGrupoMedidas(tr);
    aplicarEtiquetasTablaFila(tr);
}



    /* ============================================================================
       VALIDACIÓN DE STOCK
============================================================================ */
  function validarStock(tr) {
      if (!tr) return true;

      const cantidadInput =
          tr.querySelector(".cantidad");

      const presentacionSelect =
          tr.querySelector(".presentacion-select");

      const opcion =
          presentacionSelect?.selectedOptions?.[0];

      if (!cantidadInput || !opcion?.value) {
          return true;
      }

      const cantidad =
          Number(cantidadInput.value || 0);

      if (!Number.isFinite(cantidad) || cantidad <= 0) {
          return true;
      }

      const tipoCalculo = String(
          opcion.dataset.tipoCalculoVenta || ""
      ).trim().toUpperCase();

      const nombre = String(
          opcion.textContent || ""
      ).trim().toUpperCase();

      const esCompleta =
          tipoCalculo === "COMPLETA"
          || nombre.includes("PLANCHA COMPLETA")
          || nombre === "PLANCHA"
          || nombre.includes("VARILLA COMPLETA")
          || nombre === "VARILLA";

      const esMedia =
          tipoCalculo === "MEDIA"
          || nombre.includes("MEDIA PLANCHA")
          || nombre === "MEDIA"
          || nombre.includes("1/2 PLANCHA");

      const esAreaSuelta =
          tipoCalculo === "PIE2"
          || tipoCalculo === "M2"
          || tipoCalculo === "METRO_CUADRADO"
          || tipoCalculo === "AREA"
          || nombre.includes("PIE²")
          || nombre.includes("PIE2")
          || nombre.includes("PIE CUADRADO");

      let maximoDisponible = 0;
      let descripcionDisponible = "";

      if (esCompleta) {
          maximoDisponible =
              Number(
                  tr.dataset.completasDisponibles || 0
              );

          descripcionDisponible =
              `${maximoDisponible} plancha(s) completa(s)`;

      } else if (esMedia) {
          maximoDisponible =
              Number(
                  tr.dataset.mediasPotenciales || 0
              );

          const directas =
              Number(
                  tr.dataset.mediasDirectas || 0
              );

          descripcionDisponible =
              `${maximoDisponible} media(s) potencial(es)`
              + ` (${directas} directa(s))`;

      } else if (esAreaSuelta) {
          maximoDisponible =
              Number(
                  tr.dataset.medidaVendibleTotal || 0
              );

          const unidad =
              tr.dataset.unidadMedidaFisica === "PIE2"
                  ? "pie²"
                  : (
                      tr.dataset.unidadMedidaFisica
                      || "medida"
                  );

          descripcionDisponible =
              `${maximoDisponible} ${unidad}`;

      } else {
          /*
           * Respaldo para presentaciones que todavía funcionan
           * únicamente con stock equivalente.
           */
          const stock =
              Number(tr.dataset.stock || 0);

          const factor =
              Number(
                  tr.querySelector(
                      ".factor-stock-hidden"
                  )?.value || 1
              );

          maximoDisponible =
              factor > 0
                  ? Math.floor(
                      stock / factor
                  )
                  : 0;

          descripcionDisponible =
              `${maximoDisponible} unidad(es)`;
      }

      if (cantidad <= maximoDisponible) {
          return true;
      }

      cantidadInput.value =
          String(
              Math.max(
                  0,
                  maximoDisponible
              )
          );

      Swal.fire({
          icon: "warning",
          title: "Stock físico insuficiente",
          text:
              `La cantidad máxima disponible para `
              + `${opcion.textContent.trim()} es `
              + `${descripcionDisponible}.`,
          confirmButtonText: "Entendido"
      });

      recalcularFila(tr);

      return false;
  }


    /* ============================================================================
       CÁLCULOS DE TOTALES
============================================================================ */
  function recalcularFila(tr) {

      const prod = tr.querySelector(".seleccionar-producto")?.value;

      if (!prod || prod.trim() === "") {
          tr.querySelector(".precio").value = "0.00";
          tr.querySelector(".descuento").value = "0.00";
          tr.querySelector(".subtotal-cell").textContent = "0.00";
          recalcularTotales();
          return;
      }

      const c = parseFloat(tr.querySelector(".cantidad").value) || 0;
      const p = parseFloat(tr.querySelector(".precio").value) || 0;
      const d = parseFloat(tr.querySelector(".descuento").value) || 0;

      tr.querySelector(".subtotal-cell").textContent =
          f2(Math.max(0, c * p - d));

      recalcularTotales();
  }

    function recalcularTotales() {


        let suma = 0;

        let totalDescuento = 0;
        let totalItems = 0;

       tablaBody.querySelectorAll("tr").forEach(tr => {

           suma += Number(
               tr.querySelector(".subtotal-cell").textContent
           ) || 0;

           const cantidad = Number(
               tr.querySelector(".cantidad")?.value || 0
           );

           const descuento = Number(
               tr.querySelector(".descuento")?.value || 0
           );

           totalItems += cantidad;
           totalDescuento += descuento;

       });

        const lblItems = document.getElementById("lblItems");
        const lblDescuento = document.getElementById("lblDescuento");

       const total = suma;
       const esNotaVenta = (tipoComp?.value || "").toUpperCase() === "NOTA";

       const subtotal = esNotaVenta
           ? Number(total.toFixed(2))
           : Number((total / 1.18).toFixed(2));

       const igv = esNotaVenta
           ? 0
           : Number((total - subtotal).toFixed(2));

       // Mostrar en pantalla
       document.getElementById("lblSubtotal").textContent = "S/ " + f2(subtotal);
       document.getElementById("lblIgv").textContent       = "S/ " + f2(igv);
       document.getElementById("lblTotal").textContent     = "S/ " + f2(total);

       // Ocultar IGV cuando sea Nota de Venta
       const filaIgv = document.getElementById("filaIgvVenta");
       const labelSubtotal = document.getElementById("labelSubtotalVenta");

       if (filaIgv) {
           filaIgv.classList.toggle("d-none", esNotaVenta);
       }

       if (labelSubtotal) {
           labelSubtotal.textContent = esNotaVenta
               ? "Importe de venta"
               : "Subtotal gravado";
       }

        lblItems.textContent =
            `${totalItems} ítems`;

        lblDescuento.textContent =
            "S/ " + f2(totalDescuento);

        // Guardar valor para enviar a Spring
        totalVentaInput.value = f2(total);

        actualizarCredito();
    }


    /* ============================================================================
       CRÉDITO
============================================================================ */
    function actualizarCredito() {

        if (!formaPago) return;

        const total = Number(totalVentaInput.value || 0);
        let inicial = Number(pagoInicial.value || 0);
        const creditoBloqueado = total < 1;

        [pagoInicial, numeroCuotas, frecuenciaCuotas, fechaPrimeraCuota, btnGenerarCuotas]
            .forEach(el => {
                if (el) el.disabled = creditoBloqueado;
            });

        if (formaPago.value === "CREDITO") {

            bloqueCredito.classList.remove("d-none");
            if (creditoBloqueado) {
                deudaVenta.value = "0.00";

                cuotasPreviewBody.innerHTML = `
                    <tr>
                        <td colspan="3" class="text-center text-muted py-3">
                            Primero selecciona productos válidos para calcular la deuda.
                        </td>
                    </tr>
                `;

                return;
            }

            if (inicial < 0) inicial = 0;

            if (total > 0 && inicial >= total) {
                inicial = Math.max(total - 1, 0);
                pagoInicial.value = inicial.toFixed(2);
            }

            const deuda = Math.max(total - inicial, 0);
            deudaVenta.value = deuda.toFixed(2);

        } else {

            bloqueCredito.classList.add("d-none");
            pagoInicial.value = 0;
            deudaVenta.value = "0.00";

            if (numeroCuotas) {
                numeroCuotas.disabled = true;
                numeroCuotas.removeAttribute("required");
            }

            if (frecuenciaCuotas) {
                frecuenciaCuotas.disabled = true;
            }

            if (fechaPrimeraCuota) {
                fechaPrimeraCuota.disabled = true;
                fechaPrimeraCuota.removeAttribute("required");
            }

            if (cuotasPreviewBody) {
                cuotasPreviewBody.innerHTML = `
                    <tr>
                        <td colspan="3" class="text-center text-muted py-3">
                            Selecciona los datos del crédito y genera las cuotas.
                        </td>
                    </tr>
                `;
            }
        }
    }

    function sumarFecha(fecha, frecuencia, indice) {

        const nueva = new Date(fecha);

        if (frecuencia === "DIARIO") {
            nueva.setDate(nueva.getDate() + indice);
        }

        if (frecuencia === "SEMANAL") {
            nueva.setDate(nueva.getDate() + (indice * 7));
        }

        if (frecuencia === "MENSUAL") {
            nueva.setMonth(nueva.getMonth() + indice);
        }

        return nueva.toISOString().split("T")[0];
    }

    function generarCuotasPreview() {

        if (formaPago.value !== "CREDITO") return;

        const deuda = Number(deudaVenta.value || 0);
        const cuotas = Number(numeroCuotas.value || 0);
        const frecuencia = frecuenciaCuotas.value;
        const fechaInicio = fechaPrimeraCuota.value;

        let errores = [];

        if (deuda <= 0) {
            errores.push("La deuda debe ser mayor a S/ 0.00.");
        }

       if (cuotas <= 0 || cuotas > 24) {
           errores.push("Las cuotas deben estar entre 1 y 24.");
       }

        if (!fechaInicio) {
            errores.push("Seleccione la fecha de la primera cuota.");
        }

        if (errores.length > 0) {
            Swal.fire({
                icon: "warning",
                title: "Datos incompletos",
                html: errores.join("<br>")
            });
            return;
        }

        const montoBase = Math.floor((deuda / cuotas) * 100) / 100;
        let acumulado = 0;

        cuotasPreviewBody.innerHTML = "";

        for (let i = 1; i <= cuotas; i++) {

            let monto = montoBase;

            if (i === cuotas) {
                monto = Number((deuda - acumulado).toFixed(2));
            }

            acumulado += monto;

            const fecha = frecuencia === "PERSONALIZADO"
                ? fechaInicio
                : sumarFecha(fechaInicio, frecuencia, i - 1);

            cuotasPreviewBody.innerHTML += `
                <tr>
                    <td>
                        <span class="badge bg-primary-subtle text-primary">
                            Cuota ${i}
                        </span>
                        <input type="hidden" name="nroCuota" value="${i}">
                    </td>

                    <td>
                        <input type="date"
                               name="fechaVencimiento"
                               class="form-control form-control-sm"
                               value="${fecha}">
                    </td>

                    <td class="text-end">
                        <input type="number"
                               name="montoCuota"
                               class="form-control form-control-sm text-end"
                               value="${monto.toFixed(2)}"
                               step="0.01"
                               min="0.01"
                               readonly>
                    </td>
                </tr>
            `;
        }
    }

    formaPago?.addEventListener("change", () => {
        actualizarCredito();

        if (formaPago.value === "CREDITO" && fechaPrimeraCuota && !fechaPrimeraCuota.value) {
            const hoy = new Date();
            hoy.setMonth(hoy.getMonth() + 1);
            fechaPrimeraCuota.value = hoy.toISOString().split("T")[0];
        }
    });

    pagoInicial?.addEventListener("input", () => {
        actualizarCredito();
    });

    numeroCuotas?.addEventListener("input", generarCuotasPreview);
    frecuenciaCuotas?.addEventListener("change", generarCuotasPreview);
    fechaPrimeraCuota?.addEventListener("change", generarCuotasPreview);
    btnGenerarCuotas?.addEventListener("click", generarCuotasPreview);


    /* ============================================================================
       COMPROBANTE – OBTENER SIGUIENTE NÚMERO
============================================================================ */
    async function actualizarNumeroComprobante() {
        if (!tipoComp.value) {
            nroInput.value = "";
            return;
        }

        const resp = await fetch(`/interno/ventas/siguiente-numero?tipo=${tipoComp.value}`);
        nroInput.value = await resp.text();
    }

    tipoComp?.addEventListener("change", actualizarNumeroComprobante);
    tipoComp?.addEventListener("change", recalcularTotales);


/* ============================================================================
   CLIENTE – BASE INTERNA → API EXTERNA → INGRESO MANUAL
============================================================================ */
function documentoClienteValido(documento) {
    return /^\d{8}$/.test(documento) || /^\d{11}$/.test(documento);
}

function calcularYDistribuirMedidas(tr, cortes) {
    const tipo = obtenerTipoMedidaFila(tr);
    const limite = 10000;
    const entradas = [];

    cortes.forEach(corte => {
        const cantidad = Number(corte.cantidad);
        if (!Number.isInteger(cantidad) || cantidad <= 0 || cantidad > limite) {
            throw new Error("La cantidad de piezas debe ser un entero mayor que cero.");
        }
        for (let i = 0; i < cantidad; i++) entradas.push({ ...corte, cantidad: 1 });
        if (entradas.length > limite) throw new Error("La solicitud excede el máximo de cortes permitido.");
    });

    let total = 0;
    let sobrante = 0;
    let distribuidos = [];
    let unidades = 0;

    if (tipo === "LONGITUD") {
        const base = Number(tr.dataset.medidaTotalBase || 0);
        if (!Number.isFinite(base) || base <= 0) throw new Error("La presentación no tiene longitud base válida.");
        entradas.forEach(c => {
            const longitud = Number(c.longitudM);
            if (!Number.isFinite(longitud) || longitud <= 0 || longitud > 100000) throw new Error("Ingrese una longitud válida.");
            if (longitud > base) throw new Error(`El corte de ${longitud} m no cabe en una varilla de ${base} m.`);
        });
        entradas.sort((a, b) => Number(b.longitudM) - Number(a.longitudM));
        const barras = [];
        entradas.forEach(c => {
            const longitud = Number(c.longitudM);
            let indice = barras.findIndex(usado => usado + longitud <= base + 1e-9);
            if (indice < 0) {
                barras.push(0);
                indice = barras.length - 1;
            }
            barras[indice] += longitud;
            total += longitud;
            distribuidos.push({ tipo: "LONGITUD", longitudM: longitud, cantidad: 1, piezaOrigen: indice + 1 });
        });
        unidades = barras.length;
        sobrante = unidades * base - total;
    } else if (tipo === "AREA") {
        const largo = Number(tr.dataset.largoBase || 0);
        const ancho = Number(tr.dataset.anchoBase || 0);
        if (![largo, ancho].every(n => Number.isFinite(n) && n > 0)) throw new Error("La presentación no tiene dimensiones base válidas.");
        entradas.forEach(c => {
            const w = Number(c.anchoCm);
            const h = Number(c.altoCm);
            if (![w, h].every(n => Number.isFinite(n) && n > 0 && n <= 100000)) throw new Error("Ingrese ancho y alto válidos.");
            if (!((w <= largo && h <= ancho) || (h <= largo && w <= ancho))) {
                throw new Error(`El corte ${w} x ${h} cm no cabe en una plancha de ${largo} x ${ancho} cm.`);
            }
        });
        entradas.sort((a, b) => (Number(b.anchoCm) * Number(b.altoCm)) - (Number(a.anchoCm) * Number(a.altoCm)));
        const planchas = [];
        const colocar = (plancha, corte) => {
            const opciones = [[Number(corte.anchoCm), Number(corte.altoCm)], [Number(corte.altoCm), Number(corte.anchoCm)]];
            for (const [w, h] of opciones) {
                for (const estante of plancha) {
                    if (h <= estante.alto && estante.usado + w <= largo + 1e-9) {
                        estante.usado += w;
                        return true;
                    }
                }
                const y = plancha.reduce((acc, e) => acc + e.alto, 0);
                if (w <= largo && y + h <= ancho + 1e-9) {
                    plancha.push({ alto: h, usado: w });
                    return true;
                }
            }
            return false;
        };
        entradas.forEach(c => {
            let indice = planchas.findIndex(p => colocar(p, c));
            if (indice < 0) {
                planchas.push([]);
                indice = planchas.length - 1;
                colocar(planchas[indice], c);
            }
            total += Number(c.anchoCm) * Number(c.altoCm);
            distribuidos.push({ tipo: "AREA", anchoCm: Number(c.anchoCm), altoCm: Number(c.altoCm), cantidad: 1, piezaOrigen: indice + 1 });
        });
        unidades = planchas.length;
        const unidad = `${tr.dataset.unidadVentaNombre || ""} ${tr.dataset.unidadVentaAbrev || ""}`.toLowerCase();
        const divisor = unidad.includes("pie") || unidad.includes("ft") ? 929.0304 : 10000;
        sobrante = ((unidades * largo * ancho) - total) / divisor;
        total /= divisor;
    } else {
        throw new Error("Esta presentación se vende por cantidad y no requiere medidas.");
    }

    const tipoOriginal = (tr.dataset.tipoCalculoVentaOriginal || "").toUpperCase();
    const cantidadComercial = ["PIE2", "M2", "METRO_CUADRADO", "METRO_LINEAL", "AREA", "LONGITUD"].includes(tipoOriginal)
        ? total
        : unidades;
    return {
        cortes: distribuidos,
        cantidadComercial: Number(cantidadComercial.toFixed(6)),
        medidaTotal: Number(total.toFixed(6)),
        unidadesFisicas: unidades,
        sobrante: Number(Math.max(sobrante, 0).toFixed(6))
    };
}

function stockYaAgregadoEnVenta(trActual, idProducto) {
    return [...document.querySelectorAll("#detalleBody tr")]
        .filter(tr => tr !== trActual && tr.querySelector(".seleccionar-producto")?.value === String(idProducto))
        .reduce((acc, tr) => acc + Number(tr.dataset.stockCalculado || 0), 0);
}

async function validarStockCalculadoAntesAgregar(tr) {
    const idProducto = tr.querySelector(".seleccionar-producto")?.value;
    const factor = Number(tr.querySelector(".factor-stock-hidden")?.value || 1);
    const cantidad = Number(tr.querySelector(".cantidad")?.value || 0);
    const necesario = cantidad * factor;
    const respuesta = await fetch(`/interno/ventas/disponibilidad-producto/${idProducto}`);
    if (!respuesta.ok) throw new Error("No fue posible consultar el stock disponible.");
    const data = await respuesta.json();
    const disponible = Number(data.stockDisponible || 0) - stockYaAgregadoEnVenta(tr, idProducto);
    if (necesario > disponible + 1e-9) {
        throw new Error(`Stock insuficiente. Necesario: ${necesario.toFixed(6)}. Disponible: ${Math.max(disponible, 0).toFixed(6)}.`);
    }
    tr.dataset.stockCalculado = necesario.toFixed(6);
    tr.dataset.stock = Number(data.stockDisponible || 0);
    return true;
}

function nombreClienteValido(nombre) {
    const comparable = String(nombre || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "")
        .replace(/[^A-Za-z0-9]+/g, " ").trim().toUpperCase();
    const bloqueados = new Set(["CLIENTE", "PRUEBA", "SIN NOMBRE", "CONSUMIDOR", "CONSUMIDOR FINAL", "N A", "NA", "AAA", "123"]);
    return comparable.length >= 3 && /[A-Z]/.test(comparable) && !bloqueados.has(comparable);
}

function mostrarEstadoConsultaCliente(origen, mensaje, activo = true) {
    const configuracion = {
        BASE_INTERNA: ["Cliente registrado", activo ? "text-bg-success" : "text-bg-warning"],
        API_EXTERNA: ["Datos consultados", "text-bg-primary"],
        MANUAL: ["Completar datos manualmente", "text-bg-warning"],
        NO_ENCONTRADO: ["Completar datos manualmente", "text-bg-warning"]
    }[origen] || ["Cliente pendiente", "text-bg-secondary"];
    clienteConsultaBadge.textContent = configuracion[0];
    clienteConsultaBadge.className = `badge ${configuracion[1]}`;
    clienteConsultaMensaje.textContent = mensaje || "";
    clienteConsultaEstado.classList.remove("d-none");
}

function configurarEdicionCliente(origen) {
    const interno = origen === "BASE_INTERNA";
    inputNom.readOnly = interno || origen === "API_EXTERNA";
    inputTel.readOnly = interno;
    inputCorreo.readOnly = interno;
    inputDireccion.readOnly = interno;
}

function limpiarVinculoCliente(limpiarDatos = true) {
    inputIdCli.value = "";
    inputOrigenCliente.value = "";
    inputClienteNombres.value = "";
    inputClienteApellidos.value = "";
    inputClienteRazonSocial.value = "";
    documentoClienteConsultado = "";
    clienteConsultaEstado.classList.add("d-none");
    configurarEdicionCliente("MANUAL");
    if (limpiarDatos) {
        inputNom.value = "";
        inputTel.value = "";
        inputCorreo.value = "";
        inputDireccion.value = "";
    }
}

function aplicarClienteConsultado(cliente) {
    inputOrigenCliente.value = cliente.origen;
    inputIdCli.value = cliente.idCliente || "";
    inputNom.value = cliente.nombre || "";
    inputTel.value = cliente.telefono || "";
    inputCorreo.value = cliente.correo || "";
    inputDireccion.value = cliente.direccion || "";
    inputClienteNombres.value = cliente.nombres || "";
    inputClienteApellidos.value = cliente.apellidos || "";
    inputClienteRazonSocial.value = cliente.razonSocial || "";
    documentoClienteConsultado = cliente.documento;
    configurarEdicionCliente(cliente.origen);
    mostrarEstadoConsultaCliente(cliente.origen, cliente.mensaje, cliente.activo);
}

async function buscarCliente() {
    const doc = inputDoc.value.trim();
    if (consultaClienteEnCurso) return;
    if (!documentoClienteValido(doc)) {
        limpiarVinculoCliente(false);
        mostrarEstadoConsultaCliente("MANUAL", "Ingresa exactamente 8 dígitos para DNI o 11 para RUC.");
        inputDoc.focus();
        return;
    }
    consultaClienteEnCurso = true;
    btnBuscarCliente.disabled = true;
    btnBuscarCliente.setAttribute("aria-busy", "true");
    const contenidoBoton = btnBuscarCliente.innerHTML;
    btnBuscarCliente.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span>';
    try {
        const respuesta = await fetch(`/interno/ventas/api/clientes/documento/${encodeURIComponent(doc)}`, {
            headers: { Accept: "application/json" }
        });
        const cliente = await respuesta.json();
        if (!respuesta.ok) throw new Error(cliente.mensaje || "El documento no es válido.");
        aplicarClienteConsultado(cliente);
        ajustarComprobantePorDocumento();
    } catch (e) {
        limpiarVinculoCliente(true);
        inputOrigenCliente.value = "MANUAL";
        documentoClienteConsultado = doc;
        mostrarEstadoConsultaCliente("MANUAL", e.message || "No se pudo consultar. Completa los datos manualmente.");
    } finally {
        consultaClienteEnCurso = false;
        btnBuscarCliente.disabled = false;
        btnBuscarCliente.removeAttribute("aria-busy");
        btnBuscarCliente.innerHTML = contenidoBoton;
    }
}



/* ============================================================================
   AJUSTE AUTOMÁTICO DEL COMPROBANTE SEGÚN DOCUMENTO (CON FILTRO DE OPCIONES)
============================================================================ */
function ajustarComprobantePorDocumento() {

    const doc = inputDoc.value.trim();

    // Obtener todas las opciones del select
    const opcionBoleta  = tipoComp.querySelector('option[value="BOLETA"]');
    const opcionFactura = tipoComp.querySelector('option[value="FACTURA"]');
    const opcionNota    = tipoComp.querySelector('option[value="NOTA"]');

    // Primero habilitamos todas para luego filtrar
    opcionBoleta.hidden = false;
    opcionFactura.hidden = false;
    opcionNota.hidden = false;

    /* ------------------------------------------------------------------
       11 dígitos = RUC → SOLO FACTURA
    ------------------------------------------------------------------ */
    if (/^\d{11}$/.test(doc)) {

        tipoComp.value = "FACTURA";
        tipoComp.disabled = false;
        tipoComp.readOnly = true;

        // Ocultar las demás
        opcionBoleta.hidden = true;
        opcionNota.hidden = true;

        actualizarNumeroComprobante();
        return;
    }

   /* ------------------------------------------------------------------
      8 dígitos = DNI → BOLETA o NOTA (NO FACTURA)
   ------------------------------------------------------------------ */
   if (/^\d{8}$/.test(doc)) {

       tipoComp.disabled = false;

       opcionFactura.hidden = true;

       if (inputNom.value.trim()) {
           tipoComp.value = "BOLETA";
       } else {
           tipoComp.value = "NOTA";
       }

       actualizarNumeroComprobante();
       return;
   }


    /* ------------------------------------------------------------------
       Documento inválido → SOLO NOTA
    ------------------------------------------------------------------ */
    tipoComp.disabled = false;

    // Ocultar boleta y factura
    opcionBoleta.hidden = true;
    opcionFactura.hidden = true;

    tipoComp.value = "NOTA";
    actualizarNumeroComprobante();
}
/* ============================================================================
   AUTOCOMPLETAR COMPROBANTE AL ESCRIBIR DNI/RUC
============================================================================ */
inputDoc.addEventListener("input", () => {
    const documentoActual = inputDoc.value.trim();
    if (documentoClienteConsultado && documentoActual !== documentoClienteConsultado) {
        limpiarVinculoCliente(true);
    }
    ajustarComprobantePorDocumento();
});


/* ============================================================================
   LISTENERS DEL MÓDULO CLIENTE
============================================================================ */
btnBuscarCliente?.addEventListener("click", buscarCliente);
inputDoc?.addEventListener("keyup", e => {
    if (e.key === "Enter") {
        e.preventDefault();
        buscarCliente();
    }
});

if (inputIdCli?.value && inputDoc?.value) {
    inputOrigenCliente.value = "BASE_INTERNA";
    documentoClienteConsultado = inputDoc.value.trim();
    configurarEdicionCliente("BASE_INTERNA");
    mostrarEstadoConsultaCliente("BASE_INTERNA", "Cliente vinculado al borrador.");
}


/* ============================================================================
   VALIDACIÓN FINAL DEL FORMULARIO
============================================================================ */
let envioVentaEnCurso = false;
formVenta?.addEventListener("submit", e => {
    if (envioVentaEnCurso) {
        e.preventDefault();
        return;
    }
    let errores = [];

    const doc = inputDoc.value.trim();
    const nombre = inputNom.value.trim();
    const tipoComprobante = tipoComp.value;
    const total = Number(totalVentaInput.value || 0);
    const origenCliente = inputOrigenCliente.value;

    /* =========================================================================
       VALIDAR DOCUMENTO SEGÚN COMPROBANTE
    ============================================================================ */
    if (tipoComprobante === "BOLETA") {

        if (!/^\d{8}$/.test(doc)) {
            errores.push("Ingrese un DNI válido de 8 dígitos.");
        }

        if (documentoClienteConsultado !== doc || !origenCliente) {
            errores.push("Consulte el DNI antes de guardar la boleta.");
        }

    }

    else if (tipoComprobante === "FACTURA") {

        if (!/^\d{11}$/.test(doc)) {
            errores.push("Ingrese un RUC válido de 11 dígitos.");
        }

        if (documentoClienteConsultado !== doc || !origenCliente) {
            errores.push("Consulte el RUC antes de guardar la factura.");
        }

        if (!inputDireccion.value.trim()) {
            errores.push("La dirección fiscal es obligatoria para emitir una factura.");
        }

    }

    // NOTA → NO OBLIGA DOCUMENTO

    if (!nombreClienteValido(nombre)) {
        errores.push(tipoComprobante === "FACTURA"
            ? "Ingrese una razón social real y válida."
            : "Ingrese un nombre real y válido para el cliente.");
    }

    if (inputTel.value && !/^\d{7,15}$/.test(inputTel.value.trim())) {
        errores.push("El teléfono debe contener entre 7 y 15 dígitos.");
    }

    if (inputCorreo.value && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(inputCorreo.value.trim())) {
        errores.push("Ingrese un correo electrónico válido.");
    }

    if (/^\d{8}$/.test(doc) && !inputClienteNombres.value.trim()) {
        inputClienteNombres.value = nombre;
    }
    if (/^\d{11}$/.test(doc) && !inputClienteRazonSocial.value.trim()) {
        inputClienteRazonSocial.value = nombre;
    }

    if (!tipoComprobante) {
        errores.push("Seleccione el tipo de comprobante.");
    }

    if (tablaBody.children.length === 0) {
        errores.push("Debe agregar al menos un producto.");
    }

    tablaBody.querySelectorAll("tr").forEach((tr, index) => {

        const fila = index + 1;

        const prod = tr.querySelector(".seleccionar-producto").value;
        const presentacion = tr.querySelector(".presentacion-select")?.value;
        const unidad = tr.querySelector(".unidad-hidden").value;
        const cant = Number(tr.querySelector(".cantidad").value || 0);
        const precio = Number(tr.querySelector(".precio").value || 0);
        const descInput = tr.querySelector(".descuento");
        const descuento = Number(descInput.value || 0);
        const descuentoMaximo = Number(descInput.dataset.maximo || 0);
        const requierePreparacion = tr.querySelector(".requiere-preparacion-hidden")?.value === "1";
        const tipoPreparacion = tr.querySelector(".tipo-preparacion-hidden")?.value || "TAL_CUAL";
        const cortesJson = tr.querySelector(".cortes-json-hidden")?.value || "";

        if (!prod) {
            errores.push(`Fila ${fila}: seleccione un producto.`);
        }

        if (!presentacion) {
            errores.push(`Fila ${fila}: seleccione una presentación de venta.`);
        }

        if (!unidad || Number(unidad) <= 0) {
            errores.push(`Fila ${fila}: la unidad del producto no es válida.`);
        }

        if (cant <= 0) {
            errores.push(`Fila ${fila}: la cantidad debe ser mayor a 0.`);
        }

        if (precio <= 0) {
            errores.push(`Fila ${fila}: el precio debe ser mayor a 0.`);
        }

        if (descuento < 0) {
            errores.push(`Fila ${fila}: el descuento no puede ser negativo.`);
        }

        if (descuentoMaximo > 0 && descuento > descuentoMaximo) {
            errores.push(`Fila ${fila}: el descuento no puede superar S/ ${descuentoMaximo.toFixed(2)}.`);
        }

        if (descuento >= cant * precio) {
            errores.push(`Fila ${fila}: el descuento no puede ser igual o mayor al subtotal.`);
        }

        const subtotalFila = (cant * precio) - descuento;

        if (subtotalFila < 1) {
            errores.push(`Fila ${fila}: el subtotal mínimo permitido es S/ 1.00.`);
        }
        if (tipoPreparacion !== "TAL_CUAL" && !requierePreparacion) {
            errores.push(`Fila ${fila}: la preparación del producto no es válida.`);
        }

      if (requierePreparacion) {
          if (!cortesJson) {
              errores.push(`Fila ${fila}: registre al menos un corte para la preparación.`);
          } else {
                try {
                     const datosCortes = JSON.parse(cortesJson);
                     const cortes = Array.isArray(datosCortes) ? datosCortes : (datosCortes.cortes || []);
                    const esGrupoCalculado = !Array.isArray(datosCortes)
                        && typeof datosCortes.codigoCombinacion === "string"
                        && datosCortes.codigoCombinacion.trim() !== "";

                    if (!Array.isArray(cortes) || cortes.length === 0) {
                        errores.push(`Fila ${fila}: registre al menos un corte válido.`);
                    }

                   const tipoMedidaFila = obtenerTipoMedidaFila(tr);
                   if (esGrupoCalculado) {
                       cortes.forEach((c, i) => {
                           const cantidadCorte = Number(c?.cantidad || 0);
                           if (!Number.isInteger(cantidadCorte) || cantidadCorte <= 0) {
                               errores.push(`Fila ${fila}, corte ${i + 1}: la cantidad debe ser un entero mayor a cero.`);
                               return;
                           }
                           if (tipoMedidaFila === "LONGITUD") {
                               if (Number(c?.longitud || 0) <= 0) {
                                   errores.push(`Fila ${fila}, corte ${i + 1}: la longitud debe ser mayor a cero.`);
                               }
                           } else if (Number(c?.ancho || 0) <= 0 || Number(c?.alto || 0) <= 0) {
                               errores.push(`Fila ${fila}, corte ${i + 1}: el ancho y alto deben ser mayores a cero.`);
                           }
                       });
                   } else {
                   const unidadTexto = nombreUnidadValidacion(tipoMedidaFila);
                   const totalCortes = calcularUsoPorPieza(cortes, tipoMedidaFila);

                   const tieneMedidaFila = tr.dataset.productoTieneMedida === "1";

                   if ((tipoMedidaFila === "AREA" || tipoMedidaFila === "LONGITUD") && !tieneMedidaFila) {
                       errores.push(`Fila ${fila}: el producto no tiene medida física registrada. No puede tener cortes.`);
                   }


                   const validacionPorPieza = validarCortesPorPiezaFisica(tr, cortes, tipoMedidaFila);

                   if (!validacionPorPieza.ok) {
                       errores.push(`Fila ${fila}: ${validacionPorPieza.mensaje}`);
                   }

                   const validacionMedidas = validarMedidasIndividuales(tr, cortes, tipoMedidaFila);

                   if (!validacionMedidas.ok) {
                       errores.push(`Fila ${fila}: ${validacionMedidas.mensaje}`);
                   }

                   if (tipoMedidaFila === "AREA" && !esVentaPorAreaSuelta(tr)) {
                       const cantidadPiezas = obtenerCantidadPiezasFisicas(tr);

                       for (let i = 1; i <= cantidadPiezas; i++) {
                           const cortesPieza = cortesDePieza(cortes, i);
                           const validacionDistribucion = validarDistribucionAreaSimple(cortesPieza);

                           if (!validacionDistribucion.ok) {
                               errores.push(`Fila ${fila}, Plancha ${i}: ${validacionDistribucion.mensaje}`);
                               break;
                           }
                       }
                   }

                    if (!Array.isArray(cortes) || cortes.length === 0) {
                        errores.push(`Fila ${fila}: registre al menos un corte válido.`);
                    }

                     cortes.forEach((c, i) => {

                        if (c.tipo === "LONGITUD") {
                            if (Number(c.longitudM || 0) <= 0 || Number(c.cantidad || 0) <= 0) {
                                errores.push(`Fila ${fila}, corte ${i + 1}: la longitud y cantidad deben ser mayores a cero.`);
                            }
                        } else {
                            if (Number(c.anchoCm || 0) <= 0 || Number(c.altoCm || 0) <= 0 || Number(c.cantidad || 0) <= 0) {
                                errores.push(`Fila ${fila}, corte ${i + 1}: el ancho, alto y cantidad deben ser mayores a cero.`);
                            }
                        }

                     });
                   }

                } catch (e) {
                    errores.push(`Fila ${fila}: los datos de corte no son válidos.`);
                }
            }
        }

    });

    if (total < 1) {
        errores.push("El total mínimo de la venta debe ser S/ 1.00.");
    }
    if (tipoEntrega?.value === "PROGRAMADA") {
        if (!fechaRecojoProgramadaFecha?.value) {
            errores.push("Seleccione la fecha de recojo programado.");
        }

        if (!fechaRecojoProgramadaInput?.value) {
            errores.push("Seleccione un horario disponible para el recojo programado.");
            mostrarAlertaRecojo("Debe seleccionar un horario disponible para el recojo programado.");
        }
    }

    if (formaPago.value === "CREDITO") {
        const inicial = Number(pagoInicial.value || 0);

        const deuda = Number(deudaVenta.value || 0);
        const cuotas = Number(numeroCuotas.value || 0);
        const fechas = document.querySelectorAll("input[name='fechaVencimiento']");
        const montos = document.querySelectorAll("input[name='montoCuota']");

        if (cuotas <= 0 || cuotas > 24) {
            errores.push("Las cuotas deben estar entre 1 y 24.");
        }

        if (fechas.length !== cuotas || montos.length !== cuotas) {
            errores.push("Debe generar las cuotas antes de registrar la venta.");
        }

        let sumaCuotas = 0;

        montos.forEach(m => {
            sumaCuotas += Number(m.value || 0);
        });

        if (Number(sumaCuotas.toFixed(2)) !== Number(deuda.toFixed(2))) {
            errores.push("La suma de las cuotas debe ser igual a la deuda.");
        }

        fechas.forEach((f, index) => {
            if (!f.value) {
                errores.push(`Cuota ${index + 1}: seleccione fecha de vencimiento.`);
            }
        });

        if (inicial < 0) {
            errores.push("El pago inicial no puede ser negativo.");
        }

        if (inicial >= total) {
            errores.push("En crédito, el pago inicial debe ser menor al total.");
        }
    }

    if (errores.length > 0) {
        e.preventDefault();

        Swal.fire({
            icon: "error",
            title: "Revisa la venta",
            html: errores.join("<br>"),
            confirmButtonText: "Entendido"
        });

        return;
    }

    function codificarBase64Url(texto) {
        if (!texto) return "";

        const base64 = btoa(unescape(encodeURIComponent(texto)));

        return "B64:" + base64
            .replaceAll("+", "-")
            .replaceAll("/", "_")
            .replaceAll("=", "");
    }

    tablaBody.querySelectorAll(".cortes-json-hidden").forEach(input => {
        if (input.value && !input.value.startsWith("B64:")) {
            input.value = codificarBase64Url(input.value);
        }
    });

    tablaBody.querySelectorAll(".croquis-svg-hidden").forEach(input => {
        if (input.value && !input.value.startsWith("B64:")) {
            input.value = codificarBase64Url(input.value);
        }
    });

    envioVentaEnCurso = true;
});






/* ============================================================================
   AGREGAR POR MEDIDAS - FLUJO AUTOMÁTICO
============================================================================ */
const mdModalEl = document.getElementById("modalVentaMedidasAuto");
const mdModal = mdModalEl ? bootstrap.Modal.getOrCreateInstance(mdModalEl) : null;
const mdProducto = document.getElementById("mdProducto");
const mdProductoAyuda = document.getElementById("mdProductoAyuda");
const mdDatosProducto = document.getElementById("mdDatosProducto");
const mdTipoBadge = document.getElementById("mdTipoBadge");
const mdUnidadBadge = document.getElementById("mdUnidadBadge");
const mdGrupos = document.getElementById("mdGrupos");
const mdAgregarGrupo = document.getElementById("mdAgregarGrupo");
const mdCalcular = document.getElementById("mdCalcular");
const mdAgregarVenta = document.getElementById("mdAgregarVenta");
const mdResultado = document.getElementById("mdResultado");
const mdEstadoInicial = document.getElementById("mdEstadoInicial");
const mdPresentacionFijaPanel = document.getElementById("mdPresentacionFijaPanel");
const mdPresentacionFija = document.getElementById("mdPresentacionFija");
const mdPresentacionFijaInfo = document.getElementById("mdPresentacionFijaInfo");
const mdEntregarSobrantePanel = document.getElementById("mdEntregarSobrantePanel");
const mdEntregarSobranteAyuda = document.getElementById("mdEntregarSobranteAyuda");
const mdAlternativas = document.getElementById("mdAlternativas");

let mdEstado = {
    producto: null,
    tipo: null,
    unidad: null,
    resultado: null,
    editandoGrupo: null,
    modalidad: "COTIZAR_CORTES",
    presentaciones: [],
    disponibilidadFisica: null,
    codigoRecomendadoInicial: null,
    codigoSeleccionado: null,
    entradaModificada: false,
    resultadoConservado: false
};

let mdSeleccionandoAlternativa = false;
let mdCalculando = false;
let mdGuardandoResultado = false;
let mdEditandoGrupoEnCurso = false;
let mdVersionEntrada = 0;


function decodificarBase64UrlMedidas(valor) {
    if (!valor) return "";
    if (!valor.startsWith("B64:")) return valor;
    try {
        let base64 = valor.slice(4).replaceAll("-", "+").replaceAll("_", "/");
        while (base64.length % 4) base64 += "=";
        return decodeURIComponent(escape(atob(base64)));
    } catch (e) {
        return "";
    }
}

function obtenerGrupoMedidaFila(tr) {
    return (tr?.querySelector(".grupo-medida-uuid-hidden")?.value || tr?.dataset?.grupoMedidaUuid || "").trim();
}

const gruposMedidasControles = document.getElementById("gruposMedidasControles");
const mdEstadosGrupo = new Map();

function esLineaPrincipalMedida(tr) {
    return tr?.querySelector(".linea-principal-medida-hidden")?.value === "1";
}

function filasGrupoMedidas(grupo) {
    if (!grupo) return [];
    return [...tablaBody.querySelectorAll("tr")].filter(f => obtenerGrupoMedidaFila(f) === grupo);
}

function mdNuevaClaveLinea(grupo, idPresentacion) {
    const sufijo = window.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    return `${grupo}:${idPresentacion || "SIN-PRESENTACION"}:${sufijo}`;
}

function mdClaveLineaFila(tr) {
    return (tr?.querySelector(".linea-medida-clave-hidden")?.value || tr?.dataset?.lineaMedidaClave || "").trim();
}

function mdPlanosDeLinea(planoCorte, linea) {
    if (!planoCorte || !Array.isArray(planoCorte.piezas)) {
        return [];
    }

    const numeros = new Set(
        (linea?.numerosCorte || []).map(Number)
    );

    const idPresentacion = String(
        linea?.idPrecioPresentacion ?? ""
    );

    return planoCorte.piezas.filter(pieza => {
        const coincidePresentacion =
            idPresentacion &&
            String(pieza.idPrecioPresentacion ?? "") === idPresentacion;

        const cortesPieza = Array.isArray(pieza.cortes)
            ? pieza.cortes
            : [];

        const cortesBloque = Array.isArray(pieza.bloqueComercial?.cortes)
            ? pieza.bloqueComercial.cortes
            : [];

        const coincideCorte = [...cortesPieza, ...cortesBloque]
            .some(corte =>
                numeros.has(Number(corte.numero))
            );

        return coincidePresentacion || coincideCorte;
    });
}

function mdCrearEstadoGrupo(grupo, respuesta, datos = {}) {
    const combinacion = respuesta?.combinacionRecomendada || {};
    const lineas = (combinacion.lineas || []).map(linea => {
        const claveLinea = linea.claveLinea || mdNuevaClaveLinea(grupo, linea.idPrecioPresentacion);
        const planos = mdPlanosDeLinea(respuesta?.planoCorte, linea);
        return {
            ...linea,
            claveLinea,
            numerosCorte: Array.isArray(linea.numerosCorte) ? linea.numerosCorte.map(Number) : [],
            planosAsociados: planos.map(p => p.codigo || `PLANO-${p.numero}`),
            existente: true
        };
    });
    return {
        grupo,
        producto: datos.producto || mdEstado.producto,
        tipoMedida: datos.tipoMedida || respuesta?.tipoMedida || mdEstado.tipo,
        unidadEntrada: datos.unidadEntrada || respuesta?.unidadEntrada || mdEstado.unidad,
        modalidad: datos.modalidad || mdEstado.modalidad,
        idPrecioPresentacion: datos.idPrecioPresentacion ?? (mdEstado.modalidad === "COMPRAR_PRESENTACION" ? Number(mdPresentacionFija?.value || 0) : null),
       cantidadPresentacion:
           datos.cantidadPresentacion ??
           respuesta?.combinacionRecomendada?.lineas?.[0]?.cantidad ??
           null,
        codigoCombinacion: combinacion.codigo || datos.codigoCombinacion || null,
        cortes: datos.cortes || mdObtenerCortes(),
        entregarSobrante: datos.entregarSobrante ?? Boolean(document.getElementById("mdEntregarSobrante")?.checked),
        observacion: datos.observacion ?? (document.getElementById("mdObservacion")?.value || ""),
        respuesta: { ...respuesta, combinacionRecomendada: { ...combinacion, lineas } },
        lineas
    };
}

function mdEstadoGrupoDesdeDom(grupo) {
    const filas = filasGrupoMedidas(grupo);
    const principal = filas.find(esLineaPrincipalMedida) || filas[0];
    if (!principal) return null;
    let guardado = {};
    try {
        const raiz = JSON.parse(decodificarBase64UrlMedidas(principal.querySelector(".cortes-json-hidden")?.value || "{}"));
        guardado = Array.isArray(raiz) ? { cortes: raiz } : raiz;
    } catch (e) {
        guardado = {};
    }
    const ui = guardado.uiGrupoMedidas || {};
    const lineasGuardadas = Array.isArray(ui.lineas) ? ui.lineas : [];
    const lineas = filas.map((fila, indice) => {
        const idPresentacion = Number(fila.querySelector(".presentacion-select")?.value || 0);
        const guardada = lineasGuardadas.find(l => String(l.claveLinea || "") === mdClaveLineaFila(fila))
            || lineasGuardadas.find(l => Number(l.idPrecioPresentacion || 0) === idPresentacion && !l.__usada);
        if (guardada) guardada.__usada = true;
        const claveLinea = guardada?.claveLinea || mdClaveLineaFila(fila) || mdNuevaClaveLinea(grupo, idPresentacion);
        const hiddenClave = fila.querySelector(".linea-medida-clave-hidden");
        if (hiddenClave) hiddenClave.value = claveLinea;
        fila.dataset.lineaMedidaClave = claveLinea;
        return {
            ...(guardada || {}),
            claveLinea,
            idPrecioPresentacion,
            nombrePresentacion: fila.querySelector(".presentacion-select")?.selectedOptions?.[0]?.textContent?.trim() || guardada?.nombrePresentacion || "Presentación",
            cantidad: Number(fila.querySelector(".cantidad")?.value || 0),
            precioUnitario: Number(fila.querySelector(".precio")?.value || 0),
            subtotal: Number(fila.querySelector(".subtotal-cell")?.textContent || 0),
            factorConversionStock: Number(fila.querySelector(".factor-stock-hidden")?.value || 1),
            lineaPrincipal: indice === 0,
            numerosCorte: Array.isArray(guardada?.numerosCorte) ? guardada.numerosCorte.map(Number) : [],
            planosAsociados: Array.isArray(guardada?.planosAsociados) ? guardada.planosAsociados : [],
            existente: true
        };
    });
    lineasGuardadas.forEach(l => delete l.__usada);
    const selectProducto = principal.querySelector(".seleccionar-producto");
    const respuesta = ui.respuesta || {
        ok: true,
        tipoMedida: ui.tipoMedida || obtenerTipoMedidaFila(principal),
        unidadEntrada: ui.unidadEntrada || (obtenerTipoMedidaFila(principal) === "AREA" ? "MM" : "CM"),
        consumoFisico: ui.consumoFisico || { unidadesFisicas: 0, cantidadCortes: 0, medidaTotal: 0, sobrante: 0, aprovechamientoPorcentaje: 0 },
        planoCorte: ui.planoCorte || null,
        combinacionRecomendada: { codigo: guardado.codigoCombinacion || null, motivo: "Grupo existente", precioTotal: lineas.reduce((t, l) => t + l.subtotal, 0), materialAdquirido: 0, piezasFisicas: 0, lineas },
        alternativas: [], advertencias: [], errores: []
    };
    respuesta.combinacionRecomendada = { ...(respuesta.combinacionRecomendada || {}), lineas };
    return {
        grupo,
        producto: ui.producto || { id: selectProducto?.value, text: selectProducto?.selectedOptions?.[0]?.textContent?.trim() || "Producto por medidas", stock: Number(principal.dataset.stock || 0) },
        tipoMedida: ui.tipoMedida || respuesta.tipoMedida,
        unidadEntrada: ui.unidadEntrada || respuesta.unidadEntrada,
        modalidad: guardado.modalidad || ui.modalidad || "COTIZAR_CORTES",
        idPrecioPresentacion: guardado.idPrecioPresentacion ?? ui.idPrecioPresentacion ?? null,
        cantidadPresentacion: guardado.cantidadPresentacion ?? ui.cantidadPresentacion ?? null,
        codigoCombinacion: guardado.codigoCombinacion || ui.codigoCombinacion || respuesta.combinacionRecomendada?.codigo || null,
        cortes: Array.isArray(guardado.cortesCalculoOriginal)
            ? guardado.cortesCalculoOriginal
            : (Array.isArray(guardado.cortes) ? guardado.cortes : []),
        entregarSobrante: principal.querySelector(".entregar-sobrante-hidden")?.value === "1",
        observacion: principal.querySelector(".observacion-preparacion-hidden")?.value || "",
        respuesta,
        lineas
    };
}

function mdEstadoGrupo(grupo) {
    const actual = mdEstadosGrupo.get(grupo);
    if (actual) return actual;
    const reconstruido = mdEstadoGrupoDesdeDom(grupo);
    if (reconstruido) mdEstadosGrupo.set(grupo, reconstruido);
    return reconstruido;
}

function mdActualizarEstadoGrupoDesdeFilas(estado) {
    if (!estado) return;
    filasGrupoMedidas(estado.grupo).forEach(fila => {
        const linea = estado.lineas.find(l => l.claveLinea === mdClaveLineaFila(fila));
        if (!linea) return;
        linea.descuento = Number(fila.querySelector(".descuento")?.value || 0);
        linea.subtotal = Number(fila.querySelector(".subtotal-cell")?.textContent || 0);
        linea.existente = fila.dataset.lineaExistente !== "0";
    });
}

function mdPlanoActivoGrupo(estado) {
    const plano = estado?.respuesta?.planoCorte;

    if (!plano || !Array.isArray(plano.piezas)) {
        return plano || null;
    }

    const lineas = estado?.lineas || [];

    const codigos = new Set(
        lineas.flatMap(
            linea => linea.planosAsociados || []
        )
    );

    const idsPresentacion = new Set(
        lineas
            .map(
                linea => String(
                    linea.idPrecioPresentacion ?? ""
                )
            )
            .filter(Boolean)
    );

    const piezasFiltradas = plano.piezas.filter(pieza => {
        const codigoPlano =
            pieza.codigo || `PLANO-${pieza.numero}`;

        return codigos.has(codigoPlano)
            || idsPresentacion.has(
                String(pieza.idPrecioPresentacion ?? "")
            );
    });

    /*
     * Si solo existe una línea y no fue posible reconstruir la asociación,
     * el plano completo pertenece a esa misma línea.
     */
    if (
        piezasFiltradas.length === 0
        && lineas.length === 1
    ) {
        return {
            ...plano,
            piezas: plano.piezas
        };
    }

    return {
        ...plano,
        piezas: piezasFiltradas
    };
}

function mdResultadoActivoGrupo(estado) {
    const base = estado?.respuesta || {};
    const lineas = (estado?.lineas || []).filter(l => l.existente !== false);
    const planoCorte = mdPlanoActivoGrupo({ ...estado, lineas });
    const precioTotal = lineas.reduce((total, l) => total + Number(l.subtotal || 0), 0);
    const numeros = new Set(lineas.flatMap(l => l.numerosCorte || []).map(Number));
    return {
        ...base,
        planoCorte,
        combinacionRecomendada: {
            ...(base.combinacionRecomendada || {}),
            codigo: estado.codigoCombinacion,
            precioTotal,
            piezasFisicas: planoCorte?.piezas?.length || 0,
            lineas
        },
        consumoFisico: {
            ...(base.consumoFisico || {}),
            unidadesFisicas: planoCorte?.piezas?.length || 0,
            cantidadCortes: numeros.size || Number(base.consumoFisico?.cantidadCortes || 0)
        },
        alternativas: []
    };
}

function mdSerializarGrupo(estado) {
    const respuestaActiva = mdResultadoActivoGrupo(estado);
    return JSON.stringify({
        unidadEntrada: estado.unidadEntrada,
        modalidad: estado.modalidad,
        idPrecioPresentacion: estado.idPrecioPresentacion,
        cantidadPresentacion: estado.cantidadPresentacion,
        codigoCombinacion: estado.codigoCombinacion,
        cortes: mdCortesActivos(estado),
        cortesCalculoOriginal: estado.cortes,
        uiGrupoMedidas: {
            producto: estado.producto,
            tipoMedida: estado.tipoMedida,
            unidadEntrada: estado.unidadEntrada,
            modalidad: estado.modalidad,
            idPrecioPresentacion: estado.idPrecioPresentacion,
            cantidadPresentacion: estado.cantidadPresentacion,
            codigoCombinacion: estado.codigoCombinacion,
            lineas: estado.lineas,
            planoCorte: respuestaActiva.planoCorte,
            consumoFisico: respuestaActiva.consumoFisico,
            respuesta: respuestaActiva
        }
    });
}

function mdSincronizarGrupoDom(grupo) {
    const estado = mdEstadoGrupo(grupo);
    const filas = filasGrupoMedidas(grupo);
    if (!estado || !filas.length) return;
    filas.forEach((fila, indice) => {
        const linea = estado.lineas.find(l => l.claveLinea === mdClaveLineaFila(fila));
        if (linea) {
            linea.lineaPrincipal = indice === 0;
            linea.existente = true;
        }
        fila.querySelector(".linea-principal-medida-hidden").value = indice === 0 ? "1" : "0";
        fila.dataset.modalidadMedidas = estado.modalidad;
        fila.dataset.idPresentacionMedidas = String(linea?.idPrecioPresentacion ?? "");
        fila.dataset.numerosCorte = JSON.stringify(linea?.numerosCorte || []);
        fila.dataset.planosLinea = JSON.stringify(linea?.planosAsociados || []);
        fila.dataset.lineaExistente = "1";
        const esPrincipal = indice === 0;
        fila.querySelector(".requiere-preparacion-hidden").value = esPrincipal ? "1" : "0";
        fila.querySelector(".tipo-preparacion-hidden").value = esPrincipal ? "CORTE" : "TAL_CUAL";
        fila.querySelector(".entregar-sobrante-hidden").value = esPrincipal && estado.entregarSobrante ? "1" : "0";
        fila.querySelector(".observacion-preparacion-hidden").value = esPrincipal ? estado.observacion : "";
        fila.querySelector(".cortes-json-hidden").value = esPrincipal ? mdSerializarGrupo(estado) : "";
        fila.querySelector(".croquis-svg-hidden").value = esPrincipal ? mdCroquisPlanoReal(mdPlanoActivoGrupo(estado)) : "";
        configurarAccionesGrupoMedidas(fila);
    });
    mdRenderizarControlesGrupos();
}

function mdEtiquetaModalidad(modalidad) {
    return modalidad === "COMPRAR_PRESENTACION" ? "Compra por presentación" : "Cotización de cortes";
}

function aplicarEtiquetasTablaFila(tr) {
    [...(tr?.children || [])].forEach((td, indice) => {
        td.dataset.label = ["Producto", "Presentación", "Cantidad", "Precio", "Descuento", "Subtotal", "Plano", "Eliminar"][indice] || "";
    });
}

function mdRenderizarControlesGrupos() {
    if (!gruposMedidasControles) return;
    const grupos = [...new Set([...tablaBody.querySelectorAll("tr")].map(obtenerGrupoMedidaFila).filter(Boolean))];
    gruposMedidasControles.classList.toggle("d-none", grupos.length === 0);
    gruposMedidasControles.innerHTML = grupos.map((grupo, indice) => {
        const estado = mdEstadoGrupo(grupo);
        const modalidad = mdEtiquetaModalidad(estado?.modalidad);
        const producto = mdEscaparHtml(estado?.producto?.text || "Producto por medidas");
        return `<div class="grupo-medidas-control" data-grupo-medidas="${mdEscaparHtml(grupo)}">
            <span class="grupo-medidas-control-numero">${modalidad} ${indice + 1}</span>
            <span class="grupo-medidas-control-producto">${producto}</span>
            <button type="button" class="btn btn-sm btn-outline-primary btn-editar-grupo-medidas" title="Editar este grupo" aria-label="Editar ${modalidad} ${indice + 1}">
                <i class="bi bi-pencil-square"></i>
            </button>
        </div>`;
    }).join("");
    gruposMedidasControles.querySelectorAll(".btn-editar-grupo-medidas").forEach(btn => {
        btn.addEventListener("click", () => {
            const grupo = btn.closest("[data-grupo-medidas]")?.dataset.grupoMedidas;
            const fila = filasGrupoMedidas(grupo)[0];
            if (fila) iniciarEdicionGrupoMedidas(fila);
        });
    });
}

function configurarAccionesGrupoMedidas(tr) {
    const grupo = obtenerGrupoMedidaFila(tr);
    if (!grupo) return;

    tr.dataset.origenLinea = "MEDIDAS";
    tr.dataset.grupoMedidaUuid = grupo;
    tr.classList.add("linea-medidas-calculada");

    const productoBox = tr.querySelector(".producto-box");
    if (productoBox && !productoBox.querySelector(".badge-medidas-calculadas")) {
        const badge = document.createElement("span");
        badge.className = "badge-medidas-calculadas";
        badge.innerHTML = '<i class="bi bi-rulers"></i> Calculado por medidas';
        productoBox.appendChild(badge);
    }

   tr.querySelectorAll(".cantidad,.precio").forEach(campo => {
       campo.readOnly = true;
   });

   const descuento = tr.querySelector(".descuento");

   if (descuento) {
       descuento.readOnly = false;

       const maximo = Number(descuento.dataset.maximo || 0);

       descuento.max = String(maximo);
       descuento.placeholder = `Máx: ${maximo.toFixed(2)}`;
       descuento.title = `Descuento máximo permitido: S/ ${maximo.toFixed(2)}`;
   }
    const producto = tr.querySelector(".seleccionar-producto");
    const presentacion = tr.querySelector(".presentacion-select");
    [producto, presentacion].forEach(campo => {
        if (!campo) return;
        campo.dataset.bloqueadoMedidas = "1";
        campo.setAttribute("aria-disabled", "true");
        campo.tabIndex = -1;
    });

    const btnEditar = tr.querySelector(".btn-config-preparacion");
    if (btnEditar) {
        btnEditar.classList.add("d-none");
        btnEditar.disabled = true;
        btnEditar.setAttribute("aria-disabled", "true");
    }

    const celdaAcciones = btnEditar?.parentElement;
    if (celdaAcciones && !celdaAcciones.querySelector(".btn-ver-corte-medidas")) {
        const btnVer = document.createElement("button");
        btnVer.type = "button";
        btnVer.className = "btn btn-outline-secondary btn-sm btn-icono-tabla btn-ver-corte-medidas";
        btnVer.innerHTML = '<i class="bi bi-eye"></i>';
        btnVer.title = "Ver planos de esta línea";
        btnVer.setAttribute("aria-label", "Ver planos de esta línea");
        btnVer.addEventListener("click", () => verCorteLineaMedidas(tr));
        celdaAcciones.insertBefore(btnVer, celdaAcciones.querySelector('input[type="hidden"]'));
    }

    const btnEliminar = tr.querySelector(".btn-eliminar-fila");
    if (btnEliminar) {
        btnEliminar.classList.remove("btn-danger");
        btnEliminar.classList.add("btn-outline-danger", "btn-icono-tabla");
        btnEliminar.title = "Eliminar solo esta línea";
        btnEliminar.setAttribute("aria-label", "Eliminar solo esta línea");
        btnEliminar.innerHTML = '<i class="bi bi-trash"></i>';
    }
    aplicarEtiquetasTablaFila(tr);
}

function eliminarLineaGrupoMedidas(tr) {
    const grupo = obtenerGrupoMedidaFila(tr);
    const filas = filasGrupoMedidas(grupo);
    const estado = mdEstadoGrupo(grupo);
    if (!grupo || !filas.length || !estado) return;
    const clave = mdClaveLineaFila(tr);
    estado.lineas = estado.lineas.filter(l => l.claveLinea !== clave);
    tr.dataset.lineaExistente = "0";
    tr.remove();
    if (!filasGrupoMedidas(grupo).length) {
        mdEstadosGrupo.delete(grupo);
        mdRenderizarControlesGrupos();
    } else {
        mdSincronizarGrupoDom(grupo);
    }
    recalcularTotales();
}

function verCorteLineaMedidas(tr) {
    const estado = mdEstadoGrupo(obtenerGrupoMedidaFila(tr));
    const linea = estado?.lineas?.find(
        l => l.claveLinea === mdClaveLineaFila(tr)
    );

    if (!estado || !linea) {
        return;
    }

    const planoCompleto = estado.respuesta?.planoCorte || null;

    let piezas = mdPlanosDeLinea(
        planoCompleto,
        linea
    );

    /*
     * En una compra por presentación normalmente existe una sola línea.
     * Si la asociación por presentación o número de corte no fue
     * reconstruida, se muestran las piezas del plano completo.
     */
    if (
        piezas.length === 0
        && planoCompleto
        && Array.isArray(planoCompleto.piezas)
        && estado.lineas.length === 1
    ) {
        piezas = planoCompleto.piezas;
    }

    const planoLinea = planoCompleto
        ? {
            ...planoCompleto,
            piezas
        }
        : null;

    const contenido = mdCroquisPlanoReal(planoLinea);

    Swal.fire({
        title: mdEscaparHtml(
            linea.nombrePresentacion || "Planos de la línea"
        ),
        html: `
            <div class="text-start croquis-medidas-preview">
                ${contenido}
            </div>
        `,
        width: 900,
        didOpen: popup => mdActivarNavegacionPlano(popup),
        confirmButtonText: "Cerrar"
    });
}

function mdCortesActivos(estado) {
    const numeros = new Set((estado?.lineas || []).flatMap(l => l.numerosCorte || []).map(Number));
    if (!numeros.size) return estado?.cortes || [];
    let numero = 0;
    return (estado.cortes || []).map(corte => {
        let cantidad = 0;
        const total = Math.max(0, Number(corte.cantidad || 0));
        for (let i = 0; i < total; i++) {
            numero++;
            if (numeros.has(numero)) cantidad++;
        }
        return { ...corte, cantidad };
    }).filter(corte => corte.cantidad > 0);
}

async function iniciarEdicionGrupoMedidas(tr) {
    if (mdEditandoGrupoEnCurso) return;
    const grupo = obtenerGrupoMedidaFila(tr);
    const principal = filasGrupoMedidas(grupo)[0];
    const estadoGrupo = mdEstadoGrupo(grupo);
    if (!grupo || !principal || !estadoGrupo || !mdModal) return;
    mdActualizarEstadoGrupoDesdeFilas(estadoGrupo);

    mdResetear();
    mdEstado.editandoGrupo = grupo;
    const selectProducto = principal.querySelector(".seleccionar-producto");
    const idProducto = selectProducto?.value;
    const textoProducto = selectProducto?.selectedOptions?.[0]?.textContent?.trim() || "Producto por medidas";
    if (!idProducto) return;
    mdEditandoGrupoEnCurso = true;

    mdEstado.producto = { ...estadoGrupo.producto, id: idProducto, text: textoProducto };
    if (mdProducto && window.jQuery) {
        const opcion = new Option(textoProducto, idProducto, true, true);
        $(mdProducto).append(opcion).trigger("change");
    }

    try {
        const [respuesta, respuestaStock] = await Promise.all([
            fetch(`/interno/productos/${idProducto}/presentaciones-venta`),
            fetch(`/interno/ventas/disponibilidad-producto/${idProducto}`)
        ]);
        if (!respuesta.ok || !respuestaStock.ok) {
            throw new Error("No se pudo reconstruir la configuración o disponibilidad del producto.");
        }
        const [presentaciones, disponibilidad] = await Promise.all([
            respuesta.json(), respuestaStock.json()
        ]);
        if (disponibilidad.ok === false) throw new Error(disponibilidad.mensaje || "No se pudo consultar el stock disponible.");
       mdEstado.producto.stock =
           Number(disponibilidad.stockDisponible || 0);

       mdEstado.disponibilidadFisica = {
           completasDisponibles:
               Number(disponibilidad.completasDisponibles || 0),

           mediasDirectas:
               Number(disponibilidad.mediasDirectas || 0),

           mediasPotenciales:
               Number(disponibilidad.mediasPotenciales || 0),

           retazosDisponibles:
               Number(disponibilidad.retazosDisponibles || 0),

           medidaVendibleTotal:
               Number(disponibilidad.medidaVendibleTotal || 0),

           unidadMedida:
               String(disponibilidad.unidadMedida || "").toUpperCase(),

           tipoMedida:
               String(disponibilidad.tipoMedida || "").toUpperCase()
       };

       mdEstado.presentaciones =
           Array.isArray(presentaciones) ? presentaciones : [];
        const tipo = [...new Set((presentaciones || []).map(p => String(p.tipoMedidaProducto || "").toUpperCase()))]
            .find(t => t === "AREA" || t === "LONGITUD");
        if (!tipo) throw new Error("El producto ya no está configurado para venta por medidas.");
        mdEstado.tipo = tipo;
        mdEstado.unidad = tipo === "AREA" ? "MM" : "CM";
        mdTipoBadge.textContent = tipo === "AREA" ? "Vidrio / área" : "Aluminio / longitud";
        mdUnidadBadge.textContent = tipo === "AREA" ? "Ingreso en milímetros" : "Ingreso en centímetros";
        mdDatosProducto.classList.remove("d-none");
        mdGrupos.innerHTML = "";

        mdCargarPresentacionesFisicas();
        const cortes = mdCortesActivos(estadoGrupo);
        mdEstado.modalidad = estadoGrupo.modalidad || "COTIZAR_CORTES";
        document.querySelectorAll("input[name='mdModalidad']").forEach(radio => {
            radio.checked = radio.value === mdEstado.modalidad;
        });
        mdPresentacionFijaPanel?.classList.toggle("d-none", mdEstado.modalidad !== "COMPRAR_PRESENTACION");
        if (mdPresentacionFija && estadoGrupo.idPrecioPresentacion) {
            mdPresentacionFija.value = String(estadoGrupo.idPrecioPresentacion);
        }

        mdActualizarInfoPresentacionFija();
       cortes.forEach(c => {
           const cantidadNormalizada =
               Number.isInteger(Number(c.cantidad)) && Number(c.cantidad) > 0
                   ? Number(c.cantidad)
                   : 1;

           mdAgregarFilaGrupo(
               tipo === "AREA"
                   ? {
                       ancho: Number(c.ancho ?? c.anchoMm ?? c.anchoCm ?? 0),
                       alto: Number(c.alto ?? c.altoMm ?? c.altoCm ?? 0),
                       cantidad: cantidadNormalizada
                   }
                   : {
                       longitud: Number(
                           c.longitud ??
                           c.longitudCm ??
                           (Number(c.longitudM || 0) * 100)
                       ),
                       cantidad: cantidadNormalizada
                   }
           );
       });
        if (!mdGrupos.children.length) mdAgregarFilaGrupo();
        const obs = document.getElementById("mdObservacion");
        if (obs) obs.value = principal.querySelector(".observacion-preparacion-hidden")?.value || "";
        const sobrante = document.getElementById("mdEntregarSobrante");
        if (sobrante) sobrante.checked = principal.querySelector(".entregar-sobrante-hidden")?.value === "1";
        const resultadoActivo = mdResultadoActivoGrupo(estadoGrupo);
        mdEstado.resultado = resultadoActivo;
        mdEstado.codigoRecomendadoInicial = estadoGrupo.respuesta?.combinacionRecomendada?.codigo || estadoGrupo.codigoCombinacion;
        mdEstado.codigoSeleccionado = estadoGrupo.codigoCombinacion;
        mdEstado.entradaModificada = false;
        mdEstado.resultadoConservado = true;
        mdPintarResultado(resultadoActivo);
        mdProductoAyuda.textContent = "Editando únicamente este grupo. Puede guardarlo sin recalcular o cambiar medidas y calcular otra combinación.";
        mdModal.show();
        mdActualizarBoton();
    } catch (e) {
        Swal.fire({ icon: "error", title: "No se pudo editar", text: e.message });
    } finally {
        mdEditandoGrupoEnCurso = false;
    }
}

function mdResetear() {
    mdVersionEntrada++;
   mdEstado = {
       producto: null,
       tipo: null,
       unidad: null,
       resultado: null,
       editandoGrupo: null,
       modalidad: "COTIZAR_CORTES",
       presentaciones: [],
       disponibilidadFisica: null,
       codigoRecomendadoInicial: null,
       codigoSeleccionado: null,
       entradaModificada: false,
       resultadoConservado: false
   };
    mdDatosProducto?.classList.add("d-none");
    mdResultado?.classList.add("d-none");
    mdEstadoInicial?.classList.remove("d-none");
    mdAgregarVenta?.classList.add("d-none");
    if (mdGrupos) mdGrupos.innerHTML = "";
    if (mdAlternativas) mdAlternativas.innerHTML = "";
    document.querySelectorAll("input[name='mdModalidad']").forEach(r => {
        r.checked = r.value === "COTIZAR_CORTES";
    });
    mdPresentacionFijaPanel?.classList.add("d-none");
    if (mdPresentacionFija) mdPresentacionFija.innerHTML = "";
    const entregarSobrante = document.getElementById("mdEntregarSobrante");
    const observacion = document.getElementById("mdObservacion");
    if (entregarSobrante) entregarSobrante.checked = false;
    mdActualizarVisibilidadEntregarSobrante();
    if (observacion) observacion.value = "";
    if (mdProductoAyuda) { mdProductoAyuda.textContent = "Busca un vidrio o aluminio configurado para venta por área o longitud."; mdProductoAyuda.classList.remove("text-danger"); }
    if (mdProducto && window.jQuery) $(mdProducto).val(null).trigger("change");
    mdActualizarBoton();
}

function iniciarModalMedidasCalculadas() {
    if (!mdModal) return;
    mdResetear();
    mdModal.show();
}

function configurarProductoMedidasAuto() {
    if (!mdProducto || !window.jQuery) return;
    if ($(mdProducto).hasClass("select2-hidden-accessible")) $(mdProducto).select2("destroy");
    $(mdProducto).select2({
        theme:"bootstrap-5", width:"100%", dropdownParent:$(mdModalEl), minimumInputLength:1,
        ajax:{url:"/interno/productos/buscar-vendibles",dataType:"json",delay:200,data:p=>({term:p.term}),processResults:data=>({results:data.map(p=>({id:p.id,text:p.text,img:p.img||p.foto||"",stock:p.stock,precio:p.precio,unidadNombre:p.unidadNombre,idUnidad:p.idUnidad,descuentoMaximo:p.descuentoMaximo}))})},
        placeholder:"Buscar vidrio o aluminio", allowClear:true, templateResult:formatoProductoMini, templateSelection:i=>i.text||"", escapeMarkup:m=>m
    });
    $(mdProducto).off("select2:select.mdauto").on("select2:select.mdauto", async e=>{
        mdEstado.producto=e.params.data;
        mdEstado.tipo=null;
        mdEstado.unidad=null;
        mdEstado.presentaciones=[];
        mdInvalidarResultado();
        const versionProducto = mdVersionEntrada;
        try {
            mdProductoAyuda.textContent="Verificando configuración técnica...";
            const [r, respuestaStock] = await Promise.all([
                fetch(`/interno/productos/${e.params.data.id}/presentaciones-venta`),
                fetch(`/interno/ventas/disponibilidad-producto/${e.params.data.id}`)
            ]);
            if(!r.ok || !respuestaStock.ok) throw new Error("No se pudieron consultar las presentaciones o el stock disponible.");
            const [data, disponibilidad] = await Promise.all([r.json(), respuestaStock.json()]);
            if (disponibilidad.ok === false) throw new Error(disponibilidad.mensaje || "No se pudo consultar el stock disponible.");
            if (versionProducto !== mdVersionEntrada) return;
           mdEstado.producto.stock =
               Number(disponibilidad.stockDisponible || 0);

           mdEstado.disponibilidadFisica = {
               completasDisponibles:
                   Number(disponibilidad.completasDisponibles || 0),

               mediasDirectas:
                   Number(disponibilidad.mediasDirectas || 0),

               mediasPotenciales:
                   Number(disponibilidad.mediasPotenciales || 0),

               retazosDisponibles:
                   Number(disponibilidad.retazosDisponibles || 0),

               medidaVendibleTotal:
                   Number(disponibilidad.medidaVendibleTotal || 0),

               unidadMedida:
                   String(disponibilidad.unidadMedida || "").toUpperCase(),

               tipoMedida:
                   String(disponibilidad.tipoMedida || "").toUpperCase()
           };

           mdEstado.presentaciones =
               Array.isArray(data) ? data : [];
            const tipos=[...new Set((data||[]).map(p=>String(p.tipoMedidaProducto||"").toUpperCase()).filter(t=>t==="AREA"||t==="LONGITUD"))];
            if(!tipos.length) throw new Error("El producto no está configurado para venta por área o longitud.");
            mdEstado.tipo=tipos[0]; mdEstado.unidad=mdEstado.tipo==="AREA"?"MM":"CM";
            mdTipoBadge.textContent=mdEstado.tipo==="AREA"?"Vidrio / área":"Aluminio / longitud";
            mdUnidadBadge.textContent=mdEstado.tipo==="AREA"?"Ingreso en milímetros":"Ingreso en centímetros";
            mdDatosProducto.classList.remove("d-none"); mdGrupos.innerHTML=""; mdAgregarFilaGrupo();
            mdCargarPresentacionesFisicas();
            mdProductoAyuda.textContent="La presentación será elegida automáticamente según encaje, stock y precio.";
            mdProductoAyuda.classList.remove("text-danger");
        } catch(err) {
            if (versionProducto !== mdVersionEntrada) return;
            mdEstado.tipo=null; mdEstado.unidad=null; mdDatosProducto.classList.add("d-none");
            mdProductoAyuda.textContent=err.message; mdProductoAyuda.classList.add("text-danger");
        }
        mdActualizarBoton();
    });
}

function mdInvalidarResultado() {
    mdVersionEntrada++;
    if (mdEstado.editandoGrupo) mdEstado.entradaModificada = true;
    mdEstado.resultado = null;
    mdResultado?.classList.add("d-none");
    mdEstadoInicial?.classList.remove("d-none");
    mdAgregarVenta?.classList.add("d-none");
}

function mdFormatearDisponibilidad(valor) {
    const numero = Number(valor || 0);

    if (!Number.isFinite(numero)) {
        return "0";
    }

    return numero.toLocaleString("es-PE", {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    });
}

function mdCargarPresentacionesFisicas() {
    if (!mdPresentacionFija) return;

    const presentaciones =
        (mdEstado.presentaciones || [])
            .filter(p => p?.idPrecioPresentacion);

    const disponibilidad =
        mdEstado.disponibilidadFisica || {};

    mdPresentacionFija.innerHTML =
        '<option value="">Seleccione una presentación</option>';

    presentaciones.forEach(p => {
        const tipoCalculo = String(
            p.tipoCalculoVenta || ""
        ).trim().toUpperCase();

        const nombreNormalizado = String(
            p.nombrePresentacion || ""
        ).trim().toUpperCase();

        const esCompleta =
            tipoCalculo === "COMPLETA"
            || nombreNormalizado.includes("PLANCHA COMPLETA")
            || nombreNormalizado === "PLANCHA"
            || nombreNormalizado.includes("VARILLA COMPLETA")
            || nombreNormalizado === "VARILLA"
            || nombreNormalizado.includes("ROLLO COMPLETO");

        const esMedia =
            tipoCalculo === "MEDIA"
            || nombreNormalizado.includes("MEDIA PLANCHA")
            || nombreNormalizado === "MEDIA"
            || nombreNormalizado.includes("1/2 PLANCHA");

        const esPie2 =
            tipoCalculo === "PIE2"
            || nombreNormalizado.includes("PIE²")
            || nombreNormalizado.includes("PIE2")
            || nombreNormalizado.includes("PIE CUADRADO");

        const esAreaSuelta =
            esPie2
            || tipoCalculo === "M2"
            || tipoCalculo === "METRO_CUADRADO"
            || tipoCalculo === "AREA";

        let textoDisponibilidad;

        if (esCompleta) {
            const completas = Number(
                disponibilidad.completasDisponibles || 0
            );

            textoDisponibilidad =
                `${completas} completa${completas === 1 ? "" : "s"} disponible${completas === 1 ? "" : "s"}`;

        } else if (esMedia) {
            const directas = Number(
                disponibilidad.mediasDirectas || 0
            );

            const potenciales = Number(
                disponibilidad.mediasPotenciales || 0
            );

            textoDisponibilidad =
                `${directas} directa${directas === 1 ? "" : "s"}`
                + ` · ${potenciales} potenciales`;

        } else if (esAreaSuelta) {
            const medida = Number(
                disponibilidad.medidaVendibleTotal || 0
            );

            const unidad =
                disponibilidad.unidadMedida === "PIE2"
                    ? "pie²"
                    : String(
                        disponibilidad.unidadMedida
                        || p.abreviaturaUnidadVenta
                        || p.nombreUnidadVenta
                        || ""
                    ).toLowerCase();

            textoDisponibilidad =
                `${mdFormatearDisponibilidad(medida)} ${unidad} disponibles`;

        } else {
            /*
             * Respaldo para presentaciones no clasificadas todavía.
             */
            const factor = Number(
                p.factorConversionStock || 1
            );

            const stockProducto = Number(
                mdEstado.producto?.stock || 0
            );

            const aproximado =
                factor > 0
                    ? Math.floor(stockProducto / factor)
                    : 0;

            textoDisponibilidad =
                `${aproximado} disponibles`;
        }

        const opt = new Option(
            `${p.nombrePresentacion}`
            + ` · ${mdDinero(p.precioVenta)}`
            + ` · ${textoDisponibilidad}`,
            p.idPrecioPresentacion
        );

        opt.dataset.presentacion =
            JSON.stringify(p);

        mdPresentacionFija.appendChild(opt);
    });

    mdActualizarInfoPresentacionFija();
    mdActualizarVisibilidadEntregarSobrante();
}
function mdActualizarInfoPresentacionFija() {
    if (!mdPresentacionFijaInfo) return;

    const opt = mdPresentacionFija?.selectedOptions?.[0];

    if (!opt?.dataset.presentacion) {
        mdPresentacionFijaInfo.textContent =
            "Seleccione una presentación activa.";
        return;
    }

    const p = JSON.parse(opt.dataset.presentacion);
    const tipo = String(
        p.tipoCalculoVenta || "Presentación"
    ).replaceAll("_", " ");

    let medida = "";

    if (mdEstado.tipo === "AREA") {
        const largo = Number(p.largoBase || 0);
        const ancho = Number(p.anchoBase || 0);

        medida = largo > 0 && ancho > 0
            ? `${largo} × ${ancho} cm`
            : "Cantidad comercial calculada por área";
    } else {
        const medidaTotal = Number(p.medidaTotalBase || 0);

        medida = medidaTotal > 0
            ? `${medidaTotal} m`
            : "Cantidad comercial calculada por longitud";
    }

    mdPresentacionFijaInfo.textContent =
        `${tipo} · ${medida} · La cantidad y el total se calcularán automáticamente.`;
}


function mdTiposPresentacionSeleccionada() {
    const tipos = new Set();

    function agregarTipo(tipoValor, nombreValor) {
        const tipo = String(tipoValor || "").trim().toUpperCase();
        const nombre = String(nombreValor || "").trim().toUpperCase();

        if (
            tipo === "MEDIA" || tipo === "MEDIA_PLANCHA" ||
            nombre.includes("MEDIA PLANCHA") || nombre === "MEDIA" ||
            nombre.includes("1/2 PLANCHA")
        ) {
            tipos.add("MEDIA");
        }

        if (
            tipo === "COMPLETA" ||
            nombre.includes("PLANCHA COMPLETA") || nombre === "PLANCHA" ||
            nombre.includes("VARILLA COMPLETA") || nombre === "VARILLA" ||
            nombre.includes("ROLLO COMPLETO")
        ) {
            tipos.add("COMPLETA");
        }
    }

    if (mdEstado.modalidad === "COMPRAR_PRESENTACION") {
        const opt = mdPresentacionFija?.selectedOptions?.[0];
        if (opt?.dataset.presentacion) {
            try {
                const presentacion = JSON.parse(opt.dataset.presentacion);
                agregarTipo(
                    presentacion.tipoCalculoVenta,
                    presentacion.nombrePresentacion
                );
            } catch (_) { /* opción incompleta */ }
        }
        return [...tipos];
    }

    const lineas = Array.isArray(mdEstado.resultado?.combinacionRecomendada?.lineas)
        ? mdEstado.resultado.combinacionRecomendada.lineas
        : [];

    lineas.forEach(linea => agregarTipo(
        linea.tipoCalculoVenta,
        linea.nombrePresentacion
    ));

    return [...tipos];
}

function mdTipoPresentacionSeleccionada() {
    const tipos = mdTiposPresentacionSeleccionada();
    if (tipos.length === 0) return "";
    if (tipos.length === 1) return tipos[0];
    return "MIXTA";
}

function mdActualizarVisibilidadEntregarSobrante() {
    const interruptor = document.getElementById("mdEntregarSobrante");
    const tipos = mdTiposPresentacionSeleccionada();
    const aplica = tipos.includes("MEDIA") || tipos.includes("COMPLETA");

    mdEntregarSobrantePanel?.classList.toggle("d-none", !aplica);

    if (!aplica && interruptor) {
        interruptor.checked = false;
    }

    if (mdEntregarSobranteAyuda && aplica) {
        if (tipos.includes("MEDIA") && tipos.includes("COMPLETA")) {
            mdEntregarSobranteAyuda.textContent =
                "Afecta únicamente a las medias y planchas completas de la combinación. Las líneas por Pie² conservan sus retazos para DentIA.";
        } else if (tipos.includes("MEDIA")) {
            mdEntregarSobranteAyuda.textContent =
                "Activado: se entregan los cortes y el resto de cada media comprada. Desactivado: ese resto se desecha. Las líneas por Pie² no cambian.";
        } else {
            mdEntregarSobranteAyuda.textContent =
                "Activado: se entregan los cortes y el resto de cada plancha completa comprada. Desactivado: ese resto se desecha. Las líneas por Pie² no cambian.";
        }
    }
}

/* Al cambiar la decisión de sobrante se solicita nuevamente el plano para
 * actualizar amarillo/rojo sin alterar las líneas vendidas por Pie². */
const mdEntregarSobranteControl = document.getElementById("mdEntregarSobrante");
if (mdEntregarSobranteControl && !mdEntregarSobranteControl.dataset.listenerPlanoMixto) {
    mdEntregarSobranteControl.dataset.listenerPlanoMixto = "1";
    mdEntregarSobranteControl.addEventListener("change", async () => {
        if (!mdEstado.resultado?.combinacionRecomendada?.codigo || mdCalculando) return;
        const codigo = mdEstado.resultado.combinacionRecomendada.codigo;
        const headers = { "Content-Type": "application/json" };
        if (CSRF_HEADER && CSRF_TOKEN) headers[CSRF_HEADER] = CSRF_TOKEN;

        try {
            const resp = await fetch("/interno/ventas/api/calcular-medidas-provisional", {
                method: "POST",
                headers,
                body: JSON.stringify(mdConstruirSolicitud(codigo))
            });
            const data = await resp.json().catch(() => ({}));
            if (!resp.ok || data.ok === false) {
                throw new Error(data.mensaje || data.errores?.join(" ") || "No fue posible actualizar el plano.");
            }
            mdEstado.resultado = data;
            mdEstado.codigoSeleccionado = data.combinacionRecomendada?.codigo || codigo;
            mdPintarResultado(data);
        } catch (e) {
            mdEntregarSobranteControl.checked = !mdEntregarSobranteControl.checked;
            Swal.fire({ icon: "error", title: "No se pudo actualizar el plano", text: e.message });
        }
    });
}

function mdAgregarFilaGrupo(val = {}) {
    if (!mdGrupos || !mdEstado.tipo) return;

    const cantidadInicial =
        Number.isInteger(Number(val.cantidad)) && Number(val.cantidad) > 0
            ? Number(val.cantidad)
            : 1;

    const el = document.createElement("div");
    el.className = "md-grupo";

    if (mdEstado.tipo === "AREA") {
        el.innerHTML = `
            <div class="md-grupo-grid">
                <div>
                    <label>Ancho (mm)</label>
                    <input type="number"
                           class="form-control md-ancho"
                           min="1"
                           step="1"
                           value="${Number(val.ancho || 0) || ""}">
                </div>

                <div>
                    <label>Alto (mm)</label>
                    <input type="number"
                           class="form-control md-alto"
                           min="1"
                           step="1"
                           value="${Number(val.alto || 0) || ""}">
                </div>

                <div>
                    <label>Cantidad</label>
                    <input type="number"
                           class="form-control md-cantidad"
                           min="1"
                           step="1"
                           value="${cantidadInicial}">
                </div>

                <button type="button"
                        class="btn btn-outline-danger md-quitar">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
    } else {
        el.innerHTML = `
            <div class="md-grupo-grid longitud">
                <div>
                    <label>Longitud (cm)</label>
                    <input type="number"
                           class="form-control md-longitud"
                           min="1"
                           step="1"
                           value="${Number(val.longitud || 0) || ""}">
                </div>

                <div>
                    <label>Cantidad</label>
                    <input type="number"
                           class="form-control md-cantidad"
                           min="1"
                           step="1"
                           value="${cantidadInicial}">
                </div>

                <button type="button"
                        class="btn btn-outline-danger md-quitar">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
    }

    el.querySelector(".md-quitar")?.addEventListener("click", () => {
        el.remove();

        if (!mdGrupos.children.length) {
            mdAgregarFilaGrupo();
        }

        mdInvalidarResultado();
        mdActualizarBoton();
    });

    el.querySelectorAll("input").forEach(input => {
        input.addEventListener("input", () => {
            mdInvalidarResultado();
            mdActualizarBoton();
        });

        input.addEventListener("blur", () => {
            if (input.classList.contains("md-cantidad")) {
                let cantidad = Number(input.value || 1);

                if (!Number.isInteger(cantidad) || cantidad < 1) {
                    cantidad = 1;
                }

                input.value = String(cantidad);
                mdActualizarBoton();
            }
        });
    });

    mdGrupos.appendChild(el);
    mdActualizarBoton();
}

function mdObtenerCortes() {
    return [...mdGrupos.querySelectorAll(".md-grupo")].map(g=>({
        ancho:mdEstado.tipo==="AREA"?Number(g.querySelector(".md-ancho")?.value||0):null,
        alto:mdEstado.tipo==="AREA"?Number(g.querySelector(".md-alto")?.value||0):null,
        longitud:mdEstado.tipo==="LONGITUD"?Number(g.querySelector(".md-longitud")?.value||0):null,
        cantidad:Number(g.querySelector(".md-cantidad")?.value||0)
    }));
}
function mdCortesValidos(){
    const c=mdObtenerCortes();
    const cortesValidos=!!mdEstado.producto&&!!mdEstado.tipo&&c.length>0&&c.every(x=>x.cantidad>0&&(mdEstado.tipo==="AREA"?x.ancho>0&&x.alto>0:x.longitud>0));
    if (!cortesValidos) return false;
   if (mdEstado.modalidad === "COMPRAR_PRESENTACION") {
       return Boolean(mdPresentacionFija?.value);
   }
    return true;
}
function mdActualizarBoton(){if(mdCalcular)mdCalcular.disabled=!mdCortesValidos();}
function mdDinero(v){return new Intl.NumberFormat("es-PE",{style:"currency",currency:"PEN"}).format(Number(v||0));}
function mdEscaparHtml(valor){return String(valor??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[c]));}

function mdConstruirSolicitud(codigoCombinacion = null) {
    return {
        idProducto:Number(mdEstado.producto.id),
        tipoMedida:mdEstado.tipo,
        unidadEntrada:mdEstado.unidad,
       entregarSobrante:
           mdTiposPresentacionSeleccionada().some(tipo =>
               tipo === "MEDIA" || tipo === "COMPLETA"
           )
               ? (document.getElementById("mdEntregarSobrante")?.checked || false)
               : false,
        observacion:document.getElementById("mdObservacion")?.value||"",
        cortes:mdObtenerCortes(),
        modalidad:mdEstado.modalidad,
        idPrecioPresentacion:mdEstado.modalidad === "COMPRAR_PRESENTACION" ? Number(mdPresentacionFija?.value || 0) : null,
       cantidadPresentacion: null,
        codigoCombinacion
    };
}

async function mdCalcularAhora(){
    if(!mdCortesValidos() || mdCalculando) return;
    mdCalculando = true;
    if (mdEstado.editandoGrupo && mdEstado.entradaModificada) {
        const confirmacion = await Swal.fire({
            icon: "warning",
            title: "Recalcular este grupo",
            text: "La nueva combinación reemplazará únicamente las líneas actuales de este grupo.",
            showCancelButton: true,
            confirmButtonText: "Sí, recalcular",
            cancelButtonText: "Cancelar"
        });
        if (!confirmacion.isConfirmed) {
            mdCalculando = false;
            return;
        }
    }
    const versionSolicitud = mdVersionEntrada;
    mdCalcular.disabled=true;mdCalcular.querySelector(".md-btn-normal")?.classList.add("d-none");mdCalcular.querySelector(".md-btn-cargando")?.classList.remove("d-none");
    try{
        const headers={"Content-Type":"application/json"}; if(CSRF_HEADER&&CSRF_TOKEN)headers[CSRF_HEADER]=CSRF_TOKEN;
        const resp=await fetch("/interno/ventas/api/calcular-medidas-provisional",{method:"POST",headers,body:JSON.stringify(mdConstruirSolicitud())});
        const data=await resp.json().catch(()=>({})); if(!resp.ok||data.ok===false)throw new Error(data.mensaje||data.errores?.join(" ")||"No fue posible calcular las medidas.");
        if (versionSolicitud !== mdVersionEntrada) return;
        mdEstado.resultado=data;
        mdEstado.codigoRecomendadoInicial=data.combinacionRecomendada?.codigo||null;
        mdEstado.codigoSeleccionado=data.combinacionRecomendada?.codigo||null;
        mdEstado.entradaModificada=false;
        mdEstado.resultadoConservado=false;
        mdPintarResultado(data);
    }catch(e){Swal.fire({icon:"error",title:"No se pudo calcular",text:e.message,confirmButtonText:"Entendido"});}
    finally{mdCalculando=false;mdCalcular.querySelector(".md-btn-normal")?.classList.remove("d-none");mdCalcular.querySelector(".md-btn-cargando")?.classList.add("d-none");mdActualizarBoton();}
}

function mdPintarResultado(data){
    const c=data.combinacionRecomendada, f=data.consumoFisico||{};
    mdEstado.codigoSeleccionado = c?.codigo || mdEstado.codigoSeleccionado;
    if (!mdEstado.codigoRecomendadoInicial) mdEstado.codigoRecomendadoInicial = c?.codigo || null;
    const etiquetaResultado = document.getElementById("mdResultadoEtiqueta");
    if (etiquetaResultado) etiquetaResultado.textContent = mdEstado.codigoSeleccionado === mdEstado.codigoRecomendadoInicial
        ? "Combinación recomendada y seleccionada"
        : "Combinación seleccionada";
    const consumoStock=(c.lineas||[]).reduce((total,l)=>total+(Number(l.cantidad||0)*Number(l.factorConversionStock||0)),0);
    const stockDisponible=Number(mdEstado.producto?.stock||0);
    document.getElementById("mdResultadoTitulo").textContent=(c.lineas||[]).map(l=>`${Number(l.cantidad)} × ${l.nombrePresentacion}`).join(" + ");
    document.getElementById("mdResultadoMotivo").textContent=c.motivo||"Combinación físicamente válida y disponible.";
    document.getElementById("mdResultadoPrecio").textContent=mdDinero(c.precioTotal);
    document.getElementById("mdMetricas").innerHTML=`<div class="medida-metrica"><span>Cortes</span><strong>${f.cantidadCortes}</strong></div><div class="medida-metrica"><span>Piezas físicas</span><strong>${f.unidadesFisicas}</strong></div><div class="medida-metrica"><span>Capacidad comercial</span><strong>${Number(c.materialAdquirido||0).toFixed(3)} ${data.tipoMedida==="AREA"?"m²":"m"}</strong></div><div class="medida-metrica"><span>Stock</span><strong>${consumoStock.toFixed(4)} / ${stockDisponible.toFixed(4)}</strong></div><div class="medida-metrica"><span>Aprovechamiento</span><strong>${Number(f.aprovechamientoPorcentaje||0).toFixed(2)}%</strong></div><div class="medida-metrica"><span>Sobrante</span><strong>${Number(f.sobrante||0).toFixed(3)} ${data.tipoMedida==="AREA"?"m²":"m"}</strong></div>`;
    document.getElementById("mdLineas").innerHTML=(c.lineas||[]).map((l,i)=>`<div class="md-linea"><div><strong>${mdEscaparHtml(l.nombrePresentacion)}</strong><small>${mdEscaparHtml(l.tipoCalculoVenta||"Presentación")}${i===0?" · Línea principal":""}</small></div><div class="text-end"><strong>${Number(l.cantidad)} × ${mdDinero(l.precioUnitario)}</strong><small>${mdDinero(l.subtotal)}</small></div></div>`).join("");
    if (mdAlternativas) {
        const alternativas = Array.isArray(data.alternativas) ? data.alternativas : [];
        const opciones = [c, ...alternativas].filter((opcion, indice, lista) => opcion?.codigo && lista.findIndex(o => o?.codigo === opcion.codigo) === indice);
        mdAlternativas.innerHTML = opciones.length ? `
            <div class="small fw-semibold mb-2">Opciones del cálculo</div>
            <div class="d-grid gap-2" role="radiogroup" aria-label="Alternativas de presentación">
                ${opciones.map(a => {
                    const seleccionada = a.codigo === mdEstado.codigoSeleccionado;
                    const recomendada = a.codigo === mdEstado.codigoRecomendadoInicial;
                    return `<button type="button"
                            class="btn text-start md-elegir-alternativa ${seleccionada ? "md-alternativa-seleccionada" : "btn-outline-secondary"}"
                            data-codigo="${mdEscaparHtml(a.codigo)}" role="radio" aria-checked="${seleccionada}" aria-pressed="${seleccionada}">
                        <span class="d-flex justify-content-between align-items-start gap-2">
                            <span class="d-flex gap-2 align-items-start">
                                <i class="bi ${seleccionada ? "bi-check-circle-fill" : "bi-circle"} md-alternativa-check" aria-hidden="true"></i>
                                <span>${(a.lineas||[]).map(l=>`${Number(l.cantidad)} × ${mdEscaparHtml(l.nombrePresentacion)}`).join(" + ")}
                                    <span class="d-flex flex-wrap gap-1 mt-1">
                                        ${recomendada ? '<span class="badge text-bg-primary">Recomendada</span>' : ""}
                                        ${seleccionada ? '<span class="badge text-bg-success">Seleccionada</span>' : ""}
                                    </span>
                                    <small class="d-block text-muted mt-1">${a.piezasFisicas} pieza(s) · capacidad ${Number(a.materialAdquirido||0).toFixed(3)} · stock ${(a.lineas||[]).reduce((t,l)=>t+Number(l.cantidad||0)*Number(l.factorConversionStock||0),0).toFixed(4)}</small>
                                </span>
                            </span>
                            <strong>${mdDinero(a.precioTotal)}</strong>
                        </span>
                    </button>`;
                }).join("")}
            </div>` : "";
        mdAlternativas.querySelectorAll(".md-elegir-alternativa").forEach(btn => {
            btn.addEventListener("click", () => {
                if (btn.dataset.codigo !== mdEstado.codigoSeleccionado) mdSeleccionarAlternativa(btn.dataset.codigo);
            });
        });
    }
    const adv=[...(data.advertencias||[])];document.getElementById("mdAdvertencias").innerHTML=adv.length?`<div class="alert alert-warning py-2 mb-0"><i class="bi bi-exclamation-triangle me-1"></i>${adv.map(mdEscaparHtml).join("<br>")}</div>`:"";
        let planoPreview = document.getElementById("mdPlanoCortePreview");

        if (!planoPreview) {
            planoPreview = document.createElement("div");
            planoPreview.id = "mdPlanoCortePreview";
            planoPreview.className = "mt-4";

            const lineas = document.getElementById("mdLineas");

            if (lineas?.parentElement) {
                lineas.parentElement.insertBefore(planoPreview, lineas);
            }
        }

        planoPreview.innerHTML = `
            <div class="mb-2">
                <h6 class="fw-bold mb-1">
                    <i class="bi bi-grid-3x3-gap me-1"></i>
                    Plano físico de corte
                </h6>
                <div class="small text-muted">
                    Distribución calculada sobre la presentación física real.
                </div>
            </div>

            ${mdCroquisPlanoReal(data.planoCorte)}
        `;

        mdActivarNavegacionPlano(planoPreview);
    mdEstadoInicial.classList.add("d-none");
    mdResultado.classList.remove("d-none");
    mdAgregarVenta.classList.remove("d-none");
    mdActualizarVisibilidadEntregarSobrante();
}

async function mdSeleccionarAlternativa(codigo) {
    if (!codigo || !mdCortesValidos() || mdSeleccionandoAlternativa) return;
    mdSeleccionandoAlternativa = true;
    const versionSolicitud = mdVersionEntrada;
    mdAlternativas?.querySelectorAll(".md-elegir-alternativa").forEach(btn => btn.disabled = true);
    const headers={"Content-Type":"application/json"};
    if(CSRF_HEADER&&CSRF_TOKEN)headers[CSRF_HEADER]=CSRF_TOKEN;
    try {
        const resp=await fetch("/interno/ventas/api/calcular-medidas-provisional",{
            method:"POST",headers,body:JSON.stringify(mdConstruirSolicitud(codigo))
        });
        const data=await resp.json().catch(()=>({}));
        if(!resp.ok||data.ok===false) throw new Error(data.mensaje||data.errores?.join(" ")||"La opción ya no está disponible.");
        if (versionSolicitud !== mdVersionEntrada) return;
        mdEstado.resultado=data;
        mdEstado.codigoSeleccionado=data.combinacionRecomendada?.codigo||codigo;
        mdEstado.resultadoConservado=false;
        mdPintarResultado(data);
    } catch(e) {
        Swal.fire({icon:"error",title:"No se pudo seleccionar",text:e.message});
    } finally {
        mdSeleccionandoAlternativa = false;
        mdAlternativas?.querySelectorAll(".md-elegir-alternativa").forEach(btn => btn.disabled = false);
    }
}

function mdCroquisPlanoReal(planoCorte) {
    if (
        !planoCorte ||
        !Array.isArray(planoCorte.piezas) ||
        planoCorte.piezas.length === 0
    ) {
        return `
            <div class="text-center text-muted small py-4">
                No existe un plano físico calculado para mostrar.
            </div>
        `;
    }

    const tipo = String(planoCorte.tipoMedida || "").toUpperCase();
    const unidad = String(planoCorte.unidad || (tipo === "AREA" ? "MM" : "CM")).toLowerCase();

    const botones = planoCorte.piezas.map((pieza, index) => `
        <button type="button"
                class="btn btn-sm ${index === 0 ? "btn-primary" : "btn-outline-primary"} md-plano-tab"
                data-indice="${index}">
            Plano ${index + 1}
        </button>
    `).join("");

    const piezasHtml = planoCorte.piezas.map((pieza, index) => {
        const contenido = tipo === "AREA"
            ? mdDibujarPlanchaReal(pieza, unidad)
            : mdDibujarVarillaReal(pieza, unidad);

        return `
            <div class="md-plano-pieza ${index === 0 ? "" : "d-none"}"
                 data-indice="${index}">
                ${contenido}
            </div>
        `;
    }).join("");

    return `
        <div class="md-plano-real">
            <div class="d-flex flex-wrap gap-2 mb-3">
                ${botones}
            </div>

            <div class="md-plano-contenido">
                ${piezasHtml}
            </div>
        </div>
    `;
}

function mdDibujarPlanchaReal(pieza, unidad) {
    const anchoFisico = Number(pieza.ancho || 0);
    const altoFisico = Number(pieza.alto || 0);

    if (anchoFisico <= 0 || altoFisico <= 0) {
        return `
            <div class="text-danger small">
                La pieza seleccionada no tiene dimensiones físicas válidas.
            </div>
        `;
    }

    const viewW = 980;
    const viewH = 650;
    const margenIzquierdo = 90;
    const margenDerecho = 45;
    const margenSuperior = 105;
    const margenInferior = 125;
    const anchoDisponible = viewW - margenIzquierdo - margenDerecho;
    const altoDisponible = viewH - margenSuperior - margenInferior;
    const escala = Math.min(
        anchoDisponible / anchoFisico,
        altoDisponible / altoFisico
    );

    const piezaW = anchoFisico * escala;
    const piezaH = altoFisico * escala;
    const xPieza = margenIzquierdo;
    const yPieza = margenSuperior;

    const cortes = Array.isArray(pieza.cortes) ? pieza.cortes : [];
    const sobrantes = Array.isArray(pieza.sobrantes) ? pieza.sobrantes : [];

    const tipoParte = String(
        pieza.tipoParteInventario || pieza.tipoPlano || "PIEZA"
    ).toUpperCase();
    const idParte = pieza.idParteInventario ? ` #${pieza.idParteInventario}` : "";
    const descripcionOrigen = `${tipoParte}${idParte}`;

    const cortesSvg = cortes.map((corte, indice) => {
        const x = xPieza + Number(corte.x || 0) * escala;
        const y = yPieza + Number(corte.y || 0) * escala;
        const w = Number(corte.ancho || 0) * escala;
        const h = Number(corte.alto || 0) * escala;
        const etiqueta = mdEscaparHtml(corte.etiqueta || `Corte ${indice + 1}`);
        return `
            <g>
                <rect x="${x.toFixed(2)}" y="${y.toFixed(2)}"
                      width="${w.toFixed(2)}" height="${h.toFixed(2)}"
                      fill="#dbeafe" stroke="#2563eb" stroke-width="3"></rect>
                ${w >= 70 && h >= 30 ? `
                    <text x="${(x + w / 2).toFixed(2)}"
                          y="${(y + h / 2).toFixed(2)}"
                          text-anchor="middle" dominant-baseline="middle"
                          font-family="Arial" font-size="10" font-weight="700"
                          fill="#1e3a8a">${etiqueta}</text>
                ` : ""}
                <title>${etiqueta}</title>
            </g>
        `;
    }).join("");

    const sobrantesSvg = sobrantes.map((sobrante) => {
        const x = xPieza + Number(sobrante.x || 0) * escala;
        const y = yPieza + Number(sobrante.y || 0) * escala;
        const w = Number(sobrante.ancho || 0) * escala;
        const h = Number(sobrante.alto || 0) * escala;
        const clasificacion = String(sobrante.clasificacion || "DentIA_INVENTARIO").toUpperCase();
        const esMerma = clasificacion === "MERMA_DESECHAR";
        const esCliente = clasificacion.startsWith("CLIENTE_") || clasificacion === "ENTREGAR_CLIENTE";
        const esNoVendido = clasificacion === "DentIA_NO_VENDIDO";
        const medidaM2 = Number(sobrante.medida || 0);
        const medidaPie2 = medidaM2 * 10.7639104167;
        const fill = esCliente ? "#fef3c7" : esNoVendido ? "#e5e7eb" : esMerma ? "#fee2e2" : "#dcfce7";
        const stroke = esCliente ? "#d97706" : esNoVendido ? "#6b7280" : esMerma ? "#dc2626" : "#16a34a";
        const texto = esCliente ? "ENTREGAR AL CLIENTE" : esNoVendido ? "MEDIA NO VENDIDA · DentIA" : esMerma ? "DESECHAR" : "GUARDAR RETAZO";
        return `
            <g>
                <rect x="${x.toFixed(2)}" y="${y.toFixed(2)}"
                      width="${w.toFixed(2)}" height="${h.toFixed(2)}"
                      fill="${fill}" stroke="${stroke}" stroke-width="2"
                      stroke-dasharray="7 5"></rect>
                ${w >= 85 && h >= 34 ? `
                    <text x="${(x + w / 2).toFixed(2)}"
                          y="${(y + h / 2 - 6).toFixed(2)}"
                          text-anchor="middle" dominant-baseline="middle"
                          font-family="Arial" font-size="10" font-weight="700"
                          fill="${stroke}">${texto}</text>
                    <text x="${(x + w / 2).toFixed(2)}"
                          y="${(y + h / 2 + 10).toFixed(2)}"
                          text-anchor="middle" dominant-baseline="middle"
                          font-family="Arial" font-size="10" fill="${stroke}">
                        ${medidaPie2.toFixed(3)} Pie²
                    </text>
                ` : ""}
                <title>${texto}: ${medidaPie2.toFixed(3)} Pie²</title>
            </g>
        `;
    }).join("");

    const medidaUtilizada = Number(pieza.medidaUtilizada || 0);
    const medidaSobrante = Number(pieza.medidaSobrante || 0);
    const aprovechamiento = Number(pieza.aprovechamientoPorcentaje || 0);

    return `
        <div class="border rounded-4 bg-white p-3">
            <div class="alert alert-info border-0 rounded-4 py-2 px-3 mb-3 small">
                <strong>Flujo DentIA:</strong>
                azul: corte solicitado · amarillo: entregar al cliente · verde: guardar como retazo · rojo: desechar · gris: material no vendido que permanece en DentIA.
            </div>

            <div class="d-flex flex-wrap justify-content-between gap-3 mb-3">
                <div>
                    <strong>${mdEscaparHtml(descripcionOrigen)}</strong>
                    <div class="small text-muted">
                        Pieza física: <strong>${anchoFisico} × ${altoFisico} ${unidad}</strong>
                    </div>
                    <div class="small mt-1">
                        Cobro comercial: <strong>${Number(pieza.cantidadComercial || 0)} ${mdEscaparHtml(pieza.unidadComercial || "")}</strong>
                    </div>
                    <div class="small text-muted">
                        La entrega final depende de la presentación comprada y de la opción de sobrante seleccionada.
                    </div>
                </div>
                <div class="text-end small">
                    <div>Área cortada: <strong>${medidaUtilizada.toFixed(4)} m²</strong></div>
                    <div>Sobrante físico: <strong>${medidaSobrante.toFixed(4)} m²</strong></div>
                    <div>Aprovechamiento: <strong>${aprovechamiento.toFixed(2)}%</strong></div>
                </div>
            </div>

            <svg xmlns="http://www.w3.org/2000/svg"
                 viewBox="0 0 ${viewW} ${viewH}" width="100%" height="auto"
                 role="img" aria-label="Plano de corte exacto y clasificación de sobrantes">
                <rect width="${viewW}" height="${viewH}" fill="#f8fafc"></rect>
                <text x="${xPieza + piezaW / 2}" y="${yPieza - 30}"
                      text-anchor="middle" font-family="Arial" font-size="15"
                      font-weight="700" fill="#334155">${anchoFisico} ${unidad}</text>
                <line x1="${xPieza}" y1="${yPieza - 16}"
                      x2="${xPieza + piezaW}" y2="${yPieza - 16}"
                      stroke="#ef4444" stroke-width="1.5"></line>
                <text x="${xPieza - 32}" y="${yPieza + piezaH / 2}"
                      text-anchor="middle" font-family="Arial" font-size="15"
                      font-weight="700" fill="#334155"
                      transform="rotate(-90 ${xPieza - 32} ${yPieza + piezaH / 2})">
                    ${altoFisico} ${unidad}
                </text>
                <line x1="${xPieza - 16}" y1="${yPieza}"
                      x2="${xPieza - 16}" y2="${yPieza + piezaH}"
                      stroke="#ef4444" stroke-width="1.5"></line>
                <rect x="${xPieza}" y="${yPieza}" width="${piezaW}" height="${piezaH}"
                      fill="#e2e8f0" stroke="#0f172a" stroke-width="3"></rect>
                ${sobrantesSvg}
                ${cortesSvg}
                <text x="${xPieza + piezaW / 2}" y="${yPieza + piezaH + 48}"
                      text-anchor="middle" font-family="Arial" font-size="12" fill="#475569">
                    Azul: corte del cliente · Verde: guardar para DentIA · Rojo: desechar como merma
                </text>
            </svg>
        </div>
    `;
}

function mdDibujarVarillaReal(pieza, unidad) {
    const largoBase = Number(pieza.largo || 0);

    if (largoBase <= 0) {
        return `
            <div class="text-danger small">
                La varilla no tiene longitud física válida.
            </div>
        `;
    }

    const viewW = 980;
    const viewH = 270;
    const x0 = 70;
    const y0 = 105;
    const barraW = 840;
    const barraH = 55;
    const escala = barraW / largoBase;

    const colores = [
        "#dbeafe",
        "#dcfce7",
        "#fef3c7",
        "#fce7f3",
        "#ede9fe",
        "#cffafe"
    ];

    const cortes = Array.isArray(pieza.cortes) ? pieza.cortes : [];
    const sobrantes = Array.isArray(pieza.sobrantes) ? pieza.sobrantes : [];

    const sobrantesSvg = sobrantes.map(sobrante => {
        const x = x0 + Number(sobrante.x || 0) * escala;
        const w = Number(sobrante.largo || 0) * escala;

        if (w <= 0) return "";

        return `
            <g>
                <rect x="${x.toFixed(2)}"
                      y="${y0}"
                      width="${w.toFixed(2)}"
                      height="${barraH}"
                      fill="${sobrante.entregarCliente ? "#fef3c7" : "#f8fafc"}"
                      stroke="${sobrante.entregarCliente ? "#d97706" : "#94a3b8"}"
                      stroke-width="1.5"
                      stroke-dasharray="7 5">
                </rect>

                ${w >= 75 ? `
                    <text x="${(x + w / 2).toFixed(2)}"
                          y="${y0 + barraH / 2}"
                          text-anchor="middle"
                          dominant-baseline="middle"
                          font-family="Arial"
                          font-size="11"
                          fill="#64748b">
                        Sobrante ${Number(sobrante.largo || 0)} ${unidad}
                    </text>
                ` : ""}

                <title>
                    Sobrante: ${Number(sobrante.largo || 0)} ${unidad}
                </title>
            </g>
        `;
    }).join("");

    const cortesSvg = cortes.map((corte, index) => {
        const x = x0 + Number(corte.x || 0) * escala;
        const w = Number(corte.largo || 0) * escala;
        const color = colores[index % colores.length];

        return `
            <g>
                <rect x="${x.toFixed(2)}"
                      y="${y0}"
                      width="${w.toFixed(2)}"
                      height="${barraH}"
                      fill="${color}"
                      stroke="#1d4ed8"
                      stroke-width="2">
                </rect>

                ${w >= 55 ? `
                    <text x="${(x + w / 2).toFixed(2)}"
                          y="${y0 + barraH / 2 - 6}"
                          text-anchor="middle"
                          dominant-baseline="middle"
                          font-family="Arial"
                          font-size="11"
                          font-weight="700"
                          fill="#0f172a">
                        P${corte.numero || index + 1}
                    </text>

                    <text x="${(x + w / 2).toFixed(2)}"
                          y="${y0 + barraH / 2 + 11}"
                          text-anchor="middle"
                          dominant-baseline="middle"
                          font-family="Arial"
                          font-size="10"
                          fill="#334155">
                        ${Number(corte.largo || 0)} ${unidad}
                    </text>
                ` : ""}

                <title>
                    Corte ${corte.numero || index + 1}: ${Number(corte.largo || 0)} ${unidad}
                </title>
            </g>
        `;
    }).join("");

    const nombrePresentacion = mdEscaparHtml(
        pieza.nombrePresentacion || `Varilla ${pieza.numero || 1}`
    );

    return `
        <div class="border rounded-4 bg-white p-3">
            <div class="d-flex justify-content-between gap-2 mb-2">
                <div>
                    <strong>${nombrePresentacion}</strong>
                    <div class="small text-muted">
                        Longitud física real: ${largoBase} ${unidad}
                    </div>
                </div>

                <div class="text-end small">
                    Aprovechamiento:
                    <strong>${Number(pieza.aprovechamientoPorcentaje || 0).toFixed(2)}%</strong>
                </div>
            </div>

            <svg xmlns="http://www.w3.org/2000/svg"
                 viewBox="0 0 ${viewW} ${viewH}"
                 width="100%"
                 height="auto">

                <rect width="${viewW}"
                      height="${viewH}"
                      fill="#f8fafc">
                </rect>

                <text x="${x0 + barraW / 2}"
                      y="50"
                      text-anchor="middle"
                      font-family="Arial"
                      font-size="15"
                      font-weight="700"
                      fill="#334155">
                    ${largoBase} ${unidad}
                </text>

                <line x1="${x0}"
                      y1="67"
                      x2="${x0 + barraW}"
                      y2="67"
                      stroke="#ef4444"
                      stroke-width="1.5">
                </line>

                <rect x="${x0}"
                      y="${y0}"
                      width="${barraW}"
                      height="${barraH}"
                      fill="#ffffff"
                      stroke="#0f172a"
                      stroke-width="3">
                </rect>

                ${sobrantesSvg}
                ${cortesSvg}
            </svg>
        </div>
    `;
}

function mdActivarNavegacionPlano(contenedor) {
    if (!contenedor) return;

    contenedor.querySelectorAll(".md-plano-tab").forEach(boton => {
        boton.addEventListener("click", () => {
            const indice = boton.dataset.indice;

            contenedor.querySelectorAll(".md-plano-tab").forEach(btn => {
                const activo = btn.dataset.indice === indice;

                btn.classList.toggle("btn-primary", activo);
                btn.classList.toggle("btn-outline-primary", !activo);
            });

            contenedor.querySelectorAll(".md-plano-pieza").forEach(pieza => {
                pieza.classList.toggle(
                    "d-none",
                    pieza.dataset.indice !== indice
                );
            });
        });
    });
}

async function mdAgregarResultadoVenta(){
    const r=mdEstado.resultado?.combinacionRecomendada;if(!r||mdGuardandoResultado)return;
    mdGuardandoResultado=true;
    mdAgregarVenta.disabled=true;
    const grupo=mdEstado.editandoGrupo || (window.crypto?.randomUUID?.()||`MED-${Date.now()}-${Math.random().toString(16).slice(2)}`);
    const filasAnteriores = mdEstado.editandoGrupo ? filasGrupoMedidas(mdEstado.editandoGrupo) : [];
    const filasNuevas = [];
    const estadoAnterior = mdEstado.editandoGrupo ? mdEstadosGrupo.get(grupo) : null;
    const cortes = mdEstado.resultadoConservado && estadoAnterior ? estadoAnterior.cortes : mdObtenerCortes();
    const obs=document.getElementById("mdObservacion")?.value||"";const sobrante=document.getElementById("mdEntregarSobrante")?.checked;
    const estadoNuevo = mdCrearEstadoGrupo(grupo, mdEstado.resultado, {
        producto: mdEstado.producto,
        tipoMedida: mdEstado.tipo,
        unidadEntrada: mdEstado.unidad,
        modalidad: mdEstado.modalidad,
        idPrecioPresentacion: mdEstado.modalidad === "COMPRAR_PRESENTACION" ? Number(mdPresentacionFija?.value || 0) : null,
       cantidadPresentacion: null,
        codigoCombinacion: r.codigo,
        cortes,
        entregarSobrante: sobrante,
        observacion: obs
    });
    try{
        for(let i=0;i<estadoNuevo.lineas.length;i++){
            const l = estadoNuevo.lineas[i];
            const principal = i === 0;
            const tr = nuevaFila("MEDIDAS", false);

            filasNuevas.push(tr);

            const sel = tr.querySelector(".seleccionar-producto");
            const opt=new Option(mdEstado.producto.text,mdEstado.producto.id,true,true);sel.innerHTML="";sel.appendChild(opt);
            await aplicarProductoSeleccionadoAFila(tr,sel,mdEstado.producto);
           const ps = tr.querySelector(".presentacion-select");
           const idPresentacion = String(l.idPrecioPresentacion ?? "");

           const opcionPresentacion = [...ps.options].find(
               opcion => String(opcion.value) === idPresentacion
           );

           if (!opcionPresentacion) {
               throw new Error(
                   `No se encontró la presentación "${l.nombrePresentacion || idPresentacion}" entre las opciones activas de la sede.`
               );
           }

           ps.value = idPresentacion;
           ps.dataset.selected = idPresentacion;
           ps.dispatchEvent(new Event("change", { bubbles: true }));

           if (String(ps.value) !== idPresentacion) {
               throw new Error(
                   `No fue posible seleccionar la presentación "${l.nombrePresentacion || idPresentacion}".`
               );
           }

           const cantidadInput = tr.querySelector(".cantidad");
           cantidadInput.value = String(Number(l.cantidad || 0));
           cantidadInput.readOnly = true;

           const precioInput = tr.querySelector(".precio");
           precioInput.value = Number(l.precioUnitario || 0).toFixed(2);
           precioInput.readOnly = true;

           const descuentoInput = tr.querySelector(".descuento");
           const descuentoMaximo = Number(
               opcionPresentacion.dataset.descuentoMaximo || 0
           );

           descuentoInput.value = Number(l.descuento || 0).toFixed(2);
           descuentoInput.dataset.maximo = String(descuentoMaximo);
           descuentoInput.max = String(descuentoMaximo);
           descuentoInput.readOnly = false;
           descuentoInput.placeholder = `Máx: ${descuentoMaximo.toFixed(2)}`;
           descuentoInput.title =
               `Descuento máximo permitido: S/ ${descuentoMaximo.toFixed(2)}`;

           tr.querySelector(".factor-stock-hidden").value =
               l.factorConversionStock ||
               opcionPresentacion.dataset.factorConversionStock ||
               1;
            tr.querySelector(".grupo-medida-uuid-hidden").value=grupo;tr.querySelector(".linea-principal-medida-hidden").value=principal?"1":"0";
            tr.querySelector(".linea-medida-clave-hidden").value=l.claveLinea;
            tr.dataset.lineaMedidaClave=l.claveLinea;
            tr.classList.add("linea-medidas-calculada");tr.dataset.grupoMedidaUuid=grupo;
            if(principal){tr.querySelector(".requiere-preparacion-hidden").value="1";tr.querySelector(".tipo-preparacion-hidden").value="CORTE";tr.querySelector(".entregar-sobrante-hidden").value=sobrante?"1":"0";tr.querySelector(".observacion-preparacion-hidden").value=obs;
            tr.querySelector(".cortes-json-hidden").value = JSON.stringify({
                unidadEntrada: mdEstado.unidad,
                modalidad: mdEstado.modalidad,
                idPrecioPresentacion:
                    mdEstado.modalidad === "COMPRAR_PRESENTACION"
                        ? Number(mdPresentacionFija?.value || 0)
                        : null,
                cantidadPresentacion:
                    r.lineas?.[0]?.cantidad ?? null,
                codigoCombinacion: r.codigo,
                cortes
            });
            tr.querySelector(".croquis-svg-hidden").value =
                                                                                                                                                                                                                                                                                                                                                            mdCroquisPlanoReal(mdEstado.resultado?.planoCorte);const b=tr.querySelector(".btn-config-preparacion");b?.classList.remove("d-none");if(b){b.disabled=false;b.setAttribute("aria-disabled","false");}}
            configurarAccionesGrupoMedidas(tr);
            recalcularFila(tr);

        }
        filasAnteriores.forEach(f => f.remove());

        // El formulario crea una fila normal vacía al iniciar. Cuando el usuario
        // agrega un grupo calculado por medidas, esa fila ya no representa un
        // producto real, pero sus campos required bloquean el envío nativo del
        // formulario. Eliminamos únicamente filas normales completamente vacías;
        // nunca filas con datos parciales ni líneas generadas por medidas.
        tablaBody.querySelectorAll("tr").forEach(fila => {
            if (filasNuevas.includes(fila)) return;
            if (obtenerGrupoMedidaFila(fila)) return;

            const producto = fila.querySelector(".seleccionar-producto")?.value?.trim() || "";
            const presentacion = fila.querySelector(".presentacion-select")?.value?.trim() || "";
            const cantidad = Number(fila.querySelector(".cantidad")?.value || 0);
            const precio = Number(fila.querySelector(".precio")?.value || 0);

            if (!producto && !presentacion && cantidad <= 0 && precio <= 0) {
                fila.remove();
            }
        });

        mdEstadosGrupo.set(grupo, estadoNuevo);
        mdSincronizarGrupoDom(grupo);
        const fueEdicion = Boolean(mdEstado.editandoGrupo);
        const fueRecalculo = fueEdicion && !mdEstado.resultadoConservado;
        mdModal.hide();recalcularTotales();Swal.fire({icon:"success",title:fueEdicion?"Medidas actualizadas":"Producto agregado",text:fueEdicion?(fueRecalculo?"Se reemplazaron únicamente las líneas de este grupo.":"El grupo se guardó conservando sus líneas activas."):"La combinación calculada fue agregada a la venta.",timer:1600,showConfirmButton:false});
    }catch(e){
        filasNuevas.forEach(f => f.remove());
        if (estadoAnterior) mdEstadosGrupo.set(grupo, estadoAnterior); else mdEstadosGrupo.delete(grupo);
        mdRenderizarControlesGrupos();
        recalcularTotales();
        Swal.fire({icon:"error",title:"No se pudo agregar",text:e.message});
    }
    finally{mdGuardandoResultado=false;mdAgregarVenta.disabled=false;}
}
document.querySelectorAll("input[name='mdModalidad']").forEach(radio => {
    radio.addEventListener("change", () => {
        if (!radio.checked) return;
        mdEstado.modalidad = radio.value;
        mdPresentacionFijaPanel?.classList.toggle("d-none", radio.value !== "COMPRAR_PRESENTACION");
        mdActualizarVisibilidadEntregarSobrante();
        mdInvalidarResultado();
        mdActualizarBoton();
    });
});
mdPresentacionFija?.addEventListener("change", () => {
    mdActualizarInfoPresentacionFija();
    mdActualizarVisibilidadEntregarSobrante();
    mdInvalidarResultado();
    mdActualizarBoton();
});

mdAgregarGrupo?.addEventListener("click",()=>mdAgregarFilaGrupo());
mdCalcular?.addEventListener("click",mdCalcularAhora);
mdAgregarVenta?.addEventListener("click",mdAgregarResultadoVenta);
configurarProductoMedidasAuto();


/* ============================================================================
   INICIALIZACIÓN DEL FORMULARIO
============================================================================ */
   // 🔥 BOTÓN "AGREGAR PRODUCTO"
    btnAgregar?.addEventListener("click", () => nuevaFila("NORMAL"));
    btnAgregarMedidas?.addEventListener("click", iniciarModalMedidasCalculadas);

// Fila inicial solo si NO hay productos cargados
if (tablaBody.children.length === 0) {
    nuevaFila();
}

// 🔥 ACTIVAR EVENTOS EN FILAS YA EXISTENTES (EDITAR)
tablaBody.querySelectorAll("tr").forEach(tr => {

    const grupoMedidas = obtenerGrupoMedidaFila(tr);
    const requiereMedidas = Boolean(grupoMedidas) || tr.querySelector(".requiere-preparacion-hidden")?.value === "1";
    tr.dataset.origenLinea = requiereMedidas ? "MEDIDAS" : "NORMAL";
    if (grupoMedidas) tr.dataset.grupoMedidaUuid = grupoMedidas;
    tr.classList.toggle("linea-medidas-calculada", requiereMedidas);

    bindFilaEvents(tr);

    const select = tr.querySelector(".seleccionar-producto");

    if (select) {
        activarSelect2(select);
    }

   const idProductoActual = select?.value;

   if (idProductoActual) {
       cargarStockSedeActiva(tr, idProductoActual).then(() => {
           cargarPresentaciones(tr, idProductoActual).then(() => {
               validarStock(tr);
               recalcularFila(tr);
           });
       });
   } else {
       recalcularFila(tr);
   }
});

[...new Set([...tablaBody.querySelectorAll("tr")].map(obtenerGrupoMedidaFila).filter(Boolean))].forEach(grupo => {
    const estado = mdEstadoGrupoDesdeDom(grupo);
    if (estado) {
        mdEstadosGrupo.set(grupo, estado);
        mdSincronizarGrupoDom(grupo);
    }
});
mdRenderizarControlesGrupos();

// Comprobante
actualizarNumeroComprobante();

// Totales
recalcularTotales();

// Crédito
actualizarCredito();

// 🔥 Si ya es crédito (modo edición)
if (formaPago?.value === "CREDITO") {

    bloqueCredito.classList.remove("d-none");

    if (numeroCuotas) {
        numeroCuotas.disabled = false;
        numeroCuotas.setAttribute("required", "required");

        if (!numeroCuotas.value || Number(numeroCuotas.value) < 1) {
            numeroCuotas.value = 1;
        }
    }

    if (frecuenciaCuotas) {
        frecuenciaCuotas.disabled = false;
    }

    if (fechaPrimeraCuota) {
        fechaPrimeraCuota.disabled = false;
    }

    setTimeout(() => {
        generarCuotasPreview();
    }, 200);
}

}); // FIN DOMContentLoaded

function controlarMetodoCobro() {

    const formaPago = document.getElementById("formaPago");

    const pagoInicial = document.getElementById("pagoInicial");

    const bloqueMetodo = document.getElementById("bloqueMetodoCobro");

    const metodoPago = document.getElementById("metodoPago");

    if (!formaPago || !bloqueMetodo || !metodoPago) return;

    const esCredito = formaPago.value === "CREDITO";

    const inicial = pagoInicial
        ? Number(pagoInicial.value || 0)
        : 0;

    if (esCredito && inicial <= 0) {

        bloqueMetodo.classList.add("d-none");

        metodoPago.required = false;

        metodoPago.value = "";

    } else {

        bloqueMetodo.classList.remove("d-none");

        metodoPago.required = true;
    }
}

document.addEventListener("DOMContentLoaded", () => {

    controlarMetodoCobro();

    document.getElementById("formaPago")
        ?.addEventListener("change", controlarMetodoCobro);

    document.getElementById("pagoInicial")
        ?.addEventListener("input", controlarMetodoCobro);
});
