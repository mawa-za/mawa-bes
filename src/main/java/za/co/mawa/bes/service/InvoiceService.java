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
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Base64;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
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

    @Autowired
    InvoicePDFService invoicePDFService;

    public InvoiceEntity createInvoice(InvoiceEntity invoice) {
//        invoice.setId(UUID.randomUUID().toString());
        try {
            invoice.setInvoiceNo(numberRangeService.generateNumber(TransactionType.INVOICE));
        } catch (NumberRangeObjectNotFound e) {
            throw new RuntimeException(e);
        }

        if (invoice.getLines() == null) invoice.setLines(new java.util.ArrayList<>());
        if (invoice.getPayments() == null) invoice.setPayments(new java.util.ArrayList<>());
        if (invoice.getCreditedCents() == null) invoice.setCreditedCents(0L);
        invoice.getLines().forEach(line -> {
            if (line.getShowAmount() == null) line.setShowAmount(true);
            if (line.getProductId() != null && !line.getProductId().isBlank()) {
                productService.requireAvailableForSale(line.getProductId());
            }
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

    @Transactional
    public InvoiceEntity updateInvoice(String invoiceId, InvoiceEntity request) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));
        invoice.setExternalRef(request.getExternalRef());
        invoice.setPartnerId(request.getPartnerId());
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(request.getStatus() == null ? invoice.getStatus() : request.getStatus());
        invoice.setSubtotalCents(request.getSubtotalCents());
        invoice.setTaxCents(request.getTaxCents());
        invoice.setDiscountCents(request.getDiscountCents());
        invoice.setTotalCents(request.getTotalCents());
        invoice.setBalanceCents(Math.max(0L, value(request.getTotalCents()) - value(invoice.getPaidCents()) - value(invoice.getCreditedCents())));
        invoice.setCurrency(request.getCurrency() == null ? "ZAR" : request.getCurrency());
        invoice.setNotes(request.getNotes());
        invoice.getLines().clear();
        if (request.getLines() != null) {
            request.getLines().forEach(line -> {
                if (line.getProductId() != null && !line.getProductId().isBlank()) productService.requireAvailableForSale(line.getProductId());
                line.setId(null);
                line.setInvoice(invoice);
                if (line.getShowAmount() == null) line.setShowAmount(true);
                invoice.getLines().add(line);
            });
        }
        invoice.setUpdatedAt(LocalDateTime.now());
        InvoiceEntity saved = invoiceRepository.save(invoice);
        xeroInvoiceQueueService.queueInvoiceIfEnabled(saved);
        return saved;
    }

    @Transactional
    public InvoiceOutboundDto updateInvoiceDto(String invoiceId, InvoiceEntity request) {
        return mapToDto(updateInvoice(invoiceId, request));
    }

    private long value(Long amount) { return amount == null ? 0L : amount; }

    public Optional<InvoiceEntity> getInvoice(String invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateInvoicePdf(String invoiceId) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("Invoice ID is required");
        }
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId.trim())
                .orElseThrow(() -> new NoSuchElementException("Invoice not found with ID: " + invoiceId));
        return invoicePDFService.generateInvoicePdf(invoice);
    }

    @Transactional(readOnly = true)
    public Optional<InvoiceOutboundDto> getInvoiceDto(String invoiceId) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("Invoice ID is required");
        }
        return invoiceRepository.findById(invoiceId.trim()).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<InvoiceOutboundDto> searchInvoiceDtos(String status, String partnerId, String invoiceDate) {
        LocalDate requestedDate = null;
        if (invoiceDate != null && !invoiceDate.isBlank()) {
            try {
                requestedDate = LocalDate.parse(invoiceDate.trim());
            } catch (java.time.format.DateTimeParseException exception) {
                throw new IllegalArgumentException("Invoice date must use the format YYYY-MM-DD");
            }
        }

        final String requestedStatus = status == null ? null : status.trim();
        final String requestedPartner = partnerId == null ? null : partnerId.trim();
        final LocalDate finalRequestedDate = requestedDate;

        return invoiceRepository.findAll().stream()
                .filter(invoice -> requestedStatus == null || requestedStatus.isBlank()
                        || requestedStatus.equalsIgnoreCase(invoice.getStatus()))
                .filter(invoice -> requestedPartner == null || requestedPartner.isBlank()
                        || requestedPartner.equals(invoice.getPartnerId()))
                .filter(invoice -> finalRequestedDate == null
                        || finalRequestedDate.equals(invoice.getInvoiceDate()))
                .sorted(java.util.Comparator
                        .comparing(InvoiceEntity::getInvoiceDate,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(InvoiceEntity::getInvoiceNo,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceLineEntity> getInvoiceLines(String invoiceId) {
        return invoiceLineRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<InvoicePaymentEntity> getInvoicePayments(String invoiceId) {
        return invoicePaymentRepository.findByInvoiceId(invoiceId);
    }

    public void deleteInvoice(String invoiceId) {
        invoiceRepository.deleteById(invoiceId);
    }

    @Transactional
    public InvoiceEntity queueInvoiceForXero(String invoiceId) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));
        xeroInvoiceQueueService.queueInvoice(invoice);
        return invoiceRepository.findById(invoiceId).orElse(invoice);
    }

    @Transactional
    public InvoiceOutboundDto queueInvoiceForXeroDto(String invoiceId) {
        return mapToDto(queueInvoiceForXero(invoiceId));
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
        if (invoice.getPartnerId() != null && !invoice.getPartnerId().isBlank()) {
            try {
                PartnerDto partner = partnerService.get(invoice.getPartnerId());
                dto.setPartnerName(partner == null ? null : fullName(partner));
            } catch (Exception ignored) {
                // Historical invoices may reference a partner that is no longer available.
                // Keep the invoice readable and allow the client to display the partner reference.
            }
        }
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setStatus(invoice.getStatus());
        dto.setSubtotalCents(Conversion.safeLongToInteger(invoice.getSubtotalCents()));
        dto.setTaxCents(Conversion.safeLongToInteger(invoice.getTaxCents()));
        dto.setDiscountCents(Conversion.safeLongToInteger(invoice.getDiscountCents()));
        dto.setTotalCents(Conversion.safeLongToInteger(invoice.getTotalCents()));
        dto.setPaidCents(Conversion.safeLongToInteger(invoice.getPaidCents()));
        dto.setCreditedCents(Conversion.safeLongToInteger(invoice.getCreditedCents()));
        dto.setBalanceCents(Conversion.safeLongToInteger(invoice.getBalanceCents()));
        dto.setExternalRef(invoice.getExternalRef());
        dto.setNotes(invoice.getNotes());
        dto.setCurrency(invoice.getCurrency());
        dto.setXeroInvoiceId(invoice.getXeroInvoiceId());
        dto.setXeroInvoiceNo(invoice.getXeroInvoiceNo());
        dto.setIntegrationStatus(invoice.getIntegrationStatus());
        dto.setIntegrationError(invoice.getIntegrationError());

        // Map the line items to the nested DTO
        List<InvoiceLineEntity> lines = invoice.getLines() == null ? List.of() : invoice.getLines();
        List<InvoiceOutboundDto.InvoiceLineDto> lineDtos = lines.stream().map(line -> {
            InvoiceOutboundDto.InvoiceLineDto lineDto = new InvoiceOutboundDto.InvoiceLineDto();
            lineDto.setProductId(line.getProductId());
            lineDto.setDescription(line.getDescription() == null ? "Invoice item" : line.getDescription());
            Double quantity = line.getQuantity();
            lineDto.setQuantity(quantity == null ? 1 : Math.max(0, quantity.intValue()));
            lineDto.setShowAmount(!Boolean.FALSE.equals(line.getShowAmount()));
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