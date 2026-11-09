package com.dentia.service.interno;

import com.dentia.dao.interfaces.IMarcaIdentidadConfigDao;
import com.dentia.model.interno.MarcaIdentidadConfig;
import org.springframework.stereotype.Service;

@Service
public class MarcaIdentidadService {

    private static final String LOGO_DENTIA = "/img/dentia-mark.svg";
    private static final String LOGO_HEREDADO = "/img/logos/logodentia.png";

    private final IMarcaIdentidadConfigDao marcaIdentidadConfigDao;

    public MarcaIdentidadService(IMarcaIdentidadConfigDao marcaIdentidadConfigDao) {
        this.marcaIdentidadConfigDao = marcaIdentidadConfigDao;
    }

    public MarcaIdentidadConfig obtenerConfiguracion() {
        MarcaIdentidadConfig config = marcaIdentidadConfigDao.obtenerConfiguracionActiva();
        if (config != null) {
            config.setLogoOpcion1(reemplazarLogoHeredado(config.getLogoOpcion1()));
            config.setLogoOpcion2(reemplazarLogoHeredado(config.getLogoOpcion2()));
            config.setLogoOpcion3(reemplazarLogoHeredado(config.getLogoOpcion3()));
            config.setLogoPrincipal(valorBase(reemplazarLogoHeredado(config.getLogoPrincipal()), LOGO_DENTIA));
            config.setLogoClaro(reemplazarLogoHeredado(config.getLogoClaro()));
            config.setLogoOscuro(reemplazarLogoHeredado(config.getLogoOscuro()));
            config.setIsotipo(reemplazarLogoHeredado(config.getIsotipo()));
            config.setFavicon(reemplazarLogoHeredado(config.getFavicon()));
        }
        return config;
    }

    public void guardar(MarcaIdentidadConfig config) {
        MarcaIdentidadConfig actual = obtenerConfiguracion();

        if (config.getIdConfig() == null) {
            config.setIdConfig(actual.getIdConfig());
        }

        completarDatosFaltantes(config, actual);
        normalizarDatos(config);
        sincronizarLogoPrincipal(config);

        marcaIdentidadConfigDao.guardar(config);
    }

    public void publicar() {
        MarcaIdentidadConfig actual = marcaIdentidadConfigDao.obtenerConfiguracionActiva();
        marcaIdentidadConfigDao.publicar(actual.getIdConfig());
    }

    public void restaurar() {
        MarcaIdentidadConfig actual = marcaIdentidadConfigDao.obtenerConfiguracionActiva();
        marcaIdentidadConfigDao.restaurar(actual.getIdConfig());
    }

    private void completarDatosFaltantes(MarcaIdentidadConfig config, MarcaIdentidadConfig actual) {
        if (config.getLogoOpcion1() == null) {
            config.setLogoOpcion1(actual.getLogoOpcion1());
        }

        if (config.getLogoOpcion2() == null) {
            config.setLogoOpcion2(actual.getLogoOpcion2());
        }

        if (config.getLogoOpcion3() == null) {
            config.setLogoOpcion3(actual.getLogoOpcion3());
        }

        if (config.getLogoActivo() == null) {
            config.setLogoActivo(actual.getLogoActivo());
        }
    }

    private void normalizarDatos(MarcaIdentidadConfig config) {
        config.setNombreComercial(valorBase(config.getNombreComercial(), "DentIA"));
        // La empresa tiene una sola identidad. Estos valores derivados no se
        // editan por separado para evitar nombres distintos en web y documentos.
        config.setRazonSocial(config.getNombreComercial());
        config.setSlogan("Clínica Odontológica");

        config.setRuc(valorBase(config.getRuc(), ""));
        config.setDescripcionCorta("Clínica odontológica");

        config.setLogoOpcion1(valorBase(reemplazarLogoHeredado(config.getLogoOpcion1()), LOGO_DENTIA));
        config.setLogoOpcion2(valorBase(config.getLogoOpcion2(), ""));
        config.setLogoOpcion3(valorBase(config.getLogoOpcion3(), ""));

        config.setLogoActivo(validarLogoActivo(config.getLogoActivo()));

        config.setLogoPrincipal(valorBase(reemplazarLogoHeredado(config.obtenerLogoActivo()), LOGO_DENTIA));
        config.setLogoClaro(valorBase(config.getLogoClaro(), config.getLogoPrincipal()));
        config.setLogoOscuro(valorBase(config.getLogoOscuro(), config.getLogoPrincipal()));
        config.setIsotipo(valorBase(config.getIsotipo(), ""));
        config.setFavicon(valorBase(config.getFavicon(), ""));

        config.setColorPrimario(valorBase(config.getColorPrimario(), "#0B3D91"));
        config.setColorSecundario(valorBase(config.getColorSecundario(), "#009688"));
        config.setColorAcento(valorBase(config.getColorAcento(), "#F59E0B"));
        config.setColorNeutroOscuro(valorBase(config.getColorNeutroOscuro(), "#111827"));
        config.setColorNeutroMedio(valorBase(config.getColorNeutroMedio(), "#6B7280"));
        config.setColorNeutroClaro(valorBase(config.getColorNeutroClaro(), "#F3F4F6"));

        config.setWhatsappPrincipal(valorBase(config.getWhatsappPrincipal(), ""));
        config.setTelefonoPrincipal(valorBase(config.getTelefonoPrincipal(), ""));
        config.setCorreoContacto(valorBase(config.getCorreoContacto(), ""));
        config.setSitioWeb(valorBase(config.getSitioWeb(), ""));
        config.setDireccionPrincipal(valorBase(config.getDireccionPrincipal(), ""));
        config.setHorarioGeneral(valorBase(config.getHorarioGeneral(), ""));
        config.setNombreDocumentos(config.getNombreComercial());
        config.setPieDocumentos(construirPieDocumentos(config));

        config.setFacebookUrl(valorBase(config.getFacebookUrl(), ""));
        config.setInstagramUrl(valorBase(config.getInstagramUrl(), ""));
        config.setTiktokUrl(valorBase(config.getTiktokUrl(), ""));
        config.setLinkedinUrl(valorBase(config.getLinkedinUrl(), ""));
    }

    private String construirPieDocumentos(MarcaIdentidadConfig config) {
        StringBuilder pie = new StringBuilder(config.getNombreComercial());
        agregarDatoPie(pie, config.getRuc().isBlank() ? "" : "RUC " + config.getRuc());
        agregarDatoPie(pie, config.getDireccionPrincipal());
        agregarDatoPie(pie, config.getWhatsappPrincipal());
        return pie.toString();
    }

    private void agregarDatoPie(StringBuilder pie, String dato) {
        if (dato != null && !dato.isBlank()) {
            pie.append(" · ").append(dato.trim());
        }
    }

    private void sincronizarLogoPrincipal(MarcaIdentidadConfig config) {
        String logoActivo = config.obtenerLogoActivo();

        if (logoActivo == null || logoActivo.trim().isEmpty()) {
            logoActivo = LOGO_DENTIA;
        }

        config.setLogoPrincipal(logoActivo);
        config.setLogoClaro(logoActivo);
        config.setLogoOscuro(logoActivo);
    }

    private Integer validarLogoActivo(Integer logoActivo) {
        if (logoActivo == null) {
            return 1;
        }

        if (logoActivo < 1 || logoActivo > 3) {
            return 1;
        }

        return logoActivo;
    }

    private String valorBase(String valor, String defecto) {
        if (valor == null || valor.trim().isEmpty()) {
            return defecto;
        }

        return valor.trim();
    }

    private String reemplazarLogoHeredado(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return "";
        }
        String clean = ruta.trim();
        if (clean.equalsIgnoreCase(LOGO_HEREDADO)
                || clean.toLowerCase().contains("vialsa")
                || clean.toLowerCase().startsWith("/img/logos/")) {
            return "";
        }
        return clean;
    }
}
