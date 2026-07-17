package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestCreateRequest;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestResponse;
import za.co.mawa.bes.entity.v2.FuneralServiceClaimEntity;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestType;
import za.co.mawa.bes.repository.v2.FuneralServiceClaimRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FuneralClaimSettlementService {
 private final JdbcTemplate jdbc;
 private final FuneralServiceClaimRepository links;
 private final PaymentRequestService payments;
 private final PaymentRequestFnbPaymentQueueService paymentQueue;

 @Transactional
 public PaymentRequestResponse settleApprovedClaim(String claimId, String actor) {
  FuneralServiceClaimEntity link=links.findByMembershipClaimId(claimId).orElse(null);
  if(link!=null && link.getServicePaymentRequestId()!=null) return payments.getById(link.getServicePaymentRequestId());
  Map<String,Object> claim=jdbc.queryForMap("SELECT claim_no,claim_type,claim_amount_cents,approved_amount_cents,status,funeral_service_id,funeral_provider_tenant_id FROM membership_claim WHERE id=?",claimId);
  String type=Objects.toString(claim.get("claim_type"),"");
  if(!Set.of("FUNERAL","COMBINATION").contains(type)) return null;
  String status=Objects.toString(claim.get("status"),"");
  if(!Set.of("APPROVED","PARTIALLY_APPROVED").contains(status)) return null;
  String providerTenant=link!=null && link.getProviderTenantId()!=null && !link.getProviderTenantId().isBlank()?link.getProviderTenantId():Objects.toString(claim.get("funeral_provider_tenant_id"),TenantContext.getCurrentTenant());
  String serviceId=link!=null && link.getFuneralServiceId()!=null && !link.getFuneralServiceId().isBlank()?link.getFuneralServiceId():Objects.toString(claim.get("funeral_service_id"),null);
  String fsi=qualified(providerTenant,"funeral_service_invoice"), invoice=qualified(providerTenant,"invoice");
  List<Map<String,Object>> invoices=jdbc.queryForList("SELECT fsi.invoice_id,fsi.partner_id,fsi.amount_cents,i.invoice_no FROM "+fsi+" fsi JOIN "+invoice+" i ON i.id=fsi.invoice_id WHERE fsi.funeral_service_id=? AND fsi.membership_claim_id=? ORDER BY fsi.created_at DESC LIMIT 1",serviceId,claimId);
  if(invoices.isEmpty()){ensureProviderInvoice(providerTenant,serviceId,claimId,claim);invoices=jdbc.queryForList("SELECT fsi.invoice_id,fsi.partner_id,fsi.amount_cents,i.invoice_no FROM "+fsi+" fsi JOIN "+invoice+" i ON i.id=fsi.invoice_id WHERE fsi.funeral_service_id=? AND fsi.membership_claim_id=? ORDER BY fsi.created_at DESC LIMIT 1",serviceId,claimId);}
  if(invoices.isEmpty()) throw new IllegalStateException("Unable to create or locate funeral service invoice");
  Map<String,Object> inv=invoices.get(0);
  long amount=claim.get("approved_amount_cents")==null?((Number)claim.get("claim_amount_cents")).longValue():((Number)claim.get("approved_amount_cents")).longValue();
  PaymentRequestCreateRequest r=new PaymentRequestCreateRequest();
  r.setRequestType(PaymentRequestType.FUNERAL_SERVICE_PAYMENT); r.setSourceType(PaymentRequestSourceType.MEMBERSHIP_CLAIM); r.setSourceId(claimId);
  r.setPayeePartnerId(Objects.toString(inv.get("partner_id"),null)); r.setPayeeName(resolveProviderPartnerName(providerTenant,r.getPayeePartnerId()));
  applyProviderBanking(providerTenant,r);
  r.setAmount(BigDecimal.valueOf(amount,2)); r.setCurrency("ZAR"); r.setInvoiceNo(Objects.toString(inv.get("invoice_no"),null));
  r.setExternalReference("FUNERAL-"+Objects.toString(claim.get("claim_no"),claimId)); r.setPaymentReason("FUNERAL-SERVICE-COVER");
  r.setRequestedPaymentDate(LocalDate.now()); r.setIdempotencyKey("FUNERAL-SERVICE-"+claimId);
  String effectiveActor=actor==null?"SYSTEM":actor;
  PaymentRequestResponse response=payments.create(r,effectiveActor);
  payments.markApproved(response.getId(),effectiveActor);
  paymentQueue.queueAfterApproval(response.getId(), response.getRequestNo(), effectiveActor);
  if(link!=null){link.setServiceInvoiceId(Objects.toString(inv.get("invoice_id"),null));link.setServicePaymentRequestId(response.getId());link.setProviderTenantId(providerTenant);link.setProviderPartnerId(r.getPayeePartnerId());links.save(link);}
  jdbc.update("UPDATE "+fsi+" SET payment_request_id=?,provider_tenant_id=?,cover_tenant_id=? WHERE invoice_id=?",response.getId(),providerTenant,TenantContext.getCurrentTenant(),inv.get("invoice_id"));
  return response;
 }
 private String qualified(String tenant,String table){if(tenant==null||!tenant.matches("[A-Za-z0-9_-]{1,128}"))throw new IllegalArgumentException("Invalid provider tenant");return "`"+tenant+"`.`"+table+"`";}
 private String resolveProviderPartnerName(String tenant,String id){if(id==null)return "Funeral service provider";List<String> n=jdbc.query("SELECT TRIM(CONCAT_WS(' ', NULLIF(name2,''), NULLIF(name3,''), NULLIF(name1,''))) FROM "+qualified(tenant,"partner")+" WHERE id=?",(rs,i)->rs.getString(1),id);return n.isEmpty()||n.get(0)==null||n.get(0).isBlank()?"Funeral service provider":n.get(0);}
 private void applyProviderBanking(String tenant,PaymentRequestCreateRequest r){if(r.getPayeePartnerId()==null)return;List<Map<String,Object>> rows=jdbc.queryForList("SELECT bank_name,account_holder,account_number,branch_code,account_type FROM "+qualified(tenant,"partner_bank_account")+" WHERE partner=? ORDER BY id LIMIT 1",r.getPayeePartnerId());if(rows.isEmpty())return;var b=rows.get(0);r.setBankName(Objects.toString(b.get("bank_name"),null));r.setAccountHolder(Objects.toString(b.get("account_holder"),r.getPayeeName()));r.setAccountNumber(Objects.toString(b.get("account_number"),null));r.setBranchCode(Objects.toString(b.get("branch_code"),null));r.setAccountType(Objects.toString(b.get("account_type"),null));}
 private void ensureProviderInvoice(String tenant,String serviceId,String claimId,Map<String,Object> claim){
  if(serviceId==null)throw new IllegalStateException("Funeral service reference is missing");String service=qualified(tenant,"funeral_service"),partner=qualified(tenant,"partner"),role=qualified(tenant,"partner_role"),invoice=qualified(tenant,"invoice"),fsi=qualified(tenant,"funeral_service_invoice");
  Map<String,Object> svc=jdbc.queryForMap("SELECT service_request_no,deceased_name,deceased_identity_number,funeral_date,family_rep_id FROM "+service+" WHERE id=?",serviceId);
  List<String> owners=jdbc.query("SELECT p.id FROM "+partner+" p JOIN "+role+" r ON r.partner=p.id WHERE r.role IN ('TENANT_OWNER','OWNER','FUNERAL_SERVICE_PROVIDER','SUPPLIER') ORDER BY FIELD(r.role,'FUNERAL_SERVICE_PROVIDER','TENANT_OWNER','OWNER','SUPPLIER') LIMIT 1",(rs,i)->rs.getString(1));String provider=owners.isEmpty()?Objects.toString(svc.get("family_rep_id"),null):owners.get(0);if(provider==null)throw new IllegalStateException("Funeral service provider partner is not configured");
  long amount=claim.get("approved_amount_cents")==null?((Number)claim.get("claim_amount_cents")).longValue():((Number)claim.get("approved_amount_cents")).longValue();String invoiceId=UUID.randomUUID().toString(),invoiceNo="FUN-"+System.currentTimeMillis();java.sql.Date today=java.sql.Date.valueOf(LocalDate.now());
  jdbc.update("INSERT INTO "+invoice+"(id,invoice_no,external_ref,source_type,source_id,partner_id,invoice_date,due_date,status,subtotal_cents,tax_cents,discount_cents,total_cents,paid_cents,balance_cents,currency,notes,created_at) VALUES(?,?,?,'FUNERAL_SERVICE',?,?,?,?, 'ISSUED',?,0,0,?,0,?,'ZAR',?,CURRENT_TIMESTAMP)",invoiceId,invoiceNo,Objects.toString(svc.get("service_request_no"),serviceId),serviceId,provider,today,today,amount,amount,amount,"Membership funeral cover settlement");
  String holderName=null,holderIdentity=null;
  try {
   Map<String,Object> h=jdbc.queryForMap("SELECT TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) holder_name, (SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END, pi.type, pi.value LIMIT 1) holder_identity FROM membership_claim c JOIN membership m ON m.id=c.membership_id JOIN partner p ON p.id=m.member_id WHERE c.id=?",claimId);
   holderName=Objects.toString(h.get("holder_name"),null);holderIdentity=Objects.toString(h.get("holder_identity"),null);
  } catch(Exception ignored) {}
  jdbc.update("INSERT INTO "+fsi+"(id,funeral_service_id,invoice_id,entity_type,partner_id,membership_claim_id,amount_cents,membership_holder_name,membership_holder_identity,deceased_name,deceased_identity,provider_tenant_id,cover_tenant_id,created_at) VALUES(?,?,?,'BURIAL_SOCIETY',?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID().toString(),serviceId,invoiceId,provider,claimId,amount,holderName,holderIdentity,svc.get("deceased_name"),svc.get("deceased_identity_number"),tenant,TenantContext.getCurrentTenant());
 }
}
