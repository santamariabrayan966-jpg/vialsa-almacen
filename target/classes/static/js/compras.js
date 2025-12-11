/* ======================================================================
   COMPRAS.JS – VIALSA ERP (VERSIÓN FINAL, PROFESIONAL Y FUNCIONAL)
   ====================================================================== */

/* ======================================================================
   CSRF SPRING SECURITY
   ====================================================================== */
const csrfToken  = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
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

/* ======================================================================
   MODALES
   ====================================================================== */
window.openCompraModal = function (url, titulo = "Compras") {
    const modal = document.getElementById("compraModal");
    const iframe = document.getElementById("compraModalIframe");
    const title = document.getElementById("compraModalTitle");

    if (modal && iframe) {
        title.textContent = titulo;
        iframe.src = url;

        const bs = new bootstrap.Modal(modal);
        bs.show();

        modal.addEventListener("hidden.bs.modal", () => {
            iframe.src = "";
        }, { once: true });
    }
};

window.abrirModalCuotas = function (idCompra, moneda) {

    const simbolo = moneda === "USD" ? "$ " : "S/ ";

    fetch(`/compras/cuotas/${idCompra}`)
        .then(r => r.json())
        .then(cuotas => {
            const tbody = document.getElementById("tablaCuotas");
            const info = document.getElementById("infoCuotas");

            if (!tbody) return;

            if (!cuotas || cuotas.length === 0) {
                tbody.innerHTML = "";
                if (info) info.innerHTML = "<div class='alert alert-warning'>No hay cuotas.</div>";
                return;
            }

            tbody.innerHTML = cuotas.map(c => `
                <tr class="text-center">
                    <td>${c.numeroCuota}</td>
                    <td>${c.fechaVencimiento}</td>
                    <td>${simbolo}${f2(c.montoCuota)}</td>
                    <td>${c.estado === "PENDIENTE"
                        ? '<span class="badge bg-danger">Pendiente</span>'
                        : '<span class="badge bg-success">Pagada</span>'}</td>
                    <td>${c.estado === "PENDIENTE"
                        ? `<button onclick="pagarCuota(${c.idCuotaCompra})" class="btn btn-success btn-sm">Pagar</button>`
                        : ""}</td>
                </tr>
            `).join("");

            new bootstrap.Modal(document.getElementById("modalCuotas")).show();
        });
};

window.pagarCuota = function (idCuota) {
    if (!confirm("¿Confirmar pago?")) return;

    fetch(`/compras/cuotas/pagar/${idCuota}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
        }
    })
        .then(r => r.text())
        .then(r => {
            if (r === "OK") {
                alert("Pago registrado");
                location.reload();
            } else alert("Error al pagar cuota");
        });
};

/* ======================================================================
   FORMULARIO PRINCIPAL
   ====================================================================== */
document.addEventListener("DOMContentLoaded", () => {

    const form = document.getElementById("compraForm");
    if (!form) return;

    /* ------------------------------------------
       ELEMENTOS HTML
       ------------------------------------------ */
    const tablaBody = document.querySelector("#tablaProductos tbody");
    const btnAgregarProducto = document.getElementById("agregarProducto");

    const monedaSel = document.getElementById("moneda");
    const fechaInput = document.getElementById("fechaEmision");
    const tipoCambioInput = document.getElementById("tipoCambio");

    const tipoComp = document.getElementById("tipoComprobante");
  const serieInput = document.getElementById("inputSerie");
  const numeroInput = document.getElementById("inputNumero");

    const vistaFantasma = document.getElementById("vistaComprobante");

    const idCompraInput = document.querySelector("input[name='idCompra']");
    const esEdicion = !!idCompraInput?.value;

    /* ------------------------------------------
       ELEMENTOS CRÉDITO
       ------------------------------------------ */
    const formaPagoSel   = document.getElementById("formaPago");
    const seccionCredito = document.getElementById("seccionCredito");
    const pagoInicial    = document.getElementById("pagoInicial");
    const numCuotasInp   = document.getElementById("numeroCuotasCredito");
    const intervaloInp   = document.getElementById("intervaloPago");
    const contCuotas     = document.getElementById("contenedorCuotas");
    const deudaRestante  = document.getElementById("deudaRestante");
/* ======================================================================
   PLACEHOLDER FANTASMA DENTRO DEL INPUT
   ====================================================================== */
function actualizarPlaceholder() {
    const tipo = tipoComp.value;

    let seriePh = "SERIE";
    let numeroPh = "NÚMERO";

    switch (tipo) {
        case "FACTURA":
            seriePh = "F001";
            numeroPh = "000001";
            break;
        case "BOLETA":
            seriePh = "B001";
            numeroPh = "000001";
            break;
        case "N.CREDITO":
            seriePh = "NC01";
            numeroPh = "000001";
            break;
        case "N.DEBITO":
            seriePh = "ND01";
            numeroPh = "000001";
            break;
    }

    serieInput.placeholder = seriePh;
    numeroInput.placeholder = numeroPh;
}

tipoComp.addEventListener("change", actualizarPlaceholder);
actualizarPlaceholder();


    /* ======================================================================
       VALIDACIÓN
       ====================================================================== */
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

        if (!document.querySelector("select[name='idProveedor']").value)
            registrarError("idProveedor", "Seleccione proveedor.");

        if (!tipoComp.value)
            registrarError("tipoComprobante", "Seleccione tipo.");

        if (!serieInput.value.trim())
            registrarError("serie", "Serie obligatoria");

        if (!numeroInput.value.trim())
            registrarError("numero", "Número obligatorio");

        if (monedaSel.value === "USD" &&
            (!tipoCambioInput.value || Number(tipoCambioInput.value) <= 0))
            registrarError("tipoCambio", "Tipo de cambio inválido.");

        if (tablaBody.children.length === 0)
            registrarError("idProducto", "Debe agregar productos.");

        if (total <= 0)
            registrarError("totales", "Total inválido.");

        if (!document.getElementById("panelErrores").classList.contains("d-none"))
            e.preventDefault();
    });

    /* ======================================================================
       CÁLCULOS DE FILAS
       ====================================================================== */
    function recalcularFila(fila) {
        const c = Number(fila.querySelector(".cantidad")?.value || 0);
        const p = Number(fila.querySelector(".precio")?.value || 0);
        const d = Number(fila.querySelector(".descuento")?.value || 0);

        const subtotal = Math.max(0, c * p - d);
        fila.querySelector(".subtotal").textContent = f2(subtotal);

        recalcularTotales();
    }

    function recalcularTotales() {
        let suma = 0;
        tablaBody.querySelectorAll("tr").forEach(f => {
            suma += Number(f.querySelector(".subtotal")?.textContent || 0);
        });

        const incluyeIgv = document.getElementById("incluyeIgv").checked;
        const porc = Number(document.getElementById("porcentajeIgv").value || 0) / 100;

        let subtotal, igv, total;

        if (incluyeIgv) {
            total = suma;
            subtotal = total / (1 + porc);
            igv = total - subtotal;
        } else {
            subtotal = suma;
            igv = subtotal * porc;
            total = subtotal + igv;
        }

        const simbolo = monedaSel.value === "USD" ? "$" : "S/";

        // Totales principales (en la moneda del comprobante)
        document.getElementById("lblSubtotal").textContent = `${simbolo} ${f2(subtotal)}`;
        document.getElementById("lblIgv").textContent      = `${simbolo} ${f2(igv)}`;
        document.getElementById("lblTotal").textContent    = `${simbolo} ${f2(total)}`;

        document.getElementById("subtotalInput").value = f2(subtotal);
        document.getElementById("montoIgvInput").value = f2(igv);
        document.getElementById("totalInput").value    = f2(total);

        if (formaPagoSel.value === "CREDITO") recalcularDeuda();
        actualizarEquivalenteSoles();
        // Si es PEN ocultamos equivalente
        if (monedaSel.value !== "USD") {
            document.getElementById("equivalenteSoles").classList.add("d-none");
        }


    }
    /* ======================================================================
       EQUIVALENTE EN SOLES (PEN) – PROFESIONAL, AUTOMÁTICO
       ====================================================================== */
    function actualizarEquivalenteSoles() {
        const moneda = monedaSel.value;
        const tc = Number(tipoCambioInput.value || 0);

        const subtotal = Number(document.getElementById("subtotalInput").value || 0);
        const igv = Number(document.getElementById("montoIgvInput").value || 0);
        const total = Number(document.getElementById("totalInput").value || 0);

        const box = document.getElementById("equivalenteSoles");

        // Si no es USD o no hay tipo de cambio, ocultar
        if (moneda !== "USD" || tc <= 0) {
            box.classList.add("d-none");
            return;
        }

        // Mostrar y rellenar
        box.classList.remove("d-none");

        document.getElementById("lblSubtotalPen").textContent = "S/ " + f2(subtotal * tc);
        document.getElementById("lblIgvPen").textContent      = "S/ " + f2(igv * tc);
        document.getElementById("lblTotalPen").textContent    = "S/ " + f2(total * tc);
    }

    /* ======================================================================
       UNIDAD AUTOMÁTICA SEGÚN PRODUCTO
       ====================================================================== */
    function aplicarUnidadPorProductoEnFila(fila) {
        if (!fila) return;

        const selectProducto = fila.querySelector("select[name='idProducto']");
        if (!selectProducto) return;

        const opcion = selectProducto.options[selectProducto.selectedIndex];
        if (!opcion) return;

        const unidadId = opcion.getAttribute("data-unidad-id");
        if (!unidadId) return;

        const selectUnidad = fila.querySelector("select[name='idUnidad']");
        if (!selectUnidad) return;

        // asignar unidad correspondiente al producto
        selectUnidad.value = unidadId;
    }


        function bindEventosFila(fila) {

            // Cantidad cambia → recalcula
            fila.querySelector(".cantidad")
                ?.addEventListener("input", () => recalcularFila(fila));

            // Precio cambia → recalcula
            fila.querySelector(".precio")
                ?.addEventListener("input", () => recalcularFila(fila));

            // Descuento cambia → recalcula
            fila.querySelector(".descuento")
                ?.addEventListener("input", () => recalcularFila(fila));

            // PRODUCTO cambia → pone unidad automática + recalcula
            const selProd = fila.querySelector("select[name='idProducto']");
            if (selProd) {
                selProd.addEventListener("change", () => {
                    aplicarUnidadPorProductoEnFila(fila);   // 👈 aquí
                    recalcularFila(fila);
                });

                // Si la fila ya viene con producto seleccionado (edición / error validación)
                if (selProd.value) {
                    aplicarUnidadPorProductoEnFila(fila);   // 👈 aquí
                }
            }

            // Si la unidad cambiara manualmente (por si acaso) → recalcula
            fila.querySelector("select[name='idUnidad']")
                ?.addEventListener("change", () => recalcularFila(fila));

            // Eliminar fila → recalcula totales
            fila.querySelector(".eliminar")
                ?.addEventListener("click", () => {
                    fila.remove();
                    recalcularTotales();
                });
        }


    tablaBody.querySelectorAll("tr").forEach(fila => {
        bindEventosFila(fila);
        activarBuscadorEnFila(fila);   // 👈 NUEVO
    });


    btnAgregarProducto?.addEventListener("click", () => {
        const base = tablaBody.firstElementChild.cloneNode(true);

        // limpiar inputs
        base.querySelectorAll("input").forEach(i => {
            if (i.classList.contains("filtro-producto")) {
                i.value = "";      // 👈 buscador vacío
            } else {
                i.value = 0;       // cantidades, precios, etc.
            }
        });

        const selProd  = base.querySelector("select[name='idProducto']");
        const selUni   = base.querySelector("select[name='idUnidad']");
        const subtotal = base.querySelector(".subtotal");

        if (selProd) {
            selProd.value = "";
            // restaurar visibilidad de opciones por si la fila clonada estaba filtrada
            Array.from(selProd.options).forEach(opt => opt.style.display = "");
        }
        if (selUni) selUni.value = "";
        if (subtotal) subtotal.textContent = "0.00";

        tablaBody.appendChild(base);
        bindEventosFila(base);
        activarBuscadorEnFila(base);
        recalcularTotales();
    });

    function activarBuscadorEnFila(fila) {
        const inputFiltro = fila.querySelector(".filtro-producto");
        const select = fila.querySelector("select[name='idProducto']");

        if (!inputFiltro || !select) return;

        inputFiltro.addEventListener("input", () => {
            const filtro = inputFiltro.value.toLowerCase();

            Array.from(select.options).forEach(opt => {
                if (opt.value === "") return; // no ocultar "Seleccione..."

                const texto = opt.textContent.toLowerCase();
                opt.style.display = texto.includes(filtro) ? "" : "none";
            });

            // Si el actual ya no coincide, resetearlo
            const seleccionado = select.options[select.selectedIndex];
            if (seleccionado && seleccionado.style.display === "none") {
                select.value = "";
            }
        });
    }


   /* ======================================================================
      TIPO DE CAMBIO AUTO
      ====================================================================== */
   async function obtenerTC(fecha) {
       try {
           console.log("Solicitando tipo de cambio para:", fecha);

           const r = await fetch(`/compras/tipo-cambio?fecha=${fecha}`);

           if (!r.ok) {
               console.error("Error HTTP al obtener tipo de cambio:", r.status);
               return;
           }

           const data = await r.json();
           console.log("Respuesta tipo-cambio:", data);

           if (data.error) {
               console.warn("SUNAT no devolvió tipo de cambio:", data.mensaje || data.error);
               tipoCambioInput.value = "";
               return;
           }

           if (!data.venta) {
               console.warn("No existe venta en la respuesta:", data);
               tipoCambioInput.value = "";
               return;
           }

           tipoCambioInput.value = Number(data.venta).toFixed(3);

       } catch (e) {
           console.error("Error JS al obtener tipo de cambio:", e);
       }
   }

  function actualizarTC() {
      if (monedaSel.value !== "USD") {
          tipoCambioInput.value = "";
          return;
      }

      if (!fechaInput.value) {
          fechaInput.value = new Date().toISOString().substring(0, 10);
      }

      const f = fechaInput.value;

      obtenerTC(f);
  }


   monedaSel.addEventListener("change", actualizarTC);
   fechaInput.addEventListener("change", actualizarTC);
   if (monedaSel.value === "USD") actualizarTC();

    /* ======================================================================
       SERIE / NÚMERO AUTO DESDE BD  (solo limpia inputs y actualiza placeholder)
       ====================================================================== */
    async function actualizarSerieNumero() {
        if (esEdicion) return;

        const tipo = tipoComp.value;
        if (!tipo) return;

        // No escribir nada en los inputs, solo limpiar
        serieInput.value = "";
        numeroInput.value = "";

        // Actualiza solo el placeholder fantasma
        actualizarPlaceholder();
    }

    tipoComp.addEventListener("change", actualizarSerieNumero);
    if (!esEdicion && tipoComp.value) actualizarSerieNumero();

    /* ======================================================================
       **FUNCIÓN OBLIGATORIA PARA EVITAR ERROR**
       Antes existía en tu JS pero ya NO se usa.
       La dejamos vacía para que no rompa el script.
       ====================================================================== */
    function actualizarVistaFantasma() {
        // Función intencionalmente vacía
    }

    /* ======================================================================
       BLOQUEAR LETRAS EN SERIE Y NÚMERO
       ====================================================================== */
    serieInput.addEventListener("input", () => {
        serieInput.value = serieInput.value
            .toUpperCase()
            .replace(/[^A-Z0-9]/g, '')
            .substring(0, 4);
    });

    numeroInput.addEventListener("input", () => {
        numeroInput.value = numeroInput.value
            .replace(/\D/g, '')
            .substring(0, 6);
    });


   /* ======================================================================
      CRÉDITO
      ====================================================================== */
   function toggleCredito() {
       const show = formaPagoSel.value === "CREDITO";
       seccionCredito.classList.toggle("d-none", !show);
       if (show) generarCuotas();
   }

   formaPagoSel.addEventListener("change", toggleCredito);
   toggleCredito();

   /* 👇 ESTOS LISTENERS ESTÁN EN EL LUGAR CORRECTO */
   pagoInicial.addEventListener("input", generarCuotas);
   numCuotasInp.addEventListener("input", generarCuotas);
   intervaloInp.addEventListener("change", generarCuotas);
   fechaInput.addEventListener("change", generarCuotas);


   function generarCuotas() {
       contCuotas.innerHTML = "";

       const total = Number(document.getElementById("totalInput").value || 0);
       const inicial = Number(pagoInicial.value || 0);
       const n = Number(numCuotasInp.value || 0);
       const intervalo = Number(intervaloInp.value || 30);

       if (total <= 0 || n <= 0) {
           recalcularDeuda();
           return;
       }

       const deuda = total - inicial;
       const monto = deuda / n;

       const base = fechaInput.value || new Date().toISOString().substring(0, 10);

       for (let i = 0; i < n; i++) {
           const fecha = addDays(base, intervalo * (i + 1));

           contCuotas.innerHTML += `
               <div class="col-md-6 mb-2">
                   <label class="fw-semibold">Fecha Cuota ${i + 1}</label>
                   <input type="date" name="fechaCuota" class="form-control" value="${fecha}">
               </div>

               <div class="col-md-6 mb-2">
                   <label class="fw-semibold">Monto Cuota ${i + 1} (S/)</label>
                   <input type="number" name="montoCuota" step="0.01"
                       class="form-control monto-cuota" value="${f2(monto)}">
               </div>
           `;
       }

       document.querySelectorAll(".monto-cuota")
           .forEach(i => i.addEventListener("input", recalcularDeuda));

       recalcularDeuda();
   }

   function recalcularDeuda() {
       const total = Number(document.getElementById("totalInput").value || 0);
       const inicial = Number(pagoInicial.value || 0);

       let suma = 0;
       document.querySelectorAll(".monto-cuota").forEach(i => {
           suma += Number(i.value || 0);
       });

       const deuda = (total - inicial) - suma;
       deudaRestante.value = f2(deuda);
       deudaRestante.style.color = deuda < 0 ? "red" : "#000";
   }
});   // ← cierra el DOMContentLoaded

/* ======================================================================
   BUSCADOR DE COMPRAS (LISTADO PRINCIPAL)
   ====================================================================== */
document.addEventListener("DOMContentLoaded", () => {
    const input = document.getElementById("buscadorCompras");
    const tabla = document.querySelector("#tablaCompras tbody");

    if (!input || !tabla) return;

    input.addEventListener("keyup", () => {
        const filtro = input.value.toLowerCase();

        tabla.querySelectorAll("tr").forEach(row => {
            const texto = row.innerText.toLowerCase();
            row.style.display = texto.includes(filtro) ? "" : "none";
        });
    });

});

// FIN DEL ARCHIVO
