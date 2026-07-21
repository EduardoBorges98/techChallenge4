package br.com.techchallenge.feedback;

import br.com.techchallenge.feedback.dto.AvaliacaoRequest;
import br.com.techchallenge.feedback.dto.AvaliacaoResponse;
import br.com.techchallenge.feedback.service.AvaliacaoService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class FeedbackApplicationTest {

    @Test
    void funcaoDeveDelegarParaAvaliacaoService() {
        AvaliacaoService service = mock(AvaliacaoService.class);
        AvaliacaoRequest request = new AvaliacaoRequest("Teste", 5);
        AvaliacaoResponse esperada = new AvaliacaoResponse(
                "id", "Teste", 5, "MEDIA", OffsetDateTime.now()
        );
        when(service.receber(request)).thenReturn(esperada);

        Function<AvaliacaoRequest, AvaliacaoResponse> function =
                new FeedbackApplication().receberAvaliacao(service);

        assertSame(esperada, function.apply(request));
        verify(service).receber(request);
    }
}
