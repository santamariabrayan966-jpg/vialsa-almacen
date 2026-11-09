package com.dentia.gestion;

import com.dentia.dao.Jdbc.JdbcGestionDao;
import com.dentia.dto.gestion.GestionRequests.*;
import com.dentia.service.interno.GestionInternaService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Cada prueba usa una transacción revertida: no deja pacientes ni cobros de prueba. */
@org.junit.jupiter.api.Tag("integration")
@JdbcTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({JdbcGestionDao.class,GestionInternaService.class})
class GestionInternaIntegrationTest {
    @Autowired GestionInternaService service;
    @Autowired JdbcGestionDao dao;
    @Autowired JdbcTemplate jdbc;
    long paciente,otro;
    @BeforeEach void crearPacientes(){
        paciente=paciente("Gestión QA");otro=paciente("Otro QA");
    }
    long paciente(String nombre){return dao.insertar("INSERT INTO pacientes(nombres,apellidos,fecha_nacimiento,celular,activo) VALUES(?,?,'1990-01-01','999000000',1)",nombre,UUID.randomUUID().toString());}
    Presupuesto input(long p,String titulo){return new Presupuesto(p,titulo,LocalDate.now().plusDays(30),"Prueba reversible",List.of(new Item("Concepto A",2,new BigDecimal("100.25")),new Item("Concepto B",1,new BigDecimal("49.50"))),0);}
    long aprobado(){long id=service.guardarPresupuesto(null,input(paciente,"Presupuesto QA"),"QA");service.estadoPresupuesto(id,new Estado("APROBADO","",0),"QA");return id;}
    Pago pago(long id,String importe,String token){return new Pago(id,new BigDecimal(importe),LocalDate.now(),"EFECTIVO","",token);}
    Filtro filtro(long paciente){return new Filtro(paciente,null,null,null,null,0);}
    BigDecimal saldo(long id){return (BigDecimal)service.detalle("presupuestos",id).get("saldo");}

    @Test void presupuestoAbonoDocumentoSeguimientoYReporteConectados() throws Exception {
        long presupuesto=aprobado();assertEquals(new BigDecimal("250.00"),saldo(presupuesto));
        long cobro=service.registrarPago(pago(presupuesto,"80.00",UUID.randomUUID().toString()),"QA");
        assertEquals(new BigDecimal("170.00"),saldo(presupuesto));
        long seguimiento=service.guardarSeguimiento(null,new Seguimiento(paciente,null,presupuesto,"Contacto QA",LocalDateTime.now().plusDays(1),"LLAMADA","",0),"QA");
        service.cerrarSeguimiento(seguimiento,new Estado("COMPLETADO","Contacto realizado",0),"QA");
        var archivo=new MockMultipartFile("archivo","prueba.pdf","application/pdf","%PDF-1.4\nQA".getBytes());
        long documento=service.guardarDocumento(paciente,null,presupuesto,"Documento QA","INFORME",archivo,"QA");
        assertArrayEquals(archivo.getBytes(),(byte[])service.descargarDocumento(documento).get("contenido"));
        assertEquals("COMPLETADO",service.detalle("seguimiento",seguimiento).get("estado"));
        var reporte=service.listar("reportes",filtro(paciente));
        assertEquals(new BigDecimal("80.00"),((Map<?,?>)reporte.get("cobros")).get("cobrado"));
        assertEquals(new BigDecimal("170.00"),((Map<?,?>)reporte.get("presupuestos")).get("pendiente"));
        assertTrue(service.exportarCobros(filtro(paciente)).contains("80.00"));
        for(String modulo:List.of("presupuestos","pagos","seguimiento","documentos"))assertEquals(1L,service.listar(modulo,filtro(paciente)).get("total"));
        service.anularPago(cobro,"Registro equivocado","QA");assertEquals(new BigDecimal("250.00"),saldo(presupuesto));
        assertEquals("ANULADO",service.detalle("pagos",cobro).get("estado"));
    }
    @Test void evitaSobrepagoYReenvioDuplicado(){
        long id=aprobado();String token=UUID.randomUUID().toString();
        long primero=service.registrarPago(pago(id,"200.00",token),"QA");
        assertEquals(primero,service.registrarPago(pago(id,"200.00",token),"QA"));
        assertThrows(IllegalArgumentException.class,()->service.registrarPago(pago(id,"200.01",token),"QA"));
        assertThrows(IllegalArgumentException.class,()->service.registrarPago(pago(id,"50.01",UUID.randomUUID().toString()),"QA"));
        assertEquals(new BigDecimal("50.00"),saldo(id));
        service.registrarPago(pago(id,"50.00",UUID.randomUUID().toString()),"QA");assertEquals(new BigDecimal("0.00"),saldo(id));
    }
    @Test void noCobraBorradoresNiAnulaPresupuestoConAbonos(){
        long id=service.guardarPresupuesto(null,input(paciente,"Borrador QA"),"QA");
        assertThrows(IllegalArgumentException.class,()->service.registrarPago(pago(id,"1.00",UUID.randomUUID().toString()),"QA"));
        service.estadoPresupuesto(id,new Estado("APROBADO","",0),"QA");
        service.registrarPago(pago(id,"1.00",UUID.randomUUID().toString()),"QA");
        assertThrows(IllegalArgumentException.class,()->service.estadoPresupuesto(id,new Estado("ANULADO","Motivo",1),"QA"));
        assertThrows(IllegalArgumentException.class,()->service.guardarPresupuesto(id,new Presupuesto(paciente,"Editado",LocalDate.now().plusDays(1),"",input(paciente,"").items(),1),"QA"));
    }
    @Test void bloqueaVinculosDeOtroPaciente(){
        long id=aprobado();
        assertThrows(IllegalArgumentException.class,()->service.guardarSeguimiento(null,new Seguimiento(otro,null,id,"QA",LocalDateTime.now(),"LLAMADA","",0),"QA"));
        assertThrows(IllegalArgumentException.class,()->service.guardarDocumento(otro,null,id,"QA","INFORME",new MockMultipartFile("archivo","a.pdf","application/pdf","%PDF-1.4".getBytes()),"QA"));
    }
    @Test void editaBorradorYDetectaVersionObsoleta(){
        long id=service.guardarPresupuesto(null,input(paciente,"Original"),"QA");
        var nuevo=new Presupuesto(paciente,"Actualizado",LocalDate.now().plusDays(2),"",List.of(new Item("Único",1,new BigDecimal("35.00"))),0);
        service.guardarPresupuesto(id,nuevo,"QA");assertEquals(new BigDecimal("35.00"),saldo(id));
        assertThrows(IllegalArgumentException.class,()->service.guardarPresupuesto(id,nuevo,"QA"));
        assertEquals(1,((List<?>)service.detalle("presupuestos",id).get("items")).size());
    }
    @Test void archivaSinBorrarYValidaArchivos() throws Exception {
        long id=service.guardarDocumento(paciente,null,null,"QA","OTRO",new MockMultipartFile("archivo","qa.pdf","application/pdf","%PDF-1.4".getBytes()),"QA");
        service.estadoDocumento(id,new Estado("ARCHIVADO","Reemplazado",null),"QA");
        assertNotNull(service.descargarDocumento(id).get("contenido"));
        service.estadoDocumento(id,new Estado("ACTIVO","Restaurado",null),"QA");
        assertEquals("ACTIVO",service.detalle("documentos",id).get("estado"));
        assertThrows(IllegalArgumentException.class,()->service.guardarDocumento(paciente,null,null,"QA","OTRO",new MockMultipartFile("archivo","mal.pdf","application/pdf","<script>bad</script>".getBytes()),"QA"));
        assertThrows(IllegalArgumentException.class,()->service.guardarDocumento(paciente,null,null,"QA","OTRO",new MockMultipartFile("archivo","mal.html","application/pdf","%PDF-1.4".getBytes()),"QA"));
        assertThrows(IllegalArgumentException.class,()->service.guardarDocumento(paciente,null,null,"QA","OTRO",new MockMultipartFile("archivo","big.pdf","application/pdf",new byte[1048577]),"QA"));
    }
    @Test void filtrosPaginanYSumanTodosLosRegistros(){
        for(int i=0;i<23;i++)service.guardarPresupuesto(null,input(paciente,"QA "+i),"QA");
        var primera=service.listar("presupuestos",filtro(paciente));
        assertEquals(23L,primera.get("total"));assertEquals(20,((List<?>)primera.get("filas")).size());
        var segunda=service.listar("presupuestos",new Filtro(paciente,null,null,null,null,1));assertEquals(3,((List<?>)segunda.get("filas")).size());
        assertThrows(IllegalArgumentException.class,()->service.listar("pagos",new Filtro(null,null,null,LocalDate.now(),LocalDate.now().minusDays(1),0)));
    }
    @Test void dineroExactoYDatosObligatorios(){
        assertThrows(IllegalArgumentException.class,()->service.guardarPresupuesto(null,new Presupuesto(paciente,"QA",LocalDate.now(),"",List.of(new Item("QA",1,new BigDecimal("0.001"))),0),"QA"));
        assertThrows(IllegalArgumentException.class,()->service.guardarPresupuesto(null,new Presupuesto(paciente,"QA",LocalDate.now(),"",List.of(),0),"QA"));
        assertThrows(IllegalArgumentException.class,()->service.guardarPresupuesto(null,new Presupuesto(paciente,"QA",LocalDate.now().minusDays(1),"",input(paciente,"").items(),0),"QA"));
        assertEquals("\"'=1+1\"",GestionInternaService.celdaCsv("=1+1"));
        assertEquals("\"a\"\"b\"",GestionInternaService.celdaCsv("a\"b"));
    }
}
