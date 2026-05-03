package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.InvoiceOutboundDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.InvoiceLineRepository;
import za.co.mawa.bes.repository.InvoicePaymentRepository;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceLineRepository invoiceLineRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    NumberRangeService numberRangeService;

    public InvoiceEntity createInvoice(InvoiceEntity invoice) {
//        invoice.setId(UUID.randomUUID().toString());
        try {
            invoice.setInvoiceNo(numberRangeService.generateNumber(TransactionType.INVOICE));
        } catch (NumberRangeObjectNotFound e) {
            throw new RuntimeException(e);
        }

        invoice.getLines().forEach(line -> {
//            line.setId(UUID.randomUUID().toString());
            line.setInvoice(invoice); // Ensure proper linkage
        });
        invoice.getPayments().forEach(payment -> {
//            payment.setId(UUID.randomUUID().toString());
            payment.setInvoice(invoice); // Ensure proper linkage
        });
        return invoiceRepository.save(invoice);
    }

    public Optional<InvoiceEntity> getInvoice(String invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    public List<InvoiceLineEntity> getInvoiceLines(String invoiceId) {
        return invoiceLineRepository.findByInvoiceId(invoiceId);
    }

    public List<InvoicePaymentEntity> getInvoicePayments(String invoiceId) {
        return invoicePaymentRepository.findByInvoiceId(invoiceId);
    }

    public void deleteInvoice(String invoiceId) {
        invoiceRepository.deleteById(invoiceId);
    }
    public List<InvoiceEntity> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<InvoiceEntity> getInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status);
    }

    public List<InvoiceEntity> getInvoicesByPartnerId(String partnerId) {
        return invoiceRepository.findByPartnerId(partnerId);
    }

    public List<InvoiceEntity> getInvoicesByDate(String invoiceDate) {
        LocalDate date = LocalDate.parse(invoiceDate);
        return invoiceRepository.findByInvoiceDate(date);
    }


    public InvoiceOutboundDto mapToDto(InvoiceEntity invoice) {
        // Map the main InvoiceEntity to DTO
        InvoiceOutboundDto dto = new InvoiceOutboundDto();
        dto.setId(invoice.getId());
        dto.setPartnerId(invoice.getPartnerId());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setStatus(invoice.getStatus());
        dto.setSubtotalCents(Conversion.safeLongToInteger(invoice.getSubtotalCents()));
        dto.setTaxCents(Conversion.safeLongToInteger(invoice.getTaxCents()));
        dto.setDiscountCents(Conversion.safeLongToInteger(invoice.getDiscountCents()));
        dto.setTotalCents(Conversion.safeLongToInteger(invoice.getTotalCents()));
        dto.setCurrency(invoice.getCurrency());

        // Map the line items to the nested DTO
        List<InvoiceOutboundDto.InvoiceLineDto> lineDtos = invoice.getLines().stream().map(line -> {
            InvoiceOutboundDto.InvoiceLineDto lineDto = new InvoiceOutboundDto.InvoiceLineDto();
            lineDto.setProductId(line.getProductId());
            lineDto.setDescription(line.getDescription());
            lineDto.setQuantity(line.getQuantity().intValue());
            lineDto.setUnitPriceCents(Conversion.safeLongToInteger(line.getUnitPriceCents()));
            lineDto.setDiscountCents(Conversion.safeLongToInteger(line.getDiscountCents()));
            lineDto.setTaxCents(Conversion.safeLongToInteger(line.getTaxCents()));
            lineDto.setSubtotalCents(Conversion.safeLongToInteger(line.getSubtotalCents()));
            lineDto.setTotalCents(Conversion.safeLongToInteger(line.getTotalCents()));
            return lineDto;
        }).toList();

        dto.setLines(lineDtos);
        return dto;
    }

}