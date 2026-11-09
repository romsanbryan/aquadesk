package romsanbryan.aquadesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ThymeleafTemplateRenderingTest {

    private static final Pattern MESSAGE_KEY = Pattern.compile("#\\{([\\w.-]+)");
    private static final Pattern SCRIPT_IMPORT = Pattern.compile(
            "(?m)^import\\s+.+?\\s+from\\s+['\"]([^'\"]+)['\"]");
    private static final Pattern DOM_ID = Pattern.compile(
            "document\\.getElementById\\(['\"]([^'\"]+)['\"]\\)");
    private static final Pattern THYMELEAF_ATTRIBUTE = Pattern.compile("\\s+th:[\\w-]+=");
    private static final Set<String> OPTIONAL_DOM_IDS = Set.of("btn-borrar-evento-modal");
    private static final String[] TEMPLATE_FILES = {
            "templates/index.html",
            "templates/gestion.html",
            "templates/fragments/static.html",
            "templates/fragments/modals.html",
            "templates/fragments/error-page.html",
            "templates/error/404.html",
            "templates/error/4xx.html",
            "templates/error/5xx.html"
    };

    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/aquadesk");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setMessageSource(messages);
        templateEngine = engine;
    }

    @Test
    void rendersAquariumListWithLocalizedLabelsWithoutModelData() {
        String spanish = render("index", Locale.forLanguageTag("es"));
        String english = render("index", Locale.ENGLISH);

        assertTrue(spanish.contains("<html lang=\"es\">"));
        assertTrue(spanish.contains(">Acuarios</h1>"));
        assertTrue(spanish.contains("No hay acuarios configurados"));
        assertTrue(spanish.contains("id=\"aquarium-create-form\""));

        assertTrue(english.contains("<html lang=\"en\">"));
        assertTrue(english.contains(">Aquariums</h1>"));
        assertTrue(english.contains("No aquariums configured"));
        assertTrue(english.contains("id=\"aquarium-create-form\""));
    }

    @Test
    void rendersAquariumManagementPageAndInjectsLocalizedJavascriptContext() {
        WebContext spanishContext = context(Locale.forLanguageTag("es"));
        spanishContext.setVariable("aquariumId", 42);
        String spanish = templateEngine.process("gestion", spanishContext);

        WebContext englishContext = context(Locale.ENGLISH);
        englishContext.setVariable("aquariumId", 57);
        String english = templateEngine.process("gestion", englishContext);

        assertTrue(spanish.contains("<html lang=\"es\">"));
        assertTrue(spanish.contains("Próximos eventos"));
        assertTrue(spanish.contains("id=\"life-create-form\""));
        assertTrue(spanish.contains("window.appLocale = \"es\";"));
        assertTrue(spanish.contains("window.aquariumId = 42;"));
        assertTrue(spanish.contains("src=\"/gestion.js\""));

        assertTrue(english.contains("<html lang=\"en\">"));
        assertTrue(english.contains("Upcoming events"));
        assertTrue(english.contains("window.appLocale = \"en\";"));
        assertTrue(english.contains("window.aquariumId = 57;"));
    }

    @Test
    void rendersManagementPageWithoutAquariumContextWithoutShowingAquariumActions() {
        String html = render("gestion", Locale.forLanguageTag("es"));

        assertTrue(html.contains("window.aquariumId = null;"));
        assertTrue(html.contains("id=\"history-export-modal\""));
        assertFalse(html.contains("data-bs-target=\"#history-export-modal\""));
    }

    @Test
    void rendersNotFoundPageThroughSharedThymeleafFragmentsInBothLocales() {
        String spanish = render("error/404", Locale.forLanguageTag("es"));
        String english = render("error/404", Locale.ENGLISH);

        assertTrue(spanish.contains("<html lang=\"es\">"));
        assertTrue(spanish.contains("Página no encontrada | AquaDesk"));
        assertTrue(spanish.contains("No hemos encontrado la página que buscas."));
        assertTrue(spanish.contains("Volver a acuarios"));
        assertTrue(spanish.contains("href=\"/acuario\""));

        assertTrue(english.contains("<html lang=\"en\">"));
        assertTrue(english.contains("Page not found | AquaDesk"));
        assertTrue(english.contains("We couldn"));
        assertTrue(english.contains("looking for."));
        assertTrue(english.contains("Back to aquariums"));
    }

    @Test
    void rendersClientAndServerErrorPagesWithStatusAndTranslatedContent() {
        WebContext clientContext = context(Locale.forLanguageTag("es"));
        clientContext.setVariable("status", 400);
        String clientError = templateEngine.process("error/4xx", clientContext);

        WebContext serverContext = context(Locale.ENGLISH);
        serverContext.setVariable("status", 503);
        String serverError = templateEngine.process("error/5xx", serverContext);

        assertTrue(clientError.contains(">400</div>"));
        assertTrue(clientError.contains("La solicitud no es válida o ya no está disponible."));
        assertTrue(clientError.contains("Volver a acuarios"));

        assertTrue(serverError.contains(">503</div>"));
        assertTrue(serverError.contains("An error occurred"));
        assertTrue(serverError.contains("Something unexpected happened."));
        assertTrue(serverError.contains("Back to aquariums"));
    }

    @Test
    void renderedPagesDoNotExposeUnprocessedThymeleafAttributesOrExpressions() {
        for (String page : new String[] {
                render("index", Locale.forLanguageTag("es")),
                render("gestion", Locale.forLanguageTag("es")),
                render("error/404", Locale.forLanguageTag("es"))
        }) {
            String renderedMarkup = page.replaceAll("(?s)<!--.*?-->", "");
            Matcher unresolvedAttribute = THYMELEAF_ATTRIBUTE.matcher(renderedMarkup);
            assertFalse(unresolvedAttribute.find(), () -> "Rendered page contains a Thymeleaf attribute: "
                    + renderedMarkup.substring(Math.max(0, unresolvedAttribute.start() - 20),
                    Math.min(renderedMarkup.length(), unresolvedAttribute.end() + 40)));
            assertFalse(renderedMarkup.contains("#{"), "Rendered page contains an unresolved message expression");
            assertFalse(renderedMarkup.contains("[[#{"),
                    "Rendered page contains an unresolved JavaScript message");
        }
    }

    @Test
    void everyTemplateMessageKeyExistsInSpanishAndEnglishBundles() throws IOException {
        Properties spanish = loadProperties("i18n/aquadesk.properties");
        Properties english = loadProperties("i18n/aquadesk_en.properties");
        Set<String> keys = new HashSet<>();

        for (String templateFile : TEMPLATE_FILES) {
            Matcher matcher = MESSAGE_KEY.matcher(readResource(templateFile));
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }

        assertFalse(keys.isEmpty(), "Expected to find localized message keys in templates");
        for (String key : keys) {
            assertTrue(spanish.containsKey(key), "Missing Spanish translation for " + key);
            assertTrue(english.containsKey(key), "Missing English translation for " + key);
        }
    }

    @Test
    void frontendScriptsOnlyReferenceExistingPageElementsAndModules() throws IOException {
        assertScriptDomContract("aquarium-list.js", render("index", Locale.forLanguageTag("es")));
        assertScriptDomContract("gestion.js", render("gestion", Locale.forLanguageTag("es")));
    }

    private String render(String template, Locale locale) {
        return templateEngine.process(template, context(locale));
    }

    private void assertScriptDomContract(String entryPoint, String html) throws IOException {
        ArrayDeque<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(entryPoint);

        while (!pending.isEmpty()) {
            String script = pending.removeFirst();
            if (!visited.add(script)) {
                continue;
            }
            String source = readResource("static/" + script);
            Matcher imports = SCRIPT_IMPORT.matcher(source);
            while (imports.find()) {
                String imported = imports.group(1);
                assertTrue(imported.startsWith("./"), "Unexpected non-relative module import: " + imported);
                Path parent = Path.of(script).getParent();
                Path resolved = (parent == null ? Path.of(imported.substring(2))
                        : parent.resolve(imported.substring(2))).normalize();
                assertFalse(resolved.startsWith(".."), "Module import escapes static resources: " + imported);
                pending.addLast(resolved.toString().replace('\\', '/'));
            }

            Matcher ids = DOM_ID.matcher(source);
            while (ids.find()) {
                String id = ids.group(1);
                if (!OPTIONAL_DOM_IDS.contains(id)) {
                    assertTrue(html.contains("id=\"" + id + "\""),
                            () -> script + " references missing page element #" + id);
                }
            }
        }
    }

    private Properties loadProperties(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing classpath resource " + resource);
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private String readResource(String resource) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing classpath resource " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private WebContext context(Locale locale) {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        return new WebContext(webApplication.buildExchange(request, response), locale);
    }
}
