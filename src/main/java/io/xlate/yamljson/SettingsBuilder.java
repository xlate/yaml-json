package io.xlate.yamljson;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

interface SettingsBuilder {

    static final String MOD_SNAKEYAML = "org.yaml.snakeyaml";
    static final String MOD_SNAKEYAML_ENGINE = "org.snakeyaml.engine";

    static final String MISSING_MODULE_MESSAGE = "Required module not found: %s. "
            + "Ensure module is present on module path. Add to application module-info or "
            + "include with --add-modules command line option.";

    static <T> Optional<T> loadProvider(Map<String, Object> properties, Function<Map<String, Object>, T> providerFactory) {
        try {
            return Optional.of(providerFactory.apply(properties));
        } catch (Exception | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }

    static <T> T loadProvider(Map<String, Object> properties, Function<Map<String, Object>, T> providerFactory, String providerModule) {
        try {
            return providerFactory.apply(properties);
        } catch (Exception | NoClassDefFoundError e) {
            throw new IllegalStateException(String.format(MISSING_MODULE_MESSAGE, providerModule), e);
        }
    }

    static IllegalStateException noProvidersFound() {
        return new IllegalStateException("No YAML providers found on class/module path!");
    }

    static <T> T getProperty(Map<String, ?> properties, String key, Function<String, T> parser, T defaultValue) {
        return parser.apply(String.valueOf(Objects.requireNonNullElse(properties.get(key), defaultValue)));
    }

    static <T> void replace(Map<String, Object> properties, String key, Function<String, T> parser, T defaultValue) {
        properties.put(key, getProperty(properties, key, parser, defaultValue));
    }
}
