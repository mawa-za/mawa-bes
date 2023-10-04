package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.SupplierDto;

import java.util.List;

public interface SupplierDao {

    String assignSupplier(SupplierDto supplierDto) throws Exception;

    SupplierDto getSupplier(String id) throws Exception;

    List<SupplierDto> getAll();


}
