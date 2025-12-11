/* ============================================================================
   ventas.js – VIALSA ERP (VERSIÓN FINAL PROFESIONAL)
============================================================================ */

/* ============================================================================
   CSRF SPRING SECURITY
============================================================================ */
const CSRF_TOKEN  = document.querySelector("meta[name='_csrf']")?.content;
const CSRF_HEADER = document.querySelector("meta[name='_csrf_header']")?.content;

/* ============================================================================
   UTILIDADES
============================================================================ */
const f2 = n => Number(n || 0).toFixed(2);

/* ============================================================================
   FORMULARIO PRINCIPAL
============================================================================ */
document.addEventListener("DOMContentLoaded", () => {

    /* ----------------------------------------------------------------------
       ELEMENTOS DEL FORMULARIO
    ---------------------------------------------------------------------- */
    const tablaBody      = document.querySelector("#detalleBody");
    const btnAgregar     = document.getElementById("agregarProducto");

    const formaPago      = document.getElementById("formaPago");
    const bloqueCredito  = document.getElementById("bloqueCredito");
    const pagoInicial    = document.getElementById("pagoInicial");
    const deudaVenta     = document.getElementById("deudaVenta");

    const totalVentaInput = document.getElementById("totalVentaInput");
    const lblTotal        = document.getElementById("lblTotal");

    // CLIENTE
    const inputDoc   = document.getElementById("cliDocumento");
    const inputNom   = document.getElementById("cliNombre");
    const inputTel   = document.getElementById("cliTelefono");
    const inputIdCli = document.getElementById("idCliente");
    const btnBuscarCliente = document.getElementById("btnBuscarCliente");

    // COMPROBANTE
    const tipoComp = document.getElementById("tipoComprobante");
    const nroInput = document.getElementById("nroComprobante");

    const formVenta = document.getElementById("ventaForm");

    if (!tablaBody) return;


    /* ============================================================================
       SELECT2 – MÓDULO DE PRODUCTOS
============================================================================ */
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
            url: "/productos/buscar",
            dataType: "json",
            delay: 200,
            data: params => ({ term: params.term }),
            processResults: data => ({
                results: data.map(p => ({
                    id: p.id,
                    text: p.text,
                    img: p.img || "",              // ✔ imagen
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
    $(sel).off("select2:select").on("select2:select", function (e) {

        const data = e.params.data;
        const tr = sel.closest("tr");

        // ID PRODUCTO REAL
        sel.value = data.id;

        // UNIDAD
        tr.querySelector(".unidad-input").textContent = data.unidadNombre ?? "";
        tr.querySelector(".unidad-hidden").value = data.idUnidad ?? 0;

        // STOCK
        tr.dataset.stock = data.stock || 0;

        // PRECIO
        const precio = tr.querySelector(".precio");
        precio.value = Number(data.precio || 0).toFixed(2);
        precio.readOnly = true;

        // DESCUENTO
        const desc = tr.querySelector(".descuento");
        desc.max = Number(data.descuentoMaximo || 0);
        desc.dataset.maximo = Number(data.descuentoMaximo || 0);

        // Recalcular subtotal
        recalcularFila(tr);
    });

}


function formatoProductoMini(p) {
    if (!p.id) return p.text;

    const tieneImagen = p.img && p.img.trim() !== "";

    return $(`
        <div class="prod-item-mini">
            ${
                tieneImagen
                ? `<img src="/img/productos/${p.img}" class="prod-img-mini">`
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
                <small>Stock: ${p.stock} | S/ ${p.precio}</small>
            </div>
        </div>
    `);
}



    function formatoProducto(p) {
        if (!p.id) return p.text;

        return $(`
            <div class="d-flex align-items-center">
                <img src="/img/productos/${p.img || 'noimage.png'}" class="prod-img">
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
   function nuevaFila() {

       const tr = document.createElement("tr");

       tr.innerHTML = `
           <td>
               <select class="form-select seleccionar-producto"
                       name="idProducto"
                       required>
               </select>
           </td>

           <td>
               <span class="unidad-input badge bg-secondary"></span>
               <input type="hidden" name="idUnidad" class="unidad-hidden" value="0">
           </td>

           <td>
               <input type="number" class="form-control cantidad"
                      name="cantidad" value="1" step="0.01" required>
           </td>

           <td>
               <input type="number" class="form-control precio"
                      name="precioUnitario" value="0.00" step="0.01" required>
           </td>

           <td>
               <input type="number" class="form-control descuento"
                      name="descuento" value="0.00" step="0.01" required>
           </td>

           <td class="subtotal-cell text-end">0.00</td>

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
   }


function bindFilaEvents(tr) {

    const cantInput = tr.querySelector(".cantidad");
    const precioInput = tr.querySelector(".precio");
    const descInput = tr.querySelector(".descuento");

    // 🔥 Evitar números negativos
    cantInput.addEventListener("input", () => {
        if (cantInput.value < 0) cantInput.value = 0;
        validarStock(tr);
        recalcularFila(tr);
    });

    precioInput.addEventListener("input", () => {
        if (precioInput.value < 0) precioInput.value = 0;
        recalcularFila(tr);
    });

    descInput.addEventListener("input", () => {
        if (descInput.value < 0) descInput.value = 0;
        recalcularFila(tr);
    });

    tr.querySelector(".btn-eliminar-fila").addEventListener("click", () => {
        tr.remove();
        recalcularTotales();
    });
}



    /* ============================================================================
       VALIDACIÓN DE STOCK
============================================================================ */
    function validarStock(tr) {
        const stock = parseFloat(tr.dataset.stock || 0);
        const cant  = parseFloat(tr.querySelector(".cantidad").value || 0);

        if (cant > stock) {
            tr.querySelector(".cantidad").value = stock;
            Swal.fire("Stock insuficiente", "La cantidad supera el stock disponible.", "warning");
        }
    }


    /* ============================================================================
       CÁLCULOS DE TOTALES
============================================================================ */
    function recalcularFila(tr) {
        const c = parseFloat(tr.querySelector(".cantidad").value) || 0;
        const p = parseFloat(tr.querySelector(".precio").value)   || 0;
        const d = parseFloat(tr.querySelector(".descuento").value)|| 0;

        tr.querySelector(".subtotal-cell").textContent = f2(Math.max(0, c * p - d));

        recalcularTotales();
    }


    function recalcularTotales() {

        let suma = 0;

        tablaBody.querySelectorAll("tr").forEach(tr => {
            suma += Number(tr.querySelector(".subtotal-cell").textContent) || 0;
        });

        const subtotal = suma;
        const igv = subtotal * 0.18;
        const total = subtotal + igv;

        // Mostrar en pantalla
        document.getElementById("lblSubtotal").textContent = "S/ " + f2(subtotal);
        document.getElementById("lblIgv").textContent       = "S/ " + f2(igv);
        document.getElementById("lblTotal").textContent     = "S/ " + f2(total);

        // Guardar valor para enviar a Spring
        totalVentaInput.value = f2(total);

        actualizarCredito();
    }


    /* ============================================================================
       CRÉDITO
============================================================================ */
    function actualizarCredito() {

        if (!formaPago) return;

        if (formaPago.value === "CREDITO") {

            bloqueCredito.classList.remove("d-none");

            const total   = Number(totalVentaInput.value || 0);
            const inicial = Number(pagoInicial.value || 0);

            deudaVenta.value = f2(Math.max(total - inicial, 0));

        } else {

            bloqueCredito.classList.add("d-none");
            deudaVenta.value = "0.00";
        }
    }


    formaPago?.addEventListener("change", actualizarCredito);
    pagoInicial?.addEventListener("input", actualizarCredito);


    /* ============================================================================
       COMPROBANTE – OBTENER SIGUIENTE NÚMERO
============================================================================ */
    async function actualizarNumeroComprobante() {
        if (!tipoComp.value) {
            nroInput.value = "";
            return;
        }

        const resp = await fetch(`/ventas/siguiente-numero?tipo=${tipoComp.value}`);
        nroInput.value = await resp.text();
    }

    tipoComp?.addEventListener("change", actualizarNumeroComprobante);


/* ============================================================================
   CLIENTE – CONSULTA DNI/RUC + AUTOREGISTRO + AJUSTE COMPROBANTE
   (MÓDULO COMPLETO)
============================================================================ */
async function buscarCliente() {

    const doc = inputDoc.value.trim();
    if (!doc) return;

    // VALIDACIÓN DE FORMATO
    if (doc.length !== 8 && doc.length !== 11) {
        Swal.fire("Documento inválido", "DNI = 8 dígitos | RUC = 11 dígitos", "warning");
        return;
    }

    const tipo = (doc.length === 8) ? "dni" : "ruc";

    try {
        // CONSULTA A API EXTERNA
        const resp = await fetch(`/api/externo/${tipo}/${doc}`);

        if (!resp.ok) {
            // ❗ PERMITIR LLENADO MANUAL
            Swal.fire("Sin datos externos", "Puedes llenar los datos manualmente.", "info");

            inputNom.removeAttribute("readonly");
            inputTel.removeAttribute("readonly");

            inputNom.value = "";
            inputTel.value = "";

            inputIdCli.value = ""; // Se creará al guardar

            return;
        }

        // SI HAY DATOS EXTERNOS
        const json = await resp.json();
        const data = json.data;

        // AUTORRELLENO
        inputNom.value = data.nombre_completo || data.nombres || "";
        inputTel.value = data.telefono || "";

        // 🔒 BLOQUEAR CAMPOS SOLO SI VIENEN DE API
        inputNom.setAttribute("readonly", true);
        inputTel.setAttribute("readonly", true);

        // REGISTRO AUTOMÁTICO EN BD
        const clienteResp = await fetch("/clientes/crear-automatico", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [CSRF_HEADER]: CSRF_TOKEN
            },
            body: JSON.stringify({
                nro_documento: doc,
                nombres: inputNom.value,
                apellidos: "",
                telefono: inputTel.value || "",
                direccion: data.direccion || ""
            })
        });

        const clienteJson = await clienteResp.json();
        inputIdCli.value = clienteJson.id;

        ajustarComprobantePorDocumento();

        Swal.fire("Éxito", "Cliente cargado correctamente", "success");

    } catch (e) {
        console.error(e);

        // ❗ PERMITIR LLENADO MANUAL SI FALLA LA API
        Swal.fire("Advertencia", "No se pudo consultar el documento. Puedes llenar los datos manualmente.", "warning");

        inputNom.removeAttribute("readonly");
        inputTel.removeAttribute("readonly");

        inputNom.value = "";
        inputTel.value = "";

        inputIdCli.value = "";
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
        tipoComp.disabled = true;

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
    ajustarComprobantePorDocumento();
});


/* ============================================================================
   LISTENERS DEL MÓDULO CLIENTE
============================================================================ */
btnBuscarCliente?.addEventListener("click", buscarCliente);
inputDoc?.addEventListener("keyup", e => {
    if (e.key === "Enter") buscarCliente();
});


/* ============================================================================
   VALIDACIÓN FINAL DEL FORMULARIO
============================================================================ */
formVenta?.addEventListener("submit", e => {

    let errores = [];

    // Validar documento
    if (!/^\d{8}$|^\d{11}$/.test(inputDoc.value.trim())) {
        errores.push("Documento del cliente inválido.");
    }

    // Validar que haya filas
    if (tablaBody.children.length === 0) {
        errores.push("Debe agregar al menos un producto.");
    }

    // Validación por fila
    tablaBody.querySelectorAll("tr").forEach(tr => {

        const prod = tr.querySelector(".seleccionar-producto").value;
        const cant = tr.querySelector(".cantidad").value;
        const precio = tr.querySelector(".precio").value;

        // Validar producto
        if (!prod || prod.trim() === "") {
            errores.push("Hay un producto sin seleccionar.");
        }

        // Validar cantidad
        if (!cant || Number(cant) <= 0) {
            errores.push("Cantidad inválida en una fila.");
        }

        // Validar precio
        if (!precio || Number(precio) < 0) {
            errores.push("Precio inválido en una fila.");
        }

    });

    // SI HAY ERRORES → DETENER SUBMIT
    if (errores.length > 0) {
        e.preventDefault();
        Swal.fire("Error", errores.join("<br>"), "error");
        return; // 🔥 EVITA QUE EL FORMULARIO SE ENVÍE IGUAL
    }
});





/* ============================================================================
   INICIALIZACIÓN DEL FORMULARIO
============================================================================ */
   // 🔥 BOTÓN "AGREGAR PRODUCTO"
    btnAgregar?.addEventListener("click", nuevaFila);

// Fila inicial
nuevaFila();

// Comprobante
actualizarNumeroComprobante();

// Totales
recalcularTotales();

// Crédito
actualizarCredito();

}); // FIN DOMContentLoaded
