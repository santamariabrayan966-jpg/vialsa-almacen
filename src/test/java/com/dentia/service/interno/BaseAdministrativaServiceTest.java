package com.dentia.service.interno;

import com.dentia.dao.Jdbc.JdbcRolDao;
import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.interno.Rol;
import com.dentia.model.interno.Usuario;
import com.dentia.model.paciente.Paciente;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BaseAdministrativaServiceTest {
 @Test void protegeAdminPorNombreAunqueSuIdNoSeaUno(){JdbcRolDao dao=mock(JdbcRolDao.class);Rol admin=new Rol(9,"ADMIN");when(dao.obtenerPorId(9)).thenReturn(admin);RolService service=new RolService(dao);assertThrows(RuntimeException.class,()->service.cambiarEstadoActivo(9,false));verify(dao,never()).cambiarEstadoActivo(anyInt(),anyBoolean());}
 @Test void noDesactivaRolConUsuariosAsociados(){JdbcRolDao dao=mock(JdbcRolDao.class);Rol rol=new Rol(4,"RECEPCION");when(dao.obtenerPorId(4)).thenReturn(rol);when(dao.contarUsuariosAsociados(4)).thenReturn(2);RolService service=new RolService(dao);assertThrows(RuntimeException.class,()->service.eliminarRol(4));verify(dao,never()).eliminarRol(4);}
 @Test void creaUsuarioConBCryptYConsultorioUnico(){UsuarioDao dao=mock(UsuarioDao.class);PasswordEncoder encoder=mock(PasswordEncoder.class);when(encoder.encode("Clave#2026")).thenReturn("$2a$hash");when(dao.obtenerPorNombre("recepcion")).thenReturn(Optional.empty());UsuarioService service=new UsuarioService(dao,encoder);Usuario u=new Usuario();u.setNombreUsuario("recepcion");u.setNombres("Ana");u.setApellidos("Rojas");u.setIdRol(2);u.setContrasena("Clave#2026");service.guardarUsuarioInterno(u);assertEquals("$2a$hash",u.getContrasena());assertEquals(1,u.getIdSedePrincipal());assertFalse(u.getPuedeCambiarSede());verify(dao).crear(u);}
 @Test void pacienteInternoRequiereDniPeroPermiteCelularOpcional(){IPacienteDao dao=mock(IPacienteDao.class);when(dao.buscar("73781820",null)).thenReturn(List.of());when(dao.crear(any())).thenReturn(12L);PacienteInternoService service=new PacienteInternoService(dao);Paciente p=new Paciente();p.setDni("73781820");p.setNombres("Luis");p.setApellidos("Diaz");p.setFechaNacimiento(LocalDate.of(1990,1,1));assertEquals(12L,service.guardar(p));assertNull(p.getCelular());verify(dao).crear(p);}
}
