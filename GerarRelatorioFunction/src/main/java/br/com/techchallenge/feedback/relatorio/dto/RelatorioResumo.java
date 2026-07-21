package br.com.techchallenge.feedback.relatorio.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record RelatorioResumo(
        OffsetDateTime dataGeracao,
        int totalAvaliacoes,
        double mediaNotas,
        long criticas,
        long medias,
        long baixas,
        Map<LocalDate, Long> quantidadePorDia,
        List<AvaliacaoDetalhe> avaliacoes
) {
}