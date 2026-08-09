package za.co.mawa.bes.service.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.v2.*;
import za.co.mawa.bes.service.PartnerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LeaveConfigurationService {
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final LeaveTypeRepository leaveTypeRepository;
    private final WorkingCalendarRepository calendarRepository;
    private final WorkingCalendarHolidayRepository holidayRepository;
    private final LeaveProfileRepository profileRepository;
    private final LeaveProfileRuleRepository ruleRepository;
    private final EmploymentLeaveProfileAssignmentRepository employeeAssignmentRepository;
    private final PositionLeaveProfileAssignmentRepository positionAssignmentRepository;
    private final EmploymentRepository employmentRepository;
    private final PartnerService partnerService;

    public LeaveConfigurationService(
            LeaveTypeRepository leaveTypeRepository,
            WorkingCalendarRepository calendarRepository,
            WorkingCalendarHolidayRepository holidayRepository,
            LeaveProfileRepository profileRepository,
            LeaveProfileRuleRepository ruleRepository,
            EmploymentLeaveProfileAssignmentRepository employeeAssignmentRepository,
            PositionLeaveProfileAssignmentRepository positionAssignmentRepository,
            EmploymentRepository employmentRepository,
            PartnerService partnerService) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.calendarRepository = calendarRepository;
        this.holidayRepository = holidayRepository;
        this.profileRepository = profileRepository;
        this.ruleRepository = ruleRepository;
        this.employeeAssignmentRepository = employeeAssignmentRepository;
        this.positionAssignmentRepository = positionAssignmentRepository;
        this.employmentRepository = employmentRepository;
        this.partnerService = partnerService;
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> listLeaveTypes(Boolean activeOnly) {
        LocalDate today = LocalDate.now();
        List<LeaveTypeEntity> entities = Boolean.TRUE.equals(activeOnly)
                ? leaveTypeRepository.findByActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqualOrderByDisplayOrderAscNameAsc(today, today)
                : leaveTypeRepository.findAllByOrderByDisplayOrderAscNameAsc();
        return entities.stream().map(this::toDto).toList();
    }

    @Transactional
    public LeaveTypeDto saveLeaveType(String id, LeaveTypeDto request) {
        if (request == null) throw new IllegalArgumentException("Leave type is required");
        String code = normalizeCode(request.getCode(), "Leave type code");
        LeaveTypeEntity entity = hasText(id)
                ? leaveTypeRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Leave type not found: " + id))
                : new LeaveTypeEntity();
        leaveTypeRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), entity.getId())) {
                throw new IllegalArgumentException("Leave type code already exists: " + code);
            }
        });
        entity.setCode(code);
        entity.setName(required(request.getName(), "Leave type name", 150));
        entity.setDescription(trim(request.getDescription(), 500));
        entity.setPaid(defaultBoolean(request.getPaid(), true));
        entity.setUnit(normalizeUnit(request.getUnit()));
        entity.setAllowHalfDay(defaultBoolean(request.getAllowHalfDay(), true));
        entity.setRequiresSupportingDocument(defaultBoolean(request.getRequiresSupportingDocument(), false));
        entity.setDocumentRequiredAfter(nonNegative(request.getDocumentRequiredAfter(), "Document threshold"));
        entity.setMinimumRequest(nonNegative(defaultDecimal(request.getMinimumRequest(), new BigDecimal("0.50")), "Minimum request"));
        entity.setMaximumConsecutive(nonNegative(request.getMaximumConsecutive(), "Maximum consecutive amount"));
        entity.setAllowNegativeBalance(defaultBoolean(request.getAllowNegativeBalance(), false));
        entity.setIncludeWeekends(defaultBoolean(request.getIncludeWeekends(), false));
        entity.setIncludePublicHolidays(defaultBoolean(request.getIncludePublicHolidays(), false));
        entity.setRequiresApproval(defaultBoolean(request.getRequiresApproval(), true));
        entity.setActiveFrom(defaultDate(request.getActiveFrom(), LocalDate.now()));
        entity.setActiveTo(defaultDate(request.getActiveTo(), MAX_DATE));
        validateRange(entity.getActiveFrom(), entity.getActiveTo(), "Leave type");
        entity.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        entity.setColour(trim(request.getColour(), 20));
        entity.setIcon(trim(request.getIcon(), 50));
        entity.setActive(defaultBoolean(request.getActive(), true));
        entity.setCreatedBy(entity.getId() == null ? actor() : entity.getCreatedBy());
        entity.setUpdatedBy(actor());
        if (entity.getVersion() == null) entity.setVersion(0L);
        return toDto(leaveTypeRepository.save(entity));
    }

    @Transactional
    public void deactivateLeaveType(String id) {
        LeaveTypeEntity entity = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Leave type not found: " + id));
        entity.setActive(false);
        entity.setActiveTo(LocalDate.now());
        entity.setUpdatedBy(actor());
        leaveTypeRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<WorkingCalendarDto> listCalendars() {
        return calendarRepository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public WorkingCalendarDto saveCalendar(String id, WorkingCalendarDto request) {
        if (request == null) throw new IllegalArgumentException("Working calendar is required");
        String code = normalizeCode(request.getCode(), "Calendar code");
        WorkingCalendarEntity entity = hasText(id)
                ? calendarRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Working calendar not found: " + id))
                : new WorkingCalendarEntity();
        String entityId = entity.getId();
        calendarRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), entityId)) throw new IllegalArgumentException("Calendar code already exists: " + code);
        });
        entity.setCode(code);
        entity.setName(required(request.getName(), "Calendar name", 150));
        entity.setDescription(trim(request.getDescription(), 500));
        entity.setMondayWorking(defaultBoolean(request.getMondayWorking(), true));
        entity.setTuesdayWorking(defaultBoolean(request.getTuesdayWorking(), true));
        entity.setWednesdayWorking(defaultBoolean(request.getWednesdayWorking(), true));
        entity.setThursdayWorking(defaultBoolean(request.getThursdayWorking(), true));
        entity.setFridayWorking(defaultBoolean(request.getFridayWorking(), true));
        entity.setSaturdayWorking(defaultBoolean(request.getSaturdayWorking(), false));
        entity.setSundayWorking(defaultBoolean(request.getSundayWorking(), false));
        entity.setHoursPerDay(positive(defaultDecimal(request.getHoursPerDay(), new BigDecimal("8.00")), "Hours per day"));
        entity.setActive(defaultBoolean(request.getActive(), true));
        entity.setCreatedBy(entity.getId() == null ? actor() : entity.getCreatedBy());
        entity.setUpdatedBy(actor());
        if (entity.getVersion() == null) entity.setVersion(0L);
        entity = calendarRepository.save(entity);
        syncHolidays(entity.getId(), request.getHolidays());
        return toDto(entity);
    }

    private void syncHolidays(String calendarId, List<WorkingCalendarHolidayDto> requests) {
        if (requests == null) return;
        Map<String, WorkingCalendarHolidayEntity> existing = holidayRepository
                .findByWorkingCalendarIdOrderByHolidayDateAsc(calendarId).stream()
                .collect(Collectors.toMap(WorkingCalendarHolidayEntity::getId, Function.identity()));
        Set<String> retained = new HashSet<>();
        for (WorkingCalendarHolidayDto request : requests) {
            if (request.getHolidayDate() == null) throw new IllegalArgumentException("Holiday date is required");
            WorkingCalendarHolidayEntity entity = hasText(request.getId())
                    ? Optional.ofNullable(existing.get(request.getId())).orElseThrow(() -> new IllegalArgumentException("Holiday does not belong to this calendar"))
                    : new WorkingCalendarHolidayEntity();
            entity.setWorkingCalendarId(calendarId);
            entity.setHolidayDate(request.getHolidayDate());
            entity.setName(required(request.getName(), "Holiday name", 150));
            entity.setRecurringAnnual(defaultBoolean(request.getRecurringAnnual(), false));
            entity.setActive(defaultBoolean(request.getActive(), true));
            entity.setCreatedBy(entity.getId() == null ? actor() : entity.getCreatedBy());
            entity = holidayRepository.save(entity);
            retained.add(entity.getId());
        }
        existing.values().stream().filter(item -> !retained.contains(item.getId())).forEach(item -> {
            item.setActive(false);
            holidayRepository.save(item);
        });
    }

    @Transactional(readOnly = true)
    public List<LeaveProfileDto> listProfiles() {
        return profileRepository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public LeaveProfileDto saveProfile(String id, LeaveProfileDto request) {
        if (request == null) throw new IllegalArgumentException("Leave profile is required");
        String code = normalizeCode(request.getCode(), "Profile code");
        LeaveProfileEntity entity = hasText(id)
                ? profileRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Leave profile not found: " + id))
                : new LeaveProfileEntity();
        String entityId = entity.getId();
        profileRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), entityId)) throw new IllegalArgumentException("Leave profile code already exists: " + code);
        });
        WorkingCalendarEntity calendar = calendarRepository.findById(required(request.getWorkingCalendarId(), "Working calendar", 255))
                .orElseThrow(() -> new IllegalArgumentException("Working calendar not found"));
        boolean defaultProfile = defaultBoolean(request.getDefaultProfile(), false);
        if (defaultProfile) {
            profileRepository.findAll().stream().filter(item -> !Objects.equals(item.getId(), entityId) && Boolean.TRUE.equals(item.getDefaultProfile()))
                    .forEach(item -> { item.setDefaultProfile(false); item.setUpdatedBy(actor()); profileRepository.save(item); });
        }
        entity.setCode(code);
        entity.setName(required(request.getName(), "Profile name", 150));
        entity.setDescription(trim(request.getDescription(), 500));
        entity.setWorkingCalendarId(calendar.getId());
        entity.setDefaultProfile(defaultProfile);
        entity.setActiveFrom(defaultDate(request.getActiveFrom(), LocalDate.now()));
        entity.setActiveTo(defaultDate(request.getActiveTo(), MAX_DATE));
        validateRange(entity.getActiveFrom(), entity.getActiveTo(), "Leave profile");
        entity.setActive(defaultBoolean(request.getActive(), true));
        entity.setCreatedBy(entity.getId() == null ? actor() : entity.getCreatedBy());
        entity.setUpdatedBy(actor());
        if (entity.getVersion() == null) entity.setVersion(0L);
        entity = profileRepository.save(entity);
        syncRules(entity.getId(), request.getRules());
        return toDto(entity);
    }

    private void syncRules(String profileId, List<LeaveProfileRuleDto> requests) {
        if (requests == null) return;
        Map<String, LeaveProfileRuleEntity> existing = ruleRepository.findByLeaveProfileIdOrderByCreatedAtAsc(profileId).stream()
                .collect(Collectors.toMap(LeaveProfileRuleEntity::getId, Function.identity()));
        Set<String> retained = new HashSet<>();
        for (LeaveProfileRuleDto request : requests) {
            LeaveTypeEntity type = leaveTypeRepository.findById(required(request.getLeaveTypeId(), "Leave type", 255))
                    .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
            LeaveProfileRuleEntity entity = hasText(request.getId())
                    ? Optional.ofNullable(existing.get(request.getId())).orElseThrow(() -> new IllegalArgumentException("Profile rule does not belong to this profile"))
                    : new LeaveProfileRuleEntity();
            entity.setLeaveProfileId(profileId);
            entity.setLeaveTypeId(type.getId());
            entity.setEntitlementAmount(nonNegative(defaultDecimal(request.getEntitlementAmount(), BigDecimal.ZERO), "Entitlement"));
            entity.setCycleMonths(request.getCycleMonths() == null ? 12 : request.getCycleMonths());
            if (entity.getCycleMonths() <= 0 || entity.getCycleMonths() > 120) throw new IllegalArgumentException("Cycle months must be between 1 and 120");
            entity.setAccrualMethod(normalizeChoice(request.getAccrualMethod(), "UPFRONT", Set.of("UPFRONT", "MONTHLY", "NONE"), "Accrual method"));
            entity.setAccrualFrequency(normalizeChoice(request.getAccrualFrequency(), "MONTHLY", Set.of("MONTHLY", "ANNUAL"), "Accrual frequency"));
            entity.setAccrualAmount(nonNegative(request.getAccrualAmount(), "Accrual amount"));
            entity.setProRata(defaultBoolean(request.getProRata(), true));
            entity.setCarryOverAllowed(defaultBoolean(request.getCarryOverAllowed(), false));
            entity.setMaximumCarryOver(nonNegative(request.getMaximumCarryOver(), "Maximum carry over"));
            entity.setCarryOverExpiryMonths(request.getCarryOverExpiryMonths());
            entity.setMaximumNegativeBalance(nonNegative(defaultDecimal(request.getMaximumNegativeBalance(), BigDecimal.ZERO), "Maximum negative balance"));
            entity.setWaitingPeriodDays(request.getWaitingPeriodDays() == null ? 0 : request.getWaitingPeriodDays());
            if (entity.getWaitingPeriodDays() < 0) throw new IllegalArgumentException("Waiting period cannot be negative");
            entity.setSupportingDocumentRequiredOverride(request.getSupportingDocumentRequiredOverride());
            entity.setActiveFrom(defaultDate(request.getActiveFrom(), LocalDate.now()));
            entity.setActiveTo(defaultDate(request.getActiveTo(), MAX_DATE));
            validateRange(entity.getActiveFrom(), entity.getActiveTo(), "Profile rule");
            entity.setActive(defaultBoolean(request.getActive(), true));
            entity.setCreatedBy(entity.getId() == null ? actor() : entity.getCreatedBy());
            entity.setUpdatedBy(actor());
            if (entity.getVersion() == null) entity.setVersion(0L);
            entity = ruleRepository.save(entity);
            retained.add(entity.getId());
        }
        existing.values().stream().filter(item -> !retained.contains(item.getId())).forEach(item -> {
            item.setActive(false);
            item.setActiveTo(LocalDate.now());
            item.setUpdatedBy(actor());
            ruleRepository.save(item);
        });
    }

    @Transactional(readOnly = true)
    public List<LeaveProfileAssignmentDto> listEmployeeAssignments(String employmentId) {
        List<EmploymentLeaveProfileAssignmentEntity> assignments = hasText(employmentId)
                ? employeeAssignmentRepository.findByEmploymentIdOrderByEffectiveFromDesc(employmentId)
                : employeeAssignmentRepository.findAll();
        return assignments.stream().map(this::toDto).toList();
    }

    @Transactional
    public LeaveProfileAssignmentDto assignEmployee(LeaveProfileAssignmentDto request) {
        if (request == null) throw new IllegalArgumentException("Employee profile assignment is required");
        EmploymentEntity employment = employmentRepository.findById(required(request.getEmploymentId(), "Employment", 255))
                .orElseThrow(() -> new NoSuchElementException("Employment record not found"));
        LeaveProfileEntity profile = profileRepository.findById(required(request.getLeaveProfileId(), "Leave profile", 255))
                .orElseThrow(() -> new NoSuchElementException("Leave profile not found"));
        LocalDate from = defaultDate(request.getEffectiveFrom(), LocalDate.now());
        LocalDate to = defaultDate(request.getEffectiveTo(), MAX_DATE);
        validateRange(from, to, "Profile assignment");
        validateProfileActive(profile, from);
        Optional<EmploymentLeaveProfileAssignmentEntity> currentAssignment = employeeAssignmentRepository
                .findFirstByEmploymentIdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        employment.getId(), from, from);
        if (currentAssignment.isPresent()) {
            EmploymentLeaveProfileAssignmentEntity current = currentAssignment.get();
            if (current.getEffectiveFrom().equals(from)) {
                current.setLeaveProfileId(profile.getId());
                current.setEffectiveTo(to);
                current.setAssignmentSource("EMPLOYEE");
                current.setOverrideReason(trim(request.getOverrideReason(), 500));
                current.setActive(true);
                current.setAssignedBy(actor());
                return toDto(employeeAssignmentRepository.save(current));
            }
            current.setEffectiveTo(from.minusDays(1));
            employeeAssignmentRepository.save(current);
        }
        EmploymentLeaveProfileAssignmentEntity entity = EmploymentLeaveProfileAssignmentEntity.builder()
                .employmentId(employment.getId()).leaveProfileId(profile.getId())
                .effectiveFrom(from).effectiveTo(to).assignmentSource("EMPLOYEE")
                .overrideReason(trim(request.getOverrideReason(), 500)).active(true)
                .assignedBy(actor()).version(0L).build();
        return toDto(employeeAssignmentRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<LeaveProfileAssignmentDto> listPositionAssignments() {
        return positionAssignmentRepository.findAllByOrderByPositionCodeAscEffectiveFromDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public LeaveProfileAssignmentDto assignPosition(LeaveProfileAssignmentDto request) {
        if (request == null) throw new IllegalArgumentException("Position profile assignment is required");
        String position = normalizeCode(request.getPositionCode(), "Position code");
        LeaveProfileEntity profile = profileRepository.findById(required(request.getLeaveProfileId(), "Leave profile", 255))
                .orElseThrow(() -> new NoSuchElementException("Leave profile not found"));
        LocalDate from = defaultDate(request.getEffectiveFrom(), LocalDate.now());
        LocalDate to = defaultDate(request.getEffectiveTo(), MAX_DATE);
        validateRange(from, to, "Position profile assignment");
        validateProfileActive(profile, from);
        Optional<PositionLeaveProfileAssignmentEntity> currentAssignment = positionAssignmentRepository
                .findFirstByPositionCodeIgnoreCaseAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        position, from, from);
        if (currentAssignment.isPresent()) {
            PositionLeaveProfileAssignmentEntity current = currentAssignment.get();
            if (current.getEffectiveFrom().equals(from)) {
                current.setLeaveProfileId(profile.getId());
                current.setEffectiveTo(to);
                current.setActive(true);
                current.setAssignedBy(actor());
                return toDto(positionAssignmentRepository.save(current));
            }
            current.setEffectiveTo(from.minusDays(1));
            positionAssignmentRepository.save(current);
        }
        PositionLeaveProfileAssignmentEntity entity = PositionLeaveProfileAssignmentEntity.builder()
                .positionCode(position).leaveProfileId(profile.getId()).effectiveFrom(from).effectiveTo(to)
                .active(true).assignedBy(actor()).version(0L).build();
        return toDto(positionAssignmentRepository.save(entity));
    }

    @Transactional
    public ResolvedProfile assignResolvedProfileOnHire(EmploymentEntity employment, LocalDate effectiveDate) {
        ResolvedProfile resolved = resolveByPositionOrDefault(employment.getPosition(), effectiveDate);
        EmploymentLeaveProfileAssignmentEntity assignment = EmploymentLeaveProfileAssignmentEntity.builder()
                .employmentId(employment.getId()).leaveProfileId(resolved.getProfile().getId())
                .effectiveFrom(effectiveDate).effectiveTo(MAX_DATE)
                .assignmentSource(resolved.getSource()).overrideReason("Assigned during approved hire")
                .active(true).assignedBy(actor()).version(0L).build();
        employeeAssignmentRepository.save(assignment);
        return new ResolvedProfile(resolved.getProfile(), resolved.getSource(), assignment.getId());
    }

    @Transactional
    public ResolvedProfile realignDerivedProfileAssignment(EmploymentEntity employment, LocalDate effectiveDate) {
        if (employment == null) throw new IllegalArgumentException("Employment record is required");
        LocalDate from = defaultDate(effectiveDate, LocalDate.now());
        Optional<EmploymentLeaveProfileAssignmentEntity> current = employeeAssignmentRepository
                .findFirstByEmploymentIdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        employment.getId(), from, from);
        if (current.isPresent() && "EMPLOYEE".equalsIgnoreCase(current.get().getAssignmentSource())) {
            EmploymentLeaveProfileAssignmentEntity override = current.get();
            return new ResolvedProfile(requireProfile(override.getLeaveProfileId()), "EMPLOYEE", override.getId());
        }

        ResolvedProfile resolved = resolveByPositionOrDefault(employment.getPosition(), from);
        if (current.isPresent()) {
            EmploymentLeaveProfileAssignmentEntity existing = current.get();
            if (Objects.equals(existing.getLeaveProfileId(), resolved.getProfile().getId())
                    && Objects.equals(existing.getAssignmentSource(), resolved.getSource())) {
                return new ResolvedProfile(resolved.getProfile(), resolved.getSource(), existing.getId());
            }
            if (existing.getEffectiveFrom().equals(from)) {
                existing.setLeaveProfileId(resolved.getProfile().getId());
                existing.setAssignmentSource(resolved.getSource());
                existing.setOverrideReason("Realigned after employment position change");
                existing.setAssignedBy(actor());
                employeeAssignmentRepository.save(existing);
                return new ResolvedProfile(resolved.getProfile(), resolved.getSource(), existing.getId());
            }
            existing.setEffectiveTo(from.minusDays(1));
            employeeAssignmentRepository.save(existing);
        }

        EmploymentLeaveProfileAssignmentEntity assignment = EmploymentLeaveProfileAssignmentEntity.builder()
                .employmentId(employment.getId()).leaveProfileId(resolved.getProfile().getId())
                .effectiveFrom(from).effectiveTo(MAX_DATE).assignmentSource(resolved.getSource())
                .overrideReason("Realigned after employment position change")
                .active(true).assignedBy(actor()).version(0L).build();
        assignment = employeeAssignmentRepository.save(assignment);
        return new ResolvedProfile(resolved.getProfile(), resolved.getSource(), assignment.getId());
    }

    @Transactional(readOnly = true)
    public ResolvedProfile resolveProfile(EmploymentEntity employment, LocalDate onDate) {
        Optional<EmploymentLeaveProfileAssignmentEntity> employeeAssignment = employeeAssignmentRepository
                .findFirstByEmploymentIdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        employment.getId(), onDate, onDate);
        if (employeeAssignment.isPresent()
                && "EMPLOYEE".equalsIgnoreCase(employeeAssignment.get().getAssignmentSource())) {
            EmploymentLeaveProfileAssignmentEntity item = employeeAssignment.get();
            return new ResolvedProfile(requireProfile(item.getLeaveProfileId()), "EMPLOYEE", item.getId());
        }
        return resolveByPositionOrDefault(employment.getPosition(), onDate);
    }

    private ResolvedProfile resolveByPositionOrDefault(String position, LocalDate onDate) {
        if (hasText(position)) {
            Optional<PositionLeaveProfileAssignmentEntity> positionAssignment = positionAssignmentRepository
                    .findFirstByPositionCodeIgnoreCaseAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(position, onDate, onDate);
            if (positionAssignment.isPresent()) {
                return new ResolvedProfile(requireProfile(positionAssignment.get().getLeaveProfileId()), "POSITION", positionAssignment.get().getId());
            }
        }
        LeaveProfileEntity defaultProfile = profileRepository
                .findFirstByDefaultProfileTrueAndActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqual(onDate, onDate)
                .orElseThrow(() -> new IllegalStateException("No active default leave profile is configured"));
        return new ResolvedProfile(defaultProfile, "DEFAULT", null);
    }

    private void validateProfileActive(LeaveProfileEntity profile, LocalDate effectiveDate) {
        if (!Boolean.TRUE.equals(profile.getActive())
                || effectiveDate.isBefore(profile.getActiveFrom())
                || effectiveDate.isAfter(profile.getActiveTo())) {
            throw new IllegalArgumentException("The selected leave profile is not active on " + effectiveDate);
        }
    }

    public LeaveProfileEntity requireProfile(String id) {
        return profileRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Leave profile not found: " + id));
    }

    public LeaveTypeEntity requireLeaveTypeByCode(String code) {
        return leaveTypeRepository.findByCodeIgnoreCase(required(code, "Leave type", 50))
                .orElseThrow(() -> new IllegalArgumentException("Leave type is not configured: " + code));
    }

    public LeaveTypeEntity requireLeaveType(String id) {
        return leaveTypeRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Leave type not found: " + id));
    }

    public WorkingCalendarEntity requireCalendar(String id) {
        return calendarRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Working calendar not found: " + id));
    }

    public LeaveProfileRuleEntity requireRule(String profileId, String leaveTypeId, LocalDate onDate) {
        return ruleRepository.findFirstByLeaveProfileIdAndLeaveTypeIdAndActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqualOrderByActiveFromDesc(profileId, leaveTypeId, onDate, onDate)
                .orElseThrow(() -> new IllegalStateException("The selected leave type is not included in the employee's leave profile"));
    }

    public List<LeaveProfileRuleEntity> activeRules(String profileId, LocalDate onDate) {
        return ruleRepository.findByLeaveProfileIdAndActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqual(profileId, onDate, onDate);
    }

    public List<WorkingCalendarHolidayEntity> holidays(String calendarId, LocalDate from, LocalDate to) {
        Map<String, WorkingCalendarHolidayEntity> holidays = new LinkedHashMap<>();
        holidayRepository.findByWorkingCalendarIdAndActiveTrueAndHolidayDateBetween(calendarId, from, to)
                .forEach(item -> holidays.put(item.getId(), item));
        holidayRepository.findByWorkingCalendarIdAndActiveTrueAndRecurringAnnualTrue(calendarId)
                .forEach(item -> holidays.put(item.getId(), item));
        return new ArrayList<>(holidays.values());
    }

    private LeaveTypeDto toDto(LeaveTypeEntity e) {
        LeaveTypeDto d = new LeaveTypeDto();
        d.setId(e.getId()); d.setCode(e.getCode()); d.setName(e.getName()); d.setDescription(e.getDescription());
        d.setPaid(e.getPaid()); d.setUnit(e.getUnit()); d.setAllowHalfDay(e.getAllowHalfDay());
        d.setRequiresSupportingDocument(e.getRequiresSupportingDocument()); d.setDocumentRequiredAfter(e.getDocumentRequiredAfter());
        d.setMinimumRequest(e.getMinimumRequest()); d.setMaximumConsecutive(e.getMaximumConsecutive());
        d.setAllowNegativeBalance(e.getAllowNegativeBalance()); d.setIncludeWeekends(e.getIncludeWeekends());
        d.setIncludePublicHolidays(e.getIncludePublicHolidays()); d.setRequiresApproval(e.getRequiresApproval());
        d.setActiveFrom(e.getActiveFrom()); d.setActiveTo(e.getActiveTo()); d.setDisplayOrder(e.getDisplayOrder());
        d.setColour(e.getColour()); d.setIcon(e.getIcon()); d.setActive(e.getActive()); d.setVersion(e.getVersion());
        return d;
    }

    private WorkingCalendarDto toDto(WorkingCalendarEntity e) {
        WorkingCalendarDto d = new WorkingCalendarDto();
        d.setId(e.getId()); d.setCode(e.getCode()); d.setName(e.getName()); d.setDescription(e.getDescription());
        d.setMondayWorking(e.getMondayWorking()); d.setTuesdayWorking(e.getTuesdayWorking());
        d.setWednesdayWorking(e.getWednesdayWorking()); d.setThursdayWorking(e.getThursdayWorking());
        d.setFridayWorking(e.getFridayWorking()); d.setSaturdayWorking(e.getSaturdayWorking()); d.setSundayWorking(e.getSundayWorking());
        d.setHoursPerDay(e.getHoursPerDay()); d.setActive(e.getActive()); d.setVersion(e.getVersion());
        d.setHolidays(holidayRepository.findByWorkingCalendarIdOrderByHolidayDateAsc(e.getId()).stream().map(h -> {
            WorkingCalendarHolidayDto hd = new WorkingCalendarHolidayDto();
            hd.setId(h.getId()); hd.setHolidayDate(h.getHolidayDate()); hd.setName(h.getName());
            hd.setRecurringAnnual(h.getRecurringAnnual()); hd.setActive(h.getActive()); return hd;
        }).toList());
        return d;
    }

    private LeaveProfileDto toDto(LeaveProfileEntity e) {
        LeaveProfileDto d = new LeaveProfileDto();
        d.setId(e.getId()); d.setCode(e.getCode()); d.setName(e.getName()); d.setDescription(e.getDescription());
        d.setWorkingCalendarId(e.getWorkingCalendarId());
        d.setWorkingCalendarName(calendarRepository.findById(e.getWorkingCalendarId()).map(WorkingCalendarEntity::getName).orElse(e.getWorkingCalendarId()));
        d.setDefaultProfile(e.getDefaultProfile()); d.setActiveFrom(e.getActiveFrom()); d.setActiveTo(e.getActiveTo());
        d.setActive(e.getActive()); d.setVersion(e.getVersion());
        d.setRules(ruleRepository.findByLeaveProfileIdOrderByCreatedAtAsc(e.getId()).stream().map(this::toDto).toList());
        return d;
    }

    private LeaveProfileRuleDto toDto(LeaveProfileRuleEntity e) {
        LeaveProfileRuleDto d = new LeaveProfileRuleDto();
        d.setId(e.getId()); d.setLeaveTypeId(e.getLeaveTypeId());
        leaveTypeRepository.findById(e.getLeaveTypeId()).ifPresent(type -> { d.setLeaveTypeCode(type.getCode()); d.setLeaveTypeName(type.getName()); });
        d.setEntitlementAmount(e.getEntitlementAmount()); d.setCycleMonths(e.getCycleMonths());
        d.setAccrualMethod(e.getAccrualMethod()); d.setAccrualFrequency(e.getAccrualFrequency()); d.setAccrualAmount(e.getAccrualAmount());
        d.setProRata(e.getProRata()); d.setCarryOverAllowed(e.getCarryOverAllowed()); d.setMaximumCarryOver(e.getMaximumCarryOver());
        d.setCarryOverExpiryMonths(e.getCarryOverExpiryMonths()); d.setMaximumNegativeBalance(e.getMaximumNegativeBalance());
        d.setWaitingPeriodDays(e.getWaitingPeriodDays()); d.setSupportingDocumentRequiredOverride(e.getSupportingDocumentRequiredOverride());
        d.setActiveFrom(e.getActiveFrom()); d.setActiveTo(e.getActiveTo()); d.setActive(e.getActive()); d.setVersion(e.getVersion());
        return d;
    }

    private LeaveProfileAssignmentDto toDto(EmploymentLeaveProfileAssignmentEntity e) {
        LeaveProfileAssignmentDto d = new LeaveProfileAssignmentDto();
        d.setId(e.getId()); d.setEmploymentId(e.getEmploymentId()); d.setLeaveProfileId(e.getLeaveProfileId());
        d.setEffectiveFrom(e.getEffectiveFrom()); d.setEffectiveTo(e.getEffectiveTo()); d.setAssignmentSource(e.getAssignmentSource());
        d.setOverrideReason(e.getOverrideReason()); d.setActive(e.getActive()); d.setVersion(e.getVersion());
        profileRepository.findById(e.getLeaveProfileId()).ifPresent(p -> d.setLeaveProfileName(p.getName()));
        employmentRepository.findById(e.getEmploymentId()).ifPresent(emp -> {
            d.setEmployeeNumber(emp.getEmployeeNumber()); d.setPositionCode(emp.getPosition()); d.setEmployeeName(partnerName(emp.getPartnerId()));
        });
        return d;
    }

    private LeaveProfileAssignmentDto toDto(PositionLeaveProfileAssignmentEntity e) {
        LeaveProfileAssignmentDto d = new LeaveProfileAssignmentDto();
        d.setId(e.getId()); d.setPositionCode(e.getPositionCode()); d.setLeaveProfileId(e.getLeaveProfileId());
        d.setEffectiveFrom(e.getEffectiveFrom()); d.setEffectiveTo(e.getEffectiveTo()); d.setAssignmentSource("POSITION");
        d.setActive(e.getActive()); d.setVersion(e.getVersion());
        profileRepository.findById(e.getLeaveProfileId()).ifPresent(p -> d.setLeaveProfileName(p.getName()));
        return d;
    }

    private String partnerName(String partnerId) {
        try {
            PartnerDto p = partnerService.getOptional(partnerId);
            return java.util.stream.Stream.of(p.getName2(), p.getName3(), p.getName1())
                    .filter(this::hasText).collect(Collectors.joining(" "));
        } catch (Exception ignored) { return partnerId; }
    }

    private String actor() {
        if (hasText(UserContext.getCurrentUserId())) return UserContext.getCurrentUserId();
        if (hasText(UserContext.getCurrentUser())) return UserContext.getCurrentUser();
        return "SYSTEM";
    }

    private String normalizeCode(String value, String label) {
        String normalized = required(value, label, 255).trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (!normalized.matches("[A-Z0-9_-]+")) throw new IllegalArgumentException(label + " may only contain letters, numbers, hyphens and underscores");
        return normalized;
    }
    private String normalizeUnit(String value) { return normalizeChoice(value, "DAYS", Set.of("DAYS", "HOURS"), "Leave unit"); }
    private String normalizeChoice(String value, String fallback, Set<String> allowed, String label) {
        String normalized = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!allowed.contains(normalized)) throw new IllegalArgumentException(label + " must be one of " + allowed);
        return normalized;
    }
    private String required(String value, String label, int max) {
        if (!hasText(value)) throw new IllegalArgumentException(label + " is required");
        String result = value.trim(); if (result.length() > max) throw new IllegalArgumentException(label + " cannot exceed " + max + " characters"); return result;
    }
    private String trim(String value, int max) { if (!hasText(value)) return null; String result=value.trim(); if(result.length()>max) throw new IllegalArgumentException("Value cannot exceed "+max+" characters"); return result; }
    private void validateRange(LocalDate from, LocalDate to, String label) { if (to.isBefore(from)) throw new IllegalArgumentException(label + " end date cannot be before start date"); }
    private BigDecimal nonNegative(BigDecimal value, String label) { if (value != null && value.signum() < 0) throw new IllegalArgumentException(label + " cannot be negative"); return value; }
    private BigDecimal positive(BigDecimal value, String label) { if (value == null || value.signum() <= 0) throw new IllegalArgumentException(label + " must be greater than zero"); return value; }
    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal fallback) { return value == null ? fallback : value; }
    private Boolean defaultBoolean(Boolean value, boolean fallback) { return value == null ? fallback : value; }
    private LocalDate defaultDate(LocalDate value, LocalDate fallback) { return value == null ? fallback : value; }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

    @Getter @AllArgsConstructor
    public static class ResolvedProfile {
        private LeaveProfileEntity profile;
        private String source;
        private String assignmentId;
    }
}
