package com.dentia.service.interno;

import com.dentia.dao.Jdbc.JdbcGestionDao;
import com.dentia.dto.gestion.GestionRequests.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class GestionInternaService {
    private final JdbcGestionDao dao;
    public GestionInternaService(JdbcGestionDao dao) { this.dao=dao; }
    public static final Set<String> MODULOS=Set.of("documentos","presupuestos","pagos","reportes");
    private static final Set<String> METODOS=Set.of("EFECTIVO","TARJETA","TRANSFERENCIA","YAPE","PLIN");
    private static final Set<String> CANALES=Set.of("LLAMADA","WHATSAPP","CORREO","PRESENCIAL");
    private static final Set<String> TIPOS=Set.of("CONSENTIMIENTO","INFORME","RECETA","COMPROBANTE","OTRO");

    @Transactional(readOnly=true)
    public Map<String,Object> listar(String modulo,Filtro filtro) {
        validarFiltro(filtro);
        return modulo.equals("reportes")?dao.reporte(filtro):dao.listar(modulo,filtro);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> detalle(String modulo,long id){return dao.detalle(modulo,id);}

    @Transactional(readOnly=true)
    public Map<String,Object> opciones(String modulo,Long paciente,String q) {
        String termino=q==null?"":q.trim();
        if(termino.length()>100) throw new IllegalArgumentException("La búsqueda es demasiado larga.");
        var pacientes=dao.consultar("SELECT id_paciente id,CONCAT(nombres,' ',apellidos) nombre,dni,activo FROM pacientes WHERE (activo=1 OR id_paciente=?) AND (CONCAT(nombres,' ',apellidos) LIKE ? OR dni LIKE ? OR id_paciente=?) ORDER BY (id_paciente=?) DESC,apellidos,nombres LIMIT 100",
                paciente,"%"+termino+"%","%"+termino+"%",paciente,paciente);
        var opciones=new LinkedHashMap<String,Object>(); opciones.put("pacientes",pacientes);
        opciones.put("presupuestos",List.of());opciones.put("citas",List.of());
        if(paciente!=null) {
            // Solo los datos mínimos necesarios para vincular el registro del mismo paciente.
            opciones.put("presupuestos",dao.consultar("SELECT b.id,b.titulo,b.estado,b.total-COALESCE((SELECT SUM(monto) FROM gestion_pagos WHERE presupuesto_id=b.id AND estado='REGISTRADO'),0) saldo FROM gestion_presupuestos b WHERE b.paciente_id=? "+(modulo.equals("pagos")?"AND b.estado='APROBADO'":"")+" ORDER BY b.id DESC",paciente));
            if(!modulo.equals("pagos")) opciones.put("citas",dao.consultar("SELECT id_cita id,fecha_hora_inicio fecha,estado FROM citas WHERE id_paciente=? ORDER BY fecha_hora_inicio DESC LIMIT 100",paciente));
        }
        return opciones;
    }

    @Transactional
    public long guardarPresupuesto(Long id,Presupuesto input,String actor) {
        Objects.requireNonNull(input,"Faltan los datos.");
        pacienteActivo(input.pacienteId());
        String titulo=texto(input.titulo(),180,true),notas=texto(input.notas(),2000,false);
        if(input.vence()==null||input.vence().isBefore(LocalDate.now())) throw new IllegalArgumentException("La vigencia debe ser hoy o una fecha futura.");
        if(input.items()==null||input.items().isEmpty()||input.items().size()>100) throw new IllegalArgumentException("Agrega entre 1 y 100 conceptos.");
        BigDecimal total=BigDecimal.ZERO;
        for(Item item:input.items()) {
            if(item==null) throw new IllegalArgumentException("Concepto no válido.");
            texto(item.descripcion(),250,true);
            if(item.cantidad()==null||item.cantidad()<1||item.cantidad()>1000) throw new IllegalArgumentException("La cantidad debe estar entre 1 y 1000.");
            total=total.add(dinero(item.precio()).multiply(BigDecimal.valueOf(item.cantidad())));
        }
        dinero(total);
        if(id==null) {
            id=dao.insertar("INSERT INTO gestion_presupuestos(paciente_id,titulo,fecha,vence,total,notas,creado_por) VALUES(?,?,?,?,?,?,?)",
                    input.pacienteId(),titulo,LocalDate.now(),input.vence(),total,notas,actor);
        } else {
            var actual=bloquearPresupuesto(id);version(actual,input.version());
            exigir("BORRADOR".equals(actual.get("estado")),"Solo se pueden editar presupuestos en borrador.");
            exigir(numero(actual,"paciente_id")==input.pacienteId(),"No se puede cambiar el paciente de un presupuesto.");
            dao.actualizar("UPDATE gestion_presupuestos SET titulo=?,vence=?,total=?,notas=?,version=version+1 WHERE id=?",titulo,input.vence(),total,notas,id);
            dao.actualizar("DELETE FROM gestion_presupuesto_items WHERE presupuesto_id=?",id);
        }
        for(Item item:input.items()) dao.insertar("INSERT INTO gestion_presupuesto_items(presupuesto_id,descripcion,cantidad,precio) VALUES(?,?,?,?)",id,item.descripcion().trim(),item.cantidad(),dinero(item.precio()));
        dao.historial("presupuestos",id,"GUARDADO","Presupuesto en borrador. Total PEN "+total,actor);
        return id;
    }

    @Transactional
    public void estadoPresupuesto(long id,Estado input,String actor) {
        var actual=bloquearPresupuesto(id);version(actual,input.version());
        String previo=actual.get("estado").toString(),nuevo=input.estado();
        boolean permitido=previo.equals("BORRADOR")&&Set.of("APROBADO","RECHAZADO","ANULADO").contains(nuevo==null?"":nuevo)
                ||previo.equals("APROBADO")&&"ANULADO".equals(nuevo);
        exigir(permitido,"El cambio de estado no está permitido.");
        if("APROBADO".equals(nuevo)) {
            exigir(!LocalDate.parse(actual.get("vence").toString()).isBefore(LocalDate.now()),"El presupuesto venció. Actualiza su vigencia antes de aprobarlo.");
        }
        if("ANULADO".equals(nuevo)) exigir(pagado(id).signum()==0,"Anula primero los abonos registrados de este presupuesto.");
        String motivo=texto(input.motivo(),1000,!"APROBADO".equals(nuevo));
        dao.actualizar("UPDATE gestion_presupuestos SET estado=?,version=version+1 WHERE id=?",nuevo,id);
        dao.historial("presupuestos",id,nuevo,motivo.isBlank()?"Aprobación registrada por el personal.":motivo,actor);
    }

    @Transactional
    public long registrarPago(Pago input,String actor) {
        if(input.presupuestoId()==null) throw new IllegalArgumentException("Selecciona un presupuesto.");
        BigDecimal monto=dinero(input.monto());
        String referencia=texto(input.referencia(),120,false);
        exigir(METODOS.contains(input.metodo()==null?"":input.metodo()),"Selecciona un método válido.");
        exigir("EFECTIVO".equals(input.metodo())||!referencia.isBlank(),"Registra la referencia de la operación.");
        exigir(input.fecha()!=null&&!input.fecha().isAfter(LocalDate.now()),"La fecha del cobro no puede estar en el futuro.");
        try { UUID.fromString(input.token()); } catch(Exception e) {throw new IllegalArgumentException("Identificador de operación no válido. Reabre el formulario.");}
        var presupuesto=bloquearPresupuesto(input.presupuestoId());
        var existentes=dao.consultar("SELECT * FROM gestion_pagos WHERE token=?",input.token());
        if(!existentes.isEmpty()) {
            var previo=existentes.get(0);
            exigir(numero(previo,"presupuesto_id")==input.presupuestoId()&&monto.compareTo((BigDecimal)previo.get("monto"))==0
                    &&input.metodo().equals(previo.get("metodo"))&&referencia.equals(previo.get("referencia"))
                    &&input.fecha().toString().equals(previo.get("fecha")),"Este identificador ya fue utilizado en otra operación.");
            return numero(previo,"id");
        }
        exigir("APROBADO".equals(presupuesto.get("estado")),"Solo se reciben abonos de presupuestos aprobados.");
        exigir(!input.fecha().isBefore(LocalDate.parse(presupuesto.get("fecha").toString())),"El cobro no puede ser anterior al presupuesto.");
        BigDecimal saldo=((BigDecimal)presupuesto.get("total")).subtract(pagado(input.presupuestoId()));
        exigir(monto.compareTo(saldo)<=0,"El importe supera el saldo disponible de S/ "+saldo+".");
        long id=dao.insertar("INSERT INTO gestion_pagos(presupuesto_id,monto,fecha,metodo,referencia,token,creado_por) VALUES(?,?,?,?,?,?,?)",
                input.presupuestoId(),monto,input.fecha(),input.metodo(),referencia,input.token(),actor);
        dao.historial("pagos",id,"REGISTRADO","Abono de PEN "+monto+" al presupuesto #"+input.presupuestoId(),actor);
        dao.historial("presupuestos",input.presupuestoId(),"ABONO","Cobro #"+id+" por PEN "+monto,actor);
        return id;
    }

    @Transactional
    public void anularPago(long id,String motivo,String actor) {
        motivo=texto(motivo,1000,true);
        var pago=dao.uno("SELECT presupuesto_id FROM gestion_pagos WHERE id=?",id);
        long presupuesto=numero(pago,"presupuesto_id");bloquearPresupuesto(presupuesto);
        pago=dao.uno("SELECT * FROM gestion_pagos WHERE id=? FOR UPDATE",id);
        exigir("REGISTRADO".equals(pago.get("estado")),"Este pago ya está anulado.");
        dao.actualizar("UPDATE gestion_pagos SET estado='ANULADO',anulado_por=?,anulado_en=CURRENT_TIMESTAMP,motivo_anulacion=? WHERE id=?",actor,motivo,id);
        dao.historial("pagos",id,"ANULADO",motivo,actor);
        dao.historial("presupuestos",presupuesto,"ABONO_ANULADO","Cobro #"+id+": "+motivo,actor);
    }

    @Transactional
    public long guardarSeguimiento(Long id,Seguimiento input,String actor) {
        pacienteActivo(input.pacienteId());validarVinculos(input.pacienteId(),input.citaId(),input.presupuestoId());
        String titulo=texto(input.titulo(),180,true),notas=texto(input.notas(),2000,false);
        exigir(input.fecha()!=null,"Indica la fecha y hora del seguimiento.");
        exigir(CANALES.contains(input.canal()==null?"":input.canal()),"Selecciona un canal válido.");
        if(id==null) {
            id=dao.insertar("INSERT INTO gestion_seguimientos(paciente_id,cita_id,presupuesto_id,titulo,fecha,canal,notas,creado_por) VALUES(?,?,?,?,?,?,?,?)",
                    input.pacienteId(),input.citaId(),input.presupuestoId(),titulo,input.fecha(),input.canal(),notas,actor);
        } else {
            var actual=dao.uno("SELECT * FROM gestion_seguimientos WHERE id=? FOR UPDATE",id);version(actual,input.version());
            exigir("PENDIENTE".equals(actual.get("estado")),"Solo se pueden editar seguimientos pendientes.");
            exigir(numero(actual,"paciente_id")==input.pacienteId(),"No se puede cambiar el paciente del seguimiento.");
            dao.actualizar("UPDATE gestion_seguimientos SET cita_id=?,presupuesto_id=?,titulo=?,fecha=?,canal=?,notas=?,version=version+1 WHERE id=?",
                    input.citaId(),input.presupuestoId(),titulo,input.fecha(),input.canal(),notas,id);
        }
        dao.historial("seguimiento",id,"PROGRAMADO","Seguimiento para "+input.fecha()+" por "+input.canal(),actor);
        return id;
    }

    @Transactional
    public void cerrarSeguimiento(long id,Estado input,String actor) {
        String motivo=texto(input.motivo(),2000,true);
        exigir(Set.of("COMPLETADO","CANCELADO").contains(input.estado()==null?"":input.estado()),"Estado no válido.");
        var actual=dao.uno("SELECT * FROM gestion_seguimientos WHERE id=? FOR UPDATE",id);version(actual,input.version());
        exigir("PENDIENTE".equals(actual.get("estado")),"El seguimiento ya fue cerrado.");
        dao.actualizar("UPDATE gestion_seguimientos SET estado=?,resultado=?,cerrado_por=?,cerrado_en=CURRENT_TIMESTAMP,version=version+1 WHERE id=?",input.estado(),motivo,actor,id);
        dao.historial("seguimiento",id,input.estado(),motivo,actor);
    }

    @Transactional
    public long guardarDocumento(Long paciente,Long cita,Long presupuesto,String titulo,String tipo,MultipartFile archivo,String actor) throws IOException {
        pacienteActivo(paciente);validarVinculos(paciente,cita,presupuesto);
        titulo=texto(titulo,180,true);
        exigir(TIPOS.contains(tipo==null?"":tipo),"Selecciona un tipo de documento válido.");
        exigir(archivo!=null&&!archivo.isEmpty()&&archivo.getSize()<=1024*1024,"Adjunta un PDF, JPG o PNG de hasta 1 MB.");
        byte[] bytes=archivo.getBytes(); String mime=detectarMime(bytes);
        String nombre=archivo.getOriginalFilename()==null?"documento":archivo.getOriginalFilename().replace('\\','/');
        nombre=nombre.substring(nombre.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");
        nombre=texto(nombre,200,true);
        String lower=nombre.toLowerCase(Locale.ROOT);
        exigir(mime.equals("application/pdf")&&lower.endsWith(".pdf")||mime.equals("image/png")&&lower.endsWith(".png")||mime.equals("image/jpeg")&&(lower.endsWith(".jpg")||lower.endsWith(".jpeg")),"La extensión no coincide con el contenido del archivo.");
        long id=dao.insertar("INSERT INTO gestion_documentos(paciente_id,cita_id,presupuesto_id,titulo,tipo,nombre_archivo,mime,tamano,contenido,creado_por) VALUES(?,?,?,?,?,?,?,?,?,?)",
                paciente,cita,presupuesto,titulo,tipo,nombre,mime,bytes.length,bytes,actor);
        dao.historial("documentos",id,"ADJUNTADO",nombre,actor);return id;
    }

    public Map<String,Object> descargarDocumento(long id) {
        return dao.uno("SELECT nombre_archivo,mime,contenido FROM gestion_documentos WHERE id=?",id);
    }
    @Transactional
    public void estadoDocumento(long id,Estado input,String actor) {
        exigir(Set.of("ACTIVO","ARCHIVADO").contains(input.estado()==null?"":input.estado()),"Estado no válido.");
        var previo=dao.uno("SELECT estado FROM gestion_documentos WHERE id=? FOR UPDATE",id);
        exigir(!input.estado().equals(previo.get("estado")),"El documento ya tiene ese estado.");
        String motivo=texto(input.motivo(),1000,true);
        dao.actualizar("UPDATE gestion_documentos SET estado=? WHERE id=?",input.estado(),id);
        dao.historial("documentos",id,input.estado(),motivo,actor);
    }

    public String exportarCobros(Filtro f) {
        validarFiltro(f);var c=dao.filtrada("pagos",f);
        // Límite explícito: nunca entregar una exportación truncada silenciosamente.
        var filas=dao.consultar(c.sql()+" ORDER BY r.fecha,r.id LIMIT 10001",c.args().toArray());
        exigir(filas.size()<=10000,"Hay más de 10 000 cobros. Reduce el rango de fechas para exportar.");
        StringBuilder csv=new StringBuilder("\uFEFFCobro;Paciente;Presupuesto;Fecha;Metodo;Referencia;Estado;Monto PEN\r\n");
        for(var r:filas) {
            for(String key:List.of("id","paciente","presupuesto_id","fecha","metodo","referencia","estado","monto")) csv.append(celdaCsv(r.get(key))).append(';');
            csv.setLength(csv.length()-1);csv.append("\r\n");
        }
        return csv.toString();
    }
    public static String celdaCsv(Object valor) {
        String s=Objects.toString(valor,"");
        if(s.stripLeading().matches("^[=+@\\-].*"))s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    public static String detectarMime(byte[] b) {
        if(b.length>=5&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F'&&b[4]=='-')return "application/pdf";
        if(b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "image/png";
        if(b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "image/jpeg";
        throw new IllegalArgumentException("El contenido debe ser PDF, JPG o PNG.");
    }
    private void pacienteActivo(Long id) {
        exigir(id!=null&&id>0,"Selecciona un paciente.");
        Object activo=dao.uno("SELECT activo FROM pacientes WHERE id_paciente=?",id).get("activo");
        exigir(Boolean.TRUE.equals(activo)||activo instanceof Number n&&n.intValue()==1,"El paciente está inactivo.");
    }
    private void validarVinculos(long paciente,Long cita,Long presupuesto) {
        if(cita!=null) exigir(numero(dao.uno("SELECT id_paciente FROM citas WHERE id_cita=?",cita),"id_paciente")==paciente,"La cita no pertenece al paciente seleccionado.");
        if(presupuesto!=null) exigir(numero(dao.uno("SELECT paciente_id FROM gestion_presupuestos WHERE id=?",presupuesto),"paciente_id")==paciente,"El presupuesto no pertenece al paciente seleccionado.");
    }
    private Map<String,Object> bloquearPresupuesto(long id){return dao.uno("SELECT * FROM gestion_presupuestos WHERE id=? FOR UPDATE",id);}
    private BigDecimal pagado(long id){return dao.decimal("SELECT COALESCE(SUM(monto),0) FROM gestion_pagos WHERE presupuesto_id=? AND estado='REGISTRADO'",id);}
    private static void version(Map<String,Object> row,Integer version){exigir(version!=null&&numero(row,"version")==version,"El registro cambió en otra sesión. Actualiza la página y vuelve a intentarlo.");}
    private static long numero(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static void exigir(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
    private static String texto(String value,int max,boolean required){String v=value==null?"":value.trim();exigir(v.length()<=max&&(!required||!v.isBlank()),required?"Completa los campos obligatorios y respeta la longitud máxima.":"El texto supera la longitud máxima de "+max+" caracteres.");return v;}
    private static BigDecimal dinero(BigDecimal value) {
        exigir(value!=null&&value.signum()>0&&value.compareTo(new BigDecimal("9999999999.99"))<=0,"El importe debe ser positivo y no superar S/ 9 999 999 999,99.");
        try{return value.setScale(2,RoundingMode.UNNECESSARY);}catch(ArithmeticException e){throw new IllegalArgumentException("Usa como máximo dos decimales en los importes.");}
    }
    private static void validarFiltro(Filtro f){exigir(f.pagina()>=0&&f.pagina()<=100000,"Página no válida.");exigir(f.q()==null||f.q().length()<=100,"Búsqueda demasiado larga.");exigir(f.desde()==null||f.hasta()==null||!f.desde().isAfter(f.hasta()),"La fecha inicial debe ser anterior a la final.");}
}
