package za.co.mawa.bes.service.v2;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.EmploymentCreateDto;
import za.co.mawa.bes.dto.EmploymentDto;
import za.co.mawa.bes.dto.EmploymentEditDto;
import za.co.mawa.bes.dto.EmploymentSearchDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.service.FieldOptionService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class EmploymentV2Service {
    private final EmploymentRepository employmentRepository;
    private final PartnerService partnerService;
    private final FieldOptionService fieldOptionService;

    public EmploymentV2Service(
            EmploymentRepository employmentRepository,
            PartnerService partnerService,
            FieldOptionService fieldOptionService) {
        this.employmentRepository = employmentRepository;
        this.partnerService = partnerService;
        this.fieldOptionService = fieldOptionService;
    }

    @Transactional
    public EmploymentDto hire(EmploymentCreateDto request) {
        validateHire(request);
        String partnerId = request.getPartnerId().trim();
        if (employmentRepository.existsByPartnerIdAndStatus(partnerId, Status.ACTIVE)) {
            throw new IllegalStateException("Partner already has an active employment record");
        }

        EmploymentEntity entity = new EmploymentEntity();
        entity.setPartnerId(partnerId);
        entity.setEmployeeNumber(trimToNull(request.getEmployeeNumber()));
        entity.setType(validateOption(Field.EMPLOYMENT_TYPE, request.getType(), "Employment type", true));
        entity.setStartDate(hasText(request.getStartDate()) ? parseDate(request.getStartDate(), "Start date") : new Date());
        entity.setEndDate(hasText(request.getEndDate()) ? parseDate(request.getEndDate(), "End date") : parseDate("9999-12-31", "End date"));
        validateDateRange(entity.getStartDate(), entity.getEndDate());
        entity.setPosition(trimToNull(request.getPosition()));
        entity.setBranch(validateOption(Field.BRANCH, request.getBranch(), "Branch", false));
        entity.setDepartment(validateOption(Field.DEPARTMENT, request.getDepartment(), "Department", false));
        entity.setStatus(Status.ACTIVE);
        entity = employmentRepository.save(entity);
        ensureEmployeeRole(partnerId);
        return toDto(entity);
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
            if (hasText(criteria.getEmployeeNumber())) predicates.add(cb.equal(root.get("employeeNumber"), criteria.getEmployeeNumber().trim()));
            if (hasText(criteria.getPosition())) predicates.add(cb.equal(root.get("position"), criteria.getPosition().trim()));
            if (hasText(criteria.getBranch())) predicates.add(cb.equal(root.get("branch"), normalize(criteria.getBranch())));
            if (hasText(criteria.getDepartment())) predicates.add(cb.equal(root.get("department"), normalize(criteria.getDepartment())));
            if (hasText(criteria.getType())) predicates.add(cb.equal(root.get("type"), normalize(criteria.getType())));
            if (hasText(criteria.getStatus())) predicates.add(cb.equal(root.get("status"), normalize(criteria.getStatus())));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return employmentRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "startDate"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EmploymentDto update(String id, EmploymentEditDto request) {
        if (request == null) throw new IllegalArgumentException("Employment update is required");
        EmploymentEntity entity = require(id);
        if (hasText(request.getEmployeeNumber())) entity.setEmployeeNumber(request.getEmployeeNumber().trim());
        if (hasText(request.getType())) entity.setType(validateOption(Field.EMPLOYMENT_TYPE, request.getType(), "Employment type", true));
        if (hasText(request.getStartDate())) entity.setStartDate(parseDate(request.getStartDate(), "Start date"));
        if (hasText(request.getEndDate())) entity.setEndDate(parseDate(request.getEndDate(), "End date"));
        if (hasText(request.getPosition())) entity.setPosition(request.getPosition().trim());
        if (hasText(request.getBranch())) entity.setBranch(validateOption(Field.BRANCH, request.getBranch(), "Branch", false));
        if (hasText(request.getDepartment())) entity.setDepartment(validateOption(Field.DEPARTMENT, request.getDepartment(), "Department", false));
        validateDateRange(entity.getStartDate(), entity.getEndDate());
        return toDto(employmentRepository.save(entity));
    }

    @Transactional
    public EmploymentDto terminate(String id) {
        EmploymentEntity entity = require(id);
        entity.setStatus(Status.TERMINATED);
        entity.setEndDate(new Date());
        entity = employmentRepository.save(entity);
        if (!employmentRepository.existsByPartnerIdAndStatus(entity.getPartnerId(), Status.ACTIVE)) {
            removeEmployeeRole(entity.getPartnerId());
        }
        return toDto(entity);
    }

    @Transactional
    public EmploymentDto suspend(String id) {
        EmploymentEntity entity = require(id);
        entity.setStatus(Status.SUSPENDED);
        entity.setEndDate(new Date());
        return toDto(employmentRepository.save(entity));
    }

    @Transactional
    public EmploymentDto rehire(String id, String startDate, String endDate) {
        EmploymentEntity entity = require(id);
        entity.setStatus(Status.ACTIVE);
        entity.setStartDate(hasText(startDate) ? parseDate(startDate, "Start date") : new Date());
        entity.setEndDate(hasText(endDate) ? parseDate(endDate, "End date") : parseDate("9999-12-31", "End date"));
        validateDateRange(entity.getStartDate(), entity.getEndDate());
        entity = employmentRepository.save(entity);
        ensureEmployeeRole(entity.getPartnerId());
        return toDto(entity);
    }

    @Transactional
    public void delete(String id) {
        EmploymentEntity entity = require(id);
        String partnerId = entity.getPartnerId();
        employmentRepository.delete(entity);
        if (!employmentRepository.existsByPartnerIdAndStatus(partnerId, Status.ACTIVE)) {
            removeEmployeeRole(partnerId);
        }
    }

    @Transactional(readOnly = true)
    public List<PartnerDto> employees() {
        return employmentRepository.findAll().stream()
                .filter(entity -> Status.ACTIVE.equalsIgnoreCase(entity.getStatus()))
                .map(EmploymentEntity::getPartnerId)
                .distinct()
                .map(this::resolvePartner)
                .filter(partner -> partner != null)
                .toList();
    }

    private EmploymentEntity require(String id) {
        if (!hasText(id)) throw new IllegalArgumentException("Employment id is required");
        return employmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Employment record not found: " + id));
    }

    private void validateHire(EmploymentCreateDto request) {
        if (request == null) throw new IllegalArgumentException("Employment request is required");
        if (!hasText(request.getPartnerId())) throw new IllegalArgumentException("Partner is required");
        if (!hasText(request.getType())) throw new IllegalArgumentException("Employment type is required");
        try {
            partnerService.getOptional(request.getPartnerId().trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Partner not found: " + request.getPartnerId());
        }
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
        dto.setStatus(entity.getStatus());
        dto.setBranch(resolveOption(Field.BRANCH, entity.getBranch()));
        dto.setDepartment(resolveOption(Field.DEPARTMENT, entity.getDepartment()));
        return dto;
    }

    private PartnerDto resolvePartner(String partnerId) {
        if (!hasText(partnerId)) return null;
        try {
            return partnerService.getOptional(partnerId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private FieldOptionDto resolveOption(String field, String code) {
        if (!hasText(code)) return null;
        FieldOptionDto option = fieldOptionService.getFieldOption(field, code);
        if (option != null) return option;
        FieldOptionDto fallback = new FieldOptionDto();
        fallback.setField(field);
        fallback.setCode(code);
        fallback.setDescription(code.replace('-', ' '));
        return fallback;
    }

    private Date parseDate(String value, String label) {
        Date parsed = Conversion.stringToDate(value == null ? null : value.trim());
        if (parsed == null) throw new IllegalArgumentException(label + " must use yyyy-MM-dd format");
        return parsed;
    }

    private void validateDateRange(Date startDate, Date endDate) {
        if (startDate != null && endDate != null && endDate.before(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    private String validateOption(String field, String value, String label, boolean required) {
        if (!hasText(value)) {
            if (required) throw new IllegalArgumentException(label + " is required");
            return null;
        }
        String code = normalize(value);
        if (fieldOptionService.getFieldOption(field, code) == null) {
            throw new IllegalArgumentException("Invalid " + field + " option: " + value);
        }
        return code;
    }

    private void ensureEmployeeRole(String partnerId) {
        try {
            boolean exists = partnerService.getRoles(partnerId).stream()
                    .anyMatch(role -> RoleType.EMPLOYEE.equalsIgnoreCase(role));
            if (!exists) partnerService.addRole(partnerId, RoleType.EMPLOYEE);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to assign EMPLOYEE partner role", exception);
        }
    }

    private void removeEmployeeRole(String partnerId) {
        try {
            boolean exists = partnerService.getRoles(partnerId).stream()
                    .anyMatch(role -> RoleType.EMPLOYEE.equalsIgnoreCase(role));
            if (exists) partnerService.removeRole(partnerId, RoleType.EMPLOYEE);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to remove EMPLOYEE partner role", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
