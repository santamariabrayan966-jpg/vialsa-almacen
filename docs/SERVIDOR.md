# DentIA — publicación verificada del 24/10/2026

- Servidor: `brayan@100.90.117.108`, `brayan-server`, mediante Tailscale.
- Acceso público: https://dentia.dodsix.com.
- Servicio: `dentia.service`, `/opt/dentia/current.jar`, puerto 8080.
- Paquete publicado: `target/approved-release/dentia-1.0.0.jar`.
- SHA-256: `d9717b855d45bd94869066bbda9cc0e0cba1c017b6a60511c190458b86300e4e`.
- Respaldo previo: `/var/backups/dentia/release-20261024-183743`, incluye JAR, configuración y MySQL comprimido. La restauración en base temporal se verificó antes de publicar. No restaurar encima de producción sin revisar la actividad posterior.
- Resultado de publicación: PASS. HTTPS entrega el formulario y un usuario inexistente vuelve al formulario con error visible. Se conservaron las credenciales de los usuarios y los procesos de Suma, MySQL y Cloudflare.
- Causa del blanco en la versión anterior: `EmptyResultDataAccessException` al consultar intentos de acceso de un usuario inexistente. La consulta del paquete actual devuelve cero si no encuentra la cuenta.
- Validación del paquete: 236 pruebas Java y 19 pruebas MySQL aisladas aprobadas. No se abrió navegador en esta etapa.

## Filtros personales y navegación

Pacientes conserva acceso general. Sus opciones son Todos los pacientes, Mis pacientes y Mis atenciones. Los filtros usan el profesional vinculado al usuario; si una atención no tiene profesional asignado, se usa el usuario que la registró. Recepción no adquiere las atenciones del doctor por el hecho de digitarlas. Para vincular un doctor, usar la configuración existente de Profesionales.

En celular hay barra inferior Inicio, Pacientes, Agenda, Clínica y Más, según permisos. Más conserva los accesos del consultorio y administración. La barra lateral permanece en computadora.

## Operación

La publicación usa autenticación SSH por clave y autorización sudo personal en terminal; no se guardan contraseñas. `scripts/deploy-server.py` prepara respaldo, verifica restauración y reemplaza únicamente DentIA. Conserva la aplicación anterior si falla la comprobación del acceso público. Las modificaciones de esquema aditivas no se revierten automáticamente.

La sesión de publicación terminó tras PASS por permisos de descarga de su informe; el resultado completo quedó en `target/ssh-session/dentia-20261024/03-deploy.log`. Esto no revirtió la publicación. Una próxima operación administrativa requiere una sesión autenticada nueva.
