package za.co.raretag.mawabes.configuration.security.domain;


import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import javax.servlet.http.HttpServletRequest;
import za.co.raretag.mawabes.configuration.security.model.JwtClaim;

import java.text.ParseException;
import java.util.Optional;

public class JwtDecoder {
    private static final String AUTHORIZATION = "Authorization";
    private static final String JWT_TOKEN_LABEL = "token";
    private static final String TOKEN_PREFIX = "Bearer";
    private final String jwtToken;

    public JwtDecoder(HttpServletRequest request) {
        this.jwtToken = Optional.ofNullable(request)
                .map(req -> req.getHeader(AUTHORIZATION))
                .filter(headerWithToken -> headerWithToken.contains(TOKEN_PREFIX))
                .map(headerWithToken -> headerWithToken.substring(headerWithToken.indexOf(TOKEN_PREFIX)+TOKEN_PREFIX.length()+1))
                .map(token -> token.trim())
                .orElseThrow(() -> {
                    return new CredentialsException("Missing Authentication Token");
                });
    }


    public String getJwtParameter(JwtClaim jwtClaim) {
        return Optional.ofNullable(getSignedJWT())
                .map(this::getJWTClaimsSet)
                .map(JWTClaimsSet::getClaims)
                .map(stringObjectMap -> stringObjectMap.get(jwtClaim.getValue()))
                .map(Object::toString)
                .orElse(null);
    }

    private SignedJWT getSignedJWT() {
        try {
            return SignedJWT.parse(jwtToken);
        } catch (ParseException e) {
            throw new CredentialsException("Cannot parse jwt token");
        }

    }

    private JWTClaimsSet getJWTClaimsSet(SignedJWT signedJWT) {
        try {
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new CredentialsException("Cannot extract Claim Set from jwt token");
        }
    }

}