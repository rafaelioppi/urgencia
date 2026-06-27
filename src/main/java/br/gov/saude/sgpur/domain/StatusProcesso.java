package br.gov.saude.sgpur.domain;

/**
 * Situacao administrativa do Processo de Urgencia Renal.
 * O processo nasce EM_ANALISE e termina em uma das decisoes finais.
 */
public enum StatusProcesso {
    EM_ANALISE("Em analise"),
    DEFERIDO("Deferido"),
    INDEFERIDO("Indeferido"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusProcesso(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isFinalizado() {
        return this != EM_ANALISE;
    }
}
