package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryResultDto;
import za.co.mawa.bes.utils.TransactionType;

import java.util.List;

@Service
public class SalesOrderService extends TransactionService {
    @Autowired
    TransactionService transactionService;
    @Override
    public TransactionDto create(TransactionCreateDto transactionCreateDto) {
        transactionCreateDto.setType(TransactionType.SALES_ORDER);
        return super.create(transactionCreateDto);
    }
    @Override
    public List<TransactionQueryResultDto> search(TransactionQueryDto transactionQueryDto){
        transactionQueryDto.setType(TransactionType.SALES_ORDER);
        return super.search(transactionQueryDto);
    }

}
