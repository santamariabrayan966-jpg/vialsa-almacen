package com.dentia.service.interno;

import com.dentia.dao.Jdbc.JdbcClinicaDao;
import com.dentia.service.clinica.AtencionClinicaService;
import com.dentia.service.clinica.OdontogramaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ClinicaPresupuestoServiceTest {
    private JdbcClinicaDao dao;
    private ClinicaInternaService service;

    @BeforeEach
    void preparar() {
        dao = mock(JdbcClinicaDao.class);
        when(dao.insertar(anyString(), any(Object[].class))).thenReturn(77L);
        service = new ClinicaInternaService(
                dao,
                mock(OdontogramaService.class),
                mock(AtencionClinicaService.class));
    }

    @Test
    void reutilizaPresupuestoYaVinculado() {
        when(dao.obtenerPlanParaGenerarPresupuesto(12L)).thenReturn(plan(12L, 901L, "250.00"));

        assertEquals(901L, service.generarPresupuestoDesdePlan(12L, "doctor"));
        verify(dao, never()).listarItemsPlan(anyLong());
        verify(dao, never()).insertar(anyString(), any(Object[].class));
        verify(dao, never()).vincularPresupuestoPlan(anyLong(), anyLong());
    }

    @Test
    void generaPresupuestoYVinculaElPlan() {
        when(dao.obtenerPlanParaGenerarPresupuesto(12L)).thenReturn(plan(12L, null, "250.00"));
        when(dao.listarItemsPlan(12L)).thenReturn(List.of(item("Limpieza", "250.00")));

        assertEquals(77L, service.generarPresupuestoDesdePlan(12L, null));
        verify(dao).insertar(argThat(sql -> sql.contains("INSERT INTO gestion_presupuestos")), any(Object[].class));
        verify(dao).vincularPresupuestoPlan(12L, 77L);
        verify(dao).insertar(argThat(sql -> sql.contains("INSERT INTO gestion_historial")), any(Object[].class));
    }

    @Test
    void sumaItemsCuandoElPlanNoTieneTotal() {
        when(dao.obtenerPlanParaGenerarPresupuesto(12L)).thenReturn(plan(12L, null, "0"));
        when(dao.listarItemsPlan(12L)).thenReturn(List.of(
                item("Evaluación", "100.00"), item("Radiografía", "50.00")));

        service.generarPresupuestoDesdePlan(12L, "doctor");
        verify(dao).insertar(argThat(sql -> sql.contains("INSERT INTO gestion_presupuestos")),
                eq(7L), contains("Presupuesto: Plan"), any(), eq(new BigDecimal("150.00")), anyString(), eq("doctor"));
    }

    @Test
    void rechazaPrecioCeroAntesDeCrearPresupuesto() {
        when(dao.obtenerPlanParaGenerarPresupuesto(12L)).thenReturn(plan(12L, null, "250.00"));
        when(dao.listarItemsPlan(12L)).thenReturn(List.of(item("Limpieza", "0")));

        var error = assertThrows(IllegalArgumentException.class,
                () -> service.generarPresupuestoDesdePlan(12L, "doctor"));
        assertTrue(error.getMessage().contains("precio mayor que cero"));
        verify(dao, never()).insertar(anyString(), any(Object[].class));
    }

    @Test
    void rechazaPlanSinCostoNiPreciosValidos() {
        when(dao.obtenerPlanParaGenerarPresupuesto(12L)).thenReturn(plan(12L, null, null));
        when(dao.listarItemsPlan(12L)).thenReturn(List.of(item("Limpieza", "0")));

        assertThrows(IllegalArgumentException.class,
                () -> service.generarPresupuestoDesdePlan(12L, "doctor"));
        verify(dao, never()).insertar(anyString(), any(Object[].class));
    }

    @Test
    void apruebaPlanYGeneraDeudaDirectamente() {
        when(dao.obtenerPlanParaGenerarPresupuesto(15L)).thenReturn(plan(15L, null, "180.00"));
        when(dao.obtenerPlan(15L)).thenReturn(plan(15L, 77L, "180.00"));
        when(dao.listarItemsPlan(15L)).thenReturn(List.of(
                item("Bracket", "30.00"), item("Curación molar", "150.00")));

        var res = service.aprobarPlanYGenerarDeuda(15L, "doctor");
        assertNotNull(res);
        assertEquals(true, res.get("ok"));
        assertEquals(15L, res.get("planId"));
        assertEquals(77L, res.get("presupuestoId"));
        assertEquals(new BigDecimal("180.00"), res.get("total"));

        verify(dao).actualizarEstadoPlan(15L, "APROBADO");
        verify(dao).actualizar(contains("UPDATE gestion_presupuestos SET estado='APROBADO'"), eq(77L));
    }


    private static Map<String, Object> plan(long id, Long presupuestoId, String total) {
        var plan = new HashMap<String, Object>();
        plan.put("id", id);
        plan.put("paciente_id", 7L);
        plan.put("titulo", "Plan");
        plan.put("costo_total", total == null ? null : new BigDecimal(total));
        plan.put("presupuesto_id", presupuestoId);
        return plan;
    }

    private static Map<String, Object> item(String descripcion, String precio) {
        var item = new HashMap<String, Object>();
        item.put("descripcion", descripcion);
        item.put("precio", new BigDecimal(precio));
        return item;
    }
}
