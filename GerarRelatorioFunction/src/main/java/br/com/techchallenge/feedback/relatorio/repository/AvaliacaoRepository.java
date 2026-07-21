package br.com.techchallenge.feedback.relatorio.repository;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.List;
import java.util.Map;

@Repository
public class AvaliacaoRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public AvaliacaoRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = System.getenv("AVALIACOES_TABLE_NAME");
    }

    public List<Map<String, AttributeValue>> listarTodas() {
        return dynamoDbClient.scan(
                ScanRequest.builder()
                        .tableName(tableName)
                        .build()
        ).items();
    }
}