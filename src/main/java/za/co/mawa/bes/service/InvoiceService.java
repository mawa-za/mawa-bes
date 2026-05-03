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
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.InvoiceLineRepository;
import za.co.mawa.bes.repository.InvoicePaymentRepository;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.TransactionType;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
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
        dto.setInvoiceNo(invoice.getInvoiceNo());
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



    public String generateInvoicePdfAsBase64(String invoiceId) {
        // Fetch the invoice by ID
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        // Create a PDF in memory using ByteArrayOutputStream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(out);

        try (Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter))) {
            // Add invoice title
            document.add(new Paragraph("Invoice")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(16));

            // Add invoice metadata
            document.add(new Paragraph("Invoice ID: " + invoice.getId()));
            document.add(new Paragraph("Partner ID: " + invoice.getPartnerId()));
            document.add(new Paragraph("Invoice Date: " + invoice.getInvoiceDate()));
            document.add(new Paragraph("Due Date: " + invoice.getDueDate()));
            document.add(new Paragraph("Status: " + invoice.getStatus()));
            document.add(new Paragraph("Total: " + (float) invoice.getTotalCents() / 100 + " " + invoice.getCurrency()));
            document.add(new Paragraph("\n"));

            // Add line items table
            Table table = new Table(new float[]{3, 8, 2, 3, 3, 3});
            table.addHeaderCell(new Cell().add(new Paragraph("Product ID")));
            table.addHeaderCell(new Cell().add(new Paragraph("Description")));
            table.addHeaderCell(new Cell().add(new Paragraph("Qty")));
            table.addHeaderCell(new Cell().add(new Paragraph("Unit Price (cents)")));
            table.addHeaderCell(new Cell().add(new Paragraph("Subtotal (cents)")));
            table.addHeaderCell(new Cell().add(new Paragraph("Tax (cents)")));

            for (InvoiceLineEntity line : invoice.getLines()) {
                table.addCell(new Cell().add(new Paragraph(line.getProductId())));
                table.addCell(new Cell().add(new Paragraph(line.getDescription())));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(line.getQuantity()))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(line.getUnitPriceCents()))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(line.getSubtotalCents()))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(line.getTaxCents()))));
            }

            document.add(table);
            document.add(new Paragraph("\nThank you for your business!"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while generating PDF: " + e.getMessage());
        }

        // Convert the PDF byte array to Base64
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

}