# DentIA

Sistema de gestión odontológica de Brayan Santamaría Gonzales. Integra pacientes, agenda, historia clínica, odontograma 2D/3D, diagnósticos, planes, atenciones y gestión administrativa.

## Estado del proyecto

En consolidación por etapas. El visor 3D está conectado en el código del panel clínico y tiene una vista independiente verificada en navegador. El proyecto compila y genera un JAR. Las atenciones y el odontograma tienen pruebas de persistencia/concurrencia aprobadas en MySQL aislado. El panel completo y las pruebas heredadas aún requieren trabajo.

- [Estado verificado y siguiente etapa](docs/CONTINUIDAD.md)
- [Arquitectura y organización](docs/ARQUITECTURA.md)

## Ver el modelo 3D

Requiere Node.js instalado. Desde la raíz:

```powershell
node scripts/preview-3d.mjs
```

Abrir [Estudio dental local](http://127.0.0.1:4173/dental-studio.html). La vista usa archivos locales y no necesita MySQL. Incluye apertura mandibular, selección FDI, encías, raíces, arcadas, inclinación por pieza y descarga GLB. Los ajustes de esta vista exploratoria duran la sesión; en el panel clínico se guardan mediante la API del paciente.

El original editable está en `assets/dental/articulated-v2/dentia_articulado.blend`. Contiene 32 piezas FDI y una pieza adicional del archivo fuente. Las raíces y el eje de apertura son ilustrativos. La panorámica del panel sirve de referencia para registro manual; no realiza inferencia de IA ni reconstrucción desde radiografías.

## Compilar y verificar

Requisitos: JDK 17 o posterior, Maven 3.9, Node.js para verificar el visor y MySQL 8 para ejecutar la aplicación completa.

```powershell
node scripts/verify-dental-model.mjs
node --test scripts/test-atencion-session.cjs
mvn -B -ntp "-Ddentia.build.directory=target/dentia-verification" "-Dtest=PatientAuthControllerTest,LoginTemplateContractTest,ClinicaTemplateTest,AtencionClinicaServiceTest,ClinicaAtencionApiTest,OdontogramaServiceTest,ClinicaSecurityTest,PatientPortalControllerTest,LoginFailureHandlerTest" package
```

El 16/09/2026 pasaron 49 pruebas Java específicas, incluidas 13 de renderizado Thymeleaf; el JAR se generó con esas pruebas habilitadas. En la etapa anterior pasaron también 6 pruebas JavaScript y la verificación de integridad del modelo 3D. La suite completa aún tiene fallos heredados documentados.

Después de estas pruebas, el servidor estático permite inspeccionar el panel en `/clinica-preview`: usa HTML generado por Thymeleaf, sin pacientes ni API. Se comprobaron sus pestañas, selección dental y control de apertura en navegador. El flujo conectado, las escrituras y la revisión visual móvil siguen pendientes.

### Pruebas clínicas con MySQL aislado

Requieren PowerShell 7 y MySQL Server 8.0 instalado. El lanzador crea una instancia nueva en loopback, comprueba su directorio de datos, ejecuta solo las pruebas clínicas y la detiene al terminar:

```powershell
pwsh -NoProfile -File scripts/test-clinica-mysql.ps1
# Si MySQL está en otra ubicación:
pwsh -NoProfile -File scripts/test-clinica-mysql.ps1 -MySqlBase 'D:\MySQL'
```

**8/8 aprobadas** el 16/09/2026 sobre MySQL 8.0.43. Incluyen migraciones, borradores, idempotencia, concurrencia, rollback y odontograma. Datos sintéticos y logs quedan en `target/dentia-mysql-checks/<UUID>`. No se usan datos ni credenciales de la aplicación. Las tablas padre del entorno son mínimas; falta verificar el panel y el esquema completo. No ejecutes toda la suite con `-Pintegration` contra tu base real.

## Ejecutar la aplicación

La configuración está en `src/main/resources/application*.properties` y `env.example`. Preparar las variables de conexión MySQL y OAuth conforme a esos archivos, y el esquema existente de DentIA antes de iniciar. En la instalación local de Windows se usa la base `dentia` del puerto 3306; las credenciales y claves de sesión/recuperación se guardan fuera del repositorio como variables del usuario de Windows.

```powershell
java -jar target/dentia-verification/dentia-1.0.0.jar
```

La URL habitual es [DentIA local](http://localhost:8080/), salvo que se configure otro puerto. «Mantener mi sesión iniciada» crea una cookie firmada durante siete días; no almacena la contraseña. La recuperación usa exclusivamente el correo registrado en el usuario y requiere SMTP configurado. No se desplegó este trabajo a producción.

## Seguridad antes de producción

- Usar `SPRING_PROFILES_ACTIVE=prod` y establecer `DENTIA_PASSWORD_RESET_KEY` con una cadena aleatoria de al menos 32 caracteres en el archivo de entorno protegido del servicio. Configurar también `DENTIA_REMEMBER_ME_KEY` y el correo SMTP para la recuperación de contraseña.
- Rotar la contraseña de cualquier administrador inicial y las credenciales que hayan estado en archivos `.env` versionados; borrarlos del árbol actual no los elimina del historial Git.
- Mantener MySQL accesible solo localmente, comprobar HTTPS efectivo y el encabezado `X-Forwarded-Proto` desde el proxy de confianza, y probar login, recuperación, permisos y PWA en el servidor después de desplegar.
- El JAR se genera, pero la suite completa todavía falla en pruebas heredadas de cálculo de medidas/stock. No tomar la compilación aislada como certificación de seguridad o de funcionamiento integral.
