package br.com.techchallenge.feedback.service;

import br.com.techchallenge.feedback.dto.AvaliacaoResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacaoServiceTest {

    @Test
    void devePublicarAlertaComTodosOsDados() {
        SnsClient sns = mock(SnsClient.class);
        NotificacaoService service = new NotificacaoService(sns);
        AvaliacaoResponse avaliacao = new AvaliacaoResponse(
                "id-123", "Sistema indisponível", 1, "CRITICA",
                OffsetDateTime.parse("2026-07-21T01:00:00Z")
        );

        service.enviarAlertaCritico(avaliacao);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(captor.capture());
        PublishRequest request = captor.getValue();

        assertEquals("Alerta de avaliação crítica", request.subject());
        assertAll(
                () -> assertTrue(request.message().contains("Sistema indisponível")),
                () -> assertTrue(request.message().contains("CRITICA")),
                () -> assertTrue(request.message().contains("Nota: 1")),
                () -> assertTrue(request.message().contains("2026-07-21T01:00Z")),
                () -> assertTrue(request.message().contains("id-123"))
        );
    }
}
