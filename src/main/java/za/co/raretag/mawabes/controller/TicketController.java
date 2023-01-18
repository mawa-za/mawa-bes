package za.co.raretag.mawabes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.raretag.mawabes.dto.TicketDto;
import za.co.raretag.mawabes.service.TicketService;
import za.co.raretag.mawabes.service.UserService;

@RestController
@CrossOrigin
public class TicketController {
    Gson gson = new Gson();
    @Autowired
    TicketService ticketService;
    @Autowired
    UserService userService;

    @RequestMapping(value = "/tickets", method = RequestMethod.POST)
    public ResponseEntity<?> createTicket (@RequestBody TicketDto ticketDto){
        return ResponseEntity.ok(ticketService.create(ticketDto));
    }

    @RequestMapping(value = "/ticket/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getTicket (@PathVariable("ticketID") String ticketID) throws Exception{
        return ResponseEntity.ok(ticketService.getTicketsById(ticketID));
    }

//    @RequestMapping(value = "tickets/getTicketsByDate", method = RequestMethod.GET)
//    public ResponseEntity<?> getTicketsByDateJson (@PathVariable("startDate") String startDate,
//                                                   @PathVariable("endDate") String endDate, @PathVariable("type") String type,
//                                                   @PathVariable("status") String status) throws Exception{
//        if(startDate != null && endDate != null){
//            ArrayList<TicketDto> tickets = ticketService.getTicketsByDates(startDate, endDate, type, status);
//            String response = gson.toJson(tickets);
//            return ResponseEntity.ok(response);
//        } else {
//            String response = "false";
//            return ResponseEntity.status(false).build();
//        }
//    }


//    @GetMapping("/ticket")
//    private List<TransactionEntity> getAllTickets(){
//        return ticketService.getAllTickets();
//    }
//
//    @GetMapping("/ticket/{ticketID}")
//    private TransactionEntity getTickets(@PathVariable("ticketID") String ticketID){
//        return ticketService.getTicketsById(ticketID);
//    }
//
//    @DeleteMapping("/ticket/{ticketID}")
//    public void deleteTicket(@PathVariable("ticketID") String ticketID){
//        ticketService.delete(ticketID);
//    }
//
//    @PostMapping("/ticket")
//    private String saveTicket(@RequestBody TransactionEntity tickets){
//        OrderDateDto orderDateDto = new OrderDateDto();
//        orderDateDto.setType(OrderType.TICKET);
//        //orderDateDto.setSubType(tickets.);
//
//        ticketService.saveOrUpdate(tickets);
//        return tickets.getId();
//    }
//
//    @PutMapping("/tickets")
//    private TransactionEntity update(@RequestBody TransactionEntity tickets){
//        ticketService.saveOrUpdate(tickets);
//        return tickets;
//    }

}
