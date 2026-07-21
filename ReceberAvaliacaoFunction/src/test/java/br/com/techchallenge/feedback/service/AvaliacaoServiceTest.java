package br.com.techchallenge.feedback.service;

import br.com.techchallenge.feedback.dto.AvaliacaoRequest;
import br.com.techchallenge.feedback.dto.AvaliacaoResponse;
import br.com.techchallenge.feedback.repository.AvaliacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvaliacaoServiceTest {

    private AvaliacaoRepository repository;
    private NotificacaoService notificacaoService;
    private AvaliacaoService service;

    @BeforeEach
    void configurar() {
        repository = mock(AvaliacaoRepository.class);
        notificacaoService = mock(NotificacaoService.class);
        service = new AvaliacaoService(repository, notificacaoService);
    }

    @ParameterizedTest
    @CsvSource({"0,CRITICA", "3,CRITICA", "4,MEDIA", "6,MEDIA", "7,BAIXA", "10,BAIXA"})
    void deveReceberPersistirEClassificar(Integer nota, String urgenciaEsperada) {
        AvaliacaoResponse resposta = service.receber(new AvaliacaoRequest("Aula avaliada", nota));

        assertNotNull(resposta.id());
        assertFalse(resposta.id().isBlank());
        assertEquals("Aula avaliada", resposta.descricao());
        assertEquals(nota, resposta.nota());
        assertEquals(urgenciaEsperada, resposta.urgencia());
        assertNotNull(resposta.dataEnvio());
        assertEquals(0, resposta.dataEnvio().getOffset().getTotalSeconds());

        verify(repository).salvar(resposta);
    }

    @Test
    void deveNotificarSomenteAvaliacaoCritica() {
        AvaliacaoResponse critica = service.receber(new AvaliacaoRequest("Falha crítica", 2));
        service.receber(new AvaliacaoRequest("Regular", 5));
        service.receber(new AvaliacaoRequest("Ótima", 9));

        verify(notificacaoService).enviarAlertaCritico(critica);
        verifyNoMoreInteractions(notificacaoService);
    }

    @Test
    void devePersistirExatamenteARespostaRetornada() {
        AvaliacaoResponse resposta = service.receber(new AvaliacaoRequest("Conteúdo", 8));
        ArgumentCaptor<AvaliacaoResponse> captor = ArgumentCaptor.forClass(AvaliacaoResponse.class);

        verify(repository).salvar(captor.capture());
        assertSame(resposta, captor.getValue());
    }

    @Test
    void deveRejeitarRequisicaoNula() {
        validarErro(null, "A avaliação é obrigatória");
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL", "''", "'   '"}, nullValues = "NULL")
    void deveRejeitarDescricaoInvalida(String descricao) {
        validarErro(new AvaliacaoRequest(descricao, 5), "A descrição é obrigatória");
    }

    @Test
    void deveRejeitarNotaNula() {
        validarErro(new AvaliacaoRequest("Descrição", null), "A nota é obrigatória");
    }

    @ParameterizedTest
    @CsvSource({"-1", "11"})
    void deveRejeitarNotaForaDaFaixa(Integer nota) {
        validarErro(new AvaliacaoRequest("Descrição", nota), "A nota deve estar entre 0 e 10");
    }

    private void validarErro(AvaliacaoRequest request, String mensagem) {
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.receber(request)
        );
        assertEquals(mensagem, erro.getMessage());
        verifyNoInteractions(repository, notificacaoService);
    }
}
