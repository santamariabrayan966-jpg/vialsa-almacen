document.addEventListener("DOMContentLoaded", () => {

    // =====================================================
    // 1. MODAL GENÉRICO PARA TODAS LAS VENTANAS DE COMPRAS
    // =====================================================
    const modalEl   = document.getElementById("compraModal");
    const frame     = document.getElementById("compraModalFrame");
    let   compraModal = null;

    if (modalEl && frame && typeof bootstrap !== "undefined") {
        compraModal = new bootstrap.Modal(modalEl);

        // Delegación: cualquier elemento con data-modal-url abre el modal
        document.addEventListener("click", ev => {
            const btn = ev.target.closest("[data-modal-url]");
            if (!btn) return;

            ev.preventDefault();
            const url = btn.getAttribute("data-modal-url");
            if (!url) return;

            frame.src = url;
            compraModal.show();
        });

        // Limpiar iframe al cerrar
        modalEl.addEventListener("hidden.bs.modal", () => {
            frame.src = "";
        });
    }

    // =====================================================
    // 2. FORMULARIO DE COMPRA (sólo si existe #compraForm)
    // =====================================================
    const compraForm = document.getElementById("compraForm");
    if (compraForm) {
        inicializarFormularioCompra();
    }

    // =====================================================
    // FUNCIONES
    // =====================================================

    function inicializarFormularioCompra() {
        const tblBody       = document.querySelector("#tablaProductos tbody");
        const filaBase      = document.querySelector(".producto-item");
        const btnAgregar    = document.getElementById("agregarProducto");

        const incluyeIgv    = document.getElementById("incluyeIgv");
        const porcIgvInput  = document.getElementById("porcentajeIgv");

        const lblSubtotal   = document.getElementById("lblSubtotal");
        const lblIgv        = document.getElementById("lblIgv");
        const lblTotal      = document.getElementById("lblTotal");

        const subtotalInput = document.getElementById("subtotalInput");
        const montoIgvInput = document.getElementById("montoIgvInput");
        const totalInput    = document.getElementById("totalInput");

        const monedaSelect  = document.getElementById("moneda");
        const tipoCambio    = document.getElementById("tipoCambio");
        const formaPago     = document.getElementById("formaPago");
        const plazoDias     = document.getElementById("plazoDias");
        const numeroCuotas  = document.getElementById("numeroCuotas");

        if (!tblBody || !filaBase) return;

        // ---- Condiciones de pago ----
        function actualizarCondicionesPago() {
            if (!formaPago) return;
            if (formaPago.value === "CONTADO") {
                if (plazoDias)    { plazoDias.value = "";    plazoDias.disabled    = true; }
                if (numeroCuotas) { numeroCuotas.value = ""; numeroCuotas.disabled = true; }
            } else {
                if (plazoDias)    plazoDias.disabled    = false;
                if (numeroCuotas) numeroCuotas.disabled = false;
            }
        }

        // ---- Moneda / tipo de cambio ----
        function actualizarTipoCambio() {
            if (!monedaSelect || !tipoCambio) return;
            if (monedaSelect.value === "PEN") {
                tipoCambio.value = "";
                tipoCambio.readOnly = true;
            } else {
                if (!tipoCambio.value) tipoCambio.value = "3.8000";
                tipoCambio.readOnly = false;
            }
        }

        // ---- Cálculo de totales ----
        function calcularTotales() {
            const filas = document.querySelectorAll("#tablaProductos tbody tr");
            let totalBruto = 0;

            filas.forEach(fila => {
                const cantidad  = parseFloat(fila.querySelector(".cantidad")?.value)  || 0;
                const precio    = parseFloat(fila.querySelector(".precio")?.value)    || 0;
                const descuento = parseFloat(fila.querySelector(".descuento")?.value) || 0;

                const subtotalFila = (cantidad * precio) - descuento;
                const cellSubtotal = fila.querySelector(".subtotal");
                if (cellSubtotal) {
                    cellSubtotal.textContent = subtotalFila.toFixed(2);
                }
                totalBruto += subtotalFila;
            });

            const tasa = (parseFloat(porcIgvInput?.value) || 0) / 100;
            let baseImponible, igv, total;

            if (incluyeIgv && incluyeIgv.checked) {
                baseImponible = totalBruto / (1 + tasa);
                igv           = totalBruto - baseImponible;
                total         = totalBruto;
            } else {
                baseImponible = totalBruto;
                igv           = baseImponible * tasa;
                total         = baseImponible + igv;
            }

            if (lblSubtotal) lblSubtotal.textContent = "S/ " + baseImponible.toFixed(2);
            if (lblIgv)      lblIgv.textContent      = "S/ " + igv.toFixed(2);
            if (lblTotal)    lblTotal.textContent    = "S/ " + total.toFixed(2);

            if (subtotalInput) subtotalInput.value = baseImponible.toFixed(2);
            if (montoIgvInput) montoIgvInput.value = igv.toFixed(2);
            if (totalInput)    totalInput.value    = total.toFixed(2);
        }

        // ---- Eventos ----

        // Recalcular totales al escribir
        document.addEventListener("input", e => {
            if (e.target.matches(".cantidad, .precio, .descuento, #porcentajeIgv")) {
                calcularTotales();
            }
        });

        // Cambiar "incluye IGV"
        if (incluyeIgv) {
            incluyeIgv.addEventListener("change", calcularTotales);
        }

        // Agregar fila
        if (btnAgregar) {
            btnAgregar.addEventListener("click", () => {
                const nuevaFila = filaBase.cloneNode(true);
                // limpiar inputs
                nuevaFila.querySelectorAll("input").forEach(i => {
                    i.value = i.defaultValue;
                });
                // reset selects
                nuevaFila.querySelectorAll("select").forEach(s => {
                    s.selectedIndex = 0;
                });
                tblBody.appendChild(nuevaFila);
                calcularTotales();
            });
        }

        // Eliminar fila
        document.addEventListener("click", e => {
            if (e.target.closest(".eliminar")) {
                const filas = document.querySelectorAll(".producto-item");
                if (filas.length > 1) {
                    e.target.closest("tr").remove();
                    calcularTotales();
                } else {
                    alert("Debe haber al menos un producto en la compra.");
                }
            }
        });

        // Moneda / tipo de cambio
        if (monedaSelect) {
            monedaSelect.addEventListener("change", actualizarTipoCambio);
            actualizarTipoCambio();
        }

        // Forma de pago
        if (formaPago) {
            formaPago.addEventListener("change", actualizarCondicionesPago);
            actualizarCondicionesPago();
        }

        // Cálculo inicial
        calcularTotales();
    }

});
