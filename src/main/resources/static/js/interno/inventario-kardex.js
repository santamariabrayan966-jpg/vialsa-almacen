document.addEventListener("DOMContentLoaded", () => {
    inicializarDetalleKardex();
    inicializarPaginacionKardex();
});

function inicializarDetalleKardex() {
    const botones = document.querySelectorAll(".btnDetalleKardex");

    botones.forEach(btn => {
        btn.addEventListener("click", () => {
            const tipo = btn.dataset.tipo || "";

            document.getElementById("detalleKardexDocumento").textContent = btn.dataset.documento || "Sin documento";
            document.getElementById("detalleKardexDocumento2").textContent = btn.dataset.documento || "Sin documento";

            document.getElementById("detalleKardexFecha").textContent = btn.dataset.fecha || "";
            document.getElementById("detalleKardexSede").textContent = btn.dataset.sede || "";
            document.getElementById("detalleKardexTipoSede").textContent = btn.dataset.tipoSede || "";
            document.getElementById("detalleKardexUsuario").textContent = btn.dataset.usuario || "Sistema";

            const tipoSpan = document.getElementById("detalleKardexTipo");
            tipoSpan.textContent = formatearTipoKardex(tipo);
            tipoSpan.className = "badge px-3 py-2";
            tipoSpan.classList.add(tipo === "ENTRADA" ? "text-bg-success" : "text-bg-danger");

            document.getElementById("detalleKardexOrigen").textContent =
                formatearOrigenKardex(btn.dataset.origen || "");

            document.getElementById("detalleKardexProducto").textContent = btn.dataset.producto || "";
            document.getElementById("detalleKardexCodigoProducto").textContent = btn.dataset.codigoProducto || "";

            document.getElementById("detalleKardexEntrada").textContent = btn.dataset.entrada || "0";
            document.getElementById("detalleKardexSalida").textContent = btn.dataset.salida || "0";
            document.getElementById("detalleKardexStock").textContent = btn.dataset.stock || "0";
            document.getElementById("detalleKardexCosto").textContent =
                `S/ ${btn.dataset.costo || "0"}`;

            document.getElementById("detalleKardexObservacion").textContent =
                btn.dataset.observacion || "Sin observación";

            const btnExportarDetalle = document.getElementById("btnExportarDetalleKardex");

            if (btnExportarDetalle) {
                btnExportarDetalle.href = `/interno/inventario/kardex/movimiento/${btn.dataset.id}/pdf`;
            }
        });
    });
}

function inicializarPaginacionKardex() {
    const filas = Array.from(document.querySelectorAll("tbody tr"))
        .filter(fila => fila.querySelector(".btnDetalleKardex"));

    const contador = document.getElementById("contadorKardexVisibles");
    const contadorTotal = document.getElementById("contadorKardexTotal");
    const paginaActualTexto = document.getElementById("paginaKardexActual");
    const paginaTotalTexto = document.getElementById("paginaKardexTotal");

    const btnAnterior = document.getElementById("btnKardexAnterior");
    const btnSiguiente = document.getElementById("btnKardexSiguiente");

    const limiteVisible = 5;
    let paginaActual = 1;

    const renderizar = () => {
        const total = filas.length;
        const totalPaginas = Math.max(1, Math.ceil(total / limiteVisible));

        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas;
        }

        const inicio = (paginaActual - 1) * limiteVisible;
        const fin = inicio + limiteVisible;

        filas.forEach(fila => fila.classList.add("d-none"));

        filas.slice(inicio, fin).forEach(fila => {
            fila.classList.remove("d-none");
        });

        if (contador) {
            contador.textContent = Math.min(limiteVisible, Math.max(0, total - inicio));
        }

        if (contadorTotal) {
            contadorTotal.textContent = total;
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

    btnAnterior?.addEventListener("click", () => {
        if (paginaActual > 1) {
            paginaActual--;
            renderizar();
        }
    });

    btnSiguiente?.addEventListener("click", () => {
        const totalPaginas = Math.max(1, Math.ceil(filas.length / limiteVisible));

        if (paginaActual < totalPaginas) {
            paginaActual++;
            renderizar();
        }
    });

    renderizar();
}

function imprimirKardexDirecto(html) {
    const iframe = document.createElement("iframe");

    iframe.style.position = "fixed";
    iframe.style.right = "0";
    iframe.style.bottom = "0";
    iframe.style.width = "0";
    iframe.style.height = "0";
    iframe.style.border = "0";
    iframe.style.opacity = "0";

    document.body.appendChild(iframe);

    const ventanaIframe = iframe.contentWindow;
    const documentoIframe = ventanaIframe.document;

    documentoIframe.open();
    documentoIframe.write(html);
    documentoIframe.close();

    setTimeout(() => {
        ventanaIframe.focus();
        ventanaIframe.print();

        setTimeout(() => {
            iframe.remove();
        }, 1000);
    }, 500);
}

function formatearTipoKardex(valor) {
    return valor === "ENTRADA" ? "Entrada" : "Salida";
}

function formatearOrigenKardex(valor) {
    switch (valor) {
        case "COMPRA": return "Compra";
        case "VENTA": return "Venta";
        case "TRANSFERENCIA": return "Transferencia";
        case "AJUSTE": return "Ajuste";
        default: return valor || "Sin origen";
    }
}
