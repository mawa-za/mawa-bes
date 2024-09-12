package za.co.mawa.bes.service;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.LineItemOutboundDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.entity.transaction.TransactionItemPKEntity;
import za.co.mawa.bes.repository.TransactionItemRepository;
import za.co.mawa.bes.utils.Field;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
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

    public List<LineItemOutboundDto> getAll(String transaction) {
        try{
            List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
            List<TransactionItemDto> transactionItemDtoList = transactionService.getItems(transaction);

            for(TransactionItemDto transactionItemDto: transactionItemDtoList){
                LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();
                lineItemOutboundDto.setTransaction(transactionItemDto.getTransaction());
                lineItemOutboundDto.setItem(transactionItemDto.getItem());

                try {
                    lineItemOutboundDto.setProduct(productService.get(transactionItemDto.getProduct()));
                } catch (Exception e) {
                }

                BigDecimal unitPrice = transactionItemDto.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
                lineItemOutboundDto.setUnitPrice(unitPrice);

                BigDecimal quantity = (transactionItemDto.getQuantity().compareTo(BigDecimal.ZERO) == 0)
                        ? BigDecimal.valueOf(1)
                        : transactionItemDto.getQuantity().setScale(2, RoundingMode.HALF_UP);
                lineItemOutboundDto.setQuantity(quantity);

                lineItemOutboundDto.setUom(fieldOptionService.getFieldOption(Field.UOM, transactionItemDto.getBaseUnitOfMeasure()));

                BigDecimal lineTotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                lineItemOutboundDto.setLineTotal(lineTotal);

                BigDecimal discountPercentage = lineItemOutboundDto.getDiscountPercentage();
                BigDecimal discountAmount;

                boolean isVatInclusive = lineItemOutboundDto.isVatInclusive();

                if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) != 0 && !isValidToDateReached(transactionItemDto.getValidTo())) {
                    discountAmount = lineTotal.multiply(discountPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    lineItemOutboundDto.setDiscountAmount(discountAmount);

                    BigDecimal totalExcAfterDiscount = lineTotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
                    lineItemOutboundDto.setTotExcVat(totalExcAfterDiscount);

                    if (isVatInclusive) {
                        BigDecimal vatAmountAfterDiscount = totalExcAfterDiscount.multiply(vatPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        BigDecimal totalIncAfterDiscount = totalExcAfterDiscount.add(vatAmountAfterDiscount).setScale(2, RoundingMode.HALF_UP);

                        lineItemOutboundDto.setVatAmount(vatAmountAfterDiscount);
                        lineItemOutboundDto.setTotIncVat(totalIncAfterDiscount);
                    } else {
                        lineItemOutboundDto.setVatAmount(BigDecimal.ZERO);
                        lineItemOutboundDto.setTotIncVat(totalExcAfterDiscount);
                    }

                } else {
                    lineItemOutboundDto.setTotExcVat(lineTotal.setScale(2, RoundingMode.HALF_UP));

                    if (isVatInclusive) {
                        BigDecimal vatAmount = lineTotal.multiply(vatPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        BigDecimal totalIncVat = lineTotal.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

                        lineItemOutboundDto.setVatAmount(vatAmount);
                        lineItemOutboundDto.setTotIncVat(totalIncVat);
                    } else {
                        lineItemOutboundDto.setVatAmount(BigDecimal.ZERO);
                        lineItemOutboundDto.setTotIncVat(lineTotal);
                    }
                }

                lineItemOutboundDto.setVatPercentage(isVatInclusive ? vatPercentage.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

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

    public static boolean isValidToDateReached(Date validTo) {
        Date currentDate = new Date(System.currentTimeMillis());

        return !currentDate.before(validTo);
    }
}