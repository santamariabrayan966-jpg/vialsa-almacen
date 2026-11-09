USE dentia;

-- DentIA: cierre de la base administrativa para un unico consultorio.
-- Migracion aditiva. No crea informacion clinica ni estructuras comerciales.

ALTER TABLE marca_identidad_config
    ADD COLUMN horario_general VARCHAR(500) NULL AFTER direccion_principal,
    ADD COLUMN nombre_documentos VARCHAR(180) NULL AFTER horario_general,
    ADD COLUMN pie_documentos VARCHAR(1000) NULL AFTER nombre_documentos;

ALTER TABLE pacientes
    ADD COLUMN correo VARCHAR(180) NULL AFTER celular,
    ADD KEY idx_pacientes_busqueda_celular (celular),
    ADD KEY idx_pacientes_busqueda_nombre (apellidos, nombres);

UPDATE marca_identidad_config
SET nombre_documentos = COALESCE(NULLIF(nombre_documentos, ''), nombre_comercial),
    horario_general = COALESCE(horario_general, ''),
    pie_documentos = COALESCE(pie_documentos, '')
WHERE id_config = 1;
