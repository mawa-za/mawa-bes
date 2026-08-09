package za.co.mawa.bes.service.v2;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.service.FieldOptionService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class EmploymentV2Service {
    private final EmploymentRepository employmentRepository;
    private final PartnerService partnerService;
    private final FieldOptionService fieldOptionService;
    private final EmploymentLifecycleService lifecycleService;

    public EmploymentV2Service(
            EmploymentRepository employmentRepository,
            PartnerService partnerService,
            FieldOptionService fieldOptionService,
            EmploymentLifecycleService lifecycleService) {
        this.employmentRepository = employmentRepository;
        this.partnerService = partnerService;
        this.fieldOptionService = fieldOptionService;
        this.lifecycleService = lifecycleService;
    }

    /** Lifecycle creation is intentionally blocked here; use EmploymentLifecycleService.requestHire. */
    public EmploymentDto hire(EmploymentCreateDto request) {
        throw new IllegalStateException("Hiring requires approval. Submit an employee hire request instead");
    }

    @Transactional(readOnly = true)
    public EmploymentDto get(String id) {
        return toDto(require(id));
    }

    @Transactional(readOnly = true)
    public List<EmploymentDto> search(EmploymentSearchDto search) {
        EmploymentSearchDto criteria = search == null ? new EmploymentSearchDto() : search;
        Specification<EmploymentEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.getStartDate() != null) predicates.add(cb.equal(root.get("startDate"), criteria.getStartDate()));
            if (criteria.getEndDate() != null) predicates.add(cb.equal(root.get("endDate"), criteria.getEndDate()));
            if (hasText(criteria.getPartnerId())) predicates.add(cb.equal(root.get("partnerId"), criteria.getPartnerId().trim()));
            if (hasText(criteria.getEmployeeNumber())) predicates.add(cb.like(cb.upper(root.get("employeeNumber")), "%" + criteria.getEmployeeNumber().trim().toUpperCase(Locale.ROOT) + "%"));
            if (hasText(criteria.getPosition())) predicates.add(cb.equal(root.get("position"), normalize(criteria.getPosition())));
            if (hasText(criteria.getBranch())) predicates.add(cb.equal(root.get("branch"), normalize(criteria.getBranch())));
            if (hasText(criteria.getDepartment())) predicates.add(cb.equal(root.get("department"), normalize(criteria.getDepartment())));
            if (hasText(criteria.getType())) predicates.add(cb.equal(root.get("type"), normalize(criteria.getType())));
            if (hasText(criteria.getStatus())) predicates.add(cb.equal(root.get("status"), normalize(criteria.getStatus())));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return employmentRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "startDate"))
                .stream().map(this::toDto).toList();
    }

    public EmploymentDto update(String id, EmploymentEditDto request) {
        return lifecycleService.updateEmploymentDetails(id, request);
    }

    public EmploymentDto terminate(String id) {
        throw new IllegalStateException("Termination requires approval. Submit an employee termination request instead");
    }

    public EmploymentDto suspend(String id) {
        throw new IllegalStateException("Suspension requires approval. Submit an employee suspension request instead");
    }

    public EmploymentDto rehire(String id, String startDate, String endDate) {
        throw new IllegalStateException("Rehire requires approval. Submit an employee rehire request instead");
    }

    public void delete(String id) {
        throw new IllegalStateException("Employment records cannot be deleted because employment history must be preserved");
    }

    @Transactional(readOnly = true)
    public List<PartnerDto> employees() {
        return employmentRepository.findAll().stream()
                .filter(entity -> Status.ACTIVE.equalsIgnoreCase(entity.getStatus()) || Status.SUSPENDED.equalsIgnoreCase(entity.getStatus()))
                .map(EmploymentEntity::getPartnerId).distinct().map(this::resolvePartner)
                .filter(partner -> partner != null).toList();
    }

    private EmploymentEntity require(String id) {
        if (!hasText(id)) throw new IllegalArgumentException("Employment id is required");
        return employmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Employment record not found: " + id));
    }

    private EmploymentDto toDto(EmploymentEntity entity) {
        EmploymentDto dto = new EmploymentDto();
        dto.setId(entity.getId());
        dto.setEmployeeNumber(entity.getEmployeeNumber());
        dto.setEmployee(resolvePartner(entity.getPartnerId()));
        dto.setType(resolveOption(Field.EMPLOYMENT_TYPE, entity.getType()));
        dto.setStartDate(Conversion.dateToString(entity.getStartDate()));
        dto.setEndDate(Conversion.dateToString(entity.getEndDate()));
        dto.setPosition(entity.getPosition());
        dto.setPositionDescription(fieldOptionService.getFieldOptionDescription(Field.EMPLOYMENT_POSITION, entity.getPosition()));
        dto.setStatus(entity.getStatus());
        dto.setBranch(resolveOption(Field.BRANCH, entity.getBranch()));
        dto.setDepartment(resolveOption(Field.DEPARTMENT, entity.getDepartment()));
        return dto;
    }

    private PartnerDto resolvePartner(String partnerId) {
        if (!hasText(partnerId)) return null;
        try { return partnerService.getOptional(partnerId); } catch (Exception ignored) { return null; }
    }

    private FieldOptionDto resolveOption(String field, String code) {
        if (!hasText(code)) return null;
        FieldOptionDto option = fieldOptionService.getFieldOption(field, code);
        if (option != null) return option;
        FieldOptionDto fallback = new FieldOptionDto();
        fallback.setField(field); fallback.setCode(code); fallback.setDescription(code.replace('-', ' '));
        return fallback;
    }

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
}
