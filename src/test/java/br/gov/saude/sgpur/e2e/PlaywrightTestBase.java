package br.gov.saude.sgpur.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base para testes E2E de navegador (Playwright): sobe o SAUR real numa
 * porta aleatoria (H2 em memoria, perfil dev) e abre um browser Chromium
 * de verdade para simular um operador humano clicando na tela.
 *
 * <p>Roda so via ".\e2e.ps1" / "mvn verify -Pe2e" (maven-failsafe-plugin,
 * classes *IT.java) - nunca no "mvn test"/.\test.ps1 do dia a dia, que
 * continua rapido e sem dependencia de browser instalado.
 *
 * <p>Por padrao a janela do browser fica VISIVEL, com slowMo de 900ms entre
 * acoes (dá pra acompanhar o "bot" navegando). Para rodar sem janela (mais
 * rapido, ex. CI): ".\e2e.ps1 -Headless" (equivale a
 * "-Dsaur.e2e.headed=false").
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
public abstract class PlaywrightTestBase {

    protected static final Path SCREENSHOT_DIR = Paths.get("target", "e2e-screenshots");

    @LocalServerPort
    protected int port;

    private static Playwright playwright;
    private static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        // Aceita tanto a system property (-Dsaur.e2e.headed=true, a forma
        // confiavel via Maven/Failsafe, que forka um processo separado e nem
        // sempre herda env vars do shell pai) quanto a env var SAUR_E2E_HEADED
        // (util fora do Maven, ex. rodando a classe direto na IDE).
        boolean headed = Boolean.parseBoolean(System.getProperty("saur.e2e.headed",
            System.getenv().getOrDefault("SAUR_E2E_HEADED", "true")));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(!headed)
            .setSlowMo(headed ? 900 : 0));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void newContext() {
        context = browser.newContext(new Browser.NewContextOptions()
            .setBaseURL("http://localhost:" + port)
            .setViewportSize(1366, 900));
        // Aceita automaticamente os confirm() nativos usados no wizard
        // (ex.: "Confirmar o registro do seu voto?"), como um humano clicando OK.
        context.onDialog(dialog -> dialog.accept());
        page = context.newPage();
    }

    @AfterEach
    void closeContext(org.junit.jupiter.api.TestInfo testInfo) {
        if (context != null) {
            context.close();
        }
    }

    /** Tira um screenshot em target/e2e-screenshots, nomeado pelo teste. Chame no catch/falha. */
    protected void screenshot(String nome) {
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(SCREENSHOT_DIR.resolve(nome + ".png"))
            .setFullPage(true));
    }

    protected void login(String username, String senha) {
        page.navigate("/login");
        legenda("Fazendo login como " + username + "...");
        page.locator("input[name=username]").fill(username);
        page.locator("input[name=password]").fill(senha);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Entrar")).click();
    }

    /** Legenda fixa no topo da pagina anunciando a proxima acao do bot (ver {@link Legenda}). */
    protected void legenda(String texto) {
        Legenda.mostrar(page, texto);
    }
}
