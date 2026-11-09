package com.dentia.dao.Jdbc;

import com.dentia.dto.gestion.GestionRequests.Filtro;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Statement;
import java.util.*;

@Repository
public class JdbcGestionDao {
    private final JdbcTemplate jdbc;
    public JdbcGestionDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> consultar(String sql, Object... args) {
        return jdbc.query(sql, (rs, n) -> {
            Map<String,Object> row = new LinkedHashMap<>();
            var meta = rs.getMetaData();
            for (int i=1; i<=meta.getColumnCount(); i++) {
                Object value = rs.getObject(i);
                if (value instanceof java.sql.Timestamp t) value = t.toLocalDateTime().toString();
                if (value instanceof java.sql.Date d) value = d.toLocalDate().toString();
                row.put(meta.getColumnLabel(i), value);
            }
            return row;
        }, args);
    }

    public Map<String,Object> uno(String sql, Object... args) {
        return consultar(sql,args).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("El registro no existe."));
    }

    public long insertar(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i=0;i<args.length;i++) ps.setObject(i+1,args[i]);
            return ps;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    public int actualizar(String sql,Object... args) { return jdbc.update(sql,args); }
    public BigDecimal decimal(String sql,Object... args) {
        return jdbc.queryForObject(sql,BigDecimal.class,args);
    }
    public void historial(String modulo,long id,String accion,String detalle,String usuario) {
        actualizar("INSERT INTO gestion_historial(modulo,registro_id,accion,detalle,usuario) VALUES(?,?,?,?,?)",
                modulo,id,accion,detalle,usuario);
    }

    public String seleccion(String modulo) {
        String paciente = "p.id_paciente pacienteId,CONCAT(p.nombres,' ',p.apellidos) paciente,p.dni,p.celular";
        return switch(modulo) {
            case "presupuestos" -> "SELECT r.*,r.paciente_id pacienteRef," + paciente + ","
                    + "COALESCE(a.pagado,0) pagado,r.total-COALESCE(a.pagado,0) saldo FROM gestion_presupuestos r "
                    + "JOIN pacientes p ON p.id_paciente=r.paciente_id LEFT JOIN "
                    + "(SELECT presupuesto_id,SUM(monto) pagado FROM gestion_pagos WHERE estado='REGISTRADO' GROUP BY presupuesto_id) a ON a.presupuesto_id=r.id";
            case "pagos" -> "SELECT r.*,b.paciente_id pacienteRef,b.titulo presupuesto," + paciente
                    + " FROM gestion_pagos r JOIN gestion_presupuestos b ON b.id=r.presupuesto_id JOIN pacientes p ON p.id_paciente=b.paciente_id";
            case "seguimiento" -> "SELECT r.*," + paciente
                    + ",c.fecha_hora_inicio citaFecha,b.titulo presupuesto FROM gestion_seguimientos r JOIN pacientes p ON p.id_paciente=r.paciente_id "
                    + "LEFT JOIN citas c ON c.id_cita=r.cita_id LEFT JOIN gestion_presupuestos b ON b.id=r.presupuesto_id";
            case "documentos" -> "SELECT r.id,r.paciente_id,r.cita_id,r.presupuesto_id,r.titulo,r.tipo,r.nombre_archivo,r.mime,r.tamano,r.estado,r.creado_por,r.creado_en," + paciente
                    + ",b.titulo presupuesto FROM gestion_documentos r JOIN pacientes p ON p.id_paciente=r.paciente_id LEFT JOIN gestion_presupuestos b ON b.id=r.presupuesto_id";
            default -> throw new IllegalArgumentException("Módulo no válido.");
        };
    }

    public record Consulta(String sql,List<Object> args) {}
    public Consulta filtrada(String modulo,Filtro f) {
        StringBuilder sql = new StringBuilder(seleccion(modulo)).append(" WHERE 1=1");
        List<Object> args=new ArrayList<>();
        if(f.paciente()!=null){sql.append(" AND p.id_paciente=?");args.add(f.paciente());}
        if(f.q()!=null&&!f.q().isBlank()) {
            sql.append(" AND (CONCAT(p.nombres,' ',p.apellidos) LIKE ? OR p.dni LIKE ?)");
            args.add("%"+f.q().trim()+"%");args.add("%"+f.q().trim()+"%");
        }
        if(f.estado()!=null&&!f.estado().isBlank()){sql.append(" AND r.estado=?");args.add(f.estado());}
        String fecha="documentos".equals(modulo)?"r.creado_en":"r.fecha";
        if(f.desde()!=null){sql.append(" AND ").append(fecha).append(">=?");args.add(f.desde());}
        if(f.hasta()!=null){sql.append(" AND ").append(fecha).append("<?");args.add(f.hasta().plusDays(1));}
        return new Consulta(sql.toString(),args);
    }

    public Map<String,Object> listar(String modulo,Filtro f) {
        Consulta c=filtrada(modulo,f);
        long total=((Number)uno("SELECT COUNT(*) total FROM ("+c.sql()+") datos",c.args().toArray()).get("total")).longValue();
        var args=new ArrayList<>(c.args()); args.add(20);args.add(Math.max(0,f.pagina())*20);
        String order = "seguimiento".equals(modulo)?" ORDER BY (r.estado='PENDIENTE') DESC,r.fecha,r.id":" ORDER BY r.id DESC";
        return Map.of("filas",consultar(c.sql()+order+" LIMIT ? OFFSET ?",args.toArray()),"total",total,
                "pagina",f.pagina(),"paginas",(total+19)/20,"resumen",resumen(modulo,c));
    }

    private Map<String,Object> resumen(String modulo,Consulta c) {
        String expressions=switch(modulo) {
            case "presupuestos" -> "COUNT(*) registros,COALESCE(SUM(CASE WHEN estado='APROBADO' THEN total ELSE 0 END),0) aprobado,COALESCE(SUM(CASE WHEN estado='APROBADO' THEN saldo ELSE 0 END),0) pendiente";
            case "pagos" -> "COUNT(*) registros,COALESCE(SUM(CASE WHEN estado='REGISTRADO' THEN monto ELSE 0 END),0) cobrado,COALESCE(SUM(CASE WHEN estado='ANULADO' THEN monto ELSE 0 END),0) anulado";
            case "seguimiento" -> "COUNT(*) registros,COALESCE(SUM(estado='PENDIENTE'),0) pendientes,COALESCE(SUM(estado='PENDIENTE' AND fecha<NOW()),0) vencidos";
            case "documentos" -> "COUNT(*) registros,COALESCE(SUM(estado='ACTIVO'),0) activos,COALESCE(SUM(tamano),0) bytes";
            default -> throw new IllegalArgumentException();
        };
        return uno("SELECT "+expressions+" FROM ("+c.sql()+") datos",c.args().toArray());
    }

    public Map<String,Object> detalle(String modulo,long id) {
        var row=uno(seleccion(modulo)+" WHERE r.id=?",id);
        if(modulo.equals("presupuestos")) {
            row.put("items",consultar("SELECT id,descripcion,cantidad,precio,cantidad*precio subtotal FROM gestion_presupuesto_items WHERE presupuesto_id=? ORDER BY id",id));
        }
        row.put("historial",consultar("SELECT accion,detalle,usuario,fecha FROM gestion_historial WHERE modulo=? AND registro_id=? ORDER BY id DESC",modulo,id));
        return row;
    }

    public Map<String,Object> reporte(Filtro f) {
        var pagos=filtrada("pagos",f); var presupuestos=filtrada("presupuestos",f); var seguimientos=filtrada("seguimiento",f);
        return Map.of("cobros",resumen("pagos",pagos),"presupuestos",resumen("presupuestos",presupuestos),
                "seguimiento",resumen("seguimiento",seguimientos),
                "metodos",consultar("SELECT metodo,COUNT(*) operaciones,SUM(monto) total FROM ("+pagos.sql()+") datos WHERE estado='REGISTRADO' GROUP BY metodo ORDER BY total DESC",pagos.args().toArray()),
                "dias",consultar("SELECT fecha,SUM(monto) total FROM ("+pagos.sql()+") datos WHERE estado='REGISTRADO' GROUP BY fecha ORDER BY fecha",pagos.args().toArray()),
                "cartera",consultar("SELECT pacienteId,paciente,SUM(total) total,SUM(pagado) pagado,SUM(saldo) saldo FROM ("+presupuestos.sql()+") datos WHERE estado='APROBADO' GROUP BY pacienteId,paciente ORDER BY saldo DESC LIMIT 20",presupuestos.args().toArray()));
    }
}
