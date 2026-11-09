package com.dentia.service.interno;

import com.dentia.dao.interfaces.IMarcaIdentidadConfigDao;
import com.dentia.model.interno.MarcaIdentidadConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarcaIdentidadServiceTest {

    @Test
    void unLogoHeredadoNoApareceEnElLoginNiEnLaAplicacionInstalada() {
        IMarcaIdentidadConfigDao dao = mock(IMarcaIdentidadConfigDao.class);
        MarcaIdentidadConfig actual = new MarcaIdentidadConfig();
        actual.setLogoPrincipal("/img/logos/logodentia.png");
        actual.setLogoOpcion1("/img/logos/logodentia.png");
        when(dao.obtenerConfiguracionActiva()).thenReturn(actual);

        MarcaIdentidadConfig visible = new MarcaIdentidadService(dao).obtenerConfiguracion();

        assertEquals("/img/dentia-mark.svg", visible.getLogoPrincipal());
        assertEquals("", visible.getLogoOpcion1());
    }

    @Test
    void usaUnaSolaIdentidadEnLaWebYLosDocumentos() {
        IMarcaIdentidadConfigDao dao = mock(IMarcaIdentidadConfigDao.class);
        MarcaIdentidadConfig actual = new MarcaIdentidadConfig();
        actual.setIdConfig(1);
        actual.setLogoOpcion1("/logo-anterior.png");
        actual.setLogoActivo(1);
        when(dao.obtenerConfiguracionActiva()).thenReturn(actual);

        MarcaIdentidadConfig cambios = new MarcaIdentidadConfig();
        cambios.setNombreComercial("  Sonrisa Norte  ");
        cambios.setRazonSocial("Nombre que ya no debe usarse");
        cambios.setNombreDocumentos("Otro nombre");
        cambios.setRuc("20123456789");
        cambios.setDireccionPrincipal("Av. Central 123");
        cambios.setWhatsappPrincipal("999 888 777");

        new MarcaIdentidadService(dao).guardar(cambios);

        assertEquals("Sonrisa Norte", cambios.getNombreComercial());
        assertEquals("Sonrisa Norte", cambios.getRazonSocial());
        assertEquals("Sonrisa Norte", cambios.getNombreDocumentos());
        assertEquals("Clínica odontológica", cambios.getDescripcionCorta());
        assertEquals("Sonrisa Norte · RUC 20123456789 · Av. Central 123 · 999 888 777",
                cambios.getPieDocumentos());
        verify(dao).guardar(cambios);
    }
}
