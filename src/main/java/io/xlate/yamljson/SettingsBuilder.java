package io.xlate.yamljson;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.LoadSettingsBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;

interface SettingsBuilder {

    static final String MOD_SNAKEYAML = "org.yaml.snakeyaml";
    static final String MARKER_SNAKEYAML = "org.yaml.snakeyaml.Yaml";

    static final String MOD_SNAKEYAML_ENGINE = "org.snakeyaml.engine";
    static final String MARKER_SNAKEYAML_ENGINE = "org.snakeyaml.engine.v2.api.lowlevel.Parse";

    static final String MISSING_MODULE_MESSAGE = "Required module not found: %s. "
            + "Ensure module is present on module path. Add to application module-info or "
            + "include with --add-modules command line option.";

    default <T> T loadProvider(Supplier<T> providerSupplier, String providerModule) {
        try {
            return providerSupplier.get();
        } catch (Exception | NoClassDefFoundError e) {
            throw new IllegalStateException(String.format(MISSING_MODULE_MESSAGE, providerModule), e);
        }
    }

    default LoaderOptions buildLoaderOptions(Map<String, Object> properties) {
        LoaderOptions options = new LoaderOptions();
        replace(properties, Yaml.Settings.LOAD_MAX_ALIAS_EXPANSION_SIZE, Long::valueOf, Long.MAX_VALUE);
        // No load properties supported currently
        return options;
    }

    default DumperOptions buildDumperOptions(Map<String, Object> properties) {
        DumperOptions settings = new DumperOptions();
        settings.setExplicitStart(getProperty(properties, Yaml.Settings.DUMP_EXPLICIT_START, Boolean::valueOf, false));
        settings.setExplicitEnd(getProperty(properties, Yaml.Settings.DUMP_EXPLICIT_END, Boolean::valueOf, false));
        return settings;
    }

    default LoadSettings buildLoadSettings(Map<String, Object> properties) {
        LoadSettingsBuilder settings = LoadSettings.builder();
        settings.setUseMarks(getProperty(properties, Yaml.Settings.LOAD_USE_MARKS, Boolean::valueOf, true));
        replace(properties, Yaml.Settings.LOAD_MAX_ALIAS_EXPANSION_SIZE, Long::valueOf, Long.MAX_VALUE);
        return settings.build();
    }

    default DumpSettings buildDumpSettings(Map<String, Object> properties) {
        DumpSettingsBuilder settings = DumpSettings.builder();
        settings.setExplicitStart(getProperty(properties, Yaml.Settings.DUMP_EXPLICIT_START, Boolean::valueOf, false));
        settings.setExplicitEnd(getProperty(properties, Yaml.Settings.DUMP_EXPLICIT_END, Boolean::valueOf, false));
        return settings.build();
    }

    default <T> T getProperty(Map<String, ?> properties, String key, Function<String, T> parser, T defaultValue) {
        return parser.apply(String.valueOf(Objects.requireNonNullElse(properties.get(key), defaultValue)));
    }

    default <T> void replace(Map<String, Object> properties, String key, Function<String, T> parser, T defaultValue) {
        properties.put(key, getProperty(properties, key, parser, defaultValue));
    }
}
