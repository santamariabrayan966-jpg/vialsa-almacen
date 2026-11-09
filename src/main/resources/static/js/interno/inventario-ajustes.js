document.addEventListener("DOMContentLoaded", () => {
    inicializarPanelAjuste();
    inicializarSelectorProductoAjuste();
    inicializarPreviewEvidenciaAjuste();
    inicializarDetalleAjuste();
    inicializarFiltrosAjustes();
    enfocarFormularioSiVieneProducto();
});

function inicializarPanelAjuste() {
    const btnNuevo = document.getElementById("btnIrNuevoAjuste");
    const btnOcultar = document.getElementById("btnOcultarNuevoAjuste");
    const panel = document.getElementById("panelNuevoAjuste");
    const colHistorial = document.getElementById("colHistorialAjustes");

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
        colHistorial.classList.add("col-xl-8");

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

        colHistorial.classList.remove("col-xl-8");
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

function enfocarFormularioSiVieneProducto() {
    const params = new URLSearchParams(window.location.search);
    const idProducto = params.get("idProducto");

    if (!idProducto) {
        return;
    }

    const btnNuevo = document.getElementById("btnIrNuevoAjuste");

    setTimeout(() => {
        btnNuevo?.click();
    }, 300);
}
function inicializarSelectorProductoAjuste() {
    const inputProducto = document.getElementById("buscadorProductoAjuste");
    const inputIdProducto = document.getElementById("idProductoAjuste");
    const btnAbrir = document.getElementById("btnAbrirProductosAjuste");
    const lista = document.getElementById("listaProductosAjuste");
    const items = Array.from(document.querySelectorAll(".item-producto-ajuste"));
    const selectPresentacion = document.getElementById("tipoPresentacionAjuste");
    const form = document.getElementById("formNuevoAjuste");
    form?.querySelectorAll("input, select, textarea").forEach(campo => {
        campo.addEventListener("input", limpiarErrorAlEditar);
        campo.addEventListener("change", limpiarErrorAlEditar);
    });

    if (!inputProducto || !inputIdProducto || !lista || !selectPresentacion) {
        return;
    }

    function mostrarLista() {
        lista.classList.remove("d-none");
        inputProducto.removeAttribute("readonly");
        inputProducto.focus();
        filtrarProductosAjuste();
    }

    function ocultarLista() {
        lista.classList.add("d-none");
        inputProducto.setAttribute("readonly", "readonly");
    }

    function filtrarProductosAjuste() {
        const texto = normalizar(inputProducto.value);

        items.forEach(item => {
            const textoItem = normalizar(item.dataset.texto || item.textContent || "");
            item.classList.toggle("d-none", texto && !textoItem.includes(texto));
        });
    }

    async function seleccionarProducto(item) {
        const idProducto = item.dataset.id;
        const codigo = item.dataset.codigo || "";
        const nombre = item.dataset.nombre || "";
        const stock = item.dataset.stock || "0";

        inputIdProducto.value = idProducto;
        inputProducto.value = `${codigo} - ${nombre} | Stock: ${stock}`;

        ocultarLista();
        await cargarPresentacionesProducto(idProducto);
    }
    selectPresentacion.addEventListener("change", () => {
        configurarCantidadAjuste(selectPresentacion.value);
    });

    async function cargarPresentacionesProducto(idProducto) {
        selectPresentacion.innerHTML = `<option value="">Cargando presentaciones...</option>`;
        selectPresentacion.disabled = true;

        try {
            const resp = await fetch(`/interno/inventario/ajustes/producto/${idProducto}/presentaciones`);

            if (!resp.ok) {
                throw new Error(await resp.text());
            }

            const presentaciones = await resp.json();

            if (!presentaciones.length) {
                selectPresentacion.innerHTML = `<option value="">Sin presentaciones activas</option>`;
                selectPresentacion.disabled = true;
                return;
            }

            selectPresentacion.innerHTML = `<option value="">Seleccionar presentación</option>`;

            const tiposUnicos = new Map();

            presentaciones.forEach(p => {
                const tipo = String(p.tipoCalculoVenta || "").trim().toUpperCase();

                if (!tipo) {
                    return;
                }

                if (!tiposUnicos.has(tipo)) {
                    tiposUnicos.set(tipo, p);
                }
            });

            tiposUnicos.forEach((p, tipo) => {
                const option = document.createElement("option");

                option.value = tipo;

                switch (tipo) {
                    case "UNIDAD":
                        option.textContent = "Unidad completa";
                        break;

                    case "MEDIA":
                        option.textContent = "Media unidad";
                        break;

                    case "PIE2":
                        option.textContent = "Pie²";
                        break;

                    case "METRO_LINEAL":
                        option.textContent = "Metro lineal";
                        break;

                    default:
                        option.textContent = tipo;
                        break;
                }

                selectPresentacion.appendChild(option);
            });

            selectPresentacion.disabled = false;
            configurarCantidadAjuste("");

        } catch (error) {
            selectPresentacion.innerHTML = `<option value="">Error al cargar presentaciones</option>`;
            selectPresentacion.disabled = true;
            console.error(error);
        }
    }

    btnAbrir?.addEventListener("click", mostrarLista);

    inputProducto.addEventListener("focus", mostrarLista);
    inputProducto.addEventListener("input", filtrarProductosAjuste);

    items.forEach(item => {
        item.addEventListener("click", () => seleccionarProducto(item));
    });

    document.addEventListener("click", (e) => {
        const clickDentro =
            lista.contains(e.target) ||
            inputProducto.contains(e.target) ||
            btnAbrir?.contains(e.target);

        if (!clickDentro) {
            ocultarLista();
        }
    });

  form?.addEventListener("submit", (e) => {
      limpiarErroresFormularioAjuste();

      const motivoInput = document.getElementById("motivoAjusteGeneral");
      const tipoInput = document.getElementById("tipoAjusteGeneral");
      const cantidadInput = document.getElementById("cantidadAjuste");
      const observacionInput = document.getElementById("observacionAjuste");
      const evidenciaInput = document.getElementById("evidenciaFotoAjuste");

      const motivo = motivoInput?.value || "";
      const tipoAjuste = tipoInput?.value || "";
      const cantidadValor = Number(cantidadInput?.value || 0);
      const observacion = observacionInput?.value?.trim() || "";

      const motivosConFotoObligatoria = ["ROTURA", "DANO", "PERDIDA", "VENCIMIENTO"];
      const requiereFoto = motivosConFotoObligatoria.includes(motivo);
      const tieneFoto = evidenciaInput?.files && evidenciaInput.files.length > 0;

      let hayError = false;

      if (!motivo) {
          mostrarErrorCampo(motivoInput, "errorMotivoAjuste", "Debe seleccionar un motivo.");
          hayError = true;
      }

      if (!tipoAjuste) {
          mostrarErrorCampo(tipoInput, "errorTipoAjuste", "Debe seleccionar si el ajuste es aumento o disminución.");
          hayError = true;
      }

      if (!inputIdProducto.value) {
          mostrarErrorCampo(inputProducto, "errorProductoAjuste", "Debe seleccionar un producto.");
          hayError = true;
      }

      if (!selectPresentacion.value) {
          mostrarErrorCampo(selectPresentacion, "errorPresentacionAjuste", "Debe seleccionar una presentación válida.");
          hayError = true;
      }

      if (!cantidadInput?.value || cantidadValor <= 0) {
          mostrarErrorCampo(cantidadInput, "errorCantidadAjuste", "La cantidad debe ser mayor a 0.");
          hayError = true;
      } else if (!Number.isInteger(cantidadValor)) {
          mostrarErrorCampo(cantidadInput, "errorCantidadAjuste", "La cantidad debe ser un número entero. Ejemplo: 1, 2, 3.");
          hayError = true;
      }

      if (motivo === "OTRO" && observacion.length < 10) {
          mostrarErrorCampo(observacionInput, "errorObservacionAjuste", "Cuando seleccionas 'Otro', debes explicar el motivo. Mínimo 10 caracteres.");
          hayError = true;
      }

      if (requiereFoto && !tieneFoto) {
          mostrarErrorCampo(evidenciaInput, "errorEvidenciaAjuste", "Para este motivo debes adjuntar una foto como evidencia.");
          hayError = true;
      }

      if (hayError) {
          e.preventDefault();

          const primerError = form.querySelector(".is-invalid, .invalid-feedback:not(:empty)");
          primerError?.scrollIntoView({
              behavior: "smooth",
              block: "center"
          });

          return;
      }
  });

    if (inputIdProducto.value) {
        const itemInicial = items.find(i => i.dataset.id === inputIdProducto.value);

        if (itemInicial) {
            seleccionarProducto(itemInicial);
        }
    }
}

function configurarCantidadAjuste(tipo) {
    const cantidad = document.getElementById("cantidadAjuste");


    if (!cantidad) return;

    cantidad.value = "";
    cantidad.min = "1";
    cantidad.step = "1";
    cantidad.placeholder = "Ingrese cantidad entera";
    cantidad.inputMode = "numeric";
}

function inicializarPreviewEvidenciaAjuste() {
    const input = document.getElementById("evidenciaFotoAjuste");
    const contenedor = document.getElementById("previewEvidenciaAjuste");
    const imagen = document.getElementById("imgPreviewEvidenciaAjuste");
    const btnQuitar = document.getElementById("btnQuitarEvidenciaAjuste");

    if (!input || !contenedor || !imagen) {
        return;
    }

    input.addEventListener("change", () => {
        const archivo = input.files && input.files[0];

        if (!archivo) {
            limpiarPreview();
            return;
        }

      if (!archivo.type.startsWith("image/")) {
          mostrarErrorCampo(input, "errorEvidenciaAjuste", "Solo se permite subir imágenes.");
          limpiarPreview();
          return;
      }

      const maximoMB = 5;
      const maximoBytes = maximoMB * 1024 * 1024;

      if (archivo.size > maximoBytes) {
          mostrarErrorCampo(input, "errorEvidenciaAjuste", "La imagen no debe superar los 5 MB.");
          limpiarPreview();
          return;
      }

        const urlTemporal = URL.createObjectURL(archivo);

        imagen.src = urlTemporal;
        contenedor.classList.remove("d-none");
    });

    btnQuitar?.addEventListener("click", () => {
        limpiarPreview();
    });

   function limpiarPreview() {
       input.value = "";
       imagen.src = "";
       contenedor.classList.add("d-none");
   }
}

function mostrarErrorCampo(input, idError, mensaje) {
    const error = document.getElementById(idError);

    if (input) {
        input.classList.add("is-invalid");
    }

    if (error) {
        error.textContent = mensaje;
    }
}

function limpiarErroresFormularioAjuste() {
    const form = document.getElementById("formNuevoAjuste");

    if (!form) return;

    form.querySelectorAll(".is-invalid").forEach(campo => {
        campo.classList.remove("is-invalid");
    });

    form.querySelectorAll(".invalid-feedback").forEach(error => {
        error.textContent = "";
    });
}

function limpiarErrorAlEditar(e) {
    const campo = e.target;

    campo.classList.remove("is-invalid");

    const grupo = campo.closest(".mb-3, .col-7, .col-5, .mt-3");

    if (grupo) {
        grupo.querySelectorAll(".invalid-feedback").forEach(error => {
            error.textContent = "";
        });
    }
}

function inicializarDetalleAjuste() {
    const botones = document.querySelectorAll(".btnDetalleAjuste");

    botones.forEach(btn => {
        btn.addEventListener("click", () => {
            const tipo = btn.dataset.tipo || "";
            const evidencia = btn.dataset.evidencia || "";

            document.getElementById("detalleCodigoAjuste").textContent = btn.dataset.codigo || "";
            document.getElementById("detalleFechaAjuste").textContent = btn.dataset.fecha || "";
            document.getElementById("detalleSedeAjuste").textContent = btn.dataset.sede || "";
            document.getElementById("detalleUsuarioAjuste").textContent = btn.dataset.usuario || "Sistema";

            document.getElementById("detalleMotivoAjuste").textContent = formatearMotivoAjuste(btn.dataset.motivo || "");

            const tipoSpan = document.getElementById("detalleTipoAjuste");
            tipoSpan.textContent = formatearTipoAjuste(tipo);
            tipoSpan.className = "badge px-3 py-2";
            tipoSpan.classList.add(tipo === "AUMENTO" ? "text-bg-success" : "text-bg-danger");

            document.getElementById("detalleProductoAjuste").textContent = btn.dataset.producto || "";
            document.getElementById("detalleCodigoProductoAjuste").textContent = btn.dataset.codigoProducto || "";

            document.getElementById("detallePresentacionAjuste").textContent =
                formatearPresentacionAjuste(btn.dataset.presentacion || "");

            document.getElementById("detalleCantidadAjuste").textContent = btn.dataset.cantidad || "0";
            document.getElementById("detalleEquivalenteAjuste").textContent =
                `${btn.dataset.equivalente || "0"} unidad(es) de stock`;

            document.getElementById("detalleStockAnteriorAjuste").textContent = btn.dataset.stockAnterior || "0";
            document.getElementById("detalleStockNuevoAjuste").textContent = btn.dataset.stockNuevo || "0";

            const valor = Number(btn.dataset.valorPerdida || 0);
            document.getElementById("detalleValorPerdidaAjuste").textContent =
                valor > 0 ? `S/ ${valor.toFixed(2)}` : "Sin pérdida";

            document.getElementById("detalleObservacionAjuste").textContent =
                btn.dataset.observacion || "Sin observación";
                const btnExportarDetalle = document.getElementById("btnExportarDetalleAjuste");

                if (btnExportarDetalle) {
                    btnExportarDetalle.href = `/interno/inventario/ajustes/${btn.dataset.id}/pdf`;
                }

            const sinEvidencia = document.getElementById("detalleSinEvidenciaAjuste");
            const linkEvidencia = document.getElementById("detalleLinkEvidenciaAjuste");
            const imgEvidencia = document.getElementById("detalleImagenEvidenciaAjuste");

           if (evidencia && evidencia.trim() !== "") {

               let rutaEvidencia = evidencia.trim();

               if (!rutaEvidencia.startsWith("/uploads/") && !rutaEvidencia.startsWith("http")) {
                   rutaEvidencia = "/uploads/ajustes/" + rutaEvidencia;
               }

               sinEvidencia.classList.add("d-none");
               linkEvidencia.classList.remove("d-none");
               linkEvidencia.href = rutaEvidencia;
               imgEvidencia.src = rutaEvidencia;

           } else {
               sinEvidencia.classList.remove("d-none");
               linkEvidencia.classList.add("d-none");
               linkEvidencia.href = "#";
               imgEvidencia.src = "";
           }
        });
    });
}

function formatearMotivoAjuste(valor) {
    switch (valor) {
        case "CONTEO_FISICO": return "Conteo físico";
        case "PERDIDA": return "Pérdida";
        case "DANO": return "Daño";
        case "VENCIMIENTO": return "Vencimiento";
        case "ROTURA": return "Rotura";
        case "OTRO": return "Otro";
        default: return valor || "Sin motivo";
    }
}

function formatearTipoAjuste(valor) {
    return valor === "AUMENTO" ? "Aumento" : "Disminución";
}

function formatearPresentacionAjuste(valor) {
    switch (valor) {
        case "PIE2": return "Pie²";
        case "METRO_LINEAL": return "Metro lineal";
        case "MEDIA": return "Media unidad";
        case "UNIDAD": return "Unidad completa";
        default: return valor || "Unidad completa";
    }
}

function inicializarFiltrosAjustes() {
    const inputBusqueda = document.getElementById("buscarAjuste");
    const filtroMotivo = document.getElementById("filtroMotivoAjuste");
    const filtroTipo = document.getElementById("filtroTipoAjuste");
    const btnLimpiar = document.getElementById("btnLimpiarFiltrosAjuste");
    const filas = Array.from(document.querySelectorAll(".fila-ajuste"));

    const contador = document.getElementById("contadorAjustesVisibles");
    const contadorTotal = document.getElementById("contadorAjustesTotal");
    const paginaActualTexto = document.getElementById("paginaAjustesActual");
    const paginaTotalTexto = document.getElementById("paginaAjustesTotal");

    const btnAnterior = document.getElementById("btnAjustesAnterior");
    const btnSiguiente = document.getElementById("btnAjustesSiguiente");

    const limiteVisible = 5;
    let paginaActual = 1;
    let filtradasActuales = [];

    if (!filas.length) {
        if (contador) contador.textContent = "0";
        if (contadorTotal) contadorTotal.textContent = "0";
        if (paginaActualTexto) paginaActualTexto.textContent = "1";
        if (paginaTotalTexto) paginaTotalTexto.textContent = "1";
        return;
    }

    const obtenerFiltradas = () => {
        const texto = normalizar(inputBusqueda?.value || "");
        const motivo = filtroMotivo?.value || "";
        const tipo = filtroTipo?.value || "";

        return filas.filter((fila) => {
           const filaTexto = normalizar(
               (fila.dataset.texto || "") + " " + (fila.textContent || "")
           );
            const filaMotivo = fila.dataset.motivo || "";
            const filaTipo = fila.dataset.tipo || "";

            const coincideTexto = !texto || filaTexto.includes(texto);
            const coincideMotivo = !motivo || filaMotivo === motivo;
            const coincideTipo = !tipo || filaTipo === tipo;

            return coincideTexto && coincideMotivo && coincideTipo;
        });
    };

    const renderizarPagina = () => {
        const totalFiltradas = filtradasActuales.length;
        const totalPaginas = Math.max(1, Math.ceil(totalFiltradas / limiteVisible));

        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas;
        }

        const inicio = (paginaActual - 1) * limiteVisible;
        const fin = inicio + limiteVisible;

        filas.forEach(fila => fila.classList.add("d-none"));

        filtradasActuales.slice(inicio, fin).forEach(fila => {
            fila.classList.remove("d-none");
        });

        if (contador) {
            contador.textContent = Math.min(limiteVisible, Math.max(0, totalFiltradas - inicio));
        }

        if (contadorTotal) {
            contadorTotal.textContent = totalFiltradas;
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
    };

    const aplicarFiltros = () => {
        filtradasActuales = obtenerFiltradas();
        paginaActual = 1;
        renderizarPagina();
    };

    inputBusqueda?.addEventListener("input", aplicarFiltros);
    filtroMotivo?.addEventListener("change", aplicarFiltros);
    filtroTipo?.addEventListener("change", aplicarFiltros);

    btnAnterior?.addEventListener("click", () => {
        if (paginaActual > 1) {
            paginaActual--;
            renderizarPagina();
        }
    });

    btnSiguiente?.addEventListener("click", () => {
        const totalPaginas = Math.max(1, Math.ceil(filtradasActuales.length / limiteVisible));

        if (paginaActual < totalPaginas) {
            paginaActual++;
            renderizarPagina();
        }
    });

    btnLimpiar?.addEventListener("click", () => {
        if (inputBusqueda) inputBusqueda.value = "";
        if (filtroMotivo) filtroMotivo.value = "";
        if (filtroTipo) filtroTipo.value = "";

        aplicarFiltros();
    });

    aplicarFiltros();
}

function normalizar(valor) {
    return String(valor || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}
