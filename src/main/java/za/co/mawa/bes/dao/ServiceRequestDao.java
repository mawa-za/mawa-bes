package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.service.request.*;

import java.util.List;

public interface ServiceRequestDao {
    ServiceRequestDto create(ServiceRequestCreateDto serviceRequestCreateDto);
    ServiceRequestDto edit(ServiceRequestEditDto serviceRequestEditDto);
    List<ServiceRequestQueryResultDto> search(ServiceRequestQueryDto serviceRequestQueryDto);
    ServiceRequestDto get(String id);
    void delete(String id);
}
