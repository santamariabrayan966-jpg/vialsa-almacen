-- Datos administrativos del paciente obtenidos o completados durante su registro.
SET @dentia_paciente_direccion = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE pacientes ADD COLUMN direccion VARCHAR(255) NULL AFTER fecha_nacimiento',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pacientes'
      AND COLUMN_NAME = 'direccion'
);
PREPARE dentia_stmt FROM @dentia_paciente_direccion;
EXECUTE dentia_stmt;
DEALLOCATE PREPARE dentia_stmt;

ALTER TABLE pacientes MODIFY COLUMN celular VARCHAR(20) NULL;
