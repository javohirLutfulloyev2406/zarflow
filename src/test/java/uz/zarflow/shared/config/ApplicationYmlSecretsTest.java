package uz.zarflow.shared.config;

import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard: src/main/resources/application.yml must never carry a real
 * auth secret, only ${ENV_VAR} placeholders — test-only values belong in
 * src/test/resources/application-test.yml, which never ships in the production jar.
 */
class ApplicationYmlSecretsTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{[A-Z_]+(:.*)?}$");

    @Test
    @SuppressWarnings("unchecked")
    void mainApplicationYmlHasNoHardcodedAuthSecrets() throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
            for (Object doc : yaml.loadAll(in)) {
                Map<String, Object> document = (Map<String, Object>) doc;
                assertPlaceholderIfPresent(document, "auth", "bot-token");
                assertPlaceholderIfPresent(document, "jwt", "secret");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void assertPlaceholderIfPresent(Map<String, Object> document, String section, String key) {
        Object zarflow = document.get("zarflow");
        if (!(zarflow instanceof Map<?, ?> zarflowMap)) {
            return;
        }
        Object sectionValue = zarflowMap.get(section);
        if (!(sectionValue instanceof Map<?, ?> sectionMap)) {
            return;
        }
        Object value = sectionMap.get(key);
        if (value == null) {
            return;
        }
        assertThat(value.toString())
                .matches(PLACEHOLDER)
                .as("zarflow.%s.%s must be an env-var placeholder, not a literal value", section, key);
    }

}
