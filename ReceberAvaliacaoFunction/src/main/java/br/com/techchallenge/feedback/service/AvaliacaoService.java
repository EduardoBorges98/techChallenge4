package br.com.techchallenge.feedback.service;

import br.com.techchallenge.feedback.domain.Urgencia;
import br.com.techchallenge.feedback.dto.AvaliacaoRequest;
import br.com.techchallenge.feedback.dto.AvaliacaoResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class AvaliacaoService {

    public AvaliacaoResponse receber(AvaliacaoRequest request) {
        validar(request);

        Urgencia urgencia = Urgencia.calcular(request.nota());

        return new AvaliacaoResponse(
                UUID.randomUUID().toString(),
                request.descricao(),
                request.nota(),
                urgencia.name(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private void validar(AvaliacaoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A avaliação é obrigatória");
        }

        if (request.descricao() == null || request.descricao().isBlank()) {
            throw new IllegalArgumentException("A descrição é obrigatória");
        }

        if (request.nota() == null) {
            throw new IllegalArgumentException("A nota é obrigatória");
        }

        if (request.nota() < 0 || request.nota() > 10) {
            throw new IllegalArgumentException("A nota deve estar entre 0 e 10");
        }
    }
}