# Arquitectura de DentIA

## Organización actual

El paquete base es `com.dentia` y el artefacto es `dentia`. La separación por dominio se introduce de forma modular.

| Área | Ubicación | Responsabilidad |
|---|---|---|
| Web clínica | `controller/interno/ClinicaInternaController` | Permisos por módulo, páginas y API |
| Orquestación clínica | `service/interno/ClinicaInternaService` | Expediente, diagnósticos, planes y delegación por dominio |
| Atenciones | `service/clinica/AtencionClinicaService` | Borradores versionados, cierre transaccional y reintentos idempotentes |
| Odontograma | `service/clinica/OdontogramaService` | Validación FDI, transacciones, historial, posiciones y versiones |
| Datos clínicos | `dao/Jdbc/JdbcClinicaDao` | SQL parametrizado y acceso a MySQL |
| Contratos | `dto/clinica/ClinicaRequests` | Estructura de peticiones de la API |
| Interfaz clínica | `static/js/dentia/clinica.js` | Formularios, selección de paciente y conexión con API |
| Sesión de atención | `static/js/dentia/atencion-session.js` | Versión de apertura, exclusión de guardados y reintentos del formulario |
| Motor 3D | `static/js/dentia/dental-viewer.js` | Escena, jerarquía dental, capas, cámara y apertura |
| Vista exploratoria | `static/dental-studio.html` | Comprobación del modelo sin datos de pacientes |
| Fuentes 3D | `assets/dental/articulated-v2` | Blender, generador, GLB y manifiesto |
| Distribución web | `static/models/dental` y `static/vendor/three` | Modelo exportado y dependencias fijadas con licencia |

## Navegación por permisos

La entrada general `/interno/clinica` selecciona un módulo autorizado usando el orden estable de `MODULOS_CLINICOS`; conserva los parámetros de paciente y cita. El enlace compartido de la cabecera usa esa entrada, no presupone permiso de odontograma. Las pestañas visibles y la selección inicial del panel se renderizan según permisos y módulo actual.

## Contrato del modelo

Cada `FDI_XX` es un grupo con una corona y raíces. Se selecciona y transforma el grupo completo. La pieza adicional conserva su identidad sin recibir un FDI inventado. El GLB ya usa metros y eje Y vertical; no se le aplica una segunda conversión de ejes de Blender. La animación afecta a la mandíbula; los ajustes dentales se aplican por separado.

El expediente mantiene hallazgos clínicos y geometría ilustrativa en campos diferentes. El color del visor resume hallazgos registrados; la evaluación por superficie permanece en el odontograma 2D.

## Escritura del odontograma

La API recibe el ID de odontograma y su versión. El servicio valida todas las piezas antes de modificar datos, bloquea el odontograma durante la transacción, compara la versión, guarda los cambios, añade historial e incrementa la versión. Una edición desactualizada recibe HTTP 409. El cliente recarga los datos después de guardar.

La base conserva un historial de ajustes y eliminación de hallazgos.

## Guardado y finalización de atenciones

El borrador se guarda explícitamente, por paciente y con versión. Guardar y finalizar bloquean la fila del paciente dentro de una transacción. El cierre recibe `{version, solicitudId, contenido}`; la versión debe coincidir con la del borrador y la solicitud debe contener un UUID. Clientes antiguos sin este contrato se rechazan; no se asume una versión por defecto.

Después del cierre se conserva la fila del borrador con contenido JSON `null` y versión incrementada. Así, una sesión anterior no puede recrear un borrador cerrado usando la versión cero. La tabla `clinica_atencion_solicitudes` conserva el ID de atención y una huella del contenido, versión y actor. Un reintento idéntico devuelve la atención existente sin tocar borradores posteriores. Una clave reutilizada con otros datos recibe HTTP 409.

El formulario conserva lo escrito al cerrar el modal, fija el paciente y la versión de apertura, bloquea operaciones simultáneas y avisa antes de abandonar cambios no guardados. Si no se confirma el cierre, conserva la misma clave y permite reintentarlo. Los datos en edición viven solo en memoria; no se copian a almacenamiento local del navegador. Un conflicto no sobrescribe automáticamente el trabajo de otro profesional ni descarta el contenido local.

Las pruebas unitarias y de contrato HTTP cubren estas reglas. Ocho pruebas adicionales con MySQL 8.0.43 aislado verifican las migraciones reales, persistencia, escrituras concurrentes, reintentos y rollback de atención/cita/plan/borrador. El lanzador es `scripts/test-clinica-mysql.ps1`; valida el directorio de datos antes de escribir y detiene solo su propia instancia. Usa tablas padre sintéticas mínimas; no valida todavía el arranque de toda la aplicación ni el recorrido del navegador.

## Verificación y límites actuales

`scripts/verify-dental-model.mjs` comprueba cabecera GLB, jerarquía, número de piezas, partes, animación y equivalencia con el exportado fuente. Las pruebas unitarias cubren FDI, lotes inválidos, versiones y permisos. La vista exploratoria fue comprobada en navegador. Las tres migraciones clínicas ya se ejecutaron en MySQL aislado con datos sintéticos; no se aplicaron a la base real del usuario.

La aplicación conserva módulos heredados de comercio e inventario. Su separación y las pruebas que fallan se resolverán por etapas; no se borraron módulos ni datos anteriores.

`ClinicaTemplateTest` renderiza las vistas Thymeleaf y los consejos de cabecera/permisos reales con dependencias de datos simuladas. Sus 13 casos cubren módulos, permisos, navegación, cita, escape HTML y CSRF en la plantilla. Genera una vista estática de inspección sin pacientes, servida por `scripts/preview-3d.mjs` en `/clinica-preview`. No ofrece API ni persistencia; se usa para comprobar controles y navegación, no como demostración de un flujo conectado.
