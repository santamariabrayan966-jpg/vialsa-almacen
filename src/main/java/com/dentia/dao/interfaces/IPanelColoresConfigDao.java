package com.dentia.dao.interfaces;

import com.dentia.model.interno.PanelColoresConfig;

public interface IPanelColoresConfigDao {

    PanelColoresConfig obtenerConfiguracion();

    void guardar(PanelColoresConfig config);
}
