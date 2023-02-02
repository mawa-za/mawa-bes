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
    @Autowired
    TransactionService transactionService;
    @Override
    public TransactionDto create(TransactionCreateDto transactionCreateDto) {
        transactionCreateDto.setType(TransactionType.QUOTATION);
        return super.create(transactionCreateDto);
    }
    @Override
    public List<TransactionDto> search(TransactionQueryDto transactionQueryDto){
        transactionQueryDto.setType(TransactionType.QUOTATION);
        return super.search(transactionQueryDto);
    }

}
