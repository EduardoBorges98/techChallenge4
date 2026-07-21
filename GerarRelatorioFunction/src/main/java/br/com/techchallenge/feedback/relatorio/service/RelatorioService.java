package br.com.techchallenge.feedback.relatorio.service;

import br.com.techchallenge.feedback.relatorio.dto.AvaliacaoDetalhe;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
        List<Map<String, AttributeValue>> itens =
                avaliacaoRepository.listarTodas();

        RelatorioResumo resumo = calcularResumo(itens);
        String relatorio = montarRelatorio(resumo);
        String nomeArquivo = montarNomeArquivo(resumo.dataGeracao());

        salvarNoS3(nomeArquivo, relatorio);
        enviarPorEmail(relatorio);
    }

    private RelatorioResumo calcularResumo(
            List<Map<String, AttributeValue>> itens
    ) {
        OffsetDateTime dataGeracao = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime inicioPeriodo = dataGeracao.minusDays(7);

        List<AvaliacaoDetalhe> avaliacoes = itens.stream()
                .map(this::converterAvaliacao)
                .filter(avaliacao -> avaliacao.dataEnvio() != null)
                .filter(avaliacao ->
                        !avaliacao.dataEnvio().isBefore(inicioPeriodo)
                                && !avaliacao.dataEnvio().isAfter(dataGeracao)
                )
                .sorted(Comparator.comparing(AvaliacaoDetalhe::dataEnvio))
                .toList();

        int total = avaliacoes.size();

        double media = avaliacoes.stream()
                .mapToInt(AvaliacaoDetalhe::nota)
                .average()
                .orElse(0.0);

        long criticas = contarPorUrgencia(avaliacoes, "CRITICA");
        long medias = contarPorUrgencia(avaliacoes, "MEDIA");
        long baixas = contarPorUrgencia(avaliacoes, "BAIXA");

        Map<LocalDate, Long> quantidadePorDia = avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        avaliacao -> avaliacao.dataEnvio().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        return new RelatorioResumo(
                dataGeracao,
                total,
                media,
                criticas,
                medias,
                baixas,
                quantidadePorDia,
                avaliacoes
        );
    }

    private AvaliacaoDetalhe converterAvaliacao(
            Map<String, AttributeValue> item
    ) {
        String descricao = lerTexto(item, "descricao");
        String urgencia = lerTexto(item, "urgencia");
        int nota = lerNumero(item, "nota");
        OffsetDateTime dataEnvio = lerData(item, "dataEnvio");

        return new AvaliacaoDetalhe(
                descricao,
                nota,
                urgencia,
                dataEnvio
        );
    }

    private String lerTexto(
            Map<String, AttributeValue> item,
            String atributo
    ) {
        AttributeValue valor = item.get(atributo);

        if (valor == null || valor.s() == null) {
            return "";
        }

        return valor.s();
    }

    private int lerNumero(
            Map<String, AttributeValue> item,
            String atributo
    ) {
        AttributeValue valor = item.get(atributo);

        if (valor == null || valor.n() == null) {
            return 0;
        }

        return Integer.parseInt(valor.n());
    }

    private OffsetDateTime lerData(
            Map<String, AttributeValue> item,
            String atributo
    ) {
        AttributeValue valor = item.get(atributo);

        if (valor == null || valor.s() == null || valor.s().isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(valor.s());
        } catch (Exception exception) {
            return null;
        }
    }

    private long contarPorUrgencia(
            List<AvaliacaoDetalhe> avaliacoes,
            String urgencia
    ) {
        return avaliacoes.stream()
                .filter(avaliacao ->
                        urgencia.equals(avaliacao.urgencia())
                )
                .count();
    }

    private String montarRelatorio(RelatorioResumo resumo) {
        StringBuilder relatorio = new StringBuilder();

        relatorio.append("RELATÓRIO SEMANAL DE AVALIAÇÕES\n\n");

        relatorio.append("Período: ")
                .append(resumo.dataGeracao().minusDays(7))
                .append(" até ")
                .append(resumo.dataGeracao())
                .append("\n");

        relatorio.append("Data de geração: ")
                .append(resumo.dataGeracao())
                .append("\n");

        relatorio.append("Total de avaliações: ")
                .append(resumo.totalAvaliacoes())
                .append("\n");

        relatorio.append("Média das notas: ")
                .append(String.format("%.2f", resumo.mediaNotas()))
                .append("\n\n");

        relatorio.append("QUANTIDADE POR URGÊNCIA\n");
        relatorio.append("- Críticas: ")
                .append(resumo.criticas())
                .append("\n");

        relatorio.append("- Médias: ")
                .append(resumo.medias())
                .append("\n");

        relatorio.append("- Baixas: ")
                .append(resumo.baixas())
                .append("\n\n");

        relatorio.append("QUANTIDADE DE AVALIAÇÕES POR DIA\n");

        if (resumo.quantidadePorDia().isEmpty()) {
            relatorio.append("- Nenhuma avaliação no período\n");
        } else {
            resumo.quantidadePorDia().forEach((dia, quantidade) ->
                    relatorio.append("- ")
                            .append(dia)
                            .append(": ")
                            .append(quantidade)
                            .append("\n")
            );
        }

        relatorio.append("\nDETALHES DAS AVALIAÇÕES\n");

        if (resumo.avaliacoes().isEmpty()) {
            relatorio.append("Nenhuma avaliação encontrada no período.\n");
        } else {
            for (AvaliacaoDetalhe avaliacao : resumo.avaliacoes()) {
                relatorio.append("\n------------------------------\n");

                relatorio.append("Descrição: ")
                        .append(avaliacao.descricao())
                        .append("\n");

                relatorio.append("Nota: ")
                        .append(avaliacao.nota())
                        .append("\n");

                relatorio.append("Urgência: ")
                        .append(avaliacao.urgencia())
                        .append("\n");

                relatorio.append("Data de envio: ")
                        .append(avaliacao.dataEnvio())
                        .append("\n");
            }
        }

        return relatorio.toString();
    }

    private String montarNomeArquivo(OffsetDateTime dataGeracao) {
        return "relatorios/relatorio-semanal-"
                + dataGeracao.toLocalDate()
                + ".txt";
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