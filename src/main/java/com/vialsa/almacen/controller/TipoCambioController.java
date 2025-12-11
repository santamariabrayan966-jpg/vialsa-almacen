package com.vialsa.almacen.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class TipoCambioController {

    @GetMapping("/api/tipo-cambio")
    public Map<String, Object> obtenerTipoCambio() {

        String url = "https://api.apis.net.pe/v1/tipo-cambio-sunat";

        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.getForObject(url, Map.class);
    }
}
