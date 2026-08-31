package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.PremiumStatus;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.mapper.v2.MembershipPremiumMapper;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;
import za.co.mawa.bes.repository.v2.ReceiptAllocationRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipPremiumServiceTest {

    @Mock MembershipPremiumRepository premiumRepository;
    @Mock MembershipService membershipService;
    @Mock ReceiptAllocationRepository allocationRepository;
    @Mock ReceiptRepository receiptRepository;
    @Mock MembershipPremiumMapper premiumMapper;
    @Mock ReceiptMapper receiptMapper;
    @InjectMocks MembershipPremiumService service;

    @Test
    void reversedReceiptAndAllocationDoNotSettlePremium() {
        MembershipPremiumEntity premium = premium("premium-1");
        ReceiptAllocationEntity reversed = allocation("allocation-1", "receipt-14092", ReceiptStatus.REVERSED);
        ReceiptEntity receipt = receipt("receipt-14092", ReceiptStatus.REVERSED);

        when(allocationRepository.findByAllocationTypeAndReferenceIdOrderByCreatedAtAsc(
                ReceiptAllocationType.MEMBERSHIP_PREMIUM, premium.getId()))
                .thenReturn(List.of(reversed));
        when(receiptRepository.findAllById(any())).thenReturn(List.of(receipt));
        when(premiumRepository.saveAndFlush(premium)).thenReturn(premium);

        service.reconcileFromPostedAllocations(premium, "tester");

        assertEquals(0L, premium.getPaidAmountCents());
        assertEquals(10_000L, premium.getBalanceCents());
        assertEquals(PremiumStatus.UNPAID, premium.getStatus());
        verify(membershipService).recalculatePaidUpToPeriod("membership-1");
    }

    @Test
    void onlyPostedAllocationBackedByPostedReceiptSettlesPremium() {
        MembershipPremiumEntity premium = premium("premium-1");
        ReceiptAllocationEntity reversed = allocation("allocation-1", "receipt-14092", ReceiptStatus.REVERSED);
        ReceiptAllocationEntity posted = allocation("allocation-2", "receipt-14093", ReceiptStatus.POSTED);

        when(allocationRepository.findByAllocationTypeAndReferenceIdOrderByCreatedAtAsc(
                ReceiptAllocationType.MEMBERSHIP_PREMIUM, premium.getId()))
                .thenReturn(List.of(reversed, posted));
        when(receiptRepository.findAllById(any()))
                .thenReturn(List.of(receipt("receipt-14093", ReceiptStatus.POSTED)));
        when(premiumRepository.saveAndFlush(premium)).thenReturn(premium);

        service.reconcileFromPostedAllocations(premium, "tester");

        assertEquals(10_000L, premium.getPaidAmountCents());
        assertEquals(0L, premium.getBalanceCents());
        assertEquals(PremiumStatus.PAID, premium.getStatus());
    }

    @Test
    void legacyAllocationWithoutReferenceUsesMembershipAndPeriod() {
        MembershipPremiumEntity premium = premium("premium-1");
        premium.setPaidAmountCents(0L);
        premium.setBalanceCents(10_000L);
        premium.setStatus(PremiumStatus.UNPAID);
        ReceiptAllocationEntity legacy = allocation("allocation-legacy", "receipt-legacy", ReceiptStatus.POSTED);
        legacy.setReferenceId(null);
        legacy.setMembershipId("membership-1");
        legacy.setPeriodYYYYMM("202608");

        when(membershipService.membershipIdentifiers("membership-1"))
                .thenReturn(List.of("membership-1"));
        when(allocationRepository.findByAllocationTypeAndReferenceIdOrderByCreatedAtAsc(
                ReceiptAllocationType.MEMBERSHIP_PREMIUM, premium.getId()))
                .thenReturn(List.of());
        when(allocationRepository.findByMembershipIdInOrderByCreatedAtDesc(List.of("membership-1")))
                .thenReturn(List.of(legacy));
        when(receiptRepository.findAllById(any()))
                .thenReturn(List.of(receipt("receipt-legacy", ReceiptStatus.POSTED)));
        when(premiumRepository.saveAndFlush(premium)).thenReturn(premium);

        service.reconcileFromPostedAllocations(premium, "tester");

        assertEquals(10_000L, premium.getPaidAmountCents());
        assertEquals(0L, premium.getBalanceCents());
        assertEquals(PremiumStatus.PAID, premium.getStatus());
    }

    private MembershipPremiumEntity premium(String id) {
        MembershipPremiumEntity premium = new MembershipPremiumEntity();
        premium.setId(id);
        premium.setMembershipId("membership-1");
        premium.setPeriodYYYYMM("202608");
        premium.setAmountCents(10_000L);
        premium.setPaidAmountCents(10_000L);
        premium.setBalanceCents(0L);
        premium.setStatus(PremiumStatus.PAID);
        return premium;
    }

    private ReceiptAllocationEntity allocation(String id, String receiptId, ReceiptStatus status) {
        ReceiptAllocationEntity allocation = new ReceiptAllocationEntity();
        allocation.setId(id);
        allocation.setReceiptId(receiptId);
        allocation.setReferenceId("premium-1");
        allocation.setAllocationType(ReceiptAllocationType.MEMBERSHIP_PREMIUM);
        allocation.setAmountCents(10_000L);
        allocation.setStatus(status);
        return allocation;
    }

    private ReceiptEntity receipt(String id, ReceiptStatus status) {
        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setId(id);
        receipt.setStatus(status);
        return receipt;
    }
}
