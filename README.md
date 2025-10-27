# 🏭 VIALSA Almacén — Sistema de Control de Inventario

**Autor:** Brayan Santamaría Gonzales  
**Repositorio:** [GitHub - santamariabrayan966-jpg/vialsa-almacen](https://github.com/santamariabrayan966-jpg/vialsa-almacen)

---

## 🚀 Descripción general

VIALSA Almacén es un sistema web desarrollado en **Java 17 + Spring Boot 3 + MySQL 8 + Bootstrap 5**, diseñado para la gestión de inventarios, productos, usuarios, compras y ventas, con control de roles y autenticación segura.

---

## 🧱 Arquitectura y principios aplicados

El proyecto sigue los principios de **MVC**, **DAO**, **SOLID** y **Seguridad**:

- 🧩 **MVC (Model–View–Controller):**  
  Separación de capas en `controller`, `service`, `dao`, `model` y `templates`.
- 💾 **DAO (Data Access Object):**  
  Interfaz `UsuarioDao` y clase `JdbcUsuarioDao` para acceso a datos.
- 🔐 **Seguridad:**  
  Implementación de **Spring Security** con roles `ADMIN` y `USER`.
- 🧠 **SOLID:**  
  Clases con responsabilidad única y bajo acoplamiento entre capas.

---

## 📚 Librerías y recursos Java

El proyecto incorpora librerías modernas para mejorar la eficiencia:

| Librería | Uso |
|-----------|-----|
| **Google Guava** | Colecciones y utilidades de Java |
| **Apache POI** | Exportación de datos a Excel |
| **Apache Commons Lang** | Manejo de strings y validaciones |
| **Logback** | Registro de logs de aplicación |

---

## 🧩 Funcionalidades principales

- 🔑 Login con autenticación y roles (Spring Security)
- 🛒 Módulo de **productos** (CRUD completo)
- 🧾 Módulo de **ventas y compras**
- 👥 Gestión de **usuarios y roles** (solo admin)
- 🏷️ Control de inventario en tiempo real
- 💾 Persistencia con MySQL

---

## 🖥️ Interfaz gráfica

Diseñada con **Bootstrap 5**, siguiendo una estética moderna y responsiva:

- Logo institucional (`/images/logovialsa.png`)
- Navbar dinámica con nombre y logo de VIALSA
- Dashboard con módulos de acceso rápido
- Formularios validados con mensajes claros

---

## 🧩 Ejecución del proyecto

1. Crea la base de datos MySQL:
   ```sql
   CREATE DATABASE vialsa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
