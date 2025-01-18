package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.exception.ProductNotFoundException;
import za.co.mawa.bes.utils.Field;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingService {
    @Autowired
    ProductService productService;
    @Autowired
    FieldOptionService fieldOptionService;
    public PricingOutboundDto calculate(List<LineItemInboundDto> lineItemInboundDtoList) {
        List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
        PricingOutboundDto pricingOutboundDto = new PricingOutboundDto();
        pricingOutboundDto.setVATPercentage(new BigDecimal("15"));
        for (LineItemInboundDto lineItemInboundDto : lineItemInboundDtoList) {
            LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();
            lineItemOutboundDto.setTransaction(lineItemInboundDto.getTransaction());
            lineItemOutboundDto.setItem(lineItemInboundDto.getItemId());
            try {
                lineItemOutboundDto.setProduct(productService.get(lineItemInboundDto.getProductId()));
            } catch (ProductNotFoundException e) {

            }
            lineItemOutboundDto.setUnitPrice(lineItemInboundDto.getUnitPrice());
            lineItemOutboundDto.setUom(fieldOptionService.getFieldOption(Field.UOM, lineItemInboundDto.getUom()));
            lineItemOutboundDto.setBarcode(lineItemInboundDto.getEan());
            lineItemOutboundDto.setLineTotal(lineItemInboundDto.getQuantity().multiply(lineItemInboundDto.getUnitPrice()));
//            pricingOutboundDto.setTotalExcVat(pricingOutboundDto.getTotalExcVat().add(lineItemInboundDto.getLineTotal()));
//            pricingOutboundDto.setVATAmount(pricingOutboundDto.getTotalExcVat().multiply(pricingOutboundDto.getVATPercentage().divide(new BigDecimal("100"))));
//            pricingOutboundDto.setTotalIncVat(pricingOutboundDto.getTotalExcVat().add((pricingOutboundDto.getVATAmount())));
            if (lineItemOutboundDto.getUnitPrice() != null && lineItemOutboundDto.getQuantity() != null) {
                BigDecimal totalExcVat = lineItemOutboundDto.getUnitPrice().multiply(lineItemOutboundDto.getQuantity());
                pricingOutboundDto.setTotalExcVat(totalExcVat);

                BigDecimal vatAmount = totalExcVat.multiply(pricingOutboundDto.getVATPercentage());
                pricingOutboundDto.setVATAmount(vatAmount);

                BigDecimal totalIncVat = totalExcVat.add(vatAmount);
                pricingOutboundDto.setTotalIncVat(totalIncVat);

                BigDecimal discountAmount = new BigDecimal("0");
                pricingOutboundDto.setDiscountAmount(discountAmount);

                BigDecimal discountPercentage = new BigDecimal("0");
                if (totalExcVat.compareTo(BigDecimal.ZERO) != 0) {
                    discountPercentage = discountAmount.divide(totalExcVat, 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                }
                pricingOutboundDto.setDiscountPercentage(discountPercentage);

                pricingOutboundDto.setVATPercentage(pricingOutboundDto.getVATPercentage().multiply(new BigDecimal("100")));
            }
            lineItemOutboundDtoList.add(lineItemOutboundDto);
        }
        pricingOutboundDto.setItems(lineItemOutboundDtoList);
        return pricingOutboundDto;
    }
}
