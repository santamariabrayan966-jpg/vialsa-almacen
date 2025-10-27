package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IUnidadDao;
import com.vialsa.almacen.model.Unidad;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UnidadService {

    private final IUnidadDao dao;

    public UnidadService(IUnidadDao dao) {
        this.dao = dao;
    }

    public List<Unidad> listar() {
        return dao.listar();
    }
}
