package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IClienteDao;
import com.vialsa.almacen.model.Cliente;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final IClienteDao dao;

    public ClienteService(IClienteDao dao) {
        this.dao = dao;
    }

    public Cliente buscarPorDocumento(String documento) {
        return dao.buscarPorDocumento(documento);
    }
}
