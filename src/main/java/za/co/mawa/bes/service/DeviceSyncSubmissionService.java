package za.co.mawa.bes.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import za.co.mawa.bes.dto.v2.devicesync.*;
import za.co.mawa.bes.entity.DeviceSyncSubmissionEntity;
import za.co.mawa.bes.repository.DeviceSyncSubmissionRepository;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class DeviceSyncSubmissionService {
 private final DeviceSyncSubmissionRepository repository; private final ObjectMapper mapper;
 @Value("${device-sync.internal-base-url:http://127.0.0.1:${server.port:8080}}") private String internalBaseUrl;
 private final RestTemplate rest = new RestTemplate();

 @Transactional
 public DeviceSyncSubmissionDto submit(DeviceSyncSubmitRequest request, String userId) {
  validate(request);
  String key=request.getIdempotencyKey().trim();
  return repository.findByIdempotencyKey(key).map(this::dto).orElseGet(() -> {
   LocalDateTime now=LocalDateTime.now();
   DeviceSyncSubmissionEntity e=DeviceSyncSubmissionEntity.builder().submissionId(blank(request.getSubmissionId())?UUID.randomUUID().toString():request.getSubmissionId().trim()).idempotencyKey(key).deviceId(request.getDeviceId()).submittedBy(userId).httpMethod(request.getMethod().toUpperCase(Locale.ROOT)).targetPath(request.getPath()).requestPayload(json(request.getPayload())).status("RECEIVED").attemptCount(0).createdAt(now).updatedAt(now).build();
   return dto(repository.save(e));
  });
 }

 public DeviceSyncSubmissionDto get(String id){ return dto(find(id)); }

 public Page<DeviceSyncSubmissionDto> list(String status, String search, int page, int size) {
  Specification<DeviceSyncSubmissionEntity> spec=Specification.where(null);
  if(!blank(status) && !"ALL".equalsIgnoreCase(status)) spec=spec.and((root,q,cb)->cb.equal(root.get("status"),status.toUpperCase(Locale.ROOT)));
  if(!blank(search)){String term="%"+search.trim().toLowerCase(Locale.ROOT)+"%"; spec=spec.and((root,q,cb)->cb.or(cb.like(cb.lower(root.get("submissionId")),term),cb.like(cb.lower(root.get("idempotencyKey")),term),cb.like(cb.lower(root.get("deviceId")),term),cb.like(cb.lower(root.get("submittedBy")),term),cb.like(cb.lower(root.get("targetPath")),term),cb.like(cb.lower(root.get("errorMessage")),term)));}
  Pageable pageable=PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),200),Sort.by(Sort.Direction.DESC,"createdAt"));
  return repository.findAll(spec,pageable).map(this::dto);
 }

 @Transactional
 public DeviceSyncSubmissionDto process(String id, HttpHeaders incoming) {
  DeviceSyncSubmissionEntity e=find(id);
  if ("COMPLETED".equals(e.getStatus())) return dto(e);
  e.setStatus("PROCESSING"); e.setAttemptCount(e.getAttemptCount()+1); e.setUpdatedAt(LocalDateTime.now()); repository.save(e);
  try {
   HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
   copy(incoming, headers, HttpHeaders.AUTHORIZATION); copy(incoming, headers, "X-TenantID"); copy(incoming, headers, "X-Tenant-Id"); copy(incoming, headers, "X-UserID"); copy(incoming, headers, "X-User-Id");
   Object body=blank(e.getRequestPayload())?null:mapper.readValue(e.getRequestPayload(), Object.class);
   ResponseEntity<String> response=rest.exchange(internalBaseUrl+normalize(e.getTargetPath()), HttpMethod.valueOf(e.getHttpMethod()), new HttpEntity<>(body, headers), String.class);
   e.setResponseStatus(response.getStatusCode().value()); e.setResponsePayload(response.getBody()); e.setStatus("COMPLETED"); e.setErrorMessage(null); e.setProcessedAt(LocalDateTime.now());
  } catch (HttpStatusCodeException ex) {
   e.setResponseStatus(ex.getStatusCode().value()); e.setResponsePayload(ex.getResponseBodyAsString()); e.setErrorMessage(extractMessage(ex.getResponseBodyAsString(), ex.getMessage())); e.setStatus(ex.getStatusCode().is4xxClientError()?"CORRECTION_REQUIRED":"PROCESSING_FAILED");
  } catch (Exception ex) { e.setErrorMessage(ex.getMessage()); e.setStatus("PROCESSING_FAILED"); }
  e.setUpdatedAt(LocalDateTime.now()); return dto(repository.save(e));
 }

 @Transactional
 public DeviceSyncSubmissionDto correct(String id, DeviceSyncCorrectionRequest request, String userId) {
  DeviceSyncSubmissionEntity e=find(id); e.setRequestPayload(json(request.getPayload())); e.setResponsePayload(null); e.setResponseStatus(null); e.setErrorMessage(null); e.setStatus("RECEIVED"); e.setSubmittedBy(userId); e.setUpdatedAt(LocalDateTime.now()); e.setProcessedAt(null); return dto(repository.save(e));
 }
 private DeviceSyncSubmissionEntity find(String id){return repository.findBySubmissionId(id).orElseThrow(()->new IllegalArgumentException("Device sync submission not found: "+id));}
 private void validate(DeviceSyncSubmitRequest r){if(r==null||blank(r.getIdempotencyKey())||blank(r.getMethod())||blank(r.getPath()))throw new IllegalArgumentException("idempotencyKey, method and path are required"); if(!(r.getPath().startsWith("/v2/") || r.getPath().startsWith("/pay-app/")))throw new IllegalArgumentException("Only supported device transaction endpoints may be queued"); if(Set.of("/v2/device-sync/submissions").stream().anyMatch(r.getPath()::startsWith))throw new IllegalArgumentException("Device sync endpoint cannot queue itself"); if(!Set.of("POST","PUT","PATCH","DELETE").contains(r.getMethod().toUpperCase(Locale.ROOT)))throw new IllegalArgumentException("Unsupported queued method");}
 private DeviceSyncSubmissionDto dto(DeviceSyncSubmissionEntity e){return DeviceSyncSubmissionDto.builder().submissionId(e.getSubmissionId()).idempotencyKey(e.getIdempotencyKey()).deviceId(e.getDeviceId()).submittedBy(e.getSubmittedBy()).method(e.getHttpMethod()).path(e.getTargetPath()).requestPayload(parse(e.getRequestPayload())).responsePayload(parse(e.getResponsePayload())).responseStatus(e.getResponseStatus()).status(e.getStatus()).attemptCount(e.getAttemptCount()).errorMessage(e.getErrorMessage()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).processedAt(e.getProcessedAt()).build();}
 private Object parse(String s){if(blank(s))return null;try{return mapper.readValue(s,Object.class);}catch(Exception x){return s;}}
 private String json(Object o){try{return o==null?null:mapper.writeValueAsString(o);}catch(JsonProcessingException e){throw new IllegalArgumentException("Invalid sync payload",e);}}
 private String extractMessage(String body,String fallback){Object o=parse(body);if(o instanceof Map<?,?> m && m.get("message")!=null)return m.get("message").toString();return fallback;}
 private void copy(HttpHeaders from,HttpHeaders to,String key){String v=from.getFirst(key);if(!blank(v))to.set(key,v);} private String normalize(String p){return p.startsWith("/")?p:"/"+p;} private boolean blank(String s){return s==null||s.isBlank();}
}
