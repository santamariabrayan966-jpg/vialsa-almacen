-- Procedimientos del tarifario aportado como referencia por el consultorio.
-- Importes iniciales de referencia; los precios existentes y personalizados se conservan.
-- Los códigos VZ permiten repetir esta inicialización sin duplicar ni sobrescribir precios editados.
INSERT INTO clinica_catalogo_tratamientos
    (codigo, nombre, categoria, descripcion, precio_sugerido, duracion_estimada_min, activo)
VALUES
    ('VZ-PM-01', 'Prótesis total sin malla', 'REHABILITACION', 'Prótesis removible; confirmar indicación y precio.', 750, 60, 1),
    ('VZ-PM-02', 'Prótesis total con malla', 'REHABILITACION', 'Prótesis removible reforzada; confirmar precio.', 800, 60, 1),
    ('VZ-PM-03', 'Prótesis total superior o inferior', 'REHABILITACION', 'Prótesis total por arcada; confirmar precio.', 400, 60, 1),
    ('VZ-PM-04', 'Prótesis parcial acrílica', 'REHABILITACION', 'Prótesis removible parcial; confirmar precio.', 350, 60, 1),
    ('VZ-PM-05', 'Prótesis acrílica provisional de un diente', 'REHABILITACION', 'Pieza acrílica provisional; confirmar precio.', 100, 45, 1),
    ('VZ-PM-06', 'Prótesis provisional', 'REHABILITACION', 'Prótesis temporal; confirmar precio.', 280, 45, 1),
    ('VZ-PM-07', 'Prótesis superior con malla', 'REHABILITACION', 'Prótesis superior reforzada; confirmar precio.', 450, 60, 1),
    ('VZ-PM-08', 'Prótesis parcial de base metálica', 'REHABILITACION', 'Prótesis parcial con estructura metálica; confirmar precio.', 580, 60, 1),
    ('VZ-PM-09', 'Prótesis flexible', 'REHABILITACION', 'Prótesis removible flexible; confirmar precio.', 800, 60, 1),
    ('VZ-PF-01', 'Prótesis fija de zirconio por pieza', 'REHABILITACION', 'Restauración fija de zirconio; confirmar precio.', 800, 60, 1),
    ('VZ-PF-02', 'Prótesis fija de porcelana por pieza', 'REHABILITACION', 'Restauración fija de porcelana; confirmar precio.', 350, 60, 1),
    ('VZ-PF-03', 'Prótesis fija de Ivocron por pieza', 'REHABILITACION', 'Restauración fija de Ivocron; confirmar precio.', 250, 60, 1),
    ('VZ-PF-04', 'Prótesis fija acrílica por pieza', 'REHABILITACION', 'Restauración fija acrílica; confirmar precio.', 150, 60, 1),
    ('VZ-RE-01', 'Carilla dental', 'ESTETICA', 'Restauración estética tipo carilla; confirmar precio.', 170, 45, 1),
    ('VZ-RE-02', 'Restauración con resina', 'OPERATORIA', 'Restauración dental con resina; confirmar precio.', 50, 45, 1),
    ('VZ-RE-03', 'Restauración provisional', 'OPERATORIA', 'Restauración temporal; confirmar precio.', 30, 30, 1),
    ('VZ-EN-01', 'Endodoncia de diente central', 'ENDODONCIA', 'Tratamiento de conducto en incisivo central; confirmar precio.', 200, 60, 1),
    ('VZ-EN-02', 'Endodoncia de premolar', 'ENDODONCIA', 'Tratamiento de conducto en premolar; confirmar precio.', 250, 75, 1),
    ('VZ-EN-03', 'Endodoncia de molar', 'ENDODONCIA', 'Tratamiento de conducto en molar; confirmar precio.', 500, 90, 1),
    ('VZ-EN-04', 'Perno dental', 'ENDODONCIA', 'Colocación de perno; confirmar indicación y precio.', 120, 45, 1),
    ('VZ-CI-01', 'Extracción dental simple', 'CIRUGIA', 'Extracción simple; confirmar precio.', 40, 30, 1),
    ('VZ-CI-02', 'Extracción de tercer molar superior', 'CIRUGIA', 'Extracción de cordal superior; confirmar precio.', 80, 60, 1),
    ('VZ-CI-03', 'Extracción de tercer molar inferior', 'CIRUGIA', 'Extracción de cordal inferior; confirmar precio.', 150, 60, 1),
    ('VZ-CI-04', 'Operculectomía', 'CIRUGIA', 'Retiro quirúrgico de opérculo; confirmar precio.', 60, 45, 1),
    ('VZ-PR-01', 'Profilaxis dental', 'PREVENTIVA', 'Limpieza preventiva; confirmar precio.', 90, 30, 1),
    ('VZ-PR-02', 'Fluorización', 'PREVENTIVA', 'Aplicación tópica de flúor; confirmar precio.', 20, 20, 1),
    ('VZ-OR-01', 'Instalación de brackets', 'ORTODONCIA', 'Inicio de ortodoncia fija; confirmar precio inicial.', 700, 90, 1),
    ('VZ-OR-02', 'Control mensual de brackets', 'ORTODONCIA', 'Control y ajuste de ortodoncia; confirmar precio.', 100, 30, 1),
    ('VZ-PRT-01', 'Pegar prótesis móvil', 'REHABILITACION', 'Reparación o fijación de prótesis móvil; confirmar precio.', 30, 30, 1),
    ('VZ-PRT-02', 'Pegar prótesis fija de un diente', 'REHABILITACION', 'Fijación de prótesis fija; confirmar precio.', 20, 30, 1)
ON DUPLICATE KEY UPDATE id = id;
