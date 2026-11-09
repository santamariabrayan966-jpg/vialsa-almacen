package com.dentia.service.interno;
import com.dentia.model.interno.Producto;

import java.util.HashMap;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProductoCatalogoService {

    private final JdbcTemplate jdbc;

    public ProductoCatalogoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ============================================================
    // LISTADOS
    // ============================================================

    public List<Map<String, Object>> listarMarcas() {
        String sql = """
        SELECT idMarca, nombreMarca, descripcion, estado
        FROM marca_producto
        ORDER BY nombreMarca ASC
    """;
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listarCategorias() {

        String sql = """
        SELECT idCategoria,
               nombreCategoria,
               estado
        FROM categoria_producto
        ORDER BY nombreCategoria ASC
    """;

        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listarSubcategorias() {
        String sql = """
        SELECT sc.idSubcategoria,
               sc.idCategoria,
               sc.nombreSubcategoria,
               sc.descripcion,
               sc.estado,
               c.nombreCategoria
        FROM subcategoria_producto sc
        INNER JOIN categoria_producto c
            ON c.idCategoria = sc.idCategoria
        ORDER BY c.nombreCategoria ASC, sc.nombreSubcategoria ASC
    """;
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listarUnidades() {
        String sql = """
        SELECT idUnidad, NombreUnidad, abreviatura, tipoCalculo, estado
        FROM unidades
        ORDER BY NombreUnidad ASC
    """;
        return jdbc.queryForList(sql);
    }



    public List<Map<String, Object>> listarAtributos() {
        String sql = """
        SELECT a.idAtributo,
               a.idCategoria,
               a.nombreAtributo,
               a.tipoDato,
               a.estado,
               c.nombreCategoria
        FROM atributo_producto a
        LEFT JOIN categoria_producto c
            ON c.idCategoria = a.idCategoria
        ORDER BY c.nombreCategoria ASC, a.nombreAtributo ASC
    """;
        return jdbc.queryForList(sql);
    }
    // ============================================================
// LISTADOS SOLO ACTIVOS PARA FORMULARIO PRODUCTO
// ============================================================

    public List<Map<String, Object>> listarMarcasActivas() {
        String sql = """
        SELECT idMarca, nombreMarca, descripcion, estado
        FROM marca_producto
        WHERE estado = 1
        ORDER BY nombreMarca ASC
    """;
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listarCategoriasActivas() {
        String sql = """
        SELECT idCategoria,
               nombreCategoria,
               estado
        FROM categoria_producto
        WHERE estado = 1
        ORDER BY nombreCategoria ASC
    """;
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listarSubcategoriasActivas() {
        String sql = """
        SELECT sc.idSubcategoria,
               sc.idCategoria,
               sc.nombreSubcategoria,
               sc.descripcion,
               sc.estado,
               c.nombreCategoria
        FROM subcategoria_producto sc
        INNER JOIN categoria_producto c
            ON c.idCategoria = sc.idCategoria
        WHERE sc.estado = 1
          AND c.estado = 1
        ORDER BY c.nombreCategoria ASC, sc.nombreSubcategoria ASC
    """;
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listarUnidadesActivas() {
        String sql = """
        SELECT idUnidad, NombreUnidad, abreviatura, tipoCalculo, estado
        FROM unidades
        WHERE estado = 1
        ORDER BY NombreUnidad ASC
    """;
        return jdbc.queryForList(sql);
    }


    // ============================================================
    // REGISTRAR CATEGORÍA
    // ============================================================

    public void registrarCategoria(String nombreCategoria) {

        if (nombreCategoria == null || nombreCategoria.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }

        String nombre = nombreCategoria.trim();

        String validarSql = """
            SELECT COUNT(*)
            FROM categoria_producto
            WHERE LOWER(nombreCategoria) = LOWER(?)
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, nombre);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("La categoría ya existe.");
        }

        String sql = """
            INSERT INTO categoria_producto(nombreCategoria, descripcion, estado)
            VALUES (?, NULL, 1)
        """;

        jdbc.update(sql, nombre);
    }

    // ============================================================
    // REGISTRAR MARCA
    // ============================================================

    public void registrarMarca(String nombreMarca, String descripcion) {

        if (nombreMarca == null || nombreMarca.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la marca es obligatorio.");
        }

        String nombre = nombreMarca.trim();
        String desc = descripcion == null || descripcion.trim().isBlank()
                ? null
                : descripcion.trim();

        String validarSql = """
            SELECT COUNT(*)
            FROM marca_producto
            WHERE LOWER(nombreMarca) = LOWER(?)
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, nombre);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("La marca ya existe.");
        }

        String sql = """
            INSERT INTO marca_producto(nombreMarca, descripcion, estado)
            VALUES (?, ?, 1)
        """;

        jdbc.update(sql, nombre, desc);
    }

    // ============================================================
    // REGISTRAR UNIDAD
    // ============================================================

    public void registrarUnidad(String nombreUnidad, String abreviatura, String tipoCalculo) {

        if (nombreUnidad == null || nombreUnidad.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la unidad es obligatorio.");
        }

        if (tipoCalculo == null || tipoCalculo.trim().isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de cálculo.");
        }

        String nombre = nombreUnidad.trim();
        String abrev = abreviatura == null || abreviatura.trim().isBlank()
                ? null
                : abreviatura.trim();
        String tipo = tipoCalculo.trim();

        String validarSql = """
            SELECT COUNT(*)
            FROM unidades
            WHERE LOWER(NombreUnidad) = LOWER(?)
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, nombre);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("La unidad ya existe.");
        }

        String sql = """
            INSERT INTO unidades(NombreUnidad, abreviatura, tipoCalculo, estado)
            VALUES (?, ?, ?, 1)
        """;

        jdbc.update(sql, nombre, abrev, tipo);
    }

    // ============================================================
    // REGISTRAR SUBCATEGORÍA
    // ============================================================

    public void registrarSubcategoria(Integer idCategoria, String nombreSubcategoria) {

        if (idCategoria == null || idCategoria <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una categoría.");
        }

        if (nombreSubcategoria == null || nombreSubcategoria.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la subcategoría es obligatorio.");
        }

        String nombre = nombreSubcategoria.trim();

        String validarSql = """
            SELECT COUNT(*)
            FROM subcategoria_producto
            WHERE idCategoria = ?
              AND LOWER(nombreSubcategoria) = LOWER(?)
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, idCategoria, nombre);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("La subcategoría ya existe en esa categoría.");
        }

        String sql = """
            INSERT INTO subcategoria_producto(idCategoria, nombreSubcategoria, descripcion, estado)
            VALUES (?, ?, NULL, 1)
        """;

        jdbc.update(sql, idCategoria, nombre);
    }


    // ============================================================
    // REGISTRAR ATRIBUTO
    // ============================================================

    public void registrarAtributo(Integer idCategoria, String nombreAtributo, String tipoDato) {

        if (nombreAtributo == null || nombreAtributo.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del atributo es obligatorio.");
        }

        String nombre = nombreAtributo.trim();
        String tipo = tipoDato == null || tipoDato.trim().isBlank()
                ? "TEXTO"
                : tipoDato.trim().toUpperCase();

        String validarSql = """
            SELECT COUNT(*)
            FROM atributo_producto
            WHERE (idCategoria <=> ?)
              AND LOWER(nombreAtributo) = LOWER(?)
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, idCategoria, nombre);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("El atributo ya existe para esa categoría.");
        }

        String sql = """
            INSERT INTO atributo_producto(idCategoria, nombreAtributo, tipoDato, estado)
            VALUES (?, ?, ?, 1)
        """;

        jdbc.update(sql, idCategoria, nombre, tipo);
    }

    public List<Map<String, Object>> listarAtributosPorCategoria(Integer idCategoria) {

        String sql = """
        SELECT idAtributo,
               idCategoria,
               nombreAtributo,
               tipoDato,
               estado
        FROM atributo_producto
        WHERE estado = 1
          AND (idCategoria IS NULL OR idCategoria = ?)
        ORDER BY nombreAtributo ASC
    """;

        return jdbc.queryForList(sql, idCategoria);
    }

    public void guardarAtributosDeProducto(Integer idProducto, Map<Integer, String> atributos) {

        if (idProducto == null || idProducto <= 0) {
            throw new IllegalArgumentException("Producto inválido para guardar atributos.");
        }

        String eliminarSql = """
        DELETE FROM producto_atributo_valor
        WHERE idProducto = ?
    """;

        jdbc.update(eliminarSql, idProducto);

        if (atributos == null || atributos.isEmpty()) {
            return;
        }

        String insertarSql = """
        INSERT INTO producto_atributo_valor(idProducto, idAtributo, valor)
        VALUES (?, ?, ?)
    """;

        for (Map.Entry<Integer, String> entry : atributos.entrySet()) {
            Integer idAtributo = entry.getKey();
            String valor = entry.getValue();

            if (idAtributo != null && valor != null && !valor.trim().isBlank()) {
                jdbc.update(insertarSql, idProducto, idAtributo, valor.trim());
            }
        }
    }

    public Map<String, String> listarValoresAtributosDetallePorProducto(Integer idProducto) {

        String sql = """
        SELECT a.nombreAtributo,
               pav.valor
        FROM producto_atributo_valor pav
        INNER JOIN atributo_producto a
            ON a.idAtributo = pav.idAtributo
        WHERE pav.idProducto = ?
        ORDER BY a.nombreAtributo ASC
    """;

        Map<String, String> valores = new java.util.LinkedHashMap<>();

        jdbc.query(sql, rs -> {
            valores.put(
                    rs.getString("nombreAtributo"),
                    rs.getString("valor")
            );
        }, idProducto);

        return valores;
    }

    public Map<Integer, String> listarValoresAtributosPorProducto(Integer idProducto) {

        String sql = """
        SELECT idAtributo,
               valor
        FROM producto_atributo_valor
        WHERE idProducto = ?
    """;

        Map<Integer, String> valores = new java.util.HashMap<>();

        jdbc.query(sql, rs -> {
            valores.put(
                    rs.getInt("idAtributo"),
                    rs.getString("valor")
            );
        }, idProducto);

        return valores;
    }



    public Integer obtenerEstadoDuplicadoConAtributos(Producto producto,
                                                      Map<Integer, String> atributosFormulario) {

        if (producto == null || producto.getNombreProducto() == null) {
            return null;
        }

        String sql = """
        SELECT p.idProducto,
               p.idEstadoProducto
        FROM productos p
        WHERE LOWER(TRIM(p.NombreProducto)) = LOWER(TRIM(?))
          AND (p.idMarca <=> ?)
          AND (p.idCategoria <=> ?)
          AND (p.idSubcategoria <=> ?)
          AND (? IS NULL OR p.idProducto <> ?)
    """;

        List<Map<String, Object>> candidatos = jdbc.queryForList(
                sql,
                producto.getNombreProducto().trim(),
                producto.getIdMarca(),
                producto.getIdCategoria(),
                producto.getIdSubcategoria(),
                producto.getIdProducto(),
                producto.getIdProducto()
        );

        if (candidatos.isEmpty()) {
            return null;
        }

        Map<Integer, String> atributosNormalizadosFormulario =
                normalizarAtributosParaComparar(atributosFormulario);

        for (Map<String, Object> candidato : candidatos) {

            Integer idProductoCandidato = ((Number) candidato.get("idProducto")).intValue();
            Integer estadoCandidato = ((Number) candidato.get("idEstadoProducto")).intValue();

            Map<Integer, String> atributosBD =
                    obtenerAtributosPorIdProductoParaComparar(idProductoCandidato);

            if (Objects.equals(atributosNormalizadosFormulario, atributosBD)) {
                return estadoCandidato;
            }
        }

        return null;
    }

    private Map<Integer, String> obtenerAtributosPorIdProductoParaComparar(Integer idProducto) {

        String sql = """
        SELECT idAtributo,
               valor
        FROM producto_atributo_valor
        WHERE idProducto = ?
    """;

        Map<Integer, String> valores = new HashMap<>();

        jdbc.query(sql, rs -> {
            Integer idAtributo = rs.getInt("idAtributo");
            String valor = normalizarValorAtributo(rs.getString("valor"));

            if (valor != null && !valor.isBlank()) {
                valores.put(idAtributo, valor);
            }
        }, idProducto);

        return valores;
    }

    private Map<Integer, String> normalizarAtributosParaComparar(Map<Integer, String> atributos) {

        Map<Integer, String> normalizados = new HashMap<>();

        if (atributos == null || atributos.isEmpty()) {
            return normalizados;
        }

        for (Map.Entry<Integer, String> entry : atributos.entrySet()) {
            Integer idAtributo = entry.getKey();
            String valor = normalizarValorAtributo(entry.getValue());

            if (idAtributo != null && valor != null && !valor.isBlank()) {
                normalizados.put(idAtributo, valor);
            }
        }

        return normalizados;
    }

    private String normalizarValorAtributo(String valor) {

        if (valor == null) {
            return "";
        }

        return valor.trim().toLowerCase();
    }
    // ============================================================
    // ACTUALIZAR / CAMBIAR ESTADO - CATEGORÍAS
    // ============================================================

    public void actualizarCategoria(Integer idCategoria, String nombreCategoria) {

        if (idCategoria == null || idCategoria <= 0) {
            throw new IllegalArgumentException("Categoría inválida.");
        }

        if (nombreCategoria == null || nombreCategoria.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }

        String nombre = nombreCategoria.trim();

        String validarSql = """
            SELECT COUNT(*)
            FROM categoria_producto
            WHERE LOWER(nombreCategoria) = LOWER(?)
              AND idCategoria <> ?
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, nombre, idCategoria);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("Ya existe otra categoría con ese nombre.");
        }

        String sql = """
            UPDATE categoria_producto
            SET nombreCategoria = ?
            WHERE idCategoria = ?
        """;

        jdbc.update(sql, nombre, idCategoria);
    }

    public void cambiarEstadoCategoria(Integer idCategoria, Integer estado) {

        validarEstado(estado);

        if (idCategoria == null || idCategoria <= 0) {
            throw new IllegalArgumentException("Categoría inválida.");
        }

        String sql = """
            UPDATE categoria_producto
            SET estado = ?
            WHERE idCategoria = ?
        """;

        jdbc.update(sql, estado, idCategoria);
    }

    // ============================================================
    // ACTUALIZAR / CAMBIAR ESTADO - SUBCATEGORÍAS
    // ============================================================

    public void actualizarSubcategoria(Integer idSubcategoria, Integer idCategoria, String nombreSubcategoria) {

        if (idSubcategoria == null || idSubcategoria <= 0) {
            throw new IllegalArgumentException("Subcategoría inválida.");
        }

        if (idCategoria == null || idCategoria <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una categoría.");
        }

        if (nombreSubcategoria == null || nombreSubcategoria.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la subcategoría es obligatorio.");
        }

        String nombre = nombreSubcategoria.trim();

        String validarSql = """
            SELECT COUNT(*)
            FROM subcategoria_producto
            WHERE idCategoria = ?
              AND LOWER(nombreSubcategoria) = LOWER(?)
              AND idSubcategoria <> ?
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, idCategoria, nombre, idSubcategoria);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("Ya existe otra subcategoría con ese nombre en esa categoría.");
        }

        String sql = """
            UPDATE subcategoria_producto
            SET idCategoria = ?,
                nombreSubcategoria = ?
            WHERE idSubcategoria = ?
        """;

        jdbc.update(sql, idCategoria, nombre, idSubcategoria);
    }

    public void cambiarEstadoSubcategoria(Integer idSubcategoria, Integer estado) {

        validarEstado(estado);

        if (idSubcategoria == null || idSubcategoria <= 0) {
            throw new IllegalArgumentException("Subcategoría inválida.");
        }

        String sql = """
            UPDATE subcategoria_producto
            SET estado = ?
            WHERE idSubcategoria = ?
        """;

        jdbc.update(sql, estado, idSubcategoria);
    }

    // ============================================================
    // ACTUALIZAR / CAMBIAR ESTADO - MARCAS
    // ============================================================

    public void actualizarMarca(Integer idMarca, String nombreMarca, String descripcion) {

        if (idMarca == null || idMarca <= 0) {
            throw new IllegalArgumentException("Marca inválida.");
        }

        if (nombreMarca == null || nombreMarca.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la marca es obligatorio.");
        }

        String nombre = nombreMarca.trim();
        String desc = descripcion == null || descripcion.trim().isBlank()
                ? null
                : descripcion.trim();

        String validarSql = """
            SELECT COUNT(*)
            FROM marca_producto
            WHERE LOWER(nombreMarca) = LOWER(?)
              AND idMarca <> ?
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, nombre, idMarca);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("Ya existe otra marca con ese nombre.");
        }

        String sql = """
            UPDATE marca_producto
            SET nombreMarca = ?,
                descripcion = ?
            WHERE idMarca = ?
        """;

        jdbc.update(sql, nombre, desc, idMarca);
    }

    public void cambiarEstadoMarca(Integer idMarca, Integer estado) {

        validarEstado(estado);

        if (idMarca == null || idMarca <= 0) {
            throw new IllegalArgumentException("Marca inválida.");
        }

        String sql = """
            UPDATE marca_producto
            SET estado = ?
            WHERE idMarca = ?
        """;

        jdbc.update(sql, estado, idMarca);
    }

    // ============================================================
    // ACTUALIZAR / CAMBIAR ESTADO - UNIDADES
    // ============================================================

    public void actualizarUnidad(Integer idUnidad, String nombreUnidad, String abreviatura, String tipoCalculo) {

        if (idUnidad == null || idUnidad <= 0) {
            throw new IllegalArgumentException("Unidad inválida.");
        }

        if (nombreUnidad == null || nombreUnidad.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la unidad es obligatorio.");
        }

        if (tipoCalculo == null || tipoCalculo.trim().isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de cálculo.");
        }

        String nombre = nombreUnidad.trim();
        String abrev = abreviatura == null || abreviatura.trim().isBlank()
                ? null
                : abreviatura.trim();
        String tipo = tipoCalculo.trim().toUpperCase();

        String validarSql = """
            SELECT COUNT(*)
            FROM unidades
            WHERE LOWER(NombreUnidad) = LOWER(?)
              AND idUnidad <> ?
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, nombre, idUnidad);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("Ya existe otra unidad con ese nombre.");
        }

        String sql = """
            UPDATE unidades
            SET NombreUnidad = ?,
                abreviatura = ?,
                tipoCalculo = ?
            WHERE idUnidad = ?
        """;

        jdbc.update(sql, nombre, abrev, tipo, idUnidad);
    }

    public void cambiarEstadoUnidad(Integer idUnidad, Integer estado) {

        validarEstado(estado);

        if (idUnidad == null || idUnidad <= 0) {
            throw new IllegalArgumentException("Unidad inválida.");
        }

        String sql = """
            UPDATE unidades
            SET estado = ?
            WHERE idUnidad = ?
        """;

        jdbc.update(sql, estado, idUnidad);
    }

    // ============================================================
    // ACTUALIZAR / CAMBIAR ESTADO - ATRIBUTOS
    // ============================================================

    public void actualizarAtributo(Integer idAtributo, Integer idCategoria, String nombreAtributo, String tipoDato) {

        if (idAtributo == null || idAtributo <= 0) {
            throw new IllegalArgumentException("Atributo inválido.");
        }

        if (nombreAtributo == null || nombreAtributo.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del atributo es obligatorio.");
        }

        String nombre = nombreAtributo.trim();
        String tipo = tipoDato == null || tipoDato.trim().isBlank()
                ? "TEXTO"
                : tipoDato.trim().toUpperCase();

        String validarSql = """
            SELECT COUNT(*)
            FROM atributo_producto
            WHERE (idCategoria <=> ?)
              AND LOWER(nombreAtributo) = LOWER(?)
              AND idAtributo <> ?
        """;

        Integer existe = jdbc.queryForObject(validarSql, Integer.class, idCategoria, nombre, idAtributo);

        if (existe != null && existe > 0) {
            throw new IllegalArgumentException("Ya existe otro atributo con ese nombre para esa categoría.");
        }

        String sql = """
            UPDATE atributo_producto
            SET idCategoria = ?,
                nombreAtributo = ?,
                tipoDato = ?
            WHERE idAtributo = ?
        """;

        jdbc.update(sql, idCategoria, nombre, tipo, idAtributo);
    }

    public void cambiarEstadoAtributo(Integer idAtributo, Integer estado) {

        validarEstado(estado);

        if (idAtributo == null || idAtributo <= 0) {
            throw new IllegalArgumentException("Atributo inválido.");
        }

        String sql = """
            UPDATE atributo_producto
            SET estado = ?
            WHERE idAtributo = ?
        """;

        jdbc.update(sql, estado, idAtributo);
    }

    // ============================================================
    // VALIDACIÓN GENERAL
    // ============================================================

    private void validarEstado(Integer estado) {

        if (estado == null || (estado != 1 && estado != 2)) {
            throw new IllegalArgumentException("Estado inválido.");
        }
    }
}
