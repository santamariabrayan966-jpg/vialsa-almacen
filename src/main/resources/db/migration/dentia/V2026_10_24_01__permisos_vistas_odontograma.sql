-- Conservar el acceso 2D existente; 3D requiere habilitación explícita por rol.
INSERT INTO permisos (idRol, modulo, puedeAcceder)
SELECT p.idRol, 'odontograma_2d', MAX(p.puedeAcceder) FROM permisos p
WHERE p.modulo='odontograma'
AND NOT EXISTS (SELECT 1 FROM permisos v WHERE v.idRol=p.idRol AND v.modulo='odontograma_2d')
GROUP BY p.idRol;
INSERT INTO permisos (idRol, modulo, puedeAcceder)
SELECT p.idRol, 'odontograma_3d', 0 FROM permisos p
WHERE p.modulo='odontograma'
AND NOT EXISTS (SELECT 1 FROM permisos v WHERE v.idRol=p.idRol AND v.modulo='odontograma_3d')
GROUP BY p.idRol;
