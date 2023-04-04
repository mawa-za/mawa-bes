package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;

public interface ReceiptDao {

    ReceiptDto createReceipt(ReceiptCreateDto receipt) throws Exception;
}
