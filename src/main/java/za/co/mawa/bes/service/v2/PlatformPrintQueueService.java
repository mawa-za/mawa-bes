package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.*;
import za.co.mawa.bes.service.PlatformPrintAgentClient;
import za.co.mawa.bes.service.TenantAdminService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service @RequiredArgsConstructor
public class PlatformPrintQueueService {
    private final PlatformPrintAgentClient agents;
    private final TenantAdminService tenants;
    private final TenantPrintQueueWorker worker;

    public void heartbeat(String agentId,String secret,HeartbeatRequest request,String remoteIp){ agents.authenticate(agentId,secret,request,remoteIp); }
    public PrintJobResponse claim(String agentId,String secret){
        agents.authenticate(agentId,secret);
        String old=TenantContext.getCurrentTenant(); String oldUrl=TenantContext.getCurrentTenantURL();
        try{
            for(TenantDto t:tenants.getAll()){
                if(!StringUtils.hasText(t.getId()) || (StringUtils.hasText(t.getStatus()) && !"ACTIVE".equalsIgnoreCase(t.getStatus()))) continue;
                try{
                    TenantContext.setCurrentTenant(t.getId()); TenantContext.setCurrentTenantURL(t.getHost());
                    PrintJobResponse j=worker.claim(agentId);
                    if(j!=null){ j.setId(encode(t.getId(),j.getId())); return j; }
                }catch(Exception ignored){ /* one unavailable tenant must not stop all printing */ }
                finally { TenantContext.clear(); }
            }
            return null;
        }finally{ if(StringUtils.hasText(old)){TenantContext.setCurrentTenant(old);TenantContext.setCurrentTenantURL(oldUrl);} }
    }
    public void spooled(String agentId,String secret,String brokerJobId,JobResultRequest r){ agents.authenticate(agentId,secret); Routed x=decode(brokerJobId); withTenant(x.tenant,()->worker.spooled(agentId,x.job,r)); }
    public void failed(String agentId,String secret,String brokerJobId,JobResultRequest r){ agents.authenticate(agentId,secret); Routed x=decode(brokerJobId); withTenant(x.tenant,()->worker.failed(agentId,x.job,r)); }
    private void withTenant(String tenant,Runnable action){String old=TenantContext.getCurrentTenant(),oldUrl=TenantContext.getCurrentTenantURL();try{TenantContext.setCurrentTenant(tenant);action.run();}finally{TenantContext.clear();if(StringUtils.hasText(old)){TenantContext.setCurrentTenant(old);TenantContext.setCurrentTenantURL(oldUrl);}}}
    private String encode(String tenant,String job){return Base64.getUrlEncoder().withoutPadding().encodeToString((tenant+"|"+job).getBytes(StandardCharsets.UTF_8));}
    private Routed decode(String value){try{String raw=new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8);int p=raw.indexOf('|');if(p<1)throw new Exception();return new Routed(raw.substring(0,p),raw.substring(p+1));}catch(Exception e){throw new IllegalArgumentException("Invalid platform print job id");}}
    private record Routed(String tenant,String job){}
}
