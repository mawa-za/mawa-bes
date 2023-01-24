package za.co.mawa.bes.dao;

import jakarta.mail.internet.MimeMessage;
import za.co.mawa.bes.dto.EmailDto;

public interface EmailDao {
    MimeMessage build(EmailDto emailDto);
    boolean send();

}
