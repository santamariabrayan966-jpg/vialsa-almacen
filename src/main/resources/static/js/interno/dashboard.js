document.addEventListener("DOMContentLoaded", function () {

    const data = window.dashboardDentIA || {};

    const ventasPorDia = data.ventasPorDia || [];
    const ventasPorCanal = data.ventasPorCanal || [];

    /*
        Este dato todavía puede no venir desde tu controller.
        Si no existe, el gráfico mostrará "Sin datos".
    */
    const ventasPorFormaPago =
        data.ventasPorFormaPago ||
        data.formasPago ||
        data.metodosPago ||
        [];

    actualizarKpisCanal(ventasPorCanal);

    crearGraficoCanalVenta(
        "graficoCanalVenta",
        ventasPorCanal
    );

    crearGraficoLinea(
        "graficoVentasDia",
        ventasPorDia,
        "Ventas S/"
    );

    crearGraficoDona(
        "graficoFormaPago",
        ventasPorFormaPago
    );
});

/* ==============================
   PALETA PROFESIONAL DentIA
============================== */

const PALETA = {
    rojo: "rgba(220, 53, 69, 0.88)",
    rojoSuave: "rgba(220, 53, 69, 0.10)",

    azul: "rgba(37, 99, 235, 0.88)",
    azulSuave: "rgba(37, 99, 235, 0.10)",

    indigo: "rgba(99, 102, 241, 0.88)",
    indigoSuave: "rgba(99, 102, 241, 0.10)",

    verde: "rgba(34, 197, 94, 0.88)",
    teal: "rgba(20, 184, 166, 0.88)",

    naranja: "rgba(245, 158, 11, 0.88)",
    gris: "rgba(100, 116, 139, 0.80)"
};

/* ==============================
   UTILIDADES
============================== */

function obtenerLabels(lista) {
    return lista.map(item =>
        item.etiqueta ||
        item.ETIQUETA ||
        item.canal ||
        item.CANAL ||
        item.formaPago ||
        item.FORMA_PAGO ||
        item.metodoPago ||
        item.METODO_PAGO ||
        "-"
    );
}

function obtenerValores(lista) {
    return lista.map(item =>
        Number(
            item.valor ||
            item.VALOR ||
            item.total ||
            item.TOTAL ||
            item.monto ||
            item.MONTO ||
            0
        )
    );
}

function formatearSoles(valor) {
    return "S/ " + Number(valor || 0).toLocaleString("es-PE", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function tieneDatos(lista) {
    return Array.isArray(lista) && lista.length > 0 && obtenerValores(lista).some(v => v > 0);
}

/* ==============================
   KPI ONLINE / PRESENCIAL
============================== */

function actualizarKpisCanal(lista) {

    const totalOnline = obtenerTotalPorTexto(lista, "online");
    const totalPresencial = obtenerTotalPorTexto(lista, "presencial");

    const kpiOnline = document.getElementById("kpiVentasOnline");
    const kpiPresencial = document.getElementById("kpiVentasPresenciales");

    if (kpiOnline) {
        kpiOnline.textContent = formatearSoles(totalOnline);
    }

    if (kpiPresencial) {
        kpiPresencial.textContent = formatearSoles(totalPresencial);
    }
}

function obtenerTotalPorTexto(lista, textoBuscado) {

    return lista.reduce((total, item) => {

        const etiqueta = String(
            item.etiqueta ||
            item.ETIQUETA ||
            item.canal ||
            item.CANAL ||
            ""
        ).toLowerCase();

        const valor = Number(
            item.valor ||
            item.VALOR ||
            item.total ||
            item.TOTAL ||
            item.monto ||
            item.MONTO ||
            0
        );

        if (etiqueta.includes(textoBuscado.toLowerCase())) {
            return total + valor;
        }

        return total;

    }, 0);
}

/* ==============================
   GRÁFICO: ONLINE VS PRESENCIAL
============================== */

function crearGraficoCanalVenta(idCanvas, lista) {

    const canvas = document.getElementById(idCanvas);

    if (!canvas) return;

    if (!tieneDatos(lista)) {
        dibujarGraficoVacio(canvas, "Sin datos de ventas por canal");
        return;
    }

    const labels = obtenerLabels(lista);
    const valores = obtenerValores(lista);

    new Chart(canvas, {
        type: "bar",
        data: {
            labels: labels,
            datasets: [{
                label: "Total vendido",
                data: valores,
                backgroundColor: labels.map(label => colorPorCanal(label)),
                borderRadius: 10,
                maxBarThickness: 54
            }]
        },
        options: {
            ...opcionesBase(),
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return formatearSoles(context.raw);
                        }
                    }
                }
            }
        }
    });
}

function colorPorCanal(label) {

    const texto = String(label || "").toLowerCase();

    if (texto.includes("presencial")) {
        return PALETA.azul;
    }

    if (texto.includes("online")) {
        return PALETA.indigo;
    }

    return PALETA.gris;
}

/* ==============================
   GRÁFICO: RENDIMIENTO SEMANAL
============================== */

function crearGraficoLinea(idCanvas, lista, etiqueta) {

    const canvas = document.getElementById(idCanvas);

    if (!canvas) return;

    if (!tieneDatos(lista)) {
        dibujarGraficoVacio(canvas, "Sin datos de ventas por día");
        return;
    }

    new Chart(canvas, {
        type: "line",
        data: {
            labels: obtenerLabels(lista),
            datasets: [{
                label: etiqueta,
                data: obtenerValores(lista),
                tension: 0.35,
                fill: true,

                borderColor: PALETA.azul,
                backgroundColor: PALETA.azulSuave,
                pointBackgroundColor: PALETA.azul,
                pointBorderColor: "#ffffff",

                pointRadius: 4,
                pointHoverRadius: 6
            }]
        },
        options: {
            ...opcionesBase(),
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return formatearSoles(context.raw);
                        }
                    }
                }
            }
        }
    });
}

/* ==============================
   GRÁFICO: MÉTODOS DE PAGO
============================== */

function crearGraficoDona(idCanvas, lista) {

    const canvas = document.getElementById(idCanvas);

    if (!canvas) return;

    if (!tieneDatos(lista)) {
        dibujarGraficoVacio(canvas, "Sin datos de métodos de pago");
        return;
    }

    const labels = obtenerLabels(lista);
    const valores = obtenerValores(lista);

    new Chart(canvas, {
        type: "doughnut",
        data: {
            labels: labels,
            datasets: [{
                data: valores,
                backgroundColor: labels.map(label => colorPorMetodoPago(label)),
                borderWidth: 2,
                borderColor: "#ffffff"
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: "62%",
            plugins: {
                legend: {
                    position: "bottom",
                    labels: {
                        usePointStyle: true,
                        boxWidth: 8,
                        padding: 14
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return context.label + ": " + formatearSoles(context.raw);
                        }
                    }
                }
            }
        }
    });
}

function colorPorMetodoPago(label) {

    const texto = String(label || "").toLowerCase();

    if (texto.includes("yape")) {
        return PALETA.verde;
    }

    if (texto.includes("plin")) {
        return PALETA.teal;
    }

    if (texto.includes("tarjeta")) {
        return PALETA.indigo;
    }

    if (texto.includes("efectivo")) {
        return PALETA.naranja;
    }

    if (texto.includes("transferencia")) {
        return PALETA.azul;
    }

    return PALETA.gris;
}

/* ==============================
   OPCIONES GENERALES
============================== */

function opcionesBase() {
    return {
        responsive: true,
        maintainAspectRatio: false,
        animation: {
            duration: 700
        },
        plugins: {
            legend: {
                display: true
            }
        },
        scales: {
            x: {
                grid: {
                    display: false
                },
                ticks: {
                    color: "#64748b"
                }
            },
            y: {
                beginAtZero: true,
                grid: {
                    color: "rgba(148, 163, 184, 0.22)"
                },
                ticks: {
                    color: "#64748b",
                    callback: function (value) {
                        return "S/ " + Number(value).toLocaleString("es-PE");
                    }
                }
            }
        }
    };
}

/* ==============================
   GRÁFICO VACÍO
============================== */

function dibujarGraficoVacio(canvas, texto) {

    new Chart(canvas, {
        type: "bar",
        data: {
            labels: [""],
            datasets: [{
                data: [0],
                backgroundColor: "rgba(0,0,0,0)"
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    enabled: false
                }
            },
            scales: {
                x: {
                    display: false
                },
                y: {
                    display: false
                }
            }
        },
        plugins: [{
            id: "noDataText",
            afterDraw(chart) {
                const {ctx, chartArea} = chart;

                if (!chartArea) return;

                ctx.save();
                ctx.font = "600 14px Arial";
                ctx.fillStyle = "#94a3b8";
                ctx.textAlign = "center";
                ctx.textBaseline = "middle";
                ctx.fillText(
                    texto,
                    (chartArea.left + chartArea.right) / 2,
                    (chartArea.top + chartArea.bottom) / 2
                );
                ctx.restore();
            }
        }]
    });
}