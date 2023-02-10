package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.utils.TransactionType;

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
