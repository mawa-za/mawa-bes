package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.manualreceipt.ManualReceiptBookRequest;
import za.co.mawa.bes.dto.v2.manualreceipt.ManualReceiptBookResponse;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.v2.ManualReceiptBookEntity;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.ManualReceiptBookRepository;
import za.co.mawa.bes.utils.Status;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ManualReceiptBookService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "CLOSED", "CANCELLED", "LOST");

    private final ManualReceiptBookRepository repository;
    private final EmploymentRepository employmentRepository;
    private final PartnerRepository partnerRepository;
    private final ReferenceDataValidationService referenceDataValidationService;

    @Transactional(readOnly = true)
    public List<ManualReceiptBookResponse> list(boolean activeOnly) {
        List<ManualReceiptBookEntity> books = activeOnly
                ? repository.findByActiveTrueOrderByReceiptBookNoAsc()
                : repository.findAllByOrderByReceiptBookNoAsc();
        LocalDate today = LocalDate.now();
        return books.stream()
                .filter(book -> !activeOnly
                        || ("ACTIVE".equalsIgnoreCase(book.getStatus())
                        && (book.getEffectiveFrom() == null || !book.getEffectiveFrom().isAfter(today))
                        && (book.getEffectiveTo() == null || !book.getEffectiveTo().isBefore(today))))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ManualReceiptBookResponse get(String id) {
        return toResponse(require(id));
    }

    @Transactional
    public ManualReceiptBookResponse create(ManualReceiptBookRequest request, String actor) {
        if (request == null) throw new IllegalArgumentException("Manual receipt book is required");
        String bookNo = normalizeBookNo(request.getReceiptBookNo());
        if (repository.existsByReceiptBookNoIgnoreCase(bookNo)) {
            throw new IllegalStateException("Manual receipt book already exists: " + bookNo);
        }
        ManualReceiptBookEntity entity = new ManualReceiptBookEntity();
        entity.setReceiptBookNo(bookNo);
        apply(entity, request, actor, true);
        entity.setCreatedBy(clean(actor));
        return toResponse(repository.save(entity));
    }

    @Transactional
    public ManualReceiptBookResponse update(String id, ManualReceiptBookRequest request, String actor) {
        if (request == null) throw new IllegalArgumentException("Manual receipt book update is required");
        ManualReceiptBookEntity entity = require(id);
        if (StringUtils.hasText(request.getReceiptBookNo())
                && !entity.getReceiptBookNo().equalsIgnoreCase(request.getReceiptBookNo().trim())) {
            throw new IllegalArgumentException("Receipt book number cannot be changed after creation");
        }
        apply(entity, request, actor, false);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public ManualReceiptBookResponse deactivate(String id, String actor) {
        ManualReceiptBookEntity entity = require(id);
        entity.setActive(false);
        entity.setStatus("CLOSED");
        entity.setEffectiveTo(entity.getEffectiveTo() == null ? LocalDate.now() : entity.getEffectiveTo());
        entity.setUpdatedBy(clean(actor));
        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public ManualReceiptBookEntity requireActiveBookForReceipt(String receiptBookNo, String receiptNo) {
        ManualReceiptBookEntity book = requireActiveBook(receiptBookNo);
        BigInteger number = parseNumber(receiptNo, "manualReceiptNo");
        validateWithinConfiguredRange(book, number, "manualReceiptNo");
        return book;
    }

    @Transactional(readOnly = true)
    public ManualReceiptBookEntity requireActiveBookForRange(String receiptBookNo, String fromReceiptNo, String toReceiptNo) {
        ManualReceiptBookEntity book = requireActiveBook(receiptBookNo);
        BigInteger from = parseNumber(fromReceiptNo, "receiptFromNo");
        BigInteger to = parseNumber(toReceiptNo, "receiptToNo");
        if (from.compareTo(to) > 0) throw new IllegalArgumentException("receiptFromNo cannot be greater than receiptToNo");
        validateWithinConfiguredRange(book, from, "receiptFromNo");
        validateWithinConfiguredRange(book, to, "receiptToNo");
        return book;
    }

    @Transactional(readOnly = true)
    public EmployeeReference requireActiveEmployee(String employeePartnerId) {
        String employeeId = requireText(employeePartnerId, "Original collector/cashier is required");
        if (!employmentRepository.existsByPartnerIdAndStatus(employeeId, Status.ACTIVE)) {
            throw new IllegalArgumentException("Original collector/cashier must be an active employee");
        }
        PartnerEntity partner = partnerRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee was not found: " + employeeId));
        return new EmployeeReference(partner.getId(), displayName(partner));
    }

    @Transactional(readOnly = true)
    public AreaReference requireSalesArea(String areaCode) {
        String code = referenceDataValidationService.requireOption(
                "SALES-AREA", areaCode, "Location/branch");
        String name = referenceDataValidationService.description("SALES-AREA", code);
        return new AreaReference(code, name == null ? code : name);
    }

    @Transactional(readOnly = true)
    public BookUsageReference validateBookUsage(
            ManualReceiptBookEntity book,
            String employeePartnerId,
            String areaCode) {
        if (book == null) throw new IllegalArgumentException("Manual receipt book is required");
        EmployeeReference employee = requireActiveEmployee(employeePartnerId);
        AreaReference area = requireSalesArea(areaCode);
        if (StringUtils.hasText(book.getAssignedEmployeeId())
                && !book.getAssignedEmployeeId().equals(employee.id())) {
            throw new IllegalArgumentException(
                    "Manual receipt book " + book.getReceiptBookNo()
                            + " is assigned to a different employee");
        }
        if (StringUtils.hasText(book.getAssignedAreaCode())
                && !book.getAssignedAreaCode().equalsIgnoreCase(area.code())) {
            throw new IllegalArgumentException(
                    "Manual receipt book " + book.getReceiptBookNo()
                            + " is assigned to a different SALES-AREA");
        }
        return new BookUsageReference(employee, area);
    }

    private ManualReceiptBookEntity requireActiveBook(String receiptBookNo) {
        String bookNo = normalizeBookNo(receiptBookNo);
        ManualReceiptBookEntity book = repository.findByReceiptBookNoIgnoreCase(bookNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Manual receipt book does not exist: " + bookNo));
        LocalDate today = LocalDate.now();
        if (!Boolean.TRUE.equals(book.getActive()) || !"ACTIVE".equalsIgnoreCase(book.getStatus())) {
            throw new IllegalStateException("Manual receipt book is not active: " + bookNo);
        }
        if (book.getEffectiveFrom() != null && book.getEffectiveFrom().isAfter(today)) {
            throw new IllegalStateException("Manual receipt book is not yet effective: " + bookNo);
        }
        if (book.getEffectiveTo() != null && book.getEffectiveTo().isBefore(today)) {
            throw new IllegalStateException("Manual receipt book has expired: " + bookNo);
        }
        return book;
    }

    private void apply(ManualReceiptBookEntity entity, ManualReceiptBookRequest request, String actor, boolean creating) {
        entity.setDescription(clean(request.getDescription()));
        entity.setReceiptFromNo(normalizeOptionalNumber(request.getReceiptFromNo(), "receiptFromNo"));
        entity.setReceiptToNo(normalizeOptionalNumber(request.getReceiptToNo(), "receiptToNo"));
        validateRange(entity.getReceiptFromNo(), entity.getReceiptToNo());

        String employeeId = clean(request.getAssignedEmployeeId());
        if (employeeId != null) requireActiveEmployee(employeeId);
        entity.setAssignedEmployeeId(employeeId);

        String areaCode = clean(request.getAssignedAreaCode());
        if (areaCode != null) areaCode = requireSalesArea(areaCode).code();
        entity.setAssignedAreaCode(areaCode);

        String status = clean(request.getStatus());
        if (status == null) status = creating ? "ACTIVE" : entity.getStatus();
        status = status.toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) throw new IllegalArgumentException("Invalid receipt book status: " + status);
        entity.setStatus(status);

        Boolean active = request.getActive();
        if (active == null) active = creating ? Boolean.TRUE : entity.getActive();
        if (!"ACTIVE".equals(status)) active = false;
        entity.setActive(active);

        if (request.getEffectiveFrom() != null) entity.setEffectiveFrom(request.getEffectiveFrom());
        else if (creating && entity.getEffectiveFrom() == null) entity.setEffectiveFrom(LocalDate.now());
        entity.setEffectiveTo(request.getEffectiveTo());
        if (entity.getEffectiveTo() != null && entity.getEffectiveFrom() != null
                && entity.getEffectiveTo().isBefore(entity.getEffectiveFrom())) {
            throw new IllegalArgumentException("Effective-to date cannot be before effective-from date");
        }
        entity.setNotes(clean(request.getNotes()));
        entity.setUpdatedBy(clean(actor));
    }

    private ManualReceiptBookResponse toResponse(ManualReceiptBookEntity entity) {
        String employeeName = null;
        if (StringUtils.hasText(entity.getAssignedEmployeeId())) {
            employeeName = partnerRepository.findById(entity.getAssignedEmployeeId())
                    .map(this::displayName).orElse(entity.getAssignedEmployeeId());
        }
        String areaName = null;
        if (StringUtils.hasText(entity.getAssignedAreaCode())) {
            areaName = referenceDataValidationService.description("SALES-AREA", entity.getAssignedAreaCode());
        }
        return ManualReceiptBookResponse.builder()
                .id(entity.getId())
                .receiptBookNo(entity.getReceiptBookNo())
                .description(entity.getDescription())
                .receiptFromNo(entity.getReceiptFromNo())
                .receiptToNo(entity.getReceiptToNo())
                .assignedEmployeeId(entity.getAssignedEmployeeId())
                .assignedEmployeeName(employeeName)
                .assignedAreaCode(entity.getAssignedAreaCode())
                .assignedAreaName(areaName)
                .status(entity.getStatus())
                .active(entity.getActive())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private ManualReceiptBookEntity require(String id) {
        return repository.findById(requireText(id, "Manual receipt book id is required"))
                .orElseThrow(() -> new IllegalArgumentException("Manual receipt book was not found: " + id));
    }

    private void validateWithinConfiguredRange(ManualReceiptBookEntity book, BigInteger number, String label) {
        if (StringUtils.hasText(book.getReceiptFromNo())
                && number.compareTo(new BigInteger(book.getReceiptFromNo())) < 0) {
            throw new IllegalArgumentException(label + " is below the configured range for receipt book " + book.getReceiptBookNo());
        }
        if (StringUtils.hasText(book.getReceiptToNo())
                && number.compareTo(new BigInteger(book.getReceiptToNo())) > 0) {
            throw new IllegalArgumentException(label + " is above the configured range for receipt book " + book.getReceiptBookNo());
        }
    }

    private void validateRange(String from, String to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Both receiptFromNo and receiptToNo must be supplied together");
        }
        if (from != null && new BigInteger(from).compareTo(new BigInteger(to)) > 0) {
            throw new IllegalArgumentException("receiptFromNo cannot be greater than receiptToNo");
        }
    }

    private String normalizeOptionalNumber(String value, String label) {
        if (!StringUtils.hasText(value)) return null;
        return parseNumber(value, label).toString();
    }

    private BigInteger parseNumber(String value, String label) {
        String normalized = requireText(value, label + " is required");
        if (!normalized.matches("\\d+")) throw new IllegalArgumentException(label + " must contain digits only");
        return new BigInteger(normalized);
    }

    private String normalizeBookNo(String value) {
        return requireText(value, "Receipt book number is required").toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String displayName(PartnerEntity partner) {
        return java.util.stream.Stream.of(partner.getName2(), partner.getName3(), partner.getName1())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse(partner.getNo() == null ? partner.getId() : partner.getNo());
    }

    public record EmployeeReference(String id, String name) {}
    public record AreaReference(String code, String name) {}
    public record BookUsageReference(EmployeeReference employee, AreaReference area) {}
}
