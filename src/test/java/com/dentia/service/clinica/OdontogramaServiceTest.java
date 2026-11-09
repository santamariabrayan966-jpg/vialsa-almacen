package com.dentia.service.clinica;

import com.dentia.dao.Jdbc.JdbcClinicaDao;
import com.dentia.dto.clinica.ClinicaRequests.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OdontogramaServiceTest {
    @Test void aceptaDenticionPermanenteYTemporalRechazaFDIInexistente() {
        for (int cuadrante=1;cuadrante<=8;cuadrante++) for (int pieza=1;pieza<=(cuadrante<=4?8:5);pieza++)
            OdontogramaService.validarPieza(cuadrante*10+pieza);
        for (int numero:List.of(0,10,19,29,49,56,66,86,99)) assertThrows(IllegalArgumentException.class,()->OdontogramaService.validarPieza(numero));
    }
    @Test void unLoteInvalidoNoEscribeNingunaPieza() {
        var dao=mock(JdbcClinicaDao.class); var service=new OdontogramaService(dao);
        var input=new ConfiguracionOdontogramaInput(1L,0L,List.of(new PosicionPiezaInput(16,0d,"PRESENTE"),new PosicionPiezaInput(19,0d,"PRESENTE")));
        assertThrows(IllegalArgumentException.class,()->service.configurar(input,"doctor")); verifyNoInteractions(dao);
    }
    @Test void unaVersionAntiguaNoSobrescribeElHallazgoActual() {
        var dao=mock(JdbcClinicaDao.class); var service=new OdontogramaService(dao);
        when(dao.unoOpcional("SELECT id FROM clinica_odontogramas WHERE id = ? FOR UPDATE",1L)).thenReturn(Map.of("id",1L));
        when(dao.unoOpcional("SELECT version FROM clinica_odontograma_versiones WHERE odontograma_id = ?",1L)).thenReturn(Map.of("version",3L));
        assertThrows(OdontogramaService.EdicionConcurrenteException.class,()->service.registrar(new OdontogramaPiezaInput(1L,16,"OCLUSAL","CARIES","PATOLOGICO",null,2L),"doctor"));
        verify(dao,never()).guardarPiezaOdontograma(anyLong(),anyInt(),anyString(),anyString(),anyString(),any());
    }
    @Test void rechazaInclinacionFueraDeRangoYNoFinita() {
        var dao=mock(JdbcClinicaDao.class);var service=new OdontogramaService(dao);
        for(double angulo: new double[]{91,-91,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->service.configurar(new ConfiguracionOdontogramaInput(1L,0L,List.of(new PosicionPiezaInput(16,angulo,null))),"doctor"));
        verifyNoInteractions(dao);
    }
    @Test void permiteMarcarBracketsEnUnaPiezaCompleta() {
        var dao = mock(JdbcClinicaDao.class);
        when(dao.unoOpcional("SELECT id FROM clinica_odontogramas WHERE id = ? FOR UPDATE", 1L)).thenReturn(Map.of("id", 1L));
        when(dao.unoOpcional("SELECT version FROM clinica_odontograma_versiones WHERE odontograma_id = ?", 1L)).thenReturn(Map.of("version", 0L));
        new OdontogramaService(dao).registrar(new OdontogramaPiezaInput(1L, 11, "GENERAL", "BRACKETS", "REALIZADO", null, 0L), "doctor");
        verify(dao).guardarPiezaOdontograma(1L, 11, "GENERAL", "BRACKETS", "REALIZADO", null);
    }
    @Test void anotaDienteInfantilSinAlterarSusMarcas() {
        var dao = mock(JdbcClinicaDao.class);
        when(dao.unoOpcional("SELECT id FROM clinica_odontogramas WHERE id = ? FOR UPDATE", 1L)).thenReturn(Map.of("id", 1L));
        when(dao.unoOpcional("SELECT version FROM clinica_odontograma_versiones WHERE odontograma_id = ?", 1L)).thenReturn(Map.of("version", 2L));
        new OdontogramaService(dao).anotar(new NotaPiezaInput(1L, 55, 2L, "Control de erupción"), "doctor");
        verify(dao, never()).guardarPiezaOdontograma(anyLong(), anyInt(), anyString(), anyString(), anyString(), any());
        verify(dao).actualizar(contains("clinica_odontograma_historial"), eq(1L), eq(55), eq("GENERAL"), eq("NOTA"), isNull(), eq("Control de erupción"), eq("doctor"));
    }
}
