/* ======================================================================
   COMPRAS.JS – DentIA ERP
   Diseño profesional + lógica original conservada
   ====================================================================== */

/* ======================================================================
   CSRF SPRING SECURITY
   ====================================================================== */
const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");

/* ======================================================================
   UTILIDADES
   ====================================================================== */
const f2 = (n) => Number(n || 0).toFixed(2);

function addDays(date, days) {
    const d = new Date(date);
    d.setDate(d.getDate() + days);
    return d.toISOString().substring(0, 10);
}

function mostrarToastGlobal(titulo, mensaje, tipo = "success") {
    const toastEl = document.getElementById("toastGlobal");
    const tituloEl = document.getElementById("toastGlobalTitulo");
    const mensajeEl = document.getElementById("toastGlobalMensaje");
    const iconEl = document.getElementById("toastGlobalIcon");

    if (!toastEl || !tituloEl || !mensajeEl || !iconEl) {
        alert(mensaje);
        return;
    }

    tituloEl.textContent = titulo;
    mensajeEl.textContent = mensaje;

    iconEl.className = "bi me-2";

    if (tipo === "success") {
        iconEl.classList.add("bi-check-circle-fill", "text-success");
    } else if (tipo === "danger") {
        iconEl.classList.add("bi-exclamation-triangle-fill", "text-danger");
    } else if (tipo === "warning") {
        iconEl.classList.add("bi-exclamation-circle-fill", "text-warning");
    } else {
        iconEl.classList.add("bi-info-circle-fill", "text-primary");
    }

    const toast = new bootstrap.Toast(toastEl, {
        delay: 2500
    });

    toast.show();
}

function confirmarAccionGlobal({ titulo, subtitulo, mensaje, textoBoton, claseBoton }, callback) {
    const modalEl = document.getElementById("modalConfirmacionGlobal");
    const tituloEl = document.getElementById("modalConfirmacionTitulo");
    const subtituloEl = document.getElementById("modalConfirmacionSubtitulo");
    const mensajeEl = document.getElementById("modalConfirmacionMensaje");
    const btnConfirmar = document.getElementById("btnConfirmarAccionGlobal");

    if (!modalEl || !btnConfirmar) {
        if (confirm(mensaje || "¿Deseas continuar?")) callback();
        return;
    }

    if (tituloEl) tituloEl.textContent = titulo || "Confirmar acción";
    if (subtituloEl) subtituloEl.textContent = subtitulo || "DentIA ERP";
    if (mensajeEl) mensajeEl.textContent = mensaje || "¿Deseas continuar?";

    btnConfirmar.textContent = "";
    btnConfirmar.className = "btn rounded-pill px-4 " + (claseBoton || "btn-success");
    btnConfirmar.innerHTML = `<i class="bi bi-check-circle me-1"></i> ${textoBoton || "Confirmar"}`;

    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);

    btnConfirmar.onclick = () => {
        modal.hide();
        callback();
    };

    modal.show();
}

/* ======================================================================
   MODALES PRINCIPALES
   ====================================================================== */
window.openCompraModal = function (url, titulo = "Compras") {
    const modal = document.getElementById("compraModal");
    const iframe = document.getElementById("compraModalIframe");
    const title = document.getElementById("compraModalTitle");

    if (!modal || !iframe) return;

    if (title) title.textContent = titulo;

    iframe.src = url;

    const bs = bootstrap.Modal.getOrCreateInstance(modal);
    bs.show();

    modal.addEventListener("hidden.bs.modal", () => {
        iframe.src = "";
    }, { once: true });
};

window.abrirModalImprimir = function (id) {
    window.compraActual = id;

    const modalElement = document.getElementById("modalImprimir");
    if (!modalElement) return;

    const modalImprimir = bootstrap.Modal.getOrCreateInstance(modalElement);
    modalImprimir.show();

    const btnPdf = document.getElementById("btnPdf");
    const btnExcel = document.getElementById("btnExcel");

    if (btnPdf) {
        btnPdf.onclick = () => {
            window.location.href = `/interno/compras/pdf/${id}`;
        };
    }

    if (btnExcel) {
        btnExcel.onclick = () => {
            window.location.href = `/interno/compras/excel/${id}`;
        };
    }
};

/* ======================================================================
   MODAL CUOTAS
   ====================================================================== */
window.abrirModalCuotas = function (idCompra, moneda) {
    const simbolo = moneda === "USD" ? "$ " : "S/ ";

    fetch(`/interno/compras/cuotas/${idCompra}`)
        .then(r => r.json())
        .then(cuotas => {
            const tbody = document.getElementById("tablaCuotas");
            const info = document.getElementById("infoCuotas");

            const resumenTotalCredito = document.getElementById("resumenTotalCredito");
            const resumenTotalPagado = document.getElementById("resumenTotalPagado");
            const resumenTotalPendiente = document.getElementById("resumenTotalPendiente");

            if (!tbody) return;

            if (!cuotas || cuotas.length === 0) {
                tbody.innerHTML = "";

                if (info) {
                    info.innerHTML = `
                        <div class="alert alert-warning mb-0">
                            <i class="bi bi-info-circle me-1"></i>
                            No hay cuotas registradas para esta compra.
                        </div>`;
                }

                if (resumenTotalCredito) resumenTotalCredito.textContent = simbolo + "0.00";
                if (resumenTotalPagado) resumenTotalPagado.textContent = simbolo + "0.00";
                if (resumenTotalPendiente) resumenTotalPendiente.textContent = simbolo + "0.00";

                bootstrap.Modal.getOrCreateInstance(document.getElementById("modalCuotas")).show();
                return;
            }

            let totalCredito = 0;
            let totalPagado = 0;
            let totalPendiente = 0;

            cuotas.forEach(c => {
                const monto = Number(c.montoCuota || 0);
                totalCredito += monto;

                if (c.estado === "PAGADA") {
                    totalPagado += monto;
                } else {
                    totalPendiente += monto;
                }
            });

            if (info) {
                info.innerHTML = `
                    <div class="alert alert-light border mb-0">
                        <i class="bi bi-calendar-check me-1"></i>
                        Cronograma de pagos generado para esta compra.
                    </div>`;
            }

            tbody.innerHTML = cuotas.map(c => {
                const estado = c.estado || "PENDIENTE";
                const monto = Number(c.montoCuota || 0);

                return `
                    <tr>
                        <td class="fw-bold">
                            Cuota ${c.numeroCuota}
                        </td>

                        <td>
                            ${c.fechaVencimiento}
                        </td>

                        <td class="text-end fw-semibold">
                            ${simbolo}${f2(monto)}
                        </td>

                        <td class="text-center">
                            ${
                                estado === "PAGADA"
                                    ? '<span class="badge rounded-pill bg-success">Pagada</span>'
                                    : '<span class="badge rounded-pill bg-warning text-dark">Pendiente</span>'
                            }
                        </td>

                        <td class="text-center">
                            ${
                                estado === "PENDIENTE"
                                    ? `<button onclick="pagarCuota(${c.idCuotaCompra})" class="btn btn-success btn-sm rounded-pill px-3">
                                           <i class="bi bi-check-circle me-1"></i>
                                           Pagar
                                       </button>`
                                    : '<span class="badge bg-success rounded-pill">Cancelada</span>'
                            }
                        </td>
                    </tr>
                `;
            }).join("");

            if (resumenTotalCredito) resumenTotalCredito.textContent = simbolo + f2(totalCredito);
            if (resumenTotalPagado) resumenTotalPagado.textContent = simbolo + f2(totalPagado);
            if (resumenTotalPendiente) resumenTotalPendiente.textContent = simbolo + f2(totalPendiente);

            bootstrap.Modal.getOrCreateInstance(document.getElementById("modalCuotas")).show();
        })
        .catch(() => {
            const info = document.getElementById("infoCuotas");

            if (info) {
                info.innerHTML = `
                    <div class="alert alert-danger mb-0">
                        <i class="bi bi-exclamation-triangle me-1"></i>
                        No se pudieron cargar las cuotas.
                    </div>`;
            }

            bootstrap.Modal.getOrCreateInstance(document.getElementById("modalCuotas")).show();
        });
};

window.pagarCuota = function (idCuota) {
    confirmarAccionGlobal({
        titulo: "Confirmar pago",
        subtitulo: "Gestión de pagos de compra",
        mensaje: "¿Deseas registrar el pago de esta cuota?",
        textoBoton: "Registrar pago",
        claseBoton: "btn-success"
    }, () => {
        fetch(`/interno/compras/cuotas/pagar/${idCuota}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
            }
        })
            .then(r => r.text())
            .then(r => {
                if (r === "OK") {
                    mostrarToastGlobal(
                        "Pago registrado",
                        "La cuota fue marcada como pagada correctamente.",
                        "success"
                    );

                    setTimeout(() => {
                        location.reload();
                    }, 900);

                } else {
                    mostrarToastGlobal(
                        "No se pudo registrar",
                        "Ocurrió un error al pagar la cuota.",
                        "danger"
                    );
                }
            })
            .catch(() => {
                mostrarToastGlobal(
                    "Error de conexión",
                    "No se pudo comunicar con el servidor.",
                    "danger"
                );
            });
    });
};

/* ======================================================================
   CONFIRMAR DEVOLUCIÓN DE PROVEEDOR
   ====================================================================== */
window.abrirModalConfirmarDevolucion = function (idCompra, moneda, monto) {
    const modalEl = document.getElementById("modalConfirmarDevolucion");
    const inputIdCompra = document.getElementById("devolucionIdCompra");
    const inputMontoTexto = document.getElementById("devolucionMontoTexto");
    const inputFecha = document.getElementById("devolucionFecha");
    const inputObservacion = document.getElementById("devolucionObservacion");

    const erroresBox = document.getElementById("erroresConfirmarDevolucion");
    const listaErrores = document.getElementById("listaErroresConfirmarDevolucion");

    if (!modalEl || !inputIdCompra || !inputMontoTexto || !inputFecha || !inputObservacion) {
        alert("No se encontró el modal de confirmación de devolución.");
        return;
    }

    const simbolo = moneda === "USD" ? "$ " : "S/ ";
    const montoNumero = Number(monto || 0);

    inputIdCompra.value = idCompra;
    inputMontoTexto.value = simbolo + montoNumero.toFixed(2);
    inputObservacion.value = "";

    const hoy = new Date();
    const yyyy = hoy.getFullYear();
    const mm = String(hoy.getMonth() + 1).padStart(2, "0");
    const dd = String(hoy.getDate()).padStart(2, "0");

    inputFecha.value = `${yyyy}-${mm}-${dd}`;

    if (erroresBox) erroresBox.classList.add("d-none");
    if (listaErrores) listaErrores.innerHTML = "";

    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();
};

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("formConfirmarDevolucion");

    if (!form) return;

    form.addEventListener("submit", (e) => {
        const fecha = document.getElementById("devolucionFecha")?.value || "";
        const observacion = document.getElementById("devolucionObservacion")?.value.trim() || "";

        const erroresBox = document.getElementById("erroresConfirmarDevolucion");
        const listaErrores = document.getElementById("listaErroresConfirmarDevolucion");

        let errores = [];

        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);

        const fechaDev = fecha ? new Date(fecha + "T00:00:00") : null;

        const observacionValida =
            /^[A-Za-zÁÉÍÓÚáéíóúÑñ0-9\s.,;:()\-\/]{10,500}$/.test(observacion);

        if (!fecha) {
            errores.push("Debe ingresar la fecha de devolución.");
        }

        if (fechaDev && fechaDev > hoy) {
            errores.push("La fecha de devolución no puede ser futura.");
        }

      if (observacion && !observacionValida) {
          errores.push("Si ingresas una observación, debe tener mínimo 10 caracteres y no debe contener símbolos extraños.");
      }

        if (errores.length > 0) {
            e.preventDefault();

            if (erroresBox && listaErrores) {
                listaErrores.innerHTML = errores.map(error => `<li>${error}</li>`).join("");
                erroresBox.classList.remove("d-none");
                erroresBox.scrollIntoView({ behavior: "smooth", block: "center" });
            } else {
                alert(errores.join("\n"));
            }

            return;
        }

        const confirmar = confirm("¿Confirmas que el proveedor ya devolvió o regularizó este saldo?");

        if (!confirmar) {
            e.preventDefault();
        }
    });
});

/* ======================================================================
   LISTADO PRINCIPAL: BUSCADOR + FILTROS + PAGINACIÓN
   ====================================================================== */
document.addEventListener("DOMContentLoaded", () => {
    const buscador = document.getElementById("buscadorCompras");
    const filtroEstado = document.getElementById("filtroEstadoCompra");
    const filtroPago = document.getElementById("filtroPagoCompra");
    const btnLimpiar = document.getElementById("btnLimpiarFiltrosCompras");
    const tabla = document.getElementById("tablaCompras");

    const btnAnterior = document.getElementById("btnComprasAnterior");
    const btnSiguiente = document.getElementById("btnComprasSiguiente");
    const contador = document.getElementById("contadorComprasListado");
    const paginaActualTexto = document.getElementById("paginaComprasActual");
    const paginaTotalTexto = document.getElementById("paginaComprasTotal");

    if (!tabla) return;

    const comprasPorPagina = 10;
    let paginaActual = 1;

    function obtenerFilasCompras() {
        return Array.from(tabla.querySelectorAll("tbody tr.fila-compra"));
    }

    function normalizarTexto(valor) {
        return String(valor || "")
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/_/g, " ")
            .trim();
    }

    function obtenerFilasFiltradas() {
        const texto = normalizarTexto(buscador?.value || "");
        const estado = normalizarTexto(filtroEstado?.value || "");
        const pago = normalizarTexto(filtroPago?.value || "");

        return obtenerFilasCompras().filter(fila => {
            const id = normalizarTexto(fila.dataset.id);
            const comprobante = normalizarTexto(fila.dataset.comprobante);
            const proveedor = normalizarTexto(fila.dataset.proveedor);
            const usuario = normalizarTexto(fila.dataset.usuario);
            const estadoFila = normalizarTexto(fila.dataset.estado);
            const pagoFila = normalizarTexto(fila.dataset.pago);

            const busquedaGeneral = [
                id,
                comprobante,
                proveedor,
                usuario,
                estadoFila,
                pagoFila
            ].join(" ");

            const coincideTexto =
                !texto || busquedaGeneral.includes(texto);

            const coincideEstado =
                !estado || estadoFila === estado;

            const coincidePago =
                !pago || pagoFila === pago;

            return coincideTexto && coincideEstado && coincidePago;
        });
    }

    function aplicarFiltrosCompras() {
        const filas = obtenerFilasCompras();
        const filtradas = obtenerFilasFiltradas();

        filas.forEach(fila => {
            fila.style.display = "none";
        });

        const totalRegistros = filtradas.length;
        const totalPaginas = Math.max(1, Math.ceil(totalRegistros / comprasPorPagina));

        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas;
        }

        const inicio = (paginaActual - 1) * comprasPorPagina;
        const fin = inicio + comprasPorPagina;

        const visibles = filtradas.slice(inicio, fin);

        visibles.forEach(fila => {
            fila.style.display = "";
        });

        actualizarPaginacion(totalRegistros, visibles.length, totalPaginas);
        mostrarMensajeSinResultados(visibles.length, totalRegistros);
    }

    function actualizarPaginacion(totalRegistros, mostrando, totalPaginas) {
        const inicio = totalRegistros === 0
            ? 0
            : ((paginaActual - 1) * comprasPorPagina) + 1;

        const fin = Math.min(paginaActual * comprasPorPagina, totalRegistros);

        if (contador) {
            contador.textContent = `Mostrando ${inicio}-${fin} de ${totalRegistros} compras`;
        }

        if (paginaActualTexto) {
            paginaActualTexto.textContent = paginaActual;
        }

        if (paginaTotalTexto) {
            paginaTotalTexto.textContent = totalPaginas;
        }

        if (btnAnterior) {
            btnAnterior.disabled = paginaActual <= 1;
        }

        if (btnSiguiente) {
            btnSiguiente.disabled = paginaActual >= totalPaginas;
        }
    }

    function mostrarMensajeSinResultados(visibles, totalRegistros) {
        const tbody = tabla.querySelector("tbody");
        if (!tbody) return;

        let filaMensaje = document.getElementById("filaSinResultadosCompras");

        if (!filaMensaje) {
            filaMensaje = document.createElement("tr");
            filaMensaje.id = "filaSinResultadosCompras";
            filaMensaje.innerHTML = `
                <td colspan="12" class="text-center py-5">
                    <div class="dentia-empty-state">
                        <i class="bi bi-search"></i>
                        <h6 class="fw-bold mt-2 mb-1">No se encontraron compras</h6>
                        <p class="mb-0">Prueba cambiando los filtros de búsqueda.</p>
                    </div>
                </td>
            `;
            tbody.appendChild(filaMensaje);
        }

        filaMensaje.style.display = totalRegistros === 0 ? "" : "none";
    }

    function reiniciarPaginaYFiltrar() {
        paginaActual = 1;
        aplicarFiltrosCompras();
    }

    buscador?.addEventListener("input", reiniciarPaginaYFiltrar);
    filtroEstado?.addEventListener("change", reiniciarPaginaYFiltrar);
    filtroPago?.addEventListener("change", reiniciarPaginaYFiltrar);

    btnLimpiar?.addEventListener("click", () => {
        if (buscador) buscador.value = "";
        if (filtroEstado) filtroEstado.value = "";
        if (filtroPago) filtroPago.value = "";

        paginaActual = 1;
        aplicarFiltrosCompras();
    });

    btnAnterior?.addEventListener("click", () => {
        if (paginaActual > 1) {
            paginaActual--;
            aplicarFiltrosCompras();
        }
    });

    btnSiguiente?.addEventListener("click", () => {
        const totalPaginas = Math.max(
            1,
            Math.ceil(obtenerFilasFiltradas().length / comprasPorPagina)
        );

        if (paginaActual < totalPaginas) {
            paginaActual++;
            aplicarFiltrosCompras();
        }
    });

    aplicarFiltrosCompras();
});

/* ======================================================================
   FORMULARIO PRINCIPAL DE COMPRA
   ====================================================================== */
document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("compraForm");
    if (!form) return;

    const tablaBody = document.querySelector("#tablaProductos tbody");
    const btnAgregarProducto = document.getElementById("agregarProducto");

    const monedaSel = document.getElementById("moneda");
    const fechaInput = document.getElementById("fechaEmision");
    const periodoMes = document.getElementById("periodoMes");
    const periodoAnio = document.getElementById("periodoAnio");
    const tipoCambioInput = document.getElementById("tipoCambio");

    const tipoComp = document.getElementById("tipoComprobante");
    const serieInput = document.getElementById("inputSerie");
    const numeroInput = document.getElementById("inputNumero");

    const idCompraInput = document.querySelector("input[name='idCompra']");
    const esEdicion = !!idCompraInput?.value;

    const formaPagoSel = document.getElementById("formaPago");
    const seccionCredito = document.getElementById("seccionCredito");
    const pagoInicial = document.getElementById("pagoInicial");
    const numCuotasInp = document.getElementById("numeroCuotasCredito");
    const deudaRestante = document.getElementById("deudaRestante");

    /* ==========================================================
       PLACEHOLDER SERIE / NÚMERO
       ========================================================== */
  function obtenerLetraSeriePorTipo(tipo) {
      if (tipo === "FACTURA") return "F";
      if (tipo === "BOLETA") return "B";
      return "";
  }

  function actualizarPlaceholder() {
      if (!tipoComp || !serieInput || !numeroInput) return;

      const tipo = tipoComp.value;
      const letra = obtenerLetraSeriePorTipo(tipo);

      if (letra) {
          serieInput.placeholder = letra + "001";
          numeroInput.placeholder = "000001";
      } else {
          serieInput.placeholder = "Seleccione tipo";
          numeroInput.placeholder = "NÚMERO";
      }
  }

    tipoComp?.addEventListener("change", actualizarPlaceholder);
    actualizarPlaceholder();

    /* ==========================================================
       VALIDACIÓN
       ========================================================== */
    function limpiarErrores() {
        document.querySelectorAll(".campo-error").forEach(e => e.textContent = "");

        const panel = document.getElementById("panelErrores");
        const lista = document.getElementById("listaErrores");

        if (panel) panel.classList.add("d-none");
        if (lista) lista.innerHTML = "";
    }

    function registrarError(campo, msg) {
        const div = document.querySelector(`.campo-error[data-error-for='${campo}']`);
        if (div) div.textContent = msg;

        const panel = document.getElementById("panelErrores");
        const lista = document.getElementById("listaErrores");

        if (panel && lista) {
            panel.classList.remove("d-none");
            lista.innerHTML += `<li>${msg}</li>`;
        }
    }

    form.addEventListener("submit", (e) => {
        limpiarErrores();

        const total = Number(document.getElementById("totalInput")?.value || 0);
        const proveedor = document.querySelector("select[name='idProveedor']");

        if (proveedor && !proveedor.value) {
            registrarError("idProveedor", "Seleccione proveedor.");
        }

        if (tipoComp && !tipoComp.value) {
            registrarError("tipoComprobante", "Seleccione tipo de comprobante.");
        }

        if (serieInput && !serieInput.value.trim()) {
            registrarError("serie", "Serie obligatoria.");
        }

        if (numeroInput && !numeroInput.value.trim()) {
            registrarError("numero", "Número obligatorio.");
        }

        const tipoValor = tipoComp?.value || "";
        const letraEsperada = obtenerLetraSeriePorTipo(tipoValor);

        if (tipoValor === "FACTURA" || tipoValor === "BOLETA") {
            const serieValor = serieInput?.value.trim().toUpperCase() || "";

            if (!serieValor.match(new RegExp("^" + letraEsperada + "\\d+$"))) {
                registrarError(
                    "serie",
                    `La serie para ${tipoValor} debe iniciar con ${letraEsperada} y luego solo números. Ejemplo: ${letraEsperada}001.`
                );
            }
        }

        if (numeroInput && numeroInput.value.trim() && !numeroInput.value.trim().match(/^\d+$/)) {
            registrarError("numero", "El número solo debe contener dígitos.");
        }

        const periodoMes = document.getElementById("periodoMes");
        const periodoAnio = document.getElementById("periodoAnio");

        if (periodoMes && !periodoMes.value) {
            registrarError("periodoMes", "Seleccione el periodo mes.");
        }

        if (periodoAnio && !periodoAnio.value) {
            registrarError("periodoAnio", "Ingrese el periodo año.");
        }

        if (monedaSel && monedaSel.value === "USD" &&
            (!tipoCambioInput.value || Number(tipoCambioInput.value) <= 0)) {
            registrarError("tipoCambio", "Tipo de cambio inválido.");
        }
        const tipoRecepcionMarcado = document.querySelector("input[name='tipoRecepcion']:checked");

        if (!tipoRecepcionMarcado) {
            registrarError("tipoRecepcion", "Seleccione cómo será la recepción de la mercadería.");
        }

        if (tablaBody && tablaBody.children.length === 0) {
            registrarError("idProducto", "Debe agregar productos.");
        }

        if (total <= 0) {
            registrarError("totales", "Total inválido.");
        }

        if (formaPagoSel?.value === "CREDITO") {
            const inicial = Number(pagoInicial?.value || 0);
            const cuotas = document.querySelectorAll("input[name='fechaCuota']");
            const montos = document.querySelectorAll("input[name='montoCuota']");

            if (inicial < 0) {
                registrarError("pagoInicial", "El pago inicial no puede ser negativo.");
            }

            if (inicial >= total) {
                registrarError("pagoInicial", "El pago inicial no puede ser igual o mayor al total.");
            }

            if (cuotas.length === 0 || montos.length === 0) {
                registrarError("numeroCuotasCredito", "Debe generar las cuotas antes de guardar.");
            }
        }

        const panelErrores = document.getElementById("panelErrores");
        if (panelErrores && !panelErrores.classList.contains("d-none")) {
            e.preventDefault();
            panelErrores.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    });

    /* ==========================================================
       CÁLCULOS DE FILAS
       ========================================================== */
    function recalcularFila(fila) {
        const c = Number(fila.querySelector(".cantidad")?.value || 0);
        const p = Number(fila.querySelector(".precio")?.value || 0);
        const d = Number(fila.querySelector(".descuento")?.value || 0);

        const subtotal = Math.max(0, c * p - d);
        const subtotalCell = fila.querySelector(".subtotal");

        if (subtotalCell) subtotalCell.textContent = f2(subtotal);

        recalcularTotales();
    }

    function recalcularTotales() {
        if (!tablaBody) return;

        let suma = 0;

        tablaBody.querySelectorAll("tr").forEach(f => {
            suma += Number(f.querySelector(".subtotal")?.textContent || 0);
        });

        const incluyeIgv = document.getElementById("incluyeIgv")?.checked || false;
        const porc = Number(document.getElementById("porcentajeIgv")?.value || 0) / 100;

        let subtotal;
        let igv;
        let total;

        if (incluyeIgv) {
            total = suma;
            subtotal = porc > 0 ? total / (1 + porc) : total;
            igv = total - subtotal;
        } else {
            subtotal = suma;
            igv = subtotal * porc;
            total = subtotal + igv;
        }

        const simbolo = monedaSel?.value === "USD" ? "$" : "S/";

        const lblSubtotal = document.getElementById("lblSubtotal");
        const lblIgv = document.getElementById("lblIgv");
        const lblTotal = document.getElementById("lblTotal");

        const subtotalInput = document.getElementById("subtotalInput");
        const montoIgvInput = document.getElementById("montoIgvInput");
        const totalInput = document.getElementById("totalInput");

        if (lblSubtotal) lblSubtotal.textContent = `${simbolo} ${f2(subtotal)}`;
        if (lblIgv) lblIgv.textContent = `${simbolo} ${f2(igv)}`;
        if (lblTotal) lblTotal.textContent = `${simbolo} ${f2(total)}`;

        if (subtotalInput) subtotalInput.value = f2(subtotal);
        if (montoIgvInput) montoIgvInput.value = f2(igv);
        if (totalInput) totalInput.value = f2(total);

      if (formaPagoSel?.value === "CREDITO") {
          recalcularDeuda();
      }

        actualizarEquivalenteSoles();

        const equivalente = document.getElementById("equivalenteSoles");
        if (equivalente && monedaSel?.value !== "USD") {
            equivalente.classList.add("d-none");
        }
    }

    /* ==========================================================
       EQUIVALENTE EN SOLES
       ========================================================== */
    function actualizarEquivalenteSoles() {
        const moneda = monedaSel?.value;
        const tc = Number(tipoCambioInput?.value || 0);

        const subtotal = Number(document.getElementById("subtotalInput")?.value || 0);
        const igv = Number(document.getElementById("montoIgvInput")?.value || 0);
        const total = Number(document.getElementById("totalInput")?.value || 0);

        const box = document.getElementById("equivalenteSoles");
        if (!box) return;

        if (moneda !== "USD" || tc <= 0) {
            box.classList.add("d-none");
            return;
        }

        box.classList.remove("d-none");

        const lblSubtotalPen = document.getElementById("lblSubtotalPen");
        const lblIgvPen = document.getElementById("lblIgvPen");
        const lblTotalPen = document.getElementById("lblTotalPen");

        if (lblSubtotalPen) lblSubtotalPen.textContent = "S/ " + f2(subtotal * tc);
        if (lblIgvPen) lblIgvPen.textContent = "S/ " + f2(igv * tc);
        if (lblTotalPen) lblTotalPen.textContent = "S/ " + f2(total * tc);
    }

    /* ==========================================================
       SELECT2 PRODUCTOS CON IMAGEN
       ========================================================== */
    function getProductoFotoUrl(foto) {
        if (!foto || foto === "null" || foto.trim() === "") return "";

        if (foto.startsWith("http")) {
            return foto;
        }

        return `/uploads/productos/${foto}`;
    }

    function formatoProducto(option) {
        if (!option.id) {
            return option.text;
        }

        const el = option.element;
        const codigo = el?.getAttribute("data-codigo") || "";
        const nombre = el?.getAttribute("data-nombre") || option.text;
        const stock = el?.getAttribute("data-stock") || "0";
        const stockNum = Number(stock || 0);

        const stockFormateado = Number.isInteger(stockNum)
            ? stockNum.toString()
            : stockNum.toFixed(2).replace(/\.?0+$/, "");
        const foto = el?.getAttribute("data-foto") || "";

        const fotoUrl = getProductoFotoUrl(foto);

        const imgHtml = fotoUrl
            ? `<img src="${fotoUrl}" class="select2-producto-img" alt="Producto">`
            : `<div class="select2-producto-icon"><i class="bi bi-box-seam"></i></div>`;

        return $(`
            <div class="select2-producto-option">
                ${imgHtml}
                <div>
                    <div class="select2-producto-title">
                        ${codigo ? codigo + " - " : ""}${nombre}
                    </div>
                    <div class="select2-producto-stock">
                       Stock disponible: ${stockFormateado}
                    </div>
                </div>
            </div>
        `);
    }

    function actualizarImagenProductoFila(fila) {
        const select = fila.querySelector("select[name='idProducto']");
        if (!select) return;

        const option = select.options[select.selectedIndex];
        const foto = option?.getAttribute("data-foto") || "";

        const img = fila.querySelector(".producto-img-mini");
        const icon = fila.querySelector(".producto-icon-mini");

        if (!img || !icon) return;

        const fotoUrl = getProductoFotoUrl(foto);

        if (fotoUrl) {
            img.src = fotoUrl;
            img.classList.remove("d-none");
            icon.classList.add("d-none");
        } else {
            img.src = "";
            img.classList.add("d-none");
            icon.classList.remove("d-none");
        }
    }

    function inicializarSelectProductoFila(fila) {
        const select = fila.querySelector("select[name='idProducto']");
        if (!select) return;

        if (window.jQuery && $(select).data("select2")) {
            $(select).select2("destroy");
        }

        if (!window.jQuery || !$.fn.select2) {
            actualizarImagenProductoFila(fila);
            return;
        }

        $(select).select2({
            theme: "bootstrap-5",
            width: "100%",
            placeholder: "Buscar producto...",
            allowClear: true,
            templateResult: formatoProducto,
            templateSelection: function (option) {
                if (!option.id) return option.text;

                const el = option.element;
                const codigo = el?.getAttribute("data-codigo") || "";
                const nombre = el?.getAttribute("data-nombre") || option.text;

                return `${codigo ? codigo + " - " : ""}${nombre}`;
            },
            escapeMarkup: function (markup) {
                return markup;
            }
        });

        $(select).on("change", () => {
            actualizarImagenProductoFila(fila);
        });

        actualizarImagenProductoFila(fila);
    }

    /* ==========================================================
       UNIDAD Y PRECIO AUTOMÁTICO SEGÚN PRODUCTO
       ========================================================== */
  function aplicarUnidadPorProductoEnFila(fila) {
      if (!fila) return;

      const hiddenUnidad = fila.querySelector("input[name='idUnidad'].unidad-hidden");
      if (!hiddenUnidad) return;

      // En compras siempre trabajaremos como "Unidad".
      // Si el hidden ya viene desde Thymeleaf con idUnidadUnidad, lo respetamos.
      if (!hiddenUnidad.value) {
          const selectProducto = fila.querySelector("select[name='idProducto']");
          const opcion = selectProducto?.options[selectProducto.selectedIndex];

          const unidadIdProducto = opcion?.getAttribute("data-unidad-id");

          if (unidadIdProducto) {
              hiddenUnidad.value = unidadIdProducto;
          }
      }
  }

   function bindEventosFila(fila) {
       fila.querySelector(".cantidad")
           ?.addEventListener("input", () => recalcularFila(fila));

       fila.querySelector(".precio")
           ?.addEventListener("input", () => recalcularFila(fila));

       fila.querySelector(".descuento")
           ?.addEventListener("input", () => recalcularFila(fila));

       const selProd = fila.querySelector("select[name='idProducto']");

       if (selProd) {
           selProd.addEventListener("change", () => {
               aplicarUnidadPorProductoEnFila(fila);
               actualizarImagenProductoFila(fila);

               const opcion = selProd.options[selProd.selectedIndex];
               const precio = opcion?.getAttribute("data-precio") || 0;
               const precioInput = fila.querySelector(".precio");

               if (precioInput) {
                   precioInput.value = Number(precio).toFixed(2);
               }

               recalcularFila(fila);
           });

           if (selProd.value) {
               aplicarUnidadPorProductoEnFila(fila);
               actualizarImagenProductoFila(fila);
           }
       }

       fila.querySelector(".eliminar")
           ?.addEventListener("click", () => {
               fila.remove();
               recalcularTotales();
           });

       inicializarSelectProductoFila(fila);
   }

  function activarBuscadorEnFila(fila) {
      // Ya no se usa el input .filtro-producto.
      // Ahora la búsqueda la maneja Select2.
  }

   tablaBody?.querySelectorAll("tr").forEach(fila => {
       bindEventosFila(fila);
       recalcularFila(fila);
   });

   btnAgregarProducto?.addEventListener("click", () => {
       if (!tablaBody || !tablaBody.firstElementChild) return;

       const original = tablaBody.firstElementChild;

       // Destruir Select2 antes de clonar para no duplicar contenedores visuales
       const selectOriginal = original.querySelector("select[name='idProducto']");
       if (window.jQuery && selectOriginal && $(selectOriginal).data("select2")) {
           $(selectOriginal).select2("destroy");
       }

       const base = original.cloneNode(true);

       // Volver a inicializar la fila original
       inicializarSelectProductoFila(original);

       base.querySelectorAll("input").forEach(i => {
           if (i.classList.contains("cantidad")) {
               i.value = 1;
           } else if (i.classList.contains("unidad-hidden")) {
               // Mantiene el id de Unidad que viene del HTML
               i.value = i.value || "";
           } else {
               i.value = "0.00";
           }
       });

       const selProd = base.querySelector("select[name='idProducto']");
       const subtotal = base.querySelector(".subtotal");
       const img = base.querySelector(".producto-img-mini");
       const icon = base.querySelector(".producto-icon-mini");

       if (selProd) {
           selProd.value = "";
           selProd.removeAttribute("data-select2-id");

           Array.from(selProd.options).forEach(opt => {
               opt.selected = false;
               opt.removeAttribute("data-select2-id");
           });
       }

       base.querySelectorAll(".select2-container").forEach(el => el.remove());

       if (subtotal) subtotal.textContent = "0.00";

       if (img) {
           img.src = "";
           img.classList.add("d-none");
       }

       if (icon) {
           icon.classList.remove("d-none");
       }

       tablaBody.appendChild(base);

       bindEventosFila(base);
       recalcularTotales();
   });

    /* ==========================================================
       TIPO DE CAMBIO AUTO
       ========================================================== */
    async function obtenerTC(fecha) {
        if (!tipoCambioInput) return;

        try {
            const r = await fetch(`/interno/compras/tipo-cambio?fecha=${fecha}`);

            if (!r.ok) {
                tipoCambioInput.value = "";
                return;
            }

            const data = await r.json();

            if (data.error || !data.venta) {
                tipoCambioInput.value = "";
                return;
            }

            tipoCambioInput.value = Number(data.venta).toFixed(3);
            recalcularTotales();

        } catch (e) {
            console.error("Error JS al obtener tipo de cambio:", e);
        }
    }

    function actualizarTC() {
        if (!monedaSel || !tipoCambioInput) return;

        if (monedaSel.value !== "USD") {
            tipoCambioInput.value = "";
            recalcularTotales();
            return;
        }

        if (fechaInput && !fechaInput.value) {
            fechaInput.value = new Date().toISOString().substring(0, 10);
        }

        const f = fechaInput?.value;

        if (f) obtenerTC(f);
    }

    monedaSel?.addEventListener("change", actualizarTC);
    fechaInput?.addEventListener("change", actualizarTC);
    function completarPeriodoDesdeFecha() {
        if (!fechaInput || !periodoMes || !periodoAnio) return;
        if (!fechaInput.value) return;

        const partes = fechaInput.value.split("-");
        if (partes.length !== 3) return;

        const anio = partes[0];
        const mes = parseInt(partes[1], 10);

        if (!periodoMes.value) periodoMes.value = mes;
        if (!periodoAnio.value) periodoAnio.value = anio;
    }

    fechaInput?.addEventListener("change", completarPeriodoDesdeFecha);
    completarPeriodoDesdeFecha();
    tipoCambioInput?.addEventListener("input", recalcularTotales);

    if (monedaSel?.value === "USD") actualizarTC();

    /* ==========================================================
       SERIE / NÚMERO AUTO
       ========================================================== */
   function actualizarSerieNumero() {
       if (esEdicion) return;
       if (!tipoComp || !tipoComp.value || !serieInput || !numeroInput) return;

       const tipo = tipoComp.value;
       const letra = obtenerLetraSeriePorTipo(tipo);

       actualizarPlaceholder();

       if (!letra) {
           serieInput.value = "";
           numeroInput.value = "";
           serieInput.readOnly = true;
           return;
       }

       serieInput.readOnly = false;

       let numerosSerie = serieInput.value
           .toUpperCase()
           .replace(/^[A-Z]/, "")
           .replace(/\D/g, "");

       if (!numerosSerie) {
           numerosSerie = "001";
       }

       serieInput.value = letra + numerosSerie.substring(0, 3);

       if (!numeroInput.value) {
           numeroInput.value = "";
       }
   }

    tipoComp?.addEventListener("change", actualizarSerieNumero);
    if (!esEdicion && tipoComp?.value) actualizarSerieNumero();

   serieInput?.addEventListener("input", () => {
       const tipo = tipoComp?.value || "";
       const letra = obtenerLetraSeriePorTipo(tipo);

       if (!letra) {
           serieInput.value = "";
           return;
       }

       let valor = serieInput.value.toUpperCase();

       // Quita cualquier letra incorrecta y deja solo números después de F o B
       let numeros = valor
           .replace(/^[A-Z]/, "")
           .replace(/\D/g, "")
           .substring(0, 3);

       serieInput.value = letra + numeros;
   });

   serieInput?.addEventListener("blur", () => {
       const tipo = tipoComp?.value || "";
       const letra = obtenerLetraSeriePorTipo(tipo);

       if (!letra) return;

       let numeros = serieInput.value
           .replace(/^[A-Z]/, "")
           .replace(/\D/g, "")
           .substring(0, 3);

       if (!numeros) {
           numeros = "001";
       }

       serieInput.value = letra + numeros.padStart(3, "0");
   });

    numeroInput?.addEventListener("input", () => {
        numeroInput.value = numeroInput.value
            .replace(/\D/g, "")
            .substring(0, 6);
    });

    numeroInput?.addEventListener("blur", () => {
        if (!numeroInput.value) return;

        numeroInput.value = numeroInput.value
            .replace(/\D/g, "")
            .substring(0, 6)
            .padStart(6, "0");
    });

    document.getElementById("incluyeIgv")?.addEventListener("change", recalcularTotales);
    document.getElementById("porcentajeIgv")?.addEventListener("input", recalcularTotales);

  /* ==========================================================
     CRÉDITO - Vista tipo ventas
     ========================================================== */

  const frecuenciaCuotasCompra = document.getElementById("frecuenciaCuotasCompra");
  const fechaPrimeraCuotaCompra = document.getElementById("fechaPrimeraCuotaCompra");
  const btnGenerarCuotasCompra = document.getElementById("btnGenerarCuotasCompra");
  const cuotasPreviewBodyCompra = document.getElementById("cuotasPreviewBodyCompra");
  const bloqueDiasPersonalizadosCompra = document.getElementById("bloqueDiasPersonalizadosCompra");
  const diasPersonalizadosCompra = document.getElementById("diasPersonalizadosCompra");

  function obtenerDiasPorFrecuenciaCompra() {
      const frecuencia = frecuenciaCuotasCompra?.value || "MENSUAL";

      if (frecuencia === "DIARIO") return 1;
      if (frecuencia === "SEMANAL") return 7;
      if (frecuencia === "QUINCENAL") return 15;
      if (frecuencia === "MENSUAL") return 30;

      if (frecuencia === "PERSONALIZADO") {
          const dias = Number(diasPersonalizadosCompra?.value || 0);
          return dias > 0 ? dias : 30;
      }

      return 30;
  }

  function obtenerFechaBaseCuotaCompra() {
      if (fechaPrimeraCuotaCompra?.value) {
          return fechaPrimeraCuotaCompra.value;
      }

      const hoy = new Date().toISOString().substring(0, 10);

      if (fechaPrimeraCuotaCompra) {
          fechaPrimeraCuotaCompra.value = hoy;
      }

      return hoy;
  }

  function toggleCredito() {
      if (!formaPagoSel || !seccionCredito) return;

      const show = formaPagoSel.value === "CREDITO";

      seccionCredito.classList.toggle("d-none", !show);

      if (bloqueMetodoCobroExiste()) {
          // No aplica en compras, pero dejamos seguro por si luego agregas método de pago.
      }

      if (show) {
          recalcularDeuda();
      } else {
          limpiarCuotasCompra();
      }
  }

  function bloqueMetodoCobroExiste() {
      return false;
  }

  function toggleDiasPersonalizadosCompra() {
      if (!bloqueDiasPersonalizadosCompra || !frecuenciaCuotasCompra) return;

      const personalizado = frecuenciaCuotasCompra.value === "PERSONALIZADO";
      bloqueDiasPersonalizadosCompra.classList.toggle("d-none", !personalizado);
  }

  function recalcularDeuda() {
      if (!deudaRestante) return;

      const total = Number(document.getElementById("totalInput")?.value || 0);
      const inicial = Number(pagoInicial?.value || 0);

      let deuda = total - inicial;

      if (deuda < 0) deuda = 0;

      deudaRestante.value = f2(deuda);
      deudaRestante.style.color = deuda > 0 ? "#dc3545" : "#198754";

      return deuda;
  }

  function limpiarCuotasCompra() {
      if (!cuotasPreviewBodyCompra) return;

      cuotasPreviewBodyCompra.innerHTML = `
          <tr>
              <td colspan="3" class="text-center text-muted py-3">
                  Selecciona los datos del crédito y genera las cuotas.
              </td>
          </tr>
      `;
  }

  function generarCuotas() {
      if (!cuotasPreviewBodyCompra) return;

      cuotasPreviewBodyCompra.innerHTML = "";

      const total = Number(document.getElementById("totalInput")?.value || 0);
      const inicial = Number(pagoInicial?.value || 0);
      const n = Number(numCuotasInp?.value || 0);

      if (total <= 0) {
          cuotasPreviewBodyCompra.innerHTML = `
              <tr>
                  <td colspan="3" class="text-center text-danger py-3">
                      Primero agrega productos para calcular el total.
                  </td>
              </tr>
          `;
          recalcularDeuda();
          return;
      }

      if (inicial < 0) {
          cuotasPreviewBodyCompra.innerHTML = `
              <tr>
                  <td colspan="3" class="text-center text-danger py-3">
                      El pago inicial no puede ser negativo.
                  </td>
              </tr>
          `;
          recalcularDeuda();
          return;
      }

      if (inicial >= total) {
          cuotasPreviewBodyCompra.innerHTML = `
              <tr>
                  <td colspan="3" class="text-center text-danger py-3">
                      El pago inicial no puede ser igual o mayor al total.
                  </td>
              </tr>
          `;
          recalcularDeuda();
          return;
      }

      if (n <= 0) {
          cuotasPreviewBodyCompra.innerHTML = `
              <tr>
                  <td colspan="3" class="text-center text-danger py-3">
                      Ingresa un número válido de cuotas.
                  </td>
              </tr>
          `;
          recalcularDeuda();
          return;
      }

      const deuda = total - inicial;
      const montoBase = deuda / n;
      const dias = obtenerDiasPorFrecuenciaCompra();
      const fechaBase = obtenerFechaBaseCuotaCompra();

      let sumaCuotas = 0;

      for (let i = 0; i < n; i++) {
          const fecha = addDays(fechaBase, dias * i);

          let monto = Number(montoBase.toFixed(2));

          // Ajuste en la última cuota para evitar diferencia por redondeo
          if (i === n - 1) {
              monto = Number((deuda - sumaCuotas).toFixed(2));
          }

          sumaCuotas += monto;

          cuotasPreviewBodyCompra.innerHTML += `
              <tr>
                  <td class="fw-bold">
                      ${i + 1}
                  </td>

                  <td>
                      <input type="date"
                             name="fechaCuota"
                             class="form-control form-control-sm fecha-cuota"
                             value="${fecha}">
                  </td>

                  <td>
                      <input type="number"
                             name="montoCuota"
                             step="0.01"
                             min="0.01"
                             class="form-control form-control-sm text-end monto-cuota"
                             value="${f2(monto)}">
                  </td>
              </tr>
          `;
      }

      document.querySelectorAll(".monto-cuota")
          .forEach(i => i.addEventListener("input", recalcularDeudaPorCuotasCompra));

      recalcularDeudaPorCuotasCompra();
  }

 function recalcularDeudaPorCuotasCompra() {
     if (!deudaRestante) return;

     const total = Number(document.getElementById("totalInput")?.value || 0);
     const inicial = Number(pagoInicial?.value || 0);

     let sumaCuotas = 0;

     document.querySelectorAll(".monto-cuota").forEach(i => {
         sumaCuotas += Number(i.value || 0);
     });

     const deuda = Math.max(0, total - inicial);
     const diferencia = deuda - sumaCuotas;

     deudaRestante.value = f2(deuda);

     if (Math.abs(diferencia) > 0.05) {
         deudaRestante.style.color = "#dc3545";
         deudaRestante.title = "Las cuotas no coinciden con la deuda pendiente.";
     } else {
         deudaRestante.style.color = "#198754";
         deudaRestante.title = "Las cuotas coinciden con la deuda pendiente.";
     }
 }

  formaPagoSel?.addEventListener("change", toggleCredito);

  pagoInicial?.addEventListener("input", () => {
      recalcularDeuda();

      if (formaPagoSel?.value === "CREDITO") {
          generarCuotas();
      }
  });

  numCuotasInp?.addEventListener("input", () => {
      if (formaPagoSel?.value === "CREDITO") {
          generarCuotas();
      }
  });

  frecuenciaCuotasCompra?.addEventListener("change", () => {
      toggleDiasPersonalizadosCompra();

      if (formaPagoSel?.value === "CREDITO") {
          generarCuotas();
      }
  });

  diasPersonalizadosCompra?.addEventListener("input", () => {
      if (formaPagoSel?.value === "CREDITO"
          && frecuenciaCuotasCompra?.value === "PERSONALIZADO") {
          generarCuotas();
      }
  });

  fechaPrimeraCuotaCompra?.addEventListener("change", () => {
      if (formaPagoSel?.value === "CREDITO") {
          generarCuotas();
      }
  });

  btnGenerarCuotasCompra?.addEventListener("click", generarCuotas);

 toggleDiasPersonalizadosCompra();
 toggleCredito();

 recalcularTotales();
 });
/* ======================================================================
   EDITAR CABECERA: FORMA DE PAGO
   ====================================================================== */
document.addEventListener("DOMContentLoaded", () => {
    const formaPago = document.getElementById("formaPagoCabecera");
    const plazoDias = document.getElementById("plazoDias");
    const numeroCuotas = document.getElementById("numeroCuotas");

    if (!formaPago || !plazoDias || !numeroCuotas) return;

    function actualizarCondicionesPagoCabecera() {
        if (formaPago.value === "CONTADO") {
            plazoDias.value = "";
            numeroCuotas.value = "";
            plazoDias.disabled = true;
            numeroCuotas.disabled = true;
        } else {
            plazoDias.disabled = false;
            numeroCuotas.disabled = false;
        }
    }

    formaPago.addEventListener("change", actualizarCondicionesPagoCabecera);
    actualizarCondicionesPagoCabecera();
});
