package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.SupplierDto;
import za.co.mawa.bes.exception.PartnerNotFound;

import java.util.List;

public interface SupplierDao {

    boolean assignSupplier(SupplierDto supplierDto) throws Exception;

    SupplierDto getSupplier(String id) throws Exception;

    List<SupplierDto> getAll();


}
