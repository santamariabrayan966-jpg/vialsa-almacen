package com.dentia.service.interno;

import com.dentia.dao.interfaces.IPanelColoresConfigDao;
import com.dentia.model.interno.PanelColoresConfig;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PanelColoresConfigService {

    public static final String COLOR_HEADER = "#0f2f4f";
    public static final String COLOR_HEADER_SECUNDARIO = "#0f766e";
    public static final String COLOR_FONDO = "#f4f7fb";
    public static final String COLOR_TARJETA = "#ffffff";
    public static final String COLOR_BOTON_PRIMARIO = "#0d6efd";
    public static final String COLOR_ACENTO = "#00a6d6";
    public static final String COLOR_TEXTO_PRINCIPAL = "#0f172a";
    public static final String COLOR_TEXTO_HEADER = "#ffffff";
    public static final String COLOR_FOOTER = "#111827";
    public static final String COLOR_TEXTO_FOOTER = "#ffffff";
    public static final String POSICION_MENU = "ARRIBA";

    private static final Pattern HEX_COLOR = Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$");

    private final IPanelColoresConfigDao panelColoresConfigDao;

    public PanelColoresConfigService(IPanelColoresConfigDao panelColoresConfigDao) {
        this.panelColoresConfigDao = panelColoresConfigDao;
    }

    public PanelColoresConfig obtenerConfiguracion() {
        PanelColoresConfig config = panelColoresConfigDao.obtenerConfiguracion();
        if (config == null) {
            config = crearDefault();
        }

        normalizar(config);
        return config;
    }

    public void guardar(PanelColoresConfig config) {
        if (config == null) {
            config = crearDefault();
        }

        normalizar(config);
        panelColoresConfigDao.guardar(config);
    }

    public void restaurarDefaults() {
        panelColoresConfigDao.guardar(crearDefault());
    }

    public PanelColoresConfig crearDefault() {
        PanelColoresConfig config = new PanelColoresConfig();
        config.setIdConfig(1);
        config.setColorHeader(COLOR_HEADER);
        config.setColorHeaderSecundario(COLOR_HEADER_SECUNDARIO);
        config.setColorFondo(COLOR_FONDO);
        config.setColorTarjeta(COLOR_TARJETA);
        config.setColorBotonPrimario(COLOR_BOTON_PRIMARIO);
        config.setColorAcento(COLOR_ACENTO);
        config.setColorTextoPrincipal(COLOR_TEXTO_PRINCIPAL);
        config.setColorTextoHeader(COLOR_TEXTO_HEADER);
        config.setColorFooter(COLOR_FOOTER);
        config.setColorTextoFooter(COLOR_TEXTO_FOOTER);
        config.setPosicionMenu(POSICION_MENU);
        return config;
    }

    private void normalizar(PanelColoresConfig c) {
        c.setIdConfig(1);
        c.setColorHeader(color(c.getColorHeader(), COLOR_HEADER));
        c.setColorHeaderSecundario(color(c.getColorHeaderSecundario(), COLOR_HEADER_SECUNDARIO));
        c.setColorFondo(color(c.getColorFondo(), COLOR_FONDO));
        c.setColorTarjeta(color(c.getColorTarjeta(), COLOR_TARJETA));
        c.setColorBotonPrimario(color(c.getColorBotonPrimario(), COLOR_BOTON_PRIMARIO));
        c.setColorAcento(color(c.getColorAcento(), COLOR_ACENTO));
        c.setColorTextoPrincipal(color(c.getColorTextoPrincipal(), COLOR_TEXTO_PRINCIPAL));
        c.setColorTextoHeader(color(c.getColorTextoHeader(), COLOR_TEXTO_HEADER));
        c.setColorFooter(color(c.getColorFooter(), COLOR_FOOTER));
        c.setColorTextoFooter(color(c.getColorTextoFooter(), COLOR_TEXTO_FOOTER));
        c.setPosicionMenu(posicionMenu(c.getPosicionMenu()));
    }

    private String color(String valor, String defecto) {
        if (valor == null || valor.isBlank()) {
            return defecto;
        }

        String limpio = valor.trim();
        return HEX_COLOR.matcher(limpio).matches() ? limpio : defecto;
    }

    private String posicionMenu(String valor) {
        if (valor == null || valor.isBlank()) {
            return POSICION_MENU;
        }

        String limpio = valor.trim().toUpperCase();
        return switch (limpio) {
            case "ARRIBA", "IZQUIERDA", "DERECHA", "ABAJO" -> limpio;
            default -> POSICION_MENU;
        };
    }
}
