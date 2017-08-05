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
package nl.sonicity.sha2017.cms.cmshabackend.api;

import nl.sonicity.sha2017.cms.cmshabackend.api.models.*;
import nl.sonicity.sha2017.cms.cmshabackend.persistence.entities.SpecialZoneClaim;
import nl.sonicity.sha2017.cms.cmshabackend.titan.TitanService;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class FlamerControllerIT extends AbstractRestControllerIT {
    @MockBean
    private TitanService titanService;

    @Test
    public void testFlameClaim() throws Exception {
        prepareDatabase();

        SpecialZoneClaim flameZone = specialZoneClaimRepository.findOneByZoneName("FlameThrowers")
                .orElseThrow(() -> new Exception("Mock data not present"));

        flameZone.setClaimTag("claimticket");
        flameZone.setClaimed(LocalDateTime.now());
        flameZone.setClaimExpiration(Duration.ofSeconds(120));
        specialZoneClaimRepository.save(flameZone);

        FlamerClaimRequest request = new FlamerClaimRequest("claimticket");
        HttpEntity<FlamerClaimRequest> entity = new HttpEntity<>(request, null);

        restTemplate.exchange("http://localhost:{port}/flamer/claim", HttpMethod.POST, entity, FlamerClaimResponse.class, localServerPort);

        SpecialZoneClaim claimedFlameZone = specialZoneClaimRepository.findOneByZoneName("FlameThrowers")
                .orElseThrow(() -> new Exception("Mock data not present"));
        assertThat(claimedFlameZone.getClaimExpiration(), equalTo(Duration.ofMinutes(32)));

        FlamerFireRequest fireRequest = new FlamerFireRequest("claimticket", 1);
        HttpEntity<FlamerFireRequest> fireEntity = new HttpEntity<>(fireRequest, null);

        restTemplate.exchange("http://localhost:{port}/flamer/fire", HttpMethod.POST, fireEntity, FlamerFireResponse.class, localServerPort);

        SpecialZoneClaim firedFlameZone = specialZoneClaimRepository.findOneByZoneName("FlameThrowers")
                .orElseThrow(() -> new Exception("Mock data not present"));
        assertThat(firedFlameZone.getClaimExpiration(), equalTo(Duration.ofMinutes(2)));

        fireEntity = new HttpEntity<>(new FlamerFireRequest("claimticket", 2), null);
        restTemplate.exchange("http://localhost:{port}/flamer/fire", HttpMethod.POST, fireEntity, FlamerFireResponse.class, localServerPort);

        firedFlameZone = specialZoneClaimRepository.findOneByZoneName("FlameThrowers")
                .orElseThrow(() -> new Exception("Mock data not present"));
        assertThat(firedFlameZone.getClaimExpiration(), equalTo(Duration.ofMinutes(2)));

        FlamerStatusResponse status = restTemplate.getForObject("http://localhost:{port}/flamer/status", FlamerStatusResponse.class, localServerPort);
        assertThat(status.isAvailable(), equalTo(false));
    }

}
