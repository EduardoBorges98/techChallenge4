package br.com.techchallenge.feedback.relatorio.repository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AvaliacaoRepositoryTest {

    @Test
    void deveRetornarItensDoScan() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        Map<String, AttributeValue> item = Map.of("id", AttributeValue.builder().s("1").build());
        when(client.scan(any(ScanRequest.class))).thenReturn(
                ScanResponse.builder().items(List.of(item)).build()
        );

        AvaliacaoRepository repository = new AvaliacaoRepository(client);
        assertEquals(List.of(item), repository.listarTodas());

        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(client).scan(captor.capture());
    }
}
