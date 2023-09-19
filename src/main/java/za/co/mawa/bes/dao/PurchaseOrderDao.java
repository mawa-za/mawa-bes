package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.purchase.order.*;
import za.co.mawa.bes.dto.service.request.*;
import za.co.mawa.bes.exception.TransactionNotFound;

import java.util.List;

public interface PurchaseOrderDao {
    PurchaseOrderDto create(PurchaseOrderCreateDto serviceRequestCreateDto);
    PurchaseOrderDto edit(PurchaseOrderEditDto purchaseOrderEditDto);
    List<PurchaseOrderQueryResultDto> search(PurchaseOrderQueryDto purchaseOrderQueryDto);
    PurchaseOrderDto get(String id) throws TransactionNotFound;
    void delete(String id);
}

