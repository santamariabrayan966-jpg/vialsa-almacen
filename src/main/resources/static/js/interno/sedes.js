function nuevaSede() {
    document.getElementById("tituloModalSede").textContent = "Nueva Sede / Almacén";

    document.getElementById("idSede").value = "";
    document.getElementById("nombre").value = "";
    document.getElementById("codigo").value = "";
    document.getElementById("tipo").value = "";
    document.getElementById("direccion").value = "";
    document.getElementById("ciudad").value = "";
    document.getElementById("responsable").value = "";
    document.getElementById("telefono").value = "";
    document.getElementById("descripcion").value = "";
    document.getElementById("googleMapsUrl").value = "";
    document.getElementById("archivoFoto").value = "";
    document.getElementById("eliminarFoto").value = "false";
    document.getElementById("previewFotoSede").classList.add("d-none");
    document.getElementById("previewFotoSede").src = "";
    document.getElementById("previewIconoSede").classList.remove("d-none");
    document.getElementById("btnEliminarFoto").classList.add("d-none");

    if (typeof limpiarErroresSede === "function") {
        limpiarErroresSede();
    }
}

async function editarSede(id) {
    document.getElementById("tituloModalSede").textContent = "Editar Sede / Almacén";

    const resp = await fetch(`/interno/sedes/api/${id}`);

    if (!resp.ok) {
        alert("No se pudo cargar la sede.");
        return;
    }

    const s = await resp.json();

    document.getElementById("idSede").value = s.idSede || "";
    document.getElementById("nombre").value = s.nombre || "";
    document.getElementById("codigo").value = s.codigo || "";
    document.getElementById("tipo").value = s.tipo || "";
    document.getElementById("direccion").value = s.direccion || "";
    document.getElementById("ciudad").value = s.ciudad || "";
    document.getElementById("responsable").value = s.responsable || "";
    document.getElementById("telefono").value = s.telefono || "";
    document.getElementById("descripcion").value = s.descripcion || "";
    document.getElementById("googleMapsUrl").value = s.googleMapsUrl || "";
    document.getElementById("archivoFoto").value = "";
    document.getElementById("eliminarFoto").value = "false";

    if (typeof limpiarErroresSede === "function") {
        limpiarErroresSede();
    }

       if (s.foto) {
           document.getElementById("previewFotoSede").src = `/uploads/sedes/${s.foto}`;
           document.getElementById("previewFotoSede").classList.remove("d-none");
           document.getElementById("previewIconoSede").classList.add("d-none");
           document.getElementById("btnEliminarFoto").classList.remove("d-none");
       } else {
           document.getElementById("previewFotoSede").classList.add("d-none");
           document.getElementById("previewFotoSede").src = "";
           document.getElementById("previewIconoSede").classList.remove("d-none");
           document.getElementById("btnEliminarFoto").classList.add("d-none");
       }
}

document.addEventListener("DOMContentLoaded", function () {
    const buscar = document.getElementById("buscarSede");
    const ciudad = document.getElementById("filtroCiudad");
    const tipo = document.getElementById("filtroTipo");
    const estado = document.getElementById("filtroEstado");
    const limpiar = document.getElementById("btnLimpiarFiltros");
    const filas = document.querySelectorAll(".fila-sede");
    const archivoFoto = document.getElementById("archivoFoto");
    const previewFoto = document.getElementById("previewFotoSede");
    const previewIcono = document.getElementById("previewIconoSede");
    const btnEliminarFoto = document.getElementById("btnEliminarFoto");
    const eliminarFoto = document.getElementById("eliminarFoto");
    const selectTipoModal = document.getElementById("tipo");
    const codigoInput = document.getElementById("codigo");
    const btnVista = document.getElementById("btnVista");
    const vistaTabla = document.getElementById("vistaTabla");
    const vistaCards = document.getElementById("vistaCards");
    const formSede = document.getElementById("formSede");
    const alertaModal = document.getElementById("alertaModalSede");
    const mensajeAlertaModal = document.getElementById("mensajeAlertaModalSede");
    const formHorariosSede = document.getElementById("formHorariosSede");
    const alertaHorariosSede = document.getElementById("alertaHorariosSede");
    const mensajeAlertaHorariosSede = document.getElementById("mensajeAlertaHorariosSede");

    const reglasSede = {
        nombre: [
            { valido: valor => valor.length > 0, mensaje: "El nombre es obligatorio." }
        ],
        codigo: [
            { valido: valor => valor.length > 0, mensaje: "Selecciona el tipo para generar el código." }
        ],
        tipo: [
            { valido: valor => valor.length > 0, mensaje: "El tipo es obligatorio." }
        ],
        ciudad: [
            { valido: valor => valor.length > 0, mensaje: "La ciudad es obligatoria." }
        ],
        direccion: [
            { valido: valor => valor.length > 0, mensaje: "La dirección es obligatoria." }
        ],
        telefono: [
            { valido: valor => valor.length > 0, mensaje: "El teléfono es obligatorio." },
            { valido: valor => valor.length >= 6 && valor.length <= 20, mensaje: "El teléfono debe tener entre 6 y 20 caracteres." }
        ],
        responsable: [
            { valido: valor => valor.length === 0 || valor.length >= 3, mensaje: "El responsable debe tener al menos 3 caracteres." }
        ],
        googleMapsUrl: [
            { valido: valor => valor.length === 0 || valor.startsWith("http://") || valor.startsWith("https://"), mensaje: "El link debe empezar por http:// o https://." }
        ]
    };

    function mostrarErrorCampo(campo, mensaje) {
        if (!campo) return;

        campo.classList.add("is-invalid");

        let feedback = campo.parentElement.querySelector(`.invalid-feedback[data-error-for="${campo.id}"]`);

        if (!feedback) {
            feedback = document.createElement("div");
            feedback.className = "invalid-feedback";
            feedback.dataset.errorFor = campo.id;
            campo.insertAdjacentElement("afterend", feedback);
        }

        feedback.textContent = mensaje;
    }

    function limpiarErrorCampo(campo) {
        if (!campo) return;

        campo.classList.remove("is-invalid");

        const feedback = campo.parentElement.querySelector(`.invalid-feedback[data-error-for="${campo.id}"]`);

        if (feedback) {
            feedback.textContent = "";
        }
    }

    function validarCampoSede(campo) {
        if (!campo || !reglasSede[campo.id]) {
            return true;
        }

        const valor = (campo.value || "").trim();
        const reglaFallida = reglasSede[campo.id].find(regla => !regla.valido(valor));

        if (reglaFallida) {
            mostrarErrorCampo(campo, reglaFallida.mensaje);
            return false;
        }

        limpiarErrorCampo(campo);
        return true;
    }

    window.limpiarErroresSede = function () {
        Object.keys(reglasSede).forEach(id => limpiarErrorCampo(document.getElementById(id)));

        if (alertaModal) {
            alertaModal.classList.add("d-none");
        }

        if (mensajeAlertaModal) {
            mensajeAlertaModal.textContent = "";
        }
    };

    Object.keys(reglasSede).forEach(id => {
        const campo = document.getElementById(id);

        campo?.addEventListener("input", () => validarCampoSede(campo));
        campo?.addEventListener("change", () => validarCampoSede(campo));
    });

    btnVista?.addEventListener("click", function () {
        const mostrandoCards = !vistaCards.classList.contains("d-none");

        if (mostrandoCards) {
            vistaCards.classList.add("d-none");
            vistaTabla.classList.remove("d-none");
            btnVista.innerHTML = '<i class="bi bi-grid-3x3-gap me-1"></i> Vista';
        } else {
            vistaTabla.classList.add("d-none");
            vistaCards.classList.remove("d-none");
            btnVista.innerHTML = '<i class="bi bi-table me-1"></i> Tabla';
        }
    });

    selectTipoModal?.addEventListener("change", async function () {
        const tipo = this.value;

        if (!tipo || !codigoInput) {
            return;
        }

        const resp = await fetch(`/interno/sedes/generar-codigo?tipo=${tipo}`);

        if (!resp.ok) {
            codigoInput.value = "";
            return;
        }

        codigoInput.value = await resp.text();
        validarCampoSede(codigoInput);
    });

    formSede?.addEventListener("submit", function (e) {
        const campos = Object.keys(reglasSede)
            .map(id => document.getElementById(id))
            .filter(Boolean);

        const valido = campos.every(campo => validarCampoSede(campo));

        if (!valido) {
            e.preventDefault();

            if (mensajeAlertaModal) {
                mensajeAlertaModal.textContent = "Revisa los campos marcados antes de guardar.";
            }

            if (alertaModal) {
                alertaModal.classList.remove("d-none");
            }

            const primerError = campos.find(campo => campo.classList.contains("is-invalid"));
            primerError?.focus();
            return;
        }

        e.preventDefault();

        const botonGuardar = formSede.querySelector('button[type="submit"]');
        const textoBoton = botonGuardar?.innerHTML;

        if (botonGuardar) {
            botonGuardar.disabled = true;
            botonGuardar.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Guardando';
        }

        fetch(formSede.action, {
            method: "POST",
            body: new FormData(formSede),
            headers: {
                "X-Requested-With": "XMLHttpRequest"
            }
        })
            .then(async resp => {
                const data = await resp.json().catch(() => ({}));

                if (!resp.ok || data.ok === false) {
                    throw new Error(data.message || "No se pudo guardar la sede.");
                }

                window.location.reload();
            })
            .catch(error => {
                if (mensajeAlertaModal) {
                    mensajeAlertaModal.textContent = error.message || "No se pudo guardar la sede.";
                }

                if (alertaModal) {
                    alertaModal.classList.remove("d-none");
                }
            })
            .finally(() => {
                if (botonGuardar) {
                    botonGuardar.disabled = false;
                    botonGuardar.innerHTML = textoBoton;
                }
            });
    });

    archivoFoto?.addEventListener("change", function () {
        const file = this.files[0];

        if (!file) return;

        previewFoto.src = URL.createObjectURL(file);
        previewFoto.classList.remove("d-none");
        previewIcono.classList.add("d-none");
        btnEliminarFoto.classList.remove("d-none");
        eliminarFoto.value = "false";
    });

    btnEliminarFoto?.addEventListener("click", function () {
        archivoFoto.value = "";
        previewFoto.src = "";
        previewFoto.classList.add("d-none");
        previewIcono.classList.remove("d-none");
        btnEliminarFoto.classList.add("d-none");
        eliminarFoto.value = "true";
    });

    function filtrarSedes() {
        const texto = (buscar?.value || "").trim().toLowerCase();
        const ciudadValor = ciudad?.value || "";
        const tipoValor = tipo?.value || "";
        const estadoValor = estado?.value || "";

        filas.forEach(fila => {
            const data = (fila.dataset.busqueda || "").toLowerCase();
            const dataCiudad = fila.dataset.ciudad || "";
            const dataTipo = fila.dataset.tipo || "";
            const dataEstado = fila.dataset.estado || "";

            const coincideTexto = texto === "" || data.includes(texto);
            const coincideCiudad = ciudadValor === "" || dataCiudad === ciudadValor;
            const coincideTipo = tipoValor === "" || dataTipo === tipoValor;
            const coincideEstado = estadoValor === "" || dataEstado === estadoValor;

            fila.classList.toggle(
                "d-none",
                !(coincideTexto && coincideCiudad && coincideTipo && coincideEstado)
            );
        });
    }

    buscar?.addEventListener("input", filtrarSedes);
    ciudad?.addEventListener("change", filtrarSedes);
    tipo?.addEventListener("change", filtrarSedes);
    estado?.addEventListener("change", filtrarSedes);

    limpiar?.addEventListener("click", function () {
        if (buscar) buscar.value = "";
        if (ciudad) ciudad.value = "";
        if (tipo) tipo.value = "";
        if (estado) estado.value = "";

        filtrarSedes();
    });

    setTimeout(() => {
        document.querySelectorAll(".alert").forEach(alerta => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alerta);
            bsAlert.close();
        });
    }, 4000);

    const infoSedes = document.getElementById("infoSedes");
    const iconoInfo = document.getElementById("iconoInfoSedes");

    if (infoSedes && iconoInfo) {
        infoSedes.addEventListener("show.bs.collapse", () => {
            iconoInfo.classList.add("rotate-180");
        });

        infoSedes.addEventListener("hide.bs.collapse", () => {
            iconoInfo.classList.remove("rotate-180");
        });
    }

    formHorariosSede?.addEventListener("submit", function (e) {
        const horaInicio1 = document.getElementById("horaInicio1")?.value;
        const horaFin1 = document.getElementById("horaFin1")?.value;

        const horaInicio2 = document.getElementById("horaInicio2")?.value;
        const horaFin2 = document.getElementById("horaFin2")?.value;

        const capacidad = Number(document.getElementById("capacidadPorFranja")?.value || 0);
        const duracion = Number(document.getElementById("duracionFranjaMinutos")?.value || 0);

        if (alertaHorariosSede) {
            alertaHorariosSede.classList.add("d-none");
        }

        if (mensajeAlertaHorariosSede) {
            mensajeAlertaHorariosSede.textContent = "";
        }

        function mostrarErrorHorario(texto) {
            e.preventDefault();

            if (mensajeAlertaHorariosSede) {
                mensajeAlertaHorariosSede.textContent = texto;
            }

            if (alertaHorariosSede) {
                alertaHorariosSede.classList.remove("d-none");
            }
        }

        if (!horaInicio1 || !horaFin1) {
            mostrarErrorHorario("Debe registrar el primer bloque de atención.");
            return;
        }

      if (horaInicio1 >= horaFin1) {
          mostrarErrorHorario(
              "La hora de inicio del bloque 1 debe ser menor que la hora fin. " +
              "Ejemplo correcto: Mañana de 09:00 a 12:00."
          );
          return;
      }

        if ((horaInicio2 && !horaFin2) || (!horaInicio2 && horaFin2)) {
            mostrarErrorHorario("Si registra el bloque 2, debe completar inicio y fin.");
            return;
        }

       if (horaInicio2 && horaFin2) {
           if (horaInicio2 >= horaFin2) {
               mostrarErrorHorario(
                   "La hora de inicio del bloque 2 debe ser menor que la hora fin. " +
                   "Ejemplo correcto: Tarde de 13:00 a 18:00."
               );
               return;
           }

           if (horaInicio2 < horaFin1) {
               mostrarErrorHorario(
                   "El bloque 2 no puede iniciar antes de que termine el bloque 1. " +
                   "Recuerda usar formato de 24 horas: 13:00 significa 1:00 p.m. " +
                   "Ejemplo correcto: Mañana 09:00 a 12:00 y Tarde 13:00 a 18:00."
               );
               return;
           }
       }

        if (duracion <= 0) {
            mostrarErrorHorario("Debe seleccionar una franja válida.");
            return;
        }

        if (capacidad <= 0) {
            mostrarErrorHorario("La capacidad por franja debe ser mayor a cero.");
            return;
        }
    });
});


async function verDetalleSede(id) {
    const resp = await fetch(`/interno/sedes/api/${id}`);

    if (!resp.ok) {
        alert("No se pudo cargar el detalle.");
        return;
    }

    const s = await resp.json();
    const detalleFoto = document.getElementById("detalleFoto");
    const detalleIcono = document.getElementById("detalleIcono");

if (detalleFoto && detalleIcono && s.foto) {
        detalleFoto.src = `/uploads/sedes/${s.foto}`;
        detalleFoto.classList.remove("d-none");
        detalleIcono.classList.add("d-none");
    } else if (detalleFoto && detalleIcono) {
        detalleFoto.classList.add("d-none");
        detalleIcono.classList.remove("d-none");
    }

    document.getElementById("detalleTitulo").textContent = s.nombre || "Detalle de sede";
    document.getElementById("detalleSubtitulo").textContent = s.codigo || "Información completa registrada";
    document.getElementById("detalleNombre").textContent = s.nombre || "-";
    document.getElementById("detalleCodigo").textContent = s.codigo || "-";
    document.getElementById("detalleTipo").textContent = s.tipo === "ALMACEN" ? "Almacén" : "Tienda";
    document.getElementById("detalleCiudad").textContent = s.ciudad || "-";
    document.getElementById("detalleDireccion").textContent = s.direccion || "-";
    document.getElementById("detalleResponsable").textContent = s.responsable || "-";
    document.getElementById("detalleTelefono").textContent = s.telefono || "-";
    document.getElementById("detalleDescripcion").textContent = s.descripcion || "Sin descripción.";
    const detalleGoogleMapsUrl = document.getElementById("detalleGoogleMapsUrl");
    const detalleGoogleMapsUrlVacio = document.getElementById("detalleGoogleMapsUrlVacio");

    if (detalleGoogleMapsUrl && detalleGoogleMapsUrlVacio) {
        if (s.googleMapsUrl) {
            detalleGoogleMapsUrl.href = s.googleMapsUrl;
            detalleGoogleMapsUrl.classList.remove("d-none");
            detalleGoogleMapsUrlVacio.classList.add("d-none");
        } else {
            detalleGoogleMapsUrl.href = "#";
            detalleGoogleMapsUrl.classList.add("d-none");
            detalleGoogleMapsUrlVacio.classList.remove("d-none");
        }
    }
    document.getElementById("detalleFechaRegistro").textContent = s.fechaRegistro ? new Date(s.fechaRegistro).toLocaleString("es-PE")
        : "-";
    const estado = document.getElementById("detalleEstado");
    estado.textContent = s.activo ? "Activo" : "Inactivo";
    estado.className = s.activo
        ? "badge rounded-pill bg-success-subtle text-success"
        : "badge rounded-pill bg-secondary-subtle text-secondary";

   const icono = document.getElementById("detalleIcono");

   if (icono) {
       icono.className = s.tipo === "ALMACEN"
           ? "mx-auto mb-3 rounded-circle align-items-center justify-content-center bg-primary-subtle text-primary"
           : "mx-auto mb-3 rounded-circle align-items-center justify-content-center bg-success-subtle text-success";

       icono.innerHTML = s.tipo === "ALMACEN"
           ? '<i class="bi bi-building"></i>'
           : '<i class="bi bi-shop"></i>';

       if (s.foto) {
           icono.classList.add("d-none");
           icono.classList.remove("d-flex");
       } else {
           icono.classList.remove("d-none");
           icono.classList.add("d-flex");
       }
   }
}

async function abrirHorariosSede(idSede, nombreSede) {
    const inputIdSede = document.getElementById("horarioIdSede");
    const titulo = document.getElementById("tituloModalHorariosSede");
    const subtitulo = document.getElementById("subtituloModalHorariosSede");

    const descripcionPublica = document.getElementById("descripcionPublicaHorario");

    const nombreBloque1 = document.getElementById("nombreBloque1");
    const horaInicio1 = document.getElementById("horaInicio1");
    const horaFin1 = document.getElementById("horaFin1");

    const nombreBloque2 = document.getElementById("nombreBloque2");
    const horaInicio2 = document.getElementById("horaInicio2");
    const horaFin2 = document.getElementById("horaFin2");

    const duracionFranja = document.getElementById("duracionFranjaMinutos");
    const capacidad = document.getElementById("capacidadPorFranja");
    const activo = document.getElementById("horarioActivo");

    const alerta = document.getElementById("alertaHorariosSede");
    const mensaje = document.getElementById("mensajeAlertaHorariosSede");

    if (alerta) {
        alerta.classList.add("d-none");
    }

    if (mensaje) {
        mensaje.textContent = "";
    }

    if (inputIdSede) {
        inputIdSede.value = idSede;
    }

    if (titulo) {
        titulo.textContent = "Configurar horarios";
    }

    if (subtitulo) {
        subtitulo.textContent = `Sede: ${nombreSede || "Sede seleccionada"}`;
    }

    // Valores por defecto
    if (descripcionPublica) {
        descripcionPublica.value = "";
    }

    if (nombreBloque1) nombreBloque1.value = "Bloque 1";
    if (horaInicio1) horaInicio1.value = "10:00";
    if (horaFin1) horaFin1.value = "15:00";

    if (nombreBloque2) nombreBloque2.value = "Bloque 2";
    if (horaInicio2) horaInicio2.value = "16:00";
    if (horaFin2) horaFin2.value = "18:00";

    if (duracionFranja) duracionFranja.value = "20";
    if (capacidad) capacidad.value = "3";
    if (activo) activo.value = "true";

    try {
        const resp = await fetch(`/interno/sedes/horarios/${idSede}`);

        if (!resp.ok) {
            return;
        }

        const data = await resp.json();

        if (descripcionPublica) {
            descripcionPublica.value = data.descripcionPublica || "";
        }

        if (data.bloques && data.bloques.length > 0) {
            const b1 = data.bloques[0];

            if (nombreBloque1) nombreBloque1.value = b1.nombreBloque || "Bloque 1";
            if (horaInicio1) horaInicio1.value = cortarHora(b1.horaInicio) || "10:00";
            if (horaFin1) horaFin1.value = cortarHora(b1.horaFin) || "15:00";

            if (duracionFranja) duracionFranja.value = String(b1.duracionFranjaMinutos || 20);
            if (capacidad) capacidad.value = b1.capacidadPorFranja || 3;
            if (activo) activo.value = b1.activo === false ? "false" : "true";
        }

        if (data.bloques && data.bloques.length > 1) {
            const b2 = data.bloques[1];

            if (nombreBloque2) nombreBloque2.value = b2.nombreBloque || "Bloque 2";
            if (horaInicio2) horaInicio2.value = cortarHora(b2.horaInicio) || "16:00";
            if (horaFin2) horaFin2.value = cortarHora(b2.horaFin) || "18:00";
        }

    } catch (error) {
        console.error("Error cargando horarios de sede:", error);
    }
}

function cortarHora(valor) {
    if (!valor) {
        return "";
    }

    // Si viene como 10:00:00, lo deja como 10:00
    return valor.substring(0, 5);
}
