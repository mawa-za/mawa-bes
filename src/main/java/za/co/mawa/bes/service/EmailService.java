package za.co.mawa.bes.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import za.co.mawa.bes.dao.EmailDao;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PropertyDto;

import java.io.UnsupportedEncodingException;
@Service
public class EmailService implements EmailDao {

    private final Environment environment;

    private final JavaMailSender mailSender;

    private final TemplateEngine htmlTemplateEngine;

    public EmailService(Environment environment, JavaMailSender mailSender, TemplateEngine htmlTemplateEngine) {
        this.environment = environment;
        this.mailSender = mailSender;
        this.htmlTemplateEngine = htmlTemplateEngine;
    }

    @Override
    public String build(EmailDto emailDto) {
        return null;
    }

    @Override
    public void send(EmailDto emailDto) {
        try {
            String mailFrom = environment.getProperty("spring.mail.properties.mail.smtp.from");
            String mailFromName = environment.getProperty("mail.from.name", "Identity");
            final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
            final MimeMessageHelper email;
            email = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            email.setTo(emailDto.getTo());
            email.setSubject(emailDto.getSubject());
            email.setFrom(new InternetAddress(mailFrom, mailFromName));
            final Context ctx = new Context(LocaleContextHolder.getLocale());

            for (PropertyDto props : emailDto.getProperties()){
                ctx.setVariable(props.getKey(), props.getValue());
            }

            final String htmlContent = this.htmlTemplateEngine.process(emailDto.getTemplate(), ctx);
            email.setText(htmlContent, true);

//            ClassPathResource clr = new ClassPathResource(SPRING_LOGO_IMAGE);
//            email.addInline("springLogo", clr, PNG_MIME);

            mailSender.send(mimeMessage);

        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
