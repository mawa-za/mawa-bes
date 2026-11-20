package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import za.co.mawa.bes.dto.creditnote.CreditNoteIssueRequestDto;
import za.co.mawa.bes.dto.v2.funeral.FuneralFundingReopenRequestDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.v2.FuneralServiceEntity;
import za.co.mawa.bes.entity.v2.FuneralServiceInvoiceEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.v2.FuneralServiceClaimRepository;
import za.co.mawa.bes.repository.v2.FuneralServiceInvoiceRepository;
import za.co.mawa.bes.repository.v2.FuneralServiceRepository;
import za.co.mawa.bes.service.CreditNoteService;
import za.co.mawa.bes.service.UserAccessService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuneralFundingAmendmentTest {
    @Mock JdbcTemplate jdbcTemplate;
    @Mock FuneralServiceRepository funeralServiceRepository;
    @Mock FuneralServiceInvoiceRepository funeralServiceInvoiceRepository;
    @Mock FuneralServiceClaimRepository funeralServiceClaimRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock CreditNoteService creditNoteService;
    @Mock UserAccessService userAccessService;
    @InjectMocks FuneralManagementService service;

    @Test
    void reasonIsMandatoryBeforeAnyFinancialTransaction() {
        FuneralFundingReopenRequestDto request = reason("no");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.reopenFuneralFunding("fsr-1", request));
        assertTrue(failure.getMessage().contains("reason"));
        verifyNoInteractions(creditNoteService, funeralServiceInvoiceRepository, jdbcTemplate);
    }

    @Test
    void existingCoverPaymentRequestBlocksAllCreditNotes() {
        mockServiceAndFinancialState("fsr-1", "pay-1");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.reopenFuneralFunding("fsr-1", reason("Package upgraded")));
        assertTrue(failure.getMessage().contains("payment request"));
        verifyNoInteractions(creditNoteService);
    }

    @Test
    void eligibleReopenCreditsOriginalInvoiceWithoutDeletingInvoiceOrPayment() {
        FuneralServiceInvoiceEntity link = mockServiceAndFinancialState("fsr-1", null);
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId("inv-1");
        invoice.setInvoiceNo("INV-0001");
        invoice.setTotalCents(25_000L);
        invoice.setCreditedCents(0L);
        invoice.setPaidCents(25_000L); // cash receipt must remain auditable
        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice));
        when(funeralServiceRepository.save(any(FuneralServiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.reopenFuneralFunding("fsr-1", reason("Upgrade and use another funeral cover"));

        ArgumentCaptor<CreditNoteIssueRequestDto> credit = ArgumentCaptor.forClass(CreditNoteIssueRequestDto.class);
        verify(creditNoteService).issue(eq("inv-1"), credit.capture(), anyString());
        assertEquals(25_000L, credit.getValue().getAmountCents());
        assertTrue(credit.getValue().getReason().contains("MWE-117"));
        assertNotNull(link.getSupersededAt());
        verify(funeralServiceInvoiceRepository).save(link);
        verify(jdbcTemplate).update(contains("INSERT INTO funeral_funding_amendment"),
                anyString(), eq("fsr-1"), eq("package-1"), eq(25_000L),
                eq("Upgrade and use another funeral cover"), anyString());
        assertEquals("ARRANGEMENT_CREATED", result.getStatus());
        assertEquals(25_000L, invoice.getPaidCents());
        verify(invoiceRepository, never()).delete(any(InvoiceEntity.class));
    }

    @Test
    void retryAfterSupersedingInvoicesDoesNotIssueAnotherCredit() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("fsr-1")))
                .thenReturn(List.of("fsr-1"));
        FuneralServiceEntity funeral = new FuneralServiceEntity();
        funeral.setId("fsr-1");
        when(funeralServiceRepository.findById("fsr-1")).thenReturn(Optional.of(funeral));
        when(funeralServiceInvoiceRepository.findByFuneralServiceIdAndSupersededAtIsNull("fsr-1"))
                .thenReturn(List.of());

        service.reopenFuneralFunding("fsr-1", reason("Retry of approved amendment"));

        verifyNoInteractions(creditNoteService);
        verify(jdbcTemplate, never()).update(contains("INSERT INTO funeral_funding_amendment"), any(Object[].class));
    }

    private FuneralServiceInvoiceEntity mockServiceAndFinancialState(String id, String paymentRequestId) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(id))).thenReturn(List.of(id));
        FuneralServiceEntity funeral = new FuneralServiceEntity();
        funeral.setId(id);
        funeral.setServiceRequestNo("FSR-001");
        funeral.setPackageId("package-1");
        funeral.setTotalAmountCents(25_000L);
        funeral.setStatus("INVOICED");
        when(funeralServiceRepository.findById(id)).thenReturn(Optional.of(funeral));
        FuneralServiceInvoiceEntity invoice = new FuneralServiceInvoiceEntity();
        invoice.setFuneralServiceId(id);
        invoice.setInvoiceId("inv-1");
        invoice.setPaymentRequestId(paymentRequestId);
        when(funeralServiceInvoiceRepository.findByFuneralServiceIdAndSupersededAtIsNull(id))
                .thenReturn(List.of(invoice));
        when(funeralServiceClaimRepository.findByFuneralServiceId(id)).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(id))).thenReturn(0L);
        return invoice;
    }

    private static FuneralFundingReopenRequestDto reason(String text) {
        FuneralFundingReopenRequestDto request = new FuneralFundingReopenRequestDto();
        request.setReason(text);
        return request;
    }
}
