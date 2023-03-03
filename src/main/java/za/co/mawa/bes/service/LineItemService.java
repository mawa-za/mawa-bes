package za.co.mawa.bes.service;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import za.co.mawa.bes.controller.ItemsController;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;

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
    PricingService pricingService;
    private String id;

    public List<LineItemDto> getAll(String transaction) {
        List<LineItemDto> lineItemDtoList = new ArrayList<>();
        List<TransactionItemDto> transactionItemDtoList = transactionService.getItems(transaction);
        for(TransactionItemDto transactionItemDto: transactionItemDtoList){
            LineItemDto lineItemDto = new LineItemDto();
//            lineItemDto.setTransaction(transactionItemDto.getTransaction());
            lineItemDto.setProductId(transactionItemDto.getProduct());
            lineItemDto.setUnitPrice(transactionItemDto.getUnitPrice());
            lineItemDto.setQuantity(transactionItemDto.getQuantity());
            lineItemDto.setUom(transactionItemDto.getBaseUnitOfMeasure());
            lineItemDto.setLineTotal(lineItemDto.getQuantity().multiply(lineItemDto.getUnitPrice()));
            lineItemDtoList.add(lineItemDto);
        }
        return lineItemDtoList;
    }
    public void add( String transaction, LineItemDto lineItemDto) {
        try {
            ProductDto productDto = productService.get(lineItemDto.getProductId());
            TransactionItemDto transactionItemDto = new TransactionItemDto();
            transactionItemDto.setTransaction(transaction);
            transactionItemDto.setProduct(productDto.getId());
            transactionItemDto.setUnitPrice(lineItemDto.getUnitPrice());
            transactionItemDto.setBaseUnitOfMeasure(lineItemDto.getUom());
            transactionItemDto.setQuantity(lineItemDto.getQuantity());
            transactionService.addItem(transactionItemDto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public void edit() {
    }
    public void delete(String id) {
    }
}
