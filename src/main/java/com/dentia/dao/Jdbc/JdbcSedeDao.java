package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.ISedeDao;
import com.dentia.model.interno.Sede;
import com.dentia.model.interno.SedeHorarioRecojo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcSedeDao implements ISedeDao {

    private final JdbcTemplate jdbc;

    public JdbcSedeDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Sede> mapRow = (rs, rowNum) -> {
        Sede s = new Sede();

        s.setIdSede(rs.getInt("idSede"));
        s.setNombre(rs.getString("nombre"));
        s.setCodigo(rs.getString("codigo"));
        s.setTipo(rs.getString("tipo"));
        s.setDireccion(rs.getString("direccion"));
        s.setCiudad(rs.getString("ciudad"));
        s.setResponsable(rs.getString("responsable"));
        s.setTelefono(rs.getString("telefono"));
        s.setDescripcion(rs.getString("descripcion"));
        s.setFoto(rs.getString("foto"));
        s.setGoogleMapsUrl(rs.getString("google_maps_url"));
        s.setActivo(rs.getBoolean("activo"));

        if (rs.getTimestamp("fechaRegistro") != null) {
            s.setFechaRegistro(rs.getTimestamp("fechaRegistro").toLocalDateTime());
        }

        return s;
    };

    @Override
    public List<Sede> listar() {
        String sql = """
            SELECT idSede, nombre, codigo, tipo, direccion, ciudad,
                   responsable, telefono, descripcion, foto, google_maps_url, activo, fechaRegistro
            FROM sedes
            ORDER BY activo DESC, tipo ASC, nombre ASC
        """;

        return jdbc.query(sql, mapRow);
    }

    @Override
    public Optional<Sede> buscarPorId(Integer id) {
        String sql = """
            SELECT idSede, nombre, codigo, tipo, direccion, ciudad,
                   responsable, telefono, descripcion, foto, google_maps_url, activo, fechaRegistro
            FROM sedes
            WHERE idSede = ?
        """;

        List<Sede> lista = jdbc.query(sql, mapRow, id);
        return lista.stream().findFirst();
    }

    @Override
    public Optional<Sede> buscarPorCodigo(String codigo) {
        String sql = """
            SELECT idSede, nombre, codigo, tipo, direccion, ciudad,
                   responsable, telefono, descripcion, foto, google_maps_url, activo, fechaRegistro
            FROM sedes
            WHERE codigo = ?
        """;

        List<Sede> lista = jdbc.query(sql, mapRow, codigo);
        return lista.stream().findFirst();
    }

    @Override
    public int crear(Sede sede) {
        String sql = """
        INSERT INTO sedes
        (nombre, codigo, tipo, direccion, ciudad, responsable, telefono, descripcion, foto, google_maps_url, activo)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
    """;

        return jdbc.update(sql,
                sede.getNombre(),
                sede.getCodigo(),
                sede.getTipo(),
                sede.getDireccion(),
                sede.getCiudad(),
                sede.getResponsable(),
                sede.getTelefono(),
                sede.getDescripcion(),
                sede.getFoto(),
                sede.getGoogleMapsUrl()
        );
    }

    @Override
    public int actualizar(Sede sede) {
        String sql = """
            UPDATE sedes SET
                nombre = ?,
                codigo = ?,
                tipo = ?,
                direccion = ?,
                ciudad = ?,
                responsable = ?,
                telefono = ?,
                descripcion = ?,
                foto = ?,
                google_maps_url = ?,
                activo = ?
            WHERE idSede = ?
        """;

        return jdbc.update(sql,
                sede.getNombre(),
                sede.getCodigo(),
                sede.getTipo(),
                sede.getDireccion(),
                sede.getCiudad(),
                sede.getResponsable(),
                sede.getTelefono(),
                sede.getDescripcion(),
                sede.getFoto(),
                sede.getGoogleMapsUrl(),
                sede.isActivo(),
                sede.getIdSede()
        );
    }

    @Override
    public int cambiarEstado(Integer id, boolean activo) {
        String sql = "UPDATE sedes SET activo = ? WHERE idSede = ?";
        return jdbc.update(sql, activo, id);
    }

    @Override
    public int eliminar(Integer id) {
        String sql = "UPDATE sedes SET activo = 0 WHERE idSede = ?";
        return jdbc.update(sql, id);
    }

    @Override
    public int contarTodas() {
        String sql = "SELECT COUNT(*) FROM sedes";
        Integer total = jdbc.queryForObject(sql, Integer.class);
        return total != null ? total : 0;
    }

    @Override
    public int contarPorTipo(String tipo) {
        String sql = "SELECT COUNT(*) FROM sedes WHERE tipo = ?";
        Integer total = jdbc.queryForObject(sql, Integer.class, tipo);
        return total != null ? total : 0;
    }

    @Override
    public int contarInactivas() {
        String sql = "SELECT COUNT(*) FROM sedes WHERE activo = 0";
        Integer total = jdbc.queryForObject(sql, Integer.class);
        return total != null ? total : 0;
    }
    @Override
    public Optional<String> obtenerDescripcionPublicaAtencion(Integer idSede) {
        String sql = """
        SELECT descripcionPublica
        FROM sede_configuracion_atencion
        WHERE idSede = ?
        LIMIT 1
    """;

        List<String> lista = jdbc.query(
                sql,
                (rs, rowNum) -> rs.getString("descripcionPublica"),
                idSede
        );

        return lista.stream().findFirst();
    }

    @Override
    public List<Map<String, Object>> listarHorariosRecojoPorSede(Integer idSede) {
        String sql = """
        SELECT
            idHorarioRecojo,
            idSede,
            nombreBloque,
            horaInicio,
            horaFin,
            duracionFranjaMinutos,
            capacidadPorFranja,
            activo
        FROM sede_horario_recojo
        WHERE idSede = ?
        ORDER BY horaInicio ASC
    """;

        return jdbc.queryForList(sql, idSede);
    }

    @Override
    public int guardarDescripcionPublicaAtencion(Integer idSede, String descripcionPublica, boolean activo) {
        String sqlExiste = """
        SELECT COUNT(*)
        FROM sede_configuracion_atencion
        WHERE idSede = ?
    """;

        Integer existe = jdbc.queryForObject(sqlExiste, Integer.class, idSede);

        if (existe != null && existe > 0) {
            String sqlUpdate = """
            UPDATE sede_configuracion_atencion
            SET descripcionPublica = ?,
                activo = ?,
                fechaActualizacion = NOW()
            WHERE idSede = ?
        """;

            return jdbc.update(sqlUpdate, descripcionPublica, activo, idSede);
        }

        String sqlInsert = """
        INSERT INTO sede_configuracion_atencion
        (idSede, descripcionPublica, activo)
        VALUES (?, ?, ?)
    """;

        return jdbc.update(sqlInsert, idSede, descripcionPublica, activo);
    }

    @Override
    public int eliminarHorariosRecojoPorSede(Integer idSede) {
        String sql = """
        DELETE FROM sede_horario_recojo
        WHERE idSede = ?
    """;

        return jdbc.update(sql, idSede);
    }

    @Override
    public int crearHorarioRecojo(Integer idSede,
                                  String nombreBloque,
                                  LocalTime horaInicio,
                                  LocalTime horaFin,
                                  Integer duracionFranjaMinutos,
                                  Integer capacidadPorFranja,
                                  boolean activo) {
        String sql = """
        INSERT INTO sede_horario_recojo
        (idSede, nombreBloque, horaInicio, horaFin, duracionFranjaMinutos, capacidadPorFranja, activo)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """;

        return jdbc.update(
                sql,
                idSede,
                nombreBloque,
                horaInicio,
                horaFin,
                duracionFranjaMinutos,
                capacidadPorFranja,
                activo
        );
    }

    @Override
    public Map<LocalTime, Integer> contarReservasRecojoPorSedeYFecha(Integer idSede, LocalDate fecha) {

        String sql = """
        SELECT
            TIME(p.fechaRecojoProgramada) AS hora,
            COUNT(*) AS ocupados
        FROM pedidos p
        INNER JOIN ventas v ON p.idVenta = v.idVentas
        INNER JOIN estadopedido ep ON ep.idEstadoPedido = p.idEstadoPedido
        WHERE v.idSede = ?
          AND p.fechaRecojoProgramada IS NOT NULL
          AND DATE(p.fechaRecojoProgramada) = ?
          AND ep.nombreEstado <> 'ANULADO'
        GROUP BY TIME(p.fechaRecojoProgramada)
    """;

        Map<LocalTime, Integer> reservas = new HashMap<>();

        jdbc.query(sql, rs -> {
            java.sql.Time horaSql = rs.getTime("hora");

            if (horaSql != null) {
                reservas.put(
                        horaSql.toLocalTime(),
                        rs.getInt("ocupados")
                );
            }
        }, idSede, fecha);

        return reservas;
    }

    @Override
    public String generarCodigo(String tipo) {

        String prefijo = tipo.equalsIgnoreCase("ALMACEN") ? "ALM" : "TDA";

        String sql = """
        SELECT codigo
        FROM sedes
        WHERE codigo LIKE ?
        ORDER BY codigo DESC
        LIMIT 1
    """;

        String like = prefijo + "-%";

        List<String> codigos = jdbc.query(
                sql,
                (rs, rowNum) -> rs.getString("codigo"),
                like
        );

        if (codigos.isEmpty()) {
            return prefijo + "-001";
        }

        String ultimo = codigos.get(0);
        int numero = Integer.parseInt(ultimo.split("-")[1]);

        return String.format("%s-%03d", prefijo, numero + 1);
    }

    @Override
    public Optional<SedeHorarioRecojo> buscarHorarioRecojoPorId(Integer idHorarioRecojo) {
        String sql = """
        SELECT idHorarioRecojo, idSede, nombreBloque, horaInicio, horaFin,
               duracionFranjaMinutos, capacidadPorFranja, activo
        FROM sede_horario_recojo
        WHERE idHorarioRecojo = ?
    """;
        return jdbc.query(sql, (rs, rowNum) -> mapHorarioRecojo(rs), idHorarioRecojo)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<SedeHorarioRecojo> bloquearHorarioRecojoPorId(Integer idHorarioRecojo) {
        String sql = """
        SELECT idHorarioRecojo, idSede, nombreBloque, horaInicio, horaFin,
               duracionFranjaMinutos, capacidadPorFranja, activo
        FROM sede_horario_recojo
        WHERE idHorarioRecojo = ?
        FOR UPDATE
    """;
        return jdbc.query(sql, (rs, rowNum) -> mapHorarioRecojo(rs), idHorarioRecojo)
                .stream()
                .findFirst();
    }

    @Override
    public int contarPedidosValidosPorFranja(Integer idSede,
                                             LocalDate fechaRecojo,
                                             LocalTime horaInicioRecojo,
                                             LocalTime horaFinRecojo) {
        String sql = """
        SELECT COUNT(*) AS ocupados
        FROM pedidos p
        INNER JOIN ventas v
            ON v.idVentas = p.idVenta
        INNER JOIN estadopedido ep
            ON ep.idEstadoPedido = p.idEstadoPedido
        LEFT JOIN orden_compra_online o
            ON o.idOrdenOnline = p.idOrdenOnline
        WHERE v.idSede = ?
          AND ep.nombreEstado IN ('PENDIENTE', 'PAGADO', 'PREPARANDO', 'LISTO_RECOJO', 'ENTREGADO')
          AND (
              (
                  p.idOrdenOnline IS NOT NULL
                  AND o.idSede = ?
                  AND o.fechaRecojo = ?
                  AND o.horaInicioRecojo = ?
                  AND o.horaFinRecojo = ?
              )
              OR
              (
                  p.idOrdenOnline IS NULL
                  AND p.fechaRecojoProgramada IS NOT NULL
                  AND DATE(p.fechaRecojoProgramada) = ?
                  AND TIME(p.fechaRecojoProgramada) >= ?
                  AND TIME(p.fechaRecojoProgramada) < ?
              )
          )
    """;

        Integer total = jdbc.queryForObject(sql, Integer.class,
                idSede,
                idSede,
                fechaRecojo,
                horaInicioRecojo,
                horaFinRecojo,
                fechaRecojo,
                horaInicioRecojo,
                horaFinRecojo);

        return total != null ? total : 0;
    }

    @Override
    public int contarReservasActivasPorFranja(Integer idSede,
                                              LocalDate fechaRecojo,
                                              LocalTime horaInicioRecojo,
                                              LocalTime horaFinRecojo) {
        String sql = """
        SELECT COUNT(*) AS ocupados
        FROM inventario_reserva r
        INNER JOIN orden_compra_online o
            ON o.idOrdenOnline = r.idOrdenOnline
        WHERE r.estado = 'ACTIVA'
          AND r.fechaExpiracion > NOW()
          AND o.idSede = ?
          AND o.fechaRecojo = ?
          AND o.horaInicioRecojo = ?
          AND o.horaFinRecojo = ?
    """;

        Integer total = jdbc.queryForObject(sql, Integer.class,
                idSede,
                fechaRecojo,
                horaInicioRecojo,
                horaFinRecojo);

        return total != null ? total : 0;
    }

    private SedeHorarioRecojo mapHorarioRecojo(java.sql.ResultSet rs) throws java.sql.SQLException {
        SedeHorarioRecojo h = new SedeHorarioRecojo();
        h.setIdHorarioRecojo(rs.getInt("idHorarioRecojo"));
        h.setIdSede(rs.getInt("idSede"));
        h.setNombreBloque(rs.getString("nombreBloque"));
        h.setHoraInicio(rs.getTime("horaInicio").toLocalTime());
        h.setHoraFin(rs.getTime("horaFin").toLocalTime());
        h.setDuracionFranjaMinutos(rs.getInt("duracionFranjaMinutos"));
        h.setCapacidadPorFranja(rs.getInt("capacidadPorFranja"));
        h.setActivo(rs.getBoolean("activo"));
        return h;
    }
}
