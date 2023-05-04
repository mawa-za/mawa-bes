package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.SupplierDto;
import za.co.mawa.bes.exception.PartnerNotFound;

public interface SupplierDao {

    boolean assignSupplier(SupplierDto supplierDto) throws Exception;

    SupplierDto getSupplier(String id) throws PartnerNotFound;


}
