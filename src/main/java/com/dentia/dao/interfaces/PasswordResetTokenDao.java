package com.dentia.dao.interfaces;

import com.dentia.model.auth.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenDao {

    void guardarToken(PasswordResetToken token);

    void invalidarTokensUsuario(Integer idUsuario);

    Optional<PasswordResetToken> buscarPorCodigo(String codigo);

    boolean consumirToken(Integer idToken);
}
