package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ServiceRequestDao;
import za.co.mawa.bes.dao.TransactionDao;
import za.co.mawa.bes.dto.service.request.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceRequestService implements ServiceRequestDao {
    @Autowired
    TransactionService transactionService;
    @Override
    public ServiceRequestDto create(ServiceRequestCreateDto serviceRequestCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.SERVICE_REQUEST);
            transactionCreateDto.setDescription(serviceRequestCreateDto.getDescription());
            transactionCreateDto.setSubType(serviceRequestCreateDto.getCategory());
            transactionCreateDto.setCategory(serviceRequestCreateDto.getCategory());
            transactionCreateDto.setPriority(serviceRequestCreateDto.getPriority());
            transactionCreateDto.setCustomerId(serviceRequestCreateDto.getCustomerId());
            transactionCreateDto.setStatus(Status.NEW);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            return get(transactionDto.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServiceRequestDto edit(ServiceRequestEditDto serviceRequestEditDto) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setDescription(serviceRequestEditDto.getDescription());
            transactionEditDto.setSubType(serviceRequestEditDto.getCategory());
            transactionEditDto.setCategory(serviceRequestEditDto.getCategory());
            transactionEditDto.setPriority(serviceRequestEditDto.getPriority());
            transactionEditDto.setCustomerId(serviceRequestEditDto.getCustomerId());
            transactionEditDto.setStatus(Status.NEW);
            transactionService.edit(transactionEditDto);
            return get(serviceRequestEditDto.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ServiceRequestQueryResultDto> search(ServiceRequestQueryDto serviceRequestQueryDto) {
        List<ServiceRequestQueryResultDto> serviceRequestQueryResultDtos = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.SERVICE_REQUEST);
        transactionQueryDto.setStatus(serviceRequestQueryDto.getStatus());
        for(String id: transactionService.search(transactionQueryDto)){
            ServiceRequestQueryResultDto serviceRequestQueryResultDto = new ServiceRequestQueryResultDto();
//            serviceRequestQueryResultDto.setId(transactionQueryResultDto.getId());
//            serviceRequestQueryResultDto.setNo(transactionQueryResultDto.getNumber());
//            serviceRequestQueryResultDto.setCustomerId(transactionQueryResultDto.getCustomerId());
//            serviceRequestQueryResultDto.setCustomerName(transactionQueryResultDto.getCustomerName());
//            serviceRequestQueryResultDto.setEmployeeResponsibleId(transactionQueryResultDto.getEmployeeResponsibleId());
//            serviceRequestQueryResultDto.setDescription(transactionQueryResultDto.getDescription());
//            serviceRequestQueryResultDto.setStatus(transactionQueryResultDto.getStatus());
            serviceRequestQueryResultDtos.add(serviceRequestQueryResultDto);
        }
        return serviceRequestQueryResultDtos;
    }

    @Override
    public ServiceRequestDto get(String id) throws TransactionNotFound {
        return new ServiceRequestDto(transactionService.get(id));
    }

    @Override
    public void delete(String id) {

    }

}
