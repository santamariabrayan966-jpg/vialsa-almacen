-- DentIA - Área Clínica Integral e Interconectada
-- Migración aditiva para Historia Clínica, Odontograma, Diagnósticos, Tratamientos, Planes y Atenciones.


-- 1. Historia Clínica General (Anamnesis, Alertas Médicas y Antecedentes)
CREATE TABLE IF NOT EXISTS clinica_historias (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    alergias VARCHAR(500) NULL,
    enfermedades_sistemicas VARCHAR(500) NULL,
    medicacion_actual VARCHAR(500) NULL,
    hemorragias TINYINT(1) NOT NULL DEFAULT 0,
    embarazo TINYINT(1) NOT NULL DEFAULT 0,
    antecedentes_familiares VARCHAR(500) NULL,
    habitos VARCHAR(500) NULL,
    motivo_primera_consulta VARCHAR(1000) NULL,
    grupo_sanguineo VARCHAR(10) NULL,
    presion_arterial VARCHAR(20) NULL,
    observaciones_medicas VARCHAR(2000) NULL,
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_chist_paciente UNIQUE (paciente_id),
    CONSTRAINT fk_chist_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Odontogramas (Sesiones de Odontograma del Paciente: Inicial vs Evolución)
CREATE TABLE IF NOT EXISTS clinica_odontogramas (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL DEFAULT 'INICIAL',
    fecha DATE NOT NULL,
    observaciones VARCHAR(2000) NULL,
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_codo_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente) ON DELETE RESTRICT,
    CONSTRAINT chk_codo_tipo CHECK (tipo IN ('INICIAL', 'EVOLUCION')),
    INDEX idx_codo_paciente_fecha (paciente_id, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Hallazgos por Pieza y Superficie del Odontograma
CREATE TABLE IF NOT EXISTS clinica_odontograma_piezas (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    odontograma_id BIGINT NOT NULL,
    numero_pieza INT NOT NULL,
    superficie VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    condicion VARCHAR(50) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PATOLOGICO',
    notas VARCHAR(255) NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_copie_odontograma FOREIGN KEY (odontograma_id) REFERENCES clinica_odontogramas(id) ON DELETE CASCADE,
    CONSTRAINT chk_copie_estado CHECK (estado IN ('PATOLOGICO', 'REALIZADO', 'TEMPORAL', 'SANO')),
    INDEX idx_copie_odonto_pieza (odontograma_id, numero_pieza)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Catálogo de Diagnósticos Estomatológicos (Base CIE-10)
CREATE TABLE IF NOT EXISTS clinica_catalogo_diagnosticos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo_cie10 VARCHAR(20) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    descripcion VARCHAR(500) NULL,
    categoria VARCHAR(80) NOT NULL DEFAULT 'GENERAL',
    activo TINYINT(1) NOT NULL DEFAULT 1,
    UNIQUE KEY uk_ccd_codigo (codigo_cie10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Diagnósticos Registrados por Paciente
CREATE TABLE IF NOT EXISTS clinica_paciente_diagnosticos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    diagnostico_catalogo_id BIGINT NULL,
    codigo_cie10 VARCHAR(20) NULL,
    descripcion VARCHAR(250) NOT NULL,
    numero_pieza INT NULL,
    gravedad VARCHAR(20) NOT NULL DEFAULT 'MODERADA',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_diagnostico DATE NOT NULL,
    observaciones VARCHAR(1000) NULL,
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cpdiag_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente) ON DELETE RESTRICT,
    CONSTRAINT fk_cpdiag_catalogo FOREIGN KEY (diagnostico_catalogo_id) REFERENCES clinica_catalogo_diagnosticos(id) ON DELETE SET NULL,
    CONSTRAINT chk_cpdiag_estado CHECK (estado IN ('ACTIVO', 'EN_TRATAMIENTO', 'RESUELTO')),
    INDEX idx_cpdiag_paciente (paciente_id, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Catálogo Maestro de Tratamientos y Procedimientos Odontológicos
CREATE TABLE IF NOT EXISTS clinica_catalogo_tratamientos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NULL,
    nombre VARCHAR(200) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    descripcion VARCHAR(500) NULL,
    precio_sugerido DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    duracion_estimada_min INT NOT NULL DEFAULT 30,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    UNIQUE KEY uk_cct_codigo (codigo),
    INDEX idx_cct_categoria (categoria, activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Planes de Tratamiento del Paciente
CREATE TABLE IF NOT EXISTS clinica_planes_tratamiento (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    costo_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    presupuesto_id BIGINT NULL,
    notas VARCHAR(2000) NULL,
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cplan_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente) ON DELETE RESTRICT,
    CONSTRAINT fk_cplan_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES gestion_presupuestos(id) ON DELETE SET NULL,
    CONSTRAINT chk_cplan_estado CHECK (estado IN ('BORRADOR', 'PROPUESTO', 'APROBADO', 'EN_EJECUCION', 'COMPLETADO', 'CANCELADO')),
    INDEX idx_cplan_paciente_estado (paciente_id, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Items / Procedimientos del Plan de Tratamiento
CREATE TABLE IF NOT EXISTS clinica_plan_tratamiento_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    tratamiento_catalogo_id BIGINT NULL,
    diagnostico_id BIGINT NULL,
    numero_pieza INT NULL,
    descripcion VARCHAR(250) NOT NULL,
    precio DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    orden_fase INT NOT NULL DEFAULT 1,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    atencion_id BIGINT NULL,
    CONSTRAINT fk_cpi_plan FOREIGN KEY (plan_id) REFERENCES clinica_planes_tratamiento(id) ON DELETE CASCADE,
    CONSTRAINT fk_cpi_tratamiento FOREIGN KEY (tratamiento_catalogo_id) REFERENCES clinica_catalogo_tratamientos(id) ON DELETE SET NULL,
    CONSTRAINT fk_cpi_diagnostico FOREIGN KEY (diagnostico_id) REFERENCES clinica_paciente_diagnosticos(id) ON DELETE SET NULL,
    CONSTRAINT chk_cpi_estado CHECK (estado IN ('PENDIENTE', 'EN_CURSO', 'REALIZADO', 'CANCELADO')),
    INDEX idx_cpi_plan_fase (plan_id, orden_fase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Atenciones y Evoluciones Clínicas (Formato Médico SOEP)
CREATE TABLE IF NOT EXISTS clinica_atenciones (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NULL,
    cita_id BIGINT NULL,
    fecha_atencion DATETIME NOT NULL,
    motivo_consulta VARCHAR(500) NOT NULL,
    subjetivo VARCHAR(1500) NULL,
    objetivo VARCHAR(1500) NULL,
    evaluacion VARCHAR(1500) NULL,
    plan_procedimiento VARCHAR(2000) NOT NULL,
    piezas_tratadas VARCHAR(100) NULL,
    anestesia_utilizada VARCHAR(200) NULL,
    materiales VARCHAR(500) NULL,
    indicaciones_receta VARCHAR(2000) NULL,
    proxima_cita_sugerida VARCHAR(300) NULL,
    creado_por VARCHAR(100) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cat_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente) ON DELETE RESTRICT,
    CONSTRAINT fk_cat_profesional FOREIGN KEY (profesional_id) REFERENCES profesionales(id_profesional) ON DELETE SET NULL,
    CONSTRAINT fk_cat_cita FOREIGN KEY (cita_id) REFERENCES citas(id_cita) ON DELETE SET NULL,
    INDEX idx_cat_paciente_fecha (paciente_id, fecha_atencion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- CATÁLOGO BASE DE DIAGNÓSTICOS CIE-10 (Odontología)
-- Este script se ejecuta en cada arranque: solo añadir códigos ausentes.
-- Conservar los registros existentes, incluso sus ajustes locales e inactivación.
INSERT INTO clinica_catalogo_diagnosticos (codigo_cie10, nombre, descripcion, categoria, activo) VALUES
    ('K02.0', 'Caries limitada al esmalte', 'Mancha blanca o desmineralización del esmalte dental.', 'CARIES', 1),
    ('K02.1', 'Caries de la dentina', 'Lesión cariosa con compromiso de tejido dentinario.', 'CARIES', 1),
    ('K02.2', 'Caries del cemento', 'Caries radicular en superficie cementaria expuesta.', 'CARIES', 1),
    ('K02.5', 'Caries con compromiso pulpar', 'Caries profunda con exposición o proximidad pulpar.', 'CARIES', 1),
    ('K03.0', 'Atrición dental excesiva', 'Desgaste fisiológico o por fricción oclusal.', 'DESGASTE', 1),
    ('K03.1', 'Abrasión dental', 'Desgaste por factores mecánicos externos (cepillado traumático).', 'DESGASTE', 1),
    ('K03.2', 'Erosión dental', 'Desgaste químico por ácidos extrínsecos o intrínsecos.', 'DESGASTE', 1),
    ('K04.0', 'Pulpitis reversible / irreversible', 'Inflamación pulpar con sintomatología dolorosa.', 'ENDODONCIA', 1),
    ('K04.1', 'Necrosis de la pulpa', 'Muerte del tejido pulpar asintomática o dolorosa.', 'ENDODONCIA', 1),
    ('K04.7', 'Absceso periapical sin fístula', 'Infección aguda periapical purulenta.', 'ENDODONCIA', 1),
    ('K05.0', 'Gingivitis aguda', 'Inflamación y sangrado gingival agudo.', 'PERIODONCIA', 1),
    ('K05.1', 'Gingivitis crónica marginal', 'Inflamación persistente de encías por biopelícula y cálculo.', 'PERIODONCIA', 1),
    ('K05.3', 'Periodontitis crónica', 'Pérdida de inserción clínica y soporte óseo alveolar.', 'PERIODONCIA', 1),
    ('K07.2', 'Anomalías en la relación de las arcadas', 'Maloclusión Clase I, II o III de Angle.', 'ORTODONCIA', 1),
    ('K07.4', 'Maloclusión no especificada', 'Apiñamiento, diastemas o mordida cruzada/abierta.', 'ORTODONCIA', 1),
    ('K08.1', 'Pérdida de dientes por caries o trauma', 'Ausencia de pieza dental (edentulismo parcial o total).', 'REHABILITACION', 1)
ON DUPLICATE KEY UPDATE id = clinica_catalogo_diagnosticos.id;

-- CATÁLOGO DE TRATAMIENTOS: Se inicializa exclusivamente con el tarifario oficial Vasquez en V2026_09_21_01 y V2026_09_23_01.

