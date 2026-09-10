package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.SupplierNetworkService;

import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/supplier-network")
public class SupplierNetworkControllerV2 {
    private final SupplierNetworkService service;
    public SupplierNetworkControllerV2(SupplierNetworkService service){this.service=service;}

    @GetMapping("/connections") public ResponseEntity<?> connections(){return call(service::connections);}
    @PostMapping("/connections") public ResponseEntity<?> connection(@RequestBody SupplierNetworkService.ConnectionRequest request,@RequestHeader(value="X-User-Id",required=false)String user){return call(()->service.saveConnection(request,user));}
    @PostMapping("/purchase-orders/{id}/send") public ResponseEntity<?> send(@PathVariable String id,@RequestHeader(value="X-User-Id",required=false)String user){return call(()->service.sendPurchaseOrder(id,user));}
    @GetMapping("/orders") public ResponseEntity<?> orders(@RequestParam(required=false)String status){return call(()->service.orders(status));}
    @GetMapping("/orders/{id}") public ResponseEntity<?> order(@PathVariable String id){return call(()->service.order(id));}
    @PostMapping("/orders/{id}/response") public ResponseEntity<?> respond(@PathVariable String id,@RequestBody SupplierNetworkService.ResponseRequest request,@RequestHeader(value="X-User-Id",required=false)String user){return call(()->service.respond(id,request,user));}
    @PostMapping("/orders/{id}/reservations") public ResponseEntity<?> reserve(@PathVariable String id,@RequestBody SupplierNetworkService.ReservationRequest request,@RequestHeader(value="X-User-Id",required=false)String user){return call(()->service.reserve(id,request,user));}
    @PostMapping("/orders/{id}/complete") public ResponseEntity<?> complete(@PathVariable String id,@RequestHeader(value="X-User-Id",required=false)String user){return call(()->service.complete(id,user));}
    @PostMapping("/orders/{id}/invoice") public ResponseEntity<?> invoice(@PathVariable String id,@RequestHeader(value="X-User-Id",required=false)String user){return call(()->service.invoice(id,user));}

    private ResponseEntity<?> call(Action action){try{return ResponseEntity.ok(action.run());}catch(Exception e){Map<String,Object>b=new LinkedHashMap<>();b.put("error","SUPPLIER_NETWORK_REQUEST_FAILED");b.put("message",e.getMessage()==null?"Unable to process supplier-network request":e.getMessage());return ResponseEntity.badRequest().body(b);}}
    private interface Action{Object run();}
}
