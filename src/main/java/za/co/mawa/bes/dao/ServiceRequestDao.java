package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.service.request.ServiceRequestCreateDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestQueryDto;

import java.util.List;

public interface ServiceRequestDao {
    ServiceRequestDao create(ServiceRequestCreateDto serviceRequestCreateDto);
    List<ServiceRequestDao> search(ServiceRequestQueryDto serviceRequestQueryDto);
}
