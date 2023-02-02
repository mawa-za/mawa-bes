package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.TransactionCreateDto;
import za.co.mawa.bes.dto.TransactionDto;
import za.co.mawa.bes.dto.TransactionQueryDto;
import za.co.mawa.bes.utils.TransactionType;

import java.util.List;

@Service
public class PurchaseOrderService extends TransactionService {
    @Autowired
    TransactionService transactionService;
    @Override
    public TransactionDto create(TransactionCreateDto transactionCreateDto) {
        transactionCreateDto.setType(TransactionType.PURCHASE_ORDER);
        return super.create(transactionCreateDto);
    }
    @Override
    public List<TransactionDto> search(TransactionQueryDto transactionQueryDto){
        transactionQueryDto.setType(TransactionType.PURCHASE_ORDER);
        return super.search(transactionQueryDto);
    }

}
