-- Clasificación visible y vínculo opcional con el identificador legado de citas.
-- Los tipos de atención anteriores se conservan para las citas históricas.
CREATE TABLE IF NOT EXISTS clinica_servicio_detalles (
    tratamiento_id BIGINT NOT NULL PRIMARY KEY,
    subcategoria VARCHAR(80) NOT NULL DEFAULT 'General',
    tipo_atencion_id BIGINT NULL,
    UNIQUE KEY uk_servicio_tipo_atencion (tipo_atencion_id),
    CONSTRAINT fk_servicio_detalle_tratamiento FOREIGN KEY (tratamiento_id)
        REFERENCES clinica_catalogo_tratamientos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Los servicios de catálogo corresponden exclusivamente a la hoja de referencia Vasquez.


INSERT INTO clinica_servicio_detalles (tratamiento_id, subcategoria)
SELECT id, CASE
    WHEN codigo LIKE 'VZ-PM-%' THEN 'Prótesis móviles'
    WHEN codigo LIKE 'VZ-PF-%' THEN 'Prótesis fijas'
    WHEN codigo LIKE 'VZ-PRT-%' THEN 'Reparaciones'
    WHEN codigo LIKE 'VZ-RE-%' THEN 'Restauraciones'
    WHEN codigo LIKE 'VZ-EN-%' THEN 'Por tipo de diente'
    WHEN codigo LIKE 'VZ-CI-%' THEN 'Extracciones y cirugía'
    WHEN codigo LIKE 'VZ-OR-%' THEN 'Brackets'
    WHEN codigo LIKE 'VZ-PR-%' THEN 'Higiene y prevención'
    WHEN categoria = 'REHABILITACION' THEN 'Prótesis y rehabilitación'
    WHEN categoria = 'ENDODONCIA' THEN 'Conductos'
    WHEN categoria = 'CIRUGIA' THEN 'Extracciones y cirugía'
    WHEN categoria = 'PREVENTIVA' THEN 'Higiene y prevención'
    WHEN categoria = 'EVALUACION' THEN 'Consultas y controles'
    ELSE 'Otros procedimientos'
END
FROM clinica_catalogo_tratamientos
ON DUPLICATE KEY UPDATE tratamiento_id = clinica_servicio_detalles.tratamiento_id;

-- La foto entregada no indica tiempos. Los importes se cargan una sola vez como
-- referencia editable y nunca vuelven a sobrescribir cambios hechos por el consultorio.
CREATE TABLE IF NOT EXISTS clinica_catalogo_importaciones (
    clave VARCHAR(80) NOT NULL PRIMARY KEY,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE clinica_catalogo_tratamientos
SET precio_sugerido = CASE codigo
    WHEN 'VZ-PM-01' THEN 750 WHEN 'VZ-PM-02' THEN 800
    WHEN 'VZ-PM-03' THEN 400 WHEN 'VZ-PM-04' THEN 350
    WHEN 'VZ-PM-05' THEN 100 WHEN 'VZ-PM-06' THEN 280
    WHEN 'VZ-PM-07' THEN 450 WHEN 'VZ-PM-08' THEN 580
    WHEN 'VZ-PM-09' THEN 800
    WHEN 'VZ-PF-01' THEN 800 WHEN 'VZ-PF-02' THEN 350
    WHEN 'VZ-PF-03' THEN 250 WHEN 'VZ-PF-04' THEN 150
    WHEN 'VZ-RE-01' THEN 170 WHEN 'VZ-RE-02' THEN 50
    WHEN 'VZ-RE-03' THEN 30
    WHEN 'VZ-EN-01' THEN 200 WHEN 'VZ-EN-02' THEN 250
    WHEN 'VZ-EN-03' THEN 500 WHEN 'VZ-EN-04' THEN 120
    WHEN 'VZ-CI-01' THEN 40 WHEN 'VZ-CI-02' THEN 80
    WHEN 'VZ-CI-03' THEN 150 WHEN 'VZ-CI-04' THEN 60
    WHEN 'VZ-PR-01' THEN 90 WHEN 'VZ-PR-02' THEN 20
    WHEN 'VZ-OR-01' THEN 700 WHEN 'VZ-OR-02' THEN 100
    WHEN 'VZ-PRT-01' THEN 30 WHEN 'VZ-PRT-02' THEN 20
    ELSE precio_sugerido END
WHERE codigo LIKE 'VZ-%' AND precio_sugerido = 0
  AND NOT EXISTS (SELECT 1 FROM clinica_catalogo_importaciones WHERE clave = 'precios_vasquez_foto_2026_09');

INSERT IGNORE INTO clinica_catalogo_importaciones (clave) VALUES ('precios_vasquez_foto_2026_09');
