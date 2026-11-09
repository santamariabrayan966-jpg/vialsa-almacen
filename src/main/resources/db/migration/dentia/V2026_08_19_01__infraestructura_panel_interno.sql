USE dentia;

-- Infraestructura minima e independiente del panel interno de DentIA.
-- Este script es aditivo: no elimina ni modifica pacientes/cuentas_paciente.

CREATE TABLE IF NOT EXISTS roles (
    idRol INT NOT NULL AUTO_INCREMENT,
    NombreRol VARCHAR(80) NOT NULL,
    descripcion VARCHAR(255) NULL,
    tipo_rol VARCHAR(30) NOT NULL DEFAULT 'INTERNO',
    activo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (idRol),
    UNIQUE KEY uk_roles_nombre (NombreRol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS estadousuario (
    idEstadoUsuario INT NOT NULL AUTO_INCREMENT,
    NombreEstado VARCHAR(40) NOT NULL,
    PRIMARY KEY (idEstadoUsuario),
    UNIQUE KEY uk_estadousuario_nombre (NombreEstado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sedes (
    idSede INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    codigo VARCHAR(40) NOT NULL,
    tipo VARCHAR(40) NOT NULL DEFAULT 'CONSULTORIO',
    direccion VARCHAR(255) NULL,
    ciudad VARCHAR(100) NULL,
    responsable VARCHAR(150) NULL,
    telefono VARCHAR(30) NULL,
    descripcion VARCHAR(500) NULL,
    foto VARCHAR(500) NULL,
    google_maps_url VARCHAR(1000) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    fechaRegistro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idSede),
    UNIQUE KEY uk_sedes_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permisos (
    idPermiso INT NOT NULL AUTO_INCREMENT,
    idRol INT NOT NULL,
    modulo VARCHAR(80) NOT NULL,
    puedeAcceder TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (idPermiso),
    UNIQUE KEY uk_permisos_rol_modulo (idRol, modulo),
    CONSTRAINT fk_permisos_rol
        FOREIGN KEY (idRol) REFERENCES roles (idRol)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuarios (
    idUsuario INT NOT NULL AUTO_INCREMENT,
    NombreUsuario VARCHAR(100) NOT NULL,
    Nombres VARCHAR(150) NULL,
    Apellidos VARCHAR(150) NULL,
    NroDocumento VARCHAR(30) NULL,
    Correo VARCHAR(180) NULL,
    correo_recuperacion VARCHAR(180) NULL,
    Telefono VARCHAR(30) NULL,
    direccion VARCHAR(255) NULL,
    Contrasena VARCHAR(100) NOT NULL,
    FechaCreacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idTipoDocumento INT NULL,
    idRol INT NOT NULL,
    idSedePrincipal INT NULL,
    puedeCambiarSede TINYINT(1) NOT NULL DEFAULT 0,
    idEstadoUsuario INT NOT NULL DEFAULT 1,
    foto VARCHAR(500) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    bloqueado_hasta DATETIME NULL,
    PRIMARY KEY (idUsuario),
    UNIQUE KEY uk_usuarios_nombre (NombreUsuario),
    UNIQUE KEY uk_usuarios_correo (Correo),
    UNIQUE KEY uk_usuarios_correo_recuperacion (correo_recuperacion),
    KEY idx_usuarios_rol (idRol),
    KEY idx_usuarios_estado (idEstadoUsuario),
    KEY idx_usuarios_sede (idSedePrincipal),
    CONSTRAINT fk_usuarios_rol
        FOREIGN KEY (idRol) REFERENCES roles (idRol)
        ON UPDATE CASCADE,
    CONSTRAINT fk_usuarios_estado
        FOREIGN KEY (idEstadoUsuario) REFERENCES estadousuario (idEstadoUsuario)
        ON UPDATE CASCADE,
    CONSTRAINT fk_usuarios_sede
        FOREIGN KEY (idSedePrincipal) REFERENCES sedes (idSede)
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id_token BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    codigo VARCHAR(120) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion DATETIME NOT NULL,
    usado TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id_token),
    UNIQUE KEY uk_password_reset_codigo (codigo),
    KEY idx_password_reset_usuario (id_usuario),
    KEY idx_password_reset_expiracion (fecha_expiracion),
    CONSTRAINT fk_password_reset_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (idUsuario)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS panel_colores_config (
    idConfig INT NOT NULL,
    colorHeader VARCHAR(20) NOT NULL,
    colorHeaderSecundario VARCHAR(20) NOT NULL,
    colorFondo VARCHAR(20) NOT NULL,
    colorTarjeta VARCHAR(20) NOT NULL,
    colorBotonPrimario VARCHAR(20) NOT NULL,
    colorAcento VARCHAR(20) NOT NULL,
    colorTextoPrincipal VARCHAR(20) NOT NULL,
    colorTextoHeader VARCHAR(20) NOT NULL,
    colorFooter VARCHAR(20) NOT NULL,
    colorTextoFooter VARCHAR(20) NOT NULL,
    posicionMenu VARCHAR(20) NOT NULL DEFAULT 'IZQUIERDA',
    fechaActualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (idConfig)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS login_apariencia_config (
    idConfig INT NOT NULL,
    tipoFondo VARCHAR(20) NOT NULL DEFAULT 'DEGRADADO',
    colorFondo1 VARCHAR(20) NOT NULL,
    colorFondo2 VARCHAR(20) NOT NULL,
    imagenFondo VARCHAR(500) NULL,
    posicionCard VARCHAR(20) NOT NULL DEFAULT 'CENTRO',
    titulo VARCHAR(120) NOT NULL,
    subtitulo VARCHAR(180) NULL,
    textoInformativo VARCHAR(255) NULL,
    colorCard VARCHAR(20) NOT NULL,
    colorTexto VARCHAR(20) NOT NULL,
    colorBoton1 VARCHAR(20) NOT NULL,
    colorBoton2 VARCHAR(20) NOT NULL,
    colorTextoBoton VARCHAR(20) NOT NULL,
    carruselActivo TINYINT(1) NOT NULL DEFAULT 0,
    carruselImagen1 VARCHAR(500) NULL,
    carruselImagen1Zoom DECIMAL(7,3) NOT NULL DEFAULT 1.000,
    carruselImagen1OffsetX DECIMAL(9,3) NOT NULL DEFAULT 0.000,
    carruselImagen1OffsetY DECIMAL(9,3) NOT NULL DEFAULT 0.000,
    carruselImagen2 VARCHAR(500) NULL,
    carruselImagen2Zoom DECIMAL(7,3) NOT NULL DEFAULT 1.000,
    carruselImagen2OffsetX DECIMAL(9,3) NOT NULL DEFAULT 0.000,
    carruselImagen2OffsetY DECIMAL(9,3) NOT NULL DEFAULT 0.000,
    carruselImagen3 VARCHAR(500) NULL,
    carruselImagen3Zoom DECIMAL(7,3) NOT NULL DEFAULT 1.000,
    carruselImagen3OffsetX DECIMAL(9,3) NOT NULL DEFAULT 0.000,
    carruselImagen3OffsetY DECIMAL(9,3) NOT NULL DEFAULT 0.000,
    fechaActualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (idConfig)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marca_identidad_config (
    id_config INT NOT NULL,
    nombre_comercial VARCHAR(150) NOT NULL,
    razon_social VARCHAR(200) NULL,
    ruc VARCHAR(20) NULL,
    slogan VARCHAR(255) NULL,
    descripcion_corta VARCHAR(500) NULL,
    logo_principal VARCHAR(500) NULL,
    logo_claro VARCHAR(500) NULL,
    logo_oscuro VARCHAR(500) NULL,
    isotipo VARCHAR(500) NULL,
    favicon VARCHAR(500) NULL,
    logo_opcion_1 VARCHAR(500) NULL,
    logo_opcion_2 VARCHAR(500) NULL,
    logo_opcion_3 VARCHAR(500) NULL,
    logo_activo INT NOT NULL DEFAULT 1,
    color_primario VARCHAR(20) NOT NULL,
    color_secundario VARCHAR(20) NOT NULL,
    color_acento VARCHAR(20) NOT NULL,
    color_neutro_oscuro VARCHAR(20) NOT NULL,
    color_neutro_medio VARCHAR(20) NOT NULL,
    color_neutro_claro VARCHAR(20) NOT NULL,
    whatsapp_principal VARCHAR(30) NULL,
    telefono_principal VARCHAR(30) NULL,
    correo_contacto VARCHAR(180) NULL,
    sitio_web VARCHAR(500) NULL,
    direccion_principal VARCHAR(255) NULL,
    facebook_url VARCHAR(500) NULL,
    instagram_url VARCHAR(500) NULL,
    tiktok_url VARCHAR(500) NULL,
    linkedin_url VARCHAR(500) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    publicado TINYINT(1) NOT NULL DEFAULT 1,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_config)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO estadousuario (idEstadoUsuario, NombreEstado) VALUES
    (1, 'ACTIVO'),
    (2, 'INACTIVO'),
    (3, 'ELIMINADO')
ON DUPLICATE KEY UPDATE NombreEstado = VALUES(NombreEstado);

INSERT INTO roles (idRol, NombreRol, descripcion, tipo_rol, activo)
VALUES (1, 'ADMIN', 'Administrador de DentIA', 'INTERNO', 1)
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    tipo_rol = VALUES(tipo_rol),
    activo = VALUES(activo);

INSERT INTO sedes
    (idSede, nombre, codigo, tipo, descripcion, activo, fechaRegistro)
VALUES
    (1, 'Consultorio DentIA', 'DENTIA-UNICA', 'CONSULTORIO',
     'Registro tecnico unico para compatibilidad interna.', 1, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    tipo = VALUES(tipo),
    descripcion = VALUES(descripcion),
    activo = VALUES(activo);

INSERT INTO permisos (idRol, modulo, puedeAcceder) VALUES
    (1, 'dashboard', 1),
    (1, 'pacientes', 1),
    (1, 'citas', 1),
    (1, 'historia_clinica', 1),
    (1, 'odontograma', 1),
    (1, 'diagnosticos', 1),
    (1, 'tratamientos', 1),
    (1, 'plan_tratamiento', 1),
    (1, 'presupuestos', 1),
    (1, 'atenciones', 1),
    (1, 'pagos', 1),
    (1, 'seguimiento', 1),
    (1, 'documentos', 1),
    (1, 'profesionales', 1),
    (1, 'reportes', 1),
    (1, 'usuarios', 1),
    (1, 'roles', 1),
    (1, 'configuracion', 1)
ON DUPLICATE KEY UPDATE puedeAcceder = VALUES(puedeAcceder);

INSERT INTO panel_colores_config
    (idConfig, colorHeader, colorHeaderSecundario, colorFondo, colorTarjeta,
     colorBotonPrimario, colorAcento, colorTextoPrincipal, colorTextoHeader,
     colorFooter, colorTextoFooter, posicionMenu)
VALUES
    (1, '#0F766E', '#115E59', '#F4F7F9', '#FFFFFF',
     '#0F766E', '#14B8A6', '#1F2937', '#FFFFFF', '#134E4A', '#FFFFFF', 'IZQUIERDA')
ON DUPLICATE KEY UPDATE idConfig = idConfig;

INSERT INTO login_apariencia_config
    (idConfig, tipoFondo, colorFondo1, colorFondo2, posicionCard, titulo,
     subtitulo, textoInformativo, colorCard, colorTexto, colorBoton1,
     colorBoton2, colorTextoBoton, carruselActivo)
VALUES
    (1, 'DEGRADADO', '#0F766E', '#0F172A', 'CENTRO', 'DentIA',
     'Gestion inteligente para tu consultorio dental',
     'Acceso exclusivo para el equipo del consultorio',
     '#FFFFFF', '#1F2937', '#0F766E', '#14B8A6', '#FFFFFF', 0)
ON DUPLICATE KEY UPDATE idConfig = idConfig;

INSERT INTO marca_identidad_config
    (id_config, nombre_comercial, razon_social, slogan, descripcion_corta,
     logo_activo, color_primario, color_secundario, color_acento,
     color_neutro_oscuro, color_neutro_medio, color_neutro_claro, activo, publicado)
VALUES
    (1, 'DentIA', 'DentIA', 'Gestion dental inteligente',
     'Plataforma interna para la gestion del consultorio dental.',
     1, '#0F766E', '#115E59', '#14B8A6', '#1F2937', '#64748B', '#F1F5F9', 1, 1)
ON DUPLICATE KEY UPDATE id_config = id_config;
