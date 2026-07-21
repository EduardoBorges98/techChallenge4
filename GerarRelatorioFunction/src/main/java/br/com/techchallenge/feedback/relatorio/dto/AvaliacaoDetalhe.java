package br.com.techchallenge.feedback.relatorio.dto;

import java.time.OffsetDateTime;

public record AvaliacaoDetalhe(
        String descricao,
        int nota,
        String urgencia,
        OffsetDateTime dataEnvio
) {
}