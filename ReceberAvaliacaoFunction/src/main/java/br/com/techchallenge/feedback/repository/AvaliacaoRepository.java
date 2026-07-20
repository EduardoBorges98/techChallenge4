package br.com.techchallenge.feedback.repository;

import br.com.techchallenge.feedback.dto.AvaliacaoResponse;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

@Repository
public class AvaliacaoRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public AvaliacaoRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = System.getenv("AVALIACOES_TABLE_NAME");
    }

    public void salvar(AvaliacaoResponse avaliacao) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("id", AttributeValue.builder()
                .s(avaliacao.id())
                .build());

        item.put("descricao", AttributeValue.builder()
                .s(avaliacao.descricao())
                .build());

        item.put("nota", AttributeValue.builder()
                .n(String.valueOf(avaliacao.nota()))
                .build());

        item.put("urgencia", AttributeValue.builder()
                .s(avaliacao.urgencia())
                .build());

        item.put("dataEnvio", AttributeValue.builder()
                .s(avaliacao.dataEnvio().toString())
                .build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }
}