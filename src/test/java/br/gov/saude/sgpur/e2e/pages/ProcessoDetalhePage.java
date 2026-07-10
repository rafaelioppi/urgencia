package br.gov.saude.sgpur.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.FilePayload;

/**
 * Page Object da tela de detalhe do processo (wizard de 5 passos). Cada
 * metodo corresponde a uma acao que um operador humano realizaria clicando
 * na tela - nao chama nenhum endpoint diretamente.
 */
public class ProcessoDetalhePage {

    private final Page page;

    public ProcessoDetalhePage(Page page) {
        this.page = page;
    }

    private void clicarPasso(String paneId) {
        page.locator(".wizard-step[href='#" + paneId + "']").click();
    }

    // ===== Passo 1: Recebimento =====

    public ProcessoDetalhePage passo1_registrarRecebimento(FilePayload solicitacaoOriginal) {
        clicarPasso("pane-recebimento");
        page.locator("#pane-recebimento input[name=arquivo]").setInputFiles(solicitacaoOriginal);
        page.locator("#pane-recebimento button:has-text('Registrar recebimento')").click();
        page.waitForLoadState();
        return this;
    }

    // ===== Passo 2: Envio =====

    public ProcessoDetalhePage passo2_anexarDocumentoClinico(FilePayload documentoClinico) {
        clicarPasso("pane-envio");
        page.locator("#pane-envio form[action*='documento-clinico'] input[name=arquivo]").setInputFiles(documentoClinico);
        page.locator("#pane-envio button:has-text('Anexar documento clinico')").click();
        page.waitForLoadState();
        return this;
    }

    public ProcessoDetalhePage passo2_anexarComprovanteEnvio(FilePayload comprovante) {
        clicarPasso("pane-envio");
        page.locator("#pane-envio form[action*='comprovante-envio-avaliadores'] input[name=arquivo]").setInputFiles(comprovante);
        page.locator("#pane-envio form[action*='comprovante-envio-avaliadores'] button").click();
        page.waitForLoadState();
        return this;
    }

    public ProcessoDetalhePage passo2_registrarEnvio() {
        clicarPasso("pane-envio");
        page.locator("#pane-envio button:has-text('Registrar envio')").click();
        page.waitForLoadState();
        return this;
    }

    // ===== Passo 3: Respostas (registra resultado + anexo de UM parecer, pela linha do medico) =====

    /** Registra o parecer do medico identificado pelo nome (texto da linha da tabela). */
    public ProcessoDetalhePage passo3_registrarParecer(String nomeMedico, String resultado, FilePayload respostaAvaliador) {
        clicarPasso("pane-respostas");
        Locator linha = page.locator("#respostas tr", new Page.LocatorOptions().setHasText(nomeMedico));
        linha.locator("select[name=resultado]").selectOption(resultado);
        linha.locator("input[name=arquivo]").setInputFiles(respostaAvaliador);
        linha.locator("button.btn-anexar-resposta").click();
        page.waitForLoadState();
        return this;
    }

    // ===== Passo 4: Decisao =====

    public ProcessoDetalhePage passo4_decidir(String decisao) {
        return passo4_decidir(decisao, null);
    }

    public ProcessoDetalhePage passo4_decidir(String decisao, String motivoIndeferimento) {
        clicarPasso("pane-decisao");
        page.locator("#decisaoSelect").selectOption(decisao);
        if (motivoIndeferimento != null) {
            page.locator("#motivoIndeferimentoInput").fill(motivoIndeferimento);
        }
        page.locator("#decisao button:has-text('Registrar decisao')").click();
        page.waitForLoadState();
        return this;
    }

    // ===== Passo 5: Finalizacao =====

    public ProcessoDetalhePage passo5_anexarComprovanteSnt(FilePayload comprovanteSnt) {
        clicarPasso("pane-finalizacao");
        page.locator("#finalizacao form[action*='comprovante-snt'] input[name=arquivo]").setInputFiles(comprovanteSnt);
        page.locator("#finalizacao form[action*='comprovante-snt'] button").click();
        page.waitForLoadState();
        return this;
    }

    public ProcessoDetalhePage passo5_anexarComprovanteEnvioSolicitante(FilePayload comprovante) {
        clicarPasso("pane-finalizacao");
        page.locator("#finalizacao form[action*='comprovante-envio-solicitante'] input[name=arquivo]").setInputFiles(comprovante);
        page.locator("#finalizacao form[action*='comprovante-envio-solicitante'] button").click();
        page.waitForLoadState();
        return this;
    }

    public ProcessoDetalhePage passo5_confirmarRespostaAoSolicitante() {
        clicarPasso("pane-finalizacao");
        page.locator("#emailEnvSolicitante").check();
        page.locator("#finalizacao form[action*='resposta-solicitante'] button").click();
        page.waitForLoadState();
        return this;
    }

    // ===== Asserts / leitura de estado =====

    /** true se o passo (1 a 5) esta marcado como concluido (classe .concluida) na barra do wizard. */
    public boolean passoConcluido(int numero) {
        return page.locator(".wizard-step:nth-child(" + numero + ")")
            .getAttribute("class").contains("concluida");
    }

    public String statusAtual() {
        return page.locator("[data-testid=status-processo]").count() > 0
            ? page.locator("[data-testid=status-processo]").innerText()
            : page.locator("h1").innerText();
    }

    public Page raw() {
        return page;
    }
}
