package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.exception.DoesNotExist;

public interface ReceiptDao {

    ReceiptDto createReceipt(ReceiptCreateDto receipt) throws Exception;

    ReceiptDto getReceipt(String id) throws DoesNotExist;

}
