package br.gov.saude.sgpur.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Parecer de um membro da Urgencia Renal sobre um processo.
 *
 * Regra de negocio: todos os membros ativos avaliam o processo, EXCETO
 * quando o membro for o proprio solicitante daquele processo (conflito de
 * interesse) - nesse caso "impedido = true" e nao ha resultado.
 */
@Entity
@Table(
    name = "parecer",
    uniqueConstraints = @UniqueConstraint(columnNames = {"processo_id", "membro_id"})
)
public class Parecer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "membro_id", nullable = false)
    private MembroUrgenciaRenal membro;

    /** Resultado do parecer; nulo enquanto o membro nao respondeu. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResultadoParecer resultado;

    /** Membro impedido por ser o solicitante do processo (conflito). */
    @Column(nullable = false)
    private boolean impedido = false;

    @Column(name = "data_envio")
    private LocalDate dataEnvio;

    @Column(name = "data_resposta")
    private LocalDate dataResposta;

    public Parecer() {
    }

    public Parecer(MembroUrgenciaRenal membro) {
        this.membro = membro;
    }

    @Transient
    public boolean isRespondido() {
        return resultado != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Processo getProcesso() {
        return processo;
    }

    public void setProcesso(Processo processo) {
        this.processo = processo;
    }

    public MembroUrgenciaRenal getMembro() {
        return membro;
    }

    public void setMembro(MembroUrgenciaRenal membro) {
        this.membro = membro;
    }

    public ResultadoParecer getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoParecer resultado) {
        this.resultado = resultado;
    }

    public boolean isImpedido() {
        return impedido;
    }

    public void setImpedido(boolean impedido) {
        this.impedido = impedido;
    }

    public LocalDate getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDate dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public LocalDate getDataResposta() {
        return dataResposta;
    }

    public void setDataResposta(LocalDate dataResposta) {
        this.dataResposta = dataResposta;
    }
}
