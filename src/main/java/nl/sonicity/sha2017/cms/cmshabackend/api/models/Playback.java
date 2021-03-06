/**
 * Copyright © 2017 Sonicity (info@sonicity.nl)
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
package nl.sonicity.sha2017.cms.cmshabackend.api.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Playback {
    private String group;
    private Integer page;
    private Integer index;
    private Integer titanId;
    private Boolean active;

    @JsonCreator
    public Playback(@JsonProperty("group") String group, @JsonProperty("page") Integer page, @JsonProperty("index") Integer index, @JsonProperty("titanId") Integer titanId, @JsonProperty("active") Boolean active) {
        this.group = group;
        this.page = page;
        this.index = index;
        this.titanId = titanId;
        this.active = active;
    }

    public String getGroup() {
        return group;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getIndex() {
        return index;
    }

    public Integer getTitanId() {
        return titanId;
    }

    public Boolean getActive() {
        return active;
    }
}
