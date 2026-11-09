package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IInventarioTransferenciaDao;
import com.dentia.model.interno.inventario.InventarioTransferencia;
import com.dentia.model.interno.inventario.InventarioTransferenciaDetalle;
import com.dentia.model.interno.inventario.InventarioTransferenciaEvento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcInventarioTransferenciaDao implements IInventarioTransferenciaDao {

    private final JdbcTemplate jdbc;

    public JdbcInventarioTransferenciaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==========================
    // REGISTRAR TRANSFERENCIA
    // ==========================
    @Override
    public int registrarTransferencia(InventarioTransferencia t) {

        String sql = """
            INSERT INTO inventario_transferencia
            (
                codigo,
                idSedeOrigen,
                idSedeDestino,
                idUsuario,
                idTransportista,
                estado,
                tipoTransferencia,
                requiereTransporte,
                observacion
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, t.getCodigo());
            ps.setInt(2, t.getIdSedeOrigen());
            ps.setInt(3, t.getIdSedeDestino());

            if (t.getIdUsuario() != null) {
                ps.setInt(4, t.getIdUsuario());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            if (t.getIdTransportista() != null) {
                ps.setInt(5, t.getIdTransportista());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, t.getEstado() != null ? t.getEstado() : "CREADA");
            ps.setString(7, t.getTipoTransferencia() != null ? t.getTipoTransferencia() : "INMEDIATA");
            ps.setBoolean(8, Boolean.TRUE.equals(t.getRequiereTransporte()));
            ps.setString(9, t.getObservacion());

            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    // ==========================
    // REGISTRAR DETALLE
    // ==========================
    @Override
    public void registrarDetalle(InventarioTransferenciaDetalle d) {

        String sql = """
            INSERT INTO inventario_transferencia_detalle
            (
                idTransferencia,
                idProducto,
                cantidad,
                cantidadEnviada
            )
            VALUES (?, ?, ?, ?)
        """;

        jdbc.update(sql,
                d.getIdTransferencia(),
                d.getIdProducto(),
                d.getCantidad(),
                d.getCantidadEnviada() != null ? d.getCantidadEnviada() : d.getCantidad()
        );
    }

    // ==========================
    // LISTADOS GENERALES
    // ==========================
    @Override
    public List<InventarioTransferencia> listarTransferencias() {

        String sql = sqlBaseTransferencias() + """
            ORDER BY t.fechaTransferencia DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapTransferencia(rs));
    }

    @Override
    public List<InventarioTransferencia> listarTransferenciasPorSede(Integer idSede) {

        String sql = sqlBaseTransferencias() + """
            WHERE t.idSedeOrigen = ?
               OR t.idSedeDestino = ?
            ORDER BY t.fechaTransferencia DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapTransferencia(rs), idSede, idSede);
    }

    @Override
    public Optional<InventarioTransferencia> buscarPorId(Integer idTransferencia) {

        String sql = sqlBaseTransferencias() + """
            WHERE t.idTransferencia = ?
        """;

        List<InventarioTransferencia> resultado =
                jdbc.query(sql, (rs, rowNum) -> mapTransferencia(rs), idTransferencia);

        return resultado.stream().findFirst();
    }

    // ==========================
    // DETALLE
    // ==========================
    @Override
    public List<InventarioTransferenciaDetalle> listarDetalles(Integer idTransferencia) {

        String sql = """
    SELECT 
        d.idDetalleTransferencia,
        d.idTransferencia,
        d.idProducto,
        d.cantidad,
        d.cantidadEnviada,
        d.cantidadRecibida,
        d.cantidadFaltante,
        d.observacionRecepcion,
        p.CodigoInterno,
        p.NombreProducto,
        p.foto AS fotoProducto,
        '-' AS unidadProducto
    FROM inventario_transferencia_detalle d
    INNER JOIN productos p ON p.idProducto = d.idProducto
    WHERE d.idTransferencia = ?
""";

        return jdbc.query(sql, (rs, rowNum) -> {
            InventarioTransferenciaDetalle d = new InventarioTransferenciaDetalle();

            d.setIdDetalleTransferencia(rs.getInt("idDetalleTransferencia"));
            d.setIdTransferencia(rs.getInt("idTransferencia"));
            d.setIdProducto(rs.getInt("idProducto"));
            d.setCantidad(rs.getBigDecimal("cantidad"));
            d.setCantidadEnviada(rs.getBigDecimal("cantidadEnviada"));
            d.setCantidadRecibida(rs.getBigDecimal("cantidadRecibida"));
            d.setCantidadFaltante(rs.getBigDecimal("cantidadFaltante"));
            d.setObservacionRecepcion(rs.getString("observacionRecepcion"));

            d.setCodigoProducto(rs.getString("CodigoInterno"));
            d.setNombreProducto(rs.getString("NombreProducto"));
            d.setFotoProducto(rs.getString("fotoProducto"));
            d.setUnidadProducto(rs.getString("unidadProducto"));

            return d;
        }, idTransferencia);
    }

    // ==========================
    // CÓDIGO
    // ==========================
    @Override
    public String generarCodigo() {

        Integer siguiente = jdbc.queryForObject("""
            SELECT COALESCE(MAX(idTransferencia), 0) + 1
            FROM inventario_transferencia
        """, Integer.class);

        return String.format("TRF-%05d", siguiente == null ? 1 : siguiente);
    }



    @Override
    public void eliminarDetallesPorTransferencia(Integer idTransferencia) {

        String sql = """
        DELETE FROM inventario_transferencia_detalle
        WHERE idTransferencia = ?
    """;

        jdbc.update(sql, idTransferencia);
    }

    // ==========================
    // TRASLADOS INTERNOS - LISTADOS
    // ==========================
    @Override
    public List<InventarioTransferencia> listarTrasladosPendientes() {

        String sql = sqlBaseTransferencias() + """
            WHERE t.requiereTransporte = 1
              AND t.estado = 'CREADA'
            ORDER BY t.fechaTransferencia ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapTransferencia(rs));
    }

    @Override
    public List<InventarioTransferencia> listarTrasladosEnProceso() {

        String sql = sqlBaseTransferencias() + """
            WHERE t.requiereTransporte = 1
              AND t.estado IN (
                    'ACEPTADA',
                    'EN_RECOJO',
                    'EN_PREPARACION',
                    'LISTO_PARA_SALIR',
                    'EN_RUTA'
              )
            ORDER BY t.fechaTransferencia DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapTransferencia(rs));
    }

    @Override
    public List<InventarioTransferencia> listarTrasladosPorRecepcion() {

        String sql = sqlBaseTransferencias() + """
            WHERE t.requiereTransporte = 1
              AND t.estado = 'PENDIENTE_RECEPCION'
            ORDER BY t.fechaLlegadaDestino ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapTransferencia(rs));
    }

    @Override
    public List<InventarioTransferencia> listarHistorialTraslados() {

        String sql = sqlBaseTransferencias() + """
            WHERE t.requiereTransporte = 1
              AND t.estado IN (
                    'COMPLETADA',
                    'COMPLETADA_CON_DIFERENCIAS',
                    'CANCELADA'
              )
            ORDER BY t.fechaTransferencia DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapTransferencia(rs));
    }

    // ==========================
    // CAMBIOS DE ESTADO
    // ==========================
    @Override
    public void actualizarEstado(Integer idTransferencia, String nuevoEstado) {

        String sql = """
            UPDATE inventario_transferencia
            SET estado = ?
            WHERE idTransferencia = ?
        """;

        jdbc.update(sql, nuevoEstado, idTransferencia);
    }

    @Override
    public void aceptarTraslado(Integer idTransferencia, Integer idTransportista) {

        String sql = """
            UPDATE inventario_transferencia
            SET estado = 'ACEPTADA',
                idTransportista = ?,
                fechaAceptacion = NOW()
            WHERE idTransferencia = ?
        """;

        jdbc.update(sql, idTransportista, idTransferencia);
    }

    @Override
    public void marcarLlegadaOrigen(Integer idTransferencia, String fotoLlegadaOrigen) {

        String sql = """
        UPDATE inventario_transferencia
        SET estado = 'EN_RECOJO',
            fechaLlegadaOrigen = NOW(),
            fotoLlegadaOrigen = ?
        WHERE idTransferencia = ?
    """;

        jdbc.update(sql, fotoLlegadaOrigen, idTransferencia);
    }

    @Override
    public void marcarPedidoListo(Integer idTransferencia, String fotoCarga) {

        String sql = """
        UPDATE inventario_transferencia
        SET estado = 'LISTO_PARA_SALIR',
            fechaPedidoListo = NOW(),
            fotoCarga = ?
        WHERE idTransferencia = ?
    """;

        jdbc.update(sql, fotoCarga, idTransferencia);
    }

    @Override
    public void iniciarRuta(Integer idTransferencia) {

        String sql = """
            UPDATE inventario_transferencia
            SET estado = 'EN_RUTA',
                fechaInicioRuta = NOW()
            WHERE idTransferencia = ?
        """;

        jdbc.update(sql, idTransferencia);
    }

    @Override
    public void marcarLlegadaDestino(Integer idTransferencia, String fotoLlegada) {

        String sql = """
        UPDATE inventario_transferencia
        SET estado = 'PENDIENTE_RECEPCION',
            fechaLlegadaDestino = NOW(),
            fotoLlegada = ?
        WHERE idTransferencia = ?
    """;

        jdbc.update(sql, fotoLlegada, idTransferencia);
    }

    @Override
    public void cancelarTraslado(Integer idTransferencia, String observacion) {

        String sql = """
            UPDATE inventario_transferencia
            SET estado = 'CANCELADA',
                observacionRecepcion = ?
            WHERE idTransferencia = ?
        """;

        jdbc.update(sql, observacion, idTransferencia);
    }

    // ==========================
    // RECEPCIÓN
    // ==========================
    @Override
    public void actualizarRecepcionDetalle(
            Integer idDetalleTransferencia,
            BigDecimal cantidadRecibida,
            BigDecimal cantidadFaltante,
            String observacionRecepcion
    ) {

        String sql = """
            UPDATE inventario_transferencia_detalle
            SET cantidadRecibida = ?,
                cantidadFaltante = ?,
                observacionRecepcion = ?
            WHERE idDetalleTransferencia = ?
        """;

        jdbc.update(sql,
                cantidadRecibida,
                cantidadFaltante,
                observacionRecepcion,
                idDetalleTransferencia
        );
    }

    @Override
    public void confirmarRecepcion(
            Integer idTransferencia,
            String estadoFinal,
            String observacionRecepcion
    ) {

        String sql = """
            UPDATE inventario_transferencia
            SET estado = ?,
                fechaRecepcion = NOW(),
                observacionRecepcion = ?
            WHERE idTransferencia = ?
        """;

        jdbc.update(sql, estadoFinal, observacionRecepcion, idTransferencia);
    }

    // ==========================
    // EVENTOS / HISTORIAL
    // ==========================
    @Override
    public void registrarEvento(InventarioTransferenciaEvento evento) {

        String sql = """
            INSERT INTO inventario_transferencia_evento
            (
                idTransferencia,
                estadoAnterior,
                estadoNuevo,
                observacion,
                idUsuario
            )
            VALUES (?, ?, ?, ?, ?)
        """;

        jdbc.update(sql,
                evento.getIdTransferencia(),
                evento.getEstadoAnterior(),
                evento.getEstadoNuevo(),
                evento.getObservacion(),
                evento.getIdUsuario()
        );
    }

    @Override
    public List<InventarioTransferenciaEvento> listarEventos(Integer idTransferencia) {

        String sql = """
            SELECT
                e.idEvento,
                e.idTransferencia,
                e.estadoAnterior,
                e.estadoNuevo,
                e.observacion,
                e.idUsuario,
                e.fechaEvento,
                u.nombreUsuario AS nombreUsuario
            FROM inventario_transferencia_evento e
            LEFT JOIN usuarios u ON u.idUsuario = e.idUsuario
            WHERE e.idTransferencia = ?
            ORDER BY e.fechaEvento ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            InventarioTransferenciaEvento e = new InventarioTransferenciaEvento();

            e.setIdEvento(rs.getInt("idEvento"));
            e.setIdTransferencia(rs.getInt("idTransferencia"));
            e.setEstadoAnterior(rs.getString("estadoAnterior"));
            e.setEstadoNuevo(rs.getString("estadoNuevo"));
            e.setObservacion(rs.getString("observacion"));
            e.setIdUsuario(rs.getObject("idUsuario") == null ? null : rs.getInt("idUsuario"));

            if (rs.getTimestamp("fechaEvento") != null) {
                e.setFechaEvento(rs.getTimestamp("fechaEvento").toLocalDateTime());
            }

            e.setNombreUsuario(rs.getString("nombreUsuario"));

            return e;
        }, idTransferencia);
    }

    // ==========================
    // SQL BASE
    // ==========================
    private String sqlBaseTransferencias() {
        return """
            SELECT 
                t.idTransferencia,
                t.codigo,
                t.idSedeOrigen,
                t.idSedeDestino,
                t.idUsuario,
                t.idTransportista,
                t.estado,
                t.tipoTransferencia,
                t.requiereTransporte,
                t.observacion,
                t.fechaTransferencia,
               t.fechaAceptacion,
                       t.fechaLlegadaOrigen,
                       t.fotoLlegadaOrigen,
                       t.fechaPedidoListo,
                       t.fechaInicioRuta,
                       t.fechaLlegadaDestino,
                       t.fechaRecepcion,
                       t.fotoCarga,
                       t.fotoLlegada,
                t.observacionRecepcion,
                so.nombre AS nombreSedeOrigen,
                sd.nombre AS nombreSedeDestino,
                u.nombreUsuario AS nombreUsuario,
                ut.nombreUsuario AS nombreTransportista
            FROM inventario_transferencia t
            INNER JOIN sedes so ON so.idSede = t.idSedeOrigen
            INNER JOIN sedes sd ON sd.idSede = t.idSedeDestino
            LEFT JOIN usuarios u ON u.idUsuario = t.idUsuario
            LEFT JOIN usuarios ut ON ut.idUsuario = t.idTransportista
        """;
    }

    private String sqlBaseTransferenciasConFechaFinalizacion() {
        return """
        SELECT 
            t.idTransferencia,
            t.codigo,
            t.idSedeOrigen,
            t.idSedeDestino,
            t.idUsuario,
            t.idTransportista,
            t.estado,
            t.tipoTransferencia,
            t.requiereTransporte,
            t.observacion,
            t.fechaTransferencia,
            t.fechaAceptacion,
            t.fechaLlegadaOrigen,
            t.fotoLlegadaOrigen,
            t.fechaPedidoListo,
            t.fechaInicioRuta,
            t.fechaLlegadaDestino,
            t.fechaRecepcion,
            t.fotoCarga,
            t.fotoLlegada,
            t.observacionRecepcion,

            CASE
                WHEN t.estado = 'CANCELADA' THEN (
                    SELECT MAX(e.fechaEvento)
                    FROM inventario_transferencia_evento e
                    WHERE e.idTransferencia = t.idTransferencia
                      AND e.estadoNuevo = 'CANCELADA'
                )
                ELSE t.fechaRecepcion
            END AS fechaFinalizacion,

            so.nombre AS nombreSedeOrigen,
            sd.nombre AS nombreSedeDestino,
            u.nombreUsuario AS nombreUsuario,
            ut.nombreUsuario AS nombreTransportista
        FROM inventario_transferencia t
        INNER JOIN sedes so ON so.idSede = t.idSedeOrigen
        INNER JOIN sedes sd ON sd.idSede = t.idSedeDestino
        LEFT JOIN usuarios u ON u.idUsuario = t.idUsuario
        LEFT JOIN usuarios ut ON ut.idUsuario = t.idTransportista
    """;
    }

    private InventarioTransferencia mapTransferencia(java.sql.ResultSet rs) throws java.sql.SQLException {
        InventarioTransferencia t = new InventarioTransferencia();

        t.setIdTransferencia(rs.getInt("idTransferencia"));
        t.setCodigo(rs.getString("codigo"));
        t.setIdSedeOrigen(rs.getInt("idSedeOrigen"));
        t.setIdSedeDestino(rs.getInt("idSedeDestino"));
        t.setIdUsuario(rs.getObject("idUsuario") == null ? null : rs.getInt("idUsuario"));
        t.setIdTransportista(rs.getObject("idTransportista") == null ? null : rs.getInt("idTransportista"));

        t.setEstado(rs.getString("estado"));
        t.setTipoTransferencia(rs.getString("tipoTransferencia"));
        t.setRequiereTransporte(rs.getBoolean("requiereTransporte"));

        t.setObservacion(rs.getString("observacion"));

        if (rs.getTimestamp("fechaTransferencia") != null) {
            t.setFechaTransferencia(rs.getTimestamp("fechaTransferencia").toLocalDateTime());
        }

        if (rs.getTimestamp("fechaAceptacion") != null) {
            t.setFechaAceptacion(rs.getTimestamp("fechaAceptacion").toLocalDateTime());
        }

        if (rs.getTimestamp("fechaLlegadaOrigen") != null) {
            t.setFechaLlegadaOrigen(rs.getTimestamp("fechaLlegadaOrigen").toLocalDateTime());
        }

        if (rs.getTimestamp("fechaPedidoListo") != null) {
            t.setFechaPedidoListo(rs.getTimestamp("fechaPedidoListo").toLocalDateTime());
        }

        if (rs.getTimestamp("fechaInicioRuta") != null) {
            t.setFechaInicioRuta(rs.getTimestamp("fechaInicioRuta").toLocalDateTime());
        }

        if (rs.getTimestamp("fechaLlegadaDestino") != null) {
            t.setFechaLlegadaDestino(rs.getTimestamp("fechaLlegadaDestino").toLocalDateTime());
        }

        if (rs.getTimestamp("fechaRecepcion") != null) {
            t.setFechaRecepcion(rs.getTimestamp("fechaRecepcion").toLocalDateTime());
        }

        try {
            if (rs.getTimestamp("fechaFinalizacion") != null) {
                t.setFechaFinalizacion(rs.getTimestamp("fechaFinalizacion").toLocalDateTime());
            }
        } catch (java.sql.SQLException ignored) {
            // Algunos listados no traen fechaFinalizacion.
        }

        t.setFotoLlegadaOrigen(rs.getString("fotoLlegadaOrigen"));
        t.setFotoCarga(rs.getString("fotoCarga"));
        t.setFotoLlegada(rs.getString("fotoLlegada"));
        t.setObservacionRecepcion(rs.getString("observacionRecepcion"));

        t.setNombreSedeOrigen(rs.getString("nombreSedeOrigen"));
        t.setNombreSedeDestino(rs.getString("nombreSedeDestino"));
        t.setNombreUsuario(rs.getString("nombreUsuario"));
        t.setNombreTransportista(rs.getString("nombreTransportista"));

        return t;
    }
}
