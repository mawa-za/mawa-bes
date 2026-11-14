package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.BankTransactionTestResetService;

/** Not registered at all unless the test-only flag is enabled. Guards also run inside service. */
@RestController
@RequestMapping("/v2/bank-transactions/test-reset")
@ConditionalOnProperty(name = "mawa.bank-test-reset.enabled", havingValue = "true")
@RequiredArgsConstructor
public class BankTransactionTestResetController {
    private final BankTransactionTestResetService reset;

    @GetMapping("/preview")
    public BankTransactionTestResetService.Preview preview() {
        return reset.preview();
    }

    @PostMapping
    public BankTransactionTestResetService.ResetResult reset(
            @RequestBody BankTransactionTestResetService.ResetRequest request) {
        return reset.reset(request);
    }
}
