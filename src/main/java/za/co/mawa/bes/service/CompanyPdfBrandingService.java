package za.co.mawa.bes.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.company.CompanyLogoEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared company branding used by every generated PDF.
 * Keeps the configured tenant logo and company information consistent across
 * iText and PDFBox based documents.
 */
@Service
@RequiredArgsConstructor
public class CompanyPdfBrandingService {
    private static final float PDFBOX_HEADER_HEIGHT = 82f;

    private final CompanyInfoService companyInfoService;
    private final CompanyLogoService companyLogoService;

    public void addITextHeader(Document document, PdfFont regular, PdfFont bold) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{31, 69}))
                .useAllAvailableWidth()
                .setMarginBottom(8);

        Cell logoCell = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setPadding(0);
        Optional<CompanyLogoEntity> logo = companyLogoService.getActiveLogo();
        if (logo.isPresent() && logo.get().getContent() != null && logo.get().getContent().length > 0) {
            try {
                logoCell.add(new Image(ImageDataFactory.create(logo.get().getContent()))
                        .scaleToFit(CompanyLogoService.PDF_WIDTH_PT, CompanyLogoService.PDF_HEIGHT_PT));
            } catch (Exception ignored) {
                // A malformed stored logo must never prevent a business document from being generated.
            }
        }
        header.addCell(logoCell);

        Cell companyCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setPadding(0);
        addITextLine(companyCell, companyInfoService.getCompanyName(), bold, 12f);
        addITextLine(companyCell, companyInfoService.getCompanyAddress(), regular, 8f);
        addITextLine(companyCell, registrationLine(), regular, 8f);
        addITextLine(companyCell, companyInfoService.getContactDetails(), regular, 8f);
        header.addCell(companyCell);
        document.add(header);
    }

    public float drawPdfBoxHeader(PDDocument document,
                                  PDPageContentStream content,
                                  PDFont regular,
                                  PDFont bold,
                                  float left,
                                  float right,
                                  float topY) throws IOException {
        Optional<CompanyLogoEntity> logo = companyLogoService.getActiveLogo();
        if (logo.isPresent() && logo.get().getContent() != null && logo.get().getContent().length > 0) {
            try {
                PDImageXObject image = PDImageXObject.createFromByteArray(
                        document, logo.get().getContent(), "company-logo");
                float scale = Math.min(
                        CompanyLogoService.PDF_WIDTH_PT / image.getWidth(),
                        CompanyLogoService.PDF_HEIGHT_PT / image.getHeight());
                float width = image.getWidth() * scale;
                float height = image.getHeight() * scale;
                content.drawImage(image, left, topY - height, width, height);
            } catch (Exception ignored) {
                // Continue with textual branding if the stored image cannot be decoded.
            }
        }

        float lineY = topY - 10;
        lineY = writeRightIfPresent(content, bold, 12f, right, lineY,
                companyInfoService.getCompanyName(), 72);
        lineY = writeRightIfPresent(content, regular, 7.5f, right, lineY - 4,
                companyInfoService.getCompanyAddress(), 92);
        lineY = writeRightIfPresent(content, regular, 7.5f, right, lineY - 3,
                registrationLine(), 92);
        writeRightIfPresent(content, regular, 7.5f, right, lineY - 3,
                companyInfoService.getContactDetails(), 92);

        float separatorY = topY - PDFBOX_HEADER_HEIGHT;
        content.setLineWidth(.7f);
        content.moveTo(left, separatorY);
        content.lineTo(right, separatorY);
        content.stroke();
        return separatorY - 14f;
    }

    private void addITextLine(Cell cell, String value, PdfFont font, float size) {
        String text = safe(value);
        if (text.isBlank()) return;
        cell.add(new Paragraph(text).setFont(font).setFontSize(size).setMargin(0).setFixedLeading(size + 2));
    }

    private float writeRightIfPresent(PDPageContentStream content,
                                      PDFont font,
                                      float size,
                                      float right,
                                      float y,
                                      String value,
                                      int maxChars) throws IOException {
        String text = truncate(ascii(value), maxChars);
        if (text.isBlank()) return y;
        float width = font.getStringWidth(text) / 1000f * size;
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(Math.max(20f, right - width), y);
        content.showText(text);
        content.endText();
        return y - (size + 2f);
    }

    private String registrationLine() {
        List<String> values = new ArrayList<>();
        addLabel(values, "Reg No", companyInfoService.getCompanyRegistrationNumber());
        addLabel(values, "VAT No", companyInfoService.getVATNumber());
        addLabel(values, "FSP No", companyInfoService.getFspNumber());
        return String.join(" | ", values);
    }

    private void addLabel(List<String> values, String label, String value) {
        String cleaned = safe(value);
        if (!cleaned.isBlank()) values.add(label + ": " + cleaned);
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String ascii(Object value) {
        return safe(value).replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String truncate(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        if (maxChars <= 3) return value.substring(0, maxChars);
        return value.substring(0, maxChars - 3) + "...";
    }
}
