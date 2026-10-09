package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.*;

@Service @RequiredArgsConstructor
public class TenantPrintQueueWorker {
    private final PosPrintingService service;
    @Transactional(propagation=Propagation.REQUIRES_NEW) public PrintJobResponse claim(String agentId){ return service.claimTrusted(agentId); }
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void spooled(String agentId,String jobId,JobResultRequest r){service.markSpooledTrusted(agentId,jobId,r);}
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void failed(String agentId,String jobId,JobResultRequest r){service.markFailedTrusted(agentId,jobId,r);}
}
