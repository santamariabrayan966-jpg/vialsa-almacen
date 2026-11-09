package com.dentia.service.interno;

import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.model.paciente.Paciente;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PacienteInternoService {
 private final IPacienteDao dao; public PacienteInternoService(IPacienteDao dao){this.dao=dao;}
 public List<Paciente> buscar(String q,Boolean activo){return dao.buscar(q,activo);}
 public Paciente obtener(long id){return dao.buscarPorId(id).orElseThrow(()->new IllegalArgumentException("El paciente no existe."));}
 public long guardar(Paciente p){validar(p);normalizar(p);if(p.getIdPaciente()==null)return dao.crear(p);Paciente actual=obtener(p.getIdPaciente());p.setActivo(actual.isActivo());dao.actualizar(p);return p.getIdPaciente();}
 public void cambiarEstado(long id,boolean activo){obtener(id);dao.cambiarEstado(id,activo);}
 private void validar(Paciente p){if(p==null||vacio(p.getDni())||vacio(p.getNombres())||vacio(p.getApellidos())||p.getFechaNacimiento()==null)throw new IllegalArgumentException("DNI, nombres, apellidos y fecha de nacimiento son obligatorios.");if(p.getFechaNacimiento().isAfter(java.time.LocalDate.now()))throw new IllegalArgumentException("La fecha de nacimiento no puede estar en el futuro.");if(!p.getDni().matches("\\d{8}"))throw new IllegalArgumentException("El DNI debe tener 8 digitos.");if(dao.buscar(p.getDni(),null).stream().anyMatch(x->p.getDni().equals(x.getDni())&&!java.util.Objects.equals(p.getIdPaciente(),x.getIdPaciente())))throw new IllegalArgumentException("El DNI ya esta registrado.");if(!vacio(p.getCorreo())&&!p.getCorreo().matches("^[\\w.+'-]+@[\\w.-]+\\.[A-Za-z]{2,}$"))throw new IllegalArgumentException("Correo invalido.");}
 private void normalizar(Paciente p){p.setDni(p.getDni().trim());p.setCorreo(vacio(p.getCorreo())?null:p.getCorreo().trim());p.setDireccion(vacio(p.getDireccion())?null:p.getDireccion().trim());p.setNombres(p.getNombres().trim());p.setApellidos(p.getApellidos().trim());p.setCelular(vacio(p.getCelular())?null:p.getCelular().trim());}
 private boolean vacio(String v){return v==null||v.isBlank();}
}
