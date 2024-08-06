package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.ServiceRequestDao;
import za.co.mawa.bes.dao.TransactionDao;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.partner.PartnerEditDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.service.request.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;

import java.lang.reflect.Method;
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
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    @Qualifier("transactionService")
    TransactionService service;

    @Override
    public ServiceRequestDto create(ServiceRequestCreateDto serviceRequestCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.SERVICE_REQUEST);
            transactionCreateDto.setDescription(serviceRequestCreateDto.getDescription());
            transactionCreateDto.setCategory(serviceRequestCreateDto.getCategory());
            transactionCreateDto.setPriority(serviceRequestCreateDto.getPriority());
            transactionCreateDto.setCustomerId(serviceRequestCreateDto.getCustomer());
            transactionCreateDto.setStatus(Status.NOT_YET_STARTED);
            transactionCreateDto.setStatusReason(Status.SERVICE_REQUEST_STATUS_REASON);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            try {
                for (String assignee : serviceRequestCreateDto.getAssignees()) {
                    TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                    transactionPartnerDto.setTransaction(transactionDto.getId());
                    transactionPartnerDto.setFunction(PartnerFunction.ASSIGNEE);
                    transactionPartnerDto.setPartner(assignee);
                    transactionService.addPartner(transactionPartnerDto);
                }
            }
            catch (Exception e){

            }
            return get(transactionDto.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServiceRequestDto edit(ServiceRequestEditDto serviceRequestEditDto) {
        return  null;
    }

    public ServiceRequestDto edit(String id, ServiceRequestEditDto serviceRequestEditDto) {
        try {
            TransactionEntity entity = transactionRepository.getById(id);

            if (serviceRequestEditDto.getDescription() != null) {
                entity.setDescription(serviceRequestEditDto.getDescription());
            }
            if(serviceRequestEditDto.getCategory() != null){
                entity.setCategory(serviceRequestEditDto.getCategory());
            }
            if(serviceRequestEditDto.getPriority() != null){
                entity.setPriority(serviceRequestEditDto.getPriority());
            }
            if(serviceRequestEditDto.getStatus() != null){
                entity.setStatus(serviceRequestEditDto.getStatus());
            }
            Class<?> tservice = service.getClass();
            Method privateMethod = tservice.getDeclaredMethod("getUser");
            privateMethod.setAccessible(true);
            entity.setChangedBy((String) privateMethod.invoke(service));
            transactionRepository.save(entity);
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
        serviceRequestDto.setDescription(transactionDto.getDescription());

        if (transactionDto.getChangedBy() != null) {
            serviceRequestDto.setChangedBy(userService.getUserByName(transactionDto.getChangedBy()).getPartner());
        }
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
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.ASSIGNEE)) {
                try {
                    List<PartnerDto> partnerAssignee = new ArrayList<>();
                    partnerAssignee.add(partnerService.get(transactionPartner.getPartner()));
                    serviceRequestDto.setAssignee(partnerAssignee);
                } catch (Exception e) {
                }
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

    public ServiceRequestDto assign(String id, String assignee) throws Exception {
        try{
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.ASSIGNEE);
            transactionPartnerDto.setPartner(assignee);
            transactionService.addPartner(transactionPartnerDto);
        }
        catch(Exception e){
        }
        return get(id);
    }

    public ServiceRequestDto unassign(String id, String assignee) throws Exception {
        try{
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.ASSIGNEE);
            transactionPartnerDto.setPartner(assignee);

            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setChangedBy(UserContext.getCurrentUserPartner());

            transactionService.edit(transactionEditDto);
            transactionService.removePartner(transactionPartnerDto);
        }
        catch (Exception e){
        }
        return get(id);
    }

    public ServiceRequestDto reject(String id, String statusReason, String description) throws Exception {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.REJECTED);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason.toUpperCase());
            }
            if (description != null && description != "") {
                transactionEditDto.setDescription(description);
            }
            transactionEditDto.setChangedBy(UserContext.getCurrentUserPartner());
            transactionService.edit(transactionEditDto);
        } catch (Exception exception) {
        }
        return get(id);
    }

    public ServiceRequestDto cancel(String id, String statusReason, String description ) throws Exception {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.CANCELLED);

            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason.toUpperCase());
            }
            if (description != null && description != "") {
                transactionEditDto.setDescription(description);
            }
            transactionEditDto.setChangedBy(UserContext.getCurrentUserPartner());
            transactionService.edit(transactionEditDto);
        } catch (Exception exception) {
        }
        return get(id);
    }

    public ServiceRequestDto close(String id, String statusReason, String description ) throws Exception {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.CLOSED);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason.toUpperCase());
            }
            if (description != null && description != "") {
                transactionEditDto.setDescription(description);
            }
            transactionEditDto.setChangedBy(UserContext.getCurrentUserPartner());
            transactionService.edit(transactionEditDto);
        } catch (Exception exception) {
        }
        return get(id);
    }
}
