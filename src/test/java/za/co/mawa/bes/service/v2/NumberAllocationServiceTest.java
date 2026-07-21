package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.numbering.NumberAllocationRequest;
import za.co.mawa.bes.dto.v2.numbering.NumberAllocationResponse;
import za.co.mawa.bes.entity.v2.NumberRangeAllocationEntity;
import za.co.mawa.bes.entity.v2.NumberSequenceEntity;
import za.co.mawa.bes.repository.v2.NumberRangeAllocationRepository;
import za.co.mawa.bes.repository.v2.NumberSequenceRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NumberAllocationServiceTest {

    @Mock
    private NumberSequenceRepository sequenceRepository;
    @Mock
    private NumberRangeAllocationRepository allocationRepository;

    private NumberAllocationService service;

    @BeforeEach
    void setUp() {
        service = new NumberAllocationService(sequenceRepository, allocationRepository);
        UserContext.setCurrentUserId("user-1");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void allocateUsesConfiguredDefaultBlockSize() {
        NumberSequenceEntity sequence = sequence("RECEIPT", 100L, 1000L, 25, true);
        when(sequenceRepository.findBySeqTypeForUpdate("RECEIPT")).thenReturn(Optional.of(sequence));

        NumberAllocationRequest request = new NumberAllocationRequest();
        request.setSeqType("receipt");
        request.setDeviceId("DEVICE-1");

        NumberAllocationResponse response = service.allocate(request);

        assertEquals(100L, response.getFromNo());
        assertEquals(124L, response.getToNo());
        assertEquals(25, response.getAllocationSize());
        assertEquals(125L, sequence.getNextNo());
        verify(sequenceRepository).save(sequence);
        verify(allocationRepository).save(any(NumberRangeAllocationEntity.class));
    }

    @Test
    void allocationCannotExceedConfiguredEndNumber() {
        NumberSequenceEntity sequence = sequence("RECEIPT", 990L, 1000L, 25, true);
        when(sequenceRepository.findBySeqTypeForUpdate("RECEIPT")).thenReturn(Optional.of(sequence));

        NumberAllocationRequest request = new NumberAllocationRequest();
        request.setSeqType("RECEIPT");
        request.setDeviceId("DEVICE-1");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.allocate(request));

        assertEquals("Requested range exceeds the configured end number for RECEIPT. Remaining numbers: 11", error.getMessage());
        verify(sequenceRepository, never()).save(any());
        verify(allocationRepository, never()).save(any());
    }

    @Test
    void inactiveSequenceCannotIssueNumbers() {
        NumberSequenceEntity sequence = sequence("CASHUP", 100L, 1000L, 25, false);
        when(sequenceRepository.findBySeqTypeForUpdate("CASHUP")).thenReturn(Optional.of(sequence));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.allocateNumber("cashup")
        );

        assertEquals("Number sequence is inactive: CASHUP", error.getMessage());
        verify(sequenceRepository, never()).save(any());
    }

    private NumberSequenceEntity sequence(String seqType, long nextNo, long endNo, int allocationSize, boolean active) {
        return NumberSequenceEntity.builder()
                .id(1L)
                .seqType(seqType)
                .description("Receipt numbers")
                .startNo(1L)
                .nextNo(nextNo)
                .endNo(endNo)
                .defaultAllocationSize(allocationSize)
                .warningThreshold(100L)
                .active(active)
                .lockVersion(0L)
                .build();
    }
}
