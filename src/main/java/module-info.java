/*
 * Copyright 2021 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * yaml-json is an API to read and write YAML in Java using the Jakarta JSON API
 *
 * @author Michael Edgar
 * @see <a href="https://github.com/xlate/yaml-json" target="_blank">yaml-json on GitHub</a>
 */
module io.xlate.yamljson {

    requires java.base;
    requires java.logging;
    requires transitive jakarta.json;

    requires static org.snakeyaml.engine;
    requires static org.yaml.snakeyaml;

    exports io.xlate.yamljson;

}
