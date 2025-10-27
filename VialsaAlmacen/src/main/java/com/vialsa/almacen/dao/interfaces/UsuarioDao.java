package com.vialsa.almacen.dao.interfaces;
import com.vialsa.almacen.model.Usuario;
import java.util.Optional;
public interface UsuarioDao {
  Optional<Usuario> findByNombreUsuario(String username);
  void ensureAdminUserExists(String username, String rawPassword);
}
