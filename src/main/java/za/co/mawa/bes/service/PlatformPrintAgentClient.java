package za.co.mawa.bes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.AgentEnrollRequest;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.AgentEnrollResponse;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.AgentResponse;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.HeartbeatRequest;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

@Component
public class PlatformPrintAgentClient {
    @Value("${mawa.admin.api.url}") private String adminUrl;
    @Value("${mawa.internal.service-token:}") private String internalToken;
    private final ObjectMapper mapper=new ObjectMapper();

    public AgentEnrollResponse register(String name,String location,AgentEnrollRequest request,String remoteIp){
        Map<String,Object> body=new LinkedHashMap<>(); body.put("name",name);body.put("location",location);body.put("machineName",request.getMachineName());body.put("osName",request.getOsName());body.put("osVersion",request.getOsVersion());body.put("agentVersion",request.getAgentVersion());body.put("remoteIp",remoteIp);
        Map<String,Object> r=call("POST","/internal/erp/platform-print-agents/register",body,null,new TypeReference<>(){});
        return AgentEnrollResponse.builder().agentId(text(r,"id")).agentSecret(text(r,"agentSecret")).agentName(text(r,"name")).location((String)r.get("location")).build();
    }
    public void authenticate(String id,String secret,HeartbeatRequest h,String remoteIp){ Map<String,Object>b=new LinkedHashMap<>();if(h!=null){b.put("machineName",h.getMachineName());b.put("osName",h.getOsName());b.put("osVersion",h.getOsVersion());b.put("agentVersion",h.getAgentVersion());}b.put("remoteIp",remoteIp);call("POST","/internal/erp/platform-print-agents/"+id+"/authenticate",b,secret,new TypeReference<Map<String,Object>>(){}); }
    public void authenticate(String id,String secret){ authenticate(id,secret,null,null); }
    public List<AgentResponse> list(){ List<Map<String,Object>> rows=call("GET","/internal/erp/platform-print-agents",null,null,new TypeReference<>(){}); List<AgentResponse> out=new ArrayList<>(); for(Map<String,Object>r:rows){AgentResponse a=AgentResponse.builder().id(text(r,"id")).name(text(r,"name")).machineName(nullable(r,"machineName")).location(nullable(r,"location")).status(text(r,"status")).online(Boolean.TRUE.equals(r.get("online"))).agentVersion(nullable(r,"agentVersion")).printers(List.of()).build();out.add(a);}return out; }
    public void revoke(String id){call("POST","/internal/erp/platform-print-agents/"+id+"/revoke",Map.of(),null,new TypeReference<Map<String,Object>>(){});}
    private <T>T call(String method,String path,Object body,String secret,TypeReference<T> type){ if(!StringUtils.hasText(internalToken))throw new IllegalStateException("mawa.internal.service-token is not configured"); try{HttpURLConnection c=(HttpURLConnection)new URL(trim(adminUrl)+path).openConnection();c.setRequestMethod(method);c.setConnectTimeout(5000);c.setReadTimeout(15000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("X-Mawa-Internal-Token",internalToken);if(secret!=null)c.setRequestProperty("X-Mawa-Agent-Secret",secret);if(body!=null){byte[]p=mapper.writeValueAsBytes(body);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setFixedLengthStreamingMode(p.length);try(OutputStream o=c.getOutputStream()){o.write(p);}}int s=c.getResponseCode();String response=read(s>=200&&s<300?c.getInputStream():c.getErrorStream());if(s<200||s>=300)throw new IllegalStateException("Platform print-agent API returned HTTP "+s+": "+response);if(type.getType().getTypeName().contains("Map")&&response.isBlank())return (T)new LinkedHashMap<>();return mapper.readValue(response,type);}catch(Exception e){if(e instanceof RuntimeException re)throw re;throw new IllegalStateException("Platform print-agent API failed",e);} }
    private String read(InputStream in)throws IOException{if(in==null)return"";return new String(in.readAllBytes(),StandardCharsets.UTF_8);}private String trim(String v){return v.endsWith("/")?v.substring(0,v.length()-1):v;}private String text(Map<String,Object>m,String k){String v=nullable(m,k);if(!StringUtils.hasText(v))throw new IllegalStateException("Missing "+k);return v;}private String nullable(Map<String,Object>m,String k){Object v=m.get(k);return v==null?null:v.toString();}
}
