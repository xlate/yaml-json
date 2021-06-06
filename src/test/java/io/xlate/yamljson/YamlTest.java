package io.xlate.yamljson;

import static io.xlate.yamljson.YamlTestHelper.VERSIONS_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class YamlTest {

    @Test
    @EnabledIfSystemProperty(named = Yaml.Settings.YAML_VERSION, matches = "NONE")
    void testIllegalStateExceptionThrown() {
        assertThrows(IllegalStateException.class, () -> new YamlProvider());
    }

    @DisabledIfSystemProperty(named = Yaml.Settings.YAML_VERSION, matches = "NONE")
    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testDefaultVersion(String version) throws IOException {
        if (Yaml.Versions.supportedVersions().size() == 1) {
            assertEquals(version, new YamlProvider().defaultVersion);
        } else {
            assertEquals(Yaml.Versions.V1_1, new YamlProvider().defaultVersion);
        }
    }
}
