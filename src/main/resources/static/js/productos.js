// productos.js
document.addEventListener("DOMContentLoaded", () => {
    const modalProductoEl = document.getElementById("modalProducto");
    const modalEstadoEl = document.getElementById("modalEstadoProducto");
    const modalEliminarEl = document.getElementById("modalConfirmEliminar");

    const modalProducto = modalProductoEl ? new bootstrap.Modal(modalProductoEl) : null;
    const modalEstado = modalEstadoEl ? new bootstrap.Modal(modalEstadoEl) : null;
    const modalEliminar = modalEliminarEl ? new bootstrap.Modal(modalEliminarEl) : null;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    let idEstadoSeleccionado = null;
    let nuevoEstadoSeleccionado = null;
    let formEliminarSeleccionado = null;

    const textoEstadoProducto = document.getElementById("textoEstadoProducto");
    const btnConfirmarEstado = document.getElementById("btnConfirmarEstado");

    const textoEliminarProducto = document.getElementById("textoEliminarProducto");
    const btnConfirmarEliminar = document.getElementById("btnConfirmarEliminar");

    const formProducto = document.getElementById("formProducto");

    /* =========================
       Funciones auxiliares
    ============================ */

    // 1 = Aluminio  -> Unidad (2)
    // 2 = Vidrio    -> Plancha (4)
    // 3 = Accesorio -> Unidad (2)
    function aplicarUnidadPorTipo() {
        const tipoSelect = document.getElementById("idTipoProducto");
        const unidadSelect = document.getElementById("idUnidad");
        if (!tipoSelect || !unidadSelect) return;

        const tipo = parseInt(tipoSelect.value, 10);

        if (isNaN(tipo)) {
            unidadSelect.value = "";
            unidadSelect.disabled = true;
            return;
        }

        let idUnidad;
        if (tipo === 2) {              // Vidrio
            idUnidad = 4;              // Plancha
        } else if (tipo === 1 || tipo === 3) { // Aluminio / Accesorio
            idUnidad = 2;              // Unidad
        } else {
            unidadSelect.value = "";
            unidadSelect.disabled = true;
            return;
        }

        unidadSelect.value = String(idUnidad);
        unidadSelect.disabled = true; // no permitir que lo cambien
    }

    function limpiarFormulario() {
        if (!formProducto) return;

        formProducto.reset();
        formProducto.classList.remove("was-validated");

        const idInput = document.getElementById("idProducto");
        const previewImg = document.getElementById("previewImg");
        const stockActualInput = document.getElementById("stockActual");

        if (idInput) idInput.value = "";
        if (previewImg) previewImg.src = "/img/productos/default.png";
        if (stockActualInput) stockActualInput.value = 0; // nuevo producto siempre empieza en 0

        // Deshabilitar/limpiar unidad hasta que elijan tipo
        aplicarUnidadPorTipo();
    }

    // Exponer para Thymeleaf (onchange en select de tipo y file input)
    window.generarCodigo = function () {
        const tipoSelect = document.getElementById("idTipoProducto");
        const codigoInput = document.getElementById("codigoInterno");
        if (!tipoSelect || !codigoInput) return;

        const tipo = tipoSelect.value;

        // Primero aplicamos la regla Unidad <- Tipo
        aplicarUnidadPorTipo();

        if (!tipo) return;

        fetch(`/productos/api/codigo?tipo=${encodeURIComponent(tipo)}`)
            .then(r => r.text())
            .then(codigo => {
                codigoInput.value = codigo;
            })
            .catch(err => console.error("Error generando código:", err));
    };

    window.previewImagen = function (event) {
        const img = document.getElementById("previewImg");
        if (!img || !event.target.files?.length) return;
        img.src = URL.createObjectURL(event.target.files[0]);
    };

    /* =========================
       Nuevo / Editar producto
    ============================ */

    const btnNuevo = document.getElementById("btnNuevoProducto");
    if (btnNuevo && modalProducto) {
        btnNuevo.addEventListener("click", () => {
            limpiarFormulario();
            const titulo = document.getElementById("tituloModalProducto");
            if (titulo) titulo.textContent = "Nuevo Producto";
            modalProducto.show();
        });
    }
    function activarTooltips() {
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => {
            new bootstrap.Tooltip(el);
        });
    }

    // Activar tooltips en la carga inicial
    activarTooltips();
function abrirEditarProducto(id) {
    if (!modalProducto) return;

    fetch(`/productos/api/${id}`)
        .then(r => {
            if (!r.ok) throw new Error("No encontrado");
            return r.json();
        })
        .then(data => {

            const p = data.producto;
            const ultimo = data.ultimoPrecioCompra ?? 0;

            document.getElementById("ultimoPrecioCompra").value = ultimo;

            const titulo = document.getElementById("tituloModalProducto");
            if (titulo) titulo.textContent = "Editar Producto";

            document.getElementById("idProducto").value = p.idProducto;
            document.getElementById("codigoInterno").value = p.codigoInterno;

            const tipoSelect = document.getElementById("idTipoProducto");
            if (tipoSelect) {
                tipoSelect.value = p.idTipoProducto;
                aplicarUnidadPorTipo();
            }

            const estadoSelect = document.getElementById("idEstadoProducto");
            if (estadoSelect) estadoSelect.value = p.idEstadoProducto;

            document.getElementsByName("nombreProducto")[0].value = p.nombreProducto ?? "";
            document.getElementsByName("dimensiones")[0].value = p.dimensiones ?? "";
            document.getElementsByName("precioUnitario")[0].value = p.precioUnitario ?? 0;
            document.getElementsByName("stockActual")[0].value = p.stockActual ?? 0;
            document.getElementsByName("stockMinimo")[0].value = p.stockMinimo ?? 0;

            const descInput = document.getElementById("descuentoMaximo");
            if (descInput) descInput.value = p.descuentoMaximo ?? 0;

            const precioVentaInput = document.getElementById("precioVenta");
            const ultimoPrecioCompra = document.getElementById("ultimoPrecioCompra");

            // 🔥 VALIDACIÓN CORRECTA: descuento <= ganancia (precioVenta - costo)
            function validarDescuentoPrecio() {
                let costo = parseFloat(ultimoPrecioCompra.value || 0);
                let precioVenta = parseFloat(precioVentaInput.value || 0);
                let desc = parseFloat(descInput.value || 0);

                let ganancia = precioVenta - costo;

                if (ganancia <= 0) {
                    descInput.value = 0;
                    return;
                }

                if (desc > ganancia) {
                    descInput.value = ganancia.toFixed(2);

                    if (typeof Swal !== "undefined") {
                        Swal.fire(
                            "Descuento inválido",
                            `El descuento máximo permitido es S/ ${ganancia.toFixed(2)} (no puedes vender bajo costo).`,
                            "warning"
                        );
                    } else {
                        console.warn("SweetAlert2 no está cargado.");
                    }
                }
            }

            // Activar validación
            if (descInput && precioVentaInput && ultimoPrecioCompra) {
                validarDescuentoPrecio();
                descInput.addEventListener("input", validarDescuentoPrecio);
                precioVentaInput.addEventListener("input", validarDescuentoPrecio);
            }

            // Imagen antes del modal
            const previewImg = document.getElementById("previewImg");
            if (previewImg) {
                previewImg.src = p.foto
                    ? `/img/productos/${p.foto}`
                    : "/img/productos/default.png";
            }

            // Info de último precio antes del modal
            const info = document.getElementById("precioCompraInfo");
            if (info) {
                if (ultimo != null) {
                    info.textContent = `Último precio de compra equivalente: S/ ${ultimo}`;
                    info.style.color = "#6c757d";
                } else {
                    info.textContent = "Este producto aún no tiene compras registradas.";
                }
            }

            formProducto.classList.remove("was-validated");
            modalProducto.show();
        })
        .catch(err => {
            console.error(err);
            alert("No se pudo cargar el producto.");
        });
}


   // =========================
   // BOTÓN EDITAR — ABRIR MODAL
   // =========================
   document.querySelectorAll(".btn-editar").forEach(btn => {
       btn.addEventListener("click", () => {
           const id = btn.getAttribute("data-id");
           console.log("CLICK EDITAR → ID:", id); // Para depurar
           if (id) abrirEditarProducto(id);
       });
   });


    /* =========================
       Activar / Desactivar
    ============================ */

    document.querySelectorAll(".btn-estado").forEach(btn => {
        btn.addEventListener("click", () => {
            if (!modalEstado) return;

            const id = btn.getAttribute("data-id");
            const estadoActual = parseInt(btn.getAttribute("data-estado"), 10);
            const nombre = btn.getAttribute("data-nombre") || "";

            const activar = (estadoActual !== 1);
            idEstadoSeleccionado = id;
            nuevoEstadoSeleccionado = activar ? 1 : 2;

            if (textoEstadoProducto) {
                textoEstadoProducto.textContent = activar
                    ? `¿Deseas ACTIVAR el producto "${nombre}"?`
                    : `¿Deseas DESACTIVAR el producto "${nombre}"?`;
            }

            modalEstado.show();
        });
    });

    if (btnConfirmarEstado) {
        btnConfirmarEstado.addEventListener("click", () => {
            if (!idEstadoSeleccionado || !nuevoEstadoSeleccionado) {
                modalEstado.hide();
                return;
            }

            const url = `/productos/${idEstadoSeleccionado}/estado`;
            const body = `estado=${encodeURIComponent(nuevoEstadoSeleccionado)}`;

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
                .then(r => {
                    if (!r.ok) throw new Error("Error al cambiar el estado");
                    return r.json();
                })
                .then(data => {
                    const badge = document.getElementById(`estado-text-${data.id}`);
                    if (badge) {
                        badge.textContent = data.estadoTexto;
                        badge.classList.remove("bg-success", "bg-secondary");
                        if (data.estado === 1) {
                            badge.classList.add("bg-success");
                        } else {
                            badge.classList.add("bg-secondary");
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

                    modalEstado.hide();
                })
                .catch(err => {
                    console.error(err);
                    alert("No se pudo cambiar el estado del producto.");
                    modalEstado.hide();
                });
        });
    }

    /* =========================
       Eliminar con modal
    ============================ */

    document.querySelectorAll(".btn-eliminar").forEach(btn => {
        btn.addEventListener("click", () => {
            if (!modalEliminar) return;
            formEliminarSeleccionado = btn.closest("form");
            const nombre = btn.getAttribute("data-nombre") || "";

            if (textoEliminarProducto) {
                textoEliminarProducto.textContent =
                    `¿Seguro que deseas eliminar el producto "${nombre}"?`;
            }

            modalEliminar.show();
        });
    });

    if (btnConfirmarEliminar) {
        btnConfirmarEliminar.addEventListener("click", () => {
            if (formEliminarSeleccionado) {
                formEliminarSeleccionado.submit();
            }
        });
    }

    /* =========================
       Buscador en tiempo real
    ============================ */
    const buscador = document.getElementById("buscadorProductos");
    if (buscador) {
        buscador.addEventListener("input", function () {
            const filtro = this.value.toLowerCase();
            document
                .querySelectorAll("#tablaProductos tbody tr")
                .forEach(fila => {
                    fila.style.display =
                        fila.innerText.toLowerCase().includes(filtro) ? "" : "none";
                });
        });
    }

    /* =========================
       Validación Bootstrap
    ============================ */
    if (formProducto) {
        formProducto.addEventListener("submit", event => {
            if (!formProducto.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            formProducto.classList.add("was-validated");
        }, false);
    }

 // Al cargar la página (por si el form vino con errores y datos cargados)
 aplicarUnidadPorTipo();

 /* =====================================================
    REABRIR MODAL AUTOMÁTICAMENTE SI HAY ERRORES
    (enviado desde el controller con FlashAttributes)
 ====================================================== */
 const modalError = document.body.getAttribute("data-modal-error");
 if (modalError === "true" && modalProducto && formProducto) {

     console.log("Reabriendo modal por errores...");

     // Ajustar título según si es nuevo o edición
     const titulo = document.getElementById("tituloModalProducto");
     const idHidden = document.getElementById("idProducto");
     if (titulo && idHidden) {
         const esEdicion = idHidden.value && idHidden.value !== "0";
         titulo.textContent = esEdicion ? "Editar Producto" : "Nuevo Producto";
     }

     // Asegurar que la unidad se corresponda con el tipo
     aplicarUnidadPorTipo();

     // Mostrar modal con los datos que Thymeleaf ya rellenó
     modalProducto.show();
 }
 // ========= Cálculo automático % ↔ precio =========
 const precioVentaInput = document.getElementById("precioVenta");
 const porcentajeInput = document.getElementById("porcentajeGanancia");
 const ultimoPrecioInput = document.getElementById("ultimoPrecioCompra");

 // Verificar que existan en el DOM (evita errores)
 if (precioVentaInput && porcentajeInput && ultimoPrecioInput) {

     // Porcentaje → Precio venta
     porcentajeInput.addEventListener("input", () => {
         const base = parseFloat(ultimoPrecioInput.value || 0);
         const porcentaje = parseFloat(porcentajeInput.value || 0);
         if (base > 0) {
             const precio = base + (base * porcentaje / 100);
             precioVentaInput.value = precio.toFixed(2);
         }
     });

     // Precio venta → Porcentaje
     precioVentaInput.addEventListener("input", () => {
         const base = parseFloat(ultimoPrecioInput.value || 0);
         const precio = parseFloat(precioVentaInput.value || 0);
         if (base > 0 && precio > 0) {
             const porcentaje = ((precio - base) / base) * 100;
             porcentajeInput.value = porcentaje.toFixed(1);
         }
     });

 }


});
