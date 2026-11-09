package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IMarcaIdentidadConfigDao;
import com.dentia.model.interno.MarcaIdentidadConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class JdbcMarcaIdentidadConfigDao implements IMarcaIdentidadConfigDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMarcaIdentidadConfigDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MarcaIdentidadConfig obtenerConfiguracionActiva() {
        String sql = """
                SELECT *
                FROM marca_identidad_config
                ORDER BY id_config DESC
                LIMIT 1
                """;

        List<MarcaIdentidadConfig> lista = jdbcTemplate.query(sql, this::mapearMarcaIdentidad);

        if (lista.isEmpty()) {
            return crearConfigPorDefecto();
        }

        return lista.get(0);
    }

    @Override
    public void guardar(MarcaIdentidadConfig config) {
        String sql = """
                UPDATE marca_identidad_config
                SET
                    nombre_comercial = ?,
                    razon_social = ?,
                    ruc = ?,
                    slogan = ?,
                    descripcion_corta = ?,

                    logo_principal = ?,
                    logo_claro = ?,
                    logo_oscuro = ?,
                    isotipo = ?,
                    favicon = ?,

                    logo_opcion_1 = ?,
                    logo_opcion_2 = ?,
                    logo_opcion_3 = ?,
                    logo_activo = ?,

                    color_primario = ?,
                    color_secundario = ?,
                    color_acento = ?,
                    color_neutro_oscuro = ?,
                    color_neutro_medio = ?,
                    color_neutro_claro = ?,

                    whatsapp_principal = ?,
                    telefono_principal = ?,
                    correo_contacto = ?,
                    sitio_web = ?,
                    direccion_principal = ?,
                    horario_general = ?,
                    nombre_documentos = ?,
                    pie_documentos = ?,

                    facebook_url = ?,
                    instagram_url = ?,
                    tiktok_url = ?,
                    linkedin_url = ?,

                    activo = ?,
                    publicado = 0,
                    fecha_actualizacion = NOW()
                WHERE id_config = ?
                """;

        jdbcTemplate.update(sql,
                limpiar(config.getNombreComercial()),
                limpiar(config.getRazonSocial()),
                limpiar(config.getRuc()),
                limpiar(config.getSlogan()),
                limpiar(config.getDescripcionCorta()),

                limpiar(config.getLogoPrincipal()),
                limpiar(config.getLogoClaro()),
                limpiar(config.getLogoOscuro()),
                limpiar(config.getIsotipo()),
                limpiar(config.getFavicon()),

                limpiar(config.getLogoOpcion1()),
                limpiar(config.getLogoOpcion2()),
                limpiar(config.getLogoOpcion3()),
                validarLogoActivo(config.getLogoActivo()),

                limpiar(config.getColorPrimario()),
                limpiar(config.getColorSecundario()),
                limpiar(config.getColorAcento()),
                limpiar(config.getColorNeutroOscuro()),
                limpiar(config.getColorNeutroMedio()),
                limpiar(config.getColorNeutroClaro()),

                limpiar(config.getWhatsappPrincipal()),
                limpiar(config.getTelefonoPrincipal()),
                limpiar(config.getCorreoContacto()),
                limpiar(config.getSitioWeb()),
                limpiar(config.getDireccionPrincipal()),
                limpiar(config.getHorarioGeneral()),
                limpiar(config.getNombreDocumentos()),
                limpiar(config.getPieDocumentos()),

                limpiar(config.getFacebookUrl()),
                limpiar(config.getInstagramUrl()),
                limpiar(config.getTiktokUrl()),
                limpiar(config.getLinkedinUrl()),
                Boolean.TRUE.equals(config.getActivo()) ? 1 : 0,

                config.getIdConfig()
        );
    }

    @Override
    public void publicar(Integer idConfig) {
        String sql = """
                UPDATE marca_identidad_config
                SET publicado = 1,
                    fecha_actualizacion = NOW()
                WHERE id_config = ?
                """;

        jdbcTemplate.update(sql, idConfig);
    }

    @Override
    public void restaurar(Integer idConfig) {
        String sql = """
                UPDATE marca_identidad_config
                SET
                    nombre_comercial = 'DentIA',
                    razon_social = 'DentIA',
                    ruc = '',
                    slogan = 'Gestion dental inteligente',
                    descripcion_corta = 'Plataforma interna para la gestion del consultorio dental.',

                    logo_principal = '',
                    logo_claro = '',
                    logo_oscuro = '',
                    isotipo = '',
                    favicon = '',

                    logo_opcion_1 = '',
                    logo_opcion_2 = '',
                    logo_opcion_3 = '',
                    logo_activo = 1,

                    color_primario = '#0F766E',
                    color_secundario = '#115E59',
                    color_acento = '#14B8A6',
                    color_neutro_oscuro = '#1F2937',
                    color_neutro_medio = '#64748B',
                    color_neutro_claro = '#F1F5F9',

                    whatsapp_principal = '',
                    telefono_principal = '',
                    correo_contacto = '',
                    sitio_web = '',
                    direccion_principal = '',
                    horario_general = '',
                    nombre_documentos = 'DentIA',
                    pie_documentos = '',

                    facebook_url = '',
                    instagram_url = '',
                    tiktok_url = '',
                    linkedin_url = '',

                    publicado = 0,
                    fecha_actualizacion = NOW()
                WHERE id_config = ?
                """;

        jdbcTemplate.update(sql, idConfig);
    }

    private MarcaIdentidadConfig mapearMarcaIdentidad(ResultSet rs, int rowNum) throws SQLException {
        MarcaIdentidadConfig config = new MarcaIdentidadConfig();

        config.setIdConfig(rs.getInt("id_config"));

        config.setNombreComercial(rs.getString("nombre_comercial"));
        config.setRazonSocial(rs.getString("razon_social"));
        config.setRuc(rs.getString("ruc"));
        config.setSlogan(rs.getString("slogan"));
        config.setDescripcionCorta(rs.getString("descripcion_corta"));

        config.setLogoPrincipal(rs.getString("logo_principal"));
        config.setLogoClaro(rs.getString("logo_claro"));
        config.setLogoOscuro(rs.getString("logo_oscuro"));
        config.setIsotipo(rs.getString("isotipo"));
        config.setFavicon(rs.getString("favicon"));

        config.setLogoOpcion1(rs.getString("logo_opcion_1"));
        config.setLogoOpcion2(rs.getString("logo_opcion_2"));
        config.setLogoOpcion3(rs.getString("logo_opcion_3"));
        config.setLogoActivo(rs.getInt("logo_activo"));

        config.setColorPrimario(rs.getString("color_primario"));
        config.setColorSecundario(rs.getString("color_secundario"));
        config.setColorAcento(rs.getString("color_acento"));
        config.setColorNeutroOscuro(rs.getString("color_neutro_oscuro"));
        config.setColorNeutroMedio(rs.getString("color_neutro_medio"));
        config.setColorNeutroClaro(rs.getString("color_neutro_claro"));

        config.setWhatsappPrincipal(rs.getString("whatsapp_principal"));
        config.setTelefonoPrincipal(rs.getString("telefono_principal"));
        config.setCorreoContacto(rs.getString("correo_contacto"));
        config.setSitioWeb(rs.getString("sitio_web"));
        config.setDireccionPrincipal(rs.getString("direccion_principal"));
        config.setHorarioGeneral(rs.getString("horario_general"));
        config.setNombreDocumentos(rs.getString("nombre_documentos"));
        config.setPieDocumentos(rs.getString("pie_documentos"));

        config.setFacebookUrl(rs.getString("facebook_url"));
        config.setInstagramUrl(rs.getString("instagram_url"));
        config.setTiktokUrl(rs.getString("tiktok_url"));
        config.setLinkedinUrl(rs.getString("linkedin_url"));

        config.setActivo(rs.getBoolean("activo"));
        config.setPublicado(rs.getBoolean("publicado"));

        Timestamp fecha = rs.getTimestamp("fecha_actualizacion");
        if (fecha != null) {
            config.setFechaActualizacion(fecha.toLocalDateTime());
        }

        return config;
    }

    private MarcaIdentidadConfig crearConfigPorDefecto() {
        MarcaIdentidadConfig config = new MarcaIdentidadConfig();

        config.setIdConfig(1);
        config.setNombreComercial("DentIA");
        config.setRazonSocial("DentIA");
        config.setRuc("");
        config.setSlogan("Gestion dental inteligente");
        config.setDescripcionCorta("Plataforma interna para la gestion del consultorio dental.");

        config.setLogoPrincipal("");
        config.setLogoClaro("");
        config.setLogoOscuro("");
        config.setIsotipo("");
        config.setFavicon("");

        config.setLogoOpcion1("");
        config.setLogoOpcion2("");
        config.setLogoOpcion3("");
        config.setLogoActivo(1);

        config.setColorPrimario("#0F766E");
        config.setColorSecundario("#115E59");
        config.setColorAcento("#14B8A6");
        config.setColorNeutroOscuro("#1F2937");
        config.setColorNeutroMedio("#64748B");
        config.setColorNeutroClaro("#F1F5F9");

        config.setWhatsappPrincipal("");
        config.setTelefonoPrincipal("");
        config.setCorreoContacto("");
        config.setSitioWeb("");
        config.setDireccionPrincipal("");
        config.setHorarioGeneral("");
        config.setNombreDocumentos("DentIA");
        config.setPieDocumentos("");

        config.setFacebookUrl("");
        config.setInstagramUrl("");
        config.setTiktokUrl("");
        config.setLinkedinUrl("");

        config.setActivo(true);
        config.setPublicado(true);
        config.setFechaActualizacion(LocalDateTime.now());

        return config;
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
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
}
