package caixaverso.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO (
    @NotBlank(message = "O usuário é obrigatório.")
    String usuario,
    @NotBlank(message = "A senha é obrigatória.")
    String senha) {}
