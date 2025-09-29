package za.co.mawa.bes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.StorageBinEntity;
import za.co.mawa.bes.repository.StorageBinRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StorageBinService {

    private final StorageBinRepository storageBinRepository;

    public StorageBinEntity createStorageBin(StorageBinEntity storageBin) {
        return storageBinRepository.save(storageBin);
    }

    public Optional<StorageBinEntity> getStorageBinById(String binId) {
        return storageBinRepository.findById(binId);
    }

    public Optional<StorageBinEntity> getStorageBinByCode(String binCode) {
        return storageBinRepository.findByBinCode(binCode);
    }

    public List<StorageBinEntity> getAllBins() {
        return storageBinRepository.findAll();
    }

    public List<StorageBinEntity> getBinsByWarehouse(String warehouseId) {
        return storageBinRepository.findByWarehouseId(warehouseId);
    }

    public void deleteStorageBin(String binId) {
        storageBinRepository.deleteById(binId);
    }
}

