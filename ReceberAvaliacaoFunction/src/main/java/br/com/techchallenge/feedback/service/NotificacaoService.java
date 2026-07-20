package br.com.techchallenge.feedback.service;

import br.com.techchallenge.feedback.dto.AvaliacaoResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class NotificacaoService {

    private final SnsClient snsClient;
    private final String topicArn;

    public NotificacaoService(SnsClient snsClient) {
        this.snsClient = snsClient;
        this.topicArn = System.getenv("SNS_TOPIC_ARN");
    }

    public void enviarAlertaCritico(AvaliacaoResponse avaliacao) {
        String mensagem = """
                Foi recebida uma avaliação crítica.

                Descrição: %s
                Urgência: %s
                Nota: %d
                Data de envio: %s
                Identificador: %s
                """.formatted(
                avaliacao.descricao(),
                avaliacao.urgencia(),
                avaliacao.nota(),
                avaliacao.dataEnvio(),
                avaliacao.id()
        );

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .subject("Alerta de avaliação crítica")
                .message(mensagem)
                .build();

        snsClient.publish(request);
    }
}