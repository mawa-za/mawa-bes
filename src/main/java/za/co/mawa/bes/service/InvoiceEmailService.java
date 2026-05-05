package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.stream.Collectors;

@Service
public class InvoiceEmailService {

    @Value("${application.tenant.url}") // The tenant's base URL
    private String tenantUrl;

    @Value("${application.server.url}") // Back-end server's base URL
    private String serverUrl;

    private final ResourceLoader resourceLoader;

    public InvoiceEmailService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String generateInvoiceEmailTemplate(InvoiceEntity invoice) throws IOException {
        // Load the HTML template from resources
        Resource resource = resourceLoader.getResource("classpath:templates/invoice-template.html");
        String template = Files.readString(Paths.get(resource.getURI()));

        // Generate dynamic fields
        String invoicePreviewUrl = tenantUrl + "/#/invoice-preview?id=" + invoice.getId();
        String pdfDownloadUrl = serverUrl + "/api/invoices/" + invoice.getId() + "/pdf";

        DecimalFormat df = new DecimalFormat("#,##0.00");

        String lineItemsHtml = invoice.getLines().stream().map(line -> {
            return String.format(
                "<tr><td>%s</td><td>%s</td><td>%d</td><td>$%.2f</td><td>$%.2f</td><td>$%.2f</td></tr>",
                line.getProductId(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitPriceCents() / 100.0,
                line.getSubtotalCents() / 100.0,
                line.getTaxCents() / 100.0
            );
        }).collect(Collectors.joining());

        // Replace placeholders in template
        template = template.replace("{{invoiceId}}", invoice.getId());
        template = template.replace("{{invoiceDate}}", invoice.getInvoiceDate().toString());
        template = template.replace("{{dueDate}}", invoice.getDueDate().toString());
        template = template.replace("{{status}}", invoice.getStatus());
        template = template.replace("{{lineItems}}", lineItemsHtml);
        template = template.replace("{{total}}", df.format(invoice.getTotalCents() / 100.0));
        template = template.replace("{{invoicePreviewUrl}}", invoicePreviewUrl);
        template = template.replace("{{pdfDownloadUrl}}", pdfDownloadUrl);

        return template;
    }
}