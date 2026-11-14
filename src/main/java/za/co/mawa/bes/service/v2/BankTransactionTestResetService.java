package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.UserAccessService;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * Reset of synthetic bank-import fixtures in a specifically allowlisted NON-PRODUCTION tenant.
 * Never deletes original GCS archives, historical processing attempts, financial business records
 * or bank allocations. Unavailable unless the separate test-reset controller is feature-enabled.
 */
@Service
@RequiredArgsConstructor
public class BankTransactionTestResetService {
    private static final Set<String> NON_PRODUCTION = Set.of(
            "local", "dev", "test", "qa", "staging", "preprod", "prep", "alpha", "beta", "hotfix");
    private final EntityManager entityManager;
    private final Environment environment;
    private final UserAccessService access;

    public record Preview(String tenantId, long transactions, long importEnvelopes, long filesToRearm,
                          long allocations, long paymentReferences, long cashupReferences,
                          long processingFiles, boolean canReset, String blockedReason) { }
    public record ResetRequest(String tenantId, String confirmation, long expectedTransactions,
                               long expectedImports, long expectedFilesToRearm) { }
    public record ResetResult(String tenantId, int transactionsRemoved, int importsRemoved,
                              int filesRearmed, String status) { }

    /** Fails closed if any environment/tenant/identity guard is absent. */
    void assertAllowed() {
        String runtime = environment.getProperty("MAWA_ENVIRONMENT", "").trim().toLowerCase(Locale.ROOT);
        if (!NON_PRODUCTION.contains(runtime))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Test bank reset is unavailable in this environment");
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Test bank reset is forbidden in production");
        }
        String cloudService = environment.getProperty("K_SERVICE", "").toLowerCase(Locale.ROOT);
        if (cloudService.matches(".*(\\bproduction\\b|[-_]prod($|[-_])).*"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Test bank reset is forbidden on production services");
        if (!cloudService.isBlank()) {
            String allowedServices = environment.getProperty("mawa.bank-test-reset.allowed-services", "");
            boolean serviceAllowlisted = Arrays.stream(allowedServices.split(","))
                    .map(v -> v.trim().toLowerCase(Locale.ROOT))
                    .anyMatch(cloudService::equals);
            if (!serviceAllowlisted)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cloud Run service is not allowlisted for bank reset testing");
        }
        if (!environment.getProperty("mawa.bank-test-reset.enabled", Boolean.class, false))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Test bank reset is disabled");
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank() || UserContext.getCurrentUserId() == null
                || UserContext.getCurrentUserId().isBlank() || UserContext.isBackgroundSession()
                || UserContext.isPlatformSession() || !access.isProtectedAdministrator())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Protected tenant administrator required");
        String allowlist = environment.getProperty("mawa.bank-test-reset.allowed-tenants", "");
        boolean allowlisted = Arrays.stream(allowlist.split(","))
                .map(String::trim).anyMatch(tenant::equals);
        if (!allowlisted)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant is not allowlisted for bank reset testing");
    }

    private long count(String sql) {
        Number n = (Number) entityManager.createNativeQuery(sql).getSingleResult();
        return n == null ? 0 : n.longValue();
    }

    private Preview counts() {
        long transactions = count("SELECT COUNT(*) FROM bank_transaction");
        long imports = count("SELECT COUNT(*) FROM bank_statement_import");
        long files = count("SELECT COUNT(*) FROM bank_statement_file WHERE storage_path IS NOT NULL AND status <> 'STORED'");
        long allocations = count("SELECT COUNT(*) FROM bank_transaction_allocation");
        long paymentRefs = count("SELECT COUNT(*) FROM payment_batch WHERE bank_deposit_id IS NOT NULL");
        long cashupRefs = count("SELECT COUNT(*) FROM cashup_deposit WHERE bank_deposit_id IS NOT NULL");
        long processing = count("SELECT COUNT(*) FROM bank_statement_file WHERE status = 'PROCESSING'");
        String block = null;
        if (allocations > 0 || paymentRefs > 0 || cashupRefs > 0)
            block = "Financial links or bank allocations exist; reset is blocked. Use a fresh test tenant.";
        else if (processing > 0)
            block = "A bank statement is currently processing. Try again after processing completes.";
        else if (transactions + imports + files == 0)
            block = "There is no imported bank history to reset.";
        return new Preview(TenantContext.getCurrentTenant(), transactions, imports, files,
                allocations, paymentRefs, cashupRefs, processing, block == null, block);
    }

    @Transactional(readOnly = true)
    public Preview preview() {
        assertAllowed();
        return counts();
    }

    /** Serializable transaction protects count-based preview confirmation against concurrent imports. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResetResult reset(ResetRequest request) {
        assertAllowed();
        Preview current = counts();
        if (request == null || !current.tenantId().equals(request.tenantId())
                || !("RESET BANK HISTORY: " + current.tenantId()).equals(request.confirmation()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant-specific reset confirmation is required");
        if (!current.canReset())
            throw new ResponseStatusException(HttpStatus.CONFLICT, current.blockedReason());
        if (request.expectedTransactions() != current.transactions()
                || request.expectedImports() != current.importEnvelopes()
                || request.expectedFilesToRearm() != current.filesToRearm())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bank data changed since preview; refresh and retry");

        // Foreign key order is essential: allocation/payment references are prohibited above.
        int removedTransactions = entityManager.createNativeQuery("DELETE FROM bank_transaction").executeUpdate();
        int removedImports = entityManager.createNativeQuery("DELETE FROM bank_statement_import").executeUpdate();
        // Keep immutable source files in GCS and ALL historical processing attempts.
        // Re-arm archived files so the existing Retry action can process them again.
        int rearmed = entityManager.createNativeQuery("""
                UPDATE bank_statement_file SET status = 'STORED', imported_count = 0,
                  csv_count = 0, failed_csv_count = 0, last_error = NULL,
                  last_processed_at = NULL
                WHERE storage_path IS NOT NULL AND status <> 'STORED'
                """).executeUpdate();
        access.audit("BANK_TEST_HISTORY_RESET", "BANK_TRANSACTION_HISTORY", current.tenantId(),
                "Synthetic test-data reset", "Deleted transactions=" + removedTransactions
                        + ", import envelopes=" + removedImports + ", archived files rearmed=" + rearmed);
        return new ResetResult(current.tenantId(), removedTransactions, removedImports, rearmed,
                "RESET_COMPLETE");
    }
}
