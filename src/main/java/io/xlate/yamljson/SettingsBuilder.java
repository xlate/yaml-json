package io.xlate.yamljson;

import java.util.Map;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.LoadSettingsBuilder;

interface SettingsBuilder {

    default DumpSettings buildDumpSettings(Map<String, ?> properties) {
        DumpSettingsBuilder settings = DumpSettings.builder();

        properties.entrySet().forEach(property -> {
            final String name = property.getKey();
            final Object value = property.getValue();

            switch (name) {
            case Yaml.Settings.DUMP_EXPLICIT_START:
                settings.setExplicitStart(Boolean.valueOf(String.valueOf(value)));
                break;
            case Yaml.Settings.DUMP_EXPLICIT_END:
                settings.setExplicitEnd(Boolean.valueOf(String.valueOf(value)));
                break;
            default:
                break;
            }
            /* TODO Map to snakeyaml settings */
        });

        return settings.build();
    }

    default LoadSettings buildLoadSettings(Map<String, ?> properties) {
        LoadSettingsBuilder settings = LoadSettings.builder();

        properties.entrySet().forEach(property -> {
            /* TODO Map to snakeyaml settings */
        });

        return settings.build();
    }
}
