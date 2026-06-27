package br.gov.saude.sgpur.config;

import br.gov.saude.sgpur.domain.MembroUrgenciaRenal;
import br.gov.saude.sgpur.repository.MembroUrgenciaRenalRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Carga inicial dos membros da Urgencia Renal (extraidos da planilha 2026).
 * So insere se a tabela estiver vazia.
 */
@Configuration
public class DataSeed {

    @Bean
    CommandLineRunner seedMembros(MembroUrgenciaRenalRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            List<MembroUrgenciaRenal> membros = List.of(
                new MembroUrgenciaRenal("HBBL", "Marcia Abichequer", null),
                new MembroUrgenciaRenal("HNSP", "Cristiane M da Silveira Souto", null),
                new MembroUrgenciaRenal("HSLPUC", "Ivan Antonello", null),
                new MembroUrgenciaRenal("ISCMPA", "Clotilde Garcia", null),
                new MembroUrgenciaRenal("HCPA", "Veronica Horbe", null),
                new MembroUrgenciaRenal("SGN", "Marcelo Generali da Costa", null),
                new MembroUrgenciaRenal("HCl", "Ana Lucia", null),
                new MembroUrgenciaRenal("CET", "Rogerio Caruso Bezerra", null)
            );
            repo.saveAll(membros);
        };
    }
}
