package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.quotation.QuotationDto;
import za.co.mawa.bes.dto.quotation.QuotationQueryDto;

import java.util.List;

public interface QuotationDao {
    QuotationDto create(TransactionCreateDto transactionCreateDto) throws Exception;
    QuotationDto edit(QuotationDto quotationDto);
    QuotationDto get(String id);
    List<QuotationDto> search(QuotationQueryDto quotationSearchDto);
    void delete(String id);

}
