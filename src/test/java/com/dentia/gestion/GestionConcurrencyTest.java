package com.dentia.gestion;

import com.dentia.dao.Jdbc.JdbcGestionDao;
import com.dentia.dto.gestion.GestionRequests.*;
import com.dentia.service.interno.GestionInternaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("integration")
@JdbcTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({JdbcGestionDao.class,GestionInternaService.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class GestionConcurrencyTest {
    @Autowired JdbcGestionDao dao;
    @Autowired GestionInternaService service;
    @Test void dosCajasNoPuedenCobrarMasDelSaldo() throws Exception {
        long paciente=dao.insertar("INSERT INTO pacientes(nombres,apellidos,fecha_nacimiento,celular,activo) VALUES('Concurrencia QA',?,'1990-01-01','999000000',1)",UUID.randomUUID().toString());
        Long presupuesto=null;var executor=Executors.newFixedThreadPool(2);
        try {
            presupuesto=service.guardarPresupuesto(null,new Presupuesto(paciente,"QA simultáneo",LocalDate.now().plusDays(1),"",List.of(new Item("QA",1,new BigDecimal("100"))),0),"QA");
            service.estadoPresupuesto(presupuesto,new Estado("APROBADO","",0),"QA");
            long id=presupuesto;var salida=new CountDownLatch(1);
            Callable<Boolean> cobrar=()->{salida.await();try{service.registrarPago(new Pago(id,new BigDecimal("80"),LocalDate.now(),"EFECTIVO","",UUID.randomUUID().toString()),"QA");return true;}catch(IllegalArgumentException expected){return false;}};
            var a=executor.submit(cobrar);var b=executor.submit(cobrar);salida.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertEquals(new BigDecimal("20.00"),service.detalle("presupuestos",id).get("saldo"));
        } finally {
            executor.shutdownNow();executor.awaitTermination(10,TimeUnit.SECONDS);
            // Se eliminan únicamente las filas identificadas creadas por esta prueba.
            if(presupuesto!=null){
                for(var pago:dao.consultar("SELECT id FROM gestion_pagos WHERE presupuesto_id=?",presupuesto))dao.actualizar("DELETE FROM gestion_historial WHERE modulo='pagos' AND registro_id=?",pago.get("id"));
                dao.actualizar("DELETE FROM gestion_historial WHERE modulo='presupuestos' AND registro_id=?",presupuesto);
                dao.actualizar("DELETE FROM gestion_pagos WHERE presupuesto_id=?",presupuesto);
                dao.actualizar("DELETE FROM gestion_presupuesto_items WHERE presupuesto_id=?",presupuesto);
                dao.actualizar("DELETE FROM gestion_presupuestos WHERE id=?",presupuesto);
            }
            dao.actualizar("DELETE FROM pacientes WHERE id_paciente=?",paciente);
        }
    }
}
