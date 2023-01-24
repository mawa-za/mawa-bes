package za.co.mawa.bes.configuration.security.domain;

import jakarta.servlet.http.HttpServletRequest;

public interface SecurityDomain {

    public String getTenantIdFromJwt(HttpServletRequest request);
}