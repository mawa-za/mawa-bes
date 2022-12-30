package za.co.raretag.mawabes.security.domain;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import za.co.raretag.mawabes.security.model.JwtClaim;

import java.util.Optional;

@Component
class SecurityDomainImpl implements SecurityDomain {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    public String getTenantIdFromJwt(HttpServletRequest request) {
        return Optional.ofNullable(request)
                .map(JwtDecoder::new)
                .map(jwtDecoder -> jwtDecoder.getJwtParameter(JwtClaim.TOKEN_ID))
                .orElse(null);
    }

}