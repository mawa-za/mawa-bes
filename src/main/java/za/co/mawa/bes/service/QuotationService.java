package za.co.mawa.bes.service;

import org.bouncycastle.asn1.crmf.PKIPublicationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuotationService extends TransactionService {
//    @Autowired
//    TransactionService transactionService;

    public TransactionDto create(QuotationCreateDto quotationCreateDto) {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.QUOTATION);
        return super.create(transactionCreateDto);
    }

    public List<TransactionDto> search(QuotationQueryDto quotationQueryDto){
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.QUOTATION);
        return super.search(transactionQueryDto);
    }

}
