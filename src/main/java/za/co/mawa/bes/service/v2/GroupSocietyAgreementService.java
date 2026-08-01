package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.repository.v2.GroupSocietyRepository;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupSocietyAgreementService {
    private final GroupSocietyRepository groupSocietyRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public byte[] generate(String groupSocietyId) {
        GroupSocietyEntity society = groupSocietyRepository.findById(groupSocietyId)
                .orElseThrow(() -> new IllegalArgumentException("Group society not found: " + groupSocietyId));
        Map<String, Object> partner = jdbcTemplate.queryForMap("""
                SELECT p.number partner_no,
                       TRIM(CONCAT_WS(' ',NULLIF(p.name1,''),NULLIF(p.name2,''),NULLIF(p.name3,''))) partner_name,
                       COALESCE((SELECT pi.value FROM partner_identity pi
                                  WHERE pi.partner=p.id
                                    AND UPPER(TRIM(pi.type)) IN ('SA-ID','PASSPORT')
                                  ORDER BY CASE WHEN UPPER(TRIM(pi.type))='SA-ID' THEN 0 ELSE 1 END
                                  LIMIT 1),'') identity_number
                  FROM partner p WHERE p.id=?
                """, society.getPartnerId());
        List<Map<String,Object>> contacts = jdbcTemplate.queryForList(
                "SELECT contact_name,role,mobile_no,email,primary_contact FROM group_society_contact WHERE group_society_id=? ORDER BY primary_contact DESC,contact_name",
                groupSocietyId);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 790;
                text(content, bold, 18, 50, y, "GROUP SOCIETY COVER AGREEMENT");
                y -= 22;
                text(content, regular, 9, 50, y, "Agreement generated " + LocalDate.now());
                y -= 34;

                y = section(content, bold, regular, y, "Society details", List.of(
                        "Group number: " + safe(society.getGroupNo()),
                        "Society name: " + safe(partner.get("partner_name")),
                        "Partner number: " + safe(partner.get("partner_no")),
                        "Society type: " + safe(society.getSocietyType()),
                        "Status: " + safe(society.getStatus()),
                        "Available cover balance: R " + BigDecimal.valueOf(value(society.getAvailableBalanceCents()), 2)
                ));

                List<String> contactLines = new ArrayList<>();
                if (contacts.isEmpty()) {
                    contactLines.add("No contacts recorded.");
                } else {
                    for (Map<String,Object> contact : contacts) {
                        contactLines.add(safe(contact.get("contact_name")) + " - "
                                + safe(contact.get("role")) + " | " + safe(contact.get("mobile_no"))
                                + " | " + safe(contact.get("email")));
                    }
                }
                y = section(content, bold, regular, y - 12, "Authorised contacts", contactLines);

                y = section(content, bold, regular, y - 12, "Agreement", List.of(
                        "The group society appoints the service provider to administer prepaid funeral cover funds.",
                        "Payments increase the available society balance and are receipted through MAWA.",
                        "Approved funeral claims reduce the available society balance.",
                        "Any amount exceeding approved cover remains payable by the deceased's family.",
                        "Balance adjustments, suspension and closure are subject to approval and supporting documentation.",
                        "The society must notify the service provider when authorised contacts or society details change."
                ));

                y -= 26;
                text(content, bold, 10, 50, y, "For the Group Society");
                y -= 36;
                text(content, regular, 9, 50, y, "Name: ______________________________");
                text(content, regular, 9, 330, y, "Designation: ____________________");
                y -= 32;
                text(content, regular, 9, 50, y, "Signature: ___________________________");
                text(content, regular, 9, 330, y, "Date: ___________________________");
                y -= 48;
                text(content, bold, 10, 50, y, "For the Service Provider");
                y -= 36;
                text(content, regular, 9, 50, y, "Name: ______________________________");
                text(content, regular, 9, 330, y, "Designation: ____________________");
                y -= 32;
                text(content, regular, 9, 50, y, "Signature: ___________________________");
                text(content, regular, 9, 330, y, "Date: ___________________________");
            }
            document.save(out);
            society.setAgreementPrintCount((society.getAgreementPrintCount() == null ? 0 : society.getAgreementPrintCount()) + 1);
            society.setAgreementLastPrintedAt(new Date());
            groupSocietyRepository.save(society);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate group society agreement", e);
        }
    }

    private float section(PDPageContentStream content, PDFont bold, PDFont regular,
                          float y, String heading, List<String> lines) throws Exception {
        text(content, bold, 11, 50, y, heading);
        y -= 18;
        for (String line : lines) {
            for (String wrapped : wrap(line, 92)) {
                text(content, regular, 9, 60, y, wrapped);
                y -= 14;
            }
        }
        return y;
    }

    private List<String> wrap(String text, int max) {
        List<String> lines = new ArrayList<>();
        String remaining = safe(text);
        while (remaining.length() > max) {
            int split = remaining.lastIndexOf(' ', max);
            if (split < 1) split = max;
            lines.add(remaining.substring(0, split));
            remaining = remaining.substring(split).trim();
        }
        lines.add(remaining);
        return lines;
    }

    private void text(PDPageContentStream content, PDFont font, float size,
                      float x, float y, String value) throws Exception {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(value));
        content.endText();
    }

    private long value(Long value) { return value == null ? 0L : value; }
    private String safe(Object value) {
        return value == null ? "" : value.toString().replaceAll("[^\\x20-\\x7E]", "?");
    }
}
