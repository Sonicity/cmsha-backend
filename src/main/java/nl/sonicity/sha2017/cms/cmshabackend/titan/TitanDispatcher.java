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
package nl.sonicity.sha2017.cms.cmshabackend.titan;

import com.fasterxml.jackson.core.io.SerializedString;
import nl.sonicity.sha2017.cms.cmshabackend.titan.exceptions.RequestFailedException;
import nl.sonicity.sha2017.cms.cmshabackend.titan.exceptions.ValueOutOfRangeException;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.FixtureControlId;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.Handle;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.HandleLocation;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.HandleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class TitanDispatcher {

    private static final String FLOAT_PATTERN = "#.###";
    private static final String PARAM_HANDLE_TITAN_ID = "handle_titanId";
    private static final String PARAM_GROUP = "group";
    private static final String PARAM_INDEX = "index";
    private static final String PARAM_UPDATE_ONLY = "updateOnly";
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_LEVEL = "level";

    private String baseUrl;

    @Autowired
    @Qualifier("titanRestTemplate")
    private RestTemplate restTemplate;

    public String getBaseUrl() {
        return baseUrl;
    }

    @Value("${titan.webapi.url}")
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getVersion() {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/get/System/SoftwareVersion");

        return restTemplate
                .getForObject(builder.build().encode().toString(), SerializedString.class).getValue();
    }

    public String getShowName() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/get/Show/ShowName");

        return restTemplate
                .getForObject(builder.build().encode().toString(), SerializedString.class).getValue();

    }

    public List<Handle> listFixtures() {
        return getHandlesByType(HandleType.FIXTURES);
    }

    public List<Handle> listGroups() {
        return getHandlesByType(HandleType.GROUPS);
    }

    public List<Handle> listColours() {
        return getHandlesByType(HandleType.COLOURS);
    }

    public List<Handle> listMacros() {
        return getHandlesByType(HandleType.MACROS);
    }

    public  List<Handle> listPlaybacks() {
        return getHandlesByType(HandleType.PLAYBACKS);
    }

    private List<Handle> getHandlesByType(HandleType handleType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(String.format("/titan/handles/%s", handleType.getTitanGroupName()));

        Handle[] fixtures = restTemplate
                .getForObject(builder.build().encode().toString(), Handle[].class);

        return Arrays.asList(fixtures);
    }

    // curl "http://10.71.96.105:4430/titan/script/2/Handles/GetHandle?group=Fixtures&index=80&page=3"
    public Optional<Handle> getHandleByLocation(HandleLocation handleLocation) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Handles/GetHandle")
                .queryParam(PARAM_GROUP, handleLocation.getGroup())
                .queryParam(PARAM_INDEX, handleLocation.getIndex())
                .queryParam(PARAM_PAGE, handleLocation.getPage());

        return Optional.ofNullable(restTemplate
                .getForObject(builder.build().encode().toString(), Handle.class));

    }

    public void playbacksSelectionClear() {
        executeTitanScriptCall(getUrlForPath("/titan/script/2/Playbacks/Selection/Clear"));
    }

    public void programmerEditorClearAll() {
        executeTitanScriptCall(getUrlForPath("/titan/script/2/Programmer/Editor/ClearAll"));
    }

    public void programmerEditorFixturesSetControlValue(FixtureControlId fixtureControlId, int functionId, float value,
                                                        boolean programmer, boolean createRestorePoint) throws ValueOutOfRangeException {
        if (value < 0 || value > 1) {
            throw new ValueOutOfRangeException("Invalid value for value");
        }
        DecimalFormat formatter = new DecimalFormat(FLOAT_PATTERN);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Programmer/Editor/Fixtures/SetControlValueById")
                .queryParam("controlId", fixtureControlId.getValue())
                .queryParam("functionId",functionId )
                .queryParam("value", formatter.format(value))
                .queryParam("programmer", Boolean.toString(programmer))
                .queryParam("createRestorePoint", Boolean.toString(createRestorePoint));

        executeTitanScriptCall(builder.build().encode().toString());

    }

    public void firePlayback(int userNumber, float level, boolean alwaysRefire) throws ValueOutOfRangeException {
        if (level < 0 || level > 1) {
            throw new ValueOutOfRangeException("Invalid value for level");
        }
        DecimalFormat formatter = new DecimalFormat(FLOAT_PATTERN);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/Playbacks/FirePlaybackAtLevel")
                .queryParam("userNumber", userNumber)
                .queryParam(PARAM_LEVEL, formatter.format(level))
                .queryParam("bool", alwaysRefire);

        executeTitanScriptCall(builder.build().encode().toString());
    }

    public void selectionContextSelectFixture(int titanId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Selection/Context/Global/SelectFixture")
                .queryParam(PARAM_HANDLE_TITAN_ID, titanId);

        executeTitanScriptCall(builder.build().encode().toString());
    }

    public void groupRecallGroupById(int titanId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Group/RecallGroupById")
                .queryParam("groupId", titanId);

        executeTitanScriptCall(builder.build().encode().toString());
    }

    public void groupRecallGroup(int titanId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Group/RecallGroup")
                .queryParam(PARAM_HANDLE_TITAN_ID, titanId);

        executeTitanScriptCall(builder.build().encode().toString());
    }

    public void programmerEditorFixturesLocateSelectedFixtures(boolean allAttributes) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Programmer/Editor/Fixtures/LocateSelectedFixtures")
                .queryParam("allAttributes", Boolean.toString(allAttributes));

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    public void playbacksStoreCue(String group, int index, boolean updateOnly) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Playbacks/StoreCue")
                .queryParam(PARAM_GROUP, group)
                .queryParam(PARAM_INDEX, index)
                .queryParam(PARAM_UPDATE_ONLY, Boolean.toString(updateOnly));

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    public void playbacksPlayCue(String group, int index, float level, float accuracy) {
        DecimalFormat formatter = new DecimalFormat(FLOAT_PATTERN);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Playbacks/PlayCue")
                .queryParam(PARAM_GROUP, group)
                .queryParam(PARAM_INDEX, index)
                .queryParam(PARAM_LEVEL, formatter.format(level))
                .queryParam("accuracy", formatter.format(accuracy));

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    public void playbacksPlayback(int titanId, float level, float accuracy) {
        DecimalFormat formatter = new DecimalFormat(FLOAT_PATTERN);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Playbacks/PlayPlayback")
                .queryParam(PARAM_HANDLE_TITAN_ID, titanId)
                .queryParam(PARAM_LEVEL, formatter.format(level))
                .queryParam("accuracy", formatter.format(accuracy));

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    public void playbacksReplacePlaybackCue(int titanId, boolean updateOnly) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Playbacks/ReplacePlaybackCue")
                .queryParam(PARAM_HANDLE_TITAN_ID, titanId)
                .queryParam("updateOnly", Boolean.toString(updateOnly));

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    public void programmerSetBlindMode(boolean setChangesLive, float fadeTime) {
        DecimalFormat formatter = new DecimalFormat(FLOAT_PATTERN);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Programmer/SetBlindMode")
                .queryParam("setChangesLive", Boolean.toString(setChangesLive))
                .queryParam("fadeTime", formatter.format(fadeTime));

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    public boolean programmerIsBlindActive() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/get/2/Programmer/BlindActive");

        String booleanString = restTemplate
                .getForObject(builder.build().encode().toString(), String.class);

        return Boolean.parseBoolean(booleanString);
    }

    public void playbacksSelectEditHandle(int titanId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Playbacks/Select/EditHandle")
                .queryParam(PARAM_HANDLE_TITAN_ID, titanId);

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    public int getPlaybacksPlaybackOptionsPriority() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/get/2/Playbacks/PlaybackOptions/Priority");

        String requestUrl = builder.build().encode().toString();

        String jsonString = restTemplate
                .getForObject(requestUrl, String.class);

        return Integer.parseInt(jsonString);
    }

    public void setPlaybacksPlaybackOptionsPriority(int priority) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/set/2/Playbacks/PlaybackOptions/Priority");

        String requestUrl = builder.build().encode().toString();

        restTemplate
                .exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(Integer.toString(priority)), Void.class);
    }

    public void playbacksPlaybackEditExit() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Playbacks/PlaybackEdit/Exit");

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    public void setProgrammerBlindActive(boolean blindActive) {
        // or http://[ip]:4430/titan/script/2/ActionScript/SetProperty/Boolean?id=Programmer.BlindActive&value=true
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/set/2/Programmer/BlindActive");

        String requestUrl = builder.build().encode().toString();

        restTemplate
                .exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(Boolean.toString(blindActive)), Void.class);
    }

    public void setGroupPage(String groupName, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/titan/script/2/Handles/SetGroupPage")
                .queryParam("groupName", groupName)
                .queryParam("page", page);

        String requestUrl = builder.build().encode().toString();
        executeTitanScriptCall(requestUrl);
    }

    private ResponseEntity<Void> executeTitanScriptCall(String requestUrl) {
        try {
            return restTemplate.exchange(requestUrl, HttpMethod.GET, null, Void.class);
        } catch (HttpServerErrorException e) {

            throw new RequestFailedException(e.getResponseBodyAsString(), e);
        }
    }

    private String getUrlForPath(String path) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path);
        return builder.build().encode().toString();
    }
}
