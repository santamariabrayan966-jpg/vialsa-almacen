-- DentIA - Fase 1: identidad del paciente y su cuenta de acceso.
-- Script aditivo. No elimina ni modifica tablas comerciales heredadas.

CREATE TABLE IF NOT EXISTS pacientes (
    id_paciente BIGINT NOT NULL AUTO_INCREMENT,
    dni CHAR(8) NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    celular VARCHAR(20) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_paciente),
    UNIQUE KEY uk_pacientes_dni (dni),
    KEY idx_pacientes_apellidos_nombres (apellidos, nombres),
    CONSTRAINT chk_pacientes_dni CHECK (dni IS NULL OR dni REGEXP '^[0-9]{8}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cuentas_paciente (
    id_cuenta_paciente BIGINT NOT NULL AUTO_INCREMENT,
    id_paciente BIGINT NOT NULL,
    correo VARCHAR(191) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso DATETIME NULL,
    PRIMARY KEY (id_cuenta_paciente),
    UNIQUE KEY uk_cuentas_paciente_id_paciente (id_paciente),
    UNIQUE KEY uk_cuentas_paciente_correo (correo),
    CONSTRAINT fk_cuenta_paciente_paciente
        FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
