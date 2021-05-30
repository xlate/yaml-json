package io.xlate.yamljson;

import java.util.Map;
import java.util.function.Consumer;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.LoadSettingsBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;

interface SettingsBuilder {

    default LoaderOptions buildLoaderOptions(Map<String, ?> properties) {
        LoaderOptions settings = new LoaderOptions();
        /* TODO Map to snakeyaml settings */
        return settings;
    }

    default DumperOptions buildDumperOptions(Map<String, ?> properties) {
        DumperOptions settings = new DumperOptions();
        setBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_START, settings::setExplicitStart);
        setBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_END, settings::setExplicitEnd);
        return settings;
    }

    default LoadSettings buildLoadSettings(Map<String, ?> properties) {
        LoadSettingsBuilder settings = LoadSettings.builder();
        /* TODO Map to snakeyaml settings */
        return settings.build();
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
