package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.repository.v2.MembershipClaimLinkRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** Reprinting a paid claim must not alter the claim, its processor or its payment state. */
@ExtendWith(MockitoExtension.class)
class MembershipClaimFormReprintTest {
    @Mock MembershipClaimRepository claimRepository;
    @Mock MembershipClaimLinkRepository claimLinkRepository;
    @Mock MembershipActionGuardService membershipActionGuardService;
    @InjectMocks MembershipClaimService service;

    private MembershipClaimEntity claim(MembershipClaimType type, MembershipClaimStatus status) {
        var entity = new MembershipClaimEntity();
        entity.setId("CLAIM-1");
        entity.setClaimNo("1000002843");
        entity.setMembershipId(""); // No external membership lookup required for this unit test.
        entity.setDeceasedPartnerId("");
        entity.setClaimantPartnerId("");
        entity.setClaimType(type);
        entity.setStatus(status);
        entity.setClaimAmountCents(190000L);
        entity.setClaimFormDownloadCount(1);
        entity.setUpdatedBy("original-processor");
        when(claimRepository.findById("CLAIM-1")).thenReturn(Optional.of(entity));
        when(claimLinkRepository.findByParentClaimIdOrderByCreatedAtAsc("CLAIM-1"))
                .thenReturn(List.of());
        return entity;
    }

    @Test
    void paidCashClaimCanBeReprintedWithoutMutatingPaymentOrProcessor() {
        var entity = claim(MembershipClaimType.CASH, MembershipClaimStatus.PAID);

        var result = service.recordClaimFormDownload("CLAIM-1", "reprint-user");

        assertEquals(MembershipClaimStatus.PAID, result.getStatus());
        assertEquals(1, entity.getClaimFormDownloadCount());
        assertEquals("original-processor", entity.getUpdatedBy());
        verify(claimRepository, never()).save(any());
        verifyNoInteractions(membershipActionGuardService);
    }

    @Test
    void paidFuneralClaimCanBeReprintedWithoutChangingStatus() {
        var entity = claim(MembershipClaimType.FUNERAL, MembershipClaimStatus.PAID);

        var result = service.recordClaimFormDownload("CLAIM-1", "reprint-user");

        assertEquals(MembershipClaimStatus.PAID, result.getStatus());
        assertEquals("original-processor", entity.getUpdatedBy());
        verify(claimRepository, never()).save(any());
        verifyNoInteractions(membershipActionGuardService);
    }

    @Test
    void draftCashClaimStillRecordsFirstDownload() {
        var entity = claim(MembershipClaimType.CASH, MembershipClaimStatus.DRAFT);
        entity.setClaimFormDownloadCount(0);
        when(claimRepository.save(entity)).thenReturn(entity);

        var result = service.recordClaimFormDownload("CLAIM-1", "capturer");

        assertEquals(MembershipClaimStatus.DRAFT, result.getStatus());
        assertEquals(1, entity.getClaimFormDownloadCount());
        assertEquals("capturer", entity.getUpdatedBy());
        verify(membershipActionGuardService).requireActionable("");
        verify(claimRepository).save(entity);
    }
}
