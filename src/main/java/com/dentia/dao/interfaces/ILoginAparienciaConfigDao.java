package com.dentia.dao.interfaces;

import com.dentia.model.interno.LoginAparienciaConfig;

public interface ILoginAparienciaConfigDao {

    LoginAparienciaConfig obtenerConfiguracion();

    void guardar(LoginAparienciaConfig config);
}
