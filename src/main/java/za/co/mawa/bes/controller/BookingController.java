package za.co.mawa.bes.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.booking.BookingCreateDto;
import za.co.mawa.bes.dto.booking.BookingDto;
import za.co.mawa.bes.dto.booking.BookingEditDto;
import za.co.mawa.bes.dto.booking.BookingQueryDto;
import za.co.mawa.bes.service.BookingService;

@RestController
@CrossOrigin
@RequestMapping(value = "booking")
public class BookingController {
    Gson gson = new Gson();
    @Autowired
    BookingService bookingService;

    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> createLayBy(@RequestBody BookingCreateDto createDto){
        try{
            BookingDto bookingDto = new BookingDto();
            bookingDto.setId(bookingService.createBooking(createDto));
            return ResponseEntity.ok(gson.toJson(bookingDto));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e);
        }
    }

    @RequestMapping(value= "/{id}" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> getBooking(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(bookingService.getBooking(id)));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e);
        }
    }
    @RequestMapping( method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> getBookings(@RequestParam(required = false) String bookDate,
                                  @RequestParam(required = false) String employeeId,
                                  @RequestParam(required = false) String customerId,
                                  @RequestParam(required = false) String status){
        try{
            BookingQueryDto queryDto = new BookingQueryDto();
            String dateBook = bookDate == null ? "":bookDate;
            String employee = employeeId == null ? "" :employeeId;
            String customer  = customerId == null ? "" :customerId;
            String bookStatus = status == null ? "": status;
            if(dateBook != ""){
                queryDto.setBookDate(dateBook);
            }
            if(employee != ""){
               queryDto.setEmployeeId(employee);
            }
            if(customer != ""){
               queryDto.setCustomerId(customer);
            }
            if(bookStatus != ""){
               queryDto.setStatus(bookStatus);
            }
            return ResponseEntity.ok(gson.toJson(bookingService.querBooking(queryDto)));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e);
        }
    }

    @RequestMapping(value= "/{id}" , method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> editBookings(@PathVariable String id, @RequestBody BookingEditDto editDto){
        try{
            return ResponseEntity.ok(gson.toJson(bookingService.editBooking(editDto,id)));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e);
        }
    }
    @RequestMapping(value= "/{id}" , method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> deleteBookings(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(bookingService.removeBooking(id)));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e);
        }
    }

}
