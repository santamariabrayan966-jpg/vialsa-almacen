package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Cliente;
import com.vialsa.almacen.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // 🔍 Buscar cliente por DNI o RUC
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorDocumento(@RequestParam("documento") String documento) {
        try {
            Cliente cliente = clienteService.buscarPorDocumento(documento);
            if (cliente != null) {
                return ResponseEntity.ok(cliente);
            } else {
                return ResponseEntity.status(404).body("Cliente no encontrado");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al buscar el cliente: " + e.getMessage());
        }
    }
}
