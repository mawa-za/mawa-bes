package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.QuotationDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.util.List;

@Service
public class QuotationService implements QuotationDao {
    @Autowired
    TransactionService transactionService;
    @Override
    public QuotationDto create(QuotationCreateDto quotationCreateDto) throws Exception {
        try {
            TransactionDto transactionDto = new TransactionDto();
            transactionDto.setType(TransactionType.QUOTATION);
            QuotationDto quotationDto = (QuotationDto) transactionService.create(transactionDto);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(quotationDto.getId());
            creationDate.setType(DateType.CREATED);
            transactionService.addDate(creationDate);

            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(quotationDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
            transactionPartnerDto.setPartner(quotationCreateDto.getCustomer());
            transactionService.addPartner(transactionPartnerDto);
            return null;
        }catch (Exception exception){
            throw new Exception();
        }
    }

    @Override
    public QuotationDto edit(QuotationDto quotationDto) {
        return null;
    }

    @Override
    public QuotationDto get(String id) {
        return null;
    }

    @Override
    public List<QuotationDto> search(QuotationQueryDto quotationQueryDto) {
        return null;
    }

    @Override
    public void delete(String id) {

    }


}
