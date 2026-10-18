package za.co.mawa.bes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.xero.XeroIntegrationSettingsService;
import za.co.mawa.bes.xero.XeroInvoicePushService;
import za.co.mawa.bes.xero.XeroMasterDataPushService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Regression coverage for Xero's hard OFF barrier in the shared queue worker. */
class MessageConsumerXeroOffSafetyTest {
    private MessageConsumerService consumer;
    private MessageQueueRepository repository;
    private XeroIntegrationSettingsService settings;
    private XeroInvoicePushService invoicePush;
    private XeroMasterDataPushService masterDataPush;

    @BeforeEach
    void setUp() {
        consumer = new MessageConsumerService();
        repository = mock(MessageQueueRepository.class);
        settings = mock(XeroIntegrationSettingsService.class);
        invoicePush = mock(XeroInvoicePushService.class);
        masterDataPush = mock(XeroMasterDataPushService.class);
        ReflectionTestUtils.setField(consumer, "messageQueueRepository", repository);
        ReflectionTestUtils.setField(consumer, "xeroIntegrationSettingsService", settings);
        ReflectionTestUtils.setField(consumer, "xeroInvoicePushService", invoicePush);
        ReflectionTestUtils.setField(consumer, "xeroMasterDataPushService", masterDataPush);
        ReflectionTestUtils.setField(consumer, "settingService", mock(SettingService.class));
        when(settings.isIntegrationEnabled()).thenReturn(false);
    }

    @Test
    void disabledXeroItemsAreNotClaimedSentOrAcknowledged() {
        MessageQueueEntity invoice = item(1L, "XERO-INVOICE");
        MessageQueueEntity contact = item(2L, "XERO-CUSTOMER");
        MessageQueueEntity product = item(3L, "XERO-PRODUCT");
        when(repository.findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(any()))
                .thenReturn(List.of(invoice, contact, product));

        assertEquals(0, consumer.processCurrentTenant());
        for (MessageQueueEntity msg : List.of(invoice, contact, product)) {
            assertFalse(msg.isProcessed());
            assertEquals(0, msg.getRetryCount());
        }
        verify(repository, never()).save(any());
        verify(repository, never()).claimXeroMessage(anyLong(), any(), any());
        verify(repository, never()).completeXeroClaim(anyLong(), any(), anyBoolean(), anyInt(), any());
        verifyNoInteractions(invoicePush, masterDataPush);
    }

    @Test
    void unrelatedQueueItemContinuesWhileXeroIsDisabled() {
        MessageQueueEntity invoice = item(1L, "XERO-INVOICE");
        MessageQueueEntity email = item(2L, "INVOICE-EMAIL");
        when(repository.findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(any()))
                .thenReturn(List.of(invoice, email));

        assertEquals(1, consumer.processCurrentTenant());
        assertFalse(invoice.isProcessed());
        assertTrue(email.isProcessed());
        verify(repository).save(same(email));
        verify(repository, never()).save(same(invoice));
        verifyNoInteractions(invoicePush, masterDataPush);
    }

    @Test
    void disabledQuarantinedInvoiceQueueItemStaysPendingWithoutRetry() {
        // The invoice's reconciliation flag is owned by XeroInvoicePushService;
        // with integration OFF the queue must never invoke that service at all.
        MessageQueueEntity quarantined = item(9L, "XERO-INVOICE");
        quarantined.setRetryCount(2);
        when(repository.findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(any()))
                .thenReturn(List.of(quarantined));

        assertEquals(0, consumer.processCurrentTenant());
        assertFalse(quarantined.isProcessed());
        assertEquals(2, quarantined.getRetryCount());
        verifyNoInteractions(invoicePush, masterDataPush);
        verify(repository, never()).save(any());
    }

    private MessageQueueEntity item(long id, String type) {
        MessageQueueEntity msg = new MessageQueueEntity();
        msg.setId(id);
        msg.setType(type);
        msg.setReferenceId("ref-" + id);
        msg.setProcessed(false);
        msg.setRetryCount(0);
        msg.setNextAttemptAt(LocalDateTime.now().minusMinutes(1));
        return msg;
    }
}
