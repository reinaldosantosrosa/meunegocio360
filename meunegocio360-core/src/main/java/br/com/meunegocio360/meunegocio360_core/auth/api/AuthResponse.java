package br.com.meunegocio360.meunegocio360_core.auth.api;

public record AuthResponse(
    String tokenType, String accessToken, long expiresIn, UsuarioResponse usuario) {}
