package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IVentaDao;
import com.dentia.model.interno.Venta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import com.dentia.model.interno.DetalleVenta;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

@Repository
public class JdbcVentaDao implements IVentaDao {

    private final JdbcTemplate jdbc;

    public JdbcVentaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ======================================
    //   MAPPER PROFESIONAL
    // ======================================
    private Venta mapVenta(ResultSet rs) throws SQLException {
        Venta v = new Venta();

        v.setIdVentas(rs.getInt("idVentas"));

        // Evitar NullPointer si no existe FechaVenta
        if (rs.getTimestamp("FechaVenta") != null) {
            v.setFechaVenta(rs.getTimestamp("FechaVenta").toLocalDateTime());
        }

        v.setTipoComprobante(rs.getString("TipoComprobante"));
        v.setNroComprobante(rs.getString("NroComprobante"));

        // idClientes puede ser NULL → usar getObject()
        Integer idCli = (Integer) rs.getObject("idClientes");
        v.setIdCliente(idCli);

        v.setIdUsuario(rs.getInt("idUsuario"));
        v.setIdSede((Integer) rs.getObject("idSede"));
        try {
            v.setIdOrdenOnline((Integer) rs.getObject("idOrdenOnline"));
        } catch (Exception ignored) {}

        try {
            v.setNombreSede(rs.getString("nombreSede"));
        } catch (Exception ignored) {}
        v.setFormaPago(rs.getString("FormaPago"));
        v.setMetodoPago(rs.getString("metodoPago"));
        v.setEstadoVenta(rs.getString("EstadoVenta"));
        v.setTotalVenta(rs.getBigDecimal("TotalVenta"));
        v.setDeuda(rs.getBigDecimal("Deuda"));
        v.setPagoInicial(rs.getBigDecimal("pagoInicial"));
        v.setNumeroCuotas(rs.getInt("numeroCuotas"));
        v.setFrecuenciaCuotas(rs.getString("frecuenciaCuotas"));

        if (rs.getDate("fechaPrimeraCuota") != null) {
            v.setFechaPrimeraCuota(rs.getDate("fechaPrimeraCuota").toLocalDate());
        }

        // Datos auxiliares (pueden ser NULL si no hay cliente)
        v.setNombreCliente(rs.getString("nombreCliente"));
        v.setDocumentoCliente(rs.getString("documentoCliente"));
        v.setTelefonoCliente(rs.getString("telefonoCliente"));
        v.setNombreUsuario(rs.getString("nombreUsuario"));

        v.setCanalVenta(rs.getString("canal_venta"));
        v.setEstadoPago(rs.getString("EstadoPago"));
        v.setCodigoTicket(rs.getString("CodigoTicket"));
        v.setCodigoRecojo(rs.getString("codigoRecojo"));

        if (rs.getTimestamp("FechaRecojo") != null) {
            v.setFechaRecojo(rs.getTimestamp("FechaRecojo").toLocalDateTime());
        }

        v.setObservacionRecojo(rs.getString("ObservacionRecojo"));
        v.setIdTransaccionPasarela(rs.getString("IdTransaccionPasarela"));

        v.setIdPedido((Integer) rs.getObject("idPedido"));
        v.setIdEstadoPedido((Integer) rs.getObject("idEstadoPedido"));
        v.setNombreEstadoPedido(rs.getString("nombreEstadoPedido"));

        if (rs.getTimestamp("fechaRecojoProgramada") != null) {
            v.setFechaRecojoProgramada(
                    rs.getTimestamp("fechaRecojoProgramada").toLocalDateTime()
            );
        }

        if (rs.getTimestamp("fechaPreparando") != null) {
            v.setFechaPreparando(rs.getTimestamp("fechaPreparando").toLocalDateTime());
        }

        if (rs.getTimestamp("fechaListo") != null) {
            v.setFechaListo(rs.getTimestamp("fechaListo").toLocalDateTime());
        }

        if (rs.getTimestamp("fechaEntregado") != null) {
            v.setFechaEntregado(rs.getTimestamp("fechaEntregado").toLocalDateTime());
        }

        v.setIdUsuarioPreparando((Integer) rs.getObject("idUsuarioPreparando"));
        v.setIdUsuarioEntrega((Integer) rs.getObject("idUsuarioEntrega"));

        v.setNombreUsuarioPreparando(rs.getString("nombreUsuarioPreparando"));
        v.setNombreUsuarioEntrega(rs.getString("nombreUsuarioEntrega"));

        v.setObservacionEntrega(rs.getString("observacionEntrega"));

        try {
            v.setMinutosAlistado(rs.getLong("minutosAlistado"));
        } catch (Exception ignored) {}

        try {
            v.setMinutosTotalProceso(rs.getLong("minutosTotalProceso"));
        } catch (Exception ignored) {}

        try {
            v.setTipoEntregaHistorial(rs.getString("tipoEntregaHistorial"));
        } catch (Exception ignored) {}

        return v;
    }

    // ======================================
    //      LISTAR TODAS LAS VENTAS
    // ======================================
    @Override
    public List<Venta> listar() {
        String sql = """
           SELECT v.*,
                         COALESCE(
                                     NULLIF(v.nombreCliente, ''),
                                     CASE
                                         WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                         ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                                     END,
                                     'Cliente ocasional'
                                 ) AS nombreCliente,

                                 COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                                 COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
                         u.NombreUsuario AS nombreUsuario,
                                 s.nombre AS nombreSede,
                                 p.idPedido AS idPedido,
                          p.idEstadoPedido AS idEstadoPedido,
                          p.codigoRecojo AS codigoRecojo,
                          p.fechaRecojoProgramada AS fechaRecojoProgramada,
                                  p.fechaPreparando AS fechaPreparando,
                                  p.fechaListo AS fechaListo,
                                  p.fechaEntregado AS fechaEntregado,
                                  p.idUsuarioPreparando AS idUsuarioPreparando,
                                  p.idUsuarioEntrega AS idUsuarioEntrega,
                                  p.observacion AS observacionEntrega,
                                  up.NombreUsuario AS nombreUsuarioPreparando,
                                  ue.NombreUsuario AS nombreUsuarioEntrega,
                                  ep.nombreEstado AS nombreEstadoPedido
                   FROM ventas v
                  LEFT JOIN clientes c ON v.idClientes = c.idClientes
                          LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
                          LEFT JOIN sedes s ON v.idSede = s.idSede
                          LEFT JOIN pedidos p ON p.idVenta = v.idVentas
                           LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
                           LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
                           LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario
                   ORDER BY v.FechaVenta DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapVenta(rs));
    }

    // ======================================
    //      BUSCAR POR ID
    // ======================================
    @Override
    public Venta buscarPorId(int idVenta) {
        try {
            String sql = """
                SELECT v.*,
                             COALESCE(
                                         NULLIF(v.nombreCliente, ''),
                                         CASE
                                             WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                             ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                                         END,
                                         'Cliente ocasional'
                                     ) AS nombreCliente,

                                     COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                                     COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
                             u.NombreUsuario AS nombreUsuario,
                                     s.nombre AS nombreSede,
                                     p.idPedido AS idPedido,
                               p.idEstadoPedido AS idEstadoPedido,
                               p.codigoRecojo AS codigoRecojo,
                               p.fechaRecojoProgramada AS fechaRecojoProgramada,
                                       p.fechaPreparando AS fechaPreparando,
                                       p.fechaListo AS fechaListo,
                                       p.fechaEntregado AS fechaEntregado,
                                       p.idUsuarioPreparando AS idUsuarioPreparando,
                                       p.idUsuarioEntrega AS idUsuarioEntrega,
                                       p.observacion AS observacionEntrega,
                                       up.NombreUsuario AS nombreUsuarioPreparando,
                                       ue.NombreUsuario AS nombreUsuarioEntrega,
                                       ep.nombreEstado AS nombreEstadoPedido,

                                              TIMESTAMPDIFF(MINUTE, p.fechaPreparando, p.fechaListo) AS minutosAlistado,

                                                                                                                                   TIMESTAMPDIFF(MINUTE, v.FechaVenta, p.fechaEntregado) AS minutosTotalProceso,

                                                                                                                                   CASE
                                                                                                                                       WHEN p.fechaEntregado IS NOT NULL
                                                                                                                                            AND p.fechaPreparando IS NULL
                                                                                                                                            AND p.fechaListo IS NULL
                                                                                                                                       THEN 'INMEDIATA'
                                                                                                                                       ELSE 'PROGRAMADA'
                                                                                                                                   END AS tipoEntregaHistorial
                        FROM ventas v
                      LEFT JOIN clientes c ON v.idClientes = c.idClientes
                              LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
                              LEFT JOIN sedes s ON v.idSede = s.idSede
                              LEFT JOIN pedidos p ON p.idVenta = v.idVentas
                                LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
                                LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
                                LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario
                                WHERE v.idVentas = ?
            """;

            return jdbc.queryForObject(sql, (rs, rowNum) -> mapVenta(rs), idVenta);

        } catch (Exception e) {
            System.err.println("⚠️ Venta no encontrada en DB (ID = " + idVenta + ")");
            return null;
        }
    }

    @Override
    public Venta buscarPedidoEntregaPorId(Integer idPedido) {

        try {

            String sql = """
            SELECT v.*,

                   COALESCE(
                       NULLIF(v.nombreCliente, ''),
                       CASE
                           WHEN c.idTipoDocumento = 2 THEN c.razon_social
                           ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                       END,
                       'Cliente ocasional'
                   ) AS nombreCliente,

                   COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                   COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,

                   u.NombreUsuario AS nombreUsuario,
                   s.nombre AS nombreSede,

                   p.idPedido AS idPedido,
                   p.idEstadoPedido AS idEstadoPedido,
                   p.codigoRecojo AS codigoRecojo,
                   p.fechaRecojoProgramada AS fechaRecojoProgramada,
                   p.fechaPreparando AS fechaPreparando,
                   p.fechaListo AS fechaListo,
                   p.fechaEntregado AS fechaEntregado,
                   p.idUsuarioPreparando AS idUsuarioPreparando,
                   p.idUsuarioEntrega AS idUsuarioEntrega,
                   p.observacion AS observacionEntrega,

                   up.NombreUsuario AS nombreUsuarioPreparando,
                   ue.NombreUsuario AS nombreUsuarioEntrega,
                   ep.nombreEstado AS nombreEstadoPedido,

                   TIMESTAMPDIFF(MINUTE, p.fechaPreparando, p.fechaListo) AS minutosAlistado,
                   TIMESTAMPDIFF(MINUTE, v.FechaVenta, p.fechaEntregado) AS minutosTotalProceso,

                   CASE
                       WHEN p.fechaEntregado IS NOT NULL
                            AND p.fechaPreparando IS NULL
                            AND p.fechaListo IS NULL
                       THEN 'INMEDIATA'
                       ELSE 'PROGRAMADA'
                   END AS tipoEntregaHistorial

            FROM pedidos p
            INNER JOIN ventas v ON p.idVenta = v.idVentas
            LEFT JOIN clientes c ON v.idClientes = c.idClientes
            LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
            LEFT JOIN sedes s ON v.idSede = s.idSede
            LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
            LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
            LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario

            WHERE p.idPedido = ?

            LIMIT 1
        """;

            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> mapVenta(rs),
                    idPedido
            );

        } catch (Exception e) {
            System.err.println("⚠️ Pedido no encontrado en DB (ID Pedido = " + idPedido + ")");
            return null;
        }
    }

    // ======================================
    //   REGISTRAR BORRADOR DE VENTA
    // ======================================
    @Override
    public int registrarBorrador(Venta v) {

        String sql = """
       INSERT INTO ventas (
                       FechaVenta,
                       TipoComprobante,
                       NroComprobante,
                       idClientes,
                       nombreCliente,
                       documentoCliente,
                       telefonoCliente,
                       idUsuario,
                       idSede,
                       FormaPago,
                       metodoPago,
                       EstadoVenta,
                       TotalVenta,
                       Deuda,
                       pagoInicial,
                       numeroCuotas,
                       frecuenciaCuotas,
                       fechaPrimeraCuota
                   )
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        return jdbc.update(sql,

                LocalDateTime.now(),
                v.getTipoComprobante(),
                v.getNroComprobante(),
                v.getIdCliente(),
                v.getNombreCliente(),
                v.getDocumentoCliente(),
                v.getTelefonoCliente(),
                v.getIdUsuario(),
                v.getIdSede(),
                v.getFormaPago(),
                v.getMetodoPago(),
                "BORRADOR",
                v.getTotalVenta(),
                v.getDeuda(),
                v.getPagoInicial(),
                v.getNumeroCuotas(),
                v.getFrecuenciaCuotas(),
                v.getFechaPrimeraCuota()
        );
    }

    @Override
    public int registrarYObtenerId(Venta v) {
        registrarBorrador(v);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    // ======================================
    //     ACTUALIZAR VENTA
    // ======================================
    @Override
    public int actualizarVenta(Venta v) {
        String sql = """
        UPDATE ventas
        SET FechaVenta=?,
            TipoComprobante=?,
            NroComprobante=?,
            FormaPago=?,
            metodoPago=?,
            EstadoVenta=?,
            TotalVenta=?,
            Deuda=?,
            pagoInicial=?,
            numeroCuotas=?,
            frecuenciaCuotas=?,
            fechaPrimeraCuota=?,
           idClientes=?,
                       nombreCliente=?,
                       documentoCliente=?,
                       telefonoCliente=?,
                       idUsuario=?,
                       idSede=?,
            canal_venta=COALESCE(?, canal_venta, 'PRESENCIAL')
        WHERE idVentas=?
    """;

        return jdbc.update(sql,
                v.getFechaVenta(),
                v.getTipoComprobante(),
                v.getNroComprobante(),
                v.getFormaPago(),
                v.getMetodoPago(),
                v.getEstadoVenta(),
                v.getTotalVenta(),
                v.getDeuda(),
                v.getPagoInicial(),
                v.getNumeroCuotas(),
                v.getFrecuenciaCuotas(),
                v.getFechaPrimeraCuota(),
                v.getIdCliente(),
                v.getNombreCliente(),
                v.getDocumentoCliente(),
                v.getTelefonoCliente(),
                v.getIdUsuario(),
                v.getIdSede(),
                v.getCanalVenta(),
                v.getIdVentas()
        );
    }

    // ======================================
    //       ACTUALIZAR ESTADO
    // ======================================
    @Override
    public int actualizarEstado(int idVenta, String estado) {
        String sql = "UPDATE ventas SET EstadoVenta=? WHERE idVentas=?";
        return jdbc.update(sql, estado, idVenta);
    }

    @Override
    public void registrarEntregaInmediata(int idVenta) {

        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pedidos WHERE idVenta = ?",
                Integer.class,
                idVenta
        );

        if (existe != null && existe > 0) {

            String update = """
            UPDATE pedidos
            SET idEstadoPedido = (
                    SELECT idEstadoPedido
                    FROM estadopedido
                    WHERE nombreEstado = 'ENTREGADO'
                    LIMIT 1
                ),
                fechaRecojoProgramada = NOW(),
                fechaEntregado = NOW()
            WHERE idVenta = ?
        """;

            jdbc.update(update, idVenta);

        } else {

            String insert = """
            INSERT INTO pedidos (
                idVenta,
                idEstadoPedido,
                codigoRecojo,
                fechaRecojoProgramada,
                fechaEntregado
            )
            VALUES (
                ?,
                (SELECT idEstadoPedido FROM estadopedido WHERE nombreEstado = 'ENTREGADO' LIMIT 1),
                ?,
                NOW(),
                NOW()
            )
        """;

            jdbc.update(insert, idVenta, generarCodigoRecojoUnico());
        }
    }

    @Override
    public void registrarEntregaProgramada(int idVenta,
                                           LocalDateTime fechaRecojoProgramada) {

        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pedidos WHERE idVenta = ?",
                Integer.class,
                idVenta
        );

        if (existe != null && existe > 0) {

            String update = """
            UPDATE pedidos
            SET idEstadoPedido = (
                    SELECT idEstadoPedido
                    FROM estadopedido
                    WHERE nombreEstado = 'PENDIENTE'
                    LIMIT 1
                ),
                fechaRecojoProgramada = ?,
                fechaPreparando = NULL,
                fechaListo = NULL,
                fechaEntregado = NULL,
                idUsuarioPreparando = NULL,
                idUsuarioEntrega = NULL
            WHERE idVenta = ?
        """;

            jdbc.update(update, fechaRecojoProgramada, idVenta);

        } else {

            String insert = """
            INSERT INTO pedidos (
                idVenta,
                idEstadoPedido,
                codigoRecojo,
                fechaRecojoProgramada
            )
            VALUES (
                ?,
                (SELECT idEstadoPedido FROM estadopedido WHERE nombreEstado = 'PENDIENTE' LIMIT 1),
                ?,
                ?
            )
        """;

            jdbc.update(
                    insert,
                    idVenta,
                    generarCodigoRecojoUnico(),
                    fechaRecojoProgramada
            );
        }
    }
    // ======================================
    //       ACTUALIZAR DEUDA
    // ======================================
    @Override
    public int actualizarDeuda(int idVenta, BigDecimal deuda) {
        String sql = "UPDATE ventas SET Deuda=? WHERE idVentas=?";
        return jdbc.update(sql, deuda, idVenta);
    }

    // ======================================
    //   REGISTRAR PAGO INICIAL (CRÉDITO)
    // ======================================
    @Override
    public int registrarPagoInicial(int idVenta, BigDecimal pagoInicial) {
        String sql = "UPDATE ventas SET pagoInicial=? WHERE idVentas=?";
        return jdbc.update(sql, pagoInicial, idVenta);
    }

    // ======================================
    //   GENERAR SIGUIENTE COMPROBANTE
    // ======================================
    @Override
    public String obtenerSiguienteComprobante(String tipo) {
        String sql = """
            SELECT NroComprobante
            FROM ventas
            WHERE TipoComprobante=?
            ORDER BY idVentas DESC
            LIMIT 1
        """;

        try {
            String ultimo = jdbc.queryForObject(sql, String.class, tipo);

            if (ultimo == null) return "001-000001";

            String[] partes = ultimo.split("-");
            int correlativo = Integer.parseInt(partes[1]) + 1;

            return partes[0] + "-" + String.format("%06d", correlativo);

        } catch (Exception e) {
            return "001-000001";
        }
    }

    // ======================================
    //   ELIMINAR BORRADOR
    // ======================================
    @Override
    public int eliminarVenta(int idVenta) {
        String sql = "DELETE FROM ventas WHERE idVentas=? AND EstadoVenta='BORRADOR'";
        return jdbc.update(sql, idVenta);
    }

    // ======================================
    //   ANULAR VENTA
    // ======================================
    @Override
    public int anularVenta(int idVenta) {
        return jdbc.update("UPDATE ventas SET EstadoVenta='ANULADA' WHERE idVentas=?", idVenta);
    }

    // ======================================
    //   OBTENER ID USUARIO
    // ======================================
    @Override
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        try {
            return jdbc.queryForObject(
                    "SELECT idUsuario FROM usuarios WHERE NombreUsuario=?",
                    Integer.class,
                    nombreUsuario
            );
        } catch (Exception e) {
            return null;
        }
    }
    @Override
    public int registrarVentaOnline(Venta v) {

        String sql = """
        INSERT INTO ventas (
            FechaVenta,
            TipoComprobante,
            NroComprobante,
            idClientes,
            idUsuario,
            idSede,
            canal_venta,
            FormaPago,
            EstadoVenta,
            EstadoPago,
            TotalVenta,
            Deuda,
            pagoInicial,
            numeroCuotas,
            CodigoTicket,
            FechaRecojo,
            ObservacionRecojo,
            IdTransaccionPasarela
        )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        return jdbc.update(sql,
                v.getFechaVenta(),
                v.getTipoComprobante(),
                v.getNroComprobante(),
                v.getIdCliente(),
                v.getIdUsuario(),

                v.getIdSede() != null
                        ? v.getIdSede()
                        : null,

                v.getCanalVenta(),
                v.getFormaPago(),
                v.getEstadoVenta(),
                v.getEstadoPago(),
                v.getTotalVenta(),
                v.getDeuda(),
                v.getPagoInicial(),
                v.getNumeroCuotas(),
                v.getCodigoTicket(),
                v.getFechaRecojo(),
                v.getObservacionRecojo(),
                v.getIdTransaccionPasarela()
        );
    }

    @Override
    public int registrarVentaOnlineAprobada(Venta v) {
        String sql = """
        INSERT INTO ventas (
            FechaVenta,
            TipoComprobante,
            NroComprobante,
            idClientes,
            nombreCliente,
            documentoCliente,
            telefonoCliente,
            idUsuario,
            idSede,
            idOrdenOnline,
            canal_venta,
            FormaPago,
            metodoPago,
            EstadoVenta,
            EstadoPago,
            TotalVenta,
            Deuda,
            pagoInicial,
            numeroCuotas,
            CodigoTicket,
            FechaRecojo,
            ObservacionRecojo,
            IdTransaccionPasarela
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, toTimestamp(v.getFechaVenta()));
            ps.setString(2, v.getTipoComprobante());
            ps.setString(3, v.getNroComprobante());
            ps.setObject(4, v.getIdCliente());
            ps.setString(5, v.getNombreCliente());
            ps.setString(6, v.getDocumentoCliente());
            ps.setString(7, v.getTelefonoCliente());
            ps.setObject(8, v.getIdUsuario());
            ps.setObject(9, v.getIdSede());
            ps.setObject(10, v.getIdOrdenOnline());
            ps.setString(11, v.getCanalVenta());
            ps.setString(12, v.getFormaPago());
            ps.setString(13, v.getMetodoPago());
            ps.setString(14, v.getEstadoVenta());
            ps.setString(15, v.getEstadoPago());
            ps.setBigDecimal(16, v.getTotalVenta());
            ps.setBigDecimal(17, v.getDeuda());
            ps.setBigDecimal(18, v.getPagoInicial());
            ps.setInt(19, v.getNumeroCuotas());
            ps.setString(20, v.getCodigoTicket());
            ps.setTimestamp(21, toTimestamp(v.getFechaRecojo()));
            ps.setString(22, v.getObservacionRecojo());
            ps.setString(23, v.getIdTransaccionPasarela());
            return ps;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).intValue();
    }

    @Override
    public Venta buscarPorIdOrdenOnline(Integer idOrdenOnline) {
        String sql = """
        SELECT v.*,
               s.nombre AS nombreSede,
               u.NombreUsuario AS nombreUsuario,
               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada
        FROM ventas v
        LEFT JOIN sedes s ON s.idSede = v.idSede
        LEFT JOIN usuarios u ON u.idUsuario = v.idUsuario
        LEFT JOIN pedidos p ON p.idVenta = v.idVentas
        WHERE v.idOrdenOnline = ?
        LIMIT 1
        """;
        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> mapVenta(rs), idOrdenOnline);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Override
    public Venta buscarPorCodigoTicket(String codigoTicket) {
        if (codigoTicket == null || codigoTicket.isBlank()) {
            return null;
        }

        String sql = """
            SELECT v.*,
                   COALESCE(
                       NULLIF(v.nombreCliente, ''),
                       CASE
                           WHEN c.idTipoDocumento = 2 THEN c.razon_social
                           ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                       END,
                       'Cliente ocasional'
                   ) AS nombreCliente,
                   COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                   COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
                   u.NombreUsuario AS nombreUsuario,
                   s.nombre AS nombreSede,
                   p.idPedido AS idPedido,
                   p.idEstadoPedido AS idEstadoPedido,
                   p.codigoRecojo AS codigoRecojo,
                   p.fechaRecojoProgramada AS fechaRecojoProgramada,
                   p.fechaPreparando AS fechaPreparando,
                   p.fechaListo AS fechaListo,
                   p.fechaEntregado AS fechaEntregado,
                   p.idUsuarioPreparando AS idUsuarioPreparando,
                   p.idUsuarioEntrega AS idUsuarioEntrega,
                   p.observacion AS observacionEntrega,
                   up.NombreUsuario AS nombreUsuarioPreparando,
                   ue.NombreUsuario AS nombreUsuarioEntrega,
                   ep.nombreEstado AS nombreEstadoPedido,
                   TIMESTAMPDIFF(MINUTE, p.fechaPreparando, p.fechaListo) AS minutosAlistado,
                   TIMESTAMPDIFF(MINUTE, v.FechaVenta, p.fechaEntregado) AS minutosTotalProceso,
                   CASE
                       WHEN p.fechaEntregado IS NOT NULL
                            AND p.fechaPreparando IS NULL
                            AND p.fechaListo IS NULL
                       THEN 'INMEDIATA'
                       ELSE 'PROGRAMADA'
                   END AS tipoEntregaHistorial
              FROM ventas v
              LEFT JOIN clientes c ON v.idClientes = c.idClientes
              LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
              LEFT JOIN sedes s ON v.idSede = s.idSede
              LEFT JOIN pedidos p ON p.idVenta = v.idVentas
              LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
              LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
              LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario
             WHERE v.CodigoTicket = ?
               AND v.CodigoTicket IS NOT NULL
               AND TRIM(v.CodigoTicket) <> ''
             LIMIT 2
        """;

        List<Venta> ventas = jdbc.query(sql, (rs, rowNum) -> mapVenta(rs), codigoTicket.trim());
        return ventas.size() == 1 ? ventas.get(0) : null;
    }

    @Override
    public Integer obtenerUltimoId() {
        return jdbc.queryForObject(
                "SELECT MAX(idVentas) FROM ventas",
                Integer.class
        );
    }

    @Override
    public List<Venta> filtrar(
            String canal,
            String estado,
            String estadoEntrega,
            String buscar,
            String fechaDesde,
            String fechaHasta) {

        String sql = """
        SELECT v.*,
                        COALESCE(
                                        NULLIF(v.nombreCliente, ''),
                                        CASE
                                            WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                            ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                                        END,
                                        'Cliente ocasional'
                                    ) AS nombreCliente,

                                    COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                                    COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
                     u.NombreUsuario AS nombreUsuario,
                                 s.nombre AS nombreSede,
                                 p.idPedido AS idPedido,
                           p.idEstadoPedido AS idEstadoPedido,
                           p.codigoRecojo AS codigoRecojo,
                           p.fechaRecojoProgramada AS fechaRecojoProgramada,
                                       p.fechaPreparando AS fechaPreparando,
                                       p.fechaListo AS fechaListo,
                                       p.fechaEntregado AS fechaEntregado,
                                       p.idUsuarioPreparando AS idUsuarioPreparando,
                                       p.idUsuarioEntrega AS idUsuarioEntrega,
                                       p.observacion AS observacionEntrega,
                                       up.NombreUsuario AS nombreUsuarioPreparando,
                                       ue.NombreUsuario AS nombreUsuarioEntrega,
                                       ep.nombreEstado AS nombreEstadoPedido
                    FROM ventas v
                   LEFT JOIN clientes c ON v.idClientes = c.idClientes
                               LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
                               LEFT JOIN sedes s ON v.idSede = s.idSede
                               LEFT JOIN pedidos p ON p.idVenta = v.idVentas
                                LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
                                LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
                                LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario
                    WHERE 1=1
    """;

        List<Object> params = new ArrayList<>();

        if (canal != null && !canal.isBlank() && !"TODOS".equalsIgnoreCase(canal)) {
            sql += " AND v.canal_venta = ?";
            params.add(canal);
        }


/* ======================================
   FILTRO ESTADO VENTA
====================================== */
        if (estado != null
                && !estado.isBlank()
                && !"TODOS".equalsIgnoreCase(estado)) {

            sql += " AND v.EstadoVenta = ?";
            params.add(estado);
        }

        if (estadoEntrega != null && !estadoEntrega.isBlank() && !"TODOS".equalsIgnoreCase(estadoEntrega)) {

            String estadoEntregaReal = estadoEntrega;

            if ("LISTO".equalsIgnoreCase(estadoEntrega)) {
                estadoEntregaReal = "LISTO_RECOJO";
            }

            sql += " AND ep.nombreEstado = ?";
            params.add(estadoEntregaReal);
        }

        if (fechaDesde != null && !fechaDesde.isBlank()) {
            sql += " AND DATE(v.FechaVenta) >= ?";
            params.add(fechaDesde);
        }

        if (fechaHasta != null && !fechaHasta.isBlank()) {
            sql += " AND DATE(v.FechaVenta) <= ?";
            params.add(fechaHasta);
        }

        if (buscar != null && !buscar.isBlank()) {

            String b = "%" + buscar.trim() + "%";

            sql += """
        AND (
            v.NroComprobante LIKE ?
            OR v.TipoComprobante LIKE ?
            OR v.FormaPago LIKE ?
            OR v.metodoPago LIKE ?
            OR v.EstadoVenta LIKE ?
            OR CASE
                WHEN v.EstadoVenta = 'CREDITO' THEN 'POR COBRAR'
                ELSE v.EstadoVenta
            END LIKE ?
            OR v.canal_venta LIKE ?
           OR v.nombreCliente LIKE ?
                                                     OR v.documentoCliente LIKE ?
                                                     OR v.telefonoCliente LIKE ?
                                                     OR c.Nombres LIKE ?
                                                     OR c.Apellidos LIKE ?
                                                     OR CONCAT_WS(' ', c.Nombres, c.Apellidos) LIKE ?
                                                     OR c.razon_social LIKE ?
                                                     OR c.nro_documento LIKE ?
                                                     OR c.Telefono LIKE ?
                                                     OR u.NombreUsuario LIKE ?
        )
    """;

            params.add(b); // NroComprobante
            params.add(b); // TipoComprobante
            params.add(b); // FormaPago
            params.add(b); // metodoPago
            params.add(b); // EstadoVenta
            params.add(b); // POR COBRAR
            params.add(b); // canal
            params.add(b); // venta.nombreCliente
            params.add(b); // venta.documentoCliente
            params.add(b); // venta.telefonoCliente
            params.add(b); // nombres
            params.add(b); // apellidos
            params.add(b); // nombre completo
            params.add(b); // razon social
            params.add(b); // documento cliente tabla clientes
            params.add(b); // telefono cliente tabla clientes
            params.add(b); // usuario
        }

        sql += " ORDER BY v.FechaVenta DESC";

        return jdbc.query(sql, params.toArray(), (rs, rowNum) -> mapVenta(rs));
    }

    public void marcarCreditoPagado(Integer idVenta) {
        String sql = """
        UPDATE ventas
        SET EstadoVenta = 'PAGADO',
            Deuda = 0
        WHERE idVentas = ?
          AND FormaPago = 'CREDITO'
    """;

        jdbc.update(sql, idVenta);
    }

    @Override
    public List<Venta> filtrarPorSede(
            Integer idSede,
            String canal,
            String estado,
            String estadoEntrega,
            String buscar,
            String fechaDesde,
            String fechaHasta) {

        String sql = """
        SELECT v.*,
              COALESCE(
                              NULLIF(v.nombreCliente, ''),
                              CASE
                                  WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                  ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                              END,
                              'Cliente ocasional'
                          ) AS nombreCliente,

                          COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                          COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
               u.NombreUsuario AS nombreUsuario,
               s.nombre AS nombreSede,
               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,
               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,
               ep.nombreEstado AS nombreEstadoPedido
        FROM ventas v
        LEFT JOIN clientes c ON v.idClientes = c.idClientes
        LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
        LEFT JOIN sedes s ON v.idSede = s.idSede
        LEFT JOIN pedidos p ON p.idVenta = v.idVentas
        LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
        LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario
        WHERE v.idSede = ?
    """;

        List<Object> params = new ArrayList<>();
        params.add(idSede);

        if (canal != null && !canal.isBlank() && !"TODOS".equalsIgnoreCase(canal)) {
            sql += " AND v.canal_venta = ?";
            params.add(canal);
        }

        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            sql += " AND v.EstadoVenta = ?";
            params.add(estado);
        }

        if (estadoEntrega != null && !estadoEntrega.isBlank() && !"TODOS".equalsIgnoreCase(estadoEntrega)) {

            String estadoEntregaReal = estadoEntrega;

            if ("LISTO".equalsIgnoreCase(estadoEntrega)) {
                estadoEntregaReal = "LISTO_RECOJO";
            }

            sql += " AND ep.nombreEstado = ?";
            params.add(estadoEntregaReal);
        }

        if (fechaDesde != null && !fechaDesde.isBlank()) {
            sql += " AND DATE(v.FechaVenta) >= ?";
            params.add(fechaDesde);
        }

        if (fechaHasta != null && !fechaHasta.isBlank()) {
            sql += " AND DATE(v.FechaVenta) <= ?";
            params.add(fechaHasta);
        }

        if (buscar != null && !buscar.isBlank()) {

            String b = "%" + buscar.trim() + "%";

            sql += """
            AND (
                v.NroComprobante LIKE ?
                OR v.TipoComprobante LIKE ?
                OR v.FormaPago LIKE ?
                OR v.metodoPago LIKE ?
                OR v.EstadoVenta LIKE ?
                OR CASE
                    WHEN v.EstadoVenta = 'CREDITO' THEN 'POR COBRAR'
                    ELSE v.EstadoVenta
                END LIKE ?
                OR v.canal_venta LIKE ?
               OR v.nombreCliente LIKE ?
                           OR v.documentoCliente LIKE ?
                           OR v.telefonoCliente LIKE ?
                           OR c.Nombres LIKE ?
                           OR c.Apellidos LIKE ?
                           OR CONCAT_WS(' ', c.Nombres, c.Apellidos) LIKE ?
                           OR c.razon_social LIKE ?
                           OR c.nro_documento LIKE ?
                           OR c.Telefono LIKE ?
                           OR u.NombreUsuario LIKE ?
                           OR s.nombre LIKE ?
            )
        """;

            params.add(b); // NroComprobante
            params.add(b); // TipoComprobante
            params.add(b); // FormaPago
            params.add(b); // metodoPago
            params.add(b); // EstadoVenta
            params.add(b); // POR COBRAR
            params.add(b); // canal

            params.add(b); // ventas.nombreCliente
            params.add(b); // ventas.documentoCliente
            params.add(b); // ventas.telefonoCliente

            params.add(b); // clientes.Nombres
            params.add(b); // clientes.Apellidos
            params.add(b); // nombre completo cliente
            params.add(b); // razon social
            params.add(b); // documento cliente
            params.add(b); // telefono cliente
            params.add(b); // usuario
            params.add(b); // sede
        }

        sql += " ORDER BY v.FechaVenta DESC";

        return jdbc.query(sql, params.toArray(), (rs, rowNum) -> mapVenta(rs));
    }

    public void disminuirDeudaCredito(Integer idVenta, BigDecimal montoPago) {
        String sql = """
        UPDATE ventas
        SET Deuda = GREATEST(COALESCE(Deuda, 0) - ?, 0)
        WHERE idVentas = ?
          AND FormaPago = 'CREDITO'
    """;

        jdbc.update(sql, montoPago, idVenta);
    }

    // ======================================
// LISTAR PENDIENTES
// ======================================

    @Override
    public List<Venta> listarPendientesEntrega() {

        String sql = """
        SELECT v.*,
               COALESCE(
                               NULLIF(v.nombreCliente, ''),
                               CASE
                                   WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                   ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                               END,
                               'Cliente ocasional'
                           ) AS nombreCliente,

                           COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                           COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
               u.NombreUsuario AS nombreUsuario,

               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,

               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,
               ep.nombreEstado AS nombreEstadoPedido

        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        LEFT JOIN clientes c ON v.idClientes = c.idClientes
        LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
        LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
        LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario

                WHERE ep.nombreEstado = 'PENDIENTE'

        ORDER BY
            COALESCE(p.fechaRecojoProgramada, p.fechaPedido) ASC,
            p.fechaPedido ASC
    """;

        return jdbc.query(sql, (rs, rowNum) -> mapVenta(rs));
    }

    @Override
    public List<Venta> listarPendientesEntregaPorSede(Integer idSede) {

        String sql = """
    SELECT v.*,
           COALESCE(
               NULLIF(v.nombreCliente, ''),
               CASE
                   WHEN c.idTipoDocumento = 2 THEN c.razon_social
                   ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
               END,
               'Cliente ocasional'
           ) AS nombreCliente,

           COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
           COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,

           u.NombreUsuario AS nombreUsuario,
           s.nombre AS nombreSede,

           p.idPedido AS idPedido,
           p.idEstadoPedido AS idEstadoPedido,
           p.codigoRecojo AS codigoRecojo,
           p.fechaRecojoProgramada AS fechaRecojoProgramada,
           p.fechaPreparando AS fechaPreparando,
           p.fechaListo AS fechaListo,
           p.fechaEntregado AS fechaEntregado,
           p.idUsuarioPreparando AS idUsuarioPreparando,
           p.idUsuarioEntrega AS idUsuarioEntrega,
           p.observacion AS observacionEntrega,

           up.NombreUsuario AS nombreUsuarioPreparando,
           ue.NombreUsuario AS nombreUsuarioEntrega,
           ep.nombreEstado AS nombreEstadoPedido

    FROM pedidos p
    INNER JOIN ventas v ON p.idVenta = v.idVentas
    LEFT JOIN clientes c ON v.idClientes = c.idClientes
    LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
    LEFT JOIN sedes s ON v.idSede = s.idSede
    LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
    LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
    LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario

        WHERE ep.nombreEstado = 'PENDIENTE'
      AND v.idSede = ?

    ORDER BY
        COALESCE(p.fechaRecojoProgramada, p.fechaPedido) ASC,
        p.fechaPedido ASC
""";

        return jdbc.query(sql, (rs, rowNum) -> mapVenta(rs), idSede);
    }
// ======================================
// PEDIDO ACTUAL
// ======================================

    @Override
    public Venta obtenerPedidoActualPorUsuario(Integer idUsuario) {

        try {

            String sql = """
        SELECT v.*,

              COALESCE(
                                  NULLIF(v.nombreCliente, ''),
                                  CASE
                                      WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                      ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                                  END,
                                  'Cliente ocasional'
                              ) AS nombreCliente,

                              COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                              COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
               u.NombreUsuario AS nombreUsuario,

               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,

               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,

               ep.nombreEstado AS nombreEstadoPedido

        FROM pedidos p

        INNER JOIN ventas v
            ON p.idVenta = v.idVentas

        LEFT JOIN clientes c
            ON v.idClientes = c.idClientes

        LEFT JOIN usuarios u
            ON v.idUsuario = u.idUsuario

        LEFT JOIN estadopedido ep
            ON p.idEstadoPedido = ep.idEstadoPedido

        LEFT JOIN usuarios up
            ON p.idUsuarioPreparando = up.idUsuario

        LEFT JOIN usuarios ue
            ON p.idUsuarioEntrega = ue.idUsuario

        WHERE p.idUsuarioPreparando = ?
          AND ep.nombreEstado = 'PREPARANDO'

        LIMIT 1
    """;

            Venta venta = jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> mapVenta(rs),
                    idUsuario
            );

            if (venta != null) {

                String sqlDetalle = """
        SELECT dv.idDetalleVenta,
               dv.idVentas,
               dv.idProducto,
               dv.idUnidad,
               dv.Cantidad,
               dv.PrecioUnitario,
               dv.Descuento,

               p.NombreProducto,
               p.foto,

               u.NombreUnidad

        FROM detalleventa dv

        INNER JOIN productos p
            ON dv.idProducto = p.idProducto

        INNER JOIN unidades u
            ON dv.idUnidad = u.idUnidad

        WHERE dv.idVentas = ?
    """;

                venta.setDetalles(
                        jdbc.query(sqlDetalle, (rs, rowNum) -> {

                            DetalleVenta d = new DetalleVenta();

                            d.setIdDetalleVenta(rs.getInt("idDetalleVenta"));
                            d.setIdVenta(rs.getInt("idVentas"));
                            d.setIdProducto(rs.getInt("idProducto"));
                            d.setIdUnidad(rs.getInt("idUnidad"));

                            d.setCantidad(rs.getBigDecimal("Cantidad"));
                            d.setPrecioUnitario(rs.getBigDecimal("PrecioUnitario"));
                            d.setDescuento(rs.getBigDecimal("Descuento"));

                            d.setNombreProducto(rs.getString("NombreProducto"));
                            d.setFoto(rs.getString("foto"));
                            d.setNombreUnidad(rs.getString("NombreUnidad"));

                            return d;

                        }, venta.getIdVentas())
                );
            }

            return venta;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Venta obtenerPedidoActualPorUsuarioYSede(Integer idUsuario, Integer idSede) {

        Venta venta = obtenerPedidoActualPorUsuario(idUsuario);

        if (venta == null) {
            return null;
        }

        if (venta.getIdSede() == null || !venta.getIdSede().equals(idSede)) {
            return null;
        }

        return venta;
    }
// ======================================
// PEDIDOS LISTOS
// ======================================
@Override
public List<Venta> listarPedidosListos() {

    String sql = """
        SELECT v.*,

              COALESCE(
                          NULLIF(v.nombreCliente, ''),
                          CASE
                              WHEN c.idTipoDocumento = 2 THEN c.razon_social
                              ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                          END,
                          'Cliente ocasional'
                      ) AS nombreCliente,

                      COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                      COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
               u.NombreUsuario AS nombreUsuario,

               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,

               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,

               ep.nombreEstado AS nombreEstadoPedido

        FROM pedidos p

        INNER JOIN ventas v
            ON p.idVenta = v.idVentas

        LEFT JOIN clientes c
            ON v.idClientes = c.idClientes

        LEFT JOIN usuarios u
            ON v.idUsuario = u.idUsuario

        LEFT JOIN estadopedido ep
            ON p.idEstadoPedido = ep.idEstadoPedido

        LEFT JOIN usuarios up
            ON p.idUsuarioPreparando = up.idUsuario

        LEFT JOIN usuarios ue
            ON p.idUsuarioEntrega = ue.idUsuario

       WHERE ep.nombreEstado = 'LISTO_RECOJO'

        ORDER BY p.fechaListo ASC
    """;

    return jdbc.query(sql,
            (rs, rowNum) -> mapVenta(rs));
}

    @Override
    public List<Venta> listarPedidosListosPorSede(Integer idSede) {

        String sql = """
        SELECT v.*,

               COALESCE(
                   NULLIF(v.nombreCliente, ''),
                   CASE
                       WHEN c.idTipoDocumento = 2 THEN c.razon_social
                       ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                   END,
                   'Cliente ocasional'
               ) AS nombreCliente,

               COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
               COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,

               u.NombreUsuario AS nombreUsuario,
               s.nombre AS nombreSede,

               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,

               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,

               ep.nombreEstado AS nombreEstadoPedido

        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        LEFT JOIN clientes c ON v.idClientes = c.idClientes
        LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
        LEFT JOIN sedes s ON v.idSede = s.idSede
        LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
        LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario

        WHERE ep.nombreEstado = 'LISTO_RECOJO'
          AND v.idSede = ?

        ORDER BY p.fechaListo ASC
    """;

        return jdbc.query(sql, (rs, rowNum) -> mapVenta(rs), idSede);
    }

// ======================================
// BUSCAR POR CODIGO
// ======================================
@Override
public Venta buscarPorCodigoRecojo(String codigo) {

    try {

        String sql = """
        SELECT v.*,

               COALESCE(
                               NULLIF(v.nombreCliente, ''),
                               CASE
                                   WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                   ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                               END,
                               'Cliente ocasional'
                           ) AS nombreCliente,

                           COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                           COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
               u.NombreUsuario AS nombreUsuario,

               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,

               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,

               ep.nombreEstado AS nombreEstadoPedido

        FROM pedidos p

        INNER JOIN ventas v
            ON p.idVenta = v.idVentas

        LEFT JOIN clientes c
            ON v.idClientes = c.idClientes

        LEFT JOIN usuarios u
            ON v.idUsuario = u.idUsuario

        LEFT JOIN estadopedido ep
            ON p.idEstadoPedido = ep.idEstadoPedido

        LEFT JOIN usuarios up
            ON p.idUsuarioPreparando = up.idUsuario

        LEFT JOIN usuarios ue
            ON p.idUsuarioEntrega = ue.idUsuario

        WHERE p.codigoRecojo = ?

        LIMIT 1
    """;

        return jdbc.queryForObject(
                sql,
                (rs, rowNum) -> mapVenta(rs),
                codigo
        );

    } catch (Exception e) {
        return null;
    }
}

    @Override
    public Venta buscarPedidoListoParaEntrega(String texto) {

        try {

            String b = "%" + texto.trim() + "%";

            String sql = """
        SELECT v.*,

            COALESCE(
                                NULLIF(v.nombreCliente, ''),
                                CASE
                                    WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                    ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                                END,
                                'Cliente ocasional'
                            ) AS nombreCliente,

                            COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                            COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
               u.NombreUsuario AS nombreUsuario,

               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,

               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,

               ep.nombreEstado AS nombreEstadoPedido

        FROM pedidos p

        INNER JOIN ventas v
            ON p.idVenta = v.idVentas

        LEFT JOIN clientes c
            ON v.idClientes = c.idClientes

        LEFT JOIN usuarios u
            ON v.idUsuario = u.idUsuario

        LEFT JOIN estadopedido ep
            ON p.idEstadoPedido = ep.idEstadoPedido

        LEFT JOIN usuarios up
            ON p.idUsuarioPreparando = up.idUsuario

        LEFT JOIN usuarios ue
            ON p.idUsuarioEntrega = ue.idUsuario

        WHERE ep.nombreEstado = 'LISTO_RECOJO'

       AND (
                           v.NroComprobante LIKE ?
                           OR v.nombreCliente LIKE ?
                           OR v.documentoCliente LIKE ?
                           OR v.telefonoCliente LIKE ?
                           OR CONCAT_WS(' ', c.Nombres, c.Apellidos) LIKE ?
                           OR c.razon_social LIKE ?
                           OR c.nro_documento LIKE ?
                           OR c.Telefono LIKE ?
                       )

        ORDER BY p.fechaListo ASC

        LIMIT 1
    """;

            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> mapVenta(rs),
                    b, b, b, b, b, b, b, b
            );

        } catch (Exception e) {
            return null;
        }
    }
// ======================================
// ACEPTAR PEDIDO
// ======================================

    @Override
    public void aceptarPedido(Integer idPedido,
                              Integer idUsuario) {

        String sql = """
        UPDATE pedidos

        SET idEstadoPedido = (
                SELECT idEstadoPedido
                FROM estadopedido
                WHERE nombreEstado='PREPARANDO'
                LIMIT 1
            ),

            fechaPreparando = NOW(),

            idUsuarioPreparando = ?

        WHERE idPedido = ?
    """;

        jdbc.update(sql,
                idUsuario,
                idPedido);
    }

// ======================================
// MARCAR LISTO
// ======================================

    @Override
    public void marcarPedidoListo(Integer idPedido) {

        String sql = """
        UPDATE pedidos

        SET idEstadoPedido = (
                SELECT idEstadoPedido
                FROM estadopedido
                WHERE nombreEstado= 'LISTO_RECOJO'
                LIMIT 1
            ),

            fechaListo = NOW()

        WHERE idPedido = ?
    """;

        jdbc.update(sql, idPedido);
    }

// ======================================
// CONFIRMAR ENTREGA
// ======================================

    @Override
    public void confirmarEntrega(Integer idPedido,
                                 Integer idUsuarioEntrega) {

        String sql = """
        UPDATE pedidos

        SET idEstadoPedido = (
                SELECT idEstadoPedido
                FROM estadopedido
                WHERE nombreEstado='ENTREGADO'
                LIMIT 1
            ),

            fechaEntregado = NOW(),

            idUsuarioEntrega = ?

        WHERE idPedido = ?
    """;

        jdbc.update(sql,
                idUsuarioEntrega,
                idPedido);
    }

    public boolean existeCodigoRecojo(String codigo) {

        String sql = """
        SELECT COUNT(*)
        FROM pedidos
        WHERE codigoRecojo = ?
    """;

        Integer total = jdbc.queryForObject(
                sql,
                Integer.class,
                codigo
        );

        return total != null && total > 0;
    }
    private String generarCodigoRecojoUnico() {
        String codigo;

        do {
            codigo = String.valueOf(
                    100000 + new java.util.Random().nextInt(900000)
            );
        } while (existeCodigoRecojo(codigo));

        return codigo;
    }

    @Override
    public List<Venta> listarHistorialEntregas() {

        String sql = """
        SELECT v.*,

               COALESCE(
                               NULLIF(v.nombreCliente, ''),
                               CASE
                                   WHEN c.idTipoDocumento = 2 THEN c.razon_social
                                   ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                               END,
                               'Cliente ocasional'
                           ) AS nombreCliente,

                           COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                           COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,
               u.NombreUsuario AS nombreUsuario,

               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,

               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,
               ep.nombreEstado AS nombreEstadoPedido,

               TIMESTAMPDIFF(MINUTE, p.fechaPreparando, p.fechaListo) AS minutosAlistado,
                           TIMESTAMPDIFF(MINUTE, v.FechaVenta, p.fechaEntregado) AS minutosTotalProceso,

                                      CASE
                                                      WHEN p.fechaPreparando IS NOT NULL
                                                           OR p.fechaListo IS NOT NULL
                                                           OR p.idUsuarioPreparando IS NOT NULL
                                                      THEN 'DESPACHO'
                                                      ELSE 'INMEDIATA'
                                                  END AS tipoEntregaHistorial

        FROM ventas v
                    LEFT JOIN pedidos p ON p.idVenta = v.idVentas
        LEFT JOIN clientes c ON v.idClientes = c.idClientes
        LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
        LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
        LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario

       WHERE (
                       ep.nombreEstado IN ('ENTREGADO', 'ANULADO')
                       OR v.EstadoVenta = 'ANULADA'
                   )
       ORDER BY COALESCE(p.fechaEntregado, v.FechaVenta) DESC
    """;

        return jdbc.query(sql, (rs, rowNum) -> mapVenta(rs));
    }

    @Override
    public long contarEntregadosHoy() {

        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN estadopedido ep
            ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'ENTREGADO'
    """;

        Long total = jdbc.queryForObject(sql, Long.class);

        return total == null ? 0 : total;
    }

    @Override
    public void anularPedidoPorVenta(Integer idVenta) {
        String sql = """
        UPDATE pedidos
        SET idEstadoPedido = (
            SELECT idEstadoPedido
            FROM estadopedido
            WHERE nombreEstado = 'ANULADO'
            LIMIT 1
        )
        WHERE idVenta = ?
    """;

        jdbc.update(sql, idVenta);
    }

    @Override
    public long contarPreparando() {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'PREPARANDO'
    """;

        Long total = jdbc.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public long contarEntregasInmediatasHoy() {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'ENTREGADO'
          AND DATE(p.fechaEntregado) = CURDATE()
          AND (
              p.fechaRecojoProgramada IS NULL
              OR p.fechaRecojoProgramada <= v.FechaVenta
          )
    """;

        Long total = jdbc.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public long contarEntregasDespachoHoy() {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'ENTREGADO'
          AND DATE(p.fechaEntregado) = CURDATE()
          AND p.fechaRecojoProgramada > v.FechaVenta
    """;

        Long total = jdbc.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public long contarAnuladosHoy() {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'ANULADO'
    """;

        Long total = jdbc.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }
    @Override
    public Venta buscarPorCodigoRecojoYSede(String codigo, Integer idSede) {
        Venta venta = buscarPorCodigoRecojo(codigo);

        if (venta == null) {
            return null;
        }

        if (venta.getIdSede() == null || !venta.getIdSede().equals(idSede)) {
            return null;
        }

        return venta;
    }

    @Override
    public Venta buscarPedidoListoParaEntregaPorSede(String texto, Integer idSede) {

        try {

            String b = "%" + texto.trim() + "%";

            String sql = """
            SELECT v.*,

                   COALESCE(
                       NULLIF(v.nombreCliente, ''),
                       CASE
                           WHEN c.idTipoDocumento = 2 THEN c.razon_social
                           ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                       END,
                       'Cliente ocasional'
                   ) AS nombreCliente,

                   COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
                   COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,

                   u.NombreUsuario AS nombreUsuario,
                   s.nombre AS nombreSede,

                   p.idPedido AS idPedido,
                   p.idEstadoPedido AS idEstadoPedido,
                   p.codigoRecojo AS codigoRecojo,
                   p.fechaRecojoProgramada AS fechaRecojoProgramada,
                   p.fechaPreparando AS fechaPreparando,
                   p.fechaListo AS fechaListo,
                   p.fechaEntregado AS fechaEntregado,
                   p.idUsuarioPreparando AS idUsuarioPreparando,
                   p.idUsuarioEntrega AS idUsuarioEntrega,
                   p.observacion AS observacionEntrega,

                   up.NombreUsuario AS nombreUsuarioPreparando,
                   ue.NombreUsuario AS nombreUsuarioEntrega,

                   ep.nombreEstado AS nombreEstadoPedido

            FROM pedidos p
            INNER JOIN ventas v ON p.idVenta = v.idVentas
            LEFT JOIN clientes c ON v.idClientes = c.idClientes
            LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
            LEFT JOIN sedes s ON v.idSede = s.idSede
            LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
            LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
            LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario

            WHERE ep.nombreEstado = 'LISTO_RECOJO'
              AND v.idSede = ?

              AND (
                   v.NroComprobante LIKE ?
                   OR v.nombreCliente LIKE ?
                   OR v.documentoCliente LIKE ?
                   OR v.telefonoCliente LIKE ?
                   OR CONCAT_WS(' ', c.Nombres, c.Apellidos) LIKE ?
                   OR c.razon_social LIKE ?
                   OR c.nro_documento LIKE ?
                   OR c.Telefono LIKE ?
                   OR p.codigoRecojo LIKE ?
              )

            ORDER BY p.fechaListo ASC

            LIMIT 1
        """;

            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> mapVenta(rs),
                    idSede,
                    b, b, b, b, b, b, b, b, b
            );

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Venta> listarHistorialEntregasPorSede(Integer idSede) {

        String sql = """
        SELECT v.*,

               COALESCE(
                   NULLIF(v.nombreCliente, ''),
                   CASE
                       WHEN c.idTipoDocumento = 2 THEN c.razon_social
                       ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                   END,
                   'Cliente ocasional'
               ) AS nombreCliente,

               COALESCE(NULLIF(v.documentoCliente, ''), c.nro_documento, '') AS documentoCliente,
               COALESCE(NULLIF(v.telefonoCliente, ''), c.Telefono, '') AS telefonoCliente,

               u.NombreUsuario AS nombreUsuario,
               s.nombre AS nombreSede,

               p.idPedido AS idPedido,
               p.idEstadoPedido AS idEstadoPedido,
               p.codigoRecojo AS codigoRecojo,
               p.fechaRecojoProgramada AS fechaRecojoProgramada,
               p.fechaPreparando AS fechaPreparando,
               p.fechaListo AS fechaListo,
               p.fechaEntregado AS fechaEntregado,
               p.idUsuarioPreparando AS idUsuarioPreparando,
               p.idUsuarioEntrega AS idUsuarioEntrega,
               p.observacion AS observacionEntrega,

               up.NombreUsuario AS nombreUsuarioPreparando,
               ue.NombreUsuario AS nombreUsuarioEntrega,
               ep.nombreEstado AS nombreEstadoPedido,

               TIMESTAMPDIFF(MINUTE, p.fechaPreparando, p.fechaListo) AS minutosAlistado,
               TIMESTAMPDIFF(MINUTE, v.FechaVenta, p.fechaEntregado) AS minutosTotalProceso,

               CASE
                               WHEN p.fechaPreparando IS NOT NULL
                                    OR p.fechaListo IS NOT NULL
                                    OR p.idUsuarioPreparando IS NOT NULL
                               THEN 'DESPACHO'
                               ELSE 'INMEDIATA'
                           END AS tipoEntregaHistorial

        FROM ventas v
        LEFT JOIN pedidos p ON p.idVenta = v.idVentas
        LEFT JOIN clientes c ON v.idClientes = c.idClientes
        LEFT JOIN usuarios u ON v.idUsuario = u.idUsuario
        LEFT JOIN sedes s ON v.idSede = s.idSede
        LEFT JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        LEFT JOIN usuarios up ON p.idUsuarioPreparando = up.idUsuario
        LEFT JOIN usuarios ue ON p.idUsuarioEntrega = ue.idUsuario

        WHERE v.idSede = ?
          AND (
              ep.nombreEstado IN ('ENTREGADO', 'ANULADO')
              OR v.EstadoVenta = 'ANULADA'
          )

        ORDER BY COALESCE(p.fechaEntregado, v.FechaVenta) DESC
    """;

        return jdbc.query(sql, (rs, rowNum) -> mapVenta(rs), idSede);
    }

    @Override
    public long contarEntregadosHoyPorSede(Integer idSede) {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'ENTREGADO'
          AND v.idSede = ?
          AND DATE(p.fechaEntregado) = CURDATE()
    """;

        Long total = jdbc.queryForObject(sql, Long.class, idSede);
        return total == null ? 0 : total;
    }

    @Override
    public long contarPreparandoPorSede(Integer idSede) {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'PREPARANDO'
          AND v.idSede = ?
    """;

        Long total = jdbc.queryForObject(sql, Long.class, idSede);
        return total == null ? 0 : total;
    }

    @Override
    public long contarEntregasInmediatasHoyPorSede(Integer idSede) {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'ENTREGADO'
          AND v.idSede = ?
          AND DATE(p.fechaEntregado) = CURDATE()
          AND p.fechaPreparando IS NULL
          AND p.fechaListo IS NULL
          AND p.idUsuarioPreparando IS NULL
    """;

        Long total = jdbc.queryForObject(sql, Long.class, idSede);
        return total == null ? 0 : total;
    }

    @Override
    public long contarEntregasDespachoHoyPorSede(Integer idSede) {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'ENTREGADO'
          AND v.idSede = ?
          AND DATE(p.fechaEntregado) = CURDATE()
          AND (
              p.fechaPreparando IS NOT NULL
              OR p.fechaListo IS NOT NULL
              OR p.idUsuarioPreparando IS NOT NULL
          )
    """;

        Long total = jdbc.queryForObject(sql, Long.class, idSede);
        return total == null ? 0 : total;
    }

    @Override
    public long contarAnuladosHoyPorSede(Integer idSede) {
        String sql = """
        SELECT COUNT(*)
        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        INNER JOIN estadopedido ep ON p.idEstadoPedido = ep.idEstadoPedido
        WHERE ep.nombreEstado = 'ANULADO'
          AND v.idSede = ?
          AND DATE(COALESCE(p.fechaEntregado, v.FechaVenta)) = CURDATE()
    """;

        Long total = jdbc.queryForObject(sql, Long.class, idSede);
        return total == null ? 0 : total;
    }
    // ======================================
// DASHBOARD / ANÁLISIS
// ======================================

    private String condicionSedeDashboard(Integer idSede) {
        return idSede != null && idSede > 0
                ? " AND v.idSede = ? "
                : "";
    }

    private String condicionFechasDashboard() {
        return """
           AND DATE(v.FechaVenta) >= ?
           AND DATE(v.FechaVenta) <= ?
           """;
    }

    private Object[] parametrosDashboard(Integer idSede, String fechaDesde, String fechaHasta) {

        if (idSede != null && idSede > 0) {
            return new Object[]{fechaDesde, fechaHasta, idSede};
        }

        return new Object[]{fechaDesde, fechaHasta};
    }
    @Override
    public Map<String, Object> obtenerResumenDashboard(Integer idSede, String fechaDesde, String fechaHasta) {

        String filtroSede = condicionSedeDashboard(idSede);
        Object[] params = parametrosDashboard(idSede, fechaDesde, fechaHasta);

        String sql = """
        SELECT
            COALESCE(SUM(v.TotalVenta), 0) AS ventasMes,
            COUNT(*) AS pedidosMes,
            COALESCE(AVG(NULLIF(v.TotalVenta, 0)), 0) AS ticketPromedio,
            COUNT(DISTINCT v.idClientes) AS clientesAtendidos
        FROM ventas v
        WHERE v.EstadoVenta = 'PAGADO'
    """ + condicionFechasDashboard() + filtroSede;

        Map<String, Object> resumen = new LinkedHashMap<>(jdbc.queryForMap(sql, params));

        String sqlProductos = """
        SELECT COALESCE(SUM(dv.Cantidad), 0)
        FROM detalleventa dv
        INNER JOIN ventas v
            ON v.idVentas = dv.idVentas
        WHERE v.EstadoVenta = 'PAGADO'
    """ + condicionFechasDashboard() + filtroSede;

        BigDecimal productosVendidos = jdbc.queryForObject(
                sqlProductos,
                BigDecimal.class,
                params
        );

        resumen.put("productosVendidos", productosVendidos == null ? BigDecimal.ZERO : productosVendidos);

        return resumen;
    }

    @Override
    public List<Map<String, Object>> listarVentasPorDiaDashboard(Integer idSede, String fechaDesde, String fechaHasta) {

        String filtroSede = condicionSedeDashboard(idSede);

        String sql = """
        SELECT
            DATE_FORMAT(v.FechaVenta, '%d/%m') AS etiqueta,
            COALESCE(SUM(v.TotalVenta), 0) AS valor
        FROM ventas v
        WHERE v.EstadoVenta = 'PAGADO'
    """ + condicionFechasDashboard() + filtroSede + """
        GROUP BY DATE(v.FechaVenta), DATE_FORMAT(v.FechaVenta, '%d/%m')
        ORDER BY DATE(v.FechaVenta)
    """;

        return jdbc.queryForList(sql, parametrosDashboard(idSede, fechaDesde, fechaHasta));
    }

    @Override
    public List<Map<String, Object>> listarVentasPorCategoriaDashboard(Integer idSede, String fechaDesde, String fechaHasta) {

        String filtroSede = condicionSedeDashboard(idSede);

        String sql = """
        SELECT
            COALESCE(c.nombreCategoria, 'Sin categoría') AS etiqueta,
            COALESCE(SUM((dv.Cantidad * dv.PrecioUnitario) - COALESCE(dv.Descuento, 0)), 0) AS valor
        FROM detalleventa dv
        INNER JOIN ventas v
            ON v.idVentas = dv.idVentas
        INNER JOIN productos p
            ON p.idProducto = dv.idProducto
        LEFT JOIN categoria_producto c
            ON c.idCategoria = p.idCategoria
        WHERE v.EstadoVenta = 'PAGADO'
    """ + condicionFechasDashboard() + filtroSede + """
        GROUP BY COALESCE(c.nombreCategoria, 'Sin categoría')
        ORDER BY valor DESC
    """;

        return jdbc.queryForList(sql, parametrosDashboard(idSede, fechaDesde, fechaHasta));
    }

    @Override
    public List<Map<String, Object>> listarVentasPorSedeDashboard(String fechaDesde, String fechaHasta) {

        String sql = """
        SELECT
            COALESCE(s.nombre, 'Sin sede') AS etiqueta,
            COALESCE(SUM(v.TotalVenta), 0) AS valor
        FROM ventas v
        LEFT JOIN sedes s
            ON s.idSede = v.idSede
        WHERE v.EstadoVenta = 'PAGADO'
    """ + condicionFechasDashboard() + """
        GROUP BY COALESCE(s.nombre, 'Sin sede')
        ORDER BY valor DESC
    """;

        return jdbc.queryForList(sql, fechaDesde, fechaHasta);
    }

    @Override
    public List<Map<String, Object>> listarVentasPorCanalDashboard(Integer idSede, String fechaDesde, String fechaHasta) {

        String filtroSede = condicionSedeDashboard(idSede);

        String sql = """
        SELECT
            CASE
                WHEN UPPER(COALESCE(v.canal_venta, '')) = 'ONLINE' THEN 'ONLINE'
                WHEN UPPER(COALESCE(v.canal_venta, '')) = 'PRESENCIAL' THEN 'PRESENCIAL'
                ELSE 'OTROS'
            END AS etiqueta,
            COALESCE(SUM(v.TotalVenta), 0) AS valor
        FROM ventas v
        WHERE v.EstadoVenta = 'PAGADO'
    """ + condicionFechasDashboard() + filtroSede + """
        GROUP BY
            CASE
                WHEN UPPER(COALESCE(v.canal_venta, '')) = 'ONLINE' THEN 'ONLINE'
                WHEN UPPER(COALESCE(v.canal_venta, '')) = 'PRESENCIAL' THEN 'PRESENCIAL'
                ELSE 'OTROS'
            END
        ORDER BY valor DESC
    """;

        return jdbc.queryForList(sql, parametrosDashboard(idSede, fechaDesde, fechaHasta));
    }

    @Override
    public List<Map<String, Object>> listarVentasPorFormaPagoDashboard(Integer idSede, String fechaDesde, String fechaHasta) {

        String filtroSede = condicionSedeDashboard(idSede);

        String sql = """
        SELECT
            COALESCE(
                NULLIF(UPPER(v.metodoPago), ''),
                NULLIF(UPPER(v.FormaPago), ''),
                'SIN DEFINIR'
            ) AS etiqueta,
            COALESCE(SUM(v.TotalVenta), 0) AS valor
        FROM ventas v
        WHERE v.EstadoVenta = 'PAGADO'
    """ + condicionFechasDashboard() + filtroSede + """
        GROUP BY
            COALESCE(
                NULLIF(UPPER(v.metodoPago), ''),
                NULLIF(UPPER(v.FormaPago), ''),
                'SIN DEFINIR'
            )
        ORDER BY valor DESC
    """;

        return jdbc.queryForList(sql, parametrosDashboard(idSede, fechaDesde, fechaHasta));
    }

    @Override
    public List<Map<String, Object>> listarUltimasVentasDashboard(Integer idSede, String fechaDesde, String fechaHasta) {

        String filtroSede = condicionSedeDashboard(idSede);

        String sql = """
        SELECT
            COALESCE(v.NroComprobante, 'Sin comprobante') AS comprobante,
            DATE_FORMAT(v.FechaVenta, '%d/%m/%Y %H:%i') AS fechaHora,
            COALESCE(
                NULLIF(v.nombreCliente, ''),
                CASE
                    WHEN c.idTipoDocumento = 2 THEN c.razon_social
                    ELSE CONCAT_WS(' ', c.Nombres, c.Apellidos)
                END,
                'Cliente ocasional'
            ) AS cliente,
            CASE
                WHEN UPPER(COALESCE(v.canal_venta, '')) = 'ONLINE' THEN 'ONLINE'
                WHEN UPPER(COALESCE(v.canal_venta, '')) = 'PRESENCIAL' THEN 'PRESENCIAL'
                ELSE 'OTROS'
            END AS canal,
            COALESCE(v.EstadoVenta, 'SIN ESTADO') AS estado,
            COALESCE(v.TotalVenta, 0) AS total
        FROM ventas v
        LEFT JOIN clientes c ON v.idClientes = c.idClientes
        WHERE v.FechaVenta IS NOT NULL
    """ + condicionFechasDashboard() + filtroSede + """
        ORDER BY v.FechaVenta DESC
        LIMIT 5
    """;

        return jdbc.queryForList(sql, parametrosDashboard(idSede, fechaDesde, fechaHasta));
    }

    @Override
    public List<Map<String, Object>> listarTopProductosDashboard(Integer idSede, String fechaDesde, String fechaHasta) {

        String filtroSede = condicionSedeDashboard(idSede);

        String sql = """
        SELECT
            p.NombreProducto AS producto,
            COALESCE(SUM(dv.Cantidad), 0) AS cantidad,
            COALESCE(SUM((dv.Cantidad * dv.PrecioUnitario) - COALESCE(dv.Descuento, 0)), 0) AS total
        FROM detalleventa dv
        INNER JOIN ventas v
            ON v.idVentas = dv.idVentas
        INNER JOIN productos p
            ON p.idProducto = dv.idProducto
        WHERE v.EstadoVenta = 'PAGADO'
    """ + condicionFechasDashboard() + filtroSede + """
        GROUP BY p.idProducto, p.NombreProducto
        ORDER BY total DESC
        LIMIT 5
    """;

        return jdbc.queryForList(sql, parametrosDashboard(idSede, fechaDesde, fechaHasta));
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value != null ? Timestamp.valueOf(value) : null;
    }
}
