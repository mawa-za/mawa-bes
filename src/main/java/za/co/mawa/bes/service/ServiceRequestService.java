package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ServiceRequestDao;
import za.co.mawa.bes.dao.TransactionDao;
import za.co.mawa.bes.dto.service.request.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceRequestService implements ServiceRequestDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    UserService userService;
    @Autowired
    TransactionRepository transactionRepository;

    @Override
    public ServiceRequestDto create(ServiceRequestCreateDto serviceRequestCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.SERVICE_REQUEST);
            transactionCreateDto.setDescription(serviceRequestCreateDto.getDescription());
            transactionCreateDto.setSubType(serviceRequestCreateDto.getCategory());
            transactionCreateDto.setCategory(serviceRequestCreateDto.getCategory());
            transactionCreateDto.setPriority(serviceRequestCreateDto.getPriority());
            transactionCreateDto.setCustomerId(serviceRequestCreateDto.getCustomer());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            return get(transactionDto.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServiceRequestDto edit(ServiceRequestEditDto serviceRequestEditDto) {
        return null;
    }

    public ServiceRequestDto edit(String id, ServiceRequestEditDto serviceRequestEditDto) {
        try {
            TransactionEntity entity = transactionRepository.getById(id);
            TransactionEditDto transactionEditDto = new TransactionEditDto();

            if(entity != null){

                if(serviceRequestEditDto.getDescription() != null){
                    entity.setDescription(serviceRequestEditDto.getDescription());
                }
                if(serviceRequestEditDto.getCategory() != null){
                    entity.setCategory(serviceRequestEditDto.getCategory());
                }
                if(serviceRequestEditDto.getPriority() != null){
                    entity.setPriority(serviceRequestEditDto.getPriority());
                }
                entity.setChangedBy(transactionService.getUser());
                transactionRepository.save(entity);
            }
            return get(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ServiceRequestDto> search(ServiceRequestQueryDto serviceRequestQueryDto) {
        List<ServiceRequestDto> serviceRequestDtoList = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.SERVICE_REQUEST);
        transactionQueryDto.setStatus(serviceRequestQueryDto.getStatus());
        for (String id : transactionService.search(transactionQueryDto)) {
            try {
                serviceRequestDtoList.add(get(id));
            } catch (Exception ex) {
            }
        }
        return serviceRequestDtoList;
    }

    @Override
    public ServiceRequestDto get(String id) throws TransactionNotFound {
        ServiceRequestDto serviceRequestDto = new ServiceRequestDto();
        TransactionDto transactionDto = transactionService.get(id);
        serviceRequestDto.setId(transactionDto.getId());
        serviceRequestDto.setNumber(transactionDto.getNo());
        serviceRequestDto.setDescription(transactionDto.getDescription());
//        serviceRequestDto.setCategory(fieldOptionService.getFieldOption(Field.TRANSACTION_SUBTYPE, transactionDto.getSubType()));
        serviceRequestDto.setCategory(fieldOptionService.getFieldOption(Field.SERVICE_REQUEST_CATEGORY, transactionDto.getCategory()));
        serviceRequestDto.setPriority(fieldOptionService.getFieldOption(Field.SERVICE_REQUEST_PRIORITY, transactionDto.getPriority()));
        serviceRequestDto.setStatus(fieldOptionService.getFieldOption(Field.STATUS, transactionDto.getStatus()));
        serviceRequestDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
        try {
            serviceRequestDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
        } catch (Exception e) {

        }
        for (TransactionPartnerDto transactionPartner : transactionService.getPartners(id)) {
            try {
                if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.CUSTOMER)) {
                    serviceRequestDto.setCustomer(partnerService.get(transactionPartner.getPartner()));
                }
                if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                    serviceRequestDto.setEmployeeResponsible(partnerService.get(transactionPartner.getPartner()));
                }

            } catch (Exception e) {

            }

        }
        return serviceRequestDto;
    }

    @Override
    public void delete(String id) {
        try {
            transactionService.delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
