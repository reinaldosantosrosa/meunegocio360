package br.com.meunegocio360.meunegocio360_core.auth.application;

import java.util.UUID;

public record UsuarioAutenticado(UUID id, String nome, String email, String perfil) {}
