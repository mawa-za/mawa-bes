package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.LineItemOutboundDto;
import za.co.mawa.bes.dto.PricingOutboundDto;
import za.co.mawa.bes.exception.ProductNotFoundException;
import za.co.mawa.bes.utils.Field;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingEngineService {
    @Autowired
    ProductService productService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    LineItemService lineItemService;
//    public PricingOutboundDto calculate(String id) {
//        List<LineItemOutboundDto> lineItemOutboundDtoList = lineItemService.getAll(id);
//        List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
//        PricingOutboundDto pricingOutboundDto = new PricingOutboundDto();
//        pricingOutboundDto.setVATPercentage(new BigDecimal("15"));
//        for (LineItemInboundDto lineItemInboundDto : lineItemInboundDtoList) {
//            LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();
//            lineItemOutboundDto.setTransaction(lineItemInboundDto.getTransaction());
//            lineItemOutboundDto.setItem(lineItemInboundDto.getItemId());
//            try {
//                lineItemOutboundDto.setProduct(productService.get(lineItemInboundDto.getProductId()));
//            } catch (ProductNotFoundException e) {
//
//            }
//            lineItemOutboundDto.setUnitPrice(lineItemInboundDto.getUnitPrice());
//            lineItemOutboundDto.setUom(fieldOptionService.getFieldOption(Field.UOM, lineItemInboundDto.getUom()));
//            lineItemOutboundDto.setBarcode(lineItemInboundDto.getEan());
//            lineItemOutboundDto.setLineTotal(lineItemInboundDto.getQuantity().multiply(lineItemInboundDto.getUnitPrice()));
//            pricingOutboundDto.setTotalExcVat(pricingOutboundDto.getTotalExcVat().add(lineItemInboundDto.getLineTotal()));
//            pricingOutboundDto.setVATAmount(pricingOutboundDto.getTotalExcVat().multiply(pricingOutboundDto.getVATPercentage().divide(new BigDecimal("100"))));
//            pricingOutboundDto.setTotalIncVat(pricingOutboundDto.getTotalExcVat().add((pricingOutboundDto.getVATAmount())));
//            lineItemOutboundDtoList.add(lineItemOutboundDto);
//        }
//        pricingOutboundDto.setItems(lineItemOutboundDtoList);
//        return pricingOutboundDto;
//    }

    public PricingOutboundDto simulate(List<LineItemInboundDto> lineItemInboundDtoList) {
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
            pricingOutboundDto.setTotalExcVat(pricingOutboundDto.getTotalExcVat().add(lineItemInboundDto.getLineTotal()));
            pricingOutboundDto.setVATAmount(pricingOutboundDto.getTotalExcVat().multiply(pricingOutboundDto.getVATPercentage().divide(new BigDecimal("100"))));
            pricingOutboundDto.setTotalIncVat(pricingOutboundDto.getTotalExcVat().add((pricingOutboundDto.getVATAmount())));
            lineItemOutboundDtoList.add(lineItemOutboundDto);
        }
        pricingOutboundDto.setItems(lineItemOutboundDtoList);
        return pricingOutboundDto;
    }
}
