package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.BankFileXmlCreateDto;
import za.co.mawa.bes.dto.BankFileXmlDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;

public interface BankFileXmlDao {

    String createBankFile(BankFileXmlCreateDto bankFileCreateXmlDto)throws Exception;

    BankFileXmlDto AssignBankFileXml(BankFileXmlCreateDto bankFileCreateXmlDto) throws Exception;

}
