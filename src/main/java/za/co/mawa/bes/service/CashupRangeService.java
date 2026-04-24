package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.CashupRangeAllocationEntity;
import za.co.mawa.bes.entity.CashupSequenceEntity;
import za.co.mawa.bes.entity.ReceiptRangeAllocationEntity;
import za.co.mawa.bes.entity.ReceiptSequenceEntity;
import za.co.mawa.bes.repository.CashupRangeAllocationRepository;
import za.co.mawa.bes.repository.CashupSequenceRepository;
import za.co.mawa.bes.repository.ReceiptRangeAllocationRepository;
import za.co.mawa.bes.repository.ReceiptSequenceRepository;

@Service
public class CashupRangeService {

    public static final int DEFAULT_BLOCK_SIZE = 100;

    private final CashupSequenceRepository seqRepo;
    private final CashupRangeAllocationRepository allocRepo;

    public CashupRangeService(CashupSequenceRepository seqRepo,
                              CashupRangeAllocationRepository allocRepo) {
        this.seqRepo = seqRepo;
        this.allocRepo = allocRepo;
    }

    public record AllocateRequest(String deviceId, Integer blockSize) {}
    public record AllocateResponse(String deviceId, long fromNo, long toNo, long nextNo) {}

    /**
     * Allocates a contiguous block of cashup numbers for a device (idempotent).
     * Multitenancy schema selection is assumed to be already set up upstream.
     */
    @Transactional
    public AllocateResponse allocate(AllocateRequest req) {
        final String deviceId = req.deviceId() == null ? "" : req.deviceId().trim();
        if (deviceId.isEmpty()) throw new IllegalArgumentException("deviceId is required");

        final int blockSize = (req.blockSize() == null) ? DEFAULT_BLOCK_SIZE : req.blockSize();
        if (blockSize <= 0 || blockSize > 100_000) {
            throw new IllegalArgumentException("blockSize out of range");
        }

        // 1) Idempotent: return active allocation if exists
        final var existing = allocRepo.findByDeviceIdAndStatus(deviceId, "ACTIVE");
        if (existing.isPresent()) {
            final var a = existing.get();
            return new AllocateResponse(a.getDeviceId(), a.getFromNo(), a.getToNo(), a.getNextNo());
        }

        // 2) Lock sequence row (per tenant schema)
        var seq = seqRepo.findForUpdate()
                .orElseGet(() -> seqRepo.save(new CashupSequenceEntity(1, 1000000000)));

        // If created above, ensure we now have it locked in this TX
        seq = seqRepo.findForUpdate().orElseThrow();

        final long from = seq.getNextNo();
        final long to = from + blockSize - 1;

        // 3) Advance sequence
        seq.setNextNo(to + 1);
        seqRepo.save(seq);

        // 4) Create allocation
        final var alloc = new CashupRangeAllocationEntity(deviceId, from, to);
        allocRepo.save(alloc);

        return new AllocateResponse(deviceId, from, to, alloc.getNextNo());
    }
}
