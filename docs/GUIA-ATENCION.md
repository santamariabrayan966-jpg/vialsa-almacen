# Guía de atención de DentIA

1. **Registrar paciente**: buscar primero por DNI y guardar sus datos en Pacientes. Su ficha conserva datos, historia clínica y atenciones.
2. **Con cita**: abrir la cita en Agenda y pulsar Atender. **Sin cita**: abrir Pacientes y pulsar Atender.
3. **Marcar hallazgos**: elegir caries, resina, sellante, corona, endodoncia, brackets, ausente, implante o borrar y tocar la superficie o el marcador de pieza completa. Se guarda al tocar. Si falla, aparece Reintentar guardado; esperar la confirmación antes de salir.
4. **Cotizar**: seleccionar pieza y servicio del catálogo del consultorio, revisar el precio y pulsar Cotizar. La propuesta no representa un pago.
5. **Aprobar el plan** después de acordarlo con el paciente. Se genera su presupuesto y saldo. Atender los procedimientos desde ese mismo plan para mantener la vinculación y evitar cargos duplicados.
6. **Atención directa**: confirmar profesional, piezas y procedimiento realizado. Revisar el importe acordado opcional. Al finalizar se guarda la atención, se actualiza el odontograma y se genera un cargo pendiente si hay importe. Los procedimientos de plan conservan su presupuesto.
7. **Cobrar**: ir a Cuentas y Deudas, elegir presupuesto y registrar abono con monto, fecha, método y referencia. Confirmar únicamente un cobro recibido. Se admiten pagos parciales hasta cubrir el saldo. Ver constancia abre el registro interno para imprimir.
8. **Seguimiento**: revisar indicaciones y próxima cita sugerida. Crear la próxima cita real en Agenda: escribir una sugerencia no reserva horario. En cada sesión atender el procedimiento pendiente del mismo plan. Al completarlos, el plan pasa a Completado; el saldo se controla por separado.
9. **Revisar actividad**: Inicio y Pacientes incluyen atendidos hoy, con cita o sin cita. Reportes consulta registros reales del período; los períodos vacíos muestran cero.

## Permisos

En Roles y permisos habilitar Acceso al odontograma y escoger Ver odontograma 2D y/o Ver odontograma 3D. Los roles existentes conservan 2D y requieren habilitación expresa para 3D. ADMIN mantiene acceso completo conforme a la política existente.

## Validación y comprobantes

Corregir los campos señalados dentro del formulario. Ante un conflicto de edición, recargar el expediente antes de continuar. La atención y su cargo se guardan en una misma transacción; los reintentos de la misma solicitud no los duplican.

La constancia de cobro es interna. Esta versión no incorpora emisión de boleta electrónica ni integración tributaria.

## Versión local del 24/10/2026

Acceso: http://localhost:8080/ o http://127.0.0.1:8080/.

Paquete: `target/final-release/dentia-1.0.0.jar`. Logs: `target/local-server.log` y `target/local-server-error.log`.

Verificación: 235 pruebas Java, 18 MySQL aisladas y 7 JavaScript aprobadas. Revisión en navegador a 390 × 844. Las escrituras de prueba se ejecutaron en una base temporal, sin crear atenciones ni pagos ficticios en los pacientes reales.
