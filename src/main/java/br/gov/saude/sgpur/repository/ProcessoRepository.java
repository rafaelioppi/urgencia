package br.gov.saude.sgpur.repository;

import br.gov.saude.sgpur.domain.Processo;
import br.gov.saude.sgpur.domain.StatusProcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    List<Processo> findAllByOrderByAnoDescSequencialDesc();

    List<Processo> findByStatusOrderByAnoDescSequencialDesc(StatusProcesso status);

    Optional<Processo> findByNumero(String numero);

    /** Maior sequencial ja usado em um ano (para gerar o proximo numero). */
    @Query("select max(p.sequencial) from Processo p where p.ano = :ano")
    Integer findMaxSequencialByAno(@Param("ano") int ano);

    long countByStatus(StatusProcesso status);
}
