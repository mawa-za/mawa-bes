package za.co.raretag.mawabes.configuration.security.domain;

import javax.servlet.http.HttpServletRequest;

public interface SecurityDomain {

    public String getTenantIdFromJwt(HttpServletRequest request);
}