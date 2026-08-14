package za.co.mawa.bes.fnb.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.enums.PaymentRequestType;
import za.co.mawa.bes.fnb.FnbApiCallLogger;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TransactionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankPaymentServicePaymentReferenceTest {

    @Mock private SettingService settingService;
    @Mock private TransactionService transactionService;
    @Mock private ObjectMapper objectMapper;
    @Mock private GcpTenantSecretService gcpTenantSecretService;
    @Mock private FnbApiCallLogger fnbApiCallLogger;
    @Mock private JdbcTemplate jdbcTemplate;

    private BankPaymentService service;

    @BeforeEach
    void setUp() {
        service = new BankPaymentService(
                settingService,
                transactionService,
                objectMapper,
                gcpTenantSecretService,
                fnbApiCallLogger,
                jdbcTemplate
        );
    }

    @Test
    void supplierAccountNumberIdentityOverridesCapturedReference() {
        PaymentRequestEntity payment = supplierPayment();
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("supplier-1")))
                .thenReturn(List.of("SUP-ACC-9001"));

        String reference = ReflectionTestUtils.invokeMethod(service, "resolvePaymentReference", payment);

        assertEquals("SUP-ACC-9001", reference);
    }

    @Test
    void supplierFallsBackToCapturedReferenceWhenAccountNumberIdentityIsMissing() {
        PaymentRequestEntity payment = supplierPayment();
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("supplier-1")))
                .thenReturn(List.of());

        String reference = ReflectionTestUtils.invokeMethod(service, "resolvePaymentReference", payment);

        assertEquals("CAPTURED-REF-01", reference);
    }

    @Test
    void nonSupplierKeepsExistingReferenceBehaviourWithoutIdentityLookup() {
        PaymentRequestEntity payment = new PaymentRequestEntity();
        payment.setRequestType(PaymentRequestType.CLAIM_PAYOUT);
        payment.setExternalReference("CLAIM-REF-01");

        String reference = ReflectionTestUtils.invokeMethod(service, "resolvePaymentReference", payment);

        assertEquals("CLAIM-REF-01", reference);
        verify(jdbcTemplate, never()).queryForList(anyString(), eq(String.class), eq("supplier-1"));
    }

    private PaymentRequestEntity supplierPayment() {
        PaymentRequestEntity payment = new PaymentRequestEntity();
        payment.setRequestNo("PR-0001");
        payment.setRequestType(PaymentRequestType.SUPPLIER_INVOICE);
        payment.setPayeePartnerId("supplier-1");
        payment.setExternalReference("CAPTURED-REF-01");
        return payment;
    }
}
