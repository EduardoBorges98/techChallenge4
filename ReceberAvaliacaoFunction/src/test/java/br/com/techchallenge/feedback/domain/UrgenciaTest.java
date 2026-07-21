package br.com.techchallenge.feedback.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class UrgenciaTest {

    @ParameterizedTest
    @CsvSource({
            "0, CRITICA", "1, CRITICA", "3, CRITICA",
            "4, MEDIA", "5, MEDIA", "6, MEDIA",
            "7, BAIXA", "9, BAIXA", "10, BAIXA"
    })
    void deveClassificarNota(Integer nota, Urgencia esperada) {
        assertEquals(esperada, Urgencia.calcular(nota));
    }

    @Test
    void deveRejeitarNotaNula() {
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> Urgencia.calcular(null)
        );
        assertEquals("A nota é obrigatória", erro.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"-1", "11"})
    void deveRejeitarNotaForaDaFaixa(Integer nota) {
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> Urgencia.calcular(nota)
        );
        assertEquals("A nota deve estar entre 0 e 10", erro.getMessage());
    }
}
