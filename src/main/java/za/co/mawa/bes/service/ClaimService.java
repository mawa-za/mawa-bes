package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.TransactionCreateDto;
import za.co.mawa.bes.dto.TransactionDto;
import za.co.mawa.bes.dto.TransactionQueryDto;
import za.co.mawa.bes.utils.TransactionType;

import java.util.List;

@Service
public class ClaimService extends TransactionService{
    @Override
    public TransactionDto create(TransactionCreateDto transactionCreateDto) {
        transactionCreateDto.setType(TransactionType.CLAIM);
        return super.create(transactionCreateDto);
    }
    @Override
    public List<TransactionDto> search(TransactionQueryDto transactionQueryDto){
        transactionQueryDto.setType(TransactionType.CLAIM);
        return super.search(transactionQueryDto);
    }
}
