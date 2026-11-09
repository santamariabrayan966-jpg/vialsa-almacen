-- Catálogo exclusivo del tarifario oficial de la hoja de referencia Vasquez Consultorio Dental.
-- Se eliminan todos los tratamientos y procedimientos que no corresponden a la hoja física de precios.

DELETE FROM clinica_catalogo_tratamientos WHERE codigo NOT LIKE 'VZ-%';

-- Actualización de nombres exactos, categoría y precios según la hoja oficial
UPDATE clinica_catalogo_tratamientos SET
    nombre = CASE codigo
        WHEN 'VZ-PM-01' THEN 'Prótesis totales sin malla'
        WHEN 'VZ-PM-02' THEN 'Prótesis totales con malla'
        WHEN 'VZ-PM-03' THEN '1 prótesis total Sup. O Inf.'
        WHEN 'VZ-PM-04' THEN '1 prótesis parcial acrílica'
        WHEN 'VZ-PM-05' THEN 'Prótesis acrílica 1 diente provisional'
        WHEN 'VZ-PM-06' THEN 'Prótesis provisional'
        WHEN 'VZ-PM-07' THEN '1 prótesis sup. Con Malla'
        WHEN 'VZ-PM-08' THEN '1 Prótesis parcial base metálica'
        WHEN 'VZ-PM-09' THEN '1 prótesis flexible'
        WHEN 'VZ-PF-01' THEN 'Circonio por pieza'
        WHEN 'VZ-PF-02' THEN 'Porcelana por pieza'
        WHEN 'VZ-PF-03' THEN 'Ivocron por pieza'
        WHEN 'VZ-PF-04' THEN 'Acrílica por pieza'
        WHEN 'VZ-RE-01' THEN 'Carillas'
        WHEN 'VZ-RE-02' THEN 'Resinas'
        WHEN 'VZ-RE-03' THEN 'Provisionales'
        WHEN 'VZ-EN-01' THEN 'Centrales'
        WHEN 'VZ-EN-02' THEN 'Premolares'
        WHEN 'VZ-EN-03' THEN 'Molares'
        WHEN 'VZ-EN-04' THEN 'Perno'
        WHEN 'VZ-CI-01' THEN 'Extracción simple'
        WHEN 'VZ-CI-02' THEN '3er Molar superior'
        WHEN 'VZ-CI-03' THEN '3er Molar inferior (desde)'
        WHEN 'VZ-CI-04' THEN 'Operculectomía'
        WHEN 'VZ-PR-01' THEN 'Profilaxis'
        WHEN 'VZ-PR-02' THEN 'Fluorización'
        WHEN 'VZ-OR-01' THEN 'Instalación de brackets'
        WHEN 'VZ-OR-02' THEN 'Control mensual de brackets'
        WHEN 'VZ-PRT-01' THEN 'Pegar prótesis movible'
        WHEN 'VZ-PRT-02' THEN 'Pegar prótesis fija 1 diente'
        ELSE nombre
    END,
    categoria = CASE
        WHEN codigo = 'VZ-RE-01' THEN 'OPERATORIA'
        ELSE categoria
    END,
    precio_sugerido = CASE codigo
        WHEN 'VZ-PM-01' THEN 750.00
        WHEN 'VZ-PM-02' THEN 800.00
        WHEN 'VZ-PM-03' THEN 400.00
        WHEN 'VZ-PM-04' THEN 350.00
        WHEN 'VZ-PM-05' THEN 100.00
        WHEN 'VZ-PM-06' THEN 280.00
        WHEN 'VZ-PM-07' THEN 450.00
        WHEN 'VZ-PM-08' THEN 580.00
        WHEN 'VZ-PM-09' THEN 800.00
        WHEN 'VZ-PF-01' THEN 800.00
        WHEN 'VZ-PF-02' THEN 350.00
        WHEN 'VZ-PF-03' THEN 250.00
        WHEN 'VZ-PF-04' THEN 150.00
        WHEN 'VZ-RE-01' THEN 170.00
        WHEN 'VZ-RE-02' THEN 50.00
        WHEN 'VZ-RE-03' THEN 30.00
        WHEN 'VZ-EN-01' THEN 200.00
        WHEN 'VZ-EN-02' THEN 250.00
        WHEN 'VZ-EN-03' THEN 500.00
        WHEN 'VZ-EN-04' THEN 120.00
        WHEN 'VZ-CI-01' THEN 40.00
        WHEN 'VZ-CI-02' THEN 80.00
        WHEN 'VZ-CI-03' THEN 150.00
        WHEN 'VZ-CI-04' THEN 60.00
        WHEN 'VZ-PR-01' THEN 90.00
        WHEN 'VZ-PR-02' THEN 20.00
        WHEN 'VZ-OR-01' THEN 700.00
        WHEN 'VZ-OR-02' THEN 100.00
        WHEN 'VZ-PRT-01' THEN 30.00
        WHEN 'VZ-PRT-02' THEN 20.00
        ELSE precio_sugerido
    END
WHERE codigo LIKE 'VZ-%';

-- Actualización de subcategorías para que la visualización por pestañas sea fiel a la hoja
UPDATE clinica_servicio_detalles d
JOIN clinica_catalogo_tratamientos t ON t.id = d.tratamiento_id
SET d.subcategoria = CASE
    WHEN t.codigo LIKE 'VZ-PM-%' THEN 'Prótesis movibles'
    WHEN t.codigo LIKE 'VZ-PF-%' THEN 'Prótesis fija'
    WHEN t.codigo LIKE 'VZ-PRT-%' THEN 'Reparación / Pegado'
    WHEN t.codigo LIKE 'VZ-RE-%' THEN 'Restauraciones'
    WHEN t.codigo LIKE 'VZ-EN-%' THEN 'Endodoncias'
    WHEN t.codigo LIKE 'VZ-CI-%' THEN 'Cirugías'
    WHEN t.codigo LIKE 'VZ-PR-%' THEN 'Higiene y prevención'
    WHEN t.codigo LIKE 'VZ-OR-%' THEN 'Brackets'
    ELSE 'General'
END
WHERE t.codigo LIKE 'VZ-%';
