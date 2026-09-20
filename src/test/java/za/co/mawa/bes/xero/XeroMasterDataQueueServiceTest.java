package za.co.mawa.bes.xero;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.PartnerRoleRepository;
import za.co.mawa.bes.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XeroMasterDataQueueServiceTest {

    @Mock private MessageQueueRepository queueRepository;
    @Mock private XeroIntegrationSettingsService settings;
    @Mock private PartnerRepository partnerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PartnerRoleRepository partnerRoleRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private XeroInvoiceQueueService invoiceQueueService;

    @Test
    void queuesProductsBeforeCustomersAndIncludesNewAndExistingXeroInvoices() {
        ProductEntity product = ProductEntity.builder().id("product-1").code("PLAN-1").build();
        InvoiceEntity unposted = InvoiceEntity.builder().id("invoice-1").invoiceNo("INV-1").build();
        InvoiceEntity posted = InvoiceEntity.builder().id("invoice-2").invoiceNo("INV-2")
                .xeroInvoiceId("xero-invoice-2").build();

        when(settings.isIntegrationEnabled()).thenReturn(true);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(partnerRoleRepository.findPartnerByRole("CUSTOMER")).thenReturn(List.of());
        when(invoiceRepository.findAll()).thenReturn(List.of(unposted, posted));
        when(queueRepository.findFirstByTypeAndReferenceIdOrderByIdDesc(
                XeroMasterDataQueueService.PRODUCT_MESSAGE_TYPE, "product-1"))
                .thenReturn(Optional.empty());
        when(queueRepository.save(any(MessageQueueEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        XeroMasterDataQueueService service = new XeroMasterDataQueueService(
                queueRepository, settings, partnerRepository, productRepository,
                partnerRoleRepository, invoiceRepository, invoiceQueueService);

        service.queueAllExisting();

        ArgumentCaptor<MessageQueueEntity> message = ArgumentCaptor.forClass(MessageQueueEntity.class);
        verify(queueRepository).save(message.capture());
        assertEquals(XeroMasterDataQueueService.PRODUCT_MESSAGE_TYPE, message.getValue().getType());
        assertEquals("product-1", message.getValue().getReferenceId());
        verify(invoiceQueueService).queueInvoiceIfEnabled(unposted);
        verify(invoiceQueueService).queueInvoiceIfEnabled(posted);
        var order = inOrder(productRepository, partnerRoleRepository, invoiceRepository);
        order.verify(productRepository).findAll();
        order.verify(partnerRoleRepository).findPartnerByRole("CUSTOMER");
        order.verify(invoiceRepository).findAll();
    }
}
