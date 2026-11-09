package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IProductoFotoDao;
import com.dentia.model.interno.ProductoFoto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProductoFotoDao implements IProductoFotoDao {

    private final JdbcTemplate jdbc;

    public JdbcProductoFotoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private ProductoFoto mapear(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ProductoFoto foto = new ProductoFoto();
        foto.setIdProductoFoto(rs.getInt("idProductoFoto"));
        foto.setIdProducto(rs.getInt("idProducto"));
        foto.setNombreArchivo(rs.getString("nombreArchivo"));
        foto.setUrlArchivo(rs.getString("urlArchivo"));
        foto.setPortada(rs.getObject("portada") != null && rs.getBoolean("portada"));
        foto.setOrden(rs.getInt("orden"));
        foto.setEstado(rs.getInt("estado"));

        Timestamp fecha = rs.getTimestamp("fechaCreacion");
        foto.setFechaCreacion(fecha == null ? null : fecha.toLocalDateTime());
        return foto;
    }

    @Override
    public List<ProductoFoto> listarActivasPorProducto(Integer idProducto) {
        return jdbc.query("""
            SELECT idProductoFoto, idProducto, nombreArchivo, urlArchivo, portada, orden, estado, fechaCreacion
            FROM producto_foto
            WHERE idProducto = ?
              AND estado = 1
            ORDER BY portada DESC, orden ASC, idProductoFoto ASC
        """, this::mapear, idProducto);
    }

    @Override
    public int contarActivasPorProducto(Integer idProducto) {
        Integer total = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM producto_foto
            WHERE idProducto = ?
              AND estado = 1
        """, Integer.class, idProducto);

        return total == null ? 0 : total;
    }

    @Override
    public int guardar(ProductoFoto foto) {
        return jdbc.update("""
            INSERT INTO producto_foto
                (idProducto, nombreArchivo, urlArchivo, portada, orden, estado)
            VALUES (?, ?, ?, ?, ?, 1)
        """,
                foto.getIdProducto(),
                foto.getNombreArchivo(),
                foto.getUrlArchivo(),
                Boolean.TRUE.equals(foto.getPortada()),
                foto.getOrden() == null ? 1 : foto.getOrden()
        );
    }

    @Override
    public int desactivar(Integer idProductoFoto) {
        return jdbc.update("""
            UPDATE producto_foto
            SET estado = 2,
                portada = 0
            WHERE idProductoFoto = ?
        """, idProductoFoto);
    }

    @Override
    public int marcarPortada(Integer idProductoFoto) {
        ProductoFoto foto = buscarPorId(idProductoFoto)
                .orElseThrow(() -> new IllegalArgumentException("La foto seleccionada no existe."));

        if (foto.getEstado() == null || foto.getEstado() != 1) {
            throw new IllegalArgumentException("Solo una foto activa puede marcarse como portada.");
        }

        jdbc.update("""
            UPDATE producto_foto
            SET portada = 0
            WHERE idProducto = ?
              AND estado = 1
        """, foto.getIdProducto());

        return jdbc.update("""
            UPDATE producto_foto
            SET portada = 1
            WHERE idProductoFoto = ?
              AND estado = 1
        """, idProductoFoto);
    }

    @Override
    public Optional<ProductoFoto> buscarPorId(Integer idProductoFoto) {
        List<ProductoFoto> fotos = jdbc.query("""
            SELECT idProductoFoto, idProducto, nombreArchivo, urlArchivo, portada, orden, estado, fechaCreacion
            FROM producto_foto
            WHERE idProductoFoto = ?
            LIMIT 1
        """, this::mapear, idProductoFoto);

        return fotos.stream().findFirst();
    }

    @Override
    public Optional<ProductoFoto> obtenerPortadaPorProducto(Integer idProducto) {
        List<ProductoFoto> fotos = jdbc.query("""
            SELECT idProductoFoto, idProducto, nombreArchivo, urlArchivo, portada, orden, estado, fechaCreacion
            FROM producto_foto
            WHERE idProducto = ?
              AND estado = 1
              AND portada = 1
            ORDER BY orden ASC, idProductoFoto ASC
            LIMIT 1
        """, this::mapear, idProducto);

        return fotos.stream().findFirst();
    }

    @Override
    public Optional<ProductoFoto> obtenerPrimeraActivaPorProducto(Integer idProducto) {
        List<ProductoFoto> fotos = jdbc.query("""
            SELECT idProductoFoto, idProducto, nombreArchivo, urlArchivo, portada, orden, estado, fechaCreacion
            FROM producto_foto
            WHERE idProducto = ?
              AND estado = 1
            ORDER BY orden ASC, idProductoFoto ASC
            LIMIT 1
        """, this::mapear, idProducto);

        return fotos.stream().findFirst();
    }
}
