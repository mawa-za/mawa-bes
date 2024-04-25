//package za.co.mawa.bes.controller;
//
//import com.nimbusds.jose.shaded.gson.Gson;
//import jakarta.persistence.Column;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.bind.annotation.*;
//import za.co.mawa.bes.dto.LineItemDto;
//import za.co.mawa.bes.dto.product.ProductDto;
//import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
//import za.co.mawa.bes.service.LineItemService;
//import za.co.mawa.bes.service.PricingService;
//import za.co.mawa.bes.service.ProductService;
//import za.co.mawa.bes.service.TransactionService;
//
//import java.util.ArrayList;
//import java.util.List;
//@RestController("items")
//@CrossOrigin
//@RequestMapping("{id}/items")
//public class ItemsController {
//    @Autowired
//    TransactionService transactionService;
//    Gson gson = new Gson();
//    @Autowired
//    ProductService productService;
//    @Autowired
//    PricingService pricingService;
//    private String id;
//    private String transaction;
//    private ItemsController itemsController;
//    @Autowired
//    LineItemService lineItemService;
//
//    public ItemsController(){}
////    @RequestMapping(method = RequestMethod.GET)
//    public void get() {
//    }
//    @RequestMapping(method = RequestMethod.GET)
//    public List<LineItemDto> getAll(@PathVariable String id) {
//        List<LineItemDto> lineItemDtoList = new ArrayList<>();
//       List<TransactionItemDto> transactionItemDtoList = transactionService.getItems(transaction);
//        for(TransactionItemDto transactionItemDto: transactionItemDtoList){
//            LineItemDto lineItemDto = new LineItemDto();
//            lineItemDto.setTransaction(transactionItemDto.getTransaction());
//            lineItemDto.setProductId(transactionItemDto.getProduct());
//            lineItemDtoList.add(lineItemDto);
//        }
//        return lineItemDtoList;
//    }
//    @RequestMapping(method = RequestMethod.POST)
//    public void post(@PathVariable String id, LineItemDto lineItemDto) {
//        try {
//            ProductDto productDto = productService.get(lineItemDto.getProductId());
//            TransactionItemDto transactionItemDto = new TransactionItemDto();
//            transactionItemDto.setTransaction(transaction);
//            transactionItemDto.setProduct(productDto.getId());
//            transactionItemDto.setUnitPrice(lineItemDto.getUnitPrice());
//            transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure().getCode());
//            transactionItemDto.setQuantity(lineItemDto.getQuantity());
//            transactionService.addItem(transactionItemDto);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//    public void put() {
//    }
//    public void delete() {
//    }
//
//}
