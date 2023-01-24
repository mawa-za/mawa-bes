package za.co.mawa.bes.service;

import jakarta.mail.internet.MimeMessage;
import za.co.mawa.bes.dao.EmailDao;
import za.co.mawa.bes.dto.EmailDto;

public class EmailService implements EmailDao {
    @Override
    public MimeMessage build(EmailDto emailDto) {
        return null;
    }

    @Override
    public boolean send() {
        return false;
    }
}
