package br.com.techchallenge.feedback.repository;

import br.com.techchallenge.feedback.dto.AvaliacaoResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AvaliacaoRepositoryTest {

    @Test
    void deveConverterAvaliacaoParaItemDoDynamoDb() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        AvaliacaoRepository repository = new AvaliacaoRepository(client);
        AvaliacaoResponse avaliacao = new AvaliacaoResponse(
                "abc", "Boa aula", 9, "BAIXA",
                OffsetDateTime.parse("2026-07-21T01:00:00Z")
        );

        repository.salvar(avaliacao);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(client).putItem(captor.capture());
        var item = captor.getValue().item();

        assertEquals("abc", item.get("id").s());
        assertEquals("Boa aula", item.get("descricao").s());
        assertEquals("9", item.get("nota").n());
        assertEquals("BAIXA", item.get("urgencia").s());
        assertEquals("2026-07-21T01:00Z", item.get("dataEnvio").s());
    }
}
