package com.dentia.dao.interfaces;

import com.dentia.model.interno.MarcaIdentidadConfig;

public interface IMarcaIdentidadConfigDao {

    MarcaIdentidadConfig obtenerConfiguracionActiva();

    void guardar(MarcaIdentidadConfig config);

    void publicar(Integer idConfig);

    void restaurar(Integer idConfig);
}