package br.com.techchallenge.feedback.domain;

public enum Urgencia {

    CRITICA,
    MEDIA,
    BAIXA;

    public static Urgencia calcular(Integer nota) {
        if (nota == null) {
            throw new IllegalArgumentException("A nota é obrigatória");
        }

        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("A nota deve estar entre 0 e 10");
        }

        if (nota <= 3) {
            return CRITICA;
        }

        if (nota <= 6) {
            return MEDIA;
        }

        return BAIXA;
    }
}