package br.com.techchallenge.feedback.relatorio.service;

import br.com.techchallenge.feedback.relatorio.dto.RelatorioResumo;
import br.com.techchallenge.feedback.relatorio.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final S3Client s3Client;
    private final SnsClient snsClient;

    private final String bucketName;
    private final String topicArn;

    public RelatorioService(
            AvaliacaoRepository avaliacaoRepository,
            S3Client s3Client,
            SnsClient snsClient
    ) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.s3Client = s3Client;
        this.snsClient = snsClient;

        this.bucketName = System.getenv("RELATORIOS_BUCKET_NAME");
        this.topicArn = System.getenv("SNS_TOPIC_ARN");
    }

    public void gerar() {
        List<Map<String, AttributeValue>> avaliacoes =
                avaliacaoRepository.listarTodas();

        RelatorioResumo resumo = calcularResumo(avaliacoes);
        String relatorio = montarRelatorio(resumo);
        String nomeArquivo = montarNomeArquivo(resumo.dataGeracao());

        salvarNoS3(nomeArquivo, relatorio);
        enviarPorEmail(relatorio);
    }

    private RelatorioResumo calcularResumo(
            List<Map<String, AttributeValue>> avaliacoes
    ) {
        int total = avaliacoes.size();

        double media = avaliacoes.stream()
                .map(item -> item.get("nota"))
                .filter(valor -> valor != null && valor.n() != null)
                .mapToInt(valor -> Integer.parseInt(valor.n()))
                .average()
                .orElse(0.0);

        long criticas = contarPorUrgencia(avaliacoes, "CRITICA");
        long medias = contarPorUrgencia(avaliacoes, "MEDIA");
        long baixas = contarPorUrgencia(avaliacoes, "BAIXA");

        return new RelatorioResumo(
                OffsetDateTime.now(ZoneOffset.UTC),
                total,
                media,
                criticas,
                medias,
                baixas
        );
    }

    private long contarPorUrgencia(
            List<Map<String, AttributeValue>> avaliacoes,
            String urgencia
    ) {
        return avaliacoes.stream()
                .map(item -> item.get("urgencia"))
                .filter(valor -> valor != null && valor.s() != null)
                .filter(valor -> urgencia.equals(valor.s()))
                .count();
    }

    private String montarRelatorio(RelatorioResumo resumo) {
        return """
                RELATÓRIO DE AVALIAÇÕES

                Data de geração: %s
                Total de avaliações: %d
                Média das notas: %.2f

                Distribuição por urgência:
                - Críticas: %d
                - Médias: %d
                - Baixas: %d
                """.formatted(
                resumo.dataGeracao(),
                resumo.totalAvaliacoes(),
                resumo.mediaNotas(),
                resumo.criticas(),
                resumo.medias(),
                resumo.baixas()
        );
    }

    private String montarNomeArquivo(OffsetDateTime dataGeracao) {
        return "relatorios/relatorio-" +
                dataGeracao.toLocalDate() +
                ".txt";
    }

    private void salvarNoS3(String nomeArquivo, String conteudo) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(nomeArquivo)
                .contentType("text/plain; charset=utf-8")
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromBytes(
                        conteudo.getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    private void enviarPorEmail(String relatorio) {
        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .subject("Relatório semanal de avaliações")
                .message(relatorio)
                .build();

        snsClient.publish(request);
    }
}