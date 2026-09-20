package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.repository.v2.CashupRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashupApprovalHandlerTest {

    @Mock private CashupRepository cashupRepository;
    @InjectMocks private CashupApprovalHandler handler;

    @Test
    void approvalRejectionReturnsCashupToAwaitingDeposits() {
        CashupEntity cashup = CashupEntity.builder()
                .id("cashup-1")
                .status("SUBMITTED")
                .approvalRequestId("approval-1")
                .build();
        ApprovalRequestEntity approval = ApprovalRequestEntity.builder()
                .id("approval-1")
                .referenceId("cashup-1")
                .build();
        when(cashupRepository.findById("cashup-1")).thenReturn(Optional.of(cashup));

        handler.onRejected(approval, "approver-1", "Attach the correct deposit proof");

        assertEquals("AWAITING_DEPOSITS", cashup.getStatus());
        assertNull(cashup.getApprovalRequestId());
        assertEquals("Attach the correct deposit proof", cashup.getNotes());
        assertEquals("approver-1", cashup.getUpdatedBy());
        verify(cashupRepository).save(cashup);
    }
}
