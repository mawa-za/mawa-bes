package za.co.mawa.bes.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.InvoiceOutboundDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.entity.v2.AppointmentEntity;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.InvoiceLineRepository;
import za.co.mawa.bes.repository.InvoicePaymentRepository;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.v2.AppointmentRepository;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.TransactionType;
import za.co.mawa.bes.xero.XeroInvoiceQueueService;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
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

    @Autowired
    XeroInvoiceQueueService xeroInvoiceQueueService;

    @Autowired
    AppointmentRepository appointmentRepository;

    @Autowired
    ProductService productService;

    @Autowired
    PartnerService partnerService;

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
        InvoiceEntity savedInvoice = invoiceRepository.save(invoice);
        xeroInvoiceQueueService.queueInvoiceIfEnabled(savedInvoice);
        return savedInvoice;
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

    public InvoiceEntity queueInvoiceForXero(String invoiceId) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));
        xeroInvoiceQueueService.queueInvoice(invoice);
        return invoiceRepository.findById(invoiceId).orElse(invoice);
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


    public InvoiceEntity createInvoiceForAppointment(String appointmentId, String currentUser) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        List<InvoiceEntity> existing = invoiceRepository.findBySourceTypeAndSourceId("APPOINTMENT", appointmentId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        String description = "Appointment booking " + (appointment.getAppointmentNo() == null ? appointment.getId() : appointment.getAppointmentNo());
        Long unitPriceCents = 0L;
        if (appointment.getServiceProductId() != null && !appointment.getServiceProductId().isBlank()) {
            try {
                ProductDto product = productService.getOptionalById(appointment.getServiceProductId());
                if (product != null) {
                    String code = product.getCode() == null ? "" : product.getCode();
                    String productDescription = product.getDescription() == null ? "" : product.getDescription();
                    description = (code + " - " + productDescription).trim();
                    unitPriceCents = firstPriceInCents(product);
                }
            } catch (Exception ignored) {
            }
        }

        InvoiceLineEntity line = InvoiceLineEntity.builder()
                .productId(appointment.getServiceProductId())
                .description(description)
                .quantity(1.0)
                .unitPriceCents(unitPriceCents)
                .discountCents(0L)
                .taxCents(0L)
                .subtotalCents(unitPriceCents)
                .totalCents(unitPriceCents)
                .build();

        InvoiceEntity invoice = InvoiceEntity.builder()
                .externalRef(appointment.getAppointmentNo())
                .sourceType("APPOINTMENT")
                .sourceId(appointment.getId())
                .partnerId(appointment.getCustomerPartnerId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .status("DRAFT")
                .subtotalCents(unitPriceCents)
                .taxCents(0L)
                .discountCents(0L)
                .totalCents(unitPriceCents)
                .paidCents(0L)
                .balanceCents(unitPriceCents)
                .currency("ZAR")
                .notes("Created from appointment " + (appointment.getAppointmentNo() == null ? appointment.getId() : appointment.getAppointmentNo()))
                .createdBy(currentUser)
                .lines(new java.util.ArrayList<>(List.of(line)))
                .payments(new java.util.ArrayList<>())
                .build();
        return createInvoice(invoice);
    }

    private String fullName(PartnerDto partner) {
        return java.util.stream.Stream.of(partner.getName2(), partner.getName3(), partner.getName1(), partner.getName4())
                .filter(v -> v != null && !v.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse(partner.getNumber() == null ? partner.getId() : partner.getNumber());
    }

    private Long firstPriceInCents(ProductDto product) {
        if (product.getPricings() == null || product.getPricings().isEmpty()) return 0L;
        for (ProductPricingDto pricing : product.getPricings()) {
            BigDecimal value = pricing.getValue();
            if (value != null) return value.multiply(BigDecimal.valueOf(100)).longValue();
        }
        return 0L;
    }


    public InvoiceOutboundDto mapToDto(InvoiceEntity invoice) {
        // Map the main InvoiceEntity to DTO
        InvoiceOutboundDto dto = new InvoiceOutboundDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNo(invoice.getInvoiceNo());
        dto.setPartnerId(invoice.getPartnerId());
        dto.setSourceType(invoice.getSourceType());
        dto.setSourceId(invoice.getSourceId());
        try {
            PartnerDto partner = partnerService.get(invoice.getPartnerId());
            dto.setPartnerName(partner == null ? null : fullName(partner));
        } catch (Exception ignored) {}
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setStatus(invoice.getStatus());
        dto.setSubtotalCents(Conversion.safeLongToInteger(invoice.getSubtotalCents()));
        dto.setTaxCents(Conversion.safeLongToInteger(invoice.getTaxCents()));
        dto.setDiscountCents(Conversion.safeLongToInteger(invoice.getDiscountCents()));
        dto.setTotalCents(Conversion.safeLongToInteger(invoice.getTotalCents()));
        dto.setPaidCents(Conversion.safeLongToInteger(invoice.getPaidCents()));
        dto.setBalanceCents(Conversion.safeLongToInteger(invoice.getBalanceCents()));
        dto.setExternalRef(invoice.getExternalRef());
        dto.setNotes(invoice.getNotes());
        dto.setCurrency(invoice.getCurrency());
        dto.setXeroInvoiceId(invoice.getXeroInvoiceId());
        dto.setXeroInvoiceNo(invoice.getXeroInvoiceNo());
        dto.setIntegrationStatus(invoice.getIntegrationStatus());
        dto.setIntegrationError(invoice.getIntegrationError());

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