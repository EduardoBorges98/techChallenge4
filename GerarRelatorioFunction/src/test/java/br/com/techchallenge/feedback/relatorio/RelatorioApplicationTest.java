package br.com.techchallenge.feedback.relatorio;

import br.com.techchallenge.feedback.relatorio.service.RelatorioService;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

class RelatorioApplicationTest {

    @Test
    void consumerDeveDelegarParaRelatorioService() {
        RelatorioService service = mock(RelatorioService.class);
        Consumer<Object> consumer = new RelatorioApplication().gerarRelatorio(service);

        consumer.accept(new Object());

        verify(service).gerar();
    }
}
