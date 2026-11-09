-- Esquema aditivo: geometría ilustrativa por odontograma e historial de cambios.
CREATE TABLE IF NOT EXISTS clinica_odontograma_versiones (
    odontograma_id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cov_odonto FOREIGN KEY (odontograma_id) REFERENCES clinica_odontogramas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS clinica_odontograma_posiciones (
    odontograma_id BIGINT NOT NULL,
    numero_pieza INT NOT NULL,
    inclinacion DECIMAL(6,2) NOT NULL DEFAULT 0,
    actualizado_por VARCHAR(100) NOT NULL,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (odontograma_id, numero_pieza),
    CONSTRAINT fk_copos_odonto FOREIGN KEY (odontograma_id) REFERENCES clinica_odontogramas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS clinica_odontograma_historial (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    odontograma_id BIGINT NOT NULL,
    numero_pieza INT NOT NULL,
    superficie VARCHAR(20) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    condicion VARCHAR(50) NULL,
    notas VARCHAR(255) NULL,
    usuario VARCHAR(100) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coh_odonto FOREIGN KEY (odontograma_id) REFERENCES clinica_odontogramas(id),
    INDEX idx_coh_pieza (odontograma_id, numero_pieza, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS clinica_atencion_borradores (
    paciente_id BIGINT NOT NULL PRIMARY KEY,
    contenido LONGTEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    actualizado_por VARCHAR(100) NOT NULL,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cab_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
