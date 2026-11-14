package za.co.mawa.bes.service;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import za.co.mawa.bes.dao.EmailDao;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PropertyDto;

import java.util.List;

/** Compatibility adapter for existing MAWA email templates. All transport uses the shared gateway. */
@Service
public class EmailService implements EmailDao {
    private final SharedOutgoingEmailService outgoing;
    private final TemplateEngine htmlTemplateEngine;

    public EmailService(SharedOutgoingEmailService outgoing, TemplateEngine htmlTemplateEngine) {
        this.outgoing = outgoing;
        this.htmlTemplateEngine = htmlTemplateEngine;
    }

    @Override
    public String build(EmailDto emailDto) {
        Context context = new Context(LocaleContextHolder.getLocale());
        for (PropertyDto property : emailDto.getProperties() == null
                ? List.<PropertyDto>of() : emailDto.getProperties()) {
            context.setVariable(property.getKey(), property.getValue());
        }
        return htmlTemplateEngine.process(emailDto.getTemplate(), context);
    }

    @Override
    public void send(EmailDto emailDto) {
        outgoing.submit(emailDto.getTo(), emailDto.getSubject(), build(emailDto),
                emailDto.getFiles(), "template:" + emailDto.getTemplate());
    }
}
