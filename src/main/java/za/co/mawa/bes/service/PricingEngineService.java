package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.LineItemOutboundDto;
import za.co.mawa.bes.dto.PricingInboundDto;
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

    public PricingOutboundDto calculate(String id, PricingInboundDto pricingInboundDto) {
        List<LineItemOutboundDto> lineItemOutboundDtoList = lineItemService.getAll(id);
//        List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
        PricingOutboundDto pricingOutboundDto = new PricingOutboundDto();
//        LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();

        pricingOutboundDto.setVATPercentage(new BigDecimal("15"));
        LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();
        for (LineItemInboundDto lineItemInboundDto : pricingInboundDto.getItems()) {

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

    public PricingOutboundDto simulate(PricingInboundDto pricingInboundDto) {
        List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
        PricingOutboundDto pricingOutboundDto = new PricingOutboundDto();
        pricingOutboundDto.setVATPercentage(new BigDecimal("15"));
        BigDecimal totalExcVat = new BigDecimal("0");
        for (LineItemInboundDto lineItemInboundDto : pricingInboundDto.getItems()) {
            LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();
            lineItemOutboundDto.setTransaction(lineItemInboundDto.getTransaction());
            lineItemOutboundDto.setItem(lineItemInboundDto.getItemId());
            try {
                lineItemOutboundDto.setProduct(productService.get(lineItemInboundDto.getProductId()));
            } catch (ProductNotFoundException e) {

            }
            lineItemOutboundDto.setUnitPrice(lineItemInboundDto.getUnitPrice());
            lineItemOutboundDto.setQuantity(lineItemInboundDto.getQuantity());
            lineItemOutboundDto.setUom(fieldOptionService.getFieldOption(Field.UOM, lineItemInboundDto.getUom()));
            lineItemOutboundDto.setBarcode(lineItemInboundDto.getEan());
            lineItemOutboundDto.setLineTotal(lineItemInboundDto.getQuantity().multiply(lineItemInboundDto.getUnitPrice()));
            totalExcVat = totalExcVat.add(lineItemOutboundDto.getLineTotal());
            lineItemOutboundDtoList.add(lineItemOutboundDto);
        }
        pricingOutboundDto.setTotalExcVat(totalExcVat);
        pricingOutboundDto.setVATAmount(pricingOutboundDto.getTotalExcVat().multiply(pricingOutboundDto.getVATPercentage().divide(new BigDecimal("100"))));
        pricingOutboundDto.setTotalIncVat(pricingOutboundDto.getTotalExcVat().add((pricingOutboundDto.getVATAmount())));
        pricingOutboundDto.setItems(lineItemOutboundDtoList);
        return pricingOutboundDto;
    }
//    public PricingOutboundDto simulate(PricingInboundDto pricingInboundDto) {
//        List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
//        PricingOutboundDto pricingOutboundDto = new PricingOutboundDto();
//        BigDecimal vatPercentage = new BigDecimal("15"); // VAT percentage in percentage form
//        pricingOutboundDto.setVATPercentage(vatPercentage);
//        BigDecimal totalExcVat = BigDecimal.ZERO;
//
//        for (LineItemInboundDto lineItemInboundDto : pricingInboundDto.getItems()) {
//            LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();
//            lineItemOutboundDto.setTransaction(lineItemInboundDto.getTransaction());
//            lineItemOutboundDto.setItem(lineItemInboundDto.getItemId());
//
//            try {
//                lineItemOutboundDto.setProduct(productService.get(lineItemInboundDto.getProductId()));
//            } catch (ProductNotFoundException e) {
//                // Handle exception or continue
//            }
//
//            BigDecimal unitPrice = lineItemInboundDto.getUnitPrice();
//            BigDecimal quantity = lineItemInboundDto.getQuantity();
//
//            lineItemOutboundDto.setUnitPrice(unitPrice);
//            lineItemOutboundDto.setQuantity(quantity);
//            lineItemOutboundDto.setUom(fieldOptionService.getFieldOption(Field.UOM, lineItemInboundDto.getUom()));
//            lineItemOutboundDto.setBarcode(lineItemInboundDto.getEan());
//
//            BigDecimal lineTotal = unitPrice.multiply(quantity);
//            lineItemOutboundDto.setLineTotal(lineTotal);
//
//            totalExcVat = totalExcVat.add(lineTotal);
//
//            lineItemOutboundDtoList.add(lineItemOutboundDto);
//        }
//
//        pricingOutboundDto.setTotalExcVat(totalExcVat);
//
//        BigDecimal vatAmount = totalExcVat.multiply(vatPercentage).divide(new BigDecimal("100"));
//        pricingOutboundDto.setVATAmount(vatAmount);
//
//        BigDecimal totalIncVat = totalExcVat.add(vatAmount);
//        pricingOutboundDto.setTotalIncVat(totalIncVat);
//
//        pricingOutboundDto.setItems(lineItemOutboundDtoList);
//
//        return pricingOutboundDto;
//    }

}
