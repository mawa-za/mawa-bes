package za.co.raretag.mawabes.security.domain;

import javax.servlet.http.HttpServletRequest;

public interface SecurityDomain {

    public String getTenantIdFromJwt(HttpServletRequest request);
}