package za.co.mawa.bes.configuration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import za.co.mawa.bes.service.UserAccessService;
import java.util.Map;

@Component
public class TestUserTransactionGuardInterceptor implements HandlerInterceptor {
    @Autowired private UserAccessService accessService;
    private final ObjectMapper mapper=new ObjectMapper();
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) throws Exception {
        if("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(request.getServletPath())) return true;
        try{ accessService.validateCurrentSession(); }
        catch(SecurityException ex){ return deny(response,"ACCESS_EXPIRED",ex.getMessage()); }
        if(isExternalMutation(request) && accessService.externalTransactionsBlocked()){
            accessService.audit("EXTERNAL_TRANSACTION_BLOCKED","ENDPOINT",request.getServletPath(),"Test-user policy",request.getMethod());
            return deny(response,"TEST_USER_EXTERNAL_TRANSACTION_BLOCKED","Testing users cannot execute this external transaction in the current environment.");
        }
        return true;
    }
    private boolean isExternalMutation(HttpServletRequest r){
        if("GET".equalsIgnoreCase(r.getMethod())||"HEAD".equalsIgnoreCase(r.getMethod())) return false;
        String p=r.getServletPath().toLowerCase();
        return p.contains("/xero")||p.contains("/fnb")||p.contains("bank-payment")||p.contains("bank-report")
                ||p.contains("submit-to-bank")||p.contains("/secret")||p.contains("supplier-disbursement")||p.contains("refund-execute");
    }
    private boolean isPublic(String p){return p==null||p.contains("authenticate")||p.contains("refresh-token")||p.startsWith("/internal/admin/")||p.equals("/v2/admin-handoff/exchange")||p.startsWith("/swagger")||p.startsWith("/v3/api-docs");}
    private boolean deny(HttpServletResponse response,String code,String message)throws Exception{
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(mapper.writeValueAsString(Map.of("code",code,"message",message))); return false;
    }
}
