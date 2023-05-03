package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.SupplierDto;
import za.co.mawa.bes.exception.PartnerNotFound;

public interface SupplierDao {

    String createSupplier(SupplierDto supplierDto) throws Exception;

    SupplierDto getSupplier(String id) throws PartnerNotFound;


}
