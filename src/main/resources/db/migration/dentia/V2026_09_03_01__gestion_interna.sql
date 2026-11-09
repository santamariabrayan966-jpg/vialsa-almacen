-- Migración aditiva. Ejecutar sobre la base de DentIA antes de habilitar Gestión.
-- No modifica pacientes, citas, usuarios ni tablas comerciales heredadas.
CREATE TABLE IF NOT EXISTS gestion_presupuestos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    fecha DATE NOT NULL,
    vence DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    total DECIMAL(12,2) NOT NULL,
    notas VARCHAR(2000) NOT NULL DEFAULT '',
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gpres_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente),
    CONSTRAINT chk_gpres_total CHECK (total > 0),
    CONSTRAINT chk_gpres_estado CHECK (estado IN ('BORRADOR','APROBADO','RECHAZADO','ANULADO')),
    INDEX idx_gpres_paciente_estado (paciente_id,estado), INDEX idx_gpres_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gestion_presupuesto_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    presupuesto_id BIGINT NOT NULL,
    descripcion VARCHAR(250) NOT NULL,
    cantidad INT NOT NULL,
    precio DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_gitem_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES gestion_presupuestos(id),
    CONSTRAINT chk_gitem_valores CHECK (cantidad BETWEEN 1 AND 1000 AND precio > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gestion_pagos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    presupuesto_id BIGINT NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    fecha DATE NOT NULL,
    metodo VARCHAR(20) NOT NULL,
    referencia VARCHAR(120) NOT NULL DEFAULT '',
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADO',
    token VARCHAR(36) NOT NULL UNIQUE,
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    anulado_por VARCHAR(100) NULL,
    anulado_en DATETIME NULL,
    motivo_anulacion VARCHAR(1000) NULL,
    CONSTRAINT fk_gpago_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES gestion_presupuestos(id),
    CONSTRAINT chk_gpago_monto CHECK (monto > 0),
    CONSTRAINT chk_gpago_estado CHECK (estado IN ('REGISTRADO','ANULADO')),
    INDEX idx_gpago_fecha_estado (fecha,estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gestion_seguimientos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    cita_id BIGINT NULL,
    presupuesto_id BIGINT NULL,
    titulo VARCHAR(180) NOT NULL,
    fecha DATETIME NOT NULL,
    canal VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    notas VARCHAR(2000) NOT NULL DEFAULT '',
    resultado VARCHAR(2000) NULL,
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cerrado_por VARCHAR(100) NULL,
    cerrado_en DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gseg_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente),
    CONSTRAINT fk_gseg_cita FOREIGN KEY (cita_id) REFERENCES citas(id_cita),
    CONSTRAINT fk_gseg_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES gestion_presupuestos(id),
    CONSTRAINT chk_gseg_estado CHECK (estado IN ('PENDIENTE','COMPLETADO','CANCELADO')),
    INDEX idx_gseg_fecha_estado (fecha,estado), INDEX idx_gseg_paciente (paciente_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gestion_documentos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    cita_id BIGINT NULL,
    presupuesto_id BIGINT NULL,
    titulo VARCHAR(180) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    nombre_archivo VARCHAR(200) NOT NULL,
    mime VARCHAR(80) NOT NULL,
    tamano INT NOT NULL,
    contenido MEDIUMBLOB NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gdoc_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente),
    CONSTRAINT fk_gdoc_cita FOREIGN KEY (cita_id) REFERENCES citas(id_cita),
    CONSTRAINT fk_gdoc_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES gestion_presupuestos(id),
    CONSTRAINT chk_gdoc_estado CHECK (estado IN ('ACTIVO','ARCHIVADO')),
    INDEX idx_gdoc_paciente_fecha (paciente_id,creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gestion_historial (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    modulo VARCHAR(30) NOT NULL,
    registro_id BIGINT NOT NULL,
    accion VARCHAR(50) NOT NULL,
    detalle VARCHAR(2000) NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ghist_registro (modulo,registro_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
