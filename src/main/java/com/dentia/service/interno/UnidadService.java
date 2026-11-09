package com.dentia.service.interno;

import com.dentia.dao.interfaces.IUnidadDao;
import com.dentia.model.interno.Unidad;
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
