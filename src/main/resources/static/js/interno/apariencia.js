document.addEventListener("DOMContentLoaded", () => {
    activarTabsMarca();
    activarLogosMarca();
    activarSeleccionLogoActivo();
    activarServiciosTienda();
    activarColoresTienda();
    activarSeccionesPanelInterno();
    activarColoresPanelInterno();
    activarMenuPanelInterno();
    activarLoginPanelInterno();
});

/* =====================================================
   TABS / PANELES
===================================================== */

function activarTabsMarca() {
    const tabs = document.querySelectorAll(".identity-tab");
    const panels = document.querySelectorAll(".identity-panel");
    const tabActivaInput = document.getElementById("tabActiva");
    const tabsPermitidas = ["logos", "datos", "comunicacion", "redes"];

    if (!tabs.length || !panels.length) return;

    const activarSeccion = sectionId => {
        const seccionValida = tabsPermitidas.includes(sectionId) ? sectionId : "logos";

        tabs.forEach(item => {
            item.classList.toggle("active", item.dataset.section === seccionValida);
        });

        panels.forEach(panel => {
            panel.classList.toggle("active", panel.dataset.panel === seccionValida);
        });

        if (tabActivaInput) {
            tabActivaInput.value = seccionValida;
        }
    };

    tabs.forEach(tab => {
        tab.addEventListener("click", () => {
            activarSeccion(tab.dataset.section);
        });
    });

    const params = new URLSearchParams(window.location.search);
    activarSeccion(params.get("tab") || "logos");
}

/* =====================================================
   LOGOS - 3 OPCIONES
===================================================== */

function activarLogosMarca() {
    activarInputLogo(1);
    activarInputLogo(2);
    activarInputLogo(3);
}

function activarInputLogo(numero) {
    const inputArchivo = document.getElementById("archivoLogo" + numero);

    if (!inputArchivo) return;

    inputArchivo.addEventListener("change", () => {
        const archivo = inputArchivo.files && inputArchivo.files[0];

        if (!archivo) return;

        const tiposPermitidos = [
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/svg+xml"
        ];

        if (!tiposPermitidos.includes(archivo.type)) {
            alert("Formato no permitido. Usa PNG, JPG, JPEG, WEBP o SVG.");
            inputArchivo.value = "";
            return;
        }

        const reader = new FileReader();

        reader.onload = e => {
            mostrarLogoOpcion(numero, e.target.result);

            const eliminarInput = document.getElementById("eliminarLogo" + numero);
            if (eliminarInput) eliminarInput.value = "false";
        };

        reader.readAsDataURL(archivo);
    });
}

function mostrarLogoOpcion(numero, url) {
    const img = document.getElementById("logoPreview" + numero);
    const fallback = document.getElementById("logoFallback" + numero);

    if (img) {
        img.src = url;
        img.style.display = "block";
    }

    if (fallback) {
        fallback.style.display = "none";
    }
}

function eliminarLogoOpcion(numero) {
    const inputArchivo = document.getElementById("archivoLogo" + numero);
    const eliminarInput = document.getElementById("eliminarLogo" + numero);
    const rutaInput = document.getElementById("logoOpcion" + numero);
    const img = document.getElementById("logoPreview" + numero);
    const fallback = document.getElementById("logoFallback" + numero);

    if (inputArchivo) inputArchivo.value = "";
    if (eliminarInput) eliminarInput.value = "true";
    if (rutaInput) rutaInput.value = "";

    if (img) {
        img.removeAttribute("src");
        img.style.display = "none";
    }

    if (fallback) {
        fallback.style.display = "grid";
    }

    const radio = document.querySelector('input[name="logoActivo"]:checked');

    if (radio && Number(radio.value) === numero) {
        seleccionarPrimerLogoDisponible(numero);
    }
}

function seleccionarPrimerLogoDisponible(numeroEliminado) {
    const radios = document.querySelectorAll('input[name="logoActivo"]');

    for (const radio of radios) {
        if (Number(radio.value) !== numeroEliminado) {
            radio.checked = true;
            actualizarCardsLogoActivo();
            return;
        }
    }
}

/* =====================================================
   LOGO ACTIVO
===================================================== */

function activarSeleccionLogoActivo() {
    const radios = document.querySelectorAll('input[name="logoActivo"]');

    radios.forEach(radio => {
        radio.addEventListener("change", actualizarCardsLogoActivo);
    });

    actualizarCardsLogoActivo();
}

function actualizarCardsLogoActivo() {
    const cards = document.querySelectorAll(".logo-option-card");

    cards.forEach(card => {
        card.classList.remove("active");
    });

    const seleccionado = document.querySelector('input[name="logoActivo"]:checked');

    if (!seleccionado) return;

    const card = seleccionado.closest(".logo-option-card");

    if (card) {
        card.classList.add("active");
    }
}

/* =====================================================
   SERVICIOS - TIENDA ONLINE
===================================================== */

function activarServiciosTienda() {
    activarSelectoresIconosServicios();
    activarPreviewImagenServicios();
}

function activarSelectoresIconosServicios() {
    const selects = document.querySelectorAll(".store-service-icon-select");

    selects.forEach(select => {
        const actualizar = () => {
            const preview = select.closest(".store-service-icon-picker")?.querySelector(".store-service-icon-preview i");
            if (preview) {
                preview.className = "bi " + select.value;
            }
        };

        select.addEventListener("change", actualizar);
        actualizar();
    });
}

function activarPreviewImagenServicios() {
    const input = document.getElementById("serviciosImagenPrincipalInput");
    const preview = document.getElementById("serviciosImagenPreview");
    const fallback = document.getElementById("serviciosImagenFallback");
    const error = document.getElementById("serviciosImagenError");

    if (!input || !preview) return;

    input.addEventListener("change", () => {
        const archivo = input.files && input.files[0];
        if (!archivo) return;

        const extensionesPermitidas = ["jpg", "jpeg", "png", "webp"];
        const extension = archivo.name.split(".").pop().toLowerCase();

        if (!extensionesPermitidas.includes(extension)) {
            input.value = "";
            if (error) error.textContent = "Formato no permitido. Usa JPG, PNG o WEBP.";
            return;
        }

        if (error) error.textContent = "";

        const urlTemporal = URL.createObjectURL(archivo);
        preview.src = urlTemporal;
        preview.classList.remove("d-none");

        if (fallback) {
            fallback.classList.add("d-none");
        }
    });
}

/* =====================================================
   COLORES - TIENDA ONLINE
===================================================== */

function activarColoresTienda() {
    const preview = document.querySelector("[data-color-preview]");
    const iframePreview = document.querySelector(".preview-real-frame");
    const pickers = document.querySelectorAll(".store-color-picker");
    const textos = document.querySelectorAll(".store-color-hex");
    const temas = document.querySelectorAll(".store-color-theme");
    const mapa = {
        colorHeader: "--preview-header",
        colorFooter: "--preview-footer",
        colorFondo: "--preview-bg",
        colorBotonPrimario: "--preview-primary",
        colorAcento: "--preview-accent",
        colorTextoPrincipal: "--preview-text",
        colorTextoHeader: "--preview-header-text"
    };
    const mapaIframe = {
        colorHeader: ["--dentia-header", "--store-dark"],
        colorFooter: ["--dentia-footer"],
        colorFondo: ["--dentia-bg"],
        colorBotonPrimario: ["--dentia-primary", "--store-primary"],
        colorAcento: ["--dentia-accent", "--dentia-cyan", "--store-cyan"],
        colorTextoPrincipal: ["--dentia-text"],
        colorTextoHeader: ["--dentia-header-text"]
    };

    if (!pickers.length) return;

    const esHex = valor => /^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$/.test(valor);

    const leerColoresActuales = () => {
        const colores = {};

        Object.keys(mapaIframe).forEach(clave => {
            const picker = document.querySelector(`[data-color-target="${clave}"]`);
            const texto = document.querySelector(`[data-color-text="${clave}"]`);
            const valor = picker?.value || texto?.value?.trim();

            if (esHex(valor)) {
                colores[clave] = valor;
            }
        });

        return colores;
    };

    const obtenerDocumentoIframe = () => {
        if (!iframePreview) return null;

        try {
            return iframePreview.contentDocument || iframePreview.contentWindow?.document || null;
        } catch (error) {
            return null;
        }
    };

    const aplicarColoresTemporalesAlIframe = () => {
        const doc = obtenerDocumentoIframe();
        if (!doc) return;

        const colores = leerColoresActuales();
        const destinos = [doc.documentElement, doc.body].filter(Boolean);

        Object.entries(colores).forEach(([clave, valor]) => {
            (mapaIframe[clave] || []).forEach(variable => {
                destinos.forEach(destino => destino.style.setProperty(variable, valor));
            });
        });
    };

    const actualizarSwatch = (clave, valor) => {
        const swatch = document.querySelector(`[data-color-swatch="${clave}"]`);
        if (swatch) swatch.style.background = valor;
    };

    const aplicar = (clave, valor) => {
        if (!clave || !esHex(valor)) return;
        if (preview) preview.style.setProperty(mapa[clave], valor);
        actualizarSwatch(clave, valor);
        aplicarColoresTemporalesAlIframe();
    };

    const actualizarCampo = (clave, valor) => {
        if (!clave || !esHex(valor)) return;

        const picker = document.querySelector(`[data-color-target="${clave}"]`);
        const texto = document.querySelector(`[data-color-text="${clave}"]`);

        if (picker) picker.value = valor;
        if (texto) texto.value = valor;

        aplicar(clave, valor);
    };

    pickers.forEach(picker => {
        const clave = picker.dataset.colorTarget;
        const texto = document.querySelector(`[data-color-text="${clave}"]`);
        if (clave && picker.value) aplicar(clave, picker.value);

        picker.addEventListener("input", () => {
            if (texto) texto.value = picker.value;
            aplicar(clave, picker.value);
        });
    });

    textos.forEach(texto => {
        const clave = texto.dataset.colorText;
        const picker = document.querySelector(`[data-color-target="${clave}"]`);

        texto.addEventListener("input", () => {
            const valor = texto.value.trim();
            if (!esHex(valor)) return;
            if (picker) picker.value = valor;
            aplicar(clave, valor);
        });
    });

    temas.forEach(tema => {
        tema.addEventListener("click", () => {
            actualizarCampo("colorHeader", tema.dataset.colorHeader);
            actualizarCampo("colorFooter", tema.dataset.colorFooter);
            actualizarCampo("colorFondo", tema.dataset.colorFondo);
            actualizarCampo("colorBotonPrimario", tema.dataset.colorBotonPrimario);
            actualizarCampo("colorAcento", tema.dataset.colorAcento);
            actualizarCampo("colorTextoPrincipal", tema.dataset.colorTextoPrincipal);
            actualizarCampo("colorTextoHeader", tema.dataset.colorTextoHeader);

            temas.forEach(t => t.classList.remove("active"));
            tema.classList.add("active");
        });
    });

    if (iframePreview) {
        iframePreview.addEventListener("load", aplicarColoresTemporalesAlIframe);
        aplicarColoresTemporalesAlIframe();
    }
}


/* =====================================================
   COLORES - PANEL INTERNO
===================================================== */

function activarSeccionesPanelInterno() {
    const botones = document.querySelectorAll("[data-panel-section-button]");
    const secciones = document.querySelectorAll("[data-panel-section]");
    const seccionesPermitidas = ["colores", "login", "menu"];

    if (!botones.length || !secciones.length) return;

    const obtenerSeccionDesdeHash = () => {
        const hash = window.location.hash.replace("#", "");
        return seccionesPermitidas.includes(hash) ? hash : "colores";
    };

    const activarSeccion = seccion => {
        const seccionActiva = seccionesPermitidas.includes(seccion) ? seccion : "colores";

        botones.forEach(boton => {
            const activa = boton.dataset.panelSectionButton === seccionActiva;
            boton.classList.toggle("active", activa);
            boton.setAttribute("aria-selected", activa ? "true" : "false");
        });

        secciones.forEach(panel => {
            const activa = panel.dataset.panelSection === seccionActiva;
            panel.classList.toggle("active", activa);
            panel.hidden = !activa;
        });
    };

    botones.forEach(boton => {
        boton.addEventListener("click", () => {
            const seccion = boton.dataset.panelSectionButton;
            activarSeccion(seccion);

            if (seccionesPermitidas.includes(seccion)) {
                window.location.hash = seccion;
            }
        });
    });

    window.addEventListener("hashchange", () => activarSeccion(obtenerSeccionDesdeHash()));
    activarSeccion(obtenerSeccionDesdeHash());
}

function activarColoresPanelInterno() {
    const previews = document.querySelectorAll("[data-panel-preview], [data-panel-menu-preview]");
    const inputs = document.querySelectorAll("[data-panel-color-input]");
    const textos = document.querySelectorAll("[data-panel-color-text]");
    const temas = document.querySelectorAll("[data-panel-theme]");

    if (!inputs.length) return;

    const mapaPreview = {
        colorHeader: "--panel-preview-header",
        colorHeaderSecundario: "--panel-preview-header-secondary",
        colorTextoHeader: "--panel-preview-text-header",
        colorFooter: "--panel-preview-footer",
        colorTextoFooter: "--panel-preview-text-footer"
    };

    const esHex = valor => /^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$/.test(valor);

    const actualizarSwatchPanel = (clave, valor) => {
        const swatch = document.querySelector(`[data-panel-color-swatch="${clave}"]`);
        if (swatch) swatch.style.background = valor;
    };

    const aplicarPanel = (clave, valor) => {
        if (!clave || !esHex(valor)) return;
        if (mapaPreview[clave]) {
            previews.forEach(preview => {
                preview.style.setProperty(mapaPreview[clave], valor);
            });
        }
        actualizarSwatchPanel(clave, valor);
        document.querySelectorAll(`[data-panel-menu-color-sync="${clave}"]`).forEach(input => {
            input.value = valor;
        });
    };

    const actualizarCampoPanel = (clave, valor) => {
        if (!clave || !esHex(valor)) return;

        const input = document.querySelector(`[data-panel-color-input="${clave}"]`);
        const texto = document.querySelector(`[data-panel-color-text="${clave}"]`);

        if (input) input.value = valor;
        if (texto) texto.value = valor;

        aplicarPanel(clave, valor);
    };

    inputs.forEach(input => {
        const clave = input.dataset.panelColorInput;
        const texto = document.querySelector(`[data-panel-color-text="${clave}"]`);

        if (clave && input.value) {
            aplicarPanel(clave, input.value);
        }

        input.addEventListener("input", () => {
            if (texto) texto.value = input.value;
            aplicarPanel(clave, input.value);
        });
    });

    textos.forEach(texto => {
        const clave = texto.dataset.panelColorText;
        const input = document.querySelector(`[data-panel-color-input="${clave}"]`);

        texto.addEventListener("input", () => {
            const valor = texto.value.trim();
            if (!esHex(valor)) return;
            if (input) input.value = valor;
            aplicarPanel(clave, valor);
        });
    });

    temas.forEach(tema => {
        tema.addEventListener("click", () => {
            actualizarCampoPanel("colorHeader", tema.dataset.colorHeader);
            actualizarCampoPanel("colorHeaderSecundario", tema.dataset.colorHeaderSecundario);
            actualizarCampoPanel("colorTextoHeader", tema.dataset.colorTextoHeader);
            actualizarCampoPanel("colorFooter", tema.dataset.colorFooter);
            actualizarCampoPanel("colorTextoFooter", tema.dataset.colorTextoFooter);

            temas.forEach(t => t.classList.remove("active"));
            tema.classList.add("active");
        });
    });
}

function activarMenuPanelInterno() {
    const opciones = document.querySelectorAll("[data-panel-menu-option]");
    const preview = document.querySelector("[data-panel-menu-preview]");
    const camposMenu = document.querySelectorAll("[data-panel-menu-field]");

    if (!opciones.length || !preview) return;

    const posicionesPermitidas = ["ARRIBA", "IZQUIERDA", "DERECHA", "ABAJO"];

    const normalizarPosicion = valor => {
        const limpio = (valor || "").trim().toUpperCase();
        return posicionesPermitidas.includes(limpio) ? limpio : "ARRIBA";
    };

    const aplicarPosicion = valor => {
        const posicion = normalizarPosicion(valor);
        preview.dataset.panelMenuPreview = posicion;

        opciones.forEach(opcion => {
            const activa = normalizarPosicion(opcion.value) === posicion;
            opcion.checked = activa;
            opcion.closest(".panel-menu-option")?.classList.toggle("active", activa);
        });

        camposMenu.forEach(campo => {
            campo.value = posicion;
        });
    };

    opciones.forEach(opcion => {
        opcion.addEventListener("change", () => aplicarPosicion(opcion.value));
    });

    const seleccionada = Array.from(opciones).find(opcion => opcion.checked)?.value
        || preview.dataset.panelMenuPreview;
    aplicarPosicion(seleccionada);
}

function activarLoginPanelInterno() {
    const form = document.querySelector("[data-login-config-form]");
    const previewDeviceShell = document.querySelector("[data-login-preview-frame]");
    const iframe = document.querySelector("[data-login-preview-iframe]") || previewDeviceShell?.querySelector("iframe");
    const deviceButtons = document.querySelectorAll("[data-login-preview-device]");
    const inputs = document.querySelectorAll("[data-login-config]");
    const colorInputs = document.querySelectorAll("[data-login-color-input]");
    const colorTexts = document.querySelectorAll("[data-login-color-text]");
    const imageInput = document.querySelector("[data-login-image-input]");
    const removeImage = document.querySelector("[data-login-remove-image]");
    const carouselFiles = document.querySelectorAll("[data-login-carousel-file]");
    const carouselRemoves = document.querySelectorAll("[data-login-carousel-remove]");
    const clearFileButtons = document.querySelectorAll("[data-login-file-clear]");
    const carouselCropButtons = document.querySelectorAll("[data-carousel-crop-button]");
    const carouselCropModalElement = document.querySelector("[data-carousel-crop-modal]");
    const carouselCropStage = document.querySelector("[data-carousel-crop-stage]");
    const carouselCropImage = document.querySelector("[data-carousel-crop-image]");
    const carouselCropZoom = document.querySelector("[data-carousel-crop-zoom]");
    const carouselCropZoomLabel = document.querySelector("[data-carousel-crop-zoom-label]");
    const carouselCropReset = document.querySelector("[data-carousel-crop-reset]");
    const carouselCropApply = document.querySelector("[data-carousel-crop-apply]");
    const carouselCropCloseButtons = document.querySelectorAll("[data-carousel-crop-close]");
    const carouselGrid = form?.querySelector(".panel-login-carousel-grid");

    if (!form || !iframe) return;

    let imagenTemporal = null;
    const carruselTemporal = {};
    const dispositivosPreviewLogin = {
        desktop: { width: 1280, height: 760, maxVisibleHeight: 760 },
        tablet: { width: 820, height: 760, maxVisibleHeight: 760 },
        mobile: { width: 390, height: 820, maxVisibleHeight: 760 }
    };
    let dispositivoPreviewActivo = "desktop";
    let carruselCropActivo = null;
    let cropEstado = { zoom: 1, offsetX: 0, offsetY: 0 };
    let cropDrag = null;

    const esHex = valor => /^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$/.test(valor || "");

    const obtenerDocumentoIframe = () => {
        try {
            return iframe.contentDocument || iframe.contentWindow?.document || null;
        } catch (error) {
            return null;
        }
    };

    const valorCampo = nombre => {
        const campo = form.querySelector(`[data-login-config="${nombre}"]`);
        return campo?.value?.trim() || "";
    };

    const checkActivo = nombre => {
        const campo = form.querySelector(`[data-login-config="${nombre}"]`);
        return Boolean(campo?.checked);
    };

    const valorColor = nombre => {
        const texto = form.querySelector(`[data-login-color-text="${nombre}"]`);
        const picker = form.querySelector(`[data-login-color-input="${nombre}"]`);
        const valor = texto?.value?.trim() || picker?.value || "";
        return esHex(valor) ? valor : "";
    };

    const actualizarSwatch = (nombre, valor) => {
        const swatch = form.querySelector(`[data-login-swatch="${nombre}"]`);
        if (swatch && esHex(valor)) {
            swatch.style.background = valor;
        }
    };

    const sincronizarColor = (nombre, valor) => {
        if (!nombre || !esHex(valor)) return;

        const picker = form.querySelector(`[data-login-color-input="${nombre}"]`);
        const texto = form.querySelector(`[data-login-color-text="${nombre}"]`);

        if (picker) picker.value = valor;
        if (texto) texto.value = valor;
        actualizarSwatch(nombre, valor);
        aplicarPreviewLogin();
    };

    const aplicarClase = (elemento, prefijo, valor) => {
        Array.from(elemento.classList)
            .filter(clase => clase.startsWith(prefijo))
            .forEach(clase => elemento.classList.remove(clase));
        elemento.classList.add(prefijo + valor.toLowerCase());
    };

    const ajustarEscalaPreviewLogin = () => {
        if (!previewDeviceShell) return;

        const config = dispositivosPreviewLogin[dispositivoPreviewActivo] || dispositivosPreviewLogin.desktop;
        const paddingHorizontal = window.matchMedia("(max-width: 768px)").matches ? 16 : 24;
        const contenedor = previewDeviceShell.closest(".panel-login-real-preview-card") || previewDeviceShell.parentElement;
        const anchoContenedor = contenedor?.clientWidth || previewDeviceShell.parentElement?.clientWidth || previewDeviceShell.clientWidth;
        const anchoDisponible = Math.max(280, anchoContenedor - paddingHorizontal);
        const escala = Math.min(1, anchoDisponible / config.width);
        const anchoEscalado = Math.round(config.width * escala);
        const altoEscalado = Math.round(config.height * escala);
        const altoVisible = Math.min(altoEscalado, config.maxVisibleHeight);
        const anchoShell = Math.min(anchoEscalado + paddingHorizontal, anchoContenedor);

        previewDeviceShell.style.setProperty("--login-preview-width", `${config.width}px`);
        previewDeviceShell.style.setProperty("--login-preview-height", `${config.height}px`);
        previewDeviceShell.style.setProperty("--login-preview-scale", escala.toString());
        previewDeviceShell.style.setProperty("--login-preview-stage-width", `${anchoEscalado}px`);
        previewDeviceShell.style.setProperty("--login-preview-stage-height", `${altoEscalado}px`);
        previewDeviceShell.style.setProperty("--login-preview-shell-width", `${anchoShell}px`);
        previewDeviceShell.style.setProperty("--login-preview-visible-height", `${altoVisible + paddingHorizontal}px`);
    };

    const activarDispositivoPreview = dispositivo => {
        if (!previewDeviceShell) return;

        const dispositivoValido = Object.prototype.hasOwnProperty.call(dispositivosPreviewLogin, dispositivo)
            ? dispositivo
            : "desktop";
        dispositivoPreviewActivo = dispositivoValido;

        deviceButtons.forEach(boton => {
            const activo = boton.dataset.loginPreviewDevice === dispositivoValido;
            boton.classList.toggle("active", activo);
            boton.setAttribute("aria-selected", activo ? "true" : "false");
        });

        previewDeviceShell.classList.remove(
            "login-preview-desktop",
            "login-preview-tablet",
            "login-preview-mobile"
        );
        previewDeviceShell.classList.add("login-preview-" + dispositivoValido);

        ajustarEscalaPreviewLogin();
        aplicarPreviewLogin();
    };

    const obtenerImagenCarrusel = numero => {
        const eliminar = form.querySelector(`[data-login-carousel-remove="${numero}"]`);
        if (eliminar?.checked) return "";

        if (carruselTemporal[numero]) {
            return carruselTemporal[numero];
        }

        return valorCampo(`carruselImagen${numero}`);
    };

    const numeroEnRango = (valor, minimo, maximo, defecto) => {
        const numero = Number.parseFloat(valor);
        if (!Number.isFinite(numero)) return defecto;
        return Math.min(maximo, Math.max(minimo, numero));
    };

    const numeroGuardadoEnRango = (valor, minimo, maximo, defecto) => {
        if (valor === null || valor === undefined || valor === "") return defecto;

        const numero = Number.parseFloat(valor);
        if (!Number.isFinite(numero) || numero < minimo || numero > maximo) {
            return defecto;
        }

        return numero;
    };

    const obtenerMedidasStageCrop = () => {
        const rect = carouselCropStage?.getBoundingClientRect();
        return {
            ancho: rect?.width || 720,
            alto: rect?.height || 450
        };
    };

    const normalizarOffsetDesdeStage = estado => {
        const medidas = obtenerMedidasStageCrop();
        return {
            zoom: numeroEnRango(estado?.zoom, 1, 4, 1),
            offsetX: medidas.ancho ? numeroEnRango(estado?.offsetX, -medidas.ancho, medidas.ancho, 0) / medidas.ancho * 100 : 0,
            offsetY: medidas.alto ? numeroEnRango(estado?.offsetY, -medidas.alto, medidas.alto, 0) / medidas.alto * 100 : 0
        };
    };

    const convertirOffsetAStage = estado => {
        const medidas = obtenerMedidasStageCrop();
        return {
            zoom: numeroEnRango(estado?.zoom, 1, 4, 1),
            offsetX: medidas.ancho * numeroEnRango(estado?.offsetX, -100, 100, 0) / 100,
            offsetY: medidas.alto * numeroEnRango(estado?.offsetY, -100, 100, 0) / 100
        };
    };

    const leerOffsetNormalizadoGuardado = (valor, eje) => {
        if (valor === null || valor === undefined || valor === "") return 0;

        const numero = Number.parseFloat(valor);
        if (!Number.isFinite(numero)) return 0;
        if (Math.abs(numero) <= 100) return numero;

        const base = eje === "y" ? 450 : 720;
        return numeroEnRango(numero / base * 100, -100, 100, 0);
    };

    const inputZoomCarrusel = numero => form.querySelector(`[data-carousel-zoom="${numero}"]`);

    const inputOffsetXCarrusel = numero => form.querySelector(`[data-carousel-offset-x="${numero}"]`);

    const inputOffsetYCarrusel = numero => form.querySelector(`[data-carousel-offset-y="${numero}"]`);

    const obtenerCropCarrusel = numero => ({
        zoom: numeroGuardadoEnRango(inputZoomCarrusel(numero)?.value, 1, 4, 1),
        offsetX: leerOffsetNormalizadoGuardado(inputOffsetXCarrusel(numero)?.value, "x"),
        offsetY: leerOffsetNormalizadoGuardado(inputOffsetYCarrusel(numero)?.value, "y")
    });

    const escribirCropCarrusel = (numero, estado) => {
        const zoomInput = inputZoomCarrusel(numero);
        const offsetXInput = inputOffsetXCarrusel(numero);
        const offsetYInput = inputOffsetYCarrusel(numero);
        const cropNormalizado = normalizarOffsetDesdeStage(estado);

        if (zoomInput) zoomInput.value = cropNormalizado.zoom.toFixed(3);
        if (offsetXInput) offsetXInput.value = cropNormalizado.offsetX.toFixed(3);
        if (offsetYInput) offsetYInput.value = cropNormalizado.offsetY.toFixed(3);
    };

    const restaurarCropCarrusel = numero => {
        escribirCropCarrusel(numero, { zoom: 1, offsetX: 0, offsetY: 0 });
    };

    const aplicarCropImagen = (imagen, estado) => {
        if (!imagen) return;
        const crop = {
            zoom: numeroEnRango(estado?.zoom, 1, 4, 1),
            offsetX: numeroEnRango(estado?.offsetX, -100, 100, 0),
            offsetY: numeroEnRango(estado?.offsetY, -100, 100, 0)
        };
        imagen.style.setProperty("--carousel-zoom", crop.zoom.toString());
        imagen.style.setProperty("--carousel-offset-x", `${crop.offsetX}%`);
        imagen.style.setProperty("--carousel-offset-y", `${crop.offsetY}%`);
    };

    const aplicarCropMiniPreview = (imagen, estado) => {
        if (!imagen) return;

        const crop = {
            zoom: numeroEnRango(estado?.zoom, 1, 4, 1),
            offsetX: numeroEnRango(estado?.offsetX, -100, 100, 0),
            offsetY: numeroEnRango(estado?.offsetY, -100, 100, 0)
        };

        imagen.style.setProperty("--mini-carousel-zoom", crop.zoom.toString());
        imagen.style.setProperty("--mini-carousel-offset-x", `${crop.offsetX}%`);
        imagen.style.setProperty("--mini-carousel-offset-y", `${crop.offsetY}%`);
    };

    const checkboxEliminarActual = clave => form.querySelector(`[data-login-file-remove-current="${clave}"]`);

    const inputArchivo = clave => form.querySelector(`[data-login-file-input="${clave}"]`);

    const actualizarBotonEncuadreCarrusel = numero => {
        const boton = form.querySelector(`[data-carousel-crop-button="${numero}"]`);
        if (!boton) return;
        boton.classList.toggle("d-none", !obtenerImagenCarrusel(numero));
    };

    const actualizarBotonesEncuadreCarrusel = () => {
        [1, 2, 3].forEach(actualizarBotonEncuadreCarrusel);
    };

    const obtenerItemCarruselPanel = numero => form.querySelector(`[data-login-carousel-file="${numero}"]`)?.closest(".panel-login-carousel-item");

    const obtenerImagenesCarruselActivas = () => [1, 2, 3]
        .filter(numero => Boolean(obtenerImagenCarrusel(numero)));

    const primeraPosicionCarruselDisponible = () => [1, 2, 3]
        .find(numero => !obtenerImagenCarrusel(numero));

    const crearGestionCarruselPanel = () => {
        if (!carouselGrid || carouselGrid.dataset.carouselManagerReady === "true") return;

        carouselGrid.dataset.carouselManagerReady = "true";
        carouselGrid.classList.add("login-carousel-list");

        const manager = document.createElement("div");
        manager.className = "login-carousel-manager";
        manager.innerHTML = `
            <div class="login-carousel-manager-header">
                <span class="login-carousel-counter" data-login-carousel-counter>0/3 imagenes cargadas</span>
                <button type="button" class="login-carousel-upload-main" data-login-carousel-upload-main>
                    <i class="bi bi-cloud-arrow-up"></i>
                    <span>Subir imagen</span>
                </button>
            </div>
            <div class="login-carousel-empty" data-login-carousel-empty>
                <strong>Aun no hay imagenes en el carrusel</strong>
                <span>Puedes subir hasta 3 imagenes.</span>
                <button type="button" class="btn btn-primary btn-sm" data-login-carousel-upload-main>
                    Subir primera imagen
                </button>
            </div>
        `;
        carouselGrid.parentNode.insertBefore(manager, carouselGrid);

        [1, 2, 3].forEach(numero => {
            const item = obtenerItemCarruselPanel(numero);
            if (!item) return;

            item.classList.add("login-carousel-card");
            const input = item.querySelector(`[data-login-carousel-file="${numero}"]`);
            const titulo = item.querySelector(":scope > span");
            const botonCrop = item.querySelector(`[data-carousel-crop-button="${numero}"]`);
            const labelEliminar = item.querySelector(`[data-login-carousel-remove="${numero}"]`)?.closest("label");

            if (input) input.classList.add("login-carousel-file-input");
            if (titulo) titulo.classList.add("login-carousel-card-title");
            if (labelEliminar) labelEliminar.classList.add("d-none");
            if (botonCrop) {
                botonCrop.classList.add("login-carousel-card-crop");
                botonCrop.innerHTML = '<i class="bi bi-crop"></i> Ajustar';
            }

            if (!item.querySelector("[data-login-carousel-card-actions]")) {
                const acciones = document.createElement("div");
                acciones.className = "login-carousel-card-actions";
                acciones.dataset.loginCarouselCardActions = "";
                acciones.innerHTML = `
                    <button type="button" class="btn btn-outline-secondary btn-sm" data-login-carousel-replace="${numero}">
                        <i class="bi bi-arrow-repeat"></i> Reemplazar
                    </button>
                    <button type="button" class="btn btn-outline-danger btn-sm" data-login-carousel-delete="${numero}">
                        <i class="bi bi-trash"></i> Eliminar
                    </button>
                `;
                item.appendChild(acciones);
            }
        });

        form.querySelectorAll("[data-login-carousel-upload-main]").forEach(boton => {
            boton.addEventListener("click", () => {
                const disponible = primeraPosicionCarruselDisponible();
                if (!disponible) return;
                form.querySelector(`[data-login-carousel-file="${disponible}"]`)?.click();
            });
        });

        form.querySelectorAll("[data-login-carousel-replace]").forEach(boton => {
            boton.addEventListener("click", () => {
                form.querySelector(`[data-login-carousel-file="${boton.dataset.loginCarouselReplace}"]`)?.click();
            });
        });

        form.querySelectorAll("[data-login-carousel-delete]").forEach(boton => {
            boton.addEventListener("click", () => {
                const numero = boton.dataset.loginCarouselDelete;
                const eliminar = form.querySelector(`[data-login-carousel-remove="${numero}"]`);
                if (eliminar && valorCampo(`carruselImagen${numero}`)) {
                    eliminar.checked = true;
                }
                limpiarSeleccionArchivo(`carruselImagen${numero}`);
                actualizarGestionCarruselPanel();
            });
        });
    };

    const actualizarGestionCarruselPanel = () => {
        if (!carouselGrid) return;

        const activas = obtenerImagenesCarruselActivas();
        const total = activas.length;
        const manager = carouselGrid.previousElementSibling?.classList?.contains("login-carousel-manager")
            ? carouselGrid.previousElementSibling
            : null;

        form.querySelectorAll("[data-login-carousel-counter]").forEach(contador => {
            contador.textContent = `${total}/3 imagenes cargadas`;
        });

        manager?.querySelector("[data-login-carousel-empty]")?.classList.toggle("d-none", total > 0);
        form.querySelectorAll("[data-login-carousel-upload-main]").forEach(boton => {
            boton.disabled = total >= 3;
            boton.classList.toggle("d-none", total >= 3 && total > 0);
        });

        [1, 2, 3].forEach(numero => {
            const item = obtenerItemCarruselPanel(numero);
            if (!item) return;
            const visible = Boolean(obtenerImagenCarrusel(numero));
            item.classList.toggle("d-none", !visible);
            item.querySelector(`[data-carousel-crop-button="${numero}"]`)?.classList.toggle("d-none", !visible);
        });
    };

    const aplicarEncuadreMiniPreview = numero => {
        const imagen = form.querySelector(`[data-login-file-preview="carruselImagen${numero}"] img`);
        aplicarCropMiniPreview(imagen, obtenerCropCarrusel(numero));
    };

    const obtenerSrcMiniPreviewActual = contenedor => {
        const imagen = contenedor?.querySelector("img");
        const src = imagen?.getAttribute("src") || "";
        return src.trim();
    };

    const actualizarMiniPreview = (clave, src, notaEliminacion = false, seleccionTemporal = false) => {
        const contenedor = form.querySelector(`[data-login-file-preview="${clave}"]`);
        if (!contenedor) return;

        let imagen = contenedor.querySelector("img");
        const vacio = contenedor.querySelector(`[data-login-file-empty="${clave}"]`);
        const nota = contenedor.querySelector(`[data-login-file-remove-note="${clave}"]`);
        const limpiar = contenedor.querySelector(`[data-login-file-clear="${clave}"]`);

        if (src && !notaEliminacion) {
            if (!imagen) {
                imagen = document.createElement("img");
                imagen.alt = "Vista previa";
                contenedor.prepend(imagen);
            }

            imagen.src = src;
            imagen.classList.remove("d-none");
            if (vacio) vacio.classList.add("d-none");
            if (nota) nota.classList.add("d-none");
            if (limpiar) limpiar.classList.toggle("d-none", !seleccionTemporal);
            if (clave.startsWith("carruselImagen")) {
                const numero = clave.replace("carruselImagen", "");
                aplicarEncuadreMiniPreview(numero);
                actualizarBotonEncuadreCarrusel(numero);
            }
            return;
        }

        if (imagen) {
            imagen.classList.add("d-none");
            imagen.removeAttribute("src");
            imagen.removeAttribute("style");
        }
        if (vacio) {
            vacio.textContent = notaEliminacion ? "Se eliminará al guardar" : "Sin imagen seleccionada";
            vacio.classList.remove("d-none");
        }
        if (nota) {
            nota.classList.toggle("d-none", !notaEliminacion);
        }
        if (limpiar) limpiar.classList.add("d-none");
        if (clave.startsWith("carruselImagen")) {
            actualizarBotonEncuadreCarrusel(clave.replace("carruselImagen", ""));
        }
    };

    const restaurarMiniPreview = clave => {
        const eliminar = checkboxEliminarActual(clave);
        if (eliminar?.checked) {
            actualizarMiniPreview(clave, "", true);
            return;
        }

        const contenedor = form.querySelector(`[data-login-file-preview="${clave}"]`);
        actualizarMiniPreview(clave, valorCampo(clave) || obtenerSrcMiniPreviewActual(contenedor));
    };

    const limpiarSeleccionArchivo = clave => {
        const input = inputArchivo(clave);
        if (input) input.value = "";

        if (clave === "imagenFondo") {
            if (imagenTemporal) {
                URL.revokeObjectURL(imagenTemporal);
                imagenTemporal = null;
            }
            restaurarMiniPreview(clave);
            aplicarPreviewLogin();
            return;
        }

        const numero = clave.replace("carruselImagen", "");
        if (numero && carruselTemporal[numero]) {
            URL.revokeObjectURL(carruselTemporal[numero]);
            delete carruselTemporal[numero];
        }

        restaurarMiniPreview(clave);
        aplicarPreviewLogin();
    };

    const actualizarCarruselPreview = (doc, pagina, posicion) => {
        if (!pagina) return;

        let carrusel = doc.querySelector("[data-login-carousel]");
        const imagenes = [1, 2, 3]
            .map(numero => ({
                numero,
                src: obtenerImagenCarrusel(numero),
                crop: obtenerCropCarrusel(numero)
            }))
            .filter(imagen => Boolean(imagen.src));
        const mostrar = checkActivo("carruselActivo") && posicion !== "CENTRO" && imagenes.length > 0;

        pagina.classList.toggle("login-carousel-enabled", mostrar);

        if (!mostrar) {
            if (carrusel) carrusel.remove();
            return;
        }

        if (!carrusel) {
            carrusel = doc.createElement("aside");
            carrusel.className = "login-visual-carousel";
            carrusel.dataset.loginCarousel = "";

            const loginCard = doc.querySelector(".login-card");
            pagina.insertBefore(carrusel, loginCard || pagina.firstChild);
        }

        carrusel.innerHTML = `
            <div class="login-carousel-track">
                ${imagenes.map(imagen => `
                    <figure class="login-carousel-slide">
                        <div class="login-carousel-image-frame">
                            <img class="login-carousel-image"
                                 src="${imagen.src}"
                                 alt="Imagen del login"
                                 style="--carousel-zoom: ${imagen.crop.zoom}; --carousel-offset-x: ${imagen.crop.offsetX}%; --carousel-offset-y: ${imagen.crop.offsetY}%;">
                        </div>
                    </figure>
                `).join("")}
            </div>
            ${imagenes.length > 1 ? `
                <div class="login-carousel-controls" data-login-carousel-controls>
                    <button type="button" class="login-carousel-nav login-carousel-prev" data-login-carousel-prev aria-label="Imagen anterior">‹</button>
                    <button type="button" class="login-carousel-nav login-carousel-next" data-login-carousel-next aria-label="Imagen siguiente">›</button>
                    <div class="login-carousel-dots" data-login-carousel-dots aria-hidden="true">
                        ${imagenes.map((_, indice) => `<span class="login-carousel-dot${indice === 0 ? " active" : ""}"></span>`).join("")}
                    </div>
                </div>
            ` : ""}
        `;
    };

    const bloquearPreview = doc => {
        const formLogin = doc.getElementById("loginEmpleadoForm");
        if (formLogin) {
            formLogin.dataset.loginPreview = "true";
            formLogin.addEventListener("submit", event => event.preventDefault());
        }

        doc.querySelectorAll("[data-bs-toggle], .secondary-link").forEach(elemento => {
            elemento.addEventListener("click", event => {
                event.preventDefault();
                event.stopPropagation();
            });
        });
    };

    const actualizarCropEditor = () => {
        if (!carouselCropStage || !carouselCropZoom) return;
        const medidas = obtenerMedidasStageCrop();
        cropEstado.zoom = numeroEnRango(cropEstado.zoom, 1, 4, 1);
        cropEstado.offsetX = numeroEnRango(cropEstado.offsetX, -medidas.ancho, medidas.ancho, 0);
        cropEstado.offsetY = numeroEnRango(cropEstado.offsetY, -medidas.alto, medidas.alto, 0);

        carouselCropStage.style.setProperty("--crop-zoom", cropEstado.zoom.toString());
        carouselCropStage.style.setProperty("--crop-offset-x", `${cropEstado.offsetX}px`);
        carouselCropStage.style.setProperty("--crop-offset-y", `${cropEstado.offsetY}px`);
        carouselCropZoom.value = cropEstado.zoom.toString();
        if (carouselCropZoomLabel) {
            carouselCropZoomLabel.textContent = `${cropEstado.zoom.toFixed(2)}x`;
        }
    };

    const ajustarBaseImagenCrop = () => {
        if (!carouselCropStage || !carouselCropImage?.naturalWidth || !carouselCropImage?.naturalHeight) return;

        const medidas = obtenerMedidasStageCrop();
        const escalaBase = Math.min(
            medidas.ancho / carouselCropImage.naturalWidth,
            medidas.alto / carouselCropImage.naturalHeight
        );

        carouselCropImage.style.width = `${carouselCropImage.naturalWidth * escalaBase}px`;
        carouselCropImage.style.height = `${carouselCropImage.naturalHeight * escalaBase}px`;
        actualizarCropEditor();
    };

    let disparadorModalEncuadre = null;

    const cerrarModalEncuadre = () => {
        if (!carouselCropModalElement) return;
        const elementoActivo = document.activeElement;
        if (elementoActivo instanceof HTMLElement && carouselCropModalElement.contains(elementoActivo)) {
            elementoActivo.blur();
        }
        carouselCropModalElement.classList.remove("active");
        carouselCropModalElement.setAttribute("aria-hidden", "true");
        carouselCropStage?.classList.remove("dragging");
        if (cropDrag?.pointerId && carouselCropStage?.hasPointerCapture?.(cropDrag.pointerId)) {
            carouselCropStage.releasePointerCapture(cropDrag.pointerId);
        }
        if (carouselCropImage) {
            carouselCropImage.removeAttribute("src");
            carouselCropImage.removeAttribute("style");
        }
        cropDrag = null;
        carruselCropActivo = null;
        if (disparadorModalEncuadre?.isConnected && !disparadorModalEncuadre.disabled) {
            disparadorModalEncuadre.focus({ preventScroll: true });
        }
        disparadorModalEncuadre = null;
    };

    const abrirModalEncuadre = (numero, trigger = document.activeElement) => {
        const src = obtenerImagenCarrusel(numero);
        if (!src || !carouselCropModalElement || !carouselCropImage) return;

        carruselCropActivo = numero;
        cropEstado = convertirOffsetAStage(obtenerCropCarrusel(numero));
        carouselCropImage.onload = ajustarBaseImagenCrop;
        carouselCropImage.src = src;
        disparadorModalEncuadre = trigger instanceof HTMLElement ? trigger : null;
        carouselCropModalElement.classList.add("active");
        carouselCropModalElement.setAttribute("aria-hidden", "false");
        carouselCropModalElement.querySelector("[data-carousel-crop-close]")?.focus({ preventScroll: true });
        if (carouselCropImage.complete) {
            ajustarBaseImagenCrop();
        }
    };

    const aplicarEncuadreCarrusel = () => {
        if (!carruselCropActivo) return;

        escribirCropCarrusel(carruselCropActivo, cropEstado);
        aplicarEncuadreMiniPreview(carruselCropActivo);
        aplicarPreviewLogin();
        cerrarModalEncuadre();
    };

    const iniciarDragCrop = event => {
        if (!carouselCropStage || !carruselCropActivo) return;
        event.preventDefault();
        carouselCropStage.classList.add("dragging");
        carouselCropStage.setPointerCapture?.(event.pointerId);
        cropDrag = {
            pointerId: event.pointerId,
            startX: event.clientX,
            startY: event.clientY,
            offsetX: cropEstado.offsetX,
            offsetY: cropEstado.offsetY
        };
    };

    const moverDragCrop = event => {
        if (!cropDrag || cropDrag.pointerId !== event.pointerId) return;
        cropEstado.offsetX = cropDrag.offsetX + event.clientX - cropDrag.startX;
        cropEstado.offsetY = cropDrag.offsetY + event.clientY - cropDrag.startY;
        actualizarCropEditor();
    };

    const terminarDragCrop = event => {
        if (!cropDrag || cropDrag.pointerId !== event.pointerId) return;
        carouselCropStage?.classList.remove("dragging");
        carouselCropStage?.releasePointerCapture?.(event.pointerId);
        cropDrag = null;
    };

    const aplicarPreviewLogin = () => {
        const doc = obtenerDocumentoIframe();
        if (!doc) return;

        const root = doc.documentElement;
        const pagina = doc.querySelector(".login-page");
        const tipoFondo = valorCampo("tipoFondo") || "DEGRADADO";
        const posicion = valorCampo("posicionCard") || "CENTRO";

        const variables = {
            "--login-bg-1": valorColor("colorFondo1"),
            "--login-bg-2": valorColor("colorFondo2"),
            "--login-card": valorColor("colorCard"),
            "--login-text": valorColor("colorTexto"),
            "--login-btn-1": valorColor("colorBoton1"),
            "--login-btn-2": valorColor("colorBoton2"),
            "--login-btn-text": valorColor("colorTextoBoton")
        };

        Object.entries(variables).forEach(([variable, valor]) => {
            if (valor) {
                root.style.setProperty(variable, valor);
            }
        });

        if (pagina) {
            aplicarClase(pagina, "login-bg-", tipoFondo);
            aplicarClase(pagina, "login-position-", posicion);

            const rutaImagen = imagenTemporal || valorCampo("imagenFondo");
            if (tipoFondo === "IMAGEN" && rutaImagen) {
                const fondoDisponible = removeImage?.checked ? "" : rutaImagen;
                if (fondoDisponible) {
                    pagina.style.setProperty("--login-bg-image", `url("${fondoDisponible}")`);
                } else {
                    pagina.style.removeProperty("--login-bg-image");
                }
            } else {
                pagina.style.removeProperty("--login-bg-image");
            }

            actualizarCarruselPreview(doc, pagina, posicion);
        }

        const titulo = doc.querySelector('[data-login-preview-text="titulo"]');
        const subtitulo = doc.querySelector('[data-login-preview-text="subtitulo"]');
        const texto = doc.querySelector('[data-login-preview-text="textoInformativo"]');

        if (titulo) titulo.textContent = valorCampo("titulo") || "DentIA";
        if (subtitulo) subtitulo.textContent = valorCampo("subtitulo") || "Sistema interno de gestión";
        if (texto) texto.textContent = valorCampo("textoInformativo") || "Acceso exclusivo para trabajadores";

        bloquearPreview(doc);
    };

    colorInputs.forEach(input => {
        const nombre = input.dataset.loginColorInput;
        if (nombre && input.value) {
            actualizarSwatch(nombre, input.value);
        }

        input.addEventListener("input", () => sincronizarColor(nombre, input.value));
    });

    colorTexts.forEach(input => {
        const nombre = input.dataset.loginColorText;
        input.addEventListener("input", () => {
            const valor = input.value.trim();
            if (esHex(valor)) {
                sincronizarColor(nombre, valor);
            }
        });
    });

    inputs.forEach(input => {
        input.addEventListener("input", aplicarPreviewLogin);
        input.addEventListener("change", aplicarPreviewLogin);
    });

    if (imageInput) {
        imageInput.addEventListener("change", () => {
            const archivo = imageInput.files && imageInput.files[0];
            if (!archivo) {
                imagenTemporal = null;
                restaurarMiniPreview("imagenFondo");
                aplicarPreviewLogin();
                return;
            }

            const tiposPermitidos = ["image/jpeg", "image/jpg", "image/png", "image/webp"];
            if (!tiposPermitidos.includes(archivo.type)) {
                imageInput.value = "";
                imagenTemporal = null;
                restaurarMiniPreview("imagenFondo");
                aplicarPreviewLogin();
                return;
            }

            if (imagenTemporal) {
                URL.revokeObjectURL(imagenTemporal);
            }

            imagenTemporal = URL.createObjectURL(archivo);
            actualizarMiniPreview("imagenFondo", imagenTemporal, false, true);
            const tipoFondo = form.querySelector('[data-login-config="tipoFondo"]');
            if (tipoFondo) tipoFondo.value = "IMAGEN";
            if (removeImage) removeImage.checked = false;
            aplicarPreviewLogin();
        });
    }

    if (removeImage) {
        removeImage.addEventListener("change", () => {
            if (removeImage.checked) {
                limpiarSeleccionArchivo("imagenFondo");
                actualizarMiniPreview("imagenFondo", "", true);
            } else {
                restaurarMiniPreview("imagenFondo");
            }
            aplicarPreviewLogin();
        });
    }

    carouselFiles.forEach(input => {
        input.addEventListener("change", () => {
            const numero = input.dataset.loginCarouselFile;
            const archivo = input.files && input.files[0];

            if (!numero || !archivo) {
                aplicarPreviewLogin();
                actualizarGestionCarruselPanel();
                return;
            }

            const tiposPermitidos = ["image/jpeg", "image/jpg", "image/png", "image/webp"];
            if (!tiposPermitidos.includes(archivo.type)) {
                input.value = "";
                delete carruselTemporal[numero];
                restaurarMiniPreview(`carruselImagen${numero}`);
                aplicarPreviewLogin();
                actualizarGestionCarruselPanel();
                return;
            }

            if (carruselTemporal[numero]) {
                URL.revokeObjectURL(carruselTemporal[numero]);
            }

            carruselTemporal[numero] = URL.createObjectURL(archivo);
            restaurarCropCarrusel(numero);
            actualizarMiniPreview(`carruselImagen${numero}`, carruselTemporal[numero], false, true);
            const activo = form.querySelector('[data-login-config="carruselActivo"]');
            if (activo) activo.checked = true;

            const eliminar = form.querySelector(`[data-login-carousel-remove="${numero}"]`);
            if (eliminar) eliminar.checked = false;

            aplicarPreviewLogin();
            actualizarGestionCarruselPanel();
        });
    });

    carouselRemoves.forEach(input => {
        input.addEventListener("change", () => {
            const numero = input.dataset.loginCarouselRemove;
            if (input.checked && numero) {
                limpiarSeleccionArchivo(`carruselImagen${numero}`);
                actualizarMiniPreview(`carruselImagen${numero}`, "", true);
            } else if (numero) {
                restaurarMiniPreview(`carruselImagen${numero}`);
            }
            aplicarPreviewLogin();
            actualizarGestionCarruselPanel();
        });
    });

    clearFileButtons.forEach(boton => {
        boton.addEventListener("click", () => {
            const clave = boton.dataset.loginFileClear;
            if (!clave) return;
            limpiarSeleccionArchivo(clave);
            actualizarGestionCarruselPanel();
        });
    });

    carouselCropButtons.forEach(boton => {
        boton.addEventListener("click", () => {
            abrirModalEncuadre(boton.dataset.carouselCropButton, boton);
        });
    });

    if (carouselCropStage) {
        carouselCropStage.addEventListener("pointerdown", iniciarDragCrop);
        carouselCropStage.addEventListener("pointermove", moverDragCrop);
        carouselCropStage.addEventListener("pointerup", terminarDragCrop);
        carouselCropStage.addEventListener("pointercancel", terminarDragCrop);
    }

    if (carouselCropZoom) {
        carouselCropZoom.addEventListener("input", () => {
            cropEstado.zoom = numeroEnRango(carouselCropZoom.value, 1, 4, 1);
            actualizarCropEditor();
        });
    }

    if (carouselCropReset) {
        carouselCropReset.addEventListener("click", () => {
        cropEstado = { zoom: 1, offsetX: 0, offsetY: 0 };
        actualizarCropEditor();
        });
    }

    if (carouselCropApply) {
        carouselCropApply.addEventListener("click", aplicarEncuadreCarrusel);
    }

    carouselCropCloseButtons.forEach(boton => {
        boton.addEventListener("click", cerrarModalEncuadre);
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && carouselCropModalElement?.classList.contains("active")) {
            cerrarModalEncuadre();
        }
    });

    deviceButtons.forEach(boton => {
        boton.addEventListener("click", () => {
            activarDispositivoPreview(boton.dataset.loginPreviewDevice);
        });
    });

    crearGestionCarruselPanel();
    ["imagenFondo", "carruselImagen1", "carruselImagen2", "carruselImagen3"].forEach(restaurarMiniPreview);
    [1, 2, 3].forEach(aplicarEncuadreMiniPreview);
    actualizarBotonesEncuadreCarrusel();
    actualizarGestionCarruselPanel();

    iframe.addEventListener("load", () => {
        ajustarEscalaPreviewLogin();
        aplicarPreviewLogin();
    });
    window.addEventListener("resize", ajustarEscalaPreviewLogin);
    activarDispositivoPreview("desktop");
    aplicarPreviewLogin();
}


/* =====================================================
   RESTAURAR - POR SI LUEGO LO USAS
===================================================== */

function confirmarRestaurarMarca() {
    const confirmar = confirm("¿Seguro que deseas restaurar la marca a la versión base?");

    if (confirmar) {
        const form = document.getElementById("formRestaurarMarca");
        if (form) form.submit();
    }
}
