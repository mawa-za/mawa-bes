package za.co.mawa.bes.service;

import za.co.mawa.bes.dao.EmailDao;
import za.co.mawa.bes.dto.EmailDto;

public class EmailService implements EmailDao {
    @Override
    public String build(EmailDto emailDto) {
        return null;
    }

    @Override
    public boolean send() {
        return false;
    }
}
