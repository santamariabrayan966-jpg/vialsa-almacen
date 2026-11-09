package com.dentia.controller.interno;

import com.dentia.model.agenda.Profesional;
import com.dentia.security.CustomAuthorizationService;
import com.dentia.service.agenda.ProfesionalAgendaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/interno/reportes")
public class ReportesClinicosController {

    private final CustomAuthorizationService authorizationService;
    private final ProfesionalAgendaService profesionalService;
    private final JdbcTemplate jdbc;

    public ReportesClinicosController(CustomAuthorizationService authorizationService,
                                      ProfesionalAgendaService profesionalService,
                                      JdbcTemplate jdbc) {
        this.authorizationService = authorizationService;
        this.profesionalService = profesionalService;
        this.jdbc = jdbc;
    }

    private void autorizar(Authentication auth) {
        if (!authorizationService.tieneAcceso(auth, "reportes")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para ver reportes.");
        }
    }

    @GetMapping
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        @RequestParam(required = false) Long profesionalId,
                        Authentication auth, Model model) {
        autorizar(auth);

        LocalDate hoy = LocalDate.now();
        if (desde == null) {
            desde = hoy.withDayOfMonth(1);
        }
        if (hasta == null) {
            hasta = hoy.withDayOfMonth(hoy.lengthOfMonth());
        }

        List<Profesional> profesionales = profesionalService.listarActivos();
        Map<String, Object> stats = calcularEstadisticas(desde, hasta, profesionalId);

        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("profesionalId", profesionalId);
        model.addAttribute("profesionales", profesionales);
        model.addAttribute("stats", stats);

        return "interno/reportes/index";
    }

    @GetMapping("/datos")
    @ResponseBody
    public Map<String, Object> datosAjax(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                        @RequestParam(required = false) Long profesionalId,
                                        Authentication auth) {
        autorizar(auth);

        LocalDate hoy = LocalDate.now();
        if (desde == null) desde = hoy.withDayOfMonth(1);
        if (hasta == null) hasta = hoy.withDayOfMonth(hoy.lengthOfMonth());

        return calcularEstadisticas(desde, hasta, profesionalId);
    }

    @GetMapping("/exportar/csv")
    public ResponseEntity<String> exportarCsv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @RequestParam(required = false) Long profesionalId,
                                              Authentication auth) {
        autorizar(auth);

        LocalDate hoy = LocalDate.now();
        if (desde == null) desde = hoy.withDayOfMonth(1);
        if (hasta == null) hasta = hoy.withDayOfMonth(hoy.lengthOfMonth());

        Map<String, Object> stats = calcularEstadisticas(desde, hasta, profesionalId);

        StringBuilder csv = new StringBuilder();
        csv.append("REPORTE CLÍNICO Y ESTADÍSTICO - DENTIA\n");
        csv.append("Período;").append(desde).append(" al ").append(hasta).append("\n");
        csv.append("Generado por;").append(auth != null ? auth.getName() : "Usuario").append("\n\n");

        csv.append("INDICADORES CLAVE (KPIs)\n");
        csv.append("Métrica;Valor\n");
        csv.append("Pacientes atendidos;").append(stats.get("pacientesAtendidos")).append("\n");
        csv.append("Tratamientos realizados;").append(stats.get("tratamientosRealizados")).append("\n");
        csv.append("Diagnósticos registrados;").append(stats.get("diagnosticosRegistrados")).append("\n");
        csv.append("Ocupación de agenda;").append(stats.get("ocupacionAgenda")).append("%\n\n");

        csv.append("DISTRIBUCIÓN DE TRATAMIENTOS\n");
        csv.append("Tratamiento;Cantidad;Porcentaje\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tratamientos = (List<Map<String, Object>>) stats.get("tratamientosPorTipo");
        if (tratamientos != null) {
            for (Map<String, Object> t : tratamientos) {
                csv.append(t.get("nombre")).append(";")
                   .append(t.get("cantidad")).append(";")
                   .append(t.get("porcentaje")).append("%\n");
            }
        }
        csv.append("\n");

        csv.append("DIAGNÓSTICOS MÁS FRECUENTES\n");
        csv.append("Diagnóstico;Casos;Porcentaje\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diagnosticos = (List<Map<String, Object>>) stats.get("diagnosticosFrecuentes");
        if (diagnosticos != null) {
            for (Map<String, Object> d : diagnosticos) {
                csv.append(d.get("nombre")).append(";")
                   .append(d.get("cantidad")).append(";")
                   .append(d.get("porcentaje")).append("%\n");
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-clinico-" + desde + "-" + hasta + ".csv\"")
                .body(csv.toString());
    }

    private Map<String, Object> calcularEstadisticas(LocalDate desde, LocalDate hasta, Long profesionalId) {
        if (hasta.isBefore(desde) || desde.plusYears(2).isBefore(hasta))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El período debe ser válido y no superar dos años.");
        Map<String, Object> data = new LinkedHashMap<>();
        String filtro = profesionalId != null ? " AND profesional_id = " + profesionalId : "";
        String citasFiltro = profesionalId != null ? " AND id_profesional = " + profesionalId : "";
        String atenciones = " FROM clinica_atenciones WHERE fecha_atencion >= ? AND fecha_atencion < ?" + filtro;
        Object inicio = desde.atStartOfDay(), fin = hasta.plusDays(1).atStartOfDay();
        int pacientes = jdbc.queryForObject("SELECT COUNT(DISTINCT paciente_id)" + atenciones, Integer.class, inicio, fin);
        int tratamientos = jdbc.queryForObject("SELECT COUNT(*)" + atenciones, Integer.class, inicio, fin);
        // Diagnósticos sin profesional propio: filtrar por pacientes atendidos por ese profesional en el período.
        String diagFiltro = profesionalId == null ? "" : " AND paciente_id IN (SELECT paciente_id FROM clinica_atenciones WHERE profesional_id = " + profesionalId + " AND fecha_atencion >= '" + desde + "' AND fecha_atencion < '" + hasta.plusDays(1) + "')";
        String diagnosticos = " FROM clinica_paciente_diagnosticos WHERE fecha_diagnostico BETWEEN ? AND ?" + diagFiltro;
        int cantidadDiag = jdbc.queryForObject("SELECT COUNT(*)" + diagnosticos, Integer.class, desde, hasta);
        String citas = " FROM citas WHERE fecha_hora_inicio >= ? AND fecha_hora_inicio < ?" + citasFiltro;
        int totalCitas = jdbc.queryForObject("SELECT COUNT(*)" + citas, Integer.class, inicio, fin);
        int atendidas = jdbc.queryForObject("SELECT COUNT(*)" + citas + " AND estado='ATENDIDA'", Integer.class, inicio, fin);
        data.put("pacientesAtendidos", pacientes);
        data.put("tratamientosRealizados", tratamientos);
        data.put("diagnosticosRegistrados", cantidadDiag);
        data.put("ocupacionAgenda", totalCitas == 0 ? 0 : atendidas * 100 / totalCitas);
        for (String key : List.of("cambioPacientes", "cambioTratamientos", "cambioDiagnosticos", "cambioOcupacion")) data.put(key, "Sin comparación");
        List<Map<String, Object>> semanas = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(7)) {
            LocalDate ultimo = dia.plusDays(6).isAfter(hasta) ? hasta : dia.plusDays(6);
            int valor = jdbc.queryForObject("SELECT COUNT(DISTINCT paciente_id)" + atenciones, Integer.class, dia.atStartOfDay(), ultimo.plusDays(1).atStartOfDay());
            semanas.add(Map.of("label", "Sem " + (semanas.size()+1), "rango", dia + " / " + ultimo, "valor", valor, "pct", pacientes == 0 ? 0 : valor * 100 / pacientes));
        }
        data.put("pacientesPorSemana", semanas);
        List<Map<String, Object>> tipos = jdbc.queryForList("SELECT plan_procedimiento nombre, COUNT(*) cantidad" + atenciones + " GROUP BY plan_procedimiento ORDER BY cantidad DESC", inicio, fin);
        String[] colores = {"#0d9488", "#3b82f6", "#8b5cf6", "#f59e0b", "#ec4899"};
        double offset = 0;
        for (int i=0; i<tipos.size(); i++) {
            var t = tipos.get(i);
            double pct = tratamientos == 0 ? 0 : ((Number)t.get("cantidad")).doubleValue()*100/tratamientos;
            t.put("porcentaje", pct); t.put("offset", -offset); t.put("color", colores[i % colores.length]); offset += pct;
        }
        data.put("tratamientosPorTipo", tipos); data.put("totalTratamientosDonut", tratamientos);
        var ranking = jdbc.queryForList("SELECT descripcion nombre, COUNT(*) cantidad" + diagnosticos + " GROUP BY descripcion ORDER BY cantidad DESC", desde, hasta);
        for (var d : ranking) d.put("porcentaje", cantidadDiag == 0 ? 0 : ((Number)d.get("cantidad")).intValue()*100/cantidadDiag);
        data.put("diagnosticosFrecuentes", ranking);
        List<Map<String, Object>> meses = new ArrayList<>();
        for (int i=5; i>=0; i--) {
            LocalDate mes = hasta.withDayOfMonth(1).minusMonths(i);
            int pac = jdbc.queryForObject("SELECT COUNT(DISTINCT paciente_id)" + atenciones, Integer.class, mes.atStartOfDay(), mes.plusMonths(1).atStartOfDay());
            int trat = jdbc.queryForObject("SELECT COUNT(*)" + atenciones, Integer.class, mes.atStartOfDay(), mes.plusMonths(1).atStartOfDay());
            meses.add(Map.of("mes", mes.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-PE"))), "pacientes", pac, "tratamientos", trat));
        }
        data.put("evolucionMensual", meses);
        data.put("maximoMensual", meses.stream().mapToInt(m -> Math.max((int)m.get("pacientes"), (int)m.get("tratamientos"))).max().orElse(0) + 1);
        return data;
    }
}
