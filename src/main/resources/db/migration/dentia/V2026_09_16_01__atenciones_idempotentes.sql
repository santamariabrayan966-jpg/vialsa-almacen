-- Recibo persistente de cierre; no elimina ni modifica atenciones previas.
CREATE TABLE IF NOT EXISTS clinica_atencion_solicitudes (
    paciente_id BIGINT NOT NULL,
    solicitud_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    atencion_id BIGINT NOT NULL,
    huella CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (paciente_id, solicitud_id),
    CONSTRAINT fk_cas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id_paciente),
    CONSTRAINT fk_cas_atencion FOREIGN KEY (atencion_id) REFERENCES clinica_atenciones(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
