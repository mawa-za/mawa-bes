package za.co.mawa.bes.service.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.SettingService;

/** Tenant policy is independent of mailbox imports. Existing records are assessed at capture time. */
@Service @RequiredArgsConstructor
public class BankDepositVerificationPolicyService {
    public static final String GROUP = "BANK-DEPOSIT-VERIFICATION";
    private static final ZoneId CAT = ZoneId.of("Africa/Johannesburg");
    private final SettingService settings;
    private final JdbcTemplate jdbc;
    private final BankTransactionService bankTransactions;

    public record Policy(boolean requireDepositId, LocalDate effectiveFrom, boolean requiredNow,
                         LocalDateTime activatedAtUtc) { }
    public record Update(boolean requireDepositId, LocalDate effectiveFrom) { }

    public Policy get() {
        boolean enabled = Boolean.parseBoolean(settings.getSetting("REQUIRE-DEPOSIT-ID", GROUP));
        LocalDate effective = parseDate(settings.getSetting("EFFECTIVE-FROM", GROUP));
        String activated = settings.getSetting("ACTIVATED-AT-UTC", GROUP);
        LocalDateTime activatedAt = activated == null || activated.isBlank() ? null : LocalDateTime.parse(activated);
        return new Policy(enabled, effective, enabled && effective != null &&
                !LocalDate.now(CAT).isBefore(effective), activatedAt);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value); }
        catch (RuntimeException ex) { throw new IllegalStateException("Invalid saved bank deposit enforcement date", ex); }
    }

    @Transactional
    public Policy save(Update change) {
        if (change == null) throw new IllegalArgumentException("Policy update is required");
        Policy current = get();
        LocalDate effective = change.effectiveFrom();
        if (change.requireDepositId()) {
            if (effective == null) throw new IllegalArgumentException("An enforcement effective date is required");
            // No retroactive enforcement. Records captured before first activation stay exempt.
            if (!current.requireDepositId() && effective.isBefore(LocalDate.now(CAT))) {
                throw new IllegalArgumentException("Enforcement cannot start before today's date");
            }
            // Moving an existing enforcement date backwards must never alter historical approvals.
            if (current.requireDepositId() && current.effectiveFrom() != null &&
                    effective.isBefore(current.effectiveFrom())) {
                throw new IllegalArgumentException("Enforcement date cannot move backwards");
            }
        } else {
            effective = current.effectiveFrom(); // keep date for audit/reference when disabled
        }
        if (current.requireDepositId() != change.requireDepositId() ||
                !java.util.Objects.equals(current.effectiveFrom(), effective)) {
            String actor = UserContext.getCurrentUserId();
            if (actor == null || actor.isBlank()) throw new SecurityException("Authenticated administrator required");
            jdbc.update("INSERT INTO bank_deposit_policy_audit (old_enabled, new_enabled, old_effective_from, new_effective_from, changed_by) VALUES (?, ?, ?, ?, ?)",
                    current.requireDepositId(), change.requireDepositId(), current.effectiveFrom(), effective, actor);
            // An activation made partway through a day must not retrospectively
            // require Deposit IDs for records captured earlier on that same date.
            if (change.requireDepositId() && !current.requireDepositId())
                settings.upsertSetting("ACTIVATED-AT-UTC", GROUP, LocalDateTime.now(ZoneOffset.UTC).toString());
            settings.upsertSetting("EFFECTIVE-FROM", GROUP, effective == null ? "" : effective.toString());
            settings.upsertSetting("REQUIRE-DEPOSIT-ID", GROUP, Boolean.toString(change.requireDepositId()));
        }
        return get();
    }

    /** Database timestamps are stored in UTC, while tenant enforcement dates are CAT. */
    public boolean requiredAt(LocalDateTime recordedAt) {
        Policy p = get();
        if (!p.requireDepositId() || p.effectiveFrom() == null) return false;
        LocalDateTime capturedUtc = recordedAt == null ? LocalDateTime.now(ZoneOffset.UTC) : recordedAt;
        LocalDate recordedDate = capturedUtc.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(CAT).toLocalDate();
        return !recordedDate.isBefore(p.effectiveFrom()) &&
                (p.activatedAtUtc() == null || !capturedUtc.isBefore(p.activatedAtUtc()));
    }

    public void validateProvidedOrOptional(String depositId, LocalDateTime recordedAt) {
        if (depositId != null && !depositId.isBlank()) {
            BankTransactionService.parseId(depositId);
        } else if (requiredAt(recordedAt)) {
            throw new IllegalArgumentException("A verified bank Deposit ID is required for new transactions");
        }
    }

    /** Still verifies and allocates any supplied ID even when policy enforcement is off. */
    public Long allocateIfProvided(String depositId, long amountCents, String type, String objectId,
                                   String actor, LocalDateTime recordedAt) {
        validateProvidedOrOptional(depositId, recordedAt);
        if (depositId == null || depositId.isBlank()) return null;
        return bankTransactions.allocate(depositId, amountCents, type, objectId, actor);
    }
}
