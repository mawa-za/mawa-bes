package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingService {
    public PricingDto calculate(List<LineItemDto> items) {
        List<LineItemDto> lineItemDtoList = new ArrayList<>();
        PricingDto pricingDto = new PricingDto();
        pricingDto.setVATPercentage(new BigDecimal("15"));
        for (LineItemDto lineItemDto : items) {
            lineItemDto.setLineTotal(lineItemDto.getQuantity().multiply(lineItemDto.getUnitPrice()));
            pricingDto.setTotalExcVat(pricingDto.getTotalExcVat().add(lineItemDto.getLineTotal()));
            pricingDto.setVATAmount(pricingDto.getTotalExcVat().multiply(pricingDto.getVATPercentage().divide(new BigDecimal("100"))));
            pricingDto.setTotalIncVat(pricingDto.getTotalExcVat().add((pricingDto.getVATAmount())));
            lineItemDtoList.add(lineItemDto);
        }
        pricingDto.setItems(lineItemDtoList);
        return pricingDto;
    }

//    public PricingDto calculate(List<TransactionItemDto> items) {
//        List<LineItemDto> lineItemDtoList = new ArrayList<>();
//        PricingDto pricingDto = new PricingDto();
//        pricingDto.setVATPercentage(new BigDecimal("15"));
//        for (TransactionItemDto transactionItemDto : items) {
//            transactionItemDto.setLineTotal(transactionItemDto.getQuantity().multiply(transactionItemDto.getUnitPrice()));
//            pricingDto.setTotalExcVat(pricingDto.getTotalExcVat().add(lineItemDto.getLineTotal()));
//            pricingDto.setVATAmount(pricingDto.getTotalExcVat().multiply(pricingDto.getVATPercentage().divide(new BigDecimal("100"))));
//            pricingDto.setTotalIncVat(pricingDto.getTotalExcVat().add((pricingDto.getVATAmount())));
//            lineItemDtoList.add(transactionItemDto);
//        }
//        pricingDto.setItems(lineItemDtoList);
//        return pricingDto;
//    }
}
