package br.com.techchallenge.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AvaliacaoRequest(

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @Min(value = 0, message = "A nota mínima é 0")
        @Max(value = 10, message = "A nota máxima é 10")
        Integer nota

) {
}