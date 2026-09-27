package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.stock.InventoryDtos;
import za.co.mawa.bes.service.v2.InventoryManagementService;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/inventory")
public class InventoryManagementControllerV2 {
    private final InventoryManagementService service;
    public InventoryManagementControllerV2(InventoryManagementService service) {
        this.service=service;
    }

    @GetMapping("/availability")
    public ResponseEntity<List<Map<String,Object>>> availability(@RequestParam(required=false) String productId,@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.availability(productId,warehouseId));}

    @GetMapping("/documents")
    public ResponseEntity<List<Map<String,Object>>> documents(@RequestParam(required=false) String type,@RequestParam(required=false) String status,@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.documents(type,status,warehouseId));}

    @GetMapping("/documents/{id}")
    public ResponseEntity<Map<String,Object>> document(@PathVariable String id){return ResponseEntity.ok(service.authorizedDocument(id));}

    @PostMapping("/documents")
    public ResponseEntity<Map<String,Object>> createDocument(@RequestBody InventoryDtos.InventoryDocumentRequest request){return ResponseEntity.ok(service.createDocument(request));}

    @PostMapping("/documents/{id}/post")
    public ResponseEntity<Map<String,Object>> postDocument(@PathVariable String id){return ResponseEntity.ok(service.postDocument(id));}

    @PostMapping("/documents/{id}/submit")
    public ResponseEntity<Map<String,Object>> submitDocument(@PathVariable String id){return ResponseEntity.ok(service.submitForApproval(id));}


    @PostMapping("/warehouse-transfers")
    public ResponseEntity<Map<String,Object>> createWarehouseTransfer(@RequestBody InventoryDtos.InventoryDocumentRequest request){return ResponseEntity.ok(service.createWarehouseTransfer(request));}

    @PostMapping("/warehouse-transfers/{id}/submit")
    public ResponseEntity<Map<String,Object>> submitWarehouseTransfer(@PathVariable String id){return ResponseEntity.ok(service.submitForApproval(id));}

    @PostMapping("/warehouse-transfers/{id}/dispatch")
    public ResponseEntity<Map<String,Object>> dispatchWarehouseTransfer(@PathVariable String id){return ResponseEntity.ok(service.dispatchWarehouseTransfer(id));}

    @PostMapping("/warehouse-transfers/{id}/receive")
    public ResponseEntity<Map<String,Object>> receiveWarehouseTransfer(@PathVariable String id,@RequestBody InventoryDtos.WarehouseTransferReceiptRequest request){return ResponseEntity.ok(service.receiveWarehouseTransfer(id,request));}

    @GetMapping("/stock-counts")
    public ResponseEntity<List<Map<String,Object>>> stockCounts(@RequestParam(required=false) String status,@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.stockCounts(status,warehouseId));}

    @GetMapping("/stock-counts/{id}")
    public ResponseEntity<Map<String,Object>> stockCount(@PathVariable String id){return ResponseEntity.ok(service.stockCount(id));}

    @PostMapping("/stock-counts")
    public ResponseEntity<Map<String,Object>> createStockCount(@RequestBody InventoryDtos.StockCountCreateRequest request){return ResponseEntity.ok(service.createStockCount(request));}

    @PutMapping("/stock-counts/{id}/counts")
    public ResponseEntity<Map<String,Object>> recordStockCount(@PathVariable String id,@RequestBody InventoryDtos.StockCountUpdateRequest request){return ResponseEntity.ok(service.recordStockCount(id,request));}

    @PostMapping("/stock-counts/{id}/finalize")
    public ResponseEntity<Map<String,Object>> finalizeStockCount(@PathVariable String id){return ResponseEntity.ok(service.finalizeStockCount(id));}

    @GetMapping("/reservations")
    public ResponseEntity<List<Map<String,Object>>> reservations(@RequestParam(required=false) String status,@RequestParam(required=false) String sourceType,@RequestParam(required=false) String sourceId){return ResponseEntity.ok(service.reservations(status,sourceType,sourceId));}

    @PostMapping("/reservations")
    public ResponseEntity<Map<String,Object>> createReservation(@RequestBody InventoryDtos.ReservationRequest request){return ResponseEntity.ok(service.createReservation(request));}

    @PostMapping("/reservations/{id}/release")
    public ResponseEntity<Map<String,Object>> releaseReservation(@PathVariable String id){return ResponseEntity.ok(service.releaseReservation(id));}

    @GetMapping("/movements/reversible")
    public ResponseEntity<List<Map<String,Object>>> reversibleMovements(@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.reversibleMovements(warehouseId));}

    @PostMapping("/movements/{id}/reverse")
    public ResponseEntity<Map<String,Object>> reverseMovement(@PathVariable String id,@RequestBody(required=false) Map<String,Object> body){return ResponseEntity.ok(service.reverseMovement(id,body==null?null:String.valueOf(body.getOrDefault("notes",""))));}

    @GetMapping("/setup/policies")
    public ResponseEntity<List<Map<String,Object>>> productWarehousePolicies(@RequestParam(required=false) String productId,@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.productWarehousePolicies(productId,warehouseId));}

    @PutMapping("/setup/policies")
    public ResponseEntity<Map<String,Object>> saveProductWarehousePolicy(@RequestBody InventoryDtos.ProductWarehousePolicyRequest request){return ResponseEntity.ok(service.saveProductWarehousePolicy(request));}

    @GetMapping("/setup/products/{productId}/uom-conversions")
    public ResponseEntity<List<Map<String,Object>>> uomConversions(@PathVariable String productId){return ResponseEntity.ok(service.uomConversions(productId));}

    @PutMapping("/setup/uom-conversions")
    public ResponseEntity<Map<String,Object>> saveUomConversion(@RequestBody InventoryDtos.UomConversionRequest request){return ResponseEntity.ok(service.saveUomConversion(request));}

    @GetMapping("/setup/warehouses")
    public ResponseEntity<List<Map<String,Object>>> warehouseScopeOptions(){return ResponseEntity.ok(service.warehouseScopeOptions());}

    @GetMapping("/setup/roles/{roleId}/warehouses")
    public ResponseEntity<List<Map<String,Object>>> roleWarehouseScope(@PathVariable String roleId){return ResponseEntity.ok(service.roleWarehouseScope(roleId));}

    @PutMapping("/setup/roles/{roleId}/warehouses")
    public ResponseEntity<List<Map<String,Object>>> saveRoleWarehouseScope(@PathVariable String roleId,@RequestBody List<String> warehouseIds){return ResponseEntity.ok(service.saveRoleWarehouseScope(roleId,warehouseIds));}

    @GetMapping("/setup/users/{userId}/warehouses")
    public ResponseEntity<List<Map<String,Object>>> userWarehouseScope(@PathVariable String userId){return ResponseEntity.ok(service.userWarehouseScope(userId));}

    @PutMapping("/setup/users/{userId}/warehouses")
    public ResponseEntity<List<Map<String,Object>>> saveUserWarehouseScope(@PathVariable String userId,@RequestBody List<String> warehouseIds){return ResponseEntity.ok(service.saveUserWarehouseScope(userId,warehouseIds));}

    @GetMapping("/reason-codes")
    public ResponseEntity<List<Map<String,Object>>> reasonCodes(){return ResponseEntity.ok(service.reasonCodes());}

    @PutMapping("/reason-codes")
    public ResponseEntity<Map<String,Object>> saveReasonCode(@RequestBody InventoryDtos.ReasonCodeRequest request){return ResponseEntity.ok(service.saveReasonCode(request));}

    @GetMapping("/replenishment/recommendations")
    public ResponseEntity<List<Map<String,Object>>> replenishmentRecommendations(@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.replenishmentRecommendations(warehouseId));}

    @GetMapping("/purchase-recommendations")
    public ResponseEntity<List<Map<String,Object>>> purchaseRecommendations(@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.purchaseRecommendations(warehouseId));}

    @GetMapping("/expiry-alerts")
    public ResponseEntity<List<Map<String,Object>>> expiryAlerts(@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.expiryAlerts(warehouseId));}

    @GetMapping("/ageing")
    public ResponseEntity<List<Map<String,Object>>> stockAgeing(@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.stockAgeing(warehouseId));}

    @GetMapping("/recall")
    public ResponseEntity<Map<String,Object>> batchRecall(@RequestParam String batchNo){return ResponseEntity.ok(service.batchRecall(batchNo));}

    @GetMapping("/health")
    public ResponseEntity<Map<String,Object>> inventoryHealth(@RequestParam(required=false) String warehouseId){return ResponseEntity.ok(service.inventoryHealth(warehouseId));}

    @GetMapping("/reference/warehouses")
    public ResponseEntity<List<Map<String,Object>>> referenceWarehouses(@RequestParam String workcentre){return ResponseEntity.ok(service.referenceWarehouses(workcentre));}

    @GetMapping("/reference/locations")
    public ResponseEntity<List<Map<String,Object>>> referenceLocations(@RequestParam String workcentre,@RequestParam String warehouseId){return ResponseEntity.ok(service.referenceLocations(workcentre,warehouseId));}

    @GetMapping("/reference/products")
    public ResponseEntity<List<Map<String,Object>>> referenceProducts(@RequestParam String workcentre,@RequestParam(required=false) String query){return ResponseEntity.ok(service.referenceProducts(workcentre,query));}
}
