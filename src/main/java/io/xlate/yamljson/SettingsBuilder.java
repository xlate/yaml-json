package io.xlate.yamljson;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v2.api.LoadSettings;
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

    default LoaderOptions buildLoaderOptions(Map<String, ?> properties) {
        // No load properties supported currently
        return new LoaderOptions();
    }

    default DumperOptions buildDumperOptions(Map<String, ?> properties) {
        DumperOptions settings = new DumperOptions();
        setBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_START, settings::setExplicitStart);
        setBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_END, settings::setExplicitEnd);
        return settings;
    }

    default LoadSettings buildLoadSettings(Map<String, ?> properties) {
        // No load properties supported currently
        return LoadSettings.builder().build();
    }

    default DumpSettings buildDumpSettings(Map<String, ?> properties) {
        DumpSettingsBuilder settings = DumpSettings.builder();
        setBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_START, settings::setExplicitStart);
        setBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_END, settings::setExplicitEnd);
        return settings.build();
    }

    default void setBoolean(Map<String, ?> properties, String key, Consumer<Boolean> setter) {
        setter.accept(getBoolean(properties, key));
    }

    default Boolean getBoolean(Map<String, ?> properties, String key) {
        return Boolean.valueOf(String.valueOf(properties.get(key)));
    }
}
