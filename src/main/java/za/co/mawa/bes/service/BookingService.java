package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.BookingDao;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.booking.BookingCreateDto;
import za.co.mawa.bes.dto.booking.BookingDto;
import za.co.mawa.bes.dto.booking.BookingEditDto;
import za.co.mawa.bes.dto.booking.BookingQueryDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

@Service
public class BookingService implements BookingDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    @Override
    public String createBooking(BookingCreateDto createDto) throws Exception {
        try{
            TransactionCreateDto transactionCreate = new TransactionCreateDto();
            transactionCreate.setType(TransactionType.APPOINTMENT);
            transactionCreate.setStatus(Status.BOOKED);
            TransactionDto transactionDto = transactionService.create(transactionCreate);
            if(transactionDto.getId() != null){
                String bookTime = createDto.getBookTime() == null ? "": createDto.getBookTime();
                String bookDate = createDto.getBookDate() == null ?"":createDto.getBookDate();
                if(bookTime != "" && bookDate != ""){
                    //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                   // Date dateBook = dateFormat.parse(bookDate);
                   // SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                   // Date timeBook = timeFormat.parse(bookTime);
                   // Date bookTimeDate = new Date(dateBook.getTime() + timeBook.getTime());

                    String bookTimeDate = bookDate+ " " + bookTime;

                    TransactionDateDto dates = new TransactionDateDto();
                    dates.setType(DateType.BOOKING_DATE);
                    dates.setTransaction(transactionDto.getId());
                    dates.setValue(Conversion.dateTimeToString2(bookTimeDate));
                    transactionService.addDate(dates);
                }

                TransactionDateDto dates = new TransactionDateDto();
                dates.setType(DateType.CREATED);
                dates.setTransaction(transactionDto.getId());
                dates.setValue(new Date());
                transactionService.addDate(dates);

                if(createDto.getCustomerId() != null){
                    TransactionPartnerDto customer = new TransactionPartnerDto();
                    customer.setTransaction(transactionDto.getId());
                    customer.setPartner(createDto.getCustomerId());
                    customer.setFunction(PartnerFunction.CUSTOMER);
                    transactionService.addPartner(customer);
                }
                if(createDto.getEmployeeId() != null){
                    TransactionPartnerDto employee = new TransactionPartnerDto();
                    employee.setFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
                    employee.setPartner(createDto.getEmployeeId());
                    employee.setTransaction(transactionDto.getId());
                    transactionService.addPartner(employee);
                }
                if(createDto.getProductId() != null){
                    ProductDto productDto = productService.get(createDto.getProductId());
                    TransactionItemDto transactionItemDto = new TransactionItemDto();
                    transactionItemDto.setProduct(productDto.getId());
                    transactionItemDto.setTransaction(transactionDto.getId());
                    transactionItemDto.setQuantity(new BigDecimal("1"));
                    transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure().getCode());
//                    transactionItemDto.setUnitPrice(productDto.getSellingPrice());
                    transactionService.addItem(transactionItemDto);
                }
                return transactionDto.getId();
            }
            else {
                return null;
            }

        }catch(Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    public BookingDto getBooking(String id) throws Exception {
        BookingDto bookingDto = new BookingDto();
        TransactionDto transactionDto = transactionService.get(id);
        if(transactionDto.getType().equalsIgnoreCase(TransactionType.APPOINTMENT)){
           bookingDto.setId(transactionDto.getId());
           bookingDto.setNumber(transactionDto.getNumber());
           bookingDto.setStatus(transactionDto.getStatus());
           for(TransactionPartnerDto partner:transactionService.getPartners(id)){
               if(partner.getFunction().equalsIgnoreCase(PartnerFunction.CUSTOMER)){
                   try{
                       PartnerDto partnerDto = partnerService.get(partner.getPartner());
                       bookingDto.setCustomer(partnerDto);
                   }catch(PartnerNotFoundException ex){

                   }
               }
               if(partner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)){
                   try{
                       PartnerDto partnerDto = partnerService.get(partner.getPartner());
                       bookingDto.setEmployeeResponsible(partnerDto);
                   }catch(PartnerNotFoundException ex){

                   }
               }
           }
           for(TransactionDateDto dates:transactionService.getDates(id)){
             if(dates.getType().equalsIgnoreCase(DateType.BOOKING_DATE)){
                 bookingDto.setBookDate(Conversion.dateToString(dates.getValue()));
                 bookingDto.setBookTime(Conversion.time2ToString(dates.getValue()));
                 //break;
             }
             if(dates.getType().equalsIgnoreCase(DateType.CREATED)){
                 bookingDto.setCreatedOn(Conversion.dateToString(dates.getValue()));
             }
           }


            String productId = "";
            for(TransactionItemDto item:transactionService.getItems(transactionDto.getId())){
                int number =  item.getValidTo().compareTo(new Date());
                if(number > 0){
                    productId = item.getProduct();
                }
            }
            ProductDto productDto = productService.getOptionalById(productId);
            if(productDto != null){
                bookingDto.setProductDto(productDto);
                ProductAttributeQueryDto queryDto = new ProductAttributeQueryDto();
                queryDto.setProduct(productId);
                //queryDto.setAttribute("");
                for(ProductAttributeDto attributeDto: productService.getAttributes(productId)){
                  if(attributeDto.getAttribute().getCode().equalsIgnoreCase("DURATION")){
                      bookingDto.setDuration(attributeDto.getValue());
                      break;
                  }
                }
            }

        }
        return bookingDto;
    }

    @Override
    public ArrayList<BookingDto> querBooking(BookingQueryDto queryDto) throws Exception {
        try{
            TransactionQueryDto query = new TransactionQueryDto();
            ArrayList<BookingDto> bookings = new ArrayList<>();
            query.setType(TransactionType.APPOINTMENT);
            if(queryDto.getBookDate() != null){
                query.setValue(Conversion.stringToDate(queryDto.getBookDate()));
                query.setDateType(DateType.BOOKING_DATE);
            }
            if(queryDto.getCustomerId() != null){
                query.setPartnerNo(queryDto.getCustomerId());
                query.setPartnerFunction(PartnerFunction.CUSTOMER);
            }
            if(queryDto.getEmployeeId() != null){
                query.setPartnerNo(queryDto.getEmployeeId());
                query.setPartnerFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
            }
            if(queryDto.getStatus() != null){
               query.setStatus(queryDto.getStatus());
            }
            for(String id:transactionService.search(query)){
                BookingDto booking = getBooking(id);
                bookings.add(booking);
            }
            return bookings;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean editBooking(BookingEditDto editDto,String id) throws Exception {
        try{
            if(editDto.getEmployeeId() != null){
                TransactionPartnerEdit partnerEdit = new TransactionPartnerEdit();
                partnerEdit.setPartnerFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
                partnerEdit.setTransaction(id);
                partnerEdit.setParnter(editDto.getEmployeeId());
                transactionService.partnerEdit(partnerEdit);
            }
            if(editDto.getBookDate() != null && editDto.getBookTime() != null){
                String bookTimeDate = editDto.getBookDate()+ " " + editDto.getBookTime();
                TransactionDateEdit bookEdit = new TransactionDateEdit();
                bookEdit.setType(DateType.BOOKING_DATE);
                bookEdit.setTransaction(id);
                bookEdit.setValue(Conversion.dateTimeToString2(bookTimeDate));
                transactionService.dateEdit(bookEdit);
            }
            if(editDto.getStatus() != null){
                TransactionEditDto edit = new TransactionEditDto();
                edit.setId(id);
                edit.setStatus(editDto.getStatus());
                transactionService.edit(edit);
            }
            return true;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean removeBooking(String id) throws Exception {
        try{
            TransactionEditDto edit = new TransactionEditDto();
            edit.setId(id);
            edit.setStatus(Status.CANCELLED);
            transactionService.edit(edit);

            return true;
        }catch (Exception ex){
           throw new RuntimeException(ex);
        }

    }
}
