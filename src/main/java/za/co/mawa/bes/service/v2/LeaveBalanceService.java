package za.co.mawa.bes.service.v2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.v2.LeaveBalanceDto;
import za.co.mawa.bes.dto.v2.LeaveLedgerDto;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.v2.*;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LeaveBalanceService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final EmployeeLeaveBalanceRepository balanceRepository;
    private final EmployeeLeaveLedgerRepository ledgerRepository;
    private final EmploymentRepository employmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveProfileRuleRepository ruleRepository;
    private final LeaveConfigurationService configurationService;
    private final PartnerService partnerService;

    public LeaveBalanceService(
            EmployeeLeaveBalanceRepository balanceRepository,
            EmployeeLeaveLedgerRepository ledgerRepository,
            EmploymentRepository employmentRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveProfileRuleRepository ruleRepository,
            LeaveConfigurationService configurationService,
            PartnerService partnerService) {
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.employmentRepository = employmentRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.ruleRepository = ruleRepository;
        this.configurationService = configurationService;
        this.partnerService = partnerService;
    }

    @Transactional
    public void initialiseForEmployment(EmploymentEntity employment, LocalDate effectiveDate) {
        LeaveConfigurationService.ResolvedProfile resolved = configurationService.resolveProfile(employment, effectiveDate);
        for (LeaveProfileRuleEntity rule : configurationService.activeRules(resolved.getProfile().getId(), effectiveDate)) {
            ensureBalance(employment, rule, effectiveDate);
        }
    }

    @Transactional
    public EmployeeLeaveBalanceEntity ensureCurrentBalance(EmploymentEntity employment, LeaveTypeEntity leaveType, LocalDate onDate) {
        LeaveConfigurationService.ResolvedProfile resolved = configurationService.resolveProfile(employment, onDate);
        LeaveProfileRuleEntity rule = configurationService.requireRule(resolved.getProfile().getId(), leaveType.getId(), onDate);
        return ensureBalance(employment, rule, onDate);
    }

    @Transactional
    public EmployeeLeaveBalanceEntity ensureBalance(EmploymentEntity employment, LeaveProfileRuleEntity rule, LocalDate onDate) {
        Cycle cycle = cycleFor(employment, rule, onDate);
        EmployeeLeaveBalanceEntity balance = balanceRepository
                .findFirstByEmploymentIdAndLeaveTypeIdAndCycleStartLessThanEqualAndCycleEndGreaterThanEqual(
                        employment.getId(), rule.getLeaveTypeId(), onDate, onDate)
                .orElseGet(() -> createBalance(employment, rule, cycle));
        balance.setLeaveProfileRuleId(rule.getId());
        accrueTo(balance, rule, employment, onDate);
        return balanceRepository.save(balance);
    }

    private EmployeeLeaveBalanceEntity createBalance(EmploymentEntity employment, LeaveProfileRuleEntity rule, Cycle cycle) {
        BigDecimal opening = ZERO;
        BigDecimal accrued = ZERO;
        if ("UPFRONT".equalsIgnoreCase(rule.getAccrualMethod())) {
            accrued = entitlementForPeriod(employment, rule, cycle);
        }
        BigDecimal carriedForward = ZERO;
        if (Boolean.TRUE.equals(rule.getCarryOverAllowed())) {
            carriedForward = balanceRepository
                    .findFirstByEmploymentIdAndLeaveTypeIdAndCycleEndBeforeOrderByCycleEndDesc(
                            employment.getId(), rule.getLeaveTypeId(), cycle.start())
                    .map(previous -> scale(previous.getAvailableBalance()).max(ZERO))
                    .orElse(ZERO);
            if (rule.getMaximumCarryOver() != null) {
                carriedForward = carriedForward.min(scale(rule.getMaximumCarryOver()));
            }
        }
        BigDecimal available = accrued.add(carriedForward).setScale(2, RoundingMode.HALF_UP);
        EmployeeLeaveBalanceEntity balance = EmployeeLeaveBalanceEntity.builder()
                .employmentId(employment.getId())
                .leaveTypeId(rule.getLeaveTypeId())
                .leaveProfileRuleId(rule.getId())
                .cycleStart(cycle.start()).cycleEnd(cycle.end())
                .openingBalance(opening).accrued(accrued).taken(ZERO).adjusted(ZERO)
                .carriedForward(carriedForward).expired(ZERO).availableBalance(available)
                .lastAccrualDate("UPFRONT".equalsIgnoreCase(rule.getAccrualMethod()) ? cycle.start() : null)
                .version(0L).build();
        balance = balanceRepository.save(balance);
        if (accrued.signum() != 0) {
            ledger(balance, "OPENING_ENTITLEMENT", cycle.start(), accrued, "LEAVE_PROFILE_RULE",
                    rule.getId() + ":" + cycle.start(), "Opening entitlement for leave cycle");
        }
        if (carriedForward.signum() != 0) {
            ledger(balance, "CARRY_OVER", cycle.start(), carriedForward, "LEAVE_PROFILE_RULE",
                    rule.getId() + ":CARRY:" + cycle.start(), "Leave carried forward from the previous cycle");
        }
        return balance;
    }

    private BigDecimal entitlementForPeriod(EmploymentEntity employment, LeaveProfileRuleEntity rule, Cycle cycle) {
        BigDecimal entitlement = scale(rule.getEntitlementAmount());
        if (!Boolean.TRUE.equals(rule.getProRata()) || employment.getStartDate() == null) return entitlement;
        LocalDate employmentStart = new java.sql.Date(employment.getStartDate().getTime()).toLocalDate();
        LocalDate entitlementStart = employmentStart.isAfter(cycle.start()) ? employmentStart : cycle.start();
        if (entitlementStart.isAfter(cycle.end())) return ZERO;
        long totalDays = ChronoUnit.DAYS.between(cycle.start(), cycle.end()) + 1;
        long eligibleDays = ChronoUnit.DAYS.between(entitlementStart, cycle.end()) + 1;
        return entitlement.multiply(BigDecimal.valueOf(eligibleDays))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
    }

    private void accrueTo(EmployeeLeaveBalanceEntity balance, LeaveProfileRuleEntity rule, EmploymentEntity employment, LocalDate onDate) {
        expireCarryOverIfRequired(balance, rule, onDate);
        if (!"MONTHLY".equalsIgnoreCase(rule.getAccrualMethod())) return;
        LocalDate employmentStart = employment.getStartDate() == null
                ? balance.getCycleStart()
                : new java.sql.Date(employment.getStartDate().getTime()).toLocalDate();
        LocalDate accrualStart = employmentStart.isAfter(balance.getCycleStart()) ? employmentStart : balance.getCycleStart();
        LocalDate last = balance.getLastAccrualDate();
        LocalDate firstMonth = LocalDate.of(accrualStart.getYear(), accrualStart.getMonth(), 1);
        LocalDate targetMonth = LocalDate.of(onDate.getYear(), onDate.getMonth(), 1);
        LocalDate nextMonth = last == null
                ? firstMonth
                : LocalDate.of(last.getYear(), last.getMonth(), 1).plusMonths(1);
        if (nextMonth.isAfter(targetMonth)) return;
        BigDecimal monthly = rule.getAccrualAmount() != null
                ? rule.getAccrualAmount()
                : scale(rule.getEntitlementAmount()).divide(BigDecimal.valueOf(rule.getCycleMonths()), 4, RoundingMode.HALF_UP);
        BigDecimal amount = ZERO;
        for (LocalDate month = nextMonth; !month.isAfter(targetMonth); month = month.plusMonths(1)) {
            LocalDate monthEnd = month.plusMonths(1).minusDays(1);
            LocalDate eligibleFrom = accrualStart.isAfter(month) ? accrualStart : month;
            LocalDate eligibleTo = monthEnd;
            if (balance.getCycleStart().isAfter(eligibleFrom)) eligibleFrom = balance.getCycleStart();
            if (balance.getCycleEnd().isBefore(eligibleTo)) eligibleTo = balance.getCycleEnd();
            if (eligibleTo.isBefore(eligibleFrom)) continue;

            BigDecimal installment = monthly;
            if (Boolean.TRUE.equals(rule.getProRata())
                    && (!eligibleFrom.equals(month) || !eligibleTo.equals(monthEnd))) {
                long eligibleDays = ChronoUnit.DAYS.between(eligibleFrom, eligibleTo) + 1;
                installment = monthly.multiply(BigDecimal.valueOf(eligibleDays))
                        .divide(BigDecimal.valueOf(month.lengthOfMonth()), 4, RoundingMode.HALF_UP);
            }
            amount = amount.add(installment);
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() == 0) {
            balance.setLastAccrualDate(targetMonth);
            return;
        }
        balance.setAccrued(scale(balance.getAccrued()).add(amount));
        balance.setAvailableBalance(calculateAvailable(balance));
        balance.setLastAccrualDate(targetMonth);
        balanceRepository.save(balance);
        ledger(balance, "ACCRUAL", targetMonth, amount, "LEAVE_PROFILE_RULE",
                rule.getId() + ":" + balance.getCycleStart() + ":" + targetMonth,
                "Accrual through " + targetMonth);
    }

    private void expireCarryOverIfRequired(EmployeeLeaveBalanceEntity balance, LeaveProfileRuleEntity rule, LocalDate onDate) {
        if (!Boolean.TRUE.equals(rule.getCarryOverAllowed()) || rule.getCarryOverExpiryMonths() == null
                || rule.getCarryOverExpiryMonths() <= 0 || scale(balance.getCarriedForward()).signum() <= 0) return;
        LocalDate expiryDate = balance.getCycleStart().plusMonths(rule.getCarryOverExpiryMonths());
        if (onDate.isBefore(expiryDate) || scale(balance.getExpired()).compareTo(scale(balance.getCarriedForward())) >= 0) return;
        BigDecimal amount = scale(balance.getCarriedForward()).subtract(scale(balance.getExpired())).max(ZERO);
        if (amount.signum() == 0) return;
        balance.setExpired(scale(balance.getExpired()).add(amount));
        balance.setAvailableBalance(calculateAvailable(balance));
        balanceRepository.save(balance);
        ledger(balance, "EXPIRY", expiryDate, amount.negate(), "LEAVE_PROFILE_RULE",
                rule.getId() + ":EXPIRY:" + balance.getCycleStart(), "Expired carried-forward leave");
    }

    @Transactional
    public EmployeeLeaveLedgerEntity debitApprovedLeave(String leaveRequestId, EmploymentEntity employment,
                                                          LeaveTypeEntity leaveType, BigDecimal amount, LocalDate date) {
        Optional<EmployeeLeaveLedgerEntity> existing = ledgerRepository
                .findByReferenceTypeAndReferenceIdAndTransactionType("LEAVE_REQUEST", leaveRequestId, "LEAVE_TAKEN");
        if (existing.isPresent()) return existing.get();
        EmployeeLeaveBalanceEntity current = ensureCurrentBalance(employment, leaveType, date);
        EmployeeLeaveBalanceEntity balance = balanceRepository.findByIdForUpdate(current.getId()).orElseThrow();
        LeaveProfileRuleEntity rule = ruleRepository.findById(balance.getLeaveProfileRuleId())
                .orElseThrow(() -> new IllegalStateException("Leave profile rule not found for balance"));
        BigDecimal requested = scale(amount);
        BigDecimal projected = scale(balance.getAvailableBalance()).subtract(requested);
        BigDecimal allowedNegative = Boolean.TRUE.equals(leaveType.getAllowNegativeBalance())
                ? scale(rule.getMaximumNegativeBalance())
                : ZERO;
        if (projected.compareTo(allowedNegative.negate()) < 0) {
            throw new IllegalStateException("Insufficient " + leaveType.getName() + " balance. Available: " + balance.getAvailableBalance());
        }
        balance.setTaken(scale(balance.getTaken()).add(requested));
        balance.setAvailableBalance(projected);
        balanceRepository.save(balance);
        return ledger(balance, "LEAVE_TAKEN", date, requested.negate(), "LEAVE_REQUEST", leaveRequestId,
                "Approved leave request");
    }

    @Transactional
    public EmployeeLeaveLedgerEntity reverseApprovedLeave(String leaveRequestId, String actor) {
        Optional<EmployeeLeaveLedgerEntity> existingReversal = ledgerRepository
                .findByReferenceTypeAndReferenceIdAndTransactionType("LEAVE_REQUEST", leaveRequestId, "LEAVE_REVERSAL");
        if (existingReversal.isPresent()) return existingReversal.get();
        EmployeeLeaveLedgerEntity debit = ledgerRepository
                .findByReferenceTypeAndReferenceIdAndTransactionType("LEAVE_REQUEST", leaveRequestId, "LEAVE_TAKEN")
                .orElseThrow(() -> new IllegalStateException("Approved leave ledger entry was not found"));
        EmployeeLeaveBalanceEntity balance = balanceRepository.findByIdForUpdate(debit.getEmployeeLeaveBalanceId()).orElseThrow();
        BigDecimal amount = debit.getAmount().abs();
        balance.setTaken(scale(balance.getTaken()).subtract(amount).max(ZERO));
        balance.setAvailableBalance(scale(balance.getAvailableBalance()).add(amount));
        balanceRepository.save(balance);
        return ledger(balance, "LEAVE_REVERSAL", LocalDate.now(), amount, "LEAVE_REQUEST", leaveRequestId,
                "Approved leave cancellation reversal", actor);
    }

    @Transactional
    public EmployeeLeaveLedgerEntity applyAdjustment(String adjustmentRequestId, EmploymentEntity employment,
                                                       LeaveTypeEntity leaveType, BigDecimal amount, LocalDate effectiveDate,
                                                       String description, String actor) {
        Optional<EmployeeLeaveLedgerEntity> existing = ledgerRepository
                .findByReferenceTypeAndReferenceIdAndTransactionType("LEAVE_BALANCE_ADJUSTMENT", adjustmentRequestId, "ADJUSTMENT");
        if (existing.isPresent()) return existing.get();
        EmployeeLeaveBalanceEntity current = ensureCurrentBalance(employment, leaveType, effectiveDate);
        EmployeeLeaveBalanceEntity balance = balanceRepository.findByIdForUpdate(current.getId()).orElseThrow();
        BigDecimal adjusted = scale(amount);
        balance.setAdjusted(scale(balance.getAdjusted()).add(adjusted));
        balance.setAvailableBalance(scale(balance.getAvailableBalance()).add(adjusted));
        balanceRepository.save(balance);
        return ledger(balance, "ADJUSTMENT", effectiveDate, adjusted, "LEAVE_BALANCE_ADJUSTMENT", adjustmentRequestId,
                description, actor);
    }

    @Transactional
    public List<LeaveBalanceDto> listBalances(String employmentId) {
        LocalDate today = LocalDate.now();
        if (hasText(employmentId)) {
            EmploymentEntity employment = employmentRepository.findById(employmentId)
                    .orElseThrow(() -> new NoSuchElementException("Employment record not found"));
            initialiseForEmploymentIfEligible(employment, today);
            return balanceRepository.findByEmploymentIdOrderByCycleStartDesc(employmentId).stream()
                    .map(this::toDto).toList();
        }

        employmentRepository.findAll().stream()
                .filter(this::isLeaveEligible)
                .forEach(employment -> initialiseForEmployment(employment, today));
        return balanceRepository.findAll().stream().map(this::toDto).toList();
    }

    private void initialiseForEmploymentIfEligible(EmploymentEntity employment, LocalDate onDate) {
        if (isLeaveEligible(employment)) initialiseForEmployment(employment, onDate);
    }

    private boolean isLeaveEligible(EmploymentEntity employment) {
        return employment != null && (Status.ACTIVE.equalsIgnoreCase(employment.getStatus())
                || Status.SUSPENDED.equalsIgnoreCase(employment.getStatus()));
    }

    @Transactional(readOnly = true)
    public List<LeaveLedgerDto> listLedger(String employmentId) {
        if (!hasText(employmentId)) throw new IllegalArgumentException("Employment id is required");
        return ledgerRepository.findByEmploymentIdOrderByTransactionDateDescCreatedAtDesc(employmentId).stream()
                .map(this::toDto).toList();
    }

    public BigDecimal projectedBalance(EmploymentEntity employment, LeaveTypeEntity leaveType, BigDecimal amount, LocalDate onDate) {
        EmployeeLeaveBalanceEntity balance = ensureCurrentBalance(employment, leaveType, onDate);
        return scale(balance.getAvailableBalance()).subtract(scale(amount));
    }

    public BigDecimal availableBalance(EmploymentEntity employment, LeaveTypeEntity leaveType, LocalDate onDate) {
        return scale(ensureCurrentBalance(employment, leaveType, onDate).getAvailableBalance());
    }

    private EmployeeLeaveLedgerEntity ledger(EmployeeLeaveBalanceEntity balance, String type, LocalDate date,
                                               BigDecimal amount, String referenceType, String referenceId, String description) {
        return ledger(balance, type, date, amount, referenceType, referenceId, description, actor());
    }

    private EmployeeLeaveLedgerEntity ledger(EmployeeLeaveBalanceEntity balance, String type, LocalDate date,
                                               BigDecimal amount, String referenceType, String referenceId,
                                               String description, String actor) {
        EmployeeLeaveLedgerEntity entity = EmployeeLeaveLedgerEntity.builder()
                .employeeLeaveBalanceId(balance.getId()).employmentId(balance.getEmploymentId())
                .leaveTypeId(balance.getLeaveTypeId()).transactionType(type).transactionDate(date)
                .amount(scale(amount)).balanceAfter(scale(balance.getAvailableBalance()))
                .referenceType(referenceType).referenceId(referenceId).description(description).createdBy(actor).build();
        return ledgerRepository.save(entity);
    }

    private Cycle cycleFor(EmploymentEntity employment, LeaveProfileRuleEntity rule, LocalDate onDate) {
        LocalDate anchor = employment.getStartDate() == null
                ? LocalDate.of(onDate.getYear(), 1, 1)
                : new java.sql.Date(employment.getStartDate().getTime()).toLocalDate();
        int months = Math.max(1, rule.getCycleMonths());
        LocalDate start = anchor;
        if (onDate.isBefore(start)) return new Cycle(start, start.plusMonths(months).minusDays(1));
        long elapsedMonths = ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), onDate.withDayOfMonth(1));
        long cycleIndex = elapsedMonths / months;
        start = anchor.plusMonths(cycleIndex * months);
        while (onDate.isAfter(start.plusMonths(months).minusDays(1))) start = start.plusMonths(months);
        return new Cycle(start, start.plusMonths(months).minusDays(1));
    }

    private BigDecimal calculateAvailable(EmployeeLeaveBalanceEntity b) {
        return scale(b.getOpeningBalance()).add(scale(b.getAccrued())).add(scale(b.getAdjusted()))
                .add(scale(b.getCarriedForward())).subtract(scale(b.getExpired())).subtract(scale(b.getTaken()));
    }

    private LeaveBalanceDto toDto(EmployeeLeaveBalanceEntity b) {
        EmploymentEntity employment = employmentRepository.findById(b.getEmploymentId()).orElse(null);
        LeaveTypeEntity type = leaveTypeRepository.findById(b.getLeaveTypeId()).orElse(null);
        return LeaveBalanceDto.builder().id(b.getId()).employmentId(b.getEmploymentId())
                .employeeNumber(employment == null ? null : employment.getEmployeeNumber())
                .employeeName(employment == null ? null : partnerName(employment.getPartnerId()))
                .leaveTypeId(b.getLeaveTypeId()).leaveTypeCode(type == null ? null : type.getCode())
                .leaveTypeName(type == null ? null : type.getName()).unit(type == null ? "DAYS" : type.getUnit())
                .cycleStart(b.getCycleStart()).cycleEnd(b.getCycleEnd()).openingBalance(b.getOpeningBalance())
                .accrued(b.getAccrued()).taken(b.getTaken()).adjusted(b.getAdjusted())
                .carriedForward(b.getCarriedForward()).expired(b.getExpired()).availableBalance(b.getAvailableBalance())
                .lastAccrualDate(b.getLastAccrualDate()).build();
    }

    private LeaveLedgerDto toDto(EmployeeLeaveLedgerEntity e) {
        LeaveTypeEntity type = leaveTypeRepository.findById(e.getLeaveTypeId()).orElse(null);
        return LeaveLedgerDto.builder().id(e.getId()).employmentId(e.getEmploymentId()).leaveTypeId(e.getLeaveTypeId())
                .leaveTypeCode(type == null ? null : type.getCode()).transactionType(e.getTransactionType())
                .transactionDate(e.getTransactionDate()).amount(e.getAmount()).balanceAfter(e.getBalanceAfter())
                .referenceType(e.getReferenceType()).referenceId(e.getReferenceId()).description(e.getDescription())
                .createdAt(e.getCreatedAt()).createdBy(e.getCreatedBy()).build();
    }

    private String partnerName(String partnerId) {
        try {
            PartnerDto p = partnerService.getOptional(partnerId);
            return Stream.of(p.getName2(), p.getName3(), p.getName1()).filter(this::hasText).collect(Collectors.joining(" "));
        } catch (Exception ignored) { return partnerId; }
    }
    private String actor() {
        if (hasText(UserContext.getCurrentUserId())) return UserContext.getCurrentUserId();
        if (hasText(UserContext.getCurrentUser())) return UserContext.getCurrentUser();
        return "SYSTEM";
    }
    private BigDecimal scale(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private record Cycle(LocalDate start, LocalDate end) {}
}
