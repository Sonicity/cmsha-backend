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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.DataInputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Created by hugo on 02/07/2017.
 */
@Component
public class ApiKeyAuthenticationProvider implements org.springframework.security.authentication.AuthenticationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticationProvider.class);

    private PublicKey validationKey;

    @Value("${jwt.publickey.file}")
    private String jwtVerificationKeyFile;

    @PostConstruct
    public void initializeCertificates() {
        try {
            ClassPathResource classPathResource = new ClassPathResource(jwtVerificationKeyFile);
            DataInputStream dis = new DataInputStream(classPathResource.getInputStream());
            byte[] keyBytes = new byte[dis.available()];
            dis.readFully(keyBytes);
            dis.close();

            String temp = new String(keyBytes);
            String publicKeyPEM = temp.replace("-----BEGIN PUBLIC KEY-----\n", "");
            publicKeyPEM = publicKeyPEM.replaceAll("\n", "");
            publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

            byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec spec =
                    new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            validationKey = kf.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to configure the JWT secret", e);
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String apiKey = (String) authentication.getPrincipal();

        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(validationKey)
                    .parseClaimsJws(apiKey);

            PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(apiKey, null,
                    AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ACTUATOR"));
            authenticationToken.setAuthenticated(true);
            LOG.info("ApiKey verified, granting admin and actuator roles to {}", claims.getBody().getSubject());
            return authenticationToken;
        } catch (SignatureException e) {
            //don't trust the JWT!
            LOG.error("ApiKey failed validation", e);
            throw new BadCredentialsException(String.format("ApiKey \"%s\" failed signature validation", apiKey));
        } catch (Exception e) {
            LOG.error("ApiKey validation failed due to error", e);
            throw new BadCredentialsException("Unable to validate supplied ApiKey", e);
        }
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.isAssignableFrom(PreAuthenticatedAuthenticationToken.class);
    }
}
