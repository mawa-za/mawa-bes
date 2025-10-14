package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.StorageBinInboundDto;
import za.co.mawa.bes.dto.StorageBinOutboundDto;
import za.co.mawa.bes.entity.StorageBinEntity;
import za.co.mawa.bes.service.StorageBinService;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "storage-bin")
public class StorageBinController {
    @Autowired
    StorageBinService storageBinService;

    Gson gson = new Gson();

    @PostMapping
    public ResponseEntity<StorageBinEntity> createBin(@RequestBody StorageBinInboundDto storageBinInboundDto) {
        StorageBinEntity storageBinEntity = new StorageBinEntity();
        storageBinEntity.setWarehouseId(TenantContext.getCurrentTenant());
        storageBinEntity.setBinId(storageBinInboundDto.getBinId());
        storageBinEntity.setAisle(storageBinInboundDto.getAisle());
        storageBinEntity.setStack(storageBinInboundDto.getStack());
        storageBinEntity.setShelf(storageBinInboundDto.getShelf());
        storageBinEntity.setDescription(storageBinInboundDto.getDescription());
        storageBinEntity.setProductId(storageBinInboundDto.getProductId());
        storageBinEntity.setBinCode(storageBinInboundDto.getBinCode());
        storageBinEntity.setPublished(Boolean.valueOf(storageBinInboundDto.getPublished()));
//        storageBinService.createStorageBin(storageBinEntity);
        StorageBinOutboundDto storageBinOutboundDto = new StorageBinOutboundDto();
        return ResponseEntity.ok(storageBinService.createStorageBin(storageBinEntity));
    }

    @PutMapping
    public ResponseEntity<StorageBinEntity> editBin(@RequestBody StorageBinInboundDto storageBinInboundDto) {
        StorageBinEntity storageBinEntity = new StorageBinEntity();
        storageBinEntity.setWarehouseId(TenantContext.getCurrentTenant());
        storageBinEntity.setAisle(storageBinInboundDto.getAisle());
        storageBinEntity.setShelf(storageBinInboundDto.getShelf());
        storageBinEntity.setDescription(storageBinInboundDto.getDescription());
        storageBinEntity.setProductId(storageBinInboundDto.getProductId());
        storageBinEntity.setBinCode(storageBinInboundDto.getBinCode());
        storageBinEntity.setPublished(Boolean.valueOf(storageBinInboundDto.getPublished()));
//        storageBinService.createStorageBin(storageBinEntity);
        StorageBinOutboundDto storageBinOutboundDto = new StorageBinOutboundDto();
        return ResponseEntity.ok(storageBinService.createStorageBin(storageBinEntity));
    }

    @GetMapping("/{binId}")
    public ResponseEntity<StorageBinEntity> getBinById(@PathVariable String binId) {
        return storageBinService.getStorageBinById(binId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{binCode}")
    public ResponseEntity<StorageBinEntity> getBinByCode(@PathVariable String binCode) {
        return storageBinService.getStorageBinByCode(binCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllBins() {
        List<StorageBinOutboundDto> storageBinOutboundDtoList = new ArrayList<>();
        List<StorageBinEntity> storageBinEntities = storageBinService.getAllBins();
        for(StorageBinEntity storageBinEntity: storageBinEntities){
            StorageBinOutboundDto storageBinOutboundDto = new StorageBinOutboundDto();
            storageBinOutboundDto.setBinId(storageBinEntity.getBinId());
            storageBinOutboundDto.setBinCode(storageBinEntity.getBinCode());
            storageBinOutboundDto.setAisle(storageBinEntity.getAisle());
            storageBinOutboundDto.setProduct(storageBinEntity.getProductId());
            storageBinOutboundDto.setPublished(storageBinEntity.getPublished());
            storageBinOutboundDto.setDescription(storageBinEntity.getDescription());
            storageBinOutboundDto.setStack(storageBinEntity.getStack());
            storageBinOutboundDto.setRack(storageBinEntity.getStack());
            storageBinOutboundDto.setShelf(storageBinEntity.getShelf());
            storageBinOutboundDtoList.add(storageBinOutboundDto);
        }
        return ResponseEntity.ok(gson.toJson(storageBinOutboundDtoList));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<StorageBinEntity>> getBinsByWarehouse(@PathVariable String warehouseId) {
        return ResponseEntity.ok(storageBinService.getBinsByWarehouse(warehouseId));
    }

    @DeleteMapping("/{binId}")
    public ResponseEntity<Void> deleteBin(@PathVariable String binId) {
        storageBinService.deleteStorageBin(binId);
        return ResponseEntity.noContent().build();
    }
}

