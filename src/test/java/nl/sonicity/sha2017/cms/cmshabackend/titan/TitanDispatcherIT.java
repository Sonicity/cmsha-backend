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

import nl.sonicity.sha2017.cms.cmshabackend.titan.models.FixtureControlId;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.Handle;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.HandleLocation;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by hugo on 24/06/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:integrationtest.properties")
@ActiveProfiles({"mock-titan", "test"})
@Ignore
public class TitanDispatcherIT {
    @Autowired
    private TitanDispatcher titanDispatcher;

    @Value("${ittest.showName}")
    private String showName;

    @Value("${ittest.testGroup}")
    private String testGroup;

    @Test
    public void selectAndLocateFixture() throws Exception {
        String showname = titanDispatcher.getShowName();
        assertThat("This test depends a matching show loaded in the titan", showname, equalTo(showname));

        Handle fixture = titanDispatcher.listFixtures()
                .stream()
                .filter(f -> f.getHandleLocation().getIndex() == 1 &&
                    f.getHandleLocation().getPage() == 0)
                .findFirst().orElseThrow(() -> new Exception("No fixtures found"));

        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.programmerEditorClearAll();
        titanDispatcher.selectionContextSelectFixture(fixture.getTitanId());
        titanDispatcher.programmerEditorFixturesLocateSelectedFixtures(false);

        // Visually inspect, first lamp should be "located"
    }

    @Test
    public void selectAndLocateGroup() {
        String showname = titanDispatcher.getShowName();
        assertThat("This test depends a matching show loaded in the titan", showname, equalTo(showname));

        titanDispatcher.listGroups()
                .stream()
                .forEach(handle->{
                    System.out.println(handle.getLegend() + ": " + handle.getTitanId());
                });

        Optional<Handle> handle = titanDispatcher.listGroups()
                .stream()
                .filter(group -> group.getLegend().equals(testGroup))
                .findFirst();

        assertThat(handle.isPresent(), equalTo(true));

        titanDispatcher.programmerEditorClearAll();
        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.groupRecallGroupById(handle.get().getTitanId());
        titanDispatcher.programmerEditorFixturesLocateSelectedFixtures(false);

        // Visually inspect, first lamp should be "located"
    }

    @Test
    public void clearProgrammer() {
        String showname = titanDispatcher.getShowName();
        assertThat("This test depends a matching show loaded in the titan", showname, equalTo(showname));

        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.programmerEditorClearAll();
    }

    @Test
    public void createHighPrioColorCue() throws Exception {
        String showname = titanDispatcher.getShowName();
        assertThat("This test depends a matching show loaded in the titan", showname, equalTo(showname));

        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.programmerEditorClearAll();

        // Select group
        Handle handle = titanDispatcher.listGroups()
                .stream()
                .filter(group -> group.getLegend().equals(testGroup))
                .findFirst().orElseThrow(() -> new Exception("No handle " + testGroup + " Found"));

        // Setting Blind clears the selection
        titanDispatcher.setProgrammerBlindActive(true);
        titanDispatcher.programmerSetBlindMode(false, 0);

        titanDispatcher.groupRecallGroupById(handle.getTitanId());

        assertThat(titanDispatcher.programmerIsBlindActive(), equalTo(true));

        // Set Dimmer 100%
        titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.DIMMER, 1, 1, true, false);

        // Set Red 100%
        titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.RED, 1, 0f, true, false);

        // Set Green 8%
        titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.GREEN, 1, 1f, true, false);

        // Set Blue 0%
        titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.BLUE, 1, 0f, true, false);

        // Set Macro "Safe"
        // Set Mode "Dimmer"

        HandleLocation cueLocation = new HandleLocation("PlaybackWindow", 24, 0);

        // Check contents of the target location
        Optional<Handle> currentHandle = titanDispatcher.getHandleByLocation(cueLocation);
        assertThat(currentHandle.isPresent(), equalTo(false));

        titanDispatcher.setGroupPage("PlaybackWindow", 0);

        titanDispatcher.playbacksStoreCue(cueLocation.getGroup(), cueLocation.getIndex(), true);

        Handle createdHandle = titanDispatcher.getHandleByLocation(cueLocation)
                .orElseThrow(() -> new Exception("Handle not found by location"));

        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.programmerEditorClearAll();

        titanDispatcher.playbacksSelectEditHandle(createdHandle.getTitanId());
        titanDispatcher.setPlaybacksPlaybackOptionsPriority(75);

        int priority = titanDispatcher.getPlaybacksPlaybackOptionsPriority();

        assertThat(75, equalTo(priority));

        titanDispatcher.playbacksPlaybackEditExit();

        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.programmerEditorClearAll();

        titanDispatcher.setProgrammerBlindActive(false);
        titanDispatcher.programmerSetBlindMode(false, 0);


    }

    @Test
    public void replaceHighPrioColorCue() throws Exception {
        String showname = titanDispatcher.getShowName();
        assertThat("This test depends a matching show loaded in the titan", showname, equalTo(showname));

        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.programmerEditorClearAll();

        // Select group
        Handle handle = titanDispatcher.listGroups()
                .stream()
                .filter(group -> group.getLegend().equals(testGroup))
                .findFirst().orElseThrow(() -> new Exception("No handle " + testGroup + " Found"));

        // Setting Blind clears the selection
        titanDispatcher.setProgrammerBlindActive(true);
        titanDispatcher.programmerSetBlindMode(false, 0);

        titanDispatcher.groupRecallGroupById(handle.getTitanId());

        assertThat(titanDispatcher.programmerIsBlindActive(), equalTo(true));

        // Set Dimmer 100%
        titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.DIMMER, 1, 1, true, false);

        // Set Red 100%
        titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.RED, 1, 0f, true, false);

        // Set Green 8%
        titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.GREEN, 1, 1f, true, false);

        // Set Blue 0%
        titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.BLUE, 1, 0f, true, false);

        // Set Macro "Safe"
        // Set Mode "Dimmer"

        // Check contents of the target location
        HandleLocation cueLocation = new HandleLocation("PlaybackWindow", 0, 0);
        Handle currentHandle = titanDispatcher.getHandleByLocation(cueLocation)
                .orElseThrow(() -> new Exception("Handle not found"));

        titanDispatcher.playbacksReplacePlaybackCue(currentHandle.getTitanId(), false);

        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.programmerEditorClearAll();

        titanDispatcher.playbacksSelectEditHandle(currentHandle.getTitanId());
        titanDispatcher.setPlaybacksPlaybackOptionsPriority(75);

        int priority = titanDispatcher.getPlaybacksPlaybackOptionsPriority();

        assertThat(75, equalTo(priority));

        titanDispatcher.playbacksPlaybackEditExit();

        titanDispatcher.playbacksSelectionClear();
        titanDispatcher.programmerEditorClearAll();

        titanDispatcher.setProgrammerBlindActive(false);
        titanDispatcher.programmerSetBlindMode(false, 0);


    }

}
