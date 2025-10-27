# VIALSA — Sistema de Control de Almacén (Modernizado)
**Java 17 · Spring Boot 3 · MySQL 8 · Bootstrap 5 (rojo/azul)**

## 🚀 Ejecutar (IntelliJ)
1. Asegura que existe la BD **`vialsa`** en MySQL (usa tu propio dump).
2. Abre este proyecto en IntelliJ.
3. Ejecuta `com.vialsa.almacen.VialsaAlmacenApplication`.
4. Abre `http://localhost:8080/login` → **admin / admin123** (se crea automáticamente si no existe).

## 🔐 Seguridad
- Spring Security (form login)
- BCrypt para contraseñas
- CSRF habilitado en formularios
- Roles mapeados desde `usuarios.idRol` (1=ADMIN, 2=VENDEDOR, 3=ALMACENERO)

## 🧱 Arquitectura
- **MVC**: Controller → Service → DAO → DB
- **DAO**: `JdbcTemplate` + `BeanPropertyRowMapper`
- **SOLID**: Interfaces y responsabilidades claras
- **@Transactional**: (recomendado) aplicar en servicios de inventario/ventas

## 🎨 UI
- Bootstrap 5 con tema rojo/azul
- Dashboard y login modernos
- Imagen abstracta en `/images/abstract-glass.svg`

