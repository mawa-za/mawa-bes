package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.AgentResponse;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.EnrollmentCreateRequest;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.EnrollmentResponse;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.PrintJobResponse;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.PrinterResponse;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.PrinterConfigurationRequest;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.QueueReceiptRequest;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.TerminalAssignmentRequest;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.TerminalEnabledRequest;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.TerminalRegisterRequest;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.TerminalResponse;
import za.co.mawa.bes.service.v2.PosPrintingService;

import java.util.List;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/v2/pos-printing")
public class PosPrintingControllerV2 {

    private final PosPrintingService service;

    @PostMapping("/enrollments")
    public EnrollmentResponse enrollment(@RequestBody EnrollmentCreateRequest request) {
        return service.createEnrollment(request);
    }

    @GetMapping("/agents")
    public List<AgentResponse> agents() {
        return service.listAgents();
    }

    @GetMapping("/agents/{agentId}/printers")
    public List<PrinterResponse> printers(@PathVariable String agentId) {
        return service.printers(agentId);
    }

    @PutMapping("/printers/{printerId}")
    public PrinterResponse configurePrinter(
            @PathVariable String printerId,
            @RequestBody PrinterConfigurationRequest request
    ) {
        return service.configurePrinter(printerId, request);
    }

    @PostMapping("/agents/{agentId}/revoke")
    public void revoke(@PathVariable String agentId) {
        service.revokeAgent(agentId);
    }

    @GetMapping("/terminals")
    public List<TerminalResponse> terminals() {
        return service.listTerminals();
    }

    @PostMapping("/terminals/register")
    public TerminalResponse register(@RequestBody TerminalRegisterRequest request) {
        return service.registerTerminal(request);
    }

    @GetMapping("/terminals/by-key/{terminalKey}")
    public TerminalResponse terminal(@PathVariable String terminalKey) {
        return service.getTerminalByKey(terminalKey);
    }

    @PutMapping("/terminals/{terminalId}/assignment")
    public TerminalResponse assign(
            @PathVariable String terminalId,
            @RequestBody TerminalAssignmentRequest request
    ) {
        return service.assignTerminal(terminalId, request);
    }

    @PutMapping("/terminals/{terminalId}/enabled")
    public TerminalResponse setTerminalEnabled(
            @PathVariable String terminalId,
            @RequestBody TerminalEnabledRequest request
    ) {
        return service.setTerminalEnabled(terminalId, request);
    }

    @PostMapping("/terminals/{terminalId}/test-print")
    public PrintJobResponse testPrint(
            @PathVariable String terminalId,
            @RequestBody(required = false) QueueReceiptRequest request
    ) {
        return service.queueTestPrint(terminalId, request);
    }

    @GetMapping("/jobs")
    public List<PrintJobResponse> jobs() {
        return service.listJobs();
    }

    @PostMapping("/jobs/{jobId}/retry")
    public PrintJobResponse retry(@PathVariable String jobId) {
        return service.retry(jobId);
    }
}
