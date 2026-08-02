package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.numbering.NumberAllocationRequest;
import za.co.mawa.bes.dto.v2.numbering.NumberAllocationResponse;
import za.co.mawa.bes.entity.v2.NumberRangeAllocationEntity;
import za.co.mawa.bes.entity.v2.NumberSequenceEntity;
import za.co.mawa.bes.repository.v2.NumberRangeAllocationRepository;
import za.co.mawa.bes.repository.v2.NumberSequenceRepository;

@Service
public class NumberAllocationService {

    private final NumberSequenceRepository numberSequenceRepository;
    private final NumberRangeAllocationRepository numberRangeAllocationRepository;

    public NumberAllocationService(
            NumberSequenceRepository numberSequenceRepository,
            NumberRangeAllocationRepository numberRangeAllocationRepository
    ) {
        this.numberSequenceRepository = numberSequenceRepository;
        this.numberRangeAllocationRepository = numberRangeAllocationRepository;
    }

    @Transactional
    public NumberAllocationResponse allocate(NumberAllocationRequest request) {
        validateRequest(request);

        String seqType = normalizeSeqType(request.getSeqType());
        String deviceId = request.getDeviceId().trim();
        NumberSequenceEntity sequence = numberSequenceRepository
                .findBySeqTypeForUpdate(seqType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Number sequence not configured for type: " + seqType
                ));
        ensureAvailable(sequence);
        int allocationSize = request.getAllocationSize() == null
                ? sequence.getDefaultAllocationSize()
                : request.getAllocationSize();
        validateAllocationSize(allocationSize);

        Long fromNo = sequence.getNextNo();
        Long toNo;
        try {
            toNo = Math.addExact(fromNo, allocationSize - 1L);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Requested range is too large", ex);
        }
        if (toNo > sequence.getEndNo()) {
            throw new IllegalArgumentException(
                    "Requested range exceeds the configured end number for " + seqType
                            + ". Remaining numbers: " + Math.max(0L, sequence.getEndNo() - fromNo + 1L)
            );
        }
        Long nextNo = toNo + 1;

        sequence.setNextNo(nextNo);
        numberSequenceRepository.save(sequence);

        NumberRangeAllocationEntity allocation = new NumberRangeAllocationEntity();
        allocation.setSeqType(seqType);
        allocation.setDeviceId(deviceId);
        allocation.setFromNo(fromNo);
        allocation.setToNo(toNo);
        allocation.setNextLocalNo(fromNo);
        allocation.setAllocationSize(allocationSize);
        allocation.setStatus("ACTIVE");
        allocation.setCreatedBy(hasText(request.getRequestedBy()) ? request.getRequestedBy().trim() : currentActor());

        numberRangeAllocationRepository.save(allocation);

        return new NumberAllocationResponse(
                allocation.getSeqType(),
                allocation.getDeviceId(),
                allocation.getFromNo(),
                allocation.getToNo(),
                allocation.getNextLocalNo(),
                allocation.getAllocationSize(),
                allocation.getStatus()
        );
    }

    @Transactional
    public String allocateNumber(String seqType) {
        String normalizedSeqType = normalizeSeqType(seqType);

        NumberSequenceEntity sequence = numberSequenceRepository
                .findBySeqTypeForUpdate(normalizedSeqType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Number sequence not configured for type: " + normalizedSeqType
                ));
        ensureAvailable(sequence);

        Long fromNo = sequence.getNextNo();
        Long nextNo = fromNo + 1;
        sequence.setNextNo(nextNo);
        numberSequenceRepository.save(sequence);
        return format(sequence, fromNo);
    }

    public NumberAllocationResponse getLatestActiveRange(String deviceId, String seqType) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("deviceId is required");
        }

        if (seqType == null || seqType.trim().isEmpty()) {
            throw new IllegalArgumentException("seqType is required");
        }

        NumberRangeAllocationEntity allocation = numberRangeAllocationRepository
                .findFirstByDeviceIdAndSeqTypeAndStatusOrderByIdDesc(
                        deviceId.trim(),
                        normalizeSeqType(seqType),
                        "ACTIVE"
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active range found for deviceId=" + deviceId + ", seqType=" + seqType
                ));

        return new NumberAllocationResponse(
                allocation.getSeqType(),
                allocation.getDeviceId(),
                allocation.getFromNo(),
                allocation.getToNo(),
                allocation.getNextLocalNo(),
                allocation.getAllocationSize(),
                allocation.getStatus()
        );
    }

    private void validateRequest(NumberAllocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }

        if (request.getSeqType() == null || request.getSeqType().trim().isEmpty()) {
            throw new IllegalArgumentException("seqType is required");
        }

        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("deviceId is required");
        }

        if (request.getAllocationSize() != null) {
            validateAllocationSize(request.getAllocationSize());
        }
    }

    private void ensureAvailable(NumberSequenceEntity sequence) {
        if (!Boolean.TRUE.equals(sequence.getActive())) {
            throw new IllegalArgumentException("Number sequence is inactive: " + sequence.getSeqType());
        }
        if (sequence.getNextNo() > sequence.getEndNo()) {
            throw new IllegalArgumentException("Number sequence is exhausted: " + sequence.getSeqType());
        }
    }

    private void validateAllocationSize(int allocationSize) {
        if (allocationSize <= 0) {
            throw new IllegalArgumentException("allocationSize must be greater than zero");
        }
        if (allocationSize > 10000) {
            throw new IllegalArgumentException("allocationSize cannot exceed 10000");
        }
    }

    private String format(NumberSequenceEntity sequence, Long value) {
        String prefix = sequence.getPrefix() == null ? "" : sequence.getPrefix().trim();
        String separator = sequence.getSeparator() == null ? "" : sequence.getSeparator();
        int padding = sequence.getPaddingLength() == null ? 0 : sequence.getPaddingLength();
        String number = value.toString();
        if (padding > 0 && number.length() < padding) {
            number = "0".repeat(padding - number.length()) + number;
        }
        return prefix.isEmpty() ? number : prefix + separator + number;
    }

    private String currentActor() {
        if (hasText(UserContext.getCurrentUserId())) return UserContext.getCurrentUserId();
        if (hasText(UserContext.getCurrentUser())) return UserContext.getCurrentUser();
        return "SYSTEM";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeSeqType(String seqType) {
        if (!hasText(seqType)) throw new IllegalArgumentException("seqType is required");
        return seqType.trim().toUpperCase();
    }
}
