package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.*;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.repository.v2.*;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MembershipPartnerIdentityCorrectionService {
    private static final String SA_ID = "SA-ID";
    private final MembershipService membershipService;
    private final MembershipRepository membershipRepository;
    private final MembershipDependentRepository dependentRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerIdentityRepository identityRepository;
    private final ApprovalService approvalService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApprovalRequestResponse request(String membershipId,
            MembershipPartnerIdentityCorrectionRequest request, String actor) {
        String subjectType = clean(request.getSubjectType()).toUpperCase(Locale.ROOT);
        if (!Set.of("MEMBER", "DEPENDENT").contains(subjectType)) throw new IllegalArgumentException("Subject type must be MEMBER or DEPENDENT");
        String idNumber = normalizeAndValidateSaId(request.getIdentityNumber());
        if (clean(request.getReason()) == null) throw new IllegalArgumentException("A reason is required");
        MembershipEntity membership = membershipService.resolveMembership(membershipId);
        MembershipDependentEntity dependent = null;
        String currentPartnerId;
        if ("MEMBER".equals(subjectType)) {
            currentPartnerId = membership.getMemberId();
        } else {
            dependent = dependentRepository.findByIdAndMembershipId(clean(request.getDependentId()), membership.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Membership dependent not found"));
            currentPartnerId = dependent.getDependentPartnerId();
        }
        PartnerEntity currentPartner = requirePartner(currentPartnerId);
        Optional<PartnerIdentityEntity> ownerIdentity = identityRepository.findByNormalizedIdentity(SA_ID, idNumber);
        if (ownerIdentity.isPresent() && Objects.equals(ownerIdentity.get().getPartner(), currentPartnerId)) {
            throw new IllegalArgumentException("This SA-ID is already assigned to the current partner");
        }
        boolean override = Boolean.TRUE.equals(request.getOverrideExistingOwner());
        PartnerIdentityEntity currentSaId = identityRepository.findPartnerIdentityByTypeAndPartner(SA_ID, currentPartnerId);
        if (currentSaId != null && Objects.equals(currentSaId.getPartnerIdentityPK().getValue(), idNumber)) {
            throw new IllegalArgumentException("This SA-ID is already assigned to the current partner");
        }
        if (currentSaId != null && !override) throw new IllegalArgumentException("The current partner already has an SA-ID. Select override to replace it through approval.");
        String targetPartnerId = override ? currentPartnerId : ownerIdentity.map(PartnerIdentityEntity::getPartner).orElse(currentPartnerId);
        PartnerEntity targetPartner = requirePartner(targetPartnerId);
        String action = override && ownerIdentity.isPresent() && !Objects.equals(ownerIdentity.get().getPartner(), currentPartnerId)
                ? "REASSIGN_IDENTITY"
                : targetPartnerId.equals(currentPartnerId) ? "ASSIGN_IDENTITY" : "RELINK_PARTNER";
        validateRelationship(membership, dependent, subjectType, targetPartnerId);

        Integer pending = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM membership_partner_identity_correction WHERE membership_id=? AND subject_type=? AND COALESCE(dependent_id,'')=COALESCE(?,'') AND status='PENDING_APPROVAL'",
                Integer.class, membership.getId(), subjectType, dependent == null ? null : dependent.getId());
        if (pending != null && pending > 0) throw new IllegalStateException("An identity correction is already awaiting approval for this person");

        String requestId = UUID.randomUUID().toString();
        String actionBy = clean(actor) == null ? "SYSTEM" : actor.trim();
        jdbcTemplate.update("""
            INSERT INTO membership_partner_identity_correction(
              id,membership_id,subject_type,dependent_id,current_partner_id,target_partner_id,
              identity_type,identity_number,correction_action,status,reason,requested_by,created_at)
            VALUES(?,?,?,?,?,?,?, ?,?,'PENDING_APPROVAL',?,?,CURRENT_TIMESTAMP)
            """, requestId, membership.getId(), subjectType, dependent == null ? null : dependent.getId(),
                currentPartnerId, targetPartnerId, SA_ID, idNumber, action, request.getReason().trim(), actionBy);

        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("membershipId", membership.getId());
        payload.put("membershipNumber", membership.getMembershipNo());
        payload.put("subjectType", subjectType);
        payload.put("dependentId", dependent == null ? null : dependent.getId());
        payload.put("identityType", SA_ID);
        payload.put("identityNumber", idNumber);
        payload.put("correctionAction", action);
        payload.put("overrideExistingOwner", override);
        ownerIdentity.ifPresent(value -> payload.put("previousIdentityOwner", partnerSnapshot(requirePartner(value.getPartner()))));
        payload.put("currentPartner", partnerSnapshot(currentPartner));
        payload.put("proposedPartner", partnerSnapshot(targetPartner));
        payload.put("reason", request.getReason().trim());
        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.MEMBERSHIP_PARTNER_IDENTITY_CORRECTION);
        approval.setReferenceId(requestId);
        approval.setReferenceNo(membership.getMembershipNo() + "-IDENTITY");
        approval.setTitle("Correct " + subjectType.toLowerCase(Locale.ROOT) + " identity - " + membership.getMembershipNo());
        approval.setDescription("Review the current and proposed partner before applying this SA-ID correction.");
        approval.setRequesterId(actionBy);
        approval.setPayloadJson(toJson(payload));
        ApprovalRequestResponse response = approvalService.submitForApproval(approval);
        jdbcTemplate.update("UPDATE membership_partner_identity_correction SET approval_request_id=? WHERE id=?", response.getId(), requestId);
        return response;
    }

    @Transactional
    public void complete(String id, boolean approved, String actor, String completionStatus) {
        Map<String,Object> row = requireRequest(id);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(Objects.toString(row.get("status"), ""))) return;
        String actionBy = clean(actor) == null ? "SYSTEM" : actor.trim();
        if (!approved) { mark(id, completionStatus, actionBy); return; }
        String membershipId = Objects.toString(row.get("membership_id"), "");
        MembershipEntity membership = membershipService.resolveMembership(membershipId);
        String subjectType = Objects.toString(row.get("subject_type"), "");
        String currentPartnerId = Objects.toString(row.get("current_partner_id"), "");
        String targetPartnerId = Objects.toString(row.get("target_partner_id"), "");
        String idNumber = normalizeAndValidateSaId(Objects.toString(row.get("identity_number"), ""));
        MembershipDependentEntity dependent = null;
        if ("MEMBER".equals(subjectType)) {
            if (!Objects.equals(membership.getMemberId(), currentPartnerId)) throw new IllegalStateException("The membership member changed after this request was submitted");
        } else {
            dependent = dependentRepository.findByIdAndMembershipId(Objects.toString(row.get("dependent_id"), ""), membership.getId())
                    .orElseThrow(() -> new IllegalStateException("Membership dependent no longer exists"));
            if (!Objects.equals(dependent.getDependentPartnerId(), currentPartnerId)) throw new IllegalStateException("The dependent changed after this request was submitted");
        }
        validateRelationship(membership, dependent, subjectType, targetPartnerId);
        Optional<PartnerIdentityEntity> identity = identityRepository.findByNormalizedIdentity(SA_ID, idNumber);
        boolean reassignIdentity = "REASSIGN_IDENTITY".equalsIgnoreCase(Objects.toString(row.get("correction_action"), ""));
        if (!reassignIdentity && identity.isPresent() && !Objects.equals(identity.get().getPartner(), targetPartnerId)) throw new IllegalStateException("The SA-ID is now assigned to a different partner");
        if (reassignIdentity && identity.isPresent()) {
            PartnerIdentityEntity existingTargetIdentity = identityRepository.findPartnerIdentityByTypeAndPartner(SA_ID, targetPartnerId);
            if (existingTargetIdentity != null && !Objects.equals(existingTargetIdentity.getPartnerIdentityPK().getValue(), idNumber)) {
                identityRepository.delete(existingTargetIdentity);
                identityRepository.flush();
            }
            identity.get().setPartner(targetPartnerId);
            identityRepository.save(identity.get());
        }
        if (identity.isEmpty()) {
            if (identityRepository.findPartnerIdentityByTypeAndPartner(SA_ID, targetPartnerId) != null)
                throw new IllegalStateException("The target partner already has a different SA-ID");
            PartnerIdentityEntity created = new PartnerIdentityEntity();
            created.setPartnerIdentityPK(new PartnerIdentityPKEntity(idNumber, SA_ID));
            created.setPartner(targetPartnerId);
            created.setValidFrom(new Date());
            created.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            identityRepository.save(created);
        }
        if (!Objects.equals(currentPartnerId, targetPartnerId)) {
            if ("MEMBER".equals(subjectType)) { membership.setMemberId(targetPartnerId); membership.setUpdatedBy(actionBy); membershipRepository.save(membership); }
            else { dependent.setDependentPartnerId(targetPartnerId); dependent.setUpdatedBy(actionBy); dependentRepository.save(dependent); }
        }
        mark(id, completionStatus, actionBy);
    }

    private void validateRelationship(MembershipEntity membership, MembershipDependentEntity dependent, String subjectType, String target) {
        if ("MEMBER".equals(subjectType) && dependentRepository.existsVisibleByMembershipIdAndPartnerId(membership.getId(), target, Set.of(MembershipDependentStatus.ACTIVE, MembershipDependentStatus.DECEASED)))
            throw new IllegalArgumentException("The selected partner is already a dependent on this membership");
        if ("DEPENDENT".equals(subjectType) && Objects.equals(membership.getMemberId(), target))
            throw new IllegalArgumentException("The main member cannot also be linked as a dependent");
        if ("DEPENDENT".equals(subjectType) && dependentRepository.findByMembershipId(membership.getId()).stream().anyMatch(d -> !d.getId().equals(dependent.getId()) && Objects.equals(d.getDependentPartnerId(), target) && (d.getStatus()==MembershipDependentStatus.ACTIVE || d.getStatus()==MembershipDependentStatus.DECEASED)))
            throw new IllegalArgumentException("The selected partner is already linked as a dependent");
    }
    private String normalizeAndValidateSaId(String value) {
        String id = value == null ? "" : value.replaceAll("\\D", "");
        if (id.length()!=13) throw new IllegalArgumentException("A valid 13-digit SA-ID is required");
        int sum=0; boolean alternate=false;
        for(int i=12;i>=0;i--){ int n=id.charAt(i)-'0'; if(alternate){n*=2;if(n>9)n-=9;} sum+=n; alternate=!alternate; }
        if(sum%10!=0) throw new IllegalArgumentException("The SA-ID checksum is invalid");
        return id;
    }
    private PartnerEntity requirePartner(String id){ return partnerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Partner not found: "+id)); }
    private Map<String,Object> partnerSnapshot(PartnerEntity p){ Map<String,Object> m=new LinkedHashMap<>(); m.put("partnerId",p.getId());m.put("partnerNumber",p.getNo());m.put("name",String.join(" ", List.of(Objects.toString(p.getName2(),""),Objects.toString(p.getName1(),"")).stream().filter(s->!s.isBlank()).toList())); return m; }
    private Map<String,Object> requireRequest(String id){ List<Map<String,Object>> rows=jdbcTemplate.queryForList("SELECT * FROM membership_partner_identity_correction WHERE id=?",id);if(rows.isEmpty())throw new IllegalArgumentException("Identity correction request not found");return rows.get(0); }
    private void mark(String id,String status,String actor){jdbcTemplate.update("UPDATE membership_partner_identity_correction SET status=?,completed_by=?,completed_at=CURRENT_TIMESTAMP WHERE id=? AND status='PENDING_APPROVAL'",status,actor,id);}
    private String toJson(Object v){try{return objectMapper.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException("Unable to build approval details",e);}}
    private String clean(String v){return v==null||v.trim().isEmpty()?null:v.trim();}
}
