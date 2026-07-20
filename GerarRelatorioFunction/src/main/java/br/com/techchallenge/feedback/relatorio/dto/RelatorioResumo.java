package br.com.techchallenge.feedback.relatorio.dto;

import java.time.OffsetDateTime;

public record RelatorioResumo(
        OffsetDateTime dataGeracao,
        int totalAvaliacoes,
        double mediaNotas,
        long criticas,
        long medias,
        long baixas
) {
}