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

import nl.sonicity.sha2017.cms.cmshabackend.api.exceptions.ResourceNotFoundException;
import nl.sonicity.sha2017.cms.cmshabackend.titan.exceptions.ValueOutOfRangeException;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.CreateRgbCueResult;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.FixtureControlId;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.Handle;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.HandleLocation;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Created by hugo on 02/07/2017.
 */
@Component
@Profile("!mock-titan")
public class TitanServiceImpl implements TitanService {
    private static final Object LOCK = new Object();
    private TitanDispatcher titanDispatcher;
    private TitanProperties titanProperties;

    public TitanServiceImpl(TitanDispatcher titanDispatcher, TitanProperties titanProperties) {
        this.titanDispatcher = titanDispatcher;
        this.titanProperties = titanProperties;
    }

    @Override
    public boolean groupExists(String groupName) {
        synchronized (LOCK) {
            return titanDispatcher.listGroups().stream().anyMatch(g -> g.getLegend().equals(groupName));
        }
    }

    @Override
    public CreateRgbCueResult createRgbCue(HandleLocation cueLocation, String groupName, float red, float green, float blue) {
        synchronized (LOCK) {
            titanDispatcher.playbacksSelectionClear();
            titanDispatcher.programmerEditorClearAll();

            // Select group
            Handle handle = titanDispatcher.listGroups()
                    .stream()
                    .filter(group -> group.getLegend().equals(groupName))
                    .findFirst().orElseThrow(() -> new ResourceNotFoundException("No handle " + groupName + " Found"));

            // Setting Blind clears the selection
            titanDispatcher.setProgrammerBlindActive(true);
            titanDispatcher.programmerSetBlindMode(false, 0);

            titanDispatcher.groupRecallGroupById(handle.getTitanId());

            try {
                // Set Dimmer 100%
                titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.DIMMER, 1, 1, true, false);

                // Set Red 100%
                titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.RED, 1, red, true, false);

                // Set Green 8%
                titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.GREEN, 1, green, true, false);

                // Set Blue 0%
                titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.BLUE, 1, blue, true, false);

                // Set Strobe 0%
                // FIXME wtf is the strobe
                // titanDispatcher.programmerEditorFixturesSetControlValue(FixtureControlId.STRO, 1, blue, true, false);
            } catch (ValueOutOfRangeException e) {
                throw new IllegalStateException("Invalid arguments for internal function", e);
            }

            // Set Macro "Safe"
            // Set Mode "Dimmer"

            // Check contents of the target location
            Optional<Handle> currentHandle = titanDispatcher.getHandleByLocation(cueLocation);
            Handle playbackHandle = null;

            if (currentHandle.isPresent()) {
                titanDispatcher.playbacksReplacePlaybackCue(currentHandle.get().getTitanId(), false);
                playbackHandle = currentHandle.get();
            } else {
                titanDispatcher.playbacksStoreCue(cueLocation.getGroup(), cueLocation.getIndex(), true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore and rethrow
                    throw new RuntimeException(e);
                }
                playbackHandle = titanDispatcher.getHandleByLocation(cueLocation)
                        .orElseThrow(() -> new ResourceNotFoundException("Handle not found by location"));
            }

            titanDispatcher.playbacksSelectionClear();
            titanDispatcher.programmerEditorClearAll();

            titanDispatcher.playbacksSelectEditHandle(playbackHandle.getTitanId());
            titanDispatcher.setPlaybacksPlaybackOptionsPriority(75);

            titanDispatcher.playbacksPlaybackEditExit();

            titanDispatcher.playbacksSelectionClear();
            titanDispatcher.programmerEditorClearAll();

            titanDispatcher.setProgrammerBlindActive(false);
            titanDispatcher.programmerSetBlindMode(false, 0);

            return new CreateRgbCueResult(cueLocation, playbackHandle.getTitanId());
        }
    }

    @Override
    public TitanStatus getStatus() {
        synchronized (LOCK) {
            String showName = titanDispatcher.getShowName();
            String titanVersion = titanDispatcher.getVersion();
            return new TitanStatus(showName, titanVersion);
        }
    }

    @Override
    public String getTitanUrl() {
        return titanDispatcher.getBaseUrl();
    }

    @Override
    public void activateCue(int cueId) {
        synchronized (LOCK) {
            titanDispatcher.playbacksPlayback(cueId, 1, 1);
        }
    }

    @Override
    public void deactivateCue(int cueId) {
        synchronized (LOCK) {
            titanDispatcher.playbacksPlayback(cueId, 0, 1);
        }
    }

    @Override
    public boolean isHandleActive(HandleLocation handleLocation) {
        synchronized (LOCK) {
            Optional<Handle> handle = titanDispatcher.getHandleByLocation(handleLocation);
            return handle.isPresent() && handle.get().isActive();
        }
    }

    @Override
    public HandleLocation getHandleLocationFromProperties(String handleName) {
        return titanProperties.getHandleLocation(handleName);
    }
}
