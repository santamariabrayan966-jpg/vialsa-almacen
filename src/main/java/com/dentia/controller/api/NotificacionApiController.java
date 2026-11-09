package com.dentia.controller.api;

import com.dentia.dto.notificacion.NotificacionAlertaDto;
import com.dentia.model.paciente.Paciente;
import com.dentia.service.interno.PacienteInternoService;
import com.dentia.service.notificacion.AlertaNotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionApiController {

    private final AlertaNotificacionService alertaService;
    private final PacienteInternoService pacienteService;

    public NotificacionApiController(AlertaNotificacionService alertaService,
                                     PacienteInternoService pacienteService) {
        this.alertaService = alertaService;
        this.pacienteService = pacienteService;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<Map<String, Object>> getNotificacionesPendientes() {
        List<NotificacionAlertaDto> items = alertaService.obtenerAlertasPendientes();

        long noAsistioCount = items.stream().filter(a -> "NO_ASISTIO".equals(a.getTipo())).count();
        long hoyCount = items.stream().filter(a -> "HOY".equals(a.getTipo())).count();
        long mananaCount = items.stream().filter(a -> "MANANA".equals(a.getTipo())).count();

        Map<String, Object> resp = new HashMap<>();
        resp.put("total", items.size());
        resp.put("noAsistioCount", noAsistioCount);
        resp.put("hoyCount", hoyCount);
        resp.put("mananaCount", mananaCount);
        resp.put("items", items);
        resp.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/marcar-leida")
    public ResponseEntity<Map<String, Object>> marcarLeida(@RequestBody(required = false) Map<String, String> body) {
        String notificacionId = body != null ? body.get("notificacionId") : null;
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("notificacionId", notificacionId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/pacientes-rapido")
    public ResponseEntity<List<Map<String, Object>>> buscarPacientesRapido(@RequestParam(value = "q", defaultValue = "") String q) {
        List<Paciente> encontrados = pacienteService.buscar(q, true);
        List<Map<String, Object>> resultado = new ArrayList<>();

        int limite = Math.min(encontrados.size(), 15);
        for (int i = 0; i < limite; i++) {
            Paciente p = encontrados.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("idPaciente", p.getIdPaciente());
            map.put("nombreCompleto", p.getNombreCompleto());
            map.put("nombres", p.getNombres());
            map.put("apellidos", p.getApellidos());
            map.put("dni", p.getDni());
            map.put("celular", p.getCelular());
            resultado.add(map);
        }

        return ResponseEntity.ok(resultado);
    }

}
