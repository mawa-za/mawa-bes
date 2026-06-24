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
import za.co.mawa.bes.dto.StorageBinCreateRequestDto;
import za.co.mawa.bes.dto.StorageBinResponseDto;
import za.co.mawa.bes.dto.StorageBinUpdateRequestDto;
import za.co.mawa.bes.mapper.StorageBinMapper;


@RestController
@CrossOrigin
@RequestMapping(value = "storage-bin")
@RequiredArgsConstructor
public class StorageBinController {
    private final StorageBinMapper storageBinMapper;
    private final StorageBinService storageBinService;

    Gson gson = new Gson();

    @PostMapping
    public ResponseEntity<StorageBinResponseDto> createBin(@RequestBody StorageBinInboundDto storageBinInboundDto) {
        StorageBinEntity storageBinEntity = StorageBinEntity.builder()
                .warehouseId(TenantContext.getCurrentTenant())
                .binId(storageBinInboundDto.getBinId())
                .aisle(storageBinInboundDto.getAisle())
                .stack(storageBinInboundDto.getStack())
                .shelf(storageBinInboundDto.getShelf())
                .description(storageBinInboundDto.getDescription())
                .productId(storageBinInboundDto.getProductId())
                .binCode(storageBinInboundDto.getBinCode())
                .batchNumber(storageBinInboundDto.getBatchNumber())
                .expiryDate(storageBinInboundDto.getExpiryDate())
                .published(Boolean.valueOf(storageBinInboundDto.getPublished()))
                .build();

        StorageBinEntity savedEntity = storageBinService.createStorageBin(storageBinEntity);
        return ResponseEntity.ok(storageBinMapper.toResponse(savedEntity));
    }

    @PutMapping
    public ResponseEntity<StorageBinResponseDto> editBin(@RequestBody StorageBinInboundDto storageBinInboundDto) {
        StorageBinEntity storageBinEntity = StorageBinEntity.builder()
                .warehouseId(TenantContext.getCurrentTenant())
                .binId(storageBinInboundDto.getBinId())
                .aisle(storageBinInboundDto.getAisle())
                .stack(storageBinInboundDto.getStack())
                .shelf(storageBinInboundDto.getShelf())
                .description(storageBinInboundDto.getDescription())
                .productId(storageBinInboundDto.getProductId())
                .binCode(storageBinInboundDto.getBinCode())
                .batchNumber(storageBinInboundDto.getBatchNumber())
                .expiryDate(storageBinInboundDto.getExpiryDate())
                .published(Boolean.valueOf(storageBinInboundDto.getPublished()))
                .build();

        StorageBinEntity updatedEntity = storageBinService.createStorageBin(storageBinEntity);
        return ResponseEntity.ok(storageBinMapper.toResponse(updatedEntity));
    }

    @GetMapping("/{binId}")
    public ResponseEntity<StorageBinResponseDto> getBinById(@PathVariable String binId) {
        return storageBinService.getStorageBinById(binId)
                .map(storageBinMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{binCode}")
    public ResponseEntity<StorageBinResponseDto> getBinByCode(@PathVariable String binCode) {
        return storageBinService.getStorageBinByCode(binCode)
                .map(storageBinMapper::toResponse)
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
            storageBinOutboundDto.setBatchNumber(storageBinEntity.getBatchNumber());
            storageBinOutboundDto.setExpiryDate(storageBinEntity.getExpiryDate());
            storageBinOutboundDtoList.add(storageBinOutboundDto);
        }
        return ResponseEntity.ok(gson.toJson(storageBinOutboundDtoList));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<StorageBinResponseDto>> getBinsByWarehouse(@PathVariable String warehouseId) {
        List<StorageBinResponseDto> responses = storageBinService.getBinsByWarehouse(warehouseId).stream()
                .map(storageBinMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{binId}")
    public ResponseEntity<Void> deleteBin(@PathVariable String binId) {
        storageBinService.deleteStorageBin(binId);
        return ResponseEntity.noContent().build();
    }
}

