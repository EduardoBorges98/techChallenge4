package br.com.techchallenge.feedback;

import br.com.techchallenge.feedback.dto.AvaliacaoRequest;
import br.com.techchallenge.feedback.dto.AvaliacaoResponse;
import br.com.techchallenge.feedback.service.AvaliacaoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class FeedbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedbackApplication.class, args);
    }

    @Bean
    public Function<AvaliacaoRequest, AvaliacaoResponse> receberAvaliacao(
            AvaliacaoService avaliacaoService
    ) {
        return avaliacaoService::receber;
    }
}