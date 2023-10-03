package za.co.mawa.bes.dao;


import za.co.mawa.bes.dto.EmailDto;

public interface EmailDao {
    String build(EmailDto emailDto);
    void send(EmailDto emailDto);

}
