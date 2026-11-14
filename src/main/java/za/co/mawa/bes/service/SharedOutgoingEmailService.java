package za.co.mawa.bes.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.File;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import jakarta.mail.Session;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * The single outgoing-mail transport entry point for all in-process MAWA services.
 * A future provider/queue adapter can replace the JavaMailSender without changing callers.
 * Only the centrally configured, authenticated From address is ever used.
 */
@Service
public class SharedOutgoingEmailService {
    private static final Logger log = LoggerFactory.getLogger(SharedOutgoingEmailService.class);

    private final JavaMailSender mailSender;
    private final CompanyInfoService companyInfoService;
    private final String fromAddress;
    private final String defaultDisplayName;

    public SharedOutgoingEmailService(JavaMailSender mailSender,
                                      CompanyInfoService companyInfoService,
                                      @Value("${spring.mail.properties.mail.smtp.from:}") String fromAddress,
                                      @Value("${mail.from.name:MAWA}") String defaultDisplayName) {
        this.mailSender = mailSender;
        this.companyInfoService = companyInfoService;
        this.fromAddress = fromAddress;
        this.defaultDisplayName = defaultDisplayName;
    }

    /** SMTP submission is synchronous: success is NOT proof of delivery to the inbox. */
    public String submit(String recipients, String subject, String html, List<File> attachments, String source) {
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())) {
            throw new IllegalStateException("Tenant context is required for outgoing email");
        }
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("Central outgoing email sender is not configured");
        }
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(html)) {
            throw new IllegalArgumentException("Email subject and content are required");
        }
        validateRecipients(recipients);
        String requestId = UUID.randomUUID().toString();
        sendPrepared(compose(recipients, subject, html, attachments), source, requestId);
        return requestId;
    }

    /** Produce an immutable MIME snapshot for durable dispatch; this does NOT send it. */
    public byte[] compose(String recipients, String subject, String html, List<File> attachments) {
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())) {
            throw new IllegalStateException("Tenant context is required for outgoing email");
        }
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("Central outgoing email sender is not configured");
        }
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(html)) {
            throw new IllegalArgumentException("Email subject and content are required");
        }
        String[] parsedRecipients = validateRecipients(recipients);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(parsedRecipients);
            helper.setSubject(subject);
            String tenantName = companyInfoService.getCompanyName();
            String displayName = StringUtils.hasText(tenantName) ? tenantName.trim() : defaultDisplayName;
            helper.setFrom(new InternetAddress(fromAddress, displayName, "UTF-8"));
            String replyTo = companyInfoService.getCompanyEmail();
            if (StringUtils.hasText(replyTo)) {
                helper.setReplyTo(validatedSingleAddress(replyTo));
            }
            helper.setText(html, true);
            for (File attachment : attachments == null ? List.<File>of() : attachments) {
                if (attachment == null || !StringUtils.hasText(attachment.getContent())) continue;
                String name = safeFileName(attachment.getName(), attachment.getType());
                byte[] data = Base64.getDecoder().decode(attachment.getContent());
                helper.addAttachment(name, new ByteArrayResource(data));
            }
            message.saveChanges();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            message.writeTo(bytes);
            return bytes.toByteArray();
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException("Unable to compose outgoing email", e);
        }
    }

    /** Dispatch exactly the MIME snapshot held by the outbox; SMTP acceptance != delivery. */
    public String sendPrepared(byte[] mime, String source, String requestId) {
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())) {
            throw new IllegalStateException("Tenant context is required for outgoing email");
        }
        if (mime == null || mime.length == 0) throw new IllegalArgumentException("Email content is missing");
        try {
            MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(mime));
            mailSender.send(message);
            log.info("Outgoing SMTP accepted tenant={} source={} requestId={}",
                    TenantContext.getCurrentTenant(), source, requestId);
            return message.getMessageID();
        } catch (MessagingException e) {
            throw new IllegalStateException("Unable to read outgoing email", e);
        } catch (org.springframework.mail.MailException e) {
            log.warn("Outgoing SMTP failed tenant={} source={} requestId={}",
                    TenantContext.getCurrentTenant(), source, requestId);
            throw e;
        }
    }

    public static String[] validateRecipients(String input) {
        if (!StringUtils.hasText(input)) throw new IllegalArgumentException("Recipient email is required");
        String[] recipients = input.split(";", -1);
        if (recipients.length > 10) throw new IllegalArgumentException("Too many email recipients");
        for (int i = 0; i < recipients.length; i++) {
            recipients[i] = validatedSingleAddress(recipients[i]);
        }
        return recipients;
    }

    public static String validatedSingleAddress(String candidate) {
        if (!StringUtils.hasText(candidate)) throw new IllegalArgumentException("Invalid email address");
        String address = candidate.trim();
        if (address.length() > 254 || address.indexOf('\r') >= 0 || address.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Invalid email address");
        }
        try {
            InternetAddress parsed = new InternetAddress(address, true);
            parsed.validate();
            if (!address.equals(parsed.getAddress()) || !address.contains("@")) {
                throw new IllegalArgumentException("Invalid email address");
            }
            return address;
        } catch (jakarta.mail.internet.AddressException e) {
            throw new IllegalArgumentException("Invalid email address", e);
        }
    }

    private static String safeFileName(String name, String extension) {
        String safeName = StringUtils.hasText(name) ? name : "attachment";
        safeName = safeName.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        String safeExtension = StringUtils.hasText(extension)
                ? extension.replaceAll("[^A-Za-z0-9]", "") : "bin";
        if (safeName.isBlank()) safeName = "attachment";
        return safeName + "." + safeExtension;
    }
}
