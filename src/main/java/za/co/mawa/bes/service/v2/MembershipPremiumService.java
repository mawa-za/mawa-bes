package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.enums.PremiumStatus;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPremiumService {

    private final MembershipPremiumRepository membershipPremiumRepository;

    public List<MembershipPremiumEntity> getPremiumsForMembership(String membershipId) {
        return membershipPremiumRepository.findByMembershipIdOrderByPeriodYYYYMMAsc(membershipId);
    }

    public List<MembershipPremiumEntity> getUnpaidPremiums(String membershipId) {
        return membershipPremiumRepository.findByMembershipIdAndStatusInOrderByPeriodYYYYMMAsc(
                membershipId,
                List.of(PremiumStatus.UNPAID, PremiumStatus.PARTIALLY_PAID)
        );
    }

    public MembershipPremiumEntity findOrCreatePremium(
            String membershipId,
            String periodYYYYMM,
            Long amountCents,
            String createdBy
    ) {
        if (!PeriodUtil.isValidPeriod(periodYYYYMM)) {
            throw new RuntimeException("Invalid periodYYYYMM: " + periodYYYYMM);
        }

        return membershipPremiumRepository
                .findByMembershipIdAndPeriodYYYYMM(membershipId, periodYYYYMM)
                .orElseGet(() -> {
                    MembershipPremiumEntity premium = new MembershipPremiumEntity();
                    premium.setMembershipId(membershipId);
                    premium.setPeriodYYYYMM(periodYYYYMM);
                    premium.setAmountCents(amountCents);
                    premium.setPaidAmountCents(0L);
                    premium.setBalanceCents(amountCents);
                    premium.setStatus(PremiumStatus.UNPAID);
                    premium.setDueDate(LocalDate.now());
                    premium.setCreatedAt(LocalDateTime.now());
                    premium.setCreatedBy(createdBy);
                    return membershipPremiumRepository.save(premium);
                });
    }

    public MembershipPremiumEntity applyPayment(
            MembershipPremiumEntity premium,
            Long amountCents,
            String updatedBy
    ) {
        if (premium.getStatus() == PremiumStatus.CANCELLED || premium.getStatus() == PremiumStatus.REVERSED) {
            throw new RuntimeException("Cannot pay premium with status: " + premium.getStatus());
        }

        long paidAmount = safe(premium.getPaidAmountCents()) + safe(amountCents);
        long balance = safe(premium.getAmountCents()) - paidAmount;

        premium.setPaidAmountCents(paidAmount);
        premium.setBalanceCents(Math.max(balance, 0L));

        if (premium.getBalanceCents() <= 0) {
            premium.setStatus(PremiumStatus.PAID);
        } else {
            premium.setStatus(PremiumStatus.PARTIALLY_PAID);
        }

        premium.setUpdatedAt(LocalDateTime.now());
        premium.setUpdatedBy(updatedBy);

        return membershipPremiumRepository.save(premium);
    }

    public MembershipPremiumEntity reversePayment(
            MembershipPremiumEntity premium,
            Long amountCents,
            String updatedBy
    ) {
        long paidAmount = safe(premium.getPaidAmountCents()) - safe(amountCents);
        paidAmount = Math.max(paidAmount, 0L);

        long balance = safe(premium.getAmountCents()) - paidAmount;
        balance = Math.max(balance, 0L);

        premium.setPaidAmountCents(paidAmount);
        premium.setBalanceCents(balance);

        if (paidAmount <= 0) {
            premium.setStatus(PremiumStatus.UNPAID);
        } else if (balance > 0) {
            premium.setStatus(PremiumStatus.PARTIALLY_PAID);
        } else {
            premium.setStatus(PremiumStatus.PAID);
        }

        premium.setUpdatedAt(LocalDateTime.now());
        premium.setUpdatedBy(updatedBy);

        return membershipPremiumRepository.save(premium);
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }
}