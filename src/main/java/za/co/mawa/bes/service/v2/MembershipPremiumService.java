package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.MembershipPremiumResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.PremiumStatus;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.mapper.v2.MembershipPremiumMapper;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;
import za.co.mawa.bes.repository.v2.ReceiptAllocationRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipPremiumService {

    private final MembershipPremiumRepository membershipPremiumRepository;
    private final MembershipService membershipService;
    private final ReceiptAllocationRepository receiptAllocationRepository;
    private final ReceiptRepository receiptRepository;
    private final MembershipPremiumMapper membershipPremiumMapper;
    private final ReceiptMapper receiptMapper;

    public List<MembershipPremiumEntity> getPremiumsForMembership(String membershipId) {
        return membershipPremiumRepository.findByMembershipIdInOrderByPeriodYYYYMMAsc(
                membershipService.membershipIdentifiers(membershipId)
        );
    }

    public List<MembershipPremiumResponseDto> getPremiumHistory(String membershipId) {
        List<String> membershipIds = membershipService.membershipIdentifiers(membershipId);
        List<MembershipPremiumEntity> premiums =
                membershipPremiumRepository.findByMembershipIdInOrderByPeriodYYYYMMAsc(membershipIds);
        if (premiums.isEmpty()) {
            return List.of();
        }

        List<ReceiptAllocationEntity> allocations =
                receiptAllocationRepository.findByMembershipIdInOrderByCreatedAtDesc(membershipIds).stream()
                        .filter(allocation -> allocation.getStatus() == ReceiptStatus.POSTED)
                        .toList();

        List<String> receiptIds = allocations.stream()
                .map(ReceiptAllocationEntity::getReceiptId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, ReceiptEntity> receiptsById = receiptRepository.findAllById(receiptIds).stream()
                .collect(Collectors.toMap(ReceiptEntity::getId, Function.identity()));

        Map<String, List<ReceiptAllocationEntity>> allocationsByPremium = new HashMap<>();
        for (ReceiptAllocationEntity allocation : allocations) {
            String referenceId = trim(allocation.getReferenceId());
            if (!referenceId.isEmpty()) {
                allocationsByPremium.computeIfAbsent(referenceId, ignored -> new java.util.ArrayList<>())
                        .add(allocation);
            }
        }

        return premiums.stream().map(premium -> {
            MembershipPremiumResponseDto response = membershipPremiumMapper.toResponse(premium);
            List<ReceiptAllocationEntity> premiumAllocations =
                    allocationsByPremium.getOrDefault(premium.getId(), List.of());

            // Legacy/manual rows may not carry the premium id as reference. Fall
            // back to the membership + period tuple used by the receipt ledger.
            if (premiumAllocations.isEmpty()) {
                premiumAllocations = allocations.stream()
                        .filter(allocation -> Objects.equals(
                                trim(allocation.getPeriodYYYYMM()),
                                trim(premium.getPeriodYYYYMM())))
                        .filter(allocation -> membershipIds.contains(trim(allocation.getMembershipId())))
                        .toList();
            }

            ReceiptEntity latestReceipt = premiumAllocations.stream()
                    .map(allocation -> receiptsById.get(allocation.getReceiptId()))
                    .filter(Objects::nonNull)
                    .filter(receipt -> receipt.getStatus() == ReceiptStatus.POSTED)
                    .max((left, right) -> safeDate(left).compareTo(safeDate(right)))
                    .orElse(null);

            response.setPaymentCount(premiumAllocations.size());
            if (latestReceipt != null) {
                response.setReceiptId(latestReceipt.getId());
                response.setReceiptNo(latestReceipt.getReceiptNo());
                response.setPaymentDate(latestReceipt.getReceiptDate());
                response.setPaymentMethod(latestReceipt.getPaymentMethod());
                response.setCashier(firstNonBlank(
                        latestReceipt.getOriginalCollector(),
                        latestReceipt.getEmployeeResponsible(),
                        latestReceipt.getCapturedBy(),
                        latestReceipt.getCreatedBy()));
                response.setPaymentLocation(firstNonBlank(
                        latestReceipt.getLocationName(),
                        latestReceipt.getLocation()));
                response.setDeviceId(firstNonBlank(
                        latestReceipt.getTerminalId(),
                        latestReceipt.getDeviceId()));
            }
            return response;
        }).toList();
    }

    public List<ReceiptResponseDto> getPremiumReceipts(String membershipId, String premiumId) {
        List<String> membershipIds = membershipService.membershipIdentifiers(membershipId);
        MembershipPremiumEntity premium = membershipPremiumRepository.findById(premiumId)
                .orElseThrow(() -> new IllegalArgumentException("Premium not found: " + premiumId));
        if (!membershipIds.contains(trim(premium.getMembershipId()))) {
            throw new IllegalArgumentException("Premium does not belong to membership: " + membershipId);
        }

        List<ReceiptAllocationEntity> allocations = receiptAllocationRepository
                .findByAllocationTypeAndReferenceIdOrderByCreatedAtAsc(
                        za.co.mawa.bes.enums.ReceiptAllocationType.MEMBERSHIP_PREMIUM, premium.getId())
                .stream()
                .filter(allocation -> allocation.getStatus() == ReceiptStatus.POSTED)
                .toList();

        if (allocations.isEmpty()) {
            allocations = receiptAllocationRepository.findByMembershipIdInOrderByCreatedAtDesc(membershipIds).stream()
                    .filter(allocation -> allocation.getStatus() == ReceiptStatus.POSTED)
                    .filter(allocation -> Objects.equals(trim(allocation.getPeriodYYYYMM()), trim(premium.getPeriodYYYYMM())))
                    .toList();
        }

        Map<String, List<ReceiptAllocationEntity>> byReceipt = allocations.stream()
                .filter(allocation -> allocation.getReceiptId() != null)
                .collect(Collectors.groupingBy(ReceiptAllocationEntity::getReceiptId));
        Map<String, ReceiptEntity> receipts = receiptRepository.findAllById(byReceipt.keySet()).stream()
                .filter(receipt -> receipt.getStatus() == ReceiptStatus.POSTED)
                .collect(Collectors.toMap(ReceiptEntity::getId, Function.identity()));

        return receipts.values().stream()
                .sorted((left, right) -> safeDate(right).compareTo(safeDate(left)))
                .map(receipt -> receiptMapper.toDto(receipt, byReceipt.getOrDefault(receipt.getId(), List.of())))
                .toList();
    }

    private static LocalDateTime safeDate(ReceiptEntity receipt) {
        if (receipt.getReceiptDate() != null) return receipt.getReceiptDate();
        if (receipt.getCreatedAt() != null) return receipt.getCreatedAt();
        return LocalDateTime.MIN;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public List<MembershipPremiumEntity> getUnpaidPremiums(String membershipId) {
        return membershipPremiumRepository.findByMembershipIdInAndStatusInOrderByPeriodYYYYMMAsc(
                membershipService.membershipIdentifiers(membershipId),
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

        var membership = membershipService.resolveMembership(membershipId);
        List<String> membershipIds = membershipService.membershipIdentifiers(membershipId);
        List<MembershipPremiumEntity> existingPremiums =
                membershipPremiumRepository.findByMembershipIdInAndPeriodYYYYMMOrderByMembershipIdAsc(
                        membershipIds,
                        periodYYYYMM
                );

        return existingPremiums.stream()
                .filter(premium -> membership.getId().equals(premium.getMembershipId()))
                .findFirst()
                .or(() -> existingPremiums.stream().findFirst())
                .orElseGet(() -> {
                    MembershipPremiumEntity premium = new MembershipPremiumEntity();
                    premium.setMembershipId(membership.getId());
                    premium.setPeriodYYYYMM(periodYYYYMM);
                    premium.setAmountCents(amountCents);
                    premium.setPaidAmountCents(0L);
                    premium.setBalanceCents(amountCents);
                    premium.setStatus(PremiumStatus.UNPAID);
                    premium.setDueDate(LocalDate.now());
                    premium.setCreatedAt(LocalDateTime.now());
                    premium.setCreatedBy(createdBy);
                    MembershipPremiumEntity saved = membershipPremiumRepository.save(premium);
                    membershipService.recalculatePaidUpToPeriod(membership.getId());
                    return saved;
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

        // Flush the premium before recalculating the membership. This makes the
        // PAID status visible even when the surrounding device-sync transaction
        // is running with deferred JPA flushing.
        MembershipPremiumEntity saved = membershipPremiumRepository.saveAndFlush(premium);
        membershipService.recalculatePaidUpToPeriod(saved.getMembershipId());
        return saved;
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

        MembershipPremiumEntity saved = membershipPremiumRepository.saveAndFlush(premium);
        membershipService.recalculatePaidUpToPeriod(saved.getMembershipId());
        return saved;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }
}