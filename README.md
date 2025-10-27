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

## 📦 Subir a GitHub
Si tu repo ya existe:
```bash
git add .
git commit -m "Versión final modernizada VialsaAlmacen"
git push origin main
```
Si necesitas vincular por primera vez:
```bash
git init
git remote add origin https://github.com/santamariabrayan966-jpg/Vialsa.git
git add .
git commit -m "Versión final modernizada VialsaAlmacen"
git branch -M main
git push -u origin main
```

## ⚠️ Notas de BD
Este proyecto **no altera** tu esquema. Solo crea el usuario admin si no existe:
- Inserta rol `ADMIN (id=1)` y estado `ACTIVO (id=1)` si faltan (INSERT IGNORE).
- Inserta `usuarios(NombreUsuario='admin', Contrasena='<bcrypt>', idRol=1, idEstadoUsuario=1)`.

Ajusta los nombres de columnas si tu esquema difiere.
