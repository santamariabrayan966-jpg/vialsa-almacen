USE dentia;

-- DentIA Fase 2: profesionales, disponibilidad y citas.
-- Migracion aditiva para un unico consultorio.

CREATE TABLE IF NOT EXISTS especialidades (
    id_especialidad BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_especialidad),
    UNIQUE KEY uk_especialidades_nombre (nombre),
    KEY idx_especialidades_activo_nombre (activo, nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS profesionales (
    id_profesional BIGINT NOT NULL AUTO_INCREMENT,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    documento VARCHAR(30) NULL,
    numero_colegiatura VARCHAR(60) NULL,
    telefono VARCHAR(30) NULL,
    correo VARCHAR(191) NULL,
    foto VARCHAR(500) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    id_usuario INT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_profesional),
    UNIQUE KEY uk_profesionales_documento (documento),
    UNIQUE KEY uk_profesionales_colegiatura (numero_colegiatura),
    UNIQUE KEY uk_profesionales_usuario (id_usuario),
    KEY idx_profesionales_activo_apellidos (activo, apellidos, nombres),
    CONSTRAINT fk_profesionales_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (idUsuario)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS profesional_especialidades (
    id_profesional BIGINT NOT NULL,
    id_especialidad BIGINT NOT NULL,
    fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_profesional, id_especialidad),
    KEY idx_prof_especialidad_especialidad (id_especialidad, id_profesional),
    CONSTRAINT fk_prof_especialidad_profesional
        FOREIGN KEY (id_profesional) REFERENCES profesionales (id_profesional)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_prof_especialidad_especialidad
        FOREIGN KEY (id_especialidad) REFERENCES especialidades (id_especialidad)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS profesional_horarios (
    id_horario BIGINT NOT NULL AUTO_INCREMENT,
    id_profesional BIGINT NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL COMMENT 'ISO-8601: lunes=1, domingo=7',
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_horario),
    UNIQUE KEY uk_horario_profesional_bloque (id_profesional, dia_semana, hora_inicio, hora_fin),
    KEY idx_horario_disponibilidad (id_profesional, dia_semana, activo, hora_inicio, hora_fin),
    CONSTRAINT chk_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horario_rango CHECK (hora_inicio < hora_fin),
    CONSTRAINT fk_horario_profesional
        FOREIGN KEY (id_profesional) REFERENCES profesionales (id_profesional)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bloqueos_agenda (
    id_bloqueo BIGINT NOT NULL AUTO_INCREMENT,
    id_profesional BIGINT NOT NULL,
    fecha_hora_inicio DATETIME NOT NULL,
    fecha_hora_fin DATETIME NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    observacion VARCHAR(500) NULL,
    creado_por INT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_bloqueo),
    KEY idx_bloqueo_profesional_intervalo (id_profesional, fecha_hora_inicio, fecha_hora_fin),
    KEY idx_bloqueo_inicio_fin (fecha_hora_inicio, fecha_hora_fin),
    CONSTRAINT chk_bloqueo_rango CHECK (fecha_hora_inicio < fecha_hora_fin),
    CONSTRAINT chk_bloqueo_motivo CHECK (motivo IN ('VACACIONES','PERMISO','DESCANSO','CAPACITACION','EMERGENCIA','OTRO')),
    CONSTRAINT fk_bloqueo_profesional
        FOREIGN KEY (id_profesional) REFERENCES profesionales (id_profesional)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_bloqueo_usuario
        FOREIGN KEY (creado_por) REFERENCES usuarios (idUsuario)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tipos_atencion (
    id_tipo_atencion BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    duracion_minutos SMALLINT UNSIGNED NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_tipo_atencion),
    UNIQUE KEY uk_tipos_atencion_nombre (nombre),
    KEY idx_tipos_atencion_activo_nombre (activo, nombre),
    CONSTRAINT chk_tipo_atencion_duracion CHECK (duracion_minutos BETWEEN 5 AND 480)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS citas (
    id_cita BIGINT NOT NULL AUTO_INCREMENT,
    id_paciente BIGINT NOT NULL,
    id_profesional BIGINT NOT NULL,
    id_tipo_atencion BIGINT NOT NULL,
    fecha_hora_inicio DATETIME NOT NULL,
    fecha_hora_fin DATETIME NOT NULL,
    motivo_consulta VARCHAR(1000) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'RESERVADA',
    origen VARCHAR(30) NOT NULL,
    id_cita_origen BIGINT NULL,
    creado_por INT NULL,
    fecha_cancelacion DATETIME NULL,
    motivo_cancelacion VARCHAR(500) NULL,
    cancelado_por_usuario INT NULL,
    cancelado_por_paciente BIGINT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_cita),
    KEY idx_citas_profesional_fecha (id_profesional, fecha_hora_inicio, fecha_hora_fin, estado),
    KEY idx_citas_paciente_fecha (id_paciente, fecha_hora_inicio, fecha_hora_fin, estado),
    KEY idx_citas_fecha_estado (fecha_hora_inicio, estado),
    KEY idx_citas_tipo_fecha (id_tipo_atencion, fecha_hora_inicio),
    KEY idx_citas_origen (id_cita_origen),
    CONSTRAINT chk_cita_rango CHECK (fecha_hora_inicio < fecha_hora_fin),
    CONSTRAINT chk_cita_estado CHECK (estado IN ('RESERVADA','CONFIRMADA','EN_ESPERA','EN_ATENCION','ATENDIDA','CANCELADA','NO_ASISTIO')),
    CONSTRAINT chk_cita_origen CHECK (origen IN ('PORTAL_PACIENTE','PANEL_INTERNO')),
    CONSTRAINT fk_cita_paciente FOREIGN KEY (id_paciente) REFERENCES pacientes (id_paciente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cita_profesional FOREIGN KEY (id_profesional) REFERENCES profesionales (id_profesional) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cita_tipo FOREIGN KEY (id_tipo_atencion) REFERENCES tipos_atencion (id_tipo_atencion) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cita_origen_cita FOREIGN KEY (id_cita_origen) REFERENCES citas (id_cita) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cita_creado_por FOREIGN KEY (creado_por) REFERENCES usuarios (idUsuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cita_cancelado_usuario FOREIGN KEY (cancelado_por_usuario) REFERENCES usuarios (idUsuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cita_cancelado_paciente FOREIGN KEY (cancelado_por_paciente) REFERENCES pacientes (id_paciente) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cita_historial (
    id_historial BIGINT NOT NULL AUTO_INCREMENT,
    id_cita BIGINT NOT NULL,
    estado_anterior VARCHAR(30) NULL,
    estado_nuevo VARCHAR(30) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_responsable INT NULL,
    id_paciente_responsable BIGINT NULL,
    observacion VARCHAR(500) NULL,
    tipo_evento VARCHAR(30) NOT NULL,
    PRIMARY KEY (id_historial),
    KEY idx_historial_cita_fecha (id_cita, fecha),
    KEY idx_historial_evento_fecha (tipo_evento, fecha),
    CONSTRAINT chk_historial_evento CHECK (tipo_evento IN ('CREACION','CONFIRMACION','CAMBIO_ESTADO','CANCELACION','REPROGRAMACION')),
    CONSTRAINT fk_historial_cita FOREIGN KEY (id_cita) REFERENCES citas (id_cita) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario_responsable) REFERENCES usuarios (idUsuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_historial_paciente FOREIGN KEY (id_paciente_responsable) REFERENCES pacientes (id_paciente) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Filas tecnicas creadas bajo demanda y bloqueadas con SELECT ... FOR UPDATE.
-- Serializan reservas del mismo profesional o paciente durante un dia.
CREATE TABLE IF NOT EXISTS agenda_reserva_locks (
    tipo_entidad VARCHAR(20) NOT NULL,
    id_entidad BIGINT NOT NULL,
    fecha DATE NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tipo_entidad, id_entidad, fecha),
    CONSTRAINT chk_agenda_lock_tipo CHECK (tipo_entidad IN ('PACIENTE','PROFESIONAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO especialidades (nombre, descripcion, activo) VALUES
    ('Odontologia General', 'Atencion odontologica integral y preventiva.', 1),
    ('Ortodoncia', 'Diagnostico y correccion de posicion dental y maxilar.', 1),
    ('Endodoncia', 'Diagnostico y tratamiento del tejido pulpar.', 1),
    ('Periodoncia', 'Prevencion y tratamiento de encias y tejidos de soporte.', 1),
    ('Cirugia Oral', 'Procedimientos quirurgicos de la cavidad oral.', 1),
    ('Odontopediatria', 'Atencion odontologica de ninos y adolescentes.', 1),
    ('Rehabilitacion Oral', 'Recuperacion funcional y estetica de la denticion.', 1)
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

INSERT INTO tipos_atencion (nombre, descripcion, duracion_minutos, activo) VALUES
    ('Consulta general', 'Consulta odontologica general.', 30, 1),
    ('Evaluacion', 'Evaluacion inicial o especializada.', 30, 1),
    ('Control', 'Control posterior a una atencion.', 20, 1),
    ('Emergencia', 'Evaluacion prioritaria por urgencia odontologica.', 30, 1),
    ('Limpieza', 'Atencion preventiva de higiene dental.', 45, 1)
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);
