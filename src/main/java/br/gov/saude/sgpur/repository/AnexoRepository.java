package br.gov.saude.sgpur.repository;

import br.gov.saude.sgpur.domain.Anexo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    List<Anexo> findByProcessoIdOrderByDataUploadAsc(Long processoId);
}
