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
package nl.sonicity.sha2017.cms.cmshabackend.internal;

import nl.sonicity.sha2017.cms.cmshabackend.api.exceptions.ResourceNotFoundException;
import nl.sonicity.sha2017.cms.cmshabackend.api.exceptions.ZoneAlreadyClaimedException;
import nl.sonicity.sha2017.cms.cmshabackend.api.models.Claim;
import nl.sonicity.sha2017.cms.cmshabackend.api.models.Coordinate;
import nl.sonicity.sha2017.cms.cmshabackend.api.models.Zone;
import nl.sonicity.sha2017.cms.cmshabackend.persistence.ActiveClaimRepository;
import nl.sonicity.sha2017.cms.cmshabackend.persistence.CueLocationRepository;
import nl.sonicity.sha2017.cms.cmshabackend.persistence.ZoneMappingRepository;
import nl.sonicity.sha2017.cms.cmshabackend.persistence.entities.ActiveClaim;
import nl.sonicity.sha2017.cms.cmshabackend.persistence.entities.Colour;
import nl.sonicity.sha2017.cms.cmshabackend.persistence.entities.CueLocation;
import nl.sonicity.sha2017.cms.cmshabackend.persistence.entities.ZoneMapping;
import nl.sonicity.sha2017.cms.cmshabackend.titan.TitanService;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.CreateRgbCueResult;
import nl.sonicity.sha2017.cms.cmshabackend.titan.models.HandleLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class CoreImpl implements Core {
    private static final Logger LOG = LoggerFactory.getLogger(CoreImpl.class);

    private TitanService titanService;
    private ActiveClaimRepository activeClaimRepository;
    private ZoneMappingRepository zoneMappingRepository;
    private CueLocationRepository cueLocationRepository;
    private HandleLocationService handleLocationService;
    private FireLotteryService fireLotteryService;

    public CoreImpl(TitanService titanService, ActiveClaimRepository activeClaimRepository, ZoneMappingRepository zoneMappingRepository, CueLocationRepository cueLocationRepository, HandleLocationService handleLocationService, FireLotteryService fireLotteryService) {
        this.titanService = titanService;
        this.activeClaimRepository = activeClaimRepository;
        this.zoneMappingRepository = zoneMappingRepository;
        this.cueLocationRepository = cueLocationRepository;
        this.handleLocationService = handleLocationService;
        this.fireLotteryService = fireLotteryService;
    }

    @Override
    public ZoneMapping processClaim(String zoneName, Claim claim) {
        ZoneMapping zoneMapping = zoneMappingRepository.findOneByZoneName(zoneName)
                .orElseThrow(() -> new ResourceNotFoundException("No mapping found for zone " + zoneName));

        if (zoneMapping.getActiveClaim() != null) {
            throw new ZoneAlreadyClaimedException("This zone is already claimed");
        }

        CueLocation reservedLocation = cueLocationRepository.findAllByReservedIsFalse().stream()
                .filter((cueLocation -> {
                    try {
                        HandleLocation handleLocation = new HandleLocation(cueLocation.getGroup(), cueLocation.getPageIndex(), cueLocation.getPage());
                        Optional<HandleLocation> reservedHandle = handleLocationService.reserveHandleLocation(handleLocation);
                        return reservedHandle.isPresent();
                    } catch (Exception e) {
                        /* Catch the exception here because we want reserveHandleLocation to be able to throw some
                         * exceptions as that might be needed to rollback database changes in the transaction
                         */
                        LOG.warn("Failed to reserve location {} due to exception {}", cueLocation, e.getMessage(), e);
                    }
                    return false; }))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("No free handles in the system"));

        HandleLocation handleLocation = new HandleLocation(reservedLocation.getGroup(), reservedLocation.getPageIndex(), reservedLocation.getPage());
        LOG.debug("Selected location {} for this cue", handleLocation);

        CreateRgbCueResult rgbCue = titanService.createRgbCue(handleLocation, zoneMapping.getTitanGroupName(), claim.getRed(), claim.getGreen(), claim.getBlue());
        titanService.activateCue(rgbCue.getTitanId());

        Colour claimColour = new Colour(claim.getRed(), claim.getGreen(), claim.getBlue());
        ActiveClaim activeClaim = new ActiveClaim(LocalDateTime.now(), Duration.ofMinutes(10), rgbCue.getTitanId(), claimColour);
        activeClaimRepository.save(activeClaim);

        zoneMapping.setActiveClaim(activeClaim);
        zoneMappingRepository.save(zoneMapping);

        CueLocation claimedLocation = cueLocationRepository.findOne(new CueLocation.CueLocationPk(handleLocation.getGroup(), handleLocation.getPage(), handleLocation.getIndex()));
        claimedLocation.setActiveClaim(activeClaim);
        claimedLocation.setReserved(true); // We know its true, but hibernate doesn't
        cueLocationRepository.save(claimedLocation);
        LOG.debug("Updated location {} with titanId {}", claimedLocation, activeClaim.getPlaybackTitanId());

        return zoneMappingRepository.findOne(zoneMapping.getId());
    }

    @Override
    public List<Zone> getSpecialZones() {
        boolean available = fireLotteryService.getFireSystemAvailable();
        String claimTicket = fireLotteryService.enterDraw().orElse(null);

        List<Coordinate> towerLocations = new ArrayList<>();
        towerLocations.add(new Coordinate(52.284546, 5.525769));
        towerLocations.add(new Coordinate(52.283224, 5.525218));

        Zone flameThrower = new Zone("FlameThrowers", available, null, towerLocations, claimTicket);
        return Collections.singletonList(flameThrower);
    }
}
