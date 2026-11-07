package za.co.mawa.bes.controller.v2;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.v2.BankStatementMailConfigurationService;
import za.co.mawa.bes.service.v2.BankStatementMailboxService;
import za.co.mawa.bes.service.v2.BankTransactionService;
import za.co.mawa.bes.service.v2.Microsoft365SaasConnectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/v2/bank-transactions") @RequiredArgsConstructor
public class BankTransactionController {
    private final BankTransactionService transactions;
    private final BankStatementMailConfigurationService settings;
    private final BankStatementMailboxService mailbox;
    private final Microsoft365SaasConnectionService microsoftSaas;
    private final EntityManager entityManager;

    /** Permissions verified against current tenant user-role and workcenter assignments. */
    private void require(String workcenter) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.isBlank() || UserContext.isBackgroundSession()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated ERP user required");
        }
        Number count = (Number) entityManager.createNativeQuery("""
            SELECT COUNT(*) FROM user_role ur JOIN `role` r ON r.id = ur.role
            LEFT JOIN role_workcenter rw ON rw.`role` = ur.role AND rw.workcenter = :workcenter
            WHERE ur.user = :userId
              AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
              AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
              AND (r.access_all_workcentres = b'1' OR rw.`role` IS NOT NULL)
            """).setParameter("userId", userId).setParameter("workcenter", workcenter).getSingleResult();
        if (count.longValue() == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "The required bank transactions workcenter is not assigned to your role");
    }

    @GetMapping
    public Page<BankTransactionService.BankDeposit> search(@RequestParam(defaultValue = "") String search,
           @RequestParam(defaultValue = "false") boolean creditsOnly,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
           @RequestParam(required = false) Long amountCents,
           @RequestParam(defaultValue = "0") int page,
           @RequestParam(defaultValue = "30") int size) {
        require("bank-transactions");
        return transactions.search(search, creditsOnly, fromDate, toDate, amountCents,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)),
                        Sort.by(Sort.Direction.DESC, "transactionDate", "id")));
    }

    @GetMapping("/{depositId}")
    public BankTransactionService.BankDeposit get(@PathVariable String depositId) {
        require("bank-transactions");
        return transactions.get(depositId);
    }

    @GetMapping("/{depositId}/allocations")
    public List<BankTransactionService.Allocation> allocations(@PathVariable String depositId) {
        require("bank-transactions");
        return transactions.allocations(depositId);
    }

    @GetMapping("/config")
    public BankStatementMailConfigurationService.Configuration config() {
        require("bank-statement-mail-settings");
        return settings.get();
    }

    @PutMapping("/config")
    public BankStatementMailConfigurationService.Configuration saveConfig(
            @RequestBody BankStatementMailConfigurationService.Configuration input) {
        require("bank-statement-mail-settings");
        return settings.save(input);
    }

    @PostMapping("/sync")
    public BankStatementMailboxService.SyncResult sync() {
        require("bank-statement-mail-settings");
        return mailbox.sync();
    }

    @GetMapping("/microsoft/info")
    public Microsoft365SaasConnectionService.ApplicationInfo microsoftInfo() {
        require("bank-statement-mail-settings");
        return microsoftSaas.appInfo();
    }

    @PostMapping("/microsoft/connect")
    public Microsoft365SaasConnectionService.ConnectionStart connectMicrosoft() {
        require("bank-statement-mail-settings");
        return microsoftSaas.start();
    }

    /** Public Microsoft redirect: the callback filter resolves only the schema from state;
     * the service verifies the random state, signed ID token, issuer, audience, and nonce. */
    @GetMapping("/microsoft/callback")
    public ResponseEntity<Void> microsoftCallback(@RequestParam(required = false) String code,
                                                    @RequestParam(required = false) String state,
                                                    @RequestParam(required = false) String error) {
        if (error != null || code == null || code.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Microsoft authorization was not completed");
        var result = microsoftSaas.complete(state, code);
        String suffix = "VERIFIED".equals(result.status()) ? "?ms365=verified" : "?ms365=mailbox-access-required";
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, result.returnUrl() + suffix)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @PostMapping("/microsoft/verify")
    public Microsoft365SaasConnectionService.Verification verifyMicrosoft() {
        require("bank-statement-mail-settings");
        return microsoftSaas.verifyMailbox();
    }

    @PostMapping("/microsoft/disconnect")
    public Microsoft365SaasConnectionService.Verification disconnectMicrosoft() {
        require("bank-statement-mail-settings");
        return microsoftSaas.disconnect();
    }

}
