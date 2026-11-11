package caixaverso.application.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SimulacaoRequestDTO(
        @Positive(message = "O clienteId deve ser positivo.")
        Long clienteId,

        @NotNull(message = "O valor investido é obrigatório.")
        @Positive(message = "O valor investido deve ser positivo.")
        @Digits(integer = 16, fraction = 2,
                message = "O valor investido deve ter no máximo 16 dígitos inteiros e 2 casas decimais.")
        BigDecimal valorInvestido,

        @NotNull(message = "O prazo em meses é obrigatório.")
        @Min(value = 1, message = "O prazo em meses deve ser pelo menos 1.")
        @Max(value = PRAZO_MAXIMO_MESES, message = "O prazo em meses não pode exceder {value}.")
        Integer prazoMeses,

        @NotBlank(message = "O produto é obrigatório.")
        @Size(max = 255, message = "O produto deve ter no máximo 255 caracteres.")
        String produto) {

    public static final int PRAZO_MAXIMO_MESES = 600;
}
