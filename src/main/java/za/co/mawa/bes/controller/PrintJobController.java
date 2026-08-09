package za.co.mawa.bes.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Legacy IP-routed endpoint retained only to produce an explicit migration error. */
@Deprecated
@RestController
@RequestMapping("/print-job")
class PrintJobController {
    private ResponseEntity<String> retired() {
        return ResponseEntity.status(HttpStatus.GONE).body(
                "The IP-address print queue has been retired. Use /v2/pos-print-agents and /v2/receipts/{receiptId}/print-jobs."
        );
    }
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> retiredRoot() { return retired(); }
    @PostMapping("/{id}/complete")
    public ResponseEntity<String> retiredComplete(@PathVariable long id) { return retired(); }
}
