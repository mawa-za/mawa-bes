package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.creditnote.CreditNoteIssueRequestDto;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.funeral.FuneralPackageAmendmentRequest;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.v2.*;
import za.co.mawa.bes.service.CreditNoteService;
import za.co.mawa.bes.service.v2.NumberAllocationService;
import za.co.mawa.bes.xero.XeroInvoiceQueueService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Post-finalisation amendments preserve original FSR, invoices, paid cover and claim values. */
@Service
@RequiredArgsConstructor
public class FuneralPackageAmendmentService {
    private final FuneralServiceRepository funeralServices;
    private final FuneralPackageRepository packages;
    private final FuneralPackageItemRepository items;
    private final FuneralPackageAmendmentRepository amendments;
    private final FuneralServiceInvoiceRepository invoiceLinks;
    private final InvoiceRepository invoices;
    private final ApprovalService approvals;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final CreditNoteService creditNotes;
    private final NumberAllocationService numberAllocation;
    private final FuneralResourcePlanningService resources;
    private final XeroInvoiceQueueService xeroQueue;

    @Transactional(readOnly = true)
    public List<FuneralPackageAmendmentEntity> history(String serviceId) {
        if (!funeralServices.existsById(serviceId)) throw new IllegalArgumentException("FSR not found");
        return amendments.findByFuneralServiceIdOrderByCreatedAtDesc(serviceId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(String serviceId, String newPackageId) {
        FuneralServiceEntity service = service(serviceId);
        requireFinalised(service);
        FuneralPackageEntity next = packageById(newPackageId);
        long existingExtras = extrasTotal(service.getExtrasJson());
        long revised = Math.addExact(value(next.getBasePriceCents()), existingExtras);
        if (revised < 0) throw new IllegalArgumentException("Revised funeral price cannot be negative");
        long original = value(service.getTotalAmountCents());
        long covered = coverageInvoiced(serviceId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceRequestNo", service.getServiceRequestNo());
        // Use the immutable final-invoice snapshot where available. Older FSRs only have
        // a catalogue-based fallback and should be reviewed for historical item accuracy.
        result.put("oldPackage", StringUtils.hasText(service.getPackageSnapshotJson())
                ? decodeSnapshot(service.getPackageSnapshotJson()) : packageSnapshot(service.getPackageId()));
        result.put("historicPackageSnapshotAvailable", StringUtils.hasText(service.getPackageSnapshotJson()));
        result.put("newPackage", packageSnapshot(next.getId()));
        result.put("currentTotalCents", original);
        result.put("revisedTotalCents", revised);
        result.put("differenceCents", revised - original);
        result.put("invoicedCoverCents", covered);
        result.put("familyDifferenceCents", revised - original);
        result.put("requiresFundingReconciliation", revised < covered);
        result.put("extrasPreserved", true);
        return result;
    }

    @Transactional
    public FuneralPackageAmendmentEntity request(String serviceId, FuneralPackageAmendmentRequest request) {
        if (request == null || !StringUtils.hasText(request.getReason())
                || !StringUtils.hasText(request.getNewPackageId())) {
            throw new IllegalArgumentException("A replacement package and reason are required");
        }
        if (!Boolean.TRUE.equals(request.getCustomerAcknowledged())) {
            throw new IllegalArgumentException("Customer or family representative acknowledgement is required");
        }
        // Serialise submissions per funeral service; a second request cannot use a stale price.
        lockService(serviceId);
        FuneralServiceEntity service = service(serviceId);
        requireFinalised(service);
        if (service.getFuneralDate() != null && service.getFuneralDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("The funeral date has passed. Use a controlled financial correction "
                    + "rather than retrospectively replacing the service package.");
        }
        requireNoPending(serviceId);
        if (Objects.equals(service.getPackageId(), request.getNewPackageId())) {
            throw new IllegalArgumentException("Choose a different funeral package");
        }
        Map<String, Object> proposal = preview(serviceId, request.getNewPackageId());
        FuneralPackageAmendmentEntity amendment = new FuneralPackageAmendmentEntity();
        amendment.setId(UUID.randomUUID().toString());
        amendment.setFuneralServiceId(serviceId);
        amendment.setAmendmentNo(service.getServiceRequestNo() + "-AMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        amendment.setOldPackageId(service.getPackageId());
        amendment.setNewPackageId(request.getNewPackageId());
        amendment.setOldPackageSnapshot(encode(proposal.get("oldPackage")));
        amendment.setNewPackageSnapshot(encode(proposal.get("newPackage")));
        amendment.setOldTotalCents((Long) proposal.get("currentTotalCents"));
        amendment.setNewTotalCents((Long) proposal.get("revisedTotalCents"));
        amendment.setReason(request.getReason().trim());
        amendment.setCustomerAcknowledged(true);
        amendment.setStatus("PENDING_APPROVAL");
        amendment.setRequestedBy(requireUser());
        amendments.saveAndFlush(amendment);

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.FUNERAL_PACKAGE_AMENDMENT);
        approval.setReferenceId(amendment.getId());
        approval.setReferenceNo(amendment.getAmendmentNo());
        approval.setTitle("Funeral package change - " + service.getServiceRequestNo());
        approval.setDescription("From " + service.getPackageId() + " to " + request.getNewPackageId()
                + ", price difference (cents): " + ((Long) proposal.get("differenceCents"))
                + ". Reason: " + amendment.getReason());
        approval.setRequesterId(amendment.getRequestedBy());
        approval.setPayloadJson(encode(proposal));
        amendment.setApprovalRequestId(approvals.submitForMandatoryApproval(approval).getId());
        return amendments.save(amendment);
    }

    /** Approval callback; does not change the original FSR until reconciliation has passed. */
    @Transactional
    public void approved(String amendmentId, String actor) {
        FuneralPackageAmendmentEntity amendment = locked(amendmentId);
        if (!"PENDING_APPROVAL".equals(amendment.getStatus())) return;
        amendment.setApprovedBy(actor);
        amendment.setApprovedAt(LocalDateTime.now());
        amendment.setStatus("APPROVED");
        amendments.saveAndFlush(amendment);
        applyApproved(amendment);
    }

    @Transactional
    public void rejected(String amendmentId, boolean cancelled) {
        FuneralPackageAmendmentEntity amendment = locked(amendmentId);
        if (!"PENDING_APPROVAL".equals(amendment.getStatus())) return;
        amendment.setStatus(cancelled ? "CANCELLED" : "REJECTED");
        amendments.save(amendment);
    }

    /** Retry after authorised financial/resource reconciliation without seeking a second approval. */
    @Transactional
    public FuneralPackageAmendmentEntity apply(String amendmentId) {
        FuneralPackageAmendmentEntity amendment = locked(amendmentId);
        if (!List.of("APPROVED", "RECONCILIATION_REQUIRED").contains(amendment.getStatus())
                || amendment.getApprovedAt() == null) {
            throw new IllegalArgumentException("Only approved amendments can be applied");
        }
        applyApproved(amendment);
        return amendments.save(amendment);
    }

    private void applyApproved(FuneralPackageAmendmentEntity amendment) {
        lockService(amendment.getFuneralServiceId());
        FuneralServiceEntity service = service(amendment.getFuneralServiceId());
        requireFinalised(service);
        if (!Objects.equals(service.getPackageId(), amendment.getOldPackageId())
                || value(service.getTotalAmountCents()) != value(amendment.getOldTotalCents())) {
            amendment.setStatus("RECONCILIATION_REQUIRED");
            amendment.setReconciliationNote("Arrangement changed since amendment was requested; review required");
            amendments.save(amendment);
            return;
        }
        long change = Math.subtractExact(amendment.getNewTotalCents(), amendment.getOldTotalCents());
        List<FuneralServiceInvoiceEntity> links = invoiceLinks.findByFuneralServiceId(service.getId());
        List<FuneralServiceInvoiceEntity> family = links.stream()
                .filter(link -> "FAMILY_REP".equalsIgnoreCase(link.getEntityType()))
                .sorted(Comparator.comparing(FuneralServiceInvoiceEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        long cover = links.stream().filter(link -> !"FAMILY_REP".equalsIgnoreCase(link.getEntityType()))
                .mapToLong(link -> value(link.getAmountCents())).sum();
        String blockingReason = null;
        if (service.getFuneralDate() != null && service.getFuneralDate().isBefore(LocalDate.now())) {
            blockingReason = "Funeral date has passed; confirm any delivered services and process financial correction";
        } else if (amendment.getNewTotalCents() < cover) {
            blockingReason = "Approved or invoiced cover exceeds the revised funeral total; reconcile funding first";
        } else if (change > 0 && !StringUtils.hasText(service.getFamilyRepId())) {
            blockingReason = "Family representative must be linked to a customer before an adjustment invoice can be issued";
        } else if (change < 0 && availableFamilyCredit(family) < -change) {
            blockingReason = "Family invoice has insufficient creditable value; reconcile existing payments and invoices";
        } else if (obsoleteCommittedResources(service.getId(), amendment.getNewPackageId())) {
            blockingReason = "Existing committed resources are not in the new package; resolve supplier and resource commitments";
        }
        if (blockingReason != null) {
            amendment.setStatus("RECONCILIATION_REQUIRED");
            amendment.setReconciliationNote(blockingReason);
            amendments.save(amendment);
            return;
        }

        List<Map<String, String>> adjustmentDocs = new ArrayList<>();
        if (change > 0) {
            adjustmentDocs.add(createAdditionalInvoice(service, amendment, change));
        } else if (change < 0) {
            long remaining = -change;
            for (FuneralServiceInvoiceEntity link : family) {
                if (remaining == 0) break;
                InvoiceEntity invoice = invoices.findById(link.getInvoiceId()).orElse(null);
                if (invoice == null) continue;
                long available = Math.max(0, value(invoice.getTotalCents()) - value(invoice.getCreditedCents()));
                long credit = Math.min(remaining, available);
                if (credit <= 0) continue;
                CreditNoteIssueRequestDto request = new CreditNoteIssueRequestDto();
                request.setAmountCents(credit);
                request.setReason("Funeral package amendment " + amendment.getAmendmentNo());
                Map<String, Object> issued = creditNotes.issue(invoice.getId(), request, amendment.getApprovedBy());
                adjustmentDocs.add(Map.of("type", "CREDIT_NOTE", "id", String.valueOf(issued.get("id")),
                        "invoiceId", invoice.getId()));
                remaining -= credit;
            }
            if (remaining > 0) throw new IllegalStateException("Insufficient family invoice credit");
        }
        // Mark only obsolete, unallocated requirements as cancelled; committed resources were checked above.
        cancelUnplannedObsoleteResources(service.getId(), amendment.getNewPackageId());
        service.setPackageId(amendment.getNewPackageId());
        service.setPackageSnapshotJson(amendment.getNewPackageSnapshot());
        service.setTotalAmountCents(amendment.getNewTotalCents());
        funeralServices.saveAndFlush(service);
        resources.ensurePlan(service.getId(), amendment.getApprovedBy());
        amendment.setAdjustmentDocumentsJson(encode(adjustmentDocs));
        amendment.setReconciliationNote(change < 0
                ? "Credit issued; review any family overpayment for a separate customer refund"
                : null);
        amendment.setStatus("APPLIED");
        amendment.setAppliedAt(LocalDateTime.now());
        amendments.save(amendment);
    }

    private Map<String, String> createAdditionalInvoice(FuneralServiceEntity service,
            FuneralPackageAmendmentEntity amendment, long amount) {
        String invoiceNo = numberAllocation.allocateNumber("INVOICE");
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNo(invoiceNo);
        invoice.setSourceType("FUNERAL_PACKAGE_AMENDMENT");
        invoice.setSourceId(amendment.getId());
        invoice.setExternalRef(service.getServiceRequestNo());
        invoice.setPartnerId(service.getFamilyRepId());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(service.getFuneralDate());
        invoice.setStatus("ISSUED");
        invoice.setSubtotalCents(amount);
        invoice.setTotalCents(amount);
        invoice.setTaxCents(0L);
        invoice.setDiscountCents(0L);
        invoice.setPaidCents(0L);
        invoice.setCreditedCents(0L);
        invoice.setBalanceCents(amount);
        invoice.setCurrency("ZAR");
        invoice.setNotes("Additional funeral package charge: " + amendment.getAmendmentNo());
        invoice = invoices.saveAndFlush(invoice);
        jdbc.update("""
                INSERT INTO invoice_line(id, invoice_id, description, quantity, show_amount,
                  unit_price_cents, discount_cents, tax_cents, subtotal_cents, total_cents, created_at)
                VALUES (?, ?, ?, 1, 1, ?, 0, 0, ?, ?, CURRENT_TIMESTAMP)
                """, UUID.randomUUID().toString(), invoice.getId(),
                "Funeral package upgrade - " + amendment.getAmendmentNo(), amount, amount, amount);
        FuneralServiceInvoiceEntity link = new FuneralServiceInvoiceEntity();
        link.setFuneralServiceId(service.getId());
        link.setInvoiceId(invoice.getId());
        link.setEntityType("FAMILY_REP");
        link.setPartnerId(service.getFamilyRepId());
        link.setAmountCents(amount);
        invoiceLinks.save(link);
        xeroQueue.queueInvoiceIfEnabled(invoice);
        return Map.of("type", "INVOICE", "id", invoice.getId(), "invoiceNo", invoiceNo);
    }

    private long availableFamilyCredit(List<FuneralServiceInvoiceEntity> family) {
        return family.stream().map(link -> invoices.findById(link.getInvoiceId()).orElse(null))
                .filter(Objects::nonNull)
                .mapToLong(invoice -> Math.max(0, value(invoice.getTotalCents()) - value(invoice.getCreditedCents())))
                .sum();
    }

    private boolean obsoleteCommittedResources(String serviceId, String newPackageId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM funeral_resource_plan_item i
            JOIN funeral_resource_plan rp ON rp.id = i.resource_plan_id
            WHERE rp.funeral_service_id=? AND i.source_requirement_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM funeral_resource_requirement r
                   WHERE r.id=i.source_requirement_id AND r.funeral_package_id=? AND r.active=1)
              AND i.status NOT IN ('UNPLANNED', 'CANCELLED')
            """, Integer.class, serviceId, newPackageId);
        return count != null && count > 0;
    }

    private void cancelUnplannedObsoleteResources(String serviceId, String newPackageId) {
        jdbc.update("""
            UPDATE funeral_resource_plan_item i
            JOIN funeral_resource_plan rp ON rp.id = i.resource_plan_id
            SET i.status = 'CANCELLED'
            WHERE rp.funeral_service_id=? AND i.source_requirement_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM funeral_resource_requirement r
                    WHERE r.id=i.source_requirement_id AND r.funeral_package_id=? AND r.active=1)
              AND i.status='UNPLANNED'
            """, serviceId, newPackageId);
    }

    private Map<String, Object> packageSnapshot(String packageId) {
        FuneralPackageEntity pkg = packages.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Previous package not found"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", pkg.getId()); result.put("name", pkg.getName());
        result.put("pricingMode", pkg.getPricingMode());
        result.put("basePriceCents", pkg.getBasePriceCents());
        result.put("inclusionsJson", pkg.getInclusionsJson());
        result.put("products", items.findByFuneralPackageIdOrderByProductDescriptionAsc(pkg.getId()).stream()
                .map(item -> Map.of("description", item.getProductDescription(),
                        "quantity", item.getQuantity(), "unitPriceCents", item.getUnitPriceCents())).toList());
        return result;
    }

    private long extrasTotal(String extrasJson) {
        if (!StringUtils.hasText(extrasJson)) return 0;
        try {
            List<Map<String, Object>> extras = json.readValue(extrasJson, new TypeReference<>() {});
            long total = 0;
            for (Map<String, Object> extra : extras) {
                Object amount = extra.get("amountCents");
                if (amount instanceof Number number) total = Math.addExact(total, number.longValue());
            }
            return total;
        } catch (Exception ex) { throw new IllegalStateException("Invalid saved funeral extras", ex); }
    }

    private long coverageInvoiced(String serviceId) {
        return invoiceLinks.findByFuneralServiceId(serviceId).stream()
                .filter(link -> !"FAMILY_REP".equalsIgnoreCase(link.getEntityType()))
                .mapToLong(link -> value(link.getAmountCents())).sum();
    }

    private void requireNoPending(String serviceId) {
        boolean open = amendments.findByFuneralServiceIdOrderByCreatedAtDesc(serviceId).stream()
                .anyMatch(a -> List.of("PENDING_APPROVAL", "APPROVED", "RECONCILIATION_REQUIRED").contains(a.getStatus()));
        if (open) throw new IllegalStateException("Resolve the existing funeral package amendment first");
    }
    private FuneralPackageEntity packageById(String id) {
        FuneralPackageEntity pkg = packages.findById(id).orElseThrow(() -> new IllegalArgumentException("Package not found"));
        if (!Boolean.TRUE.equals(pkg.getActive())) throw new IllegalArgumentException("Selected package is not active");
        return pkg;
    }
    private FuneralServiceEntity service(String id) {
        return funeralServices.findById(id).orElseThrow(() -> new IllegalArgumentException("FSR not found"));
    }
    private void requireFinalised(FuneralServiceEntity service) {
        if (!"INVOICED".equalsIgnoreCase(service.getStatus()))
            throw new IllegalStateException("Only finalised funeral arrangements can be amended");
    }
    private void lockService(String id) {
        if (jdbc.queryForList("SELECT id FROM funeral_service WHERE id = ? FOR UPDATE", String.class, id).isEmpty())
            throw new IllegalArgumentException("FSR not found");
    }
    private FuneralPackageAmendmentEntity locked(String id) {
        return amendments.findWithLockById(id).orElseThrow(() -> new IllegalArgumentException("Amendment not found"));
    }
    private String requireUser() {
        String user = UserContext.getCurrentUserId();
        if (!StringUtils.hasText(user)) throw new IllegalStateException("Authenticated user is required");
        return user;
    }
    private Object decodeSnapshot(String raw) {
        try { return json.readValue(raw, new TypeReference<Map<String, Object>>() {}); }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored funeral package snapshot is invalid", e);
        }
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot save amendment snapshot", e); }
    }
    private long value(Long value) { return value == null ? 0L : value; }
}
