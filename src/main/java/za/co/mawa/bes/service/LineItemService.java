package za.co.mawa.bes.service;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.LineItemEditDto;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.LineItemOutboundDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.entity.transaction.TransactionItemPKEntity;
import za.co.mawa.bes.repository.TransactionItemRepository;
import za.co.mawa.bes.utils.Field;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Service
public class LineItemService {
    @Autowired
    TransactionService transactionService;
    Gson gson = new Gson();
    @Autowired
    ProductService productService;
    @Autowired
    TransactionItemRepository transactionItemRepository;
    @Autowired
    FieldOptionService fieldOptionService;
    private String id;
//
//    public List<LineItemOutboundDto> getAll(String transaction) {
//        try{
//            List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
//            List<TransactionItemDto> transactionItemDtoList = transactionService.getItems(transaction);
//            for(TransactionItemDto transactionItemDto: transactionItemDtoList){
//                LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();
//                lineItemOutboundDto.setTransaction(transactionItemDto.getTransaction());
//                lineItemOutboundDto.setItem(transactionItemDto.getItem());
//                lineItemOutboundDto.setProduct(productService.get(transactionItemDto.getProduct()));
//                lineItemOutboundDto.setUnitPrice(transactionItemDto.getUnitPrice());
//                lineItemOutboundDto.setQuantity(transactionItemDto.getQuantity());
//                lineItemOutboundDto.setUom(fieldOptionService.getFieldOption(Field.UOM, transactionItemDto.getBaseUnitOfMeasure()));
//                if(lineItemOutboundDto.getUnitPrice() != null){
//                    lineItemOutboundDto.setLineTotal(lineItemOutboundDto.getQuantity().multiply(lineItemOutboundDto.getUnitPrice()));
//                }
//                lineItemOutboundDtoList.add(lineItemOutboundDto);
//            }
//            return lineItemOutboundDtoList;
//        }catch (Exception ex){
//            throw new RuntimeException(ex);
//        }
//
//    }
    public List<LineItemOutboundDto> getAll(String transaction) {
        try {
            List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
            List<TransactionItemDto> transactionItemDtoList = transactionService.getItems(transaction);
            for (TransactionItemDto transactionItemDto : transactionItemDtoList) {
                LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();

                lineItemOutboundDto.setTransaction(transactionItemDto.getTransaction());
                lineItemOutboundDto.setItem(transactionItemDto.getItem());
                lineItemOutboundDto.setProduct(productService.get(transactionItemDto.getProduct()));
                lineItemOutboundDto.setUnitPrice(transactionItemDto.getUnitPrice());
                lineItemOutboundDto.setQuantity(transactionItemDto.getQuantity());
                lineItemOutboundDto.setUom(fieldOptionService.getFieldOption(Field.UOM, transactionItemDto.getBaseUnitOfMeasure()));

                BigDecimal lineTotal = lineItemOutboundDto.getQuantity().multiply(lineItemOutboundDto.getUnitPrice());
                lineItemOutboundDto.setLineTotal(lineTotal);

                BigDecimal vatPercentage = new BigDecimal("15"); // VAT is 15%
                BigDecimal vatAmount = lineTotal.multiply(vatPercentage).divide(new BigDecimal("100"));
                BigDecimal totalIncVat = lineTotal.add(vatAmount);

                lineItemOutboundDto.setTotExcVat(lineTotal);
                lineItemOutboundDto.setVatAmount(vatAmount);
                lineItemOutboundDto.setVatPercentage(vatPercentage);
                lineItemOutboundDto.setTotIncVat(totalIncVat);

                BigDecimal discountAmount = transactionItemDto.getDiscountAmount();
                lineItemOutboundDto.setDiscountAmount(discountAmount);

                if (discountAmount != null) {
                    BigDecimal totalExcAfterDiscount = lineTotal.subtract(discountAmount);
                    BigDecimal vatAmountAfterDiscount = totalExcAfterDiscount.multiply(vatPercentage).divide(new BigDecimal("100"));
                    BigDecimal totalIncAfterDiscount = totalExcAfterDiscount.add(vatAmountAfterDiscount);

                    lineItemOutboundDto.setTotExcVat(totalExcAfterDiscount);
                    lineItemOutboundDto.setVatAmount(vatAmountAfterDiscount);
                    lineItemOutboundDto.setTotIncVat(totalIncAfterDiscount);
                }

                lineItemOutboundDtoList.add(lineItemOutboundDto);
            }
            return lineItemOutboundDtoList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void add(LineItemInboundDto lineItemInboundDto) {
        try {
            ProductDto productDto = productService.get(lineItemInboundDto.getProductId());
            TransactionItemDto transactionItemDto = new TransactionItemDto();
            transactionItemDto.setTransaction(lineItemInboundDto.getTransaction());
            transactionItemDto.setProduct(productDto.getId());
            transactionItemDto.setUnitPrice(lineItemInboundDto.getUnitPrice());
            transactionItemDto.setBaseUnitOfMeasure(lineItemInboundDto.getUom());
            transactionItemDto.setQuantity(lineItemInboundDto.getQuantity());
            transactionService.addItem(transactionItemDto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public void edit(LineItemInboundDto lineItemInboundDto) {
        try{
            TransactionItemPKEntity pkEntity = new TransactionItemPKEntity();
            pkEntity.setTransaction(lineItemInboundDto.getTransaction());
            pkEntity.setItem(lineItemInboundDto.getItemId());
            TransactionItemEntity entity = transactionItemRepository.getById(pkEntity);
            if(!lineItemInboundDto.getQuantity().equals(BigDecimal.ZERO)){
                entity.setQuantity(lineItemInboundDto.getQuantity());
            }
            if(lineItemInboundDto.getUom() != null && lineItemInboundDto.getUom() != ""){
                entity.setUnitOfMeasure(lineItemInboundDto.getUom());
            }
            if(!lineItemInboundDto.getUnitPrice().equals(BigDecimal.ZERO)){
                entity.setUnitPrice(lineItemInboundDto.getUnitPrice());
            }
            transactionItemRepository.save(entity);
        }catch(Exception ex){
          throw new RuntimeException(ex);
        }
    }
    public void delete(String id) {

    }
    public void deleteItem(String id,String itemId) throws Exception{
        try{
           if(itemId != ""){
               TransactionItemPKEntity pkEntity = new TransactionItemPKEntity();
               pkEntity.setItem(itemId);
               pkEntity.setTransaction(id);
               transactionItemRepository.deleteById(pkEntity);
           }
           else{
               for(TransactionItemDto item: transactionService.getItems(id)){
                   TransactionItemPKEntity pkEntity = new TransactionItemPKEntity();
                   pkEntity.setItem(item.getItem());
                   pkEntity.setTransaction(id);
                   transactionItemRepository.deleteById(pkEntity);
               }
           }
        }catch (Exception ex){
           throw new RuntimeException(ex);
        }

    }
}
