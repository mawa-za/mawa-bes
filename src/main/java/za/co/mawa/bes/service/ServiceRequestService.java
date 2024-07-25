package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ServiceRequestDao;
import za.co.mawa.bes.dao.TransactionDao;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.service.request.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceRequestService implements ServiceRequestDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    UserService userService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;
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
            transactionEditDto.setCustomerId(serviceRequestEditDto.getCustomer());
            transactionService.edit(transactionEditDto);
            return get(serviceRequestEditDto.getId());
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
        transactionQueryDto.setCategory(serviceRequestQueryDto.getCategory());
        for(String id: transactionService.search(transactionQueryDto)){
            try {
                serviceRequestDtoList.add(get(id));
            } catch (Exception e) {
            }

        }
        return serviceRequestDtoList;
    }

    @Override
    public ServiceRequestDto get(String id) throws Exception{
        ServiceRequestDto serviceRequestDto = new ServiceRequestDto();
        TransactionDto transactionDto = transactionService.get(id);
        serviceRequestDto.setId(transactionDto.getId());
        serviceRequestDto.setNumber(transactionDto.getNumber());
        try {
            serviceRequestDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
        } catch (Exception e) {
        }
        serviceRequestDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
        try {
            serviceRequestDto.setStatusReason(fieldOptionService.getFieldOption(Field.SERVICE_REQUEST_STATUS_REASON, transactionDto.getStatusReason()));
        } catch (Exception e) {

        }
        serviceRequestDto.setCategory(fieldOptionService.getFieldOption(Field.SERVICE_REQUEST_CATEGORY, transactionDto.getCategory()));
        serviceRequestDto.setPriority(fieldOptionService.getFieldOption(Field.SERVICE_REQUEST_PRIORITY, transactionDto.getPriority()));
        for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
            if (transactionDateDto.getType().equalsIgnoreCase(DateType.DUE_DATE)) {
                serviceRequestDto.setDueDate(transactionDateDto.getValue());
            }
            if (transactionDateDto.getType().equalsIgnoreCase(DateType.CREATED)) {
                serviceRequestDto.setCreationDate(transactionDateDto.getValue());
            }
        }
        for (TransactionPartnerDto transactionPartner : transactionService.getPartners(id)) {
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.CUSTOMER)) {
                try {
                    serviceRequestDto.setCustomer(partnerService.get(transactionPartner.getPartner()));
                } catch (Exception e) {
                }
            }
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                try {
                    serviceRequestDto.setEmployeeResponsible(partnerService.get(transactionPartner.getPartner()));
                } catch (Exception e) {
                }
            }
        }
        return serviceRequestDto;
    }

    @Override
    public void delete(String id) {

    }

}
