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

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.sonicity.sha2017.cms.cmshabackend.api.models.*;
import nl.sonicity.sha2017.cms.cmshabackend.internal.FireLotteryService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static nl.sonicity.sha2017.cms.cmshabackend.api.validation.ValidationHelpers.*;

@RestController
@RequestMapping("/flamer")
@CrossOrigin(origins = "*")
public class FlamerController {
    private FireLotteryService fireLotteryService;

    public FlamerController(FireLotteryService fireLotteryService) {
        this.fireLotteryService = fireLotteryService;
    }

    @RequestMapping(path="/claim", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAuthority('ANONYMOUS') or hasRole('ROLE_ADMIN')")
    @ApiResponses({
            @ApiResponse(code = 403, message = "Invalid claim_ticket", response = ErrorDetail.class),
    })
    public FlamerClaimResponse claim(@RequestBody FlamerClaimRequest flamerClaimRequest) {
        notEmpty().test(flamerClaimRequest.getClaimTicket()).orThrow();

        LocalDateTime expiration = fireLotteryService.claim(flamerClaimRequest.getClaimTicket());
        return new FlamerClaimResponse(convertInternalTimeToNlTime(expiration));
    }

    @RequestMapping(path="/fire", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAuthority('ANONYMOUS') or hasRole('ROLE_ADMIN')")
    @ApiResponses({
            @ApiResponse(code = 403, message = "Invalid claim_ticket", response = ErrorDetail.class),
    })
    public FlamerFireResponse claim(@RequestBody FlamerFireRequest flamerFireRequest) {
        notEmpty().test(flamerFireRequest.getClaimTicket()).orThrow();
        notNull().and(between(1, 4)).test(flamerFireRequest.getAction());

        LocalDateTime expiration =
                fireLotteryService.fire(flamerFireRequest.getClaimTicket(), flamerFireRequest.getAction());
        return new FlamerFireResponse(convertInternalTimeToNlTime(expiration));
    }

    @RequestMapping(path="/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAuthority('ANONYMOUS') or hasRole('ROLE_ADMIN')")
    public FlamerStatusResponse status() {
        return new FlamerStatusResponse(fireLotteryService.getFireSystemAvailable());
    }

    private LocalDateTime convertInternalTimeToNlTime(LocalDateTime localDateTime) {
        ZonedDateTime ldtZoned = localDateTime.atZone(ZoneId.systemDefault());
        ZonedDateTime nlZoned = ldtZoned.withZoneSameInstant(ZoneId.of("Europe/Amsterdam"));

        return nlZoned.toLocalDateTime();
    }

}
