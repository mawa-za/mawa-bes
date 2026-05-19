package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
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
        int allocationSize = request.getAllocationSize();

        NumberSequenceEntity sequence = numberSequenceRepository
                .findBySeqTypeForUpdate(seqType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Number sequence not configured for type: " + seqType
                ));

        Long fromNo = sequence.getNextNo();
        Long toNo = fromNo + allocationSize - 1;
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
        allocation.setCreatedBy(request.getRequestedBy());

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
        normalizeSeqType(seqType);

        NumberSequenceEntity sequence = numberSequenceRepository
                .findBySeqTypeForUpdate(seqType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Number sequence not configured for type: " + seqType
                ));

        Long fromNo = sequence.getNextNo();
        Long nextNo = fromNo + 1;
        sequence.setNextNo(nextNo);
        numberSequenceRepository.save(sequence);
        return fromNo.toString();
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

        if (request.getAllocationSize() == null || request.getAllocationSize() <= 0) {
            throw new IllegalArgumentException("allocationSize must be greater than zero");
        }

        if (request.getAllocationSize() > 10000) {
            throw new IllegalArgumentException("allocationSize cannot exceed 10000");
        }
    }

    private String normalizeSeqType(String seqType) {
        return seqType.trim().toUpperCase();
    }
}
