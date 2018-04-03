/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class TbGetEntityAttrNodeConfiguration implements NodeConfiguration<TbGetEntityAttrNodeConfiguration> {

    private Map<String, String> attrMapping;
    private boolean isTelemetry = false;

    @Override
    public TbGetEntityAttrNodeConfiguration defaultConfiguration() {
        TbGetEntityAttrNodeConfiguration configuration = new TbGetEntityAttrNodeConfiguration();
        Map<String, String> attrMapping = new HashMap<>();
        attrMapping.putIfAbsent("temperature", "tempo");
        configuration.setAttrMapping(attrMapping);
        configuration.setTelemetry(true);
        return configuration;
    }
}