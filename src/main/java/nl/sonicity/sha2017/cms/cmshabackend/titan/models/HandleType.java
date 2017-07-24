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
package nl.sonicity.sha2017.cms.cmshabackend.titan.models;

/**
 * Created by hugo on 25/06/2017.
 */
public enum HandleType {
    FIXTURES("Fixtures"),
    GROUPS("Groups"),
    MACROS("Macros"),
    PLAYBACKS("Playbacks"),
    STATIC_PLAYBACKS("StaticPlaybacks"),
    COLOURS("Colours"),
    WORKSPACES("Workspaces"),
    EFFECTS("Effects"),
    ROLLER_A("RollerA"),
    ROLLER_B("RollerB"),
    PLAYBACK_WINDOW("PlaybackWindow");

    private String titanGroupName;

    HandleType(String titanGroupName) {
        this.titanGroupName = titanGroupName;
    }

    public String getTitanGroupName() {
        return titanGroupName;
    }
}
