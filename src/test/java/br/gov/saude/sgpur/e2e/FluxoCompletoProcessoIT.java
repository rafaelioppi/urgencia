package br.gov.saude.sgpur.e2e;

import br.gov.saude.sgpur.domain.MembroUrgenciaRenal;
import br.gov.saude.sgpur.e2e.pages.NovoProcessoPage;
import br.gov.saude.sgpur.e2e.pages.ProcessoDetalhePage;
import br.gov.saude.sgpur.repository.MembroUrgenciaRenalRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.microsoft.playwright.options.FilePayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simula, atraves de um browser Chromium real (Playwright), o operador
 * humano percorrendo TODO o fluxo do processo de Urgencia Renal do inicio
 * ao fim: login, cadastro, recebimento, envio aos 3 avaliadores, registro
 * dos 3 pareceres, decisao por maioria (Indeferido) e finalizacao (oficio +
 * resposta ao solicitante).
 *
 * <p>E um "bot de navegacao": nenhuma chamada de servico ou endpoint e feita
 * diretamente - toda acao e um clique/preenchimento/upload real na tela,
 * exatamente como um operador da Secretaria faria. O objetivo e pegar
 * regressoes que testes de unidade/MockMvc nao veem: JavaScript quebrado,
 * campo com o "name" errado, wizard travando numa aba, etc.
 *
 * <p>So roda via "mvn verify -Pe2e" (lento, abre browser). Para depurar
 * visualmente: SAUR_E2E_HEADED=true mvn verify -Pe2e
 * -Dit.test=FluxoCompletoProcessoIT
 */
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:sgpur-e2e;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FluxoCompletoProcessoIT extends PlaywrightTestBase {

    @Autowired
    private MembroUrgenciaRenalRepository membroRepository;

    private static byte[] pdf(String texto) {
        Document doc = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph(texto));
        doc.close();
        return out.toByteArray();
    }

    private static FilePayload pdfPayload(String nome, String texto) {
        return new FilePayload(nome, "application/pdf", pdf(texto));
    }

    @Test
    void operadorConduzProcessoDoRecebimentoAteIndeferimento() {
        // O MembroDevSeed (perfil dev) ja populou 3 avaliadores no boot.
        List<MembroUrgenciaRenal> medicos = membroRepository.findByAtivoTrueOrderByInstituicaoAsc();
        assertThat(medicos).hasSize(3);
        List<Long> medicoIds = medicos.stream().map(MembroUrgenciaRenal::getId).toList();

        try {
            login("admin", "admin123");
            assertThat(page.url()).doesNotContain("/login");

            // ===== Cadastro do processo =====
            ProcessoDetalhePage detalhe = new NovoProcessoPage(page)
                .abrir()
                .preencher("01/2026", LocalDate.now(),
                    "Paciente E2E da Silva", "123456789-00001",
                    "Equipe Teste E2E", "solicitante.e2e@example.com")
                .selecionarMedicos(medicoIds)
                .cadastrar();

            // ===== Passo 1: Recebimento =====
            detalhe.passo1_registrarRecebimento(pdfPayload("solicitacao.pdf", "Solicitacao original do paciente"));
            assertThat(detalhe.passoConcluido(1)).isTrue();

            // ===== Passo 2: Envio aos avaliadores =====
            detalhe
                .passo2_anexarDocumentoClinico(pdfPayload("laudo.pdf", "Laudo clinico anonimizado"))
                .passo2_anexarComprovanteEnvio(pdfPayload("comprovante-envio.pdf", "Comprovante de envio por e-mail"))
                .passo2_registrarEnvio();
            assertThat(detalhe.passoConcluido(2)).isTrue();

            // ===== Passo 3: Respostas — maioria simples (2 de 3 desfavoraveis => Indeferido) =====
            detalhe.passo3_registrarParecer(medicos.get(0).getNome(), "NAO_FAVORAVEL",
                pdfPayload("resposta1.pdf", "Parecer desfavoravel do medico 1"));
            detalhe.passo3_registrarParecer(medicos.get(1).getNome(), "NAO_FAVORAVEL",
                pdfPayload("resposta2.pdf", "Parecer desfavoravel do medico 2"));
            assertThat(detalhe.passoConcluido(3)).isTrue();

            // ===== Passo 4: Decisao (Indeferido, com motivo) =====
            detalhe.passo4_decidir("INDEFERIDO", "Maioria dos avaliadores considerou o pedido desfavoravel.");
            assertThat(detalhe.passoConcluido(4)).isTrue();

            // ===== Passo 5: Finalizacao (oficio gerado automaticamente + resposta ao solicitante) =====
            detalhe
                .passo5_anexarComprovanteEnvioSolicitante(pdfPayload("comprovante-resposta.pdf", "Comprovante de envio ao solicitante"))
                .passo5_confirmarRespostaAoSolicitante();
            assertThat(detalhe.passoConcluido(5)).isTrue();

        } catch (AssertionError | RuntimeException e) {
            screenshot("fluxo-completo-falha");
            throw e;
        }
    }
}
