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
 * <p>Roda so via "mvn verify -Pe2e" (maven-failsafe-plugin, classes *IT.java)
 * - nunca no "mvn test"/.\test.ps1 do dia a dia, que continua rapido e sem
 * dependencia de browser instalado. Ver docs/E2E-PLAYWRIGHT.md.
 *
 * <p>Modo visual: defina a env var SAUR_E2E_HEADED=true para ver o browser
 * abrindo de verdade (util ao depurar um teste localmente). Por padrao roda
 * headless (sem janela), como convem em CI.
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
        boolean headed = Boolean.parseBoolean(System.getenv().getOrDefault("SAUR_E2E_HEADED", "false"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(!headed)
            .setSlowMo(headed ? 250 : 0));
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
        page.locator("input[name=username]").fill(username);
        page.locator("input[name=password]").fill(senha);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Entrar")).click();
    }
}
