package br.com.techchallenge.feedback.relatorio.service;

import br.com.techchallenge.feedback.relatorio.repository.AvaliacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RelatorioServiceTest {

    private AvaliacaoRepository repository;
    private S3Client s3;
    private SnsClient sns;
    private RelatorioService service;

    @BeforeEach
    void configurar() {
        repository = mock(AvaliacaoRepository.class);
        s3 = mock(S3Client.class);
        sns = mock(SnsClient.class);
        service = new RelatorioService(repository, s3, sns);
    }

    @Test
    void deveGerarRelatorioCompletoDosUltimosSeteDias() throws IOException {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime dia1a = agora.minusDays(2).withHour(10);
        OffsetDateTime dia1b = agora.minusDays(2).withHour(12);
        OffsetDateTime dia2 = agora.minusDays(1).withHour(9);

        when(repository.listarTodas()).thenReturn(List.of(
                item("Falha", 2, "CRITICA", dia1a),
                item("Regular", 5, "MEDIA", dia1b),
                item("Ótima", 9, "BAIXA", dia2),
                item("Antiga", 1, "CRITICA", agora.minusDays(8)),
                itemComDataInvalida("Inválida", 3, "CRITICA")
        ));

        service.gerar();

        String conteudo = capturarConteudoS3();
        assertAll(
                () -> assertTrue(conteudo.contains("Total de avaliações: 3")),
                () -> assertTrue(conteudo.contains("Média das notas: 5")),
                () -> assertTrue(conteudo.contains("- Críticas: 1")),
                () -> assertTrue(conteudo.contains("- Médias: 1")),
                () -> assertTrue(conteudo.contains("- Baixas: 1")),
                () -> assertTrue(conteudo.contains(dia1a.toLocalDate() + ": 2")),
                () -> assertTrue(conteudo.contains(dia2.toLocalDate() + ": 1")),
                () -> assertTrue(conteudo.contains("Descrição: Falha")),
                () -> assertTrue(conteudo.contains("Descrição: Regular")),
                () -> assertTrue(conteudo.contains("Descrição: Ótima")),
                () -> assertFalse(conteudo.contains("Descrição: Antiga")),
                () -> assertFalse(conteudo.contains("Descrição: Inválida"))
        );

        ArgumentCaptor<PublishRequest> snsCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(snsCaptor.capture());
        assertEquals("Relatório semanal de avaliações", snsCaptor.getValue().subject());
        assertEquals(conteudo, snsCaptor.getValue().message());
    }

    @Test
    void deveGerarRelatorioVazioSemFalhar() throws IOException {
        when(repository.listarTodas()).thenReturn(List.of());

        service.gerar();

        String conteudo = capturarConteudoS3();
        assertTrue(conteudo.contains("Total de avaliações: 0"));
        assertTrue(conteudo.contains("Média das notas: 0"));
        assertTrue(conteudo.contains("Nenhuma avaliação no período"));
        assertTrue(conteudo.contains("Nenhuma avaliação encontrada no período."));
        verify(sns).publish(any(PublishRequest.class));
    }

    @Test
    void deveUsarPrefixoENomeSemanalNoS3() {
        when(repository.listarTodas()).thenReturn(List.of());

        service.gerar();

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest request = captor.getValue();
        assertTrue(request.key().matches("relatorios/relatorio-semanal-\\d{4}-\\d{2}-\\d{2}\\.txt"));
        assertEquals("text/plain; charset=utf-8", request.contentType());
    }

    private String capturarConteudoS3() throws IOException {
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(any(PutObjectRequest.class), bodyCaptor.capture());
        try (var stream = bodyCaptor.getValue().contentStreamProvider().newStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, AttributeValue> item(
            String descricao, int nota, String urgencia, OffsetDateTime data
    ) {
        return Map.of(
                "descricao", AttributeValue.builder().s(descricao).build(),
                "nota", AttributeValue.builder().n(String.valueOf(nota)).build(),
                "urgencia", AttributeValue.builder().s(urgencia).build(),
                "dataEnvio", AttributeValue.builder().s(data.toString()).build()
        );
    }

    private Map<String, AttributeValue> itemComDataInvalida(
            String descricao, int nota, String urgencia
    ) {
        return Map.of(
                "descricao", AttributeValue.builder().s(descricao).build(),
                "nota", AttributeValue.builder().n(String.valueOf(nota)).build(),
                "urgencia", AttributeValue.builder().s(urgencia).build(),
                "dataEnvio", AttributeValue.builder().s("data-invalida").build()
        );
    }
}
