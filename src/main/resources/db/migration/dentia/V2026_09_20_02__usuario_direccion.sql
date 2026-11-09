-- La consulta DNI puede devolver el domicilio. Se conserva para reutilizarlo
-- en profesionales y en la ficha de usuario.
SET @dentia_usuario_direccion = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE usuarios ADD COLUMN direccion VARCHAR(255) NULL AFTER Telefono',
              'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'usuarios'
      AND COLUMN_NAME = 'direccion'
);
PREPARE dentia_stmt FROM @dentia_usuario_direccion;
EXECUTE dentia_stmt;
DEALLOCATE PREPARE dentia_stmt;
