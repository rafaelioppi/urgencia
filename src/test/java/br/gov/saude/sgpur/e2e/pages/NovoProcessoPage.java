package br.gov.saude.sgpur.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.time.LocalDate;
import java.util.List;

/** Page Object do formulario "Novo processo" (/processos/novo). */
public class NovoProcessoPage {

    private final Page page;

    public NovoProcessoPage(Page page) {
        this.page = page;
    }

    public NovoProcessoPage abrir() {
        page.navigate("/processos/novo");
        return this;
    }

    public NovoProcessoPage preencher(String numero, LocalDate dataSituacaoEspecial,
                                       String pacienteNome, String pacienteRgct,
                                       String solicitanteEquipe, String solicitanteEmail) {
        var numeroInput = page.locator("input[name=numero]");
        if (numeroInput.count() > 0 && numeroInput.isEnabled()) {
            numeroInput.fill(numero);
        }
        page.locator("input[name=dataSituacaoEspecial]").fill(dataSituacaoEspecial.toString());
        page.locator("input[name=pacienteNome]").fill(pacienteNome);
        page.locator("input[name=pacienteRgct]").fill(pacienteRgct);
        page.locator("input[name=solicitanteEquipe]").fill(solicitanteEquipe);
        page.locator("input[name=solicitanteEmail]").fill(solicitanteEmail);
        return this;
    }

    /** Marca exatamente os 3 medicos avaliadores pelos ids informados. */
    public NovoProcessoPage selecionarMedicos(List<Long> medicoIds) {
        for (Long id : medicoIds) {
            page.locator("#med" + id).check();
        }
        return this;
    }

    /** Submete o form e retorna a Page de Detalhe do processo criado (redirect). */
    public ProcessoDetalhePage cadastrar() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cadastrar")).click();
        page.waitForURL("**/processos/*");
        return new ProcessoDetalhePage(page);
    }
}
