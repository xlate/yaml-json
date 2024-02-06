package io.xlate.yamljson;

import static io.xlate.yamljson.YamlTestHelper.VERSIONS_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class YamlProviderTest {

    @DisabledIfSystemProperty(named = Yaml.Settings.YAML_VERSION, matches = "NONE")
    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testDefaultVersion(String version) throws IOException {
        String defaultVersion = YamlTestHelper.detectedVersions().first();

        if (YamlTestHelper.detectedVersions().size() == 1) {
            assertEquals(version, defaultVersion);
        } else {
            assertEquals(Yaml.Versions.V1_1, defaultVersion);
        }
    }
}
