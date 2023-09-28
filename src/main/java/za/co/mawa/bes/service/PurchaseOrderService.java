package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.PurchaseOrderDao;
import za.co.mawa.bes.dto.purchase.order.*;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryResultDto;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.TransactionType;

import java.util.List;

@Service
public class PurchaseOrderService implements PurchaseOrderDao {
    @Autowired
    TransactionService transactionService;
    @Override
    public PurchaseOrderDto create(PurchaseOrderCreateDto serviceRequestCreateDto) {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.PURCHASE_ORDER);
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);
        return null;
    }

    @Override
    public PurchaseOrderDto edit(PurchaseOrderEditDto purchaseOrderEditDto) {
        return null;
    }

    @Override
    public List<PurchaseOrderQueryResultDto> search(PurchaseOrderQueryDto purchaseOrderQueryDto) {
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.PURCHASE_ORDER);
        transactionService.search(transactionQueryDto);
        return null;
    }

    @Override
    public PurchaseOrderDto get(String id) throws TransactionNotFound {
        return null;
    }

    @Override
    public void delete(String id) {

    }
}
