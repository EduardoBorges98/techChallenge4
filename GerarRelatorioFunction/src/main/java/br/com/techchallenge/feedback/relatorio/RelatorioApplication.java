package br.com.techchallenge.feedback.relatorio;

import br.com.techchallenge.feedback.relatorio.service.RelatorioService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;

@SpringBootApplication
public class RelatorioApplication {

    public static void main(String[] args) {
        SpringApplication.run(RelatorioApplication.class, args);
    }

    @Bean
    public Consumer<Object> gerarRelatorio(RelatorioService relatorioService) {
        return evento -> relatorioService.gerar();
    }
}