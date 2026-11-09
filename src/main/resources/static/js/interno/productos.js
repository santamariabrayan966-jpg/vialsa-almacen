// productos.js
function obtenerHeadersCsrf() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');

    if (tokenMeta && headerMeta && tokenMeta.content && headerMeta.content) {
        return {
            [headerMeta.content]: tokenMeta.content
        };
    }

    const csrfInput = document.querySelector('input[name="_csrf"]');

    if (csrfInput && csrfInput.value) {
        return {
            "X-CSRF-TOKEN": csrfInput.value
        };
    }

    return {};
}
function escaparHtml(valor) {
    if (valor === null || valor === undefined) {
        return "";
    }

    return String(valor)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

document.addEventListener("DOMContentLoaded", () => {

    /* =====================================================
       ADAPTACION VISUAL Y ACCESIBILIDAD DEL MODULO
       No modifica datos, validaciones ni reglas de negocio.
    ====================================================== */

    function prepararTablaResponsive(tabla) {
        if (!tabla) return;

        const encabezados = Array.from(tabla.querySelectorAll("thead th"))
            .map(th => th.textContent.trim());

        tabla.querySelectorAll("tbody tr").forEach(fila => {
            Array.from(fila.children).forEach((celda, indice) => {
                if (celda.tagName !== "TD" || celda.hasAttribute("colspan")) return;
                celda.dataset.label = encabezados[indice] || "Dato";
            });
        });
    }

    function mejorarAccesibilidadProductos(raiz = document) {
        raiz.querySelectorAll?.("button[title]:not([aria-label])").forEach(boton => {
            boton.setAttribute("aria-label", boton.getAttribute("title"));
        });

        raiz.querySelectorAll?.(".producto-modal .btn-close:not([aria-label])").forEach(boton => {
            boton.setAttribute("aria-label", "Cerrar ventana");
        });

        raiz.querySelectorAll?.('[data-bs-toggle="modal"][data-bs-target]').forEach(control => {
            const destino = control.getAttribute("data-bs-target")?.replace(/^#/, "");
            if (destino) control.setAttribute("aria-controls", destino);
            if (!control.hasAttribute("aria-haspopup")) control.setAttribute("aria-haspopup", "dialog");
        });

        raiz.querySelectorAll?.("label:not([for])").forEach(label => {
            const campo = label.parentElement?.querySelector("input[id], select[id], textarea[id]");
            if (campo?.id) label.setAttribute("for", campo.id);
        });
    }

    const tablasResponsive = Array.from(document.querySelectorAll(
        "#tablaProductos, .productos-modal-table"
    ));

    tablasResponsive.forEach(prepararTablaResponsive);
    mejorarAccesibilidadProductos();

    const observadorTablasProductos = new MutationObserver(cambios => {
        const tablasActualizadas = new Set();

        cambios.forEach(cambio => {
            const tabla = cambio.target.closest?.("table");
            if (tabla?.matches("#tablaProductos, .productos-modal-table")) {
                tablasActualizadas.add(tabla);
            }
            cambio.addedNodes.forEach(nodo => {
                if (nodo.nodeType !== Node.ELEMENT_NODE) return;
                mejorarAccesibilidadProductos(nodo);
            });
        });

        tablasActualizadas.forEach(prepararTablaResponsive);
    });

    tablasResponsive.forEach(tabla => {
        const cuerpo = tabla.querySelector("tbody");
        if (cuerpo) observadorTablasProductos.observe(cuerpo, { childList: true, subtree: true });
    });

    const disparadoresModal = new WeakMap();
    const modalesPadre = new WeakMap();
    const selectorEnfocable = [
        "button:not([disabled])",
        "[href]",
        "input:not([disabled]):not([type='hidden'])",
        "select:not([disabled])",
        "textarea:not([disabled])",
        "[tabindex]:not([tabindex='-1'])"
    ].join(",");

    function esElementoEnfocableValido(elemento) {
        if (!(elemento instanceof HTMLElement) || !elemento.isConnected) return false;
        if (elemento.matches(":disabled") || elemento.getAttribute("aria-disabled") === "true") return false;

        const estilos = window.getComputedStyle(elemento);
        return estilos.display !== "none"
            && estilos.visibility !== "hidden"
            && elemento.getClientRects().length > 0;
    }

    function registrarElementoDisparador(modal, trigger) {
        if (!(modal instanceof HTMLElement) || !(trigger instanceof HTMLElement)) return;
        if (modal.contains(trigger)) return;

        disparadoresModal.set(modal, trigger);

        const modalPadre = trigger.closest(".producto-modal.show");
        if (modalPadre && modalPadre !== modal) {
            modalesPadre.set(modal, modalPadre);
        } else {
            modalesPadre.delete(modal);
        }
    }

    function desenfocarContenidoModal(modal) {
        const elementoActivo = document.activeElement;
        if (elementoActivo instanceof HTMLElement && modal.contains(elementoActivo)) {
            elementoActivo.blur();
        }
    }

    function restaurarFocoAlCerrar(modal) {
        const trigger = disparadoresModal.get(modal);
        const modalPadre = modalesPadre.get(modal);

        disparadoresModal.delete(modal);
        modalesPadre.delete(modal);

        window.requestAnimationFrame(() => {
            if (modalPadre?.classList.contains("show")) {
                if (trigger && modalPadre.contains(trigger) && esElementoEnfocableValido(trigger)) {
                    trigger.focus({ preventScroll: true });
                    return;
                }

                const primerControlPadre = Array.from(modalPadre.querySelectorAll(selectorEnfocable))
                    .find(esElementoEnfocableValido);
                primerControlPadre?.focus({ preventScroll: true });
                return;
            }

            if (esElementoEnfocableValido(trigger)) {
                trigger.focus({ preventScroll: true });
            }
        });
    }

    function mostrarModalSeguro(modal, trigger = document.activeElement) {
        if (!(modal instanceof HTMLElement)) return;
        registrarElementoDisparador(modal, trigger);
        bootstrap.Modal.getOrCreateInstance(modal).show(trigger);
    }

    function cerrarModalSeguro(modal) {
        if (!(modal instanceof HTMLElement) || !modal.classList.contains("show")) return;
        if (modal.dataset.cerrando === "true") return;

        desenfocarContenidoModal(modal);
        bootstrap.Modal.getOrCreateInstance(modal).hide();
    }

    document.addEventListener("show.bs.modal", event => {
        const modal = event.target;
        if (!modal.classList?.contains("producto-modal")) return;

        const trigger = event.relatedTarget instanceof HTMLElement
            ? event.relatedTarget
            : document.activeElement;
        registrarElementoDisparador(modal, trigger);
        delete modal.dataset.cerrando;
    });

    document.addEventListener("hide.bs.modal", event => {
        const modal = event.target;
        if (!modal.classList?.contains("producto-modal")) return;

        desenfocarContenidoModal(modal);
        modal.dataset.cerrando = "true";
    });

    document.addEventListener("hidden.bs.modal", event => {
        const modal = event.target;
        if (!modal.classList?.contains("producto-modal")) return;

        delete modal.dataset.cerrando;
        restaurarFocoAlCerrar(modal);
    });

    document.addEventListener("hidePrevented.bs.modal", event => {
        const modal = event.target;
        if (modal.classList?.contains("producto-modal")) {
            delete modal.dataset.cerrando;
        }
    });

    /* =====================================================
       REFERENCIAS PRINCIPALES
    ====================================================== */

    const modalProductoEl = document.getElementById("modalProducto");
    const modalEstadoEl = document.getElementById("modalEstadoProducto");
    const modalEliminarEl = document.getElementById("modalConfirmEliminar");
    const modalDetalleEl = document.getElementById("modalDetalleProducto");

    const modalProducto = modalProductoEl ? bootstrap.Modal.getOrCreateInstance(modalProductoEl) : null;
    const modalEstado = modalEstadoEl ? bootstrap.Modal.getOrCreateInstance(modalEstadoEl) : null;
    const modalEliminar = modalEliminarEl ? bootstrap.Modal.getOrCreateInstance(modalEliminarEl) : null;
    const modalDetalle = modalDetalleEl ? bootstrap.Modal.getOrCreateInstance(modalDetalleEl) : null;

    const formProducto = document.getElementById("formProducto");

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    let idEstadoSeleccionado = null;
    let nuevoEstadoSeleccionado = null;
    let formEliminarSeleccionado = null;

    const textoEstadoProducto = document.getElementById("textoEstadoProducto");
    const btnConfirmarEstado = document.getElementById("btnConfirmarEstado");

    const textoEliminarProducto = document.getElementById("textoEliminarProducto");
    const btnConfirmarEliminar = document.getElementById("btnConfirmarEliminar");

    const btnNuevoProducto = document.getElementById("btnNuevoProducto");

    const precioVentaInput = document.getElementById("precioVenta");
    const porcentajeGananciaInput = document.getElementById("porcentajeGanancia");
    const ultimoPrecioCompraInput = document.getElementById("ultimoPrecioCompra");
    const errorPrecioVenta = document.getElementById("errorPrecioVenta");
    const descuentoMaximoInput = document.getElementById("descuentoMaximo");
    const contenedorAtributosProducto = document.getElementById("contenedorAtributosProducto");
    const galeriaFotosProducto = document.getElementById("galeriaFotosProducto");
    const inputGaleriaProductoFoto = document.getElementById("inputGaleriaProductoFoto");
    const textoGaleriaProductoFoto = document.getElementById("textoGaleriaProductoFoto");
    const errorGaleriaProductoFoto = document.getElementById("errorGaleriaProductoFoto");
    const fotoPortadaTemporalInput = document.getElementById("fotoPortadaTemporal");
    let atributosValoresEdicion = {};
    let fotosTemporalesProducto = [];
    let secuenciaFotoTemporal = 0;
    let fotosActualesProducto = [];

    function escaparHtml(valor) {
        if (valor === null || valor === undefined) {
            return "";
        }

        return String(valor)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    document.addEventListener("click", event => {
        const btnPortada = event.target.closest(".btn-foto-portada-producto");
        const btnEliminar = event.target.closest(".btn-eliminar-foto-producto");

        if (!btnPortada && !btnEliminar) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const btn = btnPortada || btnEliminar;
        const idProductoFoto = btn.dataset.idProductoFoto;
        const galeria = document.getElementById("galeriaFotosProducto");
        const idProductoActual = galeria?.dataset.idProducto || document.getElementById("idProducto")?.value;

        if (!idProductoFoto || idProductoFoto === "undefined" || idProductoFoto === "null") {
            mostrarErrorGaleriaProducto("No se pudo identificar la foto seleccionada.");
            return;
        }

        if (!idProductoActual) {
            mostrarErrorGaleriaProducto("No se pudo identificar el producto abierto.");
            return;
        }

        if (btnEliminar && !confirm("Eliminar esta imagen del producto?")) {
            return;
        }

        const accion = btnPortada ? "portada" : "eliminar";
        const mensajeError = btnPortada ? "No se pudo marcar la portada." : "No se pudo eliminar la imagen.";

        fetch(`/interno/productos/fotos/${idProductoFoto}/${accion}`, {
            method: "POST",
            headers: {
                ...obtenerHeadersCsrf()
            }
        })
            .then(async response => {
                const body = await response.json();
                if (!response.ok || !body.ok) {
                    throw new Error(body.mensaje || mensajeError);
                }

                if (btnPortada && body.fotos) {
                    fotosActualesProducto = body.fotos;
                    pintarGaleriaFotosProducto(idProductoActual, fotosActualesProducto);
                } else {
                    cargarGaleriaFotosProducto(idProductoActual);
                }
            })
            .catch(error => mostrarErrorGaleriaProducto(error.message || mensajeError));
    });

  const controlInventarioInput = document.getElementById("controlInventario");
  const nombreUnidadInventarioInput = document.getElementById("nombreUnidadInventario");

  const formaVentaInput = document.getElementById("formaVenta");
  const nombreUnidadVentaInput = document.getElementById("nombreUnidadVenta");
  const abreviaturaUnidadVentaInput = document.getElementById("abreviaturaUnidadVenta");

 const ventaPorMedidaInput = document.getElementById("ventaPorMedida");
 const tipoMedidaBaseInput = document.getElementById("tipoMedidaBase");

 const grupoTipoMedidaBase = document.getElementById("grupoTipoMedidaBase");
 const grupoLargoBase = document.getElementById("grupoLargoBase");
 const grupoAnchoBase = document.getElementById("grupoAnchoBase");
 const grupoMedidaVendibleBase = document.getElementById("grupoMedidaVendibleBase");
 const grupoResumenMedidaBase = document.getElementById("grupoResumenMedidaBase");
 const grupoDescripcionMedidaBase = document.getElementById("grupoDescripcionMedidaBase");

  const largoBaseInput = document.getElementById("largoBase");
  const anchoBaseInput = document.getElementById("anchoBase");
    const medidaTotalBaseInput = document.getElementById("medidaTotalBase");
    const medidaVendibleBaseInput = document.getElementById("medidaVendibleBase");
    const descripcionMedidaBaseInput = document.getElementById("descripcionMedidaBase");

    const textoMedidaReal = document.getElementById("textoMedidaReal");
    const textoMedidaVendible = document.getElementById("textoMedidaVendible");
    const unidadMedidaReal = document.getElementById("unidadMedidaReal");
    const unidadMedidaVendible = document.getElementById("unidadMedidaVendible");
    const labelLargoBase = document.getElementById("labelLargoBase");
    const ayudaLargoBase = document.getElementById("ayudaLargoBase");

    /* =====================================================
       TOOLTIP BOOTSTRAP
    ====================================================== */

    function activarTooltips() {
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => {
            new bootstrap.Tooltip(el);
        });
    }

    activarTooltips();

    /* =====================================================
       UNIDAD TEMPORAL SEGÚN TIPO ACTUAL
       Luego esto se reemplazará por unidades configurables.
    ====================================================== */

   function aplicarUnidadPorTipo() {
       /*
          Antes esta función bloqueaba la unidad según el tipo.
          Ahora la unidad es configurable y debe poder elegirse desde el formulario.
          Por eso ya no desactivamos idUnidad.
       */
       const unidadSelect = document.getElementById("idUnidad");

       if (unidadSelect) {
           unidadSelect.disabled = false;
       }
   }



    window.generarCodigo = function () {
       const categoriaSelect = document.getElementById("idCategoria");
       const codigoInput = document.getElementById("codigoInterno");

       if (!categoriaSelect || !codigoInput) return;

       const idCategoria = categoriaSelect.value;

       if (!idCategoria) {
           codigoInput.value = "";
           return;
       }

       fetch(`/interno/productos/api/codigo?categoria=${encodeURIComponent(idCategoria)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("No se pudo generar el código.");
                }
                return response.text();
            })
            .then(codigo => {
                codigoInput.value = codigo;
            })
            .catch(error => {
                console.error("Error generando código:", error);
            });
    };

    function cargarAtributosPorCategoria(idCategoria, valores = {}) {
        if (!contenedorAtributosProducto) return;

        contenedorAtributosProducto.innerHTML = `
            <div class="col-12">
                <div class="alert alert-light border mb-0 small text-muted">
                    Cargando atributos técnicos...
                </div>
            </div>
        `;

        if (!idCategoria) {
            contenedorAtributosProducto.innerHTML = `
                <div class="col-12">
                    <div class="alert alert-light border mb-0 small text-muted">
                        Selecciona una categoría para mostrar los atributos técnicos.
                    </div>
                </div>
            `;
            return;
        }

        fetch(`/interno/productos/api/atributos?idCategoria=${encodeURIComponent(idCategoria)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("No se pudieron cargar los atributos.");
                }
                return response.json();
            })
            .then(atributos => {

                if (!atributos || atributos.length === 0) {
                    contenedorAtributosProducto.innerHTML = `
                        <div class="col-12">
                            <div class="alert alert-warning border-0 mb-0 small">
                                Esta categoría todavía no tiene atributos configurados.
                            </div>
                        </div>
                    `;
                    return;
                }

                contenedorAtributosProducto.innerHTML = "";

                atributos.forEach(attr => {
                    const idAtributo = attr.idAtributo;
                    const nombre = attr.nombreAtributo || "Atributo";
                    const tipoDato = attr.tipoDato || "TEXTO";
                    const valor = valores[idAtributo] || "";

                    let inputType = "text";
                    let step = "";

                    if (tipoDato === "NUMERO") {
                        inputType = "number";
                        step = "1";
                    } else if (tipoDato === "DECIMAL") {
                        inputType = "number";
                        step = "0.01";
                    }

                    const col = document.createElement("div");
                    col.className = "col-12 col-md-4";

                    col.innerHTML = `
                        <label class="form-label fw-semibold">${nombre}</label>
                        <input type="${inputType}"
                               ${step ? `step="${step}"` : ""}
                               class="form-control"
                               name="atributo_${idAtributo}"
                               value="${valor}"
                               placeholder="Ingrese ${nombre.toLowerCase()}">
                    `;

                    contenedorAtributosProducto.appendChild(col);
                });
            })
            .catch(error => {
                console.error(error);

                contenedorAtributosProducto.innerHTML = `
                    <div class="col-12">
                        <div class="alert alert-danger border-0 mb-0 small">
                            No se pudieron cargar los atributos técnicos.
                        </div>
                    </div>
                `;
            });
    }

    /* =====================================================
       NUEVO FLUJO: FORMA DE VENTA DEL PRODUCTO
    ====================================================== */

   function actualizarMedidasFisicasProducto() {
       const tieneMedidas = ventaPorMedidaInput?.value === "true";
       const tipo = tipoMedidaBaseInput?.value || "";

       if (controlInventarioInput) {
           controlInventarioInput.value = "CANTIDAD";
       }

       if (formaVentaInput) {
           formaVentaInput.value = "CANTIDAD";
       }

       if (nombreUnidadVentaInput && !nombreUnidadVentaInput.value) {
           nombreUnidadVentaInput.value = "Unidad";
       }

       if (abreviaturaUnidadVentaInput && !abreviaturaUnidadVentaInput.value) {
           abreviaturaUnidadVentaInput.value = "und";
       }

       [
           grupoTipoMedidaBase,
           grupoLargoBase,
           grupoAnchoBase,
           grupoMedidaVendibleBase,
           grupoResumenMedidaBase,
           grupoDescripcionMedidaBase
       ].forEach(grupo => {
           if (grupo) grupo.classList.toggle("d-none", !tieneMedidas);
       });

       if (!tieneMedidas) {
           if (tipoMedidaBaseInput) tipoMedidaBaseInput.value = "";
           if (largoBaseInput) largoBaseInput.value = "";
           if (anchoBaseInput) anchoBaseInput.value = "";
           if (medidaTotalBaseInput) medidaTotalBaseInput.value = "";
           if (medidaVendibleBaseInput) medidaVendibleBaseInput.value = "";
           if (descripcionMedidaBaseInput) descripcionMedidaBaseInput.value = "";

           actualizarTextosMedida(0, 0, "");
           return;
       }

       if (grupoAnchoBase) {
           grupoAnchoBase.classList.toggle("d-none", tipo !== "AREA");
       }

       if (tipo !== "AREA" && anchoBaseInput) {
           anchoBaseInput.value = "";
       }

       if (tipo === "AREA") {
           if (unidadMedidaReal) unidadMedidaReal.textContent = "m²";
           if (unidadMedidaVendible) unidadMedidaVendible.textContent = "pie²";

           if (labelLargoBase) labelLargoBase.textContent = "Largo real";
           if (ayudaLargoBase) ayudaLargoBase.textContent = "Ingresa el largo real de la plancha.";
       }

       if (tipo === "LONGITUD") {
           if (unidadMedidaReal) unidadMedidaReal.textContent = "m";
           if (unidadMedidaVendible) unidadMedidaVendible.textContent = "ml";

           if (labelLargoBase) labelLargoBase.textContent = "Largo real";
           if (ayudaLargoBase) ayudaLargoBase.textContent = "Ingresa el largo real de la varilla o rollo.";
       }

       calcularMedidaFisicaProducto();
   }

   function calcularMedidaFisicaProducto() {
       const tieneMedidas = ventaPorMedidaInput?.value === "true";
       const tipo = tipoMedidaBaseInput?.value || "";

       if (!tieneMedidas || !tipo) {
           if (medidaTotalBaseInput) medidaTotalBaseInput.value = "";
           if (medidaVendibleBaseInput) medidaVendibleBaseInput.value = "";
           actualizarTextosMedida(0, 0, "");
           return;
       }

       const largo = Number(largoBaseInput?.value || 0);
       const ancho = Number(anchoBaseInput?.value || 0);

       let medidaReal = 0;
       let medidaVendible = 0;
       let unidadReal = "";
       let unidadVendible = "";

       if (tipo === "AREA") {
           unidadReal = "m²";
           unidadVendible = "pie²";

           if (largo > 0 && ancho > 0) {
               medidaReal = largo * ancho;
               medidaVendible = Math.floor(medidaReal * 10.7639);
           }
       }

       if (tipo === "LONGITUD") {
           unidadReal = "m";
           unidadVendible = "ml";

           if (largo > 0) {
               medidaReal = largo;
               medidaVendible = Math.floor(medidaReal);
           }
       }

       if (medidaTotalBaseInput) {
           medidaTotalBaseInput.value = medidaReal > 0 ? medidaReal.toFixed(6) : "";
       }

       if (medidaVendibleBaseInput) {
           medidaVendibleBaseInput.value = medidaVendible > 0 ? medidaVendible : "";
       }

       actualizarTextosMedida(medidaReal, medidaVendible, unidadReal, unidadVendible);
       generarDescripcionMedidaFisicaAutomatica(tipo, medidaVendible, unidadVendible);
   }

   function actualizarTextosMedida(medidaReal, medidaVendible, unidadReal, unidadVendible = unidadReal) {
       if (textoMedidaReal) {
           textoMedidaReal.textContent = medidaReal > 0 ? medidaReal.toFixed(2) : "0.00";
       }

       if (textoMedidaVendible) {
           textoMedidaVendible.textContent = medidaVendible > 0 ? medidaVendible : "0";
       }

       if (unidadMedidaReal) {
           unidadMedidaReal.textContent = unidadReal || "";
       }

       if (unidadMedidaVendible) {
           unidadMedidaVendible.textContent = unidadVendible || "";
       }
   }

   function generarDescripcionMedidaFisicaAutomatica(tipo, medidaVendible, unidadVendible) {
       if (!descripcionMedidaBaseInput) return;

       const descripcionActual = descripcionMedidaBaseInput.value?.trim();

       if (descripcionActual) return;

       if (!tipo || medidaVendible <= 0) return;

       const unidadInventario = nombreUnidadInventarioInput?.value || "unidad física";

       descripcionMedidaBaseInput.value =
           `1 ${unidadInventario.toLowerCase()} equivale a ${medidaVendible} ${unidadVendible} vendibles.`;
   }

   function inicializarEventosMedidasFisicas() {
       if (ventaPorMedidaInput) {
           ventaPorMedidaInput.addEventListener("change", () => {
               if (descripcionMedidaBaseInput) descripcionMedidaBaseInput.value = "";
               actualizarMedidasFisicasProducto();
           });
       }

       if (tipoMedidaBaseInput) {
           tipoMedidaBaseInput.addEventListener("change", () => {
               if (descripcionMedidaBaseInput) descripcionMedidaBaseInput.value = "";
               actualizarMedidasFisicasProducto();
           });
       }

       if (nombreUnidadInventarioInput) {
           nombreUnidadInventarioInput.addEventListener("input", () => {
               if (descripcionMedidaBaseInput) descripcionMedidaBaseInput.value = "";
               actualizarMedidasFisicasProducto();
           });
       }

       if (largoBaseInput) {
           largoBaseInput.addEventListener("input", () => {
               if (descripcionMedidaBaseInput) descripcionMedidaBaseInput.value = "";
               calcularMedidaFisicaProducto();
           });
       }

       if (anchoBaseInput) {
           anchoBaseInput.addEventListener("input", () => {
               if (descripcionMedidaBaseInput) descripcionMedidaBaseInput.value = "";
               calcularMedidaFisicaProducto();
           });
       }

       if (medidaVendibleBaseInput) {
           medidaVendibleBaseInput.addEventListener("input", () => {
               if (descripcionMedidaBaseInput) descripcionMedidaBaseInput.value = "";
               calcularMedidaFisicaProducto();
           });
       }
   }

   function cargarMedidasProductoEnFormulario(p) {
       asignarValor("controlInventario", "CANTIDAD");
       asignarValor("nombreUnidadInventario", p.nombreUnidadInventario ?? "Unidad");

       asignarValor("formaVenta", "CANTIDAD");
       asignarValor("nombreUnidadVenta", p.nombreUnidadVenta ?? "Unidad");
       asignarValor("abreviaturaUnidadVenta", p.abreviaturaUnidadVenta ?? "und");

       asignarValor("ventaPorMedida", p.ventaPorMedida === true ? "true" : "false");
       asignarValor("tipoMedidaBase", p.tipoMedidaBase ?? "");
       asignarValor("largoBase", p.largoBase ?? "");
       asignarValor("anchoBase", p.anchoBase ?? "");
       asignarValor("medidaTotalBase", p.medidaTotalBase ?? "");
       asignarValor("medidaVendibleBase", p.medidaVendibleBase ?? "");
       asignarValor("descripcionMedidaBase", p.descripcionMedidaBase ?? "");

       actualizarMedidasFisicasProducto();

       if (p.medidaTotalBase != null || p.medidaVendibleBase != null) {
           const tipo = p.tipoMedidaBase || "";
           const unidadReal = tipo === "LONGITUD" ? "m" : "m²";
           const unidadVendible = tipo === "LONGITUD" ? "ml" : "pie²";

           actualizarTextosMedida(
               Number(p.medidaTotalBase || 0),
               Number(p.medidaVendibleBase || 0),
               unidadReal,
               unidadVendible
           );
       }
   }

    /* =====================================================
       LIMPIAR FORMULARIO
    ====================================================== */

    function limpiarFormularioProducto() {
        if (!formProducto) return;

        formProducto.reset();
        formProducto.classList.remove("was-validated");

        const titulo = document.getElementById("tituloModalProducto");
        const idProducto = document.getElementById("idProducto");
        const codigoInterno = document.getElementById("codigoInterno");
        const stockActual = document.getElementById("stockActual");
        const previewImg = document.getElementById("previewImg");
        const previewIcon = document.getElementById("previewIcon");
        const precioCompraInfo = document.getElementById("precioCompraInfo");

        if (titulo) {
            titulo.innerHTML = `<i class="bi bi-box-seam me-1"></i> Nuevo producto`;
        }

        if (idProducto) idProducto.value = "";
        if (codigoInterno) codigoInterno.value = "";
        if (stockActual) stockActual.value = 0;

        asignarValor("idCategoria", "");
        asignarValor("idSubcategoria", "");
        asignarValor("idMarca", "");
        asignarValor("idUnidad", "");
        asignarValor("modeloProducto", "");
        asignarValor("descripcionProducto", "");

       asignarValor("controlInventario", "CANTIDAD");
       asignarValor("nombreUnidadInventario", "Unidad");
       asignarValor("formaVenta", "CANTIDAD");
       asignarValor("nombreUnidadVenta", "Unidad");
       asignarValor("abreviaturaUnidadVenta", "und");

       asignarValor("ventaPorMedida", "false");
       asignarValor("tipoMedidaBase", "");
       asignarValor("largoBase", "");
       asignarValor("anchoBase", "");
       asignarValor("medidaTotalBase", "");
       asignarValor("medidaVendibleBase", "");
       asignarValor("descripcionMedidaBase", "");

      actualizarMedidasFisicasProducto();

      if (ultimoPrecioCompraInput) ultimoPrecioCompraInput.value = "";
      if (porcentajeGananciaInput) porcentajeGananciaInput.value = "";
      if (precioVentaInput) precioVentaInput.value = "0";
      if (descuentoMaximoInput) descuentoMaximoInput.value = "0";

        if (precioCompraInfo) {
            precioCompraInfo.textContent = "";
        }

        limpiarErrorPrecioVenta();

        if (previewImg) {
            previewImg.src = "";
            previewImg.style.display = "none";
        }

        if (previewIcon) {
            previewIcon.style.display = "block";
        }

        atributosValoresEdicion = {};
        cargarAtributosPorCategoria("");
        reiniciarGaleriaProducto();

        aplicarUnidadPorTipo();
    }

    /* =====================================================
       ABRIR MODAL NUEVO PRODUCTO
    ====================================================== */

    if (btnNuevoProducto && modalProducto) {
        btnNuevoProducto.addEventListener("click", event => {
            event.preventDefault();
            limpiarFormularioProducto();
            mostrarModalSeguro(modalProductoEl, btnNuevoProducto);
        });
    }

    if (modalProductoEl) {
        modalProductoEl.addEventListener("hidden.bs.modal", () => {
            limpiarFotosPendientesProducto();
        });
    }

    /* =====================================================
       PREVISUALIZAR IMAGEN
    ====================================================== */

    window.previewImagen = function (event) {
        const img = document.getElementById("previewImg");
        const icon = document.getElementById("previewIcon");

        if (!img || !event.target.files || !event.target.files.length) return;

        const archivo = event.target.files[0];

        const tiposPermitidos = ["image/jpeg", "image/png", "image/webp"];

        if (!tiposPermitidos.includes(archivo.type)) {
            event.target.value = "";

            if (typeof Swal !== "undefined") {
                Swal.fire({
                    icon: "warning",
                    title: "Imagen no válida",
                    text: "Solo se permiten imágenes JPG, PNG o WEBP."
                });
            } else {
                alert("Solo se permiten imágenes JPG, PNG o WEBP.");
            }

            return;
        }

        img.src = URL.createObjectURL(archivo);
        img.style.display = "block";

        if (icon) {
            icon.style.display = "none";
        }
    };

    function reiniciarGaleriaProducto() {
        limpiarFotosPendientesProducto({ repintar: false, ocultarError: false });
        fotosActualesProducto = [];

        if (galeriaFotosProducto) {
            galeriaFotosProducto.innerHTML = "";
        }

        if (inputGaleriaProductoFoto) {
            inputGaleriaProductoFoto.value = "";
            inputGaleriaProductoFoto.disabled = false;
            inputGaleriaProductoFoto.multiple = true;
            delete inputGaleriaProductoFoto.dataset.idProducto;
        }

        if (textoGaleriaProductoFoto) {
            textoGaleriaProductoFoto.textContent = "Puedes seleccionar hasta 3 imagenes antes de guardar el producto.";
        }

        ocultarErrorGaleriaProducto();
        pintarGaleriaFotosTemporales();
    }

    function limpiarFotosPendientesProducto(opciones = {}) {
        const { repintar = true, ocultarError = true } = opciones;

        fotosTemporalesProducto.forEach(foto => URL.revokeObjectURL(foto.url));
        fotosTemporalesProducto = [];
        secuenciaFotoTemporal = 0;

        if (inputGaleriaProductoFoto) {
            inputGaleriaProductoFoto.value = "";
            inputGaleriaProductoFoto.disabled = fotosActualesProducto.length >= 3;
            inputGaleriaProductoFoto.multiple = true;
            delete inputGaleriaProductoFoto.dataset.idProducto;
        }

        if (fotoPortadaTemporalInput) {
            fotoPortadaTemporalInput.value = "-1";
        }

        if (ocultarError) {
            ocultarErrorGaleriaProducto();
        }

        if (repintar) {
            pintarFotosSegunModoActual();
        }
    }

    function cargarGaleriaFotosProducto(idProducto) {
        if (!idProducto || !galeriaFotosProducto) {
            reiniciarGaleriaProducto();
            return;
        }

        galeriaFotosProducto.innerHTML = `
            <div class="col-12 text-muted small">
                Cargando fotos...
            </div>
        `;

        fetch(`/interno/productos/${idProducto}/fotos`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("No se pudieron cargar las fotos.");
                }

                return response.json();
            })
            .then(fotos => {
                fotosActualesProducto = fotos || [];
                pintarGaleriaFotosProducto(idProducto, fotosActualesProducto);
            })
            .catch(error => {
                mostrarErrorGaleriaProducto(error.message);
                fotosActualesProducto = [];
                pintarGaleriaFotosProducto(idProducto, fotosActualesProducto);
            });
    }

    function pintarGaleriaFotosProducto(idProducto, fotos) {
        if (!galeriaFotosProducto) return;

        galeriaFotosProducto.dataset.idProducto = idProducto || "";

        const fotosSeguras = fotos || [];
        const totalFotos = fotosSeguras.length + fotosTemporalesProducto.length;

        if (totalFotos === 0) {
            galeriaFotosProducto.innerHTML = `
                <div class="col-12 text-muted small">
                    Este producto todavía no tiene fotos.
                </div>
            `;
        } else {
            galeriaFotosProducto.innerHTML = fotosSeguras.map(foto => `
                <div class="col-12 col-md-4">
                    <div class="border rounded-4 overflow-hidden bg-white h-100">
                        <div class="bg-light d-flex align-items-center justify-content-center"
                             style="height:150px;">
                            <img src="/uploads/productos/${escaparHtml(foto.nombreArchivo || "")}"
                                 alt="Foto producto"
                                 style="width:100%;height:100%;object-fit:cover;">
                        </div>
                        <div class="p-2">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                ${
                                    foto.portada
                                        ? `<span class="badge text-bg-primary">Portada</span>`
                                        : `<button type="button"
                                                   class="btn btn-sm btn-outline-primary btn-foto-portada-producto"
                                                   data-id-producto-foto="${foto.idProductoFoto || ""}">
                                               Marcar portada
                                           </button>`
                                }
                                <button type="button"
                                        class="btn btn-sm btn-outline-danger btn-eliminar-foto-producto"
                                        data-id-producto-foto="${foto.idProductoFoto || ""}">
                                    Eliminar
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `).join("");

            galeriaFotosProducto.innerHTML += construirHtmlFotosTemporales();
        }

        if (inputGaleriaProductoFoto) {
            inputGaleriaProductoFoto.value = "";
            inputGaleriaProductoFoto.disabled = totalFotos >= 3;
            inputGaleriaProductoFoto.multiple = true;
            delete inputGaleriaProductoFoto.dataset.idProducto;
        }

        if (textoGaleriaProductoFoto) {
            textoGaleriaProductoFoto.textContent = totalFotos >= 3
                ? "Máximo 3 imágenes por producto."
                : `Puedes agregar ${3 - totalFotos} imagen(es) mas.`;
        }

        activarAccionesGaleriaProducto(idProducto);
        sincronizarFotosTemporalesEnInput();
        activarAccionesFotosTemporales();
    }

    function activarAccionesGaleriaProducto(idProducto) {
        // Las acciones de fotos existentes se manejan con listener global en document.
    }

    function construirHtmlFotosTemporales() {
        return fotosTemporalesProducto.map(foto => `
            <div class="col-12 col-md-4">
                <div class="border border-primary rounded-4 overflow-hidden bg-white h-100">
                    <div class="bg-light d-flex align-items-center justify-content-center"
                         style="height:150px;">
                        <img src="${foto.url}"
                             alt="Foto producto"
                             style="width:100%;height:100%;object-fit:cover;">
                    </div>
                    <div class="p-2">
                        <div class="small text-truncate mb-2" title="${escaparHtml(foto.file.name)}">
                            ${escaparHtml(foto.file.name)}
                        </div>
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div class="d-flex flex-wrap gap-1">
                                <span class="badge text-bg-secondary">Pendiente</span>
                                ${
                                    foto.portada
                                        ? `<span class="badge text-bg-primary">Portada</span>`
                                        : `<button type="button"
                                                   class="btn btn-sm btn-outline-primary btn-foto-temporal-portada"
                                                   data-id="${foto.id}">
                                               Marcar portada
                                           </button>`
                                }
                            </div>
                            <button type="button"
                                    class="btn btn-sm btn-outline-danger btn-quitar-foto-temporal"
                                    data-id="${foto.id}">
                                Quitar
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `).join("");
    }

    function pintarGaleriaFotosTemporales() {
        if (!galeriaFotosProducto) return;

        if (fotosTemporalesProducto.length === 0) {
            galeriaFotosProducto.innerHTML = `
                <div class="col-12 text-muted small">
                    Selecciona imagenes para previsualizarlas antes de guardar.
                </div>
            `;
        } else {
            galeriaFotosProducto.innerHTML = construirHtmlFotosTemporales();
        }

        if (inputGaleriaProductoFoto) {
            inputGaleriaProductoFoto.value = "";
            inputGaleriaProductoFoto.disabled = fotosTemporalesProducto.length >= 3;
            inputGaleriaProductoFoto.multiple = true;
            delete inputGaleriaProductoFoto.dataset.idProducto;
        }

        if (textoGaleriaProductoFoto) {
            textoGaleriaProductoFoto.textContent = fotosTemporalesProducto.length >= 3
                ? "Maximo 3 imagenes por producto."
                : `Puedes agregar ${3 - fotosTemporalesProducto.length} imagen(es) mas.`;
        }

        sincronizarFotosTemporalesEnInput();
        activarAccionesFotosTemporales();
    }

    function activarAccionesFotosTemporales() {
        galeriaFotosProducto?.querySelectorAll(".btn-foto-temporal-portada").forEach(btn => {
            btn.addEventListener("click", () => {
                const id = Number(btn.dataset.id);
                fotosTemporalesProducto = fotosTemporalesProducto.map(foto => ({
                    ...foto,
                    portada: foto.id === id
                }));
                pintarFotosSegunModoActual();
            });
        });

        galeriaFotosProducto?.querySelectorAll(".btn-quitar-foto-temporal").forEach(btn => {
            btn.addEventListener("click", () => {
                const id = Number(btn.dataset.id);
                const removida = fotosTemporalesProducto.find(foto => foto.id === id);

                if (removida) {
                    URL.revokeObjectURL(removida.url);
                }

                fotosTemporalesProducto = fotosTemporalesProducto.filter(foto => foto.id !== id);

                if (fotosTemporalesProducto.length > 0 && !fotosTemporalesProducto.some(foto => foto.portada)) {
                    fotosTemporalesProducto[0].portada = true;
                }

                pintarFotosSegunModoActual();
            });
        });
    }

    inputGaleriaProductoFoto?.addEventListener("change", () => {
        ocultarErrorGaleriaProducto();
        agregarFotosTemporales(Array.from(inputGaleriaProductoFoto.files || []));
    });

    function agregarFotosTemporales(archivos) {
        if (!archivos || archivos.length === 0) return;

        const disponibles = 3 - fotosActualesProducto.length - fotosTemporalesProducto.length;

        if (disponibles <= 0) {
            mostrarErrorGaleriaProducto("Maximo 3 imagenes por producto.");
            inputGaleriaProductoFoto.value = "";
            return;
        }

        const seleccionadas = archivos.slice(0, disponibles);

        for (const archivo of seleccionadas) {
            if (!esImagenProductoValida(archivo)) {
                mostrarErrorGaleriaProducto("Solo se permiten imagenes JPG, PNG o WEBP.");
                inputGaleriaProductoFoto.value = "";
                return;
            }
        }

        seleccionadas.forEach(archivo => {
            fotosTemporalesProducto.push({
                id: ++secuenciaFotoTemporal,
                file: archivo,
                url: URL.createObjectURL(archivo),
                portada: debeSerPortadaTemporal()
            });
        });

        if (archivos.length > disponibles) {
            mostrarErrorGaleriaProducto("Maximo 3 imagenes por producto.");
        }

        pintarFotosSegunModoActual();
    }

    function debeSerPortadaTemporal() {
        return !fotosActualesProducto.some(foto => foto.portada)
            && !fotosTemporalesProducto.some(foto => foto.portada);
    }

    function pintarFotosSegunModoActual() {
        const idProducto = document.getElementById("idProducto")?.value;

        if (idProducto) {
            pintarGaleriaFotosProducto(idProducto, fotosActualesProducto);
            return;
        }

        pintarGaleriaFotosTemporales();
    }

    function sincronizarFotosTemporalesEnInput() {
        if (!inputGaleriaProductoFoto) return;

        const dataTransfer = new DataTransfer();
        fotosTemporalesProducto.forEach(foto => dataTransfer.items.add(foto.file));
        inputGaleriaProductoFoto.files = dataTransfer.files;

        if (fotoPortadaTemporalInput) {
            const indicePortada = fotosTemporalesProducto.findIndex(foto => foto.portada);
            fotoPortadaTemporalInput.value = String(indicePortada);
        }
    }

    function esImagenProductoValida(archivo) {
        if (!archivo) return false;

        const extension = archivo.name?.split(".").pop()?.toLowerCase();
        return ["jpg", "jpeg", "png", "webp"].includes(extension);
    }

    function mostrarErrorGaleriaProducto(mensaje) {
        if (!errorGaleriaProductoFoto) return;
        errorGaleriaProductoFoto.textContent = mensaje || "No se pudo procesar la imagen.";
        errorGaleriaProductoFoto.classList.remove("d-none");
    }

    function ocultarErrorGaleriaProducto() {
        if (!errorGaleriaProductoFoto) return;
        errorGaleriaProductoFoto.textContent = "";
        errorGaleriaProductoFoto.classList.add("d-none");
    }


    /* =====================================================
       VER DETALLE PRODUCTO
    ====================================================== */

    function abrirDetalleProducto(id, trigger) {
        if (!modalDetalle || !id) return;

        fetch(`/interno/productos/api/${id}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("No se pudo cargar el detalle del producto.");
                }

                return response.json();
            })
            .then(data => {
                const p = data.producto;
                const atributos = data.atributosDetalle || {};

                if (!p) {
                    throw new Error("La respuesta no contiene producto.");
                }

                cargarDatosDetalleProducto(p);
                cargarAtributosDetalleProducto(atributos);

                mostrarModalSeguro(modalDetalleEl, trigger);
            })
            .catch(error => {
                console.error(error);

                if (typeof Swal !== "undefined") {
                    Swal.fire({
                        icon: "error",
                        title: "No se pudo cargar",
                        text: "No se pudo cargar el detalle del producto."
                    });
                } else {
                    alert("No se pudo cargar el detalle del producto.");
                }
            });
    }

    function cargarDatosDetalleProducto(p) {
        asignarTexto("detalleIdProducto", p.idProducto ?? "-");
        asignarTexto("detalleNombreProducto", p.nombreProducto ?? "Sin nombre");
        asignarTexto("detalleCodigoProducto", p.codigoInterno ?? "-");

        asignarTexto("detalleCategoriaProducto", p.nombreCategoria ?? "Sin categoría");
        asignarTexto("detalleSubcategoriaProducto", p.nombreSubcategoria ?? "Sin subcategoría");
        asignarTexto("detalleMarcaProducto", p.nombreMarca ?? "Sin marca");

       const unidadInventario = p.nombreUnidadInventario || "Unidad";
       const tieneMedida = p.ventaPorMedida === true;
       const tipoMedida = p.tipoMedidaBase === "LONGITUD" ? "Longitud" :
                          p.tipoMedidaBase === "AREA" ? "Área" : "Sin medida";

       const unidadVendible = p.tipoMedidaBase === "LONGITUD" ? "ml" :
                              p.tipoMedidaBase === "AREA" ? "pie²" : "";

       const detalleFisico = tieneMedida
           ? `Unidad física: ${unidadInventario} | Tipo medida: ${tipoMedida} | Largo: ${p.largoBase ?? "-"} | Ancho: ${p.anchoBase ?? "-"} | Medida real: ${p.medidaTotalBase ?? "-"} | Vendible: ${p.medidaVendibleBase ?? "-"} ${unidadVendible}`
           : `Unidad física: ${unidadInventario} | Sin medidas físicas`;

       asignarTexto("detalleUnidadProducto", detalleFisico);
       asignarTexto("detalleDescripcionMedidaProducto", p.descripcionMedidaBase || "Sin equivalencia técnica registrada.");
       asignarTexto("detalleModeloProducto", p.modeloProducto || "Sin modelo");
       asignarTexto("detalleDimensionesProducto", p.dimensiones || "Sin dimensiones");
       asignarTexto("detalleUnidadVentaProducto", p.nombreUnidadVenta || "Unidad");
       asignarTexto("detalleAbreviaturaVentaProducto", p.abreviaturaUnidadVenta || "und");

        const precio = Number(p.precioUnitario || 0);
        asignarTexto(
            "detallePrecioProducto",
            precio > 0 ? `S/ ${precio.toFixed(2)}` : "Pendiente"
        );

        const descuento = Number(p.descuentoMaximo || 0);
        asignarTexto(
            "detalleDescuentoProducto",
            descuento > 0 ? `S/ ${descuento.toFixed(2)}` : "Sin descuento"
        );

        asignarTexto(
            "detalleDescripcionProducto",
            p.descripcionProducto && p.descripcionProducto.trim() !== ""
                ? p.descripcionProducto
                : "Sin descripción registrada."
        );

        cargarEstadoDetalleProducto(p.idEstadoProducto);
        cargarImagenDetalleProducto(p.foto);
    }

    function cargarEstadoDetalleProducto(estado) {
        const badge = document.getElementById("detalleEstadoProducto");

        if (!badge) return;

        badge.classList.remove("text-bg-success", "text-bg-secondary", "text-bg-danger");

        if (estado === 1) {
            badge.textContent = "Activo";
            badge.classList.add("text-bg-success");
        } else if (estado === 2) {
            badge.textContent = "Inactivo";
            badge.classList.add("text-bg-secondary");
        } else {
            badge.textContent = "Eliminado";
            badge.classList.add("text-bg-danger");
        }
    }

    function cargarImagenDetalleProducto(foto) {
        const img = document.getElementById("detalleImagenProducto");
        const sinImagen = document.getElementById("detalleSinImagen");

        if (!img || !sinImagen) return;

        if (foto) {
            img.src = `/uploads/productos/${foto}`;
            img.style.display = "block";
            sinImagen.style.display = "none";
        } else {
            img.src = "";
            img.style.display = "none";
            sinImagen.style.display = "block";
        }
    }

    function cargarAtributosDetalleProducto(atributos) {
        const contenedor = document.getElementById("detalleAtributosProducto");

        if (!contenedor) return;

        contenedor.innerHTML = "";

        const entradas = Object.entries(atributos);

        if (!entradas.length) {
            contenedor.innerHTML = `
                <div class="col-12">
                    <div class="alert alert-light border mb-0 small text-muted">
                        Sin atributos técnicos registrados.
                    </div>
                </div>
            `;
            return;
        }

        entradas.forEach(([nombre, valor]) => {
            const col = document.createElement("div");
            col.className = "col-12 col-md-4";

            col.innerHTML = `
                <div class="border rounded-4 p-3 bg-light h-100">
                    <small class="text-muted d-block">${nombre}</small>
                    <span class="fw-semibold">${valor || "-"}</span>
                </div>
            `;

            contenedor.appendChild(col);
        });
    }

    function asignarTexto(id, texto) {
        const elemento = document.getElementById(id);

        if (elemento) {
            elemento.textContent = texto;
        }
    }

    document.addEventListener("click", event => {
        const btnVer = event.target.closest(".btn-ver-producto");

        if (!btnVer) return;

        const id = btnVer.getAttribute("data-id");

        if (id) {
            abrirDetalleProducto(id, btnVer);
        }
    });

    const btnExportarPdfProducto = document.getElementById("btnExportarPdfProducto");

    if (btnExportarPdfProducto) {
        btnExportarPdfProducto.addEventListener("click", () => {
            exportarDetalleProductoPdf();
        });
    }

    function exportarDetalleProductoPdf() {
        const detalle = document.querySelector("#modalDetalleProducto .modal-body");

        if (!detalle) {
            alert("No se encontró el detalle del producto.");
            return;
        }

        if (typeof html2pdf === "undefined") {
            alert("No se pudo cargar la librería para exportar PDF.");
            return;
        }

        const nombreProducto = document.getElementById("detalleNombreProducto")?.textContent?.trim() || "producto";
        const codigoProducto = document.getElementById("detalleCodigoProducto")?.textContent?.trim() || "código";

        const copia = detalle.cloneNode(true);
        copia.querySelectorAll(".p-4").forEach(el => el.style.padding = "8px");
        copia.querySelectorAll(".p-3").forEach(el => el.style.padding = "6px");
        copia.querySelectorAll(".mb-4").forEach(el => el.style.marginBottom = "8px");
        copia.querySelectorAll(".mb-3").forEach(el => el.style.marginBottom = "6px");
        copia.querySelectorAll(".row").forEach(el => {
            el.style.marginTop = "0";
            el.style.marginBottom = "0";
        });
        copia.querySelectorAll("h4").forEach(el => el.style.fontSize = "15px");
        copia.querySelectorAll("h6").forEach(el => el.style.fontSize = "11px");
        copia.querySelectorAll("small").forEach(el => el.style.fontSize = "8px");
        copia.querySelectorAll("p, span, div").forEach(el => {
            el.style.lineHeight = "1.15";
        });
        copia.querySelectorAll("img").forEach(el => {
            el.style.maxHeight = "110px";
            el.style.objectFit = "contain";
        });

        const contenedorPdf = document.createElement("div");
        contenedorPdf.style.width = "190mm";
        contenedorPdf.style.padding = "6mm";
        contenedorPdf.style.background = "#ffffff";
        contenedorPdf.style.color = "#111111";
        contenedorPdf.style.fontFamily = "Arial, sans-serif";
        contenedorPdf.style.fontSize = "10px";
        contenedorPdf.style.margin = "0";

        contenedorPdf.innerHTML = `
            <div style="border-bottom: 1px solid #111; padding-bottom: 5px; margin-bottom: 8px;">
               <h2 style="font-size: 15px; margin: 0; font-weight: 700;">
                    Ficha técnica del producto
                </h2>
                <div style="font-size: 11px; color: #555; margin-top: 3px;">
                    DentIA | Catálogo maestro de productos
                </div>
                <div style="font-size: 11px; color: #555; margin-top: 2px;">
                    Producto: ${nombreProducto} | Código: ${codigoProducto}
                </div>
            </div>
        `;

        contenedorPdf.appendChild(copia);

        const footer = document.createElement("div");
        footer.style.borderTop = "1px solid #ccc";
        footer.style.marginTop = "10px";
        footer.style.paddingTop = "6px";
        footer.style.fontSize = "10px";
        footer.style.color = "#555";
        footer.textContent = "Documento generado desde el sistema ERP DentIA.";

        contenedorPdf.appendChild(footer);

       const opciones = {
           margin: [5, 5, 5, 5],
           filename: `Ficha_${codigoProducto}_${nombreProducto}.pdf`.replaceAll(" ", "_"),
           image: {
               type: "jpeg",
               quality: 0.98
           },
           html2canvas: {
               scale: 2,
               useCORS: true,
               scrollY: 0
           },
           jsPDF: {
               unit: "mm",
               format: "a4",
               orientation: "portrait"
           },
           pagebreak: {
               mode: ["css", "legacy"]
           }
       };

        html2pdf()
            .set(opciones)
            .from(contenedorPdf)
            .save();
    }



    /* =====================================================
       EDITAR PRODUCTO
    ====================================================== */

    function abrirEditarProducto(id, trigger) {
        if (!modalProducto || !id) return;

        fetch(`/interno/productos/api/${id}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("No se pudo encontrar el producto.");
                }
                return response.json();
            })
            .then(data => {
                const p = data.producto;
                const ultimo = data.ultimoPrecioCompra ?? 0;

                atributosValoresEdicion = data.atributosValores || {};

                if (!p) {
                    throw new Error("La respuesta no contiene producto.");
                }

                const titulo = document.getElementById("tituloModalProducto");
                if (titulo) {
                    titulo.innerHTML = `<i class="bi bi-pencil-square me-1"></i> Editar producto`;
                }

               asignarValor("idProducto", p.idProducto);
               asignarValor("codigoInterno", p.codigoInterno);

               asignarValor("idCategoria", p.idCategoria);
               asignarValor("idSubcategoria", p.idSubcategoria);
               asignarValor("idMarca", p.idMarca);
               cargarAtributosPorCategoria(p.idCategoria, atributosValoresEdicion);
              asignarValor("idUnidad", p.idUnidad);

              asignarValor("controlInventario", p.controlInventario ?? "CANTIDAD");
              asignarValor("nombreUnidadInventario", p.nombreUnidadInventario ?? "Unidad");
              asignarValor("formaVenta", p.formaVenta ?? "CANTIDAD");
              asignarValor("nombreUnidadVenta", p.nombreUnidadVenta ?? "Unidad");
              asignarValor("abreviaturaUnidadVenta", p.abreviaturaUnidadVenta ?? "und");

              asignarValor("modeloProducto", p.modeloProducto ?? "");
              asignarValor("descripcionProducto", p.descripcionProducto ?? "");

              cargarMedidasProductoEnFormulario(p);

              actualizarMedidasFisicasProducto();

               asignarValor("idEstadoProducto", p.idEstadoProducto);
               asignarValorPorName("nombreProducto", p.nombreProducto ?? "");
               asignarValorPorName("dimensiones", p.dimensiones ?? "");
               asignarValorPorName("precioUnitario", p.precioUnitario ?? 0);
               asignarValorPorName("stockActual", p.stockActual ?? 0);
               asignarValorPorName("stockMinimo", p.stockMinimo ?? 0);

                if (ultimoPrecioCompraInput) {
                    ultimoPrecioCompraInput.value = ultimo;
                }

                if (descuentoMaximoInput) {
                    descuentoMaximoInput.value = p.descuentoMaximo ?? 0;
                }

                const precioCompraInfo = document.getElementById("precioCompraInfo");
                if (precioCompraInfo) {
                    if (ultimo && Number(ultimo) > 0) {
                        precioCompraInfo.textContent = `Último precio de compra equivalente: S/ ${Number(ultimo).toFixed(2)}`;
                        precioCompraInfo.classList.remove("text-warning");
                        precioCompraInfo.classList.add("text-muted");
                    } else {
                        precioCompraInfo.textContent = "Este producto aún no tiene compras registradas.";
                        precioCompraInfo.classList.remove("text-muted");
                        precioCompraInfo.classList.add("text-warning");
                    }
                }

                limpiarFotosPendientesProducto({ repintar: false });
                cargarGaleriaFotosProducto(p.idProducto);

                limpiarErrorPrecioVenta();
                validarPrecioVentaVisual();
                validarDescuentoMaximo();

                if (formProducto) {
                    formProducto.classList.remove("was-validated");
                }

                mostrarModalSeguro(modalProductoEl, trigger);
            })
            .catch(error => {
                console.error(error);

                if (typeof Swal !== "undefined") {
                    Swal.fire({
                        icon: "error",
                        title: "No se pudo cargar",
                        text: "No se pudo cargar la información del producto."
                    });
                } else {
                    alert("No se pudo cargar la información del producto.");
                }
            });
    }

    function asignarValor(id, valor) {
        const input = document.getElementById(id);
        if (input) input.value = valor ?? "";
    }

    function asignarValorPorName(name, valor) {
        const input = document.getElementsByName(name)[0];
        if (input) input.value = valor ?? "";
    }

    function cargarPreviewProducto(foto) {
        const previewImg = document.getElementById("previewImg");
        const previewIcon = document.getElementById("previewIcon");

        if (!previewImg) return;

        if (foto) {
            previewImg.src = `/uploads/productos/${foto}`;
            previewImg.style.display = "block";

            if (previewIcon) {
                previewIcon.style.display = "none";
            }
        } else {
            previewImg.src = "";
            previewImg.style.display = "none";

            if (previewIcon) {
                previewIcon.style.display = "block";
            }
        }
    }

    document.addEventListener("click", event => {
        const btnEditar = event.target.closest(".btn-editar");

        if (!btnEditar) return;

        const id = btnEditar.getAttribute("data-id");

        if (id) {
            abrirEditarProducto(id, btnEditar);
        }
    });

    /* =====================================================
       CAMBIAR ESTADO
    ====================================================== */

    document.addEventListener("click", event => {
        const btnEstado = event.target.closest(".btn-estado");

        if (!btnEstado || !modalEstado) return;

        const id = btnEstado.getAttribute("data-id");
        const estadoActual = parseInt(btnEstado.getAttribute("data-estado"), 10);
        const nombre = btnEstado.getAttribute("data-nombre") || "";

        const activar = estadoActual !== 1;

        idEstadoSeleccionado = id;
        nuevoEstadoSeleccionado = activar ? 1 : 2;

        if (textoEstadoProducto) {
            textoEstadoProducto.textContent = activar
                ? `¿Deseas activar el producto "${nombre}"?`
                : `¿Deseas desactivar el producto "${nombre}"?`;
        }

        mostrarModalSeguro(modalEstadoEl, btnEstado);
    });

    if (btnConfirmarEstado) {
        btnConfirmarEstado.addEventListener("click", () => {
            if (!idEstadoSeleccionado || !nuevoEstadoSeleccionado) {
                cerrarModalSeguro(modalEstadoEl);
                return;
            }

            cambiarEstadoProducto(idEstadoSeleccionado, nuevoEstadoSeleccionado);
        });
    }

    function cambiarEstadoProducto(id, estado) {
        const url = `/interno/productos/${id}/estado`;
        const body = `estado=${encodeURIComponent(estado)}`;

        const headers = {
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        };

        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(url, {
            method: "POST",
            headers,
            body
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error("Error al cambiar el estado.");
                }
                return response.json();
            })
            .then(data => {
                actualizarEstadoEnTabla(data);

                if (modalEstado) {
                    cerrarModalSeguro(modalEstadoEl);
                }
            })
            .catch(error => {
                console.error(error);

                if (modalEstado) {
                    cerrarModalSeguro(modalEstadoEl);
                }

                if (typeof Swal !== "undefined") {
                    Swal.fire({
                        icon: "error",
                        title: "No se pudo cambiar el estado",
                        text: "Intenta nuevamente."
                    });
                } else {
                    alert("No se pudo cambiar el estado del producto.");
                }
            });
    }

    function actualizarEstadoEnTabla(data) {
        const badge = document.getElementById(`estado-text-${data.id}`);

        if (badge) {
            badge.textContent = data.estadoTexto;

            badge.classList.remove(
                "bg-success",
                "bg-secondary",
                "text-bg-success",
                "text-bg-secondary"
            );

            if (data.estado === 1) {
                badge.classList.add("text-bg-success");
            } else {
                badge.classList.add("text-bg-secondary");
            }
        }

        const btn = document.querySelector(`.btn-estado[data-id="${data.id}"]`);

        if (btn) {
            btn.setAttribute("data-estado", data.estado);

            const icon = btn.querySelector("i.bi");

            if (icon) {
                icon.classList.remove("bi-toggle-off", "bi-toggle-on");

                if (data.estado === 1) {
                    icon.classList.add("bi-toggle-off");
                } else {
                    icon.classList.add("bi-toggle-on");
                }
            }
        }
    }

    /* =====================================================
       ELIMINAR PRODUCTO
    ====================================================== */

    document.addEventListener("click", event => {
        const btnEliminar = event.target.closest(".btn-eliminar");

        if (!btnEliminar || !modalEliminar) return;

        formEliminarSeleccionado = btnEliminar.closest("form");

        const nombre = btnEliminar.getAttribute("data-nombre") || "";

       if (textoEliminarProducto) {
           textoEliminarProducto.textContent =
               `¿Deseas enviar el producto "${nombre}" a eliminados? Ya no aparecerá en compras, ventas ni catálogo, pero podrás restaurarlo después.`;
       }
        mostrarModalSeguro(modalEliminarEl, btnEliminar);
    });

    if (btnConfirmarEliminar) {
        btnConfirmarEliminar.addEventListener("click", () => {
            if (formEliminarSeleccionado) {
                formEliminarSeleccionado.submit();
            }
        });
    }

   /* =====================================================
      FILTROS PRODUCTOS
   ====================================================== */

   const buscadorProductos = document.getElementById("buscadorProductos");
   const filtroCategoriaProductos = document.getElementById("filtroCategoriaProductos");
   const filtroSubcategoriaProductos = document.getElementById("filtroSubcategoriaProductos");
   const filtroMarcaProductos = document.getElementById("filtroMarcaProductos");
   const filtroEstadoProductos = document.getElementById("filtroEstadoProductos");

   const btnFiltrarProductos = document.getElementById("btnFiltrarProductos");
   const btnLimpiarFiltrosProductos = document.getElementById("btnLimpiarFiltrosProductos");
   const productosPorPagina = 10;
   let paginaProductosActual = 1;

   function normalizarTexto(valor) {
       return (valor || "")
           .toString()
           .trim()
           .toLowerCase()
           .normalize("NFD")
           .replace(/[\u0300-\u036f]/g, "");
   }

  function aplicarFiltrosProductos() {
      const textoBuscar = normalizarTexto(buscadorProductos?.value);
      const categoria = normalizarTexto(filtroCategoriaProductos?.value);
      const subcategoria = normalizarTexto(filtroSubcategoriaProductos?.value);
      const marca = normalizarTexto(filtroMarcaProductos?.value);
      const estado = normalizarTexto(filtroEstadoProductos?.value);

      const filas = Array.from(document.querySelectorAll("#tablaProductos tbody tr"))
          .filter(fila => !fila.querySelector("td[colspan]"));

      const filasFiltradas = filas.filter(fila => {
          const nombreFila = normalizarTexto(fila.dataset.nombre);
          const codigoFila = normalizarTexto(fila.dataset.codigo);
          const categoriaFila = normalizarTexto(fila.dataset.categoria);
          const subcategoriaFila = normalizarTexto(fila.dataset.subcategoria);
          const marcaFila = normalizarTexto(fila.dataset.marca);
          const estadoFila = normalizarTexto(fila.dataset.estado);

          const coincideTexto =
              !textoBuscar ||
              nombreFila.includes(textoBuscar) ||
              codigoFila.includes(textoBuscar) ||
              marcaFila.includes(textoBuscar);

          const coincideCategoria =
              !categoria || categoriaFila === categoria;

          const coincideSubcategoria =
              !subcategoria || subcategoriaFila === subcategoria;

          const coincideMarca =
              !marca || marcaFila === marca;

          const coincideEstado =
              !estado || estadoFila === estado;

          return coincideTexto &&
              coincideCategoria &&
              coincideSubcategoria &&
              coincideMarca &&
              coincideEstado;
      });

      const totalPaginas = Math.max(1, Math.ceil(filasFiltradas.length / productosPorPagina));

      if (paginaProductosActual > totalPaginas) {
          paginaProductosActual = totalPaginas;
      }

      const inicio = (paginaProductosActual - 1) * productosPorPagina;
      const fin = inicio + productosPorPagina;

      filas.forEach(fila => fila.style.display = "none");

      filasFiltradas.slice(inicio, fin).forEach(fila => {
          fila.style.display = "";
      });

      actualizarPaginacionProductos(filasFiltradas.length, totalPaginas);
      mostrarMensajeSinResultados();
  }

  function actualizarPaginacionProductos(totalFiltrados, totalPaginas) {
      const textoContador = document.getElementById("contadorProductosListado");
      const paginaActual = document.getElementById("paginaProductosActual");
      const paginaTotal = document.getElementById("paginaProductosTotal");
      const btnAnterior = document.getElementById("btnProductosAnterior");
      const btnSiguiente = document.getElementById("btnProductosSiguiente");

      const inicio = totalFiltrados === 0
          ? 0
          : ((paginaProductosActual - 1) * productosPorPagina) + 1;

      const fin = Math.min(paginaProductosActual * productosPorPagina, totalFiltrados);

      if (textoContador) {
          textoContador.textContent = `Mostrando ${inicio}-${fin} de ${totalFiltrados} productos`;
      }

      if (paginaActual) {
          paginaActual.textContent = paginaProductosActual;
      }

      if (paginaTotal) {
          paginaTotal.textContent = totalPaginas;
      }

      if (btnAnterior) {
          btnAnterior.disabled = paginaProductosActual <= 1;
      }

      if (btnSiguiente) {
          btnSiguiente.disabled = paginaProductosActual >= totalPaginas;
      }
  }

  function cambiarPaginaProductos(direccion) {
      paginaProductosActual += direccion;

      if (paginaProductosActual < 1) {
          paginaProductosActual = 1;
      }

      aplicarFiltrosProductos();
  }

  function reiniciarPaginaYFiltrarProductos() {
      paginaProductosActual = 1;
      aplicarFiltrosProductos();
  }

   function mostrarMensajeSinResultados() {
       const tbody = document.querySelector("#tablaProductos tbody");
       if (!tbody) return;

       let filaMensaje = document.getElementById("filaSinResultadosProductos");

       const filasVisibles = Array.from(tbody.querySelectorAll("tr"))
           .filter(fila => !fila.querySelector("td[colspan]"))
           .some(fila => fila.style.display !== "none");

       if (!filaMensaje) {
           filaMensaje = document.createElement("tr");
           filaMensaje.id = "filaSinResultadosProductos";
           filaMensaje.innerHTML = `
               <td colspan="7" class="text-center py-5">
                   <div class="text-muted">
                       <i class="bi bi-search fs-1 d-block mb-2"></i>
                       No se encontraron productos con los filtros seleccionados.
                   </div>
               </td>
           `;
           tbody.appendChild(filaMensaje);
       }

       filaMensaje.style.display = filasVisibles ? "none" : "";
   }

   function limpiarFiltrosProductos() {
       if (buscadorProductos) buscadorProductos.value = "";
       if (filtroCategoriaProductos) filtroCategoriaProductos.value = "";
       if (filtroSubcategoriaProductos) filtroSubcategoriaProductos.value = "";
       if (filtroMarcaProductos) filtroMarcaProductos.value = "";
       if (filtroEstadoProductos) filtroEstadoProductos.value = "";

       paginaProductosActual = 1;
       aplicarFiltrosProductos();
   }

  [
      buscadorProductos,
      filtroCategoriaProductos,
      filtroSubcategoriaProductos,
      filtroMarcaProductos,
      filtroEstadoProductos
  ].forEach(control => {
      if (control) {
          control.addEventListener("input", reiniciarPaginaYFiltrarProductos);
          control.addEventListener("change", reiniciarPaginaYFiltrarProductos);
      }
  });

   if (btnFiltrarProductos) {
       btnFiltrarProductos.addEventListener("click", reiniciarPaginaYFiltrarProductos);
   }

   if (btnLimpiarFiltrosProductos) {
       btnLimpiarFiltrosProductos.addEventListener("click", limpiarFiltrosProductos);
   }

   const btnProductosAnterior = document.getElementById("btnProductosAnterior");
   const btnProductosSiguiente = document.getElementById("btnProductosSiguiente");

   if (btnProductosAnterior) {
       btnProductosAnterior.addEventListener("click", () => cambiarPaginaProductos(-1));
   }

   if (btnProductosSiguiente) {
       btnProductosSiguiente.addEventListener("click", () => cambiarPaginaProductos(1));
   }

   aplicarFiltrosProductos();

    /* =====================================================
       CÁLCULO AUTOMÁTICO DE PRECIO
    ====================================================== */

    if (precioVentaInput && porcentajeGananciaInput && ultimoPrecioCompraInput) {

        porcentajeGananciaInput.addEventListener("input", () => {
            const base = parseFloat(ultimoPrecioCompraInput.value || 0);
            const porcentaje = parseFloat(porcentajeGananciaInput.value || 0);

            if (base > 0) {
                const precio = base + (base * porcentaje / 100);
                precioVentaInput.value = precio.toFixed(2);
                validarPrecioVentaVisual();
                validarDescuentoMaximo();
            }
        });

        precioVentaInput.addEventListener("input", () => {
            const base = parseFloat(ultimoPrecioCompraInput.value || 0);
            const precio = parseFloat(precioVentaInput.value || 0);

            if (base > 0 && precio > 0) {
                const porcentaje = ((precio - base) / base) * 100;
                porcentajeGananciaInput.value = porcentaje.toFixed(1);
            }

            validarPrecioVentaVisual();
            validarDescuentoMaximo();
        });
    }

    /* =====================================================
       VALIDACIÓN PRECIO VENTA
    ====================================================== */

    function validarPrecioVentaVisual() {
        if (!precioVentaInput || !ultimoPrecioCompraInput || !errorPrecioVenta) {
            return true;
        }

        const precioVenta = parseFloat(precioVentaInput.value || 0);
        const ultimoPrecio = parseFloat(ultimoPrecioCompraInput.value || 0);

        if (ultimoPrecio > 0 && precioVenta > 0 && precioVenta < ultimoPrecio) {
            precioVentaInput.classList.add("is-invalid");
            errorPrecioVenta.classList.remove("d-none");
            errorPrecioVenta.innerHTML =
                `El precio debe ser mayor o igual a S/ ${ultimoPrecio.toFixed(2)}`;
            return false;
        }

        limpiarErrorPrecioVenta();
        return true;
    }

    function limpiarErrorPrecioVenta() {
        if (precioVentaInput) {
            precioVentaInput.classList.remove("is-invalid");
        }

        if (errorPrecioVenta) {
            errorPrecioVenta.classList.add("d-none");
            errorPrecioVenta.innerHTML = "";
        }
    }

    if (precioVentaInput) {
        precioVentaInput.addEventListener("input", validarPrecioVentaVisual);
    }

    /* =====================================================
       VALIDACIÓN DESCUENTO MÁXIMO
    ====================================================== */

    function validarDescuentoMaximo() {
        if (!descuentoMaximoInput || !precioVentaInput) {
            return true;
        }

        const precioVenta = parseFloat(precioVentaInput.value || 0);
        const descuento = parseFloat(descuentoMaximoInput.value || 0);

        if (precioVenta <= 0) {
            return true;
        }

        if (descuento > precioVenta) {
            descuentoMaximoInput.value = precioVenta.toFixed(2);

            if (typeof Swal !== "undefined") {
                Swal.fire({
                    icon: "warning",
                    title: "Descuento inválido",
                    text: `El descuento maximo no puede ser mayor al precio principal.`
                });
            }

            return false;
        }

        return true;
    }

    if (descuentoMaximoInput) {
        descuentoMaximoInput.addEventListener("input", validarDescuentoMaximo);
    }


  function validarMedidaProducto() {
      const tieneMedidas = ventaPorMedidaInput?.value === "true";
      const tipo = tipoMedidaBaseInput?.value || "";

      if (!tieneMedidas) {
          return true;
      }

      const largo = Number(largoBaseInput?.value || 0);
      const ancho = Number(anchoBaseInput?.value || 0);
      const vendible = Number(medidaVendibleBaseInput?.value || 0);

      if (!tipo) {
          Swal.fire({
              icon: "warning",
              title: "Falta tipo de medida",
              text: "Selecciona si la unidad física se mide por área o longitud."
          });
          return false;
      }

      if (largo <= 0) {
          Swal.fire({
              icon: "warning",
              title: "Falta largo real",
              text: "Ingresa el largo real de la unidad física."
          });
          return false;
      }

      if (tipo === "AREA" && ancho <= 0) {
          Swal.fire({
              icon: "warning",
              title: "Falta ancho real",
              text: "Ingresa el ancho real de la plancha."
          });
          return false;
      }

      if (vendible <= 0) {
          Swal.fire({
              icon: "warning",
              title: "Medida vendible inválida",
              text: "La medida vendible debe ser mayor a cero."
          });
          return false;
      }

      return true;
  }

    /* =====================================================
       VALIDACIÓN BOOTSTRAP FORMULARIO
    ====================================================== */

    if (formProducto) {
        formProducto.addEventListener("submit", event => {

            aplicarUnidadPorTipo();

            const unidadSelect = document.getElementById("idUnidad");

            if (unidadSelect) {
                unidadSelect.disabled = false;
            }

            asignarValor("controlInventario", "CANTIDAD");
            asignarValor("formaVenta", "CANTIDAD");
            asignarValor("nombreUnidadVenta", "Unidad");
            asignarValor("abreviaturaUnidadVenta", "und");

          actualizarMedidasFisicasProducto();
         calcularMedidaFisicaProducto();

          const precioValido = validarPrecioVentaVisual();
          const descuentoValido = validarDescuentoMaximo();
          const medidaValida = validarMedidaProducto();

          if (!formProducto.checkValidity() || !precioValido || !descuentoValido || !medidaValida) {
                event.preventDefault();
                event.stopPropagation();


            }

            formProducto.classList.add("was-validated");

        }, false);
    }

    /* =====================================================
       REABRIR MODAL SI EL CONTROLLER DEVUELVE ERROR
    ====================================================== */

    const modalError = document.body.getAttribute("data-modal-error");

    if (modalError === "true" && modalProducto && formProducto) {
        const titulo = document.getElementById("tituloModalProducto");
        const idHidden = document.getElementById("idProducto");

        if (titulo && idHidden) {
            const esEdicion = idHidden.value && idHidden.value !== "0";

            titulo.innerHTML = esEdicion
                ? `<i class="bi bi-pencil-square me-1"></i> Editar producto`
                : `<i class="bi bi-box-seam me-1"></i> Nuevo producto`;
        }

        aplicarUnidadPorTipo();
       actualizarMedidasFisicasProducto();
        validarPrecioVentaVisual();
        if (idHidden?.value && idHidden.value !== "0") {
            cargarGaleriaFotosProducto(idHidden.value);
        } else {
            reiniciarGaleriaProducto();
        }

        mostrarModalSeguro(modalProductoEl, btnNuevoProducto);
    }

   /* =====================================================
      CAMBIO DE CATEGORÍA CONFIGURABLE
   ====================================================== */

   const categoriaSelect = document.getElementById("idCategoria");
   const subcategoriaSelect = document.getElementById("idSubcategoria");

   if (categoriaSelect) {
      categoriaSelect.addEventListener("change", () => {

          window.generarCodigo();
          filtrarSubcategoriasPorCategoria();
          cargarAtributosPorCategoria(categoriaSelect.value);
      });
   }

   function filtrarSubcategoriasPorCategoria() {
       if (!categoriaSelect || !subcategoriaSelect) return;

       const idCategoria = categoriaSelect.value;

       Array.from(subcategoriaSelect.options).forEach(option => {
           if (!option.value) {
               option.hidden = false;
               return;
           }

           const categoriaOption = option.getAttribute("data-categoria");

           option.hidden = idCategoria && categoriaOption !== idCategoria;
       });

       const selectedOption = subcategoriaSelect.options[subcategoriaSelect.selectedIndex];

       if (selectedOption && selectedOption.hidden) {
           subcategoriaSelect.value = "";
       }
   }

   /* =====================================================
      INICIALIZACIÓN
   ====================================================== */

   filtrarSubcategoriasPorCategoria();
   aplicarUnidadPorTipo();

  inicializarEventosMedidasFisicas();
   actualizarMedidasFisicasProducto();

});
// ============================================================
// MANTENEDOR CATEGORÍAS: EDITAR / CANCELAR
// ============================================================

document.addEventListener("DOMContentLoaded", function () {

    const formCategoria = document.getElementById("formCategoriaMant");
    const idCategoria = document.getElementById("idCategoriaMant");
    const nombreCategoria = document.getElementById("nombreCategoriaMant");
    const btnGuardarCategoria = document.getElementById("btnGuardarCategoriaMant");
    const btnCancelarCategoria = document.getElementById("btnCancelarEdicionCategoria");

    if (!formCategoria || !idCategoria || !nombreCategoria || !btnGuardarCategoria) {
        return;
    }

    document.querySelectorAll(".btn-editar-categoria").forEach(function (btn) {
        btn.addEventListener("click", function () {

            const id = this.dataset.id;
            const nombre = this.dataset.nombre;

            idCategoria.value = id;
            nombreCategoria.value = nombre;

            btnGuardarCategoria.innerHTML = '<i class="bi bi-save me-1"></i> Actualizar';
            btnGuardarCategoria.classList.remove("btn-primary");
            btnGuardarCategoria.classList.add("btn-success");

            if (btnCancelarCategoria) {
                btnCancelarCategoria.style.display = "inline-block";
            }

            nombreCategoria.focus();
        });
    });

    if (btnCancelarCategoria) {
        btnCancelarCategoria.addEventListener("click", function () {

            idCategoria.value = "";
            nombreCategoria.value = "";

            btnGuardarCategoria.innerHTML = '<i class="bi bi-plus-circle me-1"></i> Agregar';
            btnGuardarCategoria.classList.remove("btn-success");
            btnGuardarCategoria.classList.add("btn-primary");

            btnCancelarCategoria.style.display = "none";
            nombreCategoria.focus();
        });
    }
    const modalCategoriasEl = document.getElementById("modalCategorias");

    if (modalCategoriasEl) {
        modalCategoriasEl.addEventListener("hidden.bs.modal", function () {
            idCategoria.value = "";
            nombreCategoria.value = "";

            btnGuardarCategoria.innerHTML = '<i class="bi bi-plus-circle me-1"></i> Agregar';
            btnGuardarCategoria.classList.remove("btn-success");
            btnGuardarCategoria.classList.add("btn-primary");

            if (btnCancelarCategoria) {
                btnCancelarCategoria.style.display = "none";
            }
        });
    }
});
// ============================================================
// MANTENEDOR SUBCATEGORÍAS: EDITAR / CANCELAR
// ============================================================

document.addEventListener("DOMContentLoaded", function () {

    const idSubcategoria = document.getElementById("idSubcategoriaMant");
    const idCategoriaSub = document.getElementById("idCategoriaSubMant");
    const nombreSubcategoria = document.getElementById("nombreSubcategoriaMant");
    const btnGuardarSub = document.getElementById("btnGuardarSubcategoriaMant");
    const btnCancelarSub = document.getElementById("btnCancelarEdicionSubcategoria");

    if (!idSubcategoria || !idCategoriaSub || !nombreSubcategoria || !btnGuardarSub) {
        return;
    }

    document.querySelectorAll(".btn-editar-subcategoria").forEach(function (btn) {
        btn.addEventListener("click", function () {

            idSubcategoria.value = this.dataset.id;
            idCategoriaSub.value = this.dataset.categoria;
            nombreSubcategoria.value = this.dataset.nombre;

            btnGuardarSub.innerHTML = '<i class="bi bi-save"></i>';
            btnGuardarSub.classList.remove("btn-primary");
            btnGuardarSub.classList.add("btn-success");

            if (btnCancelarSub) {
                btnCancelarSub.style.display = "inline-block";
            }

            nombreSubcategoria.focus();
        });
    });

    if (btnCancelarSub) {
        btnCancelarSub.addEventListener("click", function () {

            idSubcategoria.value = "";
            idCategoriaSub.value = "";
            nombreSubcategoria.value = "";

            btnGuardarSub.innerHTML = '<i class="bi bi-plus-circle"></i>';
            btnGuardarSub.classList.remove("btn-success");
            btnGuardarSub.classList.add("btn-primary");

            btnCancelarSub.style.display = "none";
            nombreSubcategoria.focus();
        });
    }

    const modalSubcategoriasEl = document.getElementById("modalSubcategorias");

    if (modalSubcategoriasEl) {
        modalSubcategoriasEl.addEventListener("hidden.bs.modal", function () {

            idSubcategoria.value = "";
            idCategoriaSub.value = "";
            nombreSubcategoria.value = "";

            btnGuardarSub.innerHTML = '<i class="bi bi-plus-circle"></i>';
            btnGuardarSub.classList.remove("btn-success");
            btnGuardarSub.classList.add("btn-primary");

            if (btnCancelarSub) {
                btnCancelarSub.style.display = "none";
            }
        });
    }
});
// ============================================================
// MANTENEDOR MARCAS: EDITAR / CANCELAR
// ============================================================

document.addEventListener("DOMContentLoaded", function () {

    const idMarca = document.getElementById("idMarcaMant");
    const nombreMarca = document.getElementById("nombreMarcaMant");
    const descripcionMarca = document.getElementById("descripcionMarcaMant");
    const btnGuardarMarca = document.getElementById("btnGuardarMarcaMant");
    const btnCancelarMarca = document.getElementById("btnCancelarEdicionMarca");

    if (!idMarca || !nombreMarca || !descripcionMarca || !btnGuardarMarca) {
        return;
    }

    document.querySelectorAll(".btn-editar-marca").forEach(function (btn) {
        btn.addEventListener("click", function () {

            idMarca.value = this.dataset.id || "";
            nombreMarca.value = this.dataset.nombre || "";
            descripcionMarca.value = this.dataset.descripcion || "";

            btnGuardarMarca.innerHTML = '<i class="bi bi-save"></i>';
            btnGuardarMarca.classList.remove("btn-primary");
            btnGuardarMarca.classList.add("btn-success");

            if (btnCancelarMarca) {
                btnCancelarMarca.style.display = "inline-block";
            }

            nombreMarca.focus();
        });
    });

    if (btnCancelarMarca) {
        btnCancelarMarca.addEventListener("click", function () {

            idMarca.value = "";
            nombreMarca.value = "";
            descripcionMarca.value = "";

            btnGuardarMarca.innerHTML = '<i class="bi bi-plus-circle"></i>';
            btnGuardarMarca.classList.remove("btn-success");
            btnGuardarMarca.classList.add("btn-primary");

            btnCancelarMarca.style.display = "none";
            nombreMarca.focus();
        });
    }

    const modalMarcasEl = document.getElementById("modalMarcas");

    if (modalMarcasEl) {
        modalMarcasEl.addEventListener("hidden.bs.modal", function () {

            idMarca.value = "";
            nombreMarca.value = "";
            descripcionMarca.value = "";

            btnGuardarMarca.innerHTML = '<i class="bi bi-plus-circle"></i>';
            btnGuardarMarca.classList.remove("btn-success");
            btnGuardarMarca.classList.add("btn-primary");

            if (btnCancelarMarca) {
                btnCancelarMarca.style.display = "none";
            }
        });
    }
});
// ============================================================
// MANTENEDOR ATRIBUTOS: EDITAR / CANCELAR
// ============================================================

document.addEventListener("DOMContentLoaded", function () {

    const idAtributo = document.getElementById("idAtributoMant");
    const idCategoriaAtributo = document.getElementById("idCategoriaAtributoMant");
    const nombreAtributo = document.getElementById("nombreAtributoMant");
    const tipoDatoAtributo = document.getElementById("tipoDatoAtributoMant");
    const btnGuardarAtributo = document.getElementById("btnGuardarAtributoMant");
    const btnCancelarAtributo = document.getElementById("btnCancelarEdicionAtributo");

    if (!idAtributo || !idCategoriaAtributo || !nombreAtributo || !tipoDatoAtributo || !btnGuardarAtributo) {
        return;
    }

    document.querySelectorAll(".btn-editar-atributo").forEach(function (btn) {
        btn.addEventListener("click", function () {

            idAtributo.value = this.dataset.id || "";
            idCategoriaAtributo.value = this.dataset.categoria || "";
            nombreAtributo.value = this.dataset.nombre || "";
            tipoDatoAtributo.value = this.dataset.tipo || "TEXTO";

            btnGuardarAtributo.innerHTML = '<i class="bi bi-save"></i>';
            btnGuardarAtributo.classList.remove("btn-primary");
            btnGuardarAtributo.classList.add("btn-success");

            if (btnCancelarAtributo) {
                btnCancelarAtributo.style.display = "inline-block";
            }

            nombreAtributo.focus();
        });
    });

    if (btnCancelarAtributo) {
        btnCancelarAtributo.addEventListener("click", function () {

            idAtributo.value = "";
            idCategoriaAtributo.value = "";
            nombreAtributo.value = "";
            tipoDatoAtributo.value = "TEXTO";

            btnGuardarAtributo.innerHTML = '<i class="bi bi-plus-circle"></i>';
            btnGuardarAtributo.classList.remove("btn-success");
            btnGuardarAtributo.classList.add("btn-primary");

            btnCancelarAtributo.style.display = "none";
            nombreAtributo.focus();
        });
    }

    const modalAtributosEl = document.getElementById("modalAtributos");

    if (modalAtributosEl) {
        modalAtributosEl.addEventListener("hidden.bs.modal", function () {

            idAtributo.value = "";
            idCategoriaAtributo.value = "";
            nombreAtributo.value = "";
            tipoDatoAtributo.value = "TEXTO";

            btnGuardarAtributo.innerHTML = '<i class="bi bi-plus-circle"></i>';
            btnGuardarAtributo.classList.remove("btn-success");
            btnGuardarAtributo.classList.add("btn-primary");

            if (btnCancelarAtributo) {
                btnCancelarAtributo.style.display = "none";
            }
        });
    }
});
// ============================================================
// MODAL: PRESENTACIONES DE VENTA Y PRECIOS POR PRODUCTO
// ============================================================

document.addEventListener("DOMContentLoaded", () => {

    const modalPrecios = document.getElementById("modalPreciosProducto");

    if (!modalPrecios) return;

    const inputIdProducto = document.getElementById("precioIdProducto");
    const inputIdPrecioPresentacion = document.getElementById("precioIdPrecioPresentacion");
    const inputNombreProducto = document.getElementById("precioNombreProducto");
    const inputUnidadStockProducto = document.getElementById("precioUnidadStockProducto");

    const inputTipoCalculoVenta = document.getElementById("precioTipoCalculoVenta");

    const inputNombrePresentacion = document.getElementById("precioNombrePresentacion");
    const selectUnidadVenta = document.getElementById("precioIdUnidadVenta");
    const inputCantidadIncluida = document.getElementById("precioCantidadIncluida");

    const bloqueCalculoArea = document.getElementById("bloqueCalculoArea");
    const bloqueCalculoLongitud = document.getElementById("bloqueCalculoLongitud");

    const inputLargoBase = document.getElementById("precioLargoBase");
    const inputAnchoBase = document.getElementById("precioAnchoBase");
    const inputMedidaTotalBase = document.getElementById("precioMedidaTotalBase");

    const inputLargoTotalBase = document.getElementById("precioLargoTotalBase");
    const inputMedidaTotalLongitud = document.getElementById("precioMedidaTotalLongitud");

    const inputFactorConversionStock = document.getElementById("precioFactorConversionStock");

    const inputPrecioVenta = document.getElementById("precioVentaModal");
    const inputDescuentoMaximo = document.getElementById("precioDescuentoMaximo");
    const inputDescripcionUso = document.getElementById("precioDescripcionUso");

    const inputCostoPromedioModal = document.getElementById("ultimoPrecioCompraModal");
    const inputGananciaModal = document.getElementById("porcentajeGananciaModal");
    const inputModoRemate = document.getElementById("precioModoRemate");

    const textoDescuentoPorcentaje = document.getElementById("textoDescuentoPorcentaje");
    const textoLimiteDescuento = document.getElementById("textoLimiteDescuento");

    const bloqueMotivoPrecioBajoCosto = document.getElementById("bloqueMotivoPrecioBajoCosto");
    const inputMotivoPrecioBajoCosto = document.getElementById("precioMotivoBajoCosto");

    const ayudaUnidadVenta = document.getElementById("ayudaUnidadVenta");
    const textoResultadoStock = document.getElementById("textoResultadoStock");
    const textoAyudaCalculo = document.getElementById("textoAyudaCalculo");

    const btnGuardar = document.getElementById("btnGuardarPrecioProducto");
    const btnLimpiar = document.getElementById("btnLimpiarPrecioProducto");

   const alerta = document.getElementById("alertaPrecioProducto");
   const tabla = document.getElementById("tablaPreciosProducto");
   const modalOfertaEl = document.getElementById("modalOfertaPresentacion");
   const modalOferta = modalOfertaEl ? bootstrap.Modal.getOrCreateInstance(modalOfertaEl) : null;
   const ofertaIdPrecioPresentacion = document.getElementById("ofertaIdPrecioPresentacion");
   const ofertaNombrePresentacion = document.getElementById("ofertaNombrePresentacion");
   const ofertaPrecioNormal = document.getElementById("ofertaPrecioNormal");
   const ofertaPrecioNormalTexto = document.getElementById("ofertaPrecioNormalTexto");
   const ofertaPrecioOferta = document.getElementById("ofertaPrecioOferta");
   const ofertaFechaInicio = document.getElementById("ofertaFechaInicio");
   const ofertaFechaFin = document.getElementById("ofertaFechaFin");
   const ofertaTexto = document.getElementById("ofertaTexto");
   const ofertaActivo = document.getElementById("ofertaActivo");
   const alertaOferta = document.getElementById("alertaOfertaPresentacion");
   const btnGuardarOferta = document.getElementById("btnGuardarOfertaPresentacion");
   const btnDesactivarOferta = document.getElementById("btnDesactivarOfertaPresentacion");

   const TASA_IGV_PRECIO = 0.18;
   const FACTOR_IGV_PRECIO = 1 + TASA_IGV_PRECIO;

const ID_UNIDAD_PIE2 = "13";

let pie2PorUnidadFisicaProducto = 0;
let metroLinealPorUnidadFisicaProducto = 0;
let tipoMedidaBaseActualProducto = "";

    function obtenerBaseSinIgv(precioFinalConIgv) {
        return precioFinalConIgv / FACTOR_IGV_PRECIO;
    }

    function obtenerIgvIncluido(precioFinalConIgv) {
        return precioFinalConIgv - obtenerBaseSinIgv(precioFinalConIgv);
    }

    function obtenerPrecioFinalConIgv(baseSinIgv) {
        return baseSinIgv * FACTOR_IGV_PRECIO;
    }
    const opcionesBaseUnidadVenta = Array.from(selectUnidadVenta.options)
        .map(option => ({
            value: option.value,
            tipo: option.dataset.tipo || "",
            nombre: option.dataset.nombre || option.textContent.trim(),
            abreviatura: option.dataset.abreviatura || ""
        }))
        .filter(option => option.value);

    document.querySelectorAll(".btn-precios-producto").forEach(btn => {
        btn.addEventListener("click", () => {

            const idProducto = btn.dataset.id;
            const nombreProducto = btn.dataset.nombre;

            const idUnidadInventario = btn.dataset.idUnidadInventario || "";
            const unidadInventario = btn.dataset.unidadStock || "Unidad";

            const formaVentaProducto = btn.dataset.formaVenta || "CANTIDAD";
            const unidadVentaProducto = btn.dataset.unidadVenta || "Unidad";
            const abreviaturaVentaProducto = btn.dataset.abreviaturaVenta || "";

          const medidaTotalProducto = btn.dataset.medidaTotal || "";
          const largoBaseProducto = btn.dataset.largoBase || "";
          const anchoBaseProducto = btn.dataset.anchoBase || "";
          const tieneMedidaProducto = btn.dataset.tieneMedida === "true";
          const tipoMedidaBaseProducto = btn.dataset.tipoMedidaBase || "";
          const medidaVendibleProducto = Number(btn.dataset.medidaVendible || 0);

         tipoMedidaBaseActualProducto = tipoMedidaBaseProducto;

         pie2PorUnidadFisicaProducto =
             tieneMedidaProducto &&
             tipoMedidaBaseProducto === "AREA" &&
             medidaVendibleProducto > 0
                 ? medidaVendibleProducto
                 : 0;

         metroLinealPorUnidadFisicaProducto =
             tieneMedidaProducto &&
             tipoMedidaBaseProducto === "LONGITUD" &&
             medidaVendibleProducto > 0
                 ? medidaVendibleProducto
                 : 0;

        const gananciaSugeridaProducto = btn.dataset.gananciaSugerida || "10";

        inputIdProducto.value = idProducto;
        inputNombreProducto.value = nombreProducto || "";

        if (inputCostoPromedioModal) {
            inputCostoPromedioModal.value = "0.00";
        }

        if (inputGananciaModal) {
            inputGananciaModal.value = Number(gananciaSugeridaProducto || 10).toFixed(2);
        }

        cargarCostoPromedioSedeActiva(idProducto);

           if (inputUnidadStockProducto) {
               inputUnidadStockProducto.value = `Inventario: ${unidadInventario}`;
           }
          limpiarFormularioPrecio();

          cargarMedidasBaseProducto(
              formaVentaProducto,
              medidaTotalProducto,
              largoBaseProducto,
              anchoBaseProducto
          );

         configurarFormasVentaProducto(
             formaVentaProducto,
             idUnidadInventario,
             unidadInventario,
             unidadVentaProducto,
             abreviaturaVentaProducto,
             tieneMedidaProducto,
             tipoMedidaBaseProducto
         );
          actualizarBloquesSegunUnidad();
          calcularDescuentoStock();
          calcularPrecioDesdeGananciaModal();
          controlarModoRemateModal();
          validarDescuentoMaximoPrecioModal();
          actualizarResumenDescuentoModal();

          cargarPreciosProducto(idProducto);
        });
    });

    btnGuardar?.addEventListener("click", guardarPrecioProducto);
    btnLimpiar?.addEventListener("click", limpiarFormularioPrecio);

   selectUnidadVenta?.addEventListener("change", () => {
       actualizarBloquesSegunUnidad();
       calcularDescuentoStock();
       sugerirNombreYDescripcion();
       calcularPrecioDesdeGananciaModal();
       validarDescuentoMaximoPrecioModal();
       actualizarResumenDescuentoModal();
   });

   inputCantidadIncluida?.addEventListener("input", () => {
       calcularDescuentoStock();
       calcularPrecioDesdeGananciaModal();
       actualizarResumenDescuentoModal();
   });

   inputLargoBase?.addEventListener("input", () => {
       calcularDescuentoStock();
       calcularPrecioDesdeGananciaModal();
       actualizarResumenDescuentoModal();
   });

   inputAnchoBase?.addEventListener("input", () => {
       calcularDescuentoStock();
       calcularPrecioDesdeGananciaModal();
       actualizarResumenDescuentoModal();
   });

   inputLargoTotalBase?.addEventListener("input", () => {
       calcularDescuentoStock();
       calcularPrecioDesdeGananciaModal();
       actualizarResumenDescuentoModal();
   });

   inputGananciaModal?.addEventListener("input", () => {
       calcularPrecioDesdeGananciaModal();
       actualizarResumenDescuentoModal();
   });

   inputPrecioVenta?.addEventListener("input", () => {
       if (!inputModoRemate?.checked) {
           calcularGananciaDesdePrecioModal();
       }

       controlarModoRemateModal();
       validarDescuentoMaximoPrecioModal();
       actualizarResumenDescuentoModal();
   });

   inputDescuentoMaximo?.addEventListener("input", () => {
       validarDescuentoMaximoPrecioModal();
       actualizarResumenDescuentoModal();
   });

   inputModoRemate?.addEventListener("change", () => {
       controlarModoRemateModal();
       validarDescuentoMaximoPrecioModal();
       actualizarResumenDescuentoModal();
   });

    function cargarCostoPromedioSedeActiva(idProducto) {
        if (!idProducto || !inputCostoPromedioModal) {
            return;
        }

       fetch(`/interno/productos/api/${idProducto}`)
           .then(response => {
               if (!response.ok) {
                   throw new Error("No se pudo obtener el costo promedio por sede.");
               }

               return response.json();
           })
           .then(data => {
               const costoSede = Number(data.costoPromedioSedeActiva || 0);

                inputCostoPromedioModal.value = costoSede.toFixed(2);

                calcularPrecioDesdeGananciaModal();
               validarDescuentoMaximoPrecioModal();
               actualizarResumenDescuentoModal();
           })
           .catch(error => {
               console.error(error);

               inputCostoPromedioModal.value = "0.00";
                mostrarAlertaPrecio("No se pudo cargar el costo promedio de la sede activa.");
            });
    }

   function configurarFormasVentaProducto(formaVentaProducto,
                                         idUnidadInventario,
                                         unidadInventario,
                                         unidadVentaProducto,
                                         abreviaturaVentaProducto,
                                         tieneMedidaProducto,
                                         tipoMedidaBaseProducto) {
      if (!selectUnidadVenta) return;

      selectUnidadVenta.innerHTML = "";

      const optionDefault = document.createElement("option");
      optionDefault.value = "";
      optionDefault.textContent = "Seleccione...";
      selectUnidadVenta.appendChild(optionDefault);

      const unidadBaseId = idUnidadInventario || buscarPrimeraUnidadCantidad(opcionesBaseUnidadVenta);
      const nombreBase = unidadInventario || "Unidad";

      // Siempre puede vender unidad completa
      agregarOpcionTipoVenta(
          unidadBaseId,
          "UNIDAD",
          "Unidad completa",
          nombreBase,
          ""
      );

      const textoUnidad = normalizarPrecioTexto(nombreBase);

      const esPlancha = textoUnidad.includes("plancha");
      const esVarilla = textoUnidad.includes("varilla");
      const esRollo = textoUnidad.includes("rollo");
      const esUnidadPartible = esPlancha || esVarilla || esRollo;

      // Media ya no es solo para plancha.
      // También aplica para varilla o rollo si la unidad física se puede partir.
      if (esUnidadPartible) {
          agregarOpcionTipoVenta(
              unidadBaseId,
              "MEDIA",
              "Mitad",
              nombreBase,
              ""
          );
      }

      // Pie² solo para productos con medida por área
      if (tieneMedidaProducto && tipoMedidaBaseProducto === "AREA") {
          agregarOpcionTipoVenta(
              ID_UNIDAD_PIE2,
              "PIE2",
              "Pie²",
              "Pie cuadrado",
              "pie²"
          );
      }

      // Metro lineal solo para productos con medida por longitud
      if (tieneMedidaProducto && tipoMedidaBaseProducto === "LONGITUD") {
          const unidadLongitud = opcionesBaseUnidadVenta.find(o =>
              o.tipo === "LONGITUD" ||
              normalizarPrecioTexto(`${o.nombre} ${o.abreviatura}`).includes("metro")
          );

          if (unidadLongitud) {
              agregarOpcionTipoVenta(
                  unidadLongitud.value,
                  "METRO_LINEAL",
                  "Metro lineal",
                  unidadLongitud.nombre || "Metro lineal",
                  unidadLongitud.abreviatura || "ml"
              );
          }
      }

      selectUnidadVenta.value = "";

      actualizarBloquesSegunUnidad();
      calcularDescuentoStock();
  }

   function agregarOpcionTipoVenta(value, tipo, texto, nombreUnidad, abreviatura) {
       if (!selectUnidadVenta || !value) return;

       const option = document.createElement("option");
       option.value = value;
       option.dataset.tipo = tipo;
       option.dataset.nombre = nombreUnidad || texto;
       option.dataset.abreviatura = abreviatura || "";
       option.textContent = texto;

       selectUnidadVenta.appendChild(option);
   }

    function buscarPrimeraUnidadCantidad(opcionesBaseUnidadVenta) {
        const unidadCantidad = opcionesBaseUnidadVenta.find(o => o.tipo === "CANTIDAD");
        return unidadCantidad ? unidadCantidad.value : "";
    }

    function cargarMedidasBaseProducto(formaVentaProducto,
                                       medidaTotalProducto,
                                       largoBaseProducto,
                                       anchoBaseProducto) {
        if (formaVentaProducto === "AREA") {
            if (inputLargoBase) inputLargoBase.value = largoBaseProducto || "";
            if (inputAnchoBase) inputAnchoBase.value = anchoBaseProducto || "";
            if (inputMedidaTotalBase) inputMedidaTotalBase.value = medidaTotalProducto || "";
        }

        if (formaVentaProducto === "LONGITUD") {
            if (inputLargoTotalBase) inputLargoTotalBase.value = medidaTotalProducto || largoBaseProducto || "";
            if (inputMedidaTotalLongitud) inputMedidaTotalLongitud.value = medidaTotalProducto || largoBaseProducto || "";
        }
    }

   function validarDescuentoMaximoPrecioModal() {
       ocultarAlertaPrecio();

       const precioFinalConIgv = Number(inputPrecioVenta?.value || 0);
       const descuentoFinalConIgv = Number(inputDescuentoMaximo?.value || 0);
       const costoBaseSinIgv = obtenerCostoBasePresentacionModal();
       const esRemate = inputModoRemate?.checked === true;

       inputPrecioVenta?.classList.remove("is-invalid");
       inputDescuentoMaximo?.classList.remove("is-invalid");

       if (precioFinalConIgv < 0) {
           inputPrecioVenta?.classList.add("is-invalid");
           mostrarAlertaPrecio("El precio final con IGV no puede ser negativo.");
           return false;
       }

       if (descuentoFinalConIgv < 0) {
           inputDescuentoMaximo?.classList.add("is-invalid");
           mostrarAlertaPrecio("El descuento maximo no puede ser negativo.");
           return false;
       }

       if (precioFinalConIgv > 0 && descuentoFinalConIgv > precioFinalConIgv) {
           inputDescuentoMaximo?.classList.add("is-invalid");
           mostrarAlertaPrecio(
               `El descuento maximo no puede ser mayor al precio final. Maximo permitido: S/ ${precioFinalConIgv.toFixed(2)}.`
           );
           return false;
       }

       if (esRemate) {
           if (descuentoFinalConIgv > 0) {
               inputDescuentoMaximo?.classList.add("is-invalid");
               mostrarAlertaPrecio("En modo remate, el descuento maximo debe ser S/ 0.00.");
               return false;
           }

           return true;
       }

       if (costoBaseSinIgv > 0 && precioFinalConIgv > 0) {
           const baseVentaSinIgv = obtenerBaseSinIgv(precioFinalConIgv);
           const gananciaNetaSinIgv = baseVentaSinIgv - costoBaseSinIgv;

           if (gananciaNetaSinIgv <= 0 && descuentoFinalConIgv > 0) {
               inputDescuentoMaximo?.classList.add("is-invalid");
               mostrarAlertaPrecio("Este precio no deja ganancia neta disponible para aplicar descuento.");
               return false;
           }

           const limiteTrabajadorFinalConIgv =
               gananciaNetaSinIgv * 0.50 * FACTOR_IGV_PRECIO;

           if (descuentoFinalConIgv > limiteTrabajadorFinalConIgv) {
               inputDescuentoMaximo?.classList.add("is-invalid");
               mostrarAlertaPrecio(
                   `El descuento maximo para trabajadores no puede superar el 50% de la ganancia. Maximo permitido: S/ ${limiteTrabajadorFinalConIgv.toFixed(2)}.`
               );
               return false;
           }
       }

       return true;
   }

    function obtenerCostoBasePresentacionModal() {
        calcularDescuentoStock(false);

        const costoPromedioProducto = Number(inputCostoPromedioModal?.value || 0);
        const factor = Number(inputFactorConversionStock?.value || 0);

        if (costoPromedioProducto <= 0 || factor <= 0) {
            return 0;
        }

        return costoPromedioProducto * factor;
    }

   function calcularPrecioDesdeGananciaModal() {
       if (inputModoRemate?.checked) {
           return;
       }

       const costoBaseSinIgv = obtenerCostoBasePresentacionModal();
       const ganancia = Number(inputGananciaModal?.value || 0);

       if (costoBaseSinIgv <= 0 || ganancia < 0) {
           return;
       }

       const baseVentaSinIgv = costoBaseSinIgv + (costoBaseSinIgv * ganancia / 100);
       const precioFinalConIgv = obtenerPrecioFinalConIgv(baseVentaSinIgv);

       if (inputPrecioVenta) {
           inputPrecioVenta.value = precioFinalConIgv.toFixed(2);
       }
   }

   function calcularGananciaDesdePrecioModal() {
       if (inputModoRemate?.checked) {
           return;
       }

       const costoBaseSinIgv = obtenerCostoBasePresentacionModal();
       const precioFinalConIgv = Number(inputPrecioVenta?.value || 0);

       if (costoBaseSinIgv <= 0 || precioFinalConIgv <= 0) {
           return;
       }

       const baseVentaSinIgv = obtenerBaseSinIgv(precioFinalConIgv);
       const porcentajeGanancia = ((baseVentaSinIgv - costoBaseSinIgv) / costoBaseSinIgv) * 100;

       if (inputGananciaModal) {
           inputGananciaModal.value = porcentajeGanancia.toFixed(2);
       }
   }

    function actualizarResumenDescuentoModal() {
        const precioFinalConIgv = Number(inputPrecioVenta?.value || 0);
        const descuentoFinalConIgv = Number(inputDescuentoMaximo?.value || 0);
        const costoBaseSinIgv = obtenerCostoBasePresentacionModal();
        const esRemate = inputModoRemate?.checked === true;

        const baseVentaSinIgv = obtenerBaseSinIgv(precioFinalConIgv);
        const igvIncluido = obtenerIgvIncluido(precioFinalConIgv);

        if (textoDescuentoPorcentaje) {
            if (precioFinalConIgv > 0 && descuentoFinalConIgv > 0) {
                const porcentajeDescuento = (descuentoFinalConIgv / precioFinalConIgv) * 100;
                textoDescuentoPorcentaje.textContent =
                    `Equivale a ${porcentajeDescuento.toFixed(2)}% del precio final con IGV.`;
            } else {
                textoDescuentoPorcentaje.textContent =
                    "Equivale a 0.00% del precio final con IGV.";
            }
        }

        if (!textoLimiteDescuento) return;

        if (esRemate) {
            textoLimiteDescuento.textContent =
                "Modo remate activo: el descuento queda bloqueado en S/ 0.00.";
            return;
        }

        if (costoBaseSinIgv <= 0 || precioFinalConIgv <= 0) {
            textoLimiteDescuento.textContent =
                "Se calculara con costo sin IGV, base de venta, IGV y ganancia neta.";
            return;
        }

        const gananciaNetaSinIgv = baseVentaSinIgv - costoBaseSinIgv;
        const gananciaDisponibleFinalConIgv = gananciaNetaSinIgv * FACTOR_IGV_PRECIO;
        const limiteTrabajadorFinalConIgv = gananciaDisponibleFinalConIgv * 0.50;

        if (gananciaNetaSinIgv <= 0) {
            textoLimiteDescuento.textContent =
                `Base sin IGV: S/ ${baseVentaSinIgv.toFixed(2)} | IGV incluido: S/ ${igvIncluido.toFixed(2)} | No hay ganancia disponible.`;
            return;
        }

        textoLimiteDescuento.textContent =
            `Base sin IGV: S/ ${baseVentaSinIgv.toFixed(2)} | ` +
            `IGV incluido: S/ ${igvIncluido.toFixed(2)} | ` +
            `Ganancia neta: S/ ${gananciaNetaSinIgv.toFixed(2)} | ` +
            `Trabajador maximo: S/ ${limiteTrabajadorFinalConIgv.toFixed(2)}.`;
    }

   function controlarModoRemateModal() {
       const esRemate = inputModoRemate?.checked === true;
       const costoBaseSinIgv = obtenerCostoBasePresentacionModal();
       const precioFinalConIgv = Number(inputPrecioVenta?.value || 0);
       const baseVentaSinIgv = obtenerBaseSinIgv(precioFinalConIgv);

       // Primero limpiar estado visual
       bloqueMotivoPrecioBajoCosto?.classList.add("d-none");

       if (inputMotivoPrecioBajoCosto && !esRemate && baseVentaSinIgv >= costoBaseSinIgv) {
           inputMotivoPrecioBajoCosto.value = "";
       }

       if (inputGananciaModal) {
           inputGananciaModal.disabled = false;
       }

       if (inputDescuentoMaximo) {
           inputDescuentoMaximo.disabled = false;
       }

       // Caso 1: remate marcado manualmente
       if (esRemate) {
           if (inputGananciaModal) {
               inputGananciaModal.value = "";
               inputGananciaModal.disabled = true;
           }

           if (inputDescuentoMaximo) {
               inputDescuentoMaximo.value = "0.00";
               inputDescuentoMaximo.disabled = true;
           }

           bloqueMotivoPrecioBajoCosto?.classList.remove("d-none");
           return;
       }

       // Caso 2: precio realmente por debajo del costo
       if (costoBaseSinIgv > 0 && precioFinalConIgv > 0 && baseVentaSinIgv < costoBaseSinIgv) {
           bloqueMotivoPrecioBajoCosto?.classList.remove("d-none");
           return;
       }

       // Caso normal: hay ganancia o esta igual al costo
       bloqueMotivoPrecioBajoCosto?.classList.add("d-none");

       if (inputMotivoPrecioBajoCosto) {
           inputMotivoPrecioBajoCosto.value = "";
       }
   }


    function guardarPrecioProducto() {

        ocultarAlertaPrecio();
        calcularDescuentoStock();
        controlarModoRemateModal();
        actualizarResumenDescuentoModal();

        const idProducto = inputIdProducto.value;

        if (!idProducto) {
            mostrarAlertaPrecio("No se selecciono ningun producto.");
            return;
        }

       const tipoCalculo = inputTipoCalculoVenta?.value || "UNIDAD";

        const data = {
            idPrecioPresentacion: inputIdPrecioPresentacion.value || null,
            nombrePresentacion: inputNombrePresentacion.value.trim(),
            idUnidadVenta: selectUnidadVenta.value,
            tipoCalculoVenta: tipoCalculo,
            cantidadIncluida: inputCantidadIncluida.value,
            largoBase: obtenerLargoBaseParaGuardar(tipoCalculo),
            anchoBase: obtenerAnchoBaseParaGuardar(tipoCalculo),
            medidaTotalBase: obtenerMedidaTotalParaGuardar(tipoCalculo),
            factorConversionStock: inputFactorConversionStock.value,
            precioVenta: inputPrecioVenta.value,
            descuentoMaximo: inputDescuentoMaximo.value || 0,
           descripcionUso: inputDescripcionUso.value.trim(),
           motivoPrecioBajoCosto: inputMotivoPrecioBajoCosto?.value?.trim() || null
        };

        if (!data.nombrePresentacion) {
            mostrarAlertaPrecio("Ingrese el nombre de la presentacion.");
            inputNombrePresentacion.focus();
            return;
        }

        if (!data.idUnidadVenta) {
            mostrarAlertaPrecio("Seleccione la unidad de venta.");
            selectUnidadVenta.focus();
            return;
        }

        if (!data.cantidadIncluida || Number(data.cantidadIncluida) <= 0) {
            mostrarAlertaPrecio("La cantidad de venta base debe ser mayor a cero.");
            inputCantidadIncluida.focus();
            return;
        }

       if ((tipoCalculo === "UNIDAD" || tipoCalculo === "MEDIA" || tipoCalculo === "METRO_LINEAL")
           && !Number.isInteger(Number(data.cantidadIncluida))) {

           mostrarAlertaPrecio("Para Unidad, Mitad o Metro lineal, la cantidad incluida debe ser un numero entero. Ejemplo: 1, 2, 3 o 6.");
           inputCantidadIncluida.focus();
           return;
       }


        if (!data.factorConversionStock || Number(data.factorConversionStock) <= 0) {
            mostrarAlertaPrecio("No se pudo calcular cuanto descuenta del stock.");
            return;
        }

        if (data.precioVenta === "" || Number(data.precioVenta) < 0) {
            mostrarAlertaPrecio("El precio de venta no puede ser negativo.");
            inputPrecioVenta.focus();
            return;
        }
        if (!validarDescuentoMaximoPrecioModal()) {
            inputDescuentoMaximo.focus();
            return;
        }

       const costoBaseSinIgvGuardar = obtenerCostoBasePresentacionModal();
       const precioFinalConIgvGuardar = Number(inputPrecioVenta?.value || 0);
       const baseVentaSinIgvGuardar = obtenerBaseSinIgv(precioFinalConIgvGuardar);

       const requiereMotivoRemate =
           inputModoRemate?.checked === true ||
           (
               costoBaseSinIgvGuardar > 0 &&
               precioFinalConIgvGuardar > 0 &&
               baseVentaSinIgvGuardar < costoBaseSinIgvGuardar
           );

       if (requiereMotivoRemate && (!inputMotivoPrecioBajoCosto?.value || inputMotivoPrecioBajoCosto.value.trim() === "")) {
           mostrarAlertaPrecio("Ingrese el motivo del remate.");
           inputMotivoPrecioBajoCosto?.focus();
           return;
       }

       if (!requiereMotivoRemate && inputMotivoPrecioBajoCosto) {
           inputMotivoPrecioBajoCosto.value = "";
       }

        btnGuardar.disabled = true;
        btnGuardar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Guardando...`;

        fetch(`/interno/productos/${idProducto}/precios`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...obtenerHeadersCsrf()
            },
            body: JSON.stringify(data)
        })
            .then(async response => {
                const body = await response.json();

                if (!response.ok || !body.ok) {
                    throw new Error(body.mensaje || "No se pudo guardar el precio.");
                }

                limpiarFormularioPrecio();
                cargarPreciosProducto(idProducto);
            })
            .catch(error => {
                mostrarAlertaPrecio(error.message);

                const mensaje = (error.message || "").toLowerCase();

                if (mensaje.includes("motivo") || mensaje.includes("bajo costo")) {
                    bloqueMotivoPrecioBajoCosto?.classList.remove("d-none");
                    inputMotivoPrecioBajoCosto?.focus();
                }
            })
            .finally(() => {
                btnGuardar.disabled = false;
                btnGuardar.innerHTML = `<i class="bi bi-save me-1"></i> Guardar precio`;
            });
    }

    function cargarPreciosProducto(idProducto) {

        tabla.innerHTML = `
            <tr>
                <td colspan="10" class="text-center text-muted py-3">
                    Cargando precios...
                </td>
            </tr>
        `;

        fetch(`/interno/productos/${idProducto}/precios`)
            .then(async response => {
                if (!response.ok) {
                    const texto = await response.text();
                    console.error("Error HTTP cargando precios:", response.status, texto);
                    throw new Error("No se pudieron cargar los precios del producto.");
                }

                return response.json();
            })
            .then(precios => {

                if (!precios || precios.length === 0) {
                    tabla.innerHTML = `
                        <tr>
                            <td colspan="10" class="text-center text-muted py-3">
                                Este producto todavía no tiene presentaciones de venta.
                            </td>
                        </tr>
                    `;
                    return;
                }

                tabla.innerHTML = precios.map(p => {

                    const estadoTexto = p.estado === 1 ? "Activo" : "Inactivo";
                    const estadoClase = p.estado === 1 ? "text-bg-success" : "text-bg-secondary";
                    const nuevoEstado = p.estado === 1 ? 2 : 1;
                    const textoBotonEstado = p.estado === 1 ? "Desactivar" : "Activar";

                    const precioJson = encodeURIComponent(JSON.stringify(p));

                    let etiquetaControlPrecio = "";

                    if (p.precioBajoCosto) {
                        etiquetaControlPrecio = `
                            <span class="badge text-bg-danger ms-1">
                                Bajo costo
                            </span>
                        `;
                    } else if (p.margenBajo) {
                        etiquetaControlPrecio = `
                            <span class="badge text-bg-warning ms-1">
                                Margen bajo
                            </span>
                        `;
                    }

                    return `
                        <tr>
                           <td class="fw-semibold">
                               ${escaparHtml(p.nombrePresentacion)}
                               ${etiquetaControlPrecio}
                           </td>

                            <td>
                                <span class="badge text-bg-light border">
                                    ${escaparHtml(traducirTipoCalculo(p.tipoCalculoVenta))}
                                </span>
                            </td>

                            <td>${formatearNumero(p.cantidadIncluida)}</td>

                            <td class="fw-semibold">
                                S/ ${formatearNumero(p.precioVenta)}
                            </td>

                            <td>
                                ${
                                    Number(p.descuentoMaximo || 0) > 0
                                        ? `<span class="fw-semibold">S/ ${formatearNumero(p.descuentoMaximo)}</span>`
                                        : `<span class="text-muted">Sin descuento</span>`
                                }
                            </td>

                            <td>
                                ${
                                    p.estado === 1
                                        ? `<button type="button"
                                                   class="btn btn-sm btn-outline-success btn-oferta-presentacion"
                                                   data-precio="${precioJson}">
                                               <i class="bi bi-tags me-1"></i> Oferta
                                           </button>`
                                        : `<span class="text-muted">-</span>`
                                }
                            </td>

                            <td>
                                ${
                                    p.precioPortada
                                        ? `<span class="badge text-bg-primary">Portada</span>`
                                        : (
                                            p.estado === 1
                                                ? `<button type="button"
                                                           class="btn btn-sm btn-outline-primary btn-portada-precio-producto"
                                                           data-id="${p.idPrecioPresentacion}">
                                                       Marcar portada
                                                   </button>`
                                                : `<span class="text-muted">-</span>`
                                        )
                                }
                            </td>

                            <td class="presentacion-descripcion-celda">
                                <div id="descripcion-presentacion-${p.idPrecioPresentacion}"
                                     class="presentacion-descripcion">
                                    ${escaparHtml(p.descripcionUso || "-")}
                                </div>
                                ${
                                    String(p.descripcionUso || "").length > 90
                                        ? `<button type="button"
                                                   class="btn btn-link btn-sm p-0 mt-1 btn-expandir-descripcion-presentacion"
                                                   aria-expanded="false"
                                                   aria-controls="descripcion-presentacion-${p.idPrecioPresentacion}">
                                               Ver m&aacute;s
                                           </button>`
                                        : ""
                                }
                            </td>

                            <td>
                                <span class="badge ${estadoClase}">
                                    ${estadoTexto}
                                </span>
                            </td>

                            <td class="text-end">
                                <button type="button"
                                        class="btn btn-sm btn-outline-primary me-1 btn-editar-precio-producto"
                                        data-precio="${precioJson}">
                                    Editar
                                </button>

                                <button type="button"
                                        class="btn btn-sm btn-outline-secondary btn-estado-precio-producto"
                                        data-id="${p.idPrecioPresentacion}"
                                        data-estado="${nuevoEstado}">
                                    ${textoBotonEstado}
                                </button>
                            </td>
                        </tr>
                    `;
                }).join("");

                activarBotonesTablaPrecios();
            })
            .catch(error => {
                console.error("Error cargando precios del producto:", error);

                tabla.innerHTML = `
                    <tr>
                        <td colspan="10" class="text-center text-danger py-3">
                            No se pudieron cargar los precios del producto.
                        </td>
                    </tr>
                `;
            });
    }

    function activarBotonesTablaPrecios() {

        tabla.querySelectorAll(".btn-expandir-descripcion-presentacion").forEach(btn => {
            btn.addEventListener("click", () => {
                const descripcion = document.getElementById(btn.getAttribute("aria-controls"));
                if (!descripcion) return;

                const expandida = btn.getAttribute("aria-expanded") === "true";
                btn.setAttribute("aria-expanded", String(!expandida));
                descripcion.classList.toggle("is-expanded", !expandida);
                btn.textContent = expandida ? "Ver m\u00e1s" : "Ver menos";
            });
        });

        tabla.querySelectorAll(".btn-oferta-presentacion").forEach(btn => {
            btn.addEventListener("click", () => {
                const precio = JSON.parse(decodeURIComponent(btn.dataset.precio));
                abrirModalOfertaPresentacion(precio, btn);
            });
        });

        tabla.querySelectorAll(".btn-editar-precio-producto").forEach(btn => {
            btn.addEventListener("click", () => {

                const precio = JSON.parse(decodeURIComponent(btn.dataset.precio));

                inputIdPrecioPresentacion.value = precio.idPrecioPresentacion || "";
                inputNombrePresentacion.value = precio.nombrePresentacion || "";
               seleccionarTipoVentaGuardado(precio.idUnidadVenta, precio.tipoCalculoVenta);
                inputCantidadIncluida.value = precio.cantidadIncluida || "1";

                if (inputTipoCalculoVenta) {
                    inputTipoCalculoVenta.value = precio.tipoCalculoVenta || "CANTIDAD";
                }

                inputPrecioVenta.value = precio.precioVenta || "";
                inputDescuentoMaximo.value = precio.descuentoMaximo || "0.00";
                inputDescripcionUso.value = precio.descripcionUso || "";

                if (inputMotivoPrecioBajoCosto) {
                    inputMotivoPrecioBajoCosto.value = precio.motivoPrecioBajoCosto || "";
                }

               if (inputModoRemate) {
                   inputModoRemate.checked = Boolean(precio.precioBajoCosto);
               }

               if (precio.precioBajoCosto) {
                   if (inputGananciaModal) {
                       inputGananciaModal.value = "";
                       inputGananciaModal.disabled = true;
                   }

                   if (inputDescuentoMaximo) {
                       inputDescuentoMaximo.value = "0.00";
                       inputDescuentoMaximo.disabled = true;
                   }

                   bloqueMotivoPrecioBajoCosto?.classList.remove("d-none");
               } else {
                   if (inputGananciaModal) {
                       inputGananciaModal.disabled = false;
                   }

                   if (inputDescuentoMaximo) {
                       inputDescuentoMaximo.disabled = false;
                   }

                   bloqueMotivoPrecioBajoCosto?.classList.add("d-none");
               }

                if (inputLargoBase) inputLargoBase.value = precio.largoBase || "";
                if (inputAnchoBase) inputAnchoBase.value = precio.anchoBase || "";
                if (inputMedidaTotalBase) inputMedidaTotalBase.value = precio.medidaTotalBase || "";

                if (inputLargoTotalBase) {
                    inputLargoTotalBase.value = precio.largoBase || precio.medidaTotalBase || "";
                }

                if (inputMedidaTotalLongitud) {
                    inputMedidaTotalLongitud.value = precio.medidaTotalBase || "";
                }

                if (inputFactorConversionStock) {
                    inputFactorConversionStock.value = precio.factorConversionStock || "1.000000";
                }

             actualizarBloquesSegunUnidad(false);
             calcularDescuentoStock(false);

             if (!precio.precioBajoCosto) {
                 calcularGananciaDesdePrecioModal();
             }

             controlarModoRemateModal();
             validarDescuentoMaximoPrecioModal();
             actualizarResumenDescuentoModal();

             inputNombrePresentacion.focus();
            });
        });

        tabla.querySelectorAll(".btn-estado-precio-producto").forEach(btn => {
            btn.addEventListener("click", () => {

                const idPrecio = btn.dataset.id;
                const estado = btn.dataset.estado;
                const idProducto = inputIdProducto.value;

                fetch(`/interno/productos/precios/${idPrecio}/estado?estado=${estado}`, {
                    method: "POST",
                    headers: {
                        ...obtenerHeadersCsrf()
                    }
                })
                    .then(async response => {
                        const body = await response.json();

                        if (!response.ok || !body.ok) {
                            throw new Error(body.mensaje || "No se pudo actualizar el estado.");
                        }

                        cargarPreciosProducto(idProducto);
                    })
                    .catch(error => {
                        mostrarAlertaPrecio(error.message);
                    });
            });
        });

        tabla.querySelectorAll(".btn-portada-precio-producto").forEach(btn => {
            btn.addEventListener("click", () => {

                const idPrecio = btn.dataset.id;
                const idProducto = inputIdProducto.value;

                fetch(`/interno/productos/precios/${idPrecio}/portada`, {
                    method: "POST",
                    headers: {
                        ...obtenerHeadersCsrf()
                    }
                })
                    .then(async response => {
                        const body = await response.json();

                        if (!response.ok || !body.ok) {
                            throw new Error(body.mensaje || "No se pudo marcar el precio portada.");
                        }

                        cargarPreciosProducto(idProducto);
                    })
                    .catch(error => {
                        mostrarAlertaPrecio(error.message);
                    });
            });
        });
    }

    btnGuardarOferta?.addEventListener("click", guardarOfertaPresentacion);
    btnDesactivarOferta?.addEventListener("click", desactivarOfertaPresentacion);

    function abrirModalOfertaPresentacion(precio, trigger) {
        if (!modalOferta || !precio || !precio.idPrecioPresentacion) {
            mostrarAlertaPrecio("No se pudo identificar la presentacion.");
            return;
        }

        limpiarModalOfertaPresentacion();

        const precioNormal = Number(precio.precioVenta || 0);
        ofertaIdPrecioPresentacion.value = precio.idPrecioPresentacion;
        ofertaPrecioNormal.value = precioNormal.toFixed(2);
        ofertaPrecioNormalTexto.value = `S/ ${precioNormal.toFixed(2)}`;
        ofertaNombrePresentacion.textContent = precio.nombrePresentacion || "Presentacion";

        const hoyTexto = new Date().toISOString().split("T")[0];

        if (ofertaFechaInicio) {
            ofertaFechaInicio.min = hoyTexto;
        }

        if (ofertaFechaFin) {
            ofertaFechaFin.min = hoyTexto;
        }

        mostrarModalSeguro(modalOfertaEl, trigger);
        cargarOfertaPresentacion(precio.idPrecioPresentacion);
    }

    function limpiarModalOfertaPresentacion() {
        ocultarAlertaOferta();

        if (ofertaIdPrecioPresentacion) ofertaIdPrecioPresentacion.value = "";
        if (ofertaNombrePresentacion) ofertaNombrePresentacion.textContent = "Presentacion";
        if (ofertaPrecioNormal) ofertaPrecioNormal.value = "";
        if (ofertaPrecioNormalTexto) ofertaPrecioNormalTexto.value = "";
        if (ofertaPrecioOferta) ofertaPrecioOferta.value = "";
        if (ofertaFechaInicio) ofertaFechaInicio.value = "";
        if (ofertaFechaFin) ofertaFechaFin.value = "";
        if (ofertaTexto) ofertaTexto.value = "";
        if (ofertaActivo) ofertaActivo.checked = true;

        btnDesactivarOferta?.classList.add("d-none");
    }

    function cargarOfertaPresentacion(idPrecioPresentacion) {
        fetch(`/interno/productos/presentaciones/${idPrecioPresentacion}/oferta`)
            .then(async response => {
                const body = await response.json();

                if (!response.ok || !body.ok) {
                    throw new Error(body.mensaje || "No se pudo cargar la oferta.");
                }

                if (body.oferta) {
                    ofertaPrecioOferta.value = body.oferta.precioOferta || "";
                    ofertaFechaInicio.value = normalizarFechaOferta(body.oferta.fechaInicio);
                    ofertaFechaFin.value = normalizarFechaOferta(body.oferta.fechaFin);
                    ofertaTexto.value = body.oferta.textoOferta || "";
                    ofertaActivo.checked = body.oferta.activo !== false;
                    btnDesactivarOferta?.classList.remove("d-none");
                }
            })
            .catch(error => mostrarAlertaOferta(error.message));
    }

    function guardarOfertaPresentacion() {
        ocultarAlertaOferta();

        const idPrecioPresentacion = ofertaIdPrecioPresentacion?.value;
        const precioNormal = Number(ofertaPrecioNormal?.value || 0);
        const precioOferta = Number(ofertaPrecioOferta?.value || 0);
        const fechaInicio = ofertaFechaInicio?.value;
        const fechaFin = ofertaFechaFin?.value;

        if (!idPrecioPresentacion) {
            mostrarAlertaOferta("No se pudo identificar la presentacion.");
            return;
        }

        if (!precioOferta || precioOferta <= 0) {
            mostrarAlertaOferta("Ingrese un precio de oferta mayor a cero.");
            ofertaPrecioOferta?.focus();
            return;
        }

        if (precioNormal > 0 && precioOferta >= precioNormal) {
            mostrarAlertaOferta("El precio de oferta debe ser menor al precio normal.");
            ofertaPrecioOferta?.focus();
            return;
        }

        if (!fechaInicio) {
            mostrarAlertaOferta("Ingrese la fecha de inicio.");
            ofertaFechaInicio?.focus();
            return;
        }

        if (!fechaFin) {
            mostrarAlertaOferta("Ingrese la fecha de fin.");
            ofertaFechaFin?.focus();
            return;
        }

        const hoyTexto = new Date().toISOString().split("T")[0];

        if (fechaInicio < hoyTexto) {
            mostrarAlertaOferta("La fecha de inicio no puede ser anterior a la fecha actual.");
            ofertaFechaInicio?.focus();
            return;
        }

        if (fechaFin < hoyTexto) {
            mostrarAlertaOferta("La fecha de fin no puede ser anterior a la fecha actual.");
            ofertaFechaFin?.focus();
            return;
        }

        if (fechaFin < fechaInicio) {
            mostrarAlertaOferta("La fecha fin no puede ser anterior a la fecha inicio.");
            ofertaFechaFin?.focus();
            return;
        }

        const body = new URLSearchParams();
        body.append("precioOferta", precioOferta.toFixed(2));
        body.append("fechaInicio", fechaInicio);
        body.append("fechaFin", fechaFin);
        body.append("textoOferta", ofertaTexto?.value?.trim() || "");
        body.append("activo", ofertaActivo?.checked ? "true" : "false");

        btnGuardarOferta.disabled = true;
        btnGuardarOferta.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Guardando...`;

        fetch(`/interno/productos/presentaciones/${idPrecioPresentacion}/oferta`, {
            method: "POST",
            headers: {
                ...obtenerHeadersCsrf()
            },
            body
        })
            .then(async response => {
                const data = await response.json();

                if (!response.ok || !data.ok) {
                    throw new Error(data.mensaje || "No se pudo guardar la oferta.");
                }

                btnDesactivarOferta?.classList.toggle("d-none", ofertaActivo?.checked !== true);
                cerrarModalSeguro(modalOfertaEl);
            })
            .catch(error => mostrarAlertaOferta(error.message))
            .finally(() => {
                btnGuardarOferta.disabled = false;
                btnGuardarOferta.innerHTML = `<i class="bi bi-save me-1"></i> Guardar oferta`;
            });
    }

    function desactivarOfertaPresentacion() {
        ocultarAlertaOferta();

        const idPrecioPresentacion = ofertaIdPrecioPresentacion?.value;

        if (!idPrecioPresentacion) {
            mostrarAlertaOferta("No se pudo identificar la presentacion.");
            return;
        }

        btnDesactivarOferta.disabled = true;
        btnDesactivarOferta.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Desactivando...`;

        fetch(`/interno/productos/presentaciones/${idPrecioPresentacion}/oferta/desactivar`, {
            method: "POST",
            headers: {
                ...obtenerHeadersCsrf()
            }
        })
            .then(async response => {
                const data = await response.json();

                if (!response.ok || !data.ok) {
                    throw new Error(data.mensaje || "No se pudo desactivar la oferta.");
                }

                ofertaActivo.checked = false;
                btnDesactivarOferta?.classList.add("d-none");
                cerrarModalSeguro(modalOfertaEl);
            })
            .catch(error => mostrarAlertaOferta(error.message))
            .finally(() => {
                btnDesactivarOferta.disabled = false;
                btnDesactivarOferta.innerHTML = "Desactivar oferta";
            });
    }

    function mostrarAlertaOferta(mensaje) {
        if (!alertaOferta) return;
        alertaOferta.textContent = mensaje || "No se pudo procesar la oferta.";
        alertaOferta.classList.remove("d-none");
    }

    function ocultarAlertaOferta() {
        if (!alertaOferta) return;
        alertaOferta.textContent = "";
        alertaOferta.classList.add("d-none");
    }

    function normalizarFechaOferta(valor) {
        if (!valor) return "";

        if (Array.isArray(valor) && valor.length >= 3) {
            const [anio, mes, dia] = valor;
            return `${anio}-${String(mes).padStart(2, "0")}-${String(dia).padStart(2, "0")}`;
        }

        return String(valor).substring(0, 10);
    }

    function seleccionarTipoVentaGuardado(idUnidadVenta, tipoCalculoVenta) {
        if (!selectUnidadVenta) return;

        const opciones = Array.from(selectUnidadVenta.options);

        const opcionExacta = opciones.find(option =>
            String(option.value) === String(idUnidadVenta || "") &&
            String(option.dataset.tipo || "") === String(tipoCalculoVenta || "")
        );

        if (opcionExacta) {
            selectUnidadVenta.value = opcionExacta.value;
            opcionExacta.selected = true;
            return;
        }

        const opcionPorUnidad = opciones.find(option =>
            String(option.value) === String(idUnidadVenta || "")
        );

        if (opcionPorUnidad) {
            selectUnidadVenta.value = opcionPorUnidad.value;
            opcionPorUnidad.selected = true;
        } else {
            selectUnidadVenta.value = "";
        }
    }

   function actualizarBloquesSegunUnidad(limpiarMedidas = true) {

       const unidad = obtenerUnidadSeleccionada();
       const tipo = unidad.tipoCalculo || "UNIDAD";

       if (inputCantidadIncluida) {
           if (tipo === "UNIDAD" || tipo === "MEDIA" || tipo === "METRO_LINEAL") {
               inputCantidadIncluida.step = "1";
               inputCantidadIncluida.min = "1";
           } else if (tipo === "PIE2") {
               inputCantidadIncluida.step = "1";
               inputCantidadIncluida.min = "1";
           } else {
               inputCantidadIncluida.step = "0.01";
               inputCantidadIncluida.min = "0.01";
           }
       }

       if (inputTipoCalculoVenta) {
           inputTipoCalculoVenta.value = tipo;
       }

       if (ayudaUnidadVenta) {
           if (tipo === "UNIDAD") {
               ayudaUnidadVenta.textContent = "Descuenta unidades completas del inventario. Ej: caja x100 descuenta 100 unidades.";
          } else if (tipo === "MEDIA") {
              ayudaUnidadVenta.textContent = "Descuenta media unidad física. Ej: media plancha o media varilla descuenta 0.5.";
           } else if (tipo === "PIE2") {
               ayudaUnidadVenta.textContent = "Descuenta por pie cuadrado usando piezas reales del inventario.";
           } else if (tipo === "METRO_LINEAL") {
               ayudaUnidadVenta.textContent = "Descuenta por metros enteros vendidos según los metros vendibles de la unidad física.";
           }
       }

       if (textoAyudaCalculo) {
           textoAyudaCalculo.textContent = "El cálculo se hace según el tipo de venta seleccionado.";
       }

       calcularDescuentoStock();
   }


 function calcularDescuentoStock(actualizarMensaje = true) {

     const tipo = inputTipoCalculoVenta?.value || "UNIDAD";
     const cantidad = Number(inputCantidadIncluida.value || 0);

     if (cantidad <= 0) {
         inputFactorConversionStock.value = "";
         return;
     }

     let factor = 0;

     if (tipo === "UNIDAD") {
         factor = cantidad;

     } else if (tipo === "MEDIA") {
         factor = cantidad * 0.5;

     } else if (tipo === "PIE2") {
         if (!pie2PorUnidadFisicaProducto || pie2PorUnidadFisicaProducto <= 0) {
             inputFactorConversionStock.value = "";
             actualizarTextoResultado("Este producto no tiene medida vendible configurada para vender por pie².");
             return;
         }

         factor = cantidad / pie2PorUnidadFisicaProducto;

     } else if (tipo === "METRO_LINEAL") {
         if (!metroLinealPorUnidadFisicaProducto || metroLinealPorUnidadFisicaProducto <= 0) {
             inputFactorConversionStock.value = "";
             actualizarTextoResultado("Este producto no tiene metros vendibles configurados.");
             return;
         }

         factor = cantidad / metroLinealPorUnidadFisicaProducto;

     } else {
         factor = cantidad;
     }

     inputFactorConversionStock.value = factor.toFixed(6);

     if (actualizarMensaje) {
         const unidadStock = inputUnidadStockProducto?.value || "inventario";

         if (tipo === "UNIDAD") {
             actualizarTextoResultado(
                 `Esta presentación descontará ${factor.toFixed(6)} unidad(es) físicas del inventario.`
             );

         } else if (tipo === "MEDIA") {
             actualizarTextoResultado(
                 `${cantidad} mitad(es) descontará ${factor.toFixed(6)} de ${unidadStock}.`
             );

         } else if (tipo === "PIE2") {
             actualizarTextoResultado(
                 `${cantidad} pie² descontará ${factor.toFixed(6)} de una plancha. Base: ${pie2PorUnidadFisicaProducto} pie² por plancha.`
             );

         } else if (tipo === "METRO_LINEAL") {
             actualizarTextoResultado(
                 `${cantidad} metro(s) lineales descontará ${factor.toFixed(6)} del inventario. Base: ${metroLinealPorUnidadFisicaProducto} ml por unidad física.`
             );
         }
     }
 }

   function sugerirNombreYDescripcion() {

       const unidad = obtenerUnidadSeleccionada();
       const tipo = unidad.tipoCalculo || "UNIDAD";

       if (!inputNombrePresentacion.value.trim()) {
           if (tipo === "UNIDAD") {
               inputNombrePresentacion.value = "Presentacion por unidad";
           } else if (tipo === "MEDIA") {
               inputNombrePresentacion.value = tipoMedidaBaseActualProducto === "LONGITUD"
                   ? "Media varilla"
                   : "Media plancha";
           } else if (tipo === "PIE2") {
               inputNombrePresentacion.value = "Pie cuadrado";
           } else if (tipo === "METRO_LINEAL") {
               inputNombrePresentacion.value = "Metro lineal";
           }
       }

       if (!inputDescripcionUso.value.trim()) {
           if (tipo === "UNIDAD") {
               inputDescripcionUso.value = "Venta por unidades completas o presentacion comercial";
           } else if (tipo === "MEDIA") {
               inputDescripcionUso.value = tipoMedidaBaseActualProducto === "LONGITUD"
                   ? "Venta por media varilla"
                   : "Venta por media plancha";
           } else if (tipo === "PIE2") {
               inputDescripcionUso.value = "Venta por pie cuadrado";
           } else if (tipo === "METRO_LINEAL") {
               inputDescripcionUso.value = "Venta por metro lineal";
           }
       }
   }

    function limpiarFormularioPrecio() {
        inputIdPrecioPresentacion.value = "";
        inputNombrePresentacion.value = "";
        selectUnidadVenta.value = "";
        inputCantidadIncluida.value = "1";

        if (inputTipoCalculoVenta) inputTipoCalculoVenta.value = "UNIDAD";

        if (inputLargoBase) inputLargoBase.value = "";
        if (inputAnchoBase) inputAnchoBase.value = "";
        if (inputMedidaTotalBase) inputMedidaTotalBase.value = "";

        if (inputLargoTotalBase) inputLargoTotalBase.value = "";
        if (inputMedidaTotalLongitud) inputMedidaTotalLongitud.value = "";

       inputFactorConversionStock.value = "1.000000";
        inputPrecioVenta.value = "";
        inputDescuentoMaximo.value = "0.00";
        inputDescripcionUso.value = "";

       if (inputModoRemate) {
           inputModoRemate.checked = false;
       }

       if (inputGananciaModal) {
           inputGananciaModal.disabled = false;
       }

       if (inputDescuentoMaximo) {
           inputDescuentoMaximo.disabled = false;
       }

       if (inputMotivoPrecioBajoCosto) {
           inputMotivoPrecioBajoCosto.value = "";
       }

       bloqueMotivoPrecioBajoCosto?.classList.add("d-none");
       actualizarResumenDescuentoModal();

        bloqueCalculoArea?.classList.add("d-none");
        bloqueCalculoLongitud?.classList.add("d-none");

        if (ayudaUnidadVenta) {
          ayudaUnidadVenta.textContent = "Selecciona el tipo de venta de esta presentacion.";
        }

        actualizarTextoResultado("Selecciona una forma de venta para calcular el descuento del inventario.");
        ocultarAlertaPrecio();
    }

    function obtenerUnidadSeleccionada() {
        const option = selectUnidadVenta?.options[selectUnidadVenta.selectedIndex];

        if (!option || !option.value) {
           return {
               id: "",
               tipoCalculo: "UNIDAD",
               nombre: "",
               abreviatura: ""
           };
        }

        return {
            id: option.value,
            tipoCalculo: option.dataset.tipo || "UNIDAD",
            nombre: option.dataset.nombre || option.textContent.trim(),
            abreviatura: option.dataset.abreviatura || ""
        };
    }

    function obtenerLargoBaseParaGuardar(tipo) {
        if (tipo === "AREA") {
            return inputLargoBase?.value || null;
        }

        if (tipo === "LONGITUD") {
            return inputLargoTotalBase?.value || null;
        }

        return null;
    }

    function obtenerAnchoBaseParaGuardar(tipo) {
        if (tipo === "AREA") {
            return inputAnchoBase?.value || null;
        }

        return null;
    }

    function obtenerMedidaTotalParaGuardar(tipo) {
        if (tipo === "AREA") {
            return inputMedidaTotalBase?.value || null;
        }

        if (tipo === "LONGITUD") {
            return inputMedidaTotalLongitud?.value || inputLargoTotalBase?.value || null;
        }

        return null;
    }

    function esMetroCuadrado(unidad) {
        const texto = normalizarPrecioTexto(`${unidad.nombre} ${unidad.abreviatura}`);

        return texto.includes("metro cuadrado")
            || texto.includes("m2")
            || texto.includes("m²")
            || unidad.tipoCalculo === "AREA";
    }

    function esMetroLineal(unidad) {
        const texto = normalizarPrecioTexto(`${unidad.nombre} ${unidad.abreviatura}`);

        return !esMetroCuadrado(unidad)
            && (
                texto === "metro"
                || texto.includes("metro lineal")
                || texto.includes(" m ")
                || unidad.tipoCalculo === "LONGITUD"
            );
    }

    function esCajaOPaquete(unidad) {
        const texto = normalizarPrecioTexto(`${unidad.nombre} ${unidad.abreviatura}`);

        return texto.includes("caja")
            || texto.includes("cajon")
            || texto.includes("paquete")
            || texto.includes("pack");
    }

    function normalizarPrecioTexto(valor) {
        return String(valor || "")
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .trim();
    }

   function traducirTipoCalculo(tipo) {
       if (tipo === "UNIDAD") return "Unidad";
       if (tipo === "MEDIA") return "Media";
       if (tipo === "PIE2") return "Pie²";
       if (tipo === "METRO_LINEAL") return "Metro lineal";

       if (tipo === "CANTIDAD") return "Unidad";
       if (tipo === "AREA") return "Pie²";
       if (tipo === "LONGITUD") return "Metro lineal";

       return "Unidad";
   }

    function actualizarTextoResultado(mensaje) {
        if (textoResultadoStock) {
            textoResultadoStock.textContent = mensaje;
        }
    }

    function mostrarAlertaPrecio(mensaje) {
        alerta.textContent = mensaje;
        alerta.classList.remove("d-none");
    }

    function ocultarAlertaPrecio() {
        alerta.textContent = "";
        alerta.classList.add("d-none");
    }

    function formatearNumero(valor) {
        const numero = Number(valor || 0);
        return numero.toFixed(2);
    }

    function formatearNumero6(valor) {
        const numero = Number(valor || 0);
        return numero.toFixed(6);
    }

});
