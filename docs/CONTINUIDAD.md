# DentIA — punto de reanudación

## Publicación confirmada en servidor — 24/10/2026, 18:38 Lima

Actualización posterior: publicado `target/approved-release/dentia-1.0.0.jar` en el servidor real `brayan-server` (100.90.117.108), acceso https://dentia.dodsix.com. Resultado PASS con respaldo restaurado en base temporal, credenciales conservadas y Suma/MySQL/Cloudflare sin reiniciar. Se corrigió el blanco del acceso de cuentas inexistentes mediante la versión actual; añadidos filtros opcionales Mis pacientes/Mis atenciones y navegación inferior móvil. 236 pruebas Java y 19 MySQL aisladas aprobadas; sin navegador. Detalles y evidencia: docs/SERVIDOR.md. La referencia anterior a despliegue exclusivamente local queda superada por esta publicación.

## Actualización del 24/10/2026 — flujo clínico y despliegue en PC

- Tarjetas de inicio enlazadas, próximas citas y filtro de pacientes atendidos hoy incluyendo atención directa.
- Permisos independientes de vistas 2D/3D, renderizado condicionado y protección de configuración 3D. ADMIN conserva su acceso completo.
- Marcas 2D con guardado inmediato y reintento; cotizador conectado al catálogo real y conservación de identificadores al añadir ítems.
- Atención directa con importe acordado opcional: cargo pendiente en la misma transacción y cierre idempotente. Los procedimientos de plan no duplican cargos; su avance actualiza ejecución/completado.
- Reintentos de cobro conservan su identificador; enlace a constancia interna imprimible. No hay emisión tributaria.
- Reportes con registros reales, filtros, CSV y gráficos; eliminados valores ficticios y corregida presentación móvil.
- El inicializador deja de restablecer la contraseña del administrador y de ejecutar la migración heredada que borraba/sobrescribía servicios personalizados. Los seeds conservan ediciones del consultorio.
- Verificación: 235 pruebas Java, 18 MySQL aisladas y 7 JavaScript aprobadas. Revisión en navegador a 390 × 844. Guía: docs/GUIA-ATENCION.md.
- El usuario identificó su PC como servidor. Despliegue local en 8080: target/final-release/dentia-1.0.0.jar. No se identificó ni modificó un servidor remoto.

## Objetivo autorizado

Consolidar el proyecto DentIA, con estructura mantenible, flujo clínico funcional y modelo dental 3D articulado integrado. El usuario pidió continuar por etapas antes de agotar el cupo Plus y retomar después de su reinicio. Automatización cada cinco horas en esta tarea. No consumir reinicios de cupo ni comprar créditos.

## Cómo continuar

1. Consultar los límites con la herramienta de uso. No abrir una etapa nueva si queda menos de 20% en cinco horas o 10% semanal.
2. Leer este archivo y verificar el estado real del repositorio. Hay cientos de cambios previos del usuario: no hacer reset, checkout ni limpieza masiva.
3. Cerrar una etapa implementada y probada. Actualizar este archivo antes de ampliar el trabajo.
4. El usuario está trabajando otro proyecto en el puerto 8080. No iniciar servidores locales ni usar, cerrar o reclamar ese puerto hasta que lo solicite expresamente.

## Vigésima sexta etapa cerrada — Rediseño Ergonómico Móvil del Odontograma 2D, Eliminación de Cards Redundantes y Drawer Táctil, 15 de octubre de 2026

- **Eliminación Total de Cards y Botones de Dentición Redundantes**:
  - Se eliminó el bloque `.odonto-dentition-switch` ("Vista unificada", "Dientes permanentes", "Dientes de leche") que ocupaba espacio excesivo. La vista 2D por definición presenta la dentición completa anatómica integrada (52 piezas FDI) de forma unificada y limpia.
- **Reducción Drástica de Textos y Cartas Duplicadas**:
  - Se suprimieron los párrafos explicativos largos y títulos reiterativos (`.odonto-2d-intro`, `.odonto-sheet-heading`, encabezados de herramientas).
  - Se compactaron los badges y etiquetas de cuadrantes en micro-píldoras minimalistas (`Adultos 18–28`, `Niños 55–65`, `Línea Oclusal`, `Niños 85–75`, `Adultos 48–38`), reduciendo significativamente la altura vertical.
  - La barra de herramientas se condensó en un listón desplazable táctil con botones compactos e iconos directos.
- **Optimización y Ergonomía Móvil (Celulares y Tablets)**:
  - **Dientes compactos**: En pantallas `<= 991px`, el ancho de cada diente se redujo de 60px a 35px, ajustando proporcionalmente las siluetas SVG y cajas de marcado. El ancho total de arcada pasó de 1040px a 590px, reduciendo a más de la mitad el desplazamiento horizontal.
  - **Filtro Rápido de Arcadas para Móvil**: Se incorporaron tres micro-tabs táctiles (`🦷 Todo (52)`, `⬆️ Maxilar Sup`, `⬇️ Mandíbula Inf`) para permitir a la doctora centrarse en la arcada de interés en pantallas reducidas con 1 solo toque.
  - **Panel Drawer / Bottom-Sheet para la Pieza Seleccionada**: En celular, `#odonto-sidebar-panel` pasa de estar oculto bajo 4 filas de dientes a convertirse en un bottom-sheet deslizable interactivo que se eleva al tocar cualquier diente. Cuenta con botón de cierre 'X' superior (cumpliendo con la regla global), backdrop táctil para dismiss y acceso directo a Atender Pieza y Cotizar en Plan.
- **Verificación**: Suite de pruebas unitarias y de integración aprobada al 100% (**229/229 pruebas aprobadas**, 0 fallos, 0 errores, compilación y sintaxis `node --check` limpias). Servidor Spring Boot activo en puerto 8081.

## Vigésima quinta etapa cerrada — Sistema Universal de Modales, Validaciones In Situ, Cierre Exclusivo por 'X' y Mayúsculas Obligatorias, 30 de septiembre de 2026

- **Restricción y Estandarización de Cierre de Modales**:
  - Se configuró a nivel global en todos los modales del sistema (vía HTML y mediante `bootstrap.Modal.Default`) el comportamiento `backdrop: 'static'` y `keyboard: false`.
  - Los modales ya **no se cierran al hacer clic fuera de ellos ni presionando Escape**.
  - **Cierre exclusivo con botón 'X' superior**: Se eliminaron y deshabilitaron los botones secundarios "Cancelar" o "Cerrar" en los pies (`modal-footer`) de los formularios modales en todos los módulos (Pacientes, Profesionales, Usuarios, Roles, Servicios y Precios, Tipos de Atención, Especialidades, Sedes, Proveedores y Clínica). Cada modal cuenta únicamente con la 'X' superior como control de cierre y el botón de acción principal (Guardar / Confirmar).
- **Validaciones Rigurosas e In Situ en Casilleros**:
  - Restricción de entrada de caracteres no permitidos en tiempo real (`keydown` e `input`):
    - DNI: Solo números y exactamente 8 dígitos.
    - RUC: Solo números y exactamente 11 dígitos.
    - Teléfono / Celular: Solo números y 9 dígitos (iniciando con 9).
    - Nombres y Apellidos: Restringido exclusivamente a letras del alfabeto, tildes y espacios; no admite números ni caracteres especiales extraños.
    - Fechas de nacimiento: Delimitadas entre `1900-01-01` y la fecha actual (`today`), impidiendo años futuros y fechas absurdas (como `275760`).
  - **Reporte In Situ**: Al cometerse un error o intentar enviar datos inválidos, la alerta se presenta **ahí mismo en el casillero correspondiente dentro del modal** con resaltado perimetral rojo (`.is-invalid`) y un mensaje contextual `.invalid-feedback` explicativo inmediatamente debajo del input, enfocando automáticamente el campo erróneo y manteniendo el modal abierto.
- **Nombres, Apellidos y Razones Sociales en Mayúsculas Obligatorias**:
  - En la vista y durante la digitación, se muestran inmediatamente en mayúsculas gracias a reglas de estilo `text-transform: uppercase` y transformación en tiempo real con `toUpperCase()`.
  - En el guardado frontend y en la persistencia backend en controladores Java (`PacienteInternoController`, `UsuarioController`, `ProfesionalAgendaController`, `RolController`, `CatalogoAgendaController`), los nombres, apellidos y denominaciones son convertidos y persistidos permanentemente en mayúsculas.
- **Motor Centralizado (`modales-sistema.js`)**:
  - Desarrollado e integrado en `header.html` para monitorear y aplicar dinámicamente estas reglas en todos los modales existentes y los que se renderizan vía AJAX o asíncronamente en el DOM.
- **Verificación**: Compilación exitosa y suite completa de pruebas unitarias y de integración aprobada al 100% (**229/229 pruebas aprobadas**, 0 fallos, 0 errores). Servidor Spring Boot activo.

## Vigésima cuarta etapa cerrada — Estado dental y visualización de última evolución en la Ficha del Paciente, 30 de septiembre de 2026

- **Integración del estado actual de los dientes en la Ficha del Paciente (`ficha.html`)**:
  - En la pestaña principal **"Resumen General"** se incorporó la sección clínica **"Estado Dental Actual (Cómo quedaron sus dientes)"**:
    1. **Resumen de la última sesión clínica**: Detalla la fecha de la última atención, el doctor tratante, las piezas dentales trabajadas en esa sesión (con badges distintivos como `Pieza 46 (Curación/Resina)`), el procedimiento y las notas médicas de evolución.
    2. **Mini Odontograma Visual FDI (32 piezas)**: Mapa dental gráfico interactivo con los cuadrantes superiores (18..11 | 21..28) e inferiores (48..41 | 31..38). Muestra las superficies anatómicas de cada diente coloreadas según su estado real: azul (curado/obturación), rojo (caries activa/por tratar), verde (sellante), amarillo/naranja (endodoncia/corona) y tachado gris (pieza ausente/extraída).
    3. **Inspección interactiva al pasar el cursor o hacer clic**: Muestra instantáneamente el número, nombre anatómico y el diagnóstico o tratamiento de la pieza seleccionada.
  - **Nueva pestaña dedicada "Estado Dental (Odontograma)"**:
    1. Añadida a la barra de navegación de la ficha (`.ficha-nav`), permitiendo alternar sin recargar la página.
    2. Muestra KPIs odontológicos (Dientes Curados/Tratados, Caries por Tratar, Piezas Ausentes y Dientes Sanos).
    3. Tabla detallada de piezas dentales con hallazgos (FDI, nombre, cara tratada, condición y estado).
    4. Tabla cronológica de evolución dental histórica con fechas, usuarios y notas.
    5. Botones de acceso directo para abrir el **Sillón Clínico 360°** y atender al paciente.
- **Verificación**: Suite de pruebas de templates ejecutada satisfactoriamente (19/19 pruebas aprobadas, 0 fallos, 0 errores), compilación limpia y servidor en ejecución.

## Vigésima tercera etapa cerrada — Eliminación de vinculación Baileys por riesgo de baneo y adaptación a WhatsApp Oficial Editable y 100% Legal, 30 de septiembre de 2026

- **Eliminación definitiva de vinculación con QR no oficial (Baileys)**:
  - Ante las políticas estrictas de Meta/WhatsApp que detectan y sancionan con baneo de cuenta el uso de WebSockets de emulación no autorizada (Baileys), se desactivó y purgó la vinculación por código QR y el subproceso de socket local en segundo plano.
  - Se eliminó el modal `#modalVincularWhatsApp` de `header.html` y el botón de estado / polling constante.
  - Se detuvo y limpió el proceso Node legado (`server.js`) y las sesiones de autenticación.
- **Integración 100% Legal, Oficial y Segura (`whatsapp-smart-sender.js`, `WhatsAppServiceImpl.java`)**:
  - Se adoptó el esquema oficial de Meta (`https://api.whatsapp.com/send?phone=...&text=...`), libre de cualquier riesgo de bloqueo y sin depender de servicios externos o librerías de terceros.
  - **Botón directo en la barra superior (Topbar)**: Un botón verde con icono oficial de WhatsApp da acceso al modal de comunicación con pacientes.
  - **Modal de Comunicación Totalmente Modificable**:
    - Permite buscar cualquier paciente en vivo o escribir/editar directamente el número de teléfono con formato automático (+51).
    - Selector de plantillas odontológicas clínicas (Recordatorio de cita, Reprogramación por inasistencia, Cuidados post-operatorios, Presupuesto disponible, Profilaxis semestral o Mensaje libre).
    - Campo de texto amplio y editable para que el odontólogo o recepcionista pueda personalizar cualquier nota, horario o indicación antes de enviar.
    - Botón prominente *"Abrir en WhatsApp"* que abre directamente la conversación con el mensaje listo para enviar o para revisión final en WhatsApp Web o la App oficial.
  - **Botones de acción rápida en Agenda y Notificaciones**:
    - Las acciones de *"Recordar"* y *"Reagendar"* en la Agenda y en la campana de notificaciones abren el chat oficial de WhatsApp con el número limpio y el mensaje odontológico pre-cargado.
- **Verificación**: Compilación limpia, 229 pruebas aprobadas (0 fallos, 0 errores) y servidor Spring Boot corriendo en modo dev local.

## Vigésima segunda etapa cerrada — Inasistencia automática por caducidad de horario en Agenda, 30 de septiembre de 2026

- **Transición automática a `NO_ASISTIO` al pasar el horario de la cita**:
  - En `ICitaAgendaDao` y `JdbcCitaAgendaDao` se implementó `marcarCitasVencidasComoNoAsistio`: marca automáticamente como `NO_ASISTIO` toda cita que continúe en estado `RESERVADA` o `CONFIRMADA` una vez que su hora de fin (`hora_fin`) haya transcurrido.
  - Se incorporó la regla en `CitaAgendaService.marcarCitasVencidas()` y se ejecuta automáticamente al consultar la agenda y al cambiar fechas.
  - **Planificador en segundo plano (`CitaAgendaScheduler.java`)**: Ejecución programada cada 60 segundos (`@Scheduled(fixedDelay = 60000)`) que garantiza la auto-expiración continua de citas pasadas en tiempo real.
  - **Filtro visual de inasistencias en la Agenda (`agenda.html`)**: Se añadió el estado `NO_ASISTIO` en el selector de filtros de la agenda junto a su badge representativo y acción directa de contactar al paciente para reprogramar su turno.
  - **Pruebas**: Se creó `CitaVencidaInasistenciaTest` verificando la transición atómica y se verificó en base de datos MySQL.

## Vigésima primera etapa cerrada — Consultas y Atenciones Clínicas directas sin cita obligatoria, 29 de septiembre de 2026

- **Desacoplamiento de citas obligatorias para la atención clínica (`clinica.js`, `index.html`)**:
  - Se eliminó la restricción que impedía atender a un paciente si no contaba previamente con una cita agendada en la agenda. El odontólogo ahora puede seleccionar a cualquier paciente y registrar su consulta clínica completa de forma inmediata.
  - **Banner del paciente en Sillón Clínico (`clinica.js`)**: Cuando el paciente seleccionado no tiene cita activa, el banner muestra el distintivo *"Consulta directa"* y el botón prominente *"Registrar Atención"* (`#btn-hero-atencion-directa`), abriendo el formulario SOEP con un solo clic. Si la cita ya fue atendida, ofrece además el botón *"Nueva Atención"*.
  - **Pestaña de Atenciones Realizadas (`index.html`)**: Se habilitó el botón superior `+ Nueva Atención / Consulta` (`#btn-nueva-atencion`).
  - **Modal de Atención SOEP (`modal-crear-atencion`)**: Se añadió el indicador informativo *"Consulta / Atención Directa (Sin Cita)"* y se convirtió la asignación del odontólogo en un selector interactivo (`#atencion-profesional-id`) para poder seleccionar o cambiar el doctor tratante cuando es una atención directa sin cita.
  - **Persistencia y auto-evolución**: Al finalizar la atención sin cita previa, los datos se guardan con `cita_id = null`, conservando el guardado transaccional de notas SOEP, receta médica, indicaciones, materiales y la auto-evolución en el odontograma.
- **Acceso ergonómico desde la Lista de Pacientes (`list.html`) y Agenda (`agenda.html`)**:
  - En la lista de pacientes (tanto en escritorio como en vista móvil), se añadió el botón de acción rápida **"Atender"** que abre el expediente clínico del paciente en el sillón listo para iniciar su consulta.
  - En la cabecera de la agenda se incluyó el botón **"Atención Directa (Sin Cita)"** para facilitar el flujo cuando llega un paciente sin turno agendado.
- **Historial e inspección en la Ficha del Paciente (`ficha.html`)**:
  - Se agregó el botón **"Atender Consulta"** en las acciones del encabezado.
  - En la pestaña de *Historial de Citas y Atenciones*, se integró la sección dinámica de *Consultas y Atenciones Directas Realizadas*, permitiendo consultar el detalle SOEP, piezas dentales tratadas y receta médica de las atenciones que no tuvieron cita previa.
- **Verificación**: Suite de pruebas ejecutada satisfactoriamente (**38/38 pruebas aprobadas**, 0 fallos, 0 errores en `PacienteListTemplateTest`, `ClinicaTemplateTest`, `AtencionClinicaServiceTest` y `ClinicaAtencionApiTest`; validación sintáctica `node --check` con 0 errores). Sin ocupar puerto 8080 ni alterar datos reales.

## Vigésima etapa cerrada — Odontograma 2D unificado (Adulto y Niño en vista única), 28 de septiembre de 2026

- **Unificación anatómica del Odontograma 2D en una sola vista (`index.html`, `clinica.js`, `clinica.css`)**:
  - Se eliminó la división rígida en paneles separados para dentición adulta e infantil. Ahora ambas denticiones conviven de forma integrada en una sola vista continua conforme al estándar universal FDI y la solicitud de la odontóloga:
    1. **Fila 1 (Maxilar Superior - Permanentes)**: Cuadrantes 1 y 2 (piezas 18 a 28).
    2. **Fila 2 (Maxilar Superior - De leche/Niños)**: Cuadrantes 5 y 6 (piezas 55 a 65), centradas y alineadas verticalmente con los incisivos, caninos y premolares superiores.
    3. **Línea media / Oclusión**: Línea divisoria continua representativa del plano oclusal y centro de la boca.
    4. **Fila 3 (Mandíbula Inferior - De leche/Niños)**: Cuadrantes 8 y 7 (piezas 85 a 75), con las caras oclusales/incisales orientadas anatómicamente hacia la línea media de oclusión.
    5. **Fila 4 (Mandíbula Inferior - Permanentes)**: Cuadrantes 4 y 3 (piezas 48 a 38).
  - **Scrollboard unificado (`.odonto-unified-board`)**: Los 8 cuadrantes están contenidos en un único contenedor de desplazamiento horizontal sincronizado, evitando desalineaciones entre arcadas en pantallas estrechas o tabletas.
  - **Alineación exacta de línea media dental**: Cuadrantes izquierdo y derecho alinean incisivos centrales (11, 21, 51, 61, 81, 71, 41, 31) simétricamente sobre el eje vertical central.
  - **Diferenciación estética y reactiva**: Dientes temporales destacados con numeración y bordes suaves en verde azulado (`#0b7f86`), conservando interacción táctil, marcado de patologías y sincronización con el panel lateral de pieza seleccionada.
  - **Selector de modo visual**: Por defecto la vista inicia en **"Vista unificada (Adulto y Niño)"** mostrando todos los cuadrantes; incluye botones para aislar rápidamente solo adultos o solo niños si la doctora lo requiere.
- **Verificación**: Suite de pruebas ejecutadas satisfactoriamente (`ClinicaTemplateTest`, `OdontogramaServiceTest`, `AtencionClinicaServiceTest` 36/36 pruebas aprobadas, `node --check` 0 errores). Sin ocupar puerto 8080 ni alterar datos reales.

## Decimonovena etapa cerrada — Evolución transaccional del odontograma y unificación ergonómica sin scroll anidado, 26 de septiembre de 2026

- **Evolución atómica y transaccional en backend (`AtencionClinicaService`)**:
  - Al registrar y finalizar una atención clínica (`registrarAtencion`), el servidor ejecuta de forma atómica la auto-evolución en el odontograma del paciente dentro de la misma transacción de base de datos.
  - Las piezas tratadas con procedimientos de curación/resina evolucionan las superficies con `CARIES` (rojo) a `OBTURACION` (azul, curado) con estado `REALIZADO`. Se admiten además endodoncias, coronas, sellantes, extracciones (ausente), implantes y brackets.
  - Se registra el evento en `clinica_odontograma_historial` y se incrementa la versión del odontograma. Se añadió prueba unitaria que verifica este flujo completo.
- **Limpieza de borrador local de marcas en frontend (`clinica.js`)**:
  - Al completar la atención, se purga del mapa en memoria `state.odontoPending` cualquier marca pendiente de las piezas tratadas, garantizando que el repintado no sobreescriba los hallazgos ya curados.
- **Eliminación del scroll anidado (doble barra de desplazamiento)**:
  - En `clinica.css`, se removió `max-height: calc(100vh - 100px);` y `overflow-y: auto;` de `#odonto-sidebar-panel`, eliminando por completo la barra de desplazamiento interior duplicada.
- **Diseño compacto del panel lateral y eliminación de listas redundantes**:
  - Se retiraron del panel lateral las listas repetidas de atenciones anteriores y procedimientos del plan (las cuales ya tienen su espacio oficial en las pestañas superiores `[Plan y Tratamientos]` y `[Atenciones Realizadas]`).
  - El panel lateral ahora se concentra exclusivamente en la pieza seleccionada:
    1. Header con número de pieza, nombre anatómico y cuadrante.
    2. Estado odontológico actual (superficies y patologías).
    3. Botón de acción directa prominente: **"Atender Pieza"** (abre el formulario SOAP con la pieza cargada en 1 clic).
    4. Cotizador rápido de tratamiento en plan.
    5. Tarjeta de resumen compacto de expediente con enlaces directos para navegar a las pestañas superiores principales (`btn-sidebar-ver-planes`, `btn-sidebar-ver-atenciones`).

## Decimoctava etapa cerrada — Odontograma Unificado y evolución clínica en pantalla única, 26 de septiembre de 2026

- **Odontograma 2D unificado con panel de control lateral (`#odonto-sidebar-panel`)**: Se integró el flujo de trabajo clínico en una sola pantalla sin necesidad de alternar entre pestañas. La columna izquierda presenta las arcadas FDI interactivas y la paleta de herramientas; la columna derecha presenta el panel persistente y reactivo de la pieza dental seleccionada.
- **Selección y foco inmediato de pieza**: Al hacer clic en cualquier diente o marcar una cara, la pieza se resalta con clase `.selected` en el odontograma y el panel lateral muestra en tiempo real su anatomía, cuadrante, patologías y marcas registradas.
- **Cotizador Rápido en Plan de Tratamiento**: Permite cotizar directamente el procedimiento para la pieza seleccionada (con precios sugeridos para resinas, endodoncias, coronas, implantes, etc. o procedimiento personalizado). El ítem entra al plan del paciente en estado `PROPUESTO` (cotización sin generar deuda preventiva).
- **Regla de Aprobación de Plan y Habilitación de Atención**: En estado `BORRADOR` / `PROPUESTO`, el botón "Atender" permanece bloqueado preventivamente con un aviso explícito, ofreciendo el botón de un clic "Aprobar Plan y Generar Deuda". Una vez aprobado, el botón "Atender Tratamiento" se activa inmediatamente.
- **Auto-evolución dental al finalizar atención (`guardarAtencion(true)`)**: Al registrar la atención médica, el sistema evoluciona automáticamente los hallazgos del odontograma: las superficies con Caries (rojo) evolucionan a Obturación/Resina (azul); piezas ausentes evolucionan a Implante al realizarse implante; se asigna Corona, Endodoncia o Brackets según el procedimiento clínico realizado; el ítem del plan pasa a `REALIZADO` y tanto el odontograma como el panel lateral se refrescan al instante.
- **Verificación**: Compilación y suite completa de pruebas unitarias ejecutadas con éxito (**221/221 pruebas aprobadas**, 0 fallos, 0 errores, 0 omisiones).

## Decimoséptima etapa cerrada — evaluación completa en SOEP, 23 de septiembre de 2026

- El formulario denominado SOEP omitía la **E de Evaluación**, aunque el DTO, servicio y tabla ya soportaban `evaluacion`. Se añadió el campo “E (Evaluación) — Valoración clínica” entre Objetivo y Procedimiento.
- El campo queda integrado automáticamente en la lectura genérica del formulario y recuperación de borradores, con `maxlength="1500"` alineado a la columna y a la validación del servidor. No incluye contenido clínico precargado.
- La prueba de renderizado confirma el campo, su etiqueta y límite. **28/28 pruebas aprobadas**, sin fallos, errores ni omisiones: `ClinicaTemplateTest` y `AtencionClinicaServiceTest`, en `target/dentia-soep-evaluation-check`; `git diff --check` pasó.
- No se inició servidor, no se usó el puerto 8080 ni se conectó o modificó la base real. Pendiente: recorrer captura, borrador, recuperación y cierre SOEP completo en el entorno integral aislado.

## Decimosexta etapa cerrada — formulario clínico sin datos ficticios, 22 de septiembre de 2026

- Retirados del formulario de atención los valores precargados `26` en piezas tratadas y `Resina Filtek Z250 A2` en materiales. Eran ejemplos que podían terminar registrados como hechos clínicos sin haber sido confirmados por el profesional.
- Los campos inician vacíos y conservan orientación mediante `placeholder`, que no se incluye en el envío. La acción explícita desde el inspector 3D sigue completando la pieza seleccionada cuando corresponde.
- La prueba de renderizado comprueba que ambos campos mantienen sus límites y no contienen atributo `value`. **13/13 pruebas aprobadas**, sin fallos, errores ni omisiones, en `target/dentia-clinical-empty-defaults-check`; `git diff --check` pasó.
- No se inició servidor, no se usó el puerto 8080 ni se conectó o modificó la base real. Pendiente: confirmar visualmente el formulario y el traspaso explícito desde el visor 3D en el entorno integral aislado.

## Decimoquinta etapa cerrada — límites visibles en atención clínica, 22 de septiembre de 2026

- El formulario SOEP ahora refleja los límites ya protegidos por el servidor: piezas tratadas (100), subjetivo y objetivo (1500), procedimiento e indicaciones (2000) y materiales (500 caracteres). El procedimiento está marcado como obligatorio y muestra su límite junto al campo.
- La validación del navegador evita envíos previsiblemente inválidos; la validación backend de las etapas anteriores continúa siendo la autoridad y protege solicitudes directas o clientes antiguos.
- La prueba de renderizado verifica cada `maxlength` y el atributo `required`. **33/33 pruebas aprobadas**, sin fallos, errores ni omisiones: `ClinicaTemplateTest`, `AtencionClinicaServiceTest` y `ClinicaAtencionApiTest`, en `target/dentia-clinical-form-limits-check`; `git diff --check` pasó.
- No se inició servidor, no se usó el puerto 8080 ni se conectó o modificó la base real. Sigue pendiente el recorrido integral del formulario con una base temporal aislada.

## Decimocuarta etapa cerrada — contrato del límite clínico, 21 de septiembre de 2026

- Añadida regresión de frontera para el cierre de atenciones: un procedimiento de **exactamente 2000 caracteres** se envía completo al DAO, sin recorte ni rechazo. La prueba anterior ya cubre 2001 caracteres y ausencia de inserción.
- Añadida prueba HTTP del controlador clínico: cuando el servicio rechaza un campo demasiado largo, la API responde **400** con el mensaje que nombra el procedimiento y el límite. Así el formulario puede mostrar una corrección concreta al profesional.
- **20/20 pruebas aprobadas**, sin fallos ni errores: `AtencionClinicaServiceTest` y `ClinicaAtencionApiTest`, en `target/dentia-clinical-length-contract-check`. `git diff --check` pasó.
- No se inició servidor, no se usó el puerto 8080 ni se conectó o modificó la base real. Aún falta el recorrido completo del formulario en un entorno integral con base temporal aislada.

## Decimotercera etapa cerrada — no recortar datos de atenciones, 21 de septiembre de 2026

- Al finalizar una atención, los campos que superan la capacidad de las columnas clínicas ya no se truncan silenciosamente. `AtencionClinicaService` rechaza el cierre con HTTP 400 y un mensaje que identifica el campo y su límite, permitiendo corregir el texto antes de guardar. Conserva el recorte de espacios iniciales/finales, pero no elimina contenido clínico.
- Añadida prueba de procedimiento e indicaciones de 2001 caracteres; confirma que no se inserta atención ni recibo y no avanza la versión del borrador. Los límites corresponden al esquema `clinica_atenciones`.
- **18/18 pruebas aprobadas**, sin fallos ni errores: `AtencionClinicaServiceTest` y `ClinicaAtencionApiTest`. Compilación y ejecución aisladas en `target/dentia-clinical-length-check`; `git diff --check` de los archivos modificados pasó.
- No se inició servidor, no se usó el puerto 8080 ni se conectó o modificó la base real. Pendiente: validar el mensaje y la conservación del texto en el formulario real con una base integral temporal aislada.

## Duodécima etapa cerrada — integridad del modelo articulado, 21 de septiembre de 2026

- Reforzado `scripts/verify-dental-model.mjs`: ahora exige el conjunto exacto de 32 identificadores FDI permanentes, no solo 32 nombres con formato válido. Contrasta para cada diente el número FDI, la arcada, el número de raíces y la pertenencia de corona/raíces con el manifiesto fuente.
- Comprueba que la única pieza adicional sea `EXTRA_L_01` sin FDI, que existan las 33 coronas y los 33 grupos de raíces, y que el manifiesto servido al navegador sea idéntico al fuente y declare el tamaño real del GLB. Se conservan las comprobaciones previas de encías, animación mandibular y equivalencia binaria con el exportado de Blender.
- `node --check scripts/verify-dental-model.mjs` y `node scripts/verify-dental-model.mjs` aprobaron: **32 FDI, 1 adicional, 33 coronas, 33 raíces, 2 encías, animación mandibular; 3 127 852 bytes**. `git diff --check` del script pasó. No se modificaron los archivos `.blend`, `.glb` ni los manifiestos.
- Etapa de validación de activo, no de refinamiento visual. Sigue pendiente comprobar en navegador la experiencia 3D y móvil dentro de un entorno integral aislado. No se inició servidor, no se usó 8080 ni se tocó la base de datos real.

## Undécima etapa cerrada — recuperación segura de conflictos clínicos, 20 de septiembre de 2026

- Corregido el ciclo de edición de atenciones cuando el servidor responde HTTP 409: la sesión ya no conserva una versión obsoleta permitiendo reintentos que volverían a fallar ni sobrescribe silenciosamente el borrador de otro profesional.
- El formulario conserva lo escrito a la vista, bloquea nuevas escrituras y muestra una explicación. El nuevo botón **Recargar borrador actualizado** consulta el expediente vigente y reemplaza la edición local únicamente después de una acción explícita del usuario; si esa consulta falla, conserva la edición conflictiva bloqueada.
- Actualizados los identificadores de caché de `atencion-session.js` y `clinica.js` a `v4.2`, evitando que el navegador mezcle la plantilla nueva con scripts anteriores.
- **7/7 pruebas JavaScript aprobadas** y ambos scripts pasaron `node --check`. **13/13 pruebas Java de renderizado aprobadas**, incluyendo presencia inicial oculta del control de recuperación y las versiones de recursos; ejecución aislada en `target/dentia-conflict-recovery-check`.
- No se inició servidor, no se usó el puerto 8080 y no se conectó ni modificó ninguna base de datos. Sigue pendiente recorrer este conflicto con dos sesiones reales dentro del futuro entorno integral aislado.

## Décima etapa cerrada — disponibilidad OAuth centralizada, 19 de septiembre de 2026

- Centralizada en `GoogleOAuthAvailability` la validación del ID y secreto de Google. La portada y la ruta heredada de acceso ya no mantienen copias independientes de esa regla, por lo que un cambio futuro de configuración tendrá un único punto de mantenimiento.
- Los controladores reciben esta dependencia por constructor. Las pruebas dejaron de alterar campos privados mediante reflexión y ahora validan explícitamente configuraciones reales, vacías, nulas y el marcador `not-configured`, incluso con diferencias de mayúsculas.
- **19/19 pruebas aprobadas**, sin fallos, errores ni omisiones: `GoogleOAuthAvailabilityTest`, `PatientAuthControllerTest`, `PatientPortalControllerTest`, `PatientLandingTemplateTest`, `LoginTemplateContractTest` y `LoginFailureHandlerTest`. Compilación y ejecución aisladas en `target/dentia-oauth-availability-check`.
- No se inició ningún servidor, no se usó el puerto 8080 y no se conectó ni modificó ninguna base de datos. No se consumieron reinicios ni créditos de pago.
- Pendiente funcional sin cambios: configurar credenciales reales y comprobar el recorrido OAuth con Google. La siguiente etapa de implementación debe seguir priorizando el arranque integral con una base temporal aislada, sin ocupar 8080.

## Novena etapa cerrada — OAuth visible solo si está configurado, 18 de septiembre de 2026

- La portada ahora calcula si existen un ID y secreto reales de Google. Con valores vacíos o `not-configured`, oculta los enlaces que fallarían y muestra un aviso de reserva temporalmente deshabilitada; con ambas credenciales conserva el flujo OAuth. La ruta antigua `/paciente/login-google-demo` aplica la misma validación y vuelve a la portada con un aviso si el proveedor no está configurado.
- Añadidas pruebas del modelo de portada para los dos estados, un contrato de plantilla y renderizado Thymeleaf real para confirmar que no queda ningún enlace OAuth activo cuando faltan credenciales.
- **15/15 pruebas aprobadas**: `PatientAuthControllerTest` (6), `PatientPortalControllerTest` (3), `PatientLandingTemplateTest` (2) y `LoginTemplateContractTest` (4). Compilación y ejecución aisladas en `target/dentia-oauth-check`; no se inició ningún servidor ni se usó el puerto 8080.
- Última consulta de cupo: **87% restante en cinco horas y 25% semanal**. No se usaron reinicios ni créditos de pago.
- Pendiente: configurar credenciales Google reales y verificar el flujo con el proveedor. Mientras falten, la interfaz informa correctamente que la reserva autenticada no está disponible.

## Verificación local activa — 18 de septiembre de 2026, 01:28 Lima

- Por solicitud del usuario se liberó el puerto 8080 cerrando únicamente `MinimarketApplication` (PID 32728) y se inició DentIA desde este workspace con Maven.
- DentIA quedó activo en `http://localhost:8080/` y la portada respondió HTTP 200. Se abrió en el navegador y cargó la agenda pública con tipos de atención y profesionales provenientes de la base configurada en el perfil `dev`.
- A la 01:27 otra copia externa (PID 44992) había reemplazado a DentIA en 8080. Se cerró únicamente ese proceso conforme a la instrucción previa del usuario y se restauró esta copia de DentIA (PID 49896). La ruta del proceso y el título de la portada se verificaron después del arranque.
- Para este arranque se desactivaron el bootstrap del administrador, las inicializaciones automáticas de Clínica/Gestión y los procesos comerciales heredados. Esto evita escrituras automáticas al iniciar, pero no convierte la aplicación en solo lectura: no usar formularios de escritura como prueba aislada.
- Este arranque confirma servidor, renderizado y lecturas públicas; no sustituye el pendiente de una base integral aislada ni valida autenticación y escrituras del flujo clínico.
- Estado posterior: el usuario pidió terminar el servidor y reservar 8080 para otro proyecto. Al comprobarlo, el puerto ya estaba libre. La automatización fue actualizada para no volver a ocuparlo.

## Octava etapa cerrada — conservar catálogos al arrancar, 17 de septiembre de 2026

- Confirmado que `ClinicaSchemaConfig` reejecuta el SQL clínico en cada arranque y que sus seeds sobrescribían nombres/descripciones de diagnósticos y nombres/precios de tratamientos existentes.
- Ajustado el SQL para añadir solo códigos ausentes y conservar todos los campos de registros existentes. Añadidas dos pruebas MySQL de reinicialización repetida: preservación de ajustes, IDs, inactivos y códigos propios; reposición de códigos base ausentes sin duplicados.
- **16/16 pruebas MySQL aprobadas**, sin fallos, errores ni omisiones, a las 15:29 Lima. Incluye las 14 anteriores y las dos regresiones de catálogos; cada una repite la inicialización dos veces y comprueba los registros persistidos.
- Comando: `pwsh -NoProfile -File scripts/test-clinica-mysql.ps1`. Ejecución en `target/dentia-mysql-checks/6d9ba9cd6a714bd89cdb490f67fae150`, puerto 60754. Instancia detenida y ausencia de procesos temporales comprobada. Datos sintéticos e informes conservados; la base real no se modificó.
- Última consulta de cupo: **80% restante en cinco horas y 31% semanal**. Etapa cerrada sin ampliar alcance; no se usaron reinicios, no se reempaquetó el JAR ni se ejecutó la suite heredada completa.
- Próxima etapa: preparar el arranque integral en base aislada para recorrer los guardados desde el navegador. El cambio de catálogos está probado mediante el inicializador real, no mediante un reinicio del servidor web completo.

## Séptima etapa cerrada — presupuestos en MySQL, 17 de septiembre de 2026

- Ampliada `ClinicaMysqlIntegrationTest` para cargar la migración real de Gestión y el servicio clínico transaccional. Pacientes, profesionales y citas siguen siendo tablas padre sintéticas en la instancia temporal exclusiva.
- Primera ejecución: 13/14 pruebas MySQL aprobadas; concurrencia, reintento y rollback confirmados. El caso de costo previo negativo detectó que -50 más procedimientos por 150 producía un presupuesto de 100.
- Corregido el recálculo para iniciar desde cero cuando el costo previo no es positivo. La repetición aprobó **14/14 pruebas MySQL**, sin errores ni omisiones, a las 09:56 Lima. Incluye las ocho pruebas clínicas anteriores y seis casos de presupuestos: concurrencia/reintento, rollback de cabecera/detalles/auditoría/vínculo, recálculo desde cero/negativo y rechazo de precios cero/negativos. También se comprobó persistencia del actor `SISTEMA`.
- Comando: `pwsh -NoProfile -File scripts/test-clinica-mysql.ps1`. Ejecución aprobada en `target/dentia-mysql-checks/56f4b881b90b4087a9b54212ae90c457`, puerto 52702. El lanzador detuvo la instancia; conserva datos sintéticos e informes en `target/dentia-verification/surefire-reports`.
- Última lectura de cupo antes del cierre: **76% restante en cinco horas y 34% semanal**. No se usaron reinicios ni créditos de pago. No se reempaquetó el JAR ni se ejecutó la suite heredada completa.
- Pendiente: arranque integral aislado y recorrido de presupuestos/atenciones en navegador; estas pruebas ejecutan servicios transaccionales y migraciones reales, pero no el servidor web completo.

## Sexta etapa cerrada — presupuestos clínicos idempotentes, 16 de septiembre de 2026

- La generación desde un plan clínico ahora bloquea la fila del plan (`FOR UPDATE`) y reutiliza `presupuesto_id` si otra solicitud ya generó el presupuesto. Esto evita duplicados ante doble clic o solicitudes concurrentes.
- Se validan todos los precios de los procedimientos antes de insertar la cabecera. Los valores nulos, cero o negativos ya no se convierten silenciosamente en `1.00`; se rechazan con un mensaje claro. También se rechaza un plan sin costo positivo.
- La auditoría usa `SISTEMA` cuando no hay actor autenticado, manteniendo el registro completo de la operación.
- Añadida `ClinicaPresupuestoServiceTest`: **5/5 pruebas aprobadas** (reutilización, generación y vínculo, suma de items, precios inválidos y plan sin costo). Ejecución aislada con `-Ddentia.build.directory=target/dentia-budget-check`; no se tocó ninguna base real.
- MySQL comprobado en la séptima etapa; falta el recorrido en navegador dentro del arranque integral aislado.

## Quinta etapa cerrada — acceso de pacientes, 16 de septiembre de 2026

- El usuario pidió avances visibles y reservar aproximadamente 10% del cupo **en esta ejecución**. El umbral de las automatizaciones futuras no se modificó.
- Retirado el código que creaba una cuenta compartida y autenticaba sin Google. La ruta antigua `/paciente/login-google-demo` solo redirige a OAuth real, sin escribir cuentas ni sesiones.
- Retirados enlaces demo de landing/login; el acceso visible usa Google. Eliminado el formulario local de contraseña de la plantilla de pacientes, que no correspondía a la configuración actual (sin formLogin de pacientes). Se mantiene la autenticación del personal.
- 49/49 pruebas Java seleccionadas aprobadas y empaquetado BUILD SUCCESS a las 16:27 Lima. Incluye 5 pruebas nuevas del controlador y 4 contratos de acceso. No se eliminaron cuentas ni datos existentes.
- Previsualización para el usuario: http://127.0.0.1:4174/clinica-preview (Node sesión 32191, activo; HTTP 200 comprobado). Sin datos reales ni guardado; apertura en Codex solicitada, respuesta queued.
- Última consulta de cupo: 12% restante antes de documentación; terminar para conservar aproximadamente 10%. Falta OAuth real y aplicación conectada en entorno aislado.

## Cuarta etapa cerrada — renderizado del panel, 16 de septiembre de 2026

- Corregida entrada general del expediente: nueva ruta `/interno/clinica` elige el primer módulo permitido, con orden estable y conservando paciente/cita. La cabecera ya no dirige siempre al odontograma a usuarios sin ese permiso.
- Plantilla: pestaña activa y panel inicialmente visible se calculan en servidor a partir del módulo solicitado; eliminada la selección fija del odontograma.
- Añadida `ClinicaTemplateTest`: **13/13 pruebas aprobadas** de renderizado real Thymeleaf y consejos MVC con datos sintéticos, sin base de datos. Cubre los ocho módulos con permisos individuales, navegación inicial, redirección con paciente/cita, 403/404, escape de nombres y metadatos CSRF.
- La prueba genera `target/dentia-verification/preview/clinica.html`; el servidor estático puede mostrarla en `/clinica-preview`. Es solo inspección sin pacientes ni API, no el sistema clínico conectado.
- Se usó un servidor temporal en 127.0.0.1:4174 y se detuvo al terminar, verificando el PID por puerto y comando. No se detuvo el servidor previo del estudio 3D.
- En navegador se comprobó el cambio a Atenciones, rechazo de registrar sin paciente, regreso al odontograma, selección FDI 1- Regresión seleccionada y empaquetado: **40/40 pruebas Java aprobadas**, BUILD SUCCESS, 16/09/2026 12:26 Lima. JAR actualizado en `target/dentia-verification/dentia-1.0.0.jar`. Las 8 pruebas MySQL aprobadas de la etapa previa no se reejecutaron en esta etapa.
- No se tocó la base real ni se desplegó. Sigue pendiente iniciar la aplicación completa en entorno aislado y recorrer el flujo de guardado en navegador. Priorizar también la retirada del acceso demo inseguro antes de cualquier uso real.

## Tercera etapa cerrada — MySQL aislado, 16 de septiembre de 2026

- Detectado MySQL 8.0.43 instalado en `C:/Program Files/MySQL/MySQL Server 8.0`; no estaba en PATH.
- Añadidos `scripts/test-clinica-mysql.ps1` (PowerShell 7) y `ClinicaMysqlIntegrationTest`. El script crea un datadir nuevo en `target/dentia-mysql-checks/<UUID>`, puerto de loopback efímero y esquema sintético nuevo; verifica `@@datadir` antes de escribir. No utiliza credenciales ni configuración de la aplicación real. Arranca MySQL sin archivos de configuración externos, solo en 127.0.0.1 y con cuenta local vacía para la prueba temporal.
- **8/8 pruebas de integración MySQL aprobadas**, sin fallos, errores ni omisiones: migraciones reejecutables, persistencia/reapertura de borradores, cierre y reintento con borrador posterior, dos guardados simultáneos, dos cierres idénticos simultáneos, guardar contra finalizar, rollback integral ante fallo provocado y persistencia/concurrencia del odontograma.
- Se ejecutaron las tres migraciones clínicas reales mediante `ClinicaSchemaConfig`. Los servicios usan proxies transaccionales Spring reales y JDBC/MySQL, sin mocks. Las cuatro tablas padre (pacientes, profesionales, citas y presupuestos) son estructuras mínimas sintéticas: esto NO equivale a haber arrancado toda la aplicación desde una base vacía.
- Se comprobó rollback de atención, estado de cita, estado del procedimiento y borrador al provocar un fallo SQL al insertar el recibo final. El trigger de prueba se retiró en la misma base sintética.
- Comando aprobado: `pwsh -NoProfile -File scripts/test-clinica-mysql.ps1`. BUILD SUCCESS el 16/09/2026 a las 06:32 Lima. Informe: `target/dentia-verification/surefire-reports/com.dentia.service.clinica.ClinicaMysqlIntegrationTest.txt`.
- Ejecución aprobada: `target/dentia-mysql-checks/74e3794819354529b91448aa44987290`, puerto temporal 57196. Instancia detenida por el script; datos sintéticos y logs conservados. Dos intentos previos solo fallaron en el lanzador (PowerShell 5/lectura de ExitCode y opción CLI no soportada); el lanzador final requiere PowerShell 7 y es compatible con MySQL instalado.
- Repetición final tras reforzar el cierre de procesos hijos MySQL en Windows: **8/8 aprobadas** a las 06:35 Lima, directorio `target/dentia-mysql-checks/10d371ac9f6440b280afc03e4db33aea`, puerto 57280. Se cerró también el hijo residual del intento fallido anterior tras verificar su datadir. Comprobación final con Win32_Process: **ninguna instancia de dentia-mysql-checks permanece activa**. No se detuvieron servicios MySQL ajenos.
- Siguiente etapa: renderizar Thymeleaf y probar el panel completo en navegador. Continúan pendientes el acceso demo inseguro y fallos heredados; no desplegar aún.

## Estado al 16 de septiembre de 2026 — cierre de segunda etapa

- Se completó una etapa acotada: ciclo seguro de guardar borrador y finalizar atención. No se ejecutó la aplicación ni migraciones contra la base real.
- Nuevo `AtencionClinicaService` extraído de `ClinicaInternaService`: bloqueo por paciente, versión obligatoria al finalizar y validación de cita/procedimiento. Rechaza pacientes inexistentes, citas ajenas/cerradas y procedimientos ajenos/cerrados.
- Nuevo contrato de POST `/interno/clinica/api/atenciones`: `{version, solicitudId, contenido}`, con UUID de solicitud. Cliente actualizado; el formato anterior se rechaza.
- Cerrar conserva un marcador JSON `null` con versión creciente en `clinica_atencion_borradores`; ya no elimina la fila. Evita recreación de borradores desde sesiones antiguas.
- Migración aditiva `V2026_09_16_01__atenciones_idempotentes.sql` conectada a `ClinicaSchemaConfig`: recibos persistentes con clave, atención y SHA-256 de solicitud/actor. Reintentar devuelve el mismo ID sin borrar borradores posteriores. No se ha aplicado a MySQL durante esta tarea.
- Nuevo `atencion-session.js`: estado de edición en memoria y reintentos estables. `clinica.js` bloquea doble envío y guardar/finalizar simultáneos, conserva texto al cerrar modal, avisa al cambiar paciente/salir con cambios y mantiene versión de apertura. No guarda datos clínicos en localStorage.
- Corregido vínculo de cita en formulario: no reutiliza el ID inicial de otro paciente ni el de una cita ya finalizada.
- `AtencionClinicaServiceTest`: 13 pruebas nuevas; `ClinicaAtencionApiTest`: 4 pruebas nuevas de contrato, permisos, JSON inválido y HTTP 409. Total Java seleccionado **27/27** aprobado, sin conectar a una base de datos.
- `node --test scripts/test-atencion-session.cjs`: **6/6** aprobadas. `node --check .../clinica.js`: OK. Verificador GLB: OK (32 FDI + 1 adicional; fuente conservada).
- Empaquetado con las 27 pruebas habilitadas: **BUILD SUCCESS**, 16/09/2026 06:25 Lima. JAR: `target/dentia-verification/dentia-1.0.0.jar`.
- Pendiente en esta funcionalidad: MySQL aislado (migraciones, persistencia, bloqueo/rollback concurrente) y recorrido del formulario real en navegador. Las pruebas actuales son unitarias/contrato HTTP, no equivalen a una prueba integral. Mejorar más adelante la conciliación guiada de conflictos sin forzar sobrescrituras.

## Primera etapa — antecedentes verificados el 15 de septiembre

- Base: Java 17 / Spring Boot 3.2.6 / Thymeleaf / MySQL. Paquete base migrado a `com.dentia`.
- Modelo autorizado: `assets/dental/articulated-v2/dentia_articulado.blend` y `.glb`; 32 FDI más una pieza adicional del archivo fuente, conservada sin FDI. Raíces y articulación ilustrativas.
- Añadido `static/js/dentia/dental-viewer.js`: visor reutilizable por grupos de dientes, apertura, selección, capas, cámara y disposición de recursos. Conectado en `clinica.js`; prueba independiente en `static/dental-studio.html`.
- Copiado el GLB v2 y manifiesto a `static/models/dental/dentia-articulado-v2.*`. Three.js 0.128 y controles/cargador/licencia disponibles localmente en `static/vendor/three`.
- Añadidos y conectados `OdontogramaService`, DTO de posiciones/versiones/borradores, `ClinicaExceptionHandler` y migración aditiva `V2026_09_14_01__odontograma_versionado.sql`. Esquema aún no ejecutado en MySQL durante esta tarea.
- `ClinicaInternaService` carga posiciones, versión, historial y borrador. Guardado de borrador con versión; finalización valida vínculo entre cita/procedimiento y paciente. Revisar conflictos de borrador al finalizar como siguiente mejora.
- `ClinicaInternaController` exige permisos por módulo y filtra campos del expediente según permisos. Antes existía un permiso de odontograma que daba acceso a todas las escrituras.
- Eliminadas detecciones IA fijas en la panorámica; ahora revisión visual manual y guardado explícito. Quitados fechas, nombres y avances clínicos ficticios del inspector.
- `clinica.js` bajó de 2285 a 1674 líneas; reemplazado motor antiguo por adaptador. Corregidos zoom/escala, reapertura de visor, estado entre pacientes, doble JSON y superficies mesiales/distales FDI.
- Vista independiente con apertura, arcadas, encías, raíces, selección FDI e inclinación. `node scripts/preview-3d.mjs` sirve solo archivos estáticos en 127.0.0.1:4173. Puede quedar ejecutándose; comprobar puerto antes de iniciarlo otra vez.
- Actualizadas pruebas obsoletas de reserva para `HomeController` y de acceso paciente por Google.
- Surefire 3.2.5 y separación de pruebas de integración mediante etiqueta `integration`; perfil `-Pintegration` las habilita. Las pruebas con MySQL requieren base aislada.
- Primer `mvn test`: pruebas antiguas no compilaban (corregidas). Segundo intento: Windows bloqueó `target/maven-status/.../createdFiles.lst`; usar un directorio de compilación propio, sin borrar ni detener procesos del usuario.
- Directorio aislado operativo: `-Ddentia.build.directory=target/dentia-verification`.
- Empaquetado `mvn -B -ntp '-Ddentia.build.directory=target/dentia-verification' -DskipTests package`: **BUILD SUCCESS**, 15/09/2026 17:24 Lima. JAR generado en ese directorio; no se ejecutó contra la base del usuario.
- README sustituido por documentación de DentIA. Arquitectura en `docs/ARQUITECTURA.md`.
- **10 pruebas específicas aprobadas:** `OdontogramaServiceTest` (4), `ClinicaSecurityTest` (2), `PatientPortalControllerTest` (1), `LoginFailureHandlerTest` (3). Informes en `target/dentia-verification/surefire-reports`.
- Suite completa previa: 128 pruebas, 5 fallos y 23 errores. Dos fallos de login ya corregidos. Restan fallos heredados de plantillas/login y cálculos de materiales/ventas; no ocultarlos ni afirmar suite completa verde.
- `node scripts/verify-dental-model.mjs`: OK, GLB idéntico al v2 fuente, 32 FDI + 1 adicional, 33 coronas, 33 raíces, 2 encías y animación mandibular, 3.127.852 bytes. Tres JS pasaron `node --check`.
- Navegador real: modelo renderizado; apertura cambió a 100%, se ocultaron encías y se seleccionó FDI 16, confirmado en interfaz. Falta revisar móvil y el panel clínico completo conectado a MySQL. La geometría fuente requiere refinamiento visual en límites corona/encía; preservar originales.

## Próximos pasos concretos

1. Renderizado Thymeleaf comprobado (13 pruebas nuevas) y empaquetado con 40 pruebas específicas. Falta QA visual completa/responsive y aplicación conectada; la previsualización estática no sustituye esos recorridos.
2. Pruebas MySQL clínicas, de presupuestos y catálogos aisladas completadas (16/16). Los seeds ya conservan nombres/precios y demás ajustes al arrancar. Falta preparar esquema integral y arranque seguro del panel completo en una base de prueba: no apuntar a la base del usuario.
3. Verificar el flujo de atención completo en navegador: guardar/reabrir, conflicto entre pestañas, reintento tras perder respuesta, cambio de paciente y cita. La protección ya está implementada y probada unitariamente; falta integración real.
4. Acceso demo retirado: ruta antigua redirige a OAuth, sin crear cuentas ni sesiones. Verificar OAuth real y revisar cuentas/sesiones demo preexistentes antes de producción; no se tocaron datos existentes.
5. Ordenar módulos administrativos frente al núcleo DentIA sin eliminar trabajo existente. Separar más responsabilidades de `ClinicaInternaService` y `clinica.js` por dominio.
6. Prueba responsive y panel real; mejorar separación visual de coronas/encías del modelo fuente conservando originales. Documentar procedencia/licencia del modelo aportado.
7. Completar flujo de planes, presupuestos, atenciones, seguimiento y reportes. La generación de presupuestos ya valida precios y está comprobada en MySQL (concurrencia, reintento y rollback); falta verificarla en navegador.

## Gestión del cupo

El cupo se consumió más rápido de lo previsto en dos sesiones. Consultar uso cada 2–3 bloques de herramientas y actualizar este archivo al cerrar cada cambio, no solo al final. Hacer una etapa pequeña por activación y terminar con resultado verificable; no esperar a agotar el cupo. Automatización: `continuar-dentia-por-etapas`.

## Contexto recuperado

Flujo previsto: paciente → cita → atención → odontograma → diagnóstico → plan → seguimiento → pagos. Guardar trabajo incompleto para reanudar tras salir. El trabajo 3D anterior se interrumpió por cuota después de generar los archivos v2; falta su conexión correcta al sistema.
