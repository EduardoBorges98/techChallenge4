package br.com.techchallenge.feedback.dto;

import java.time.OffsetDateTime;

public record AvaliacaoResponse(
        String id,
        String descricao,
        Integer nota,
        String urgencia,
        OffsetDateTime dataEnvio
) {
}