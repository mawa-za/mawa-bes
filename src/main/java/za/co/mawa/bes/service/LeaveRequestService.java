package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.leave.request.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.transaction.text.TransactionTextDto;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeaveRequestService {
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionService transactionService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    PartnerService partnerService;
    List<String> autoApprovalTypeList = new ArrayList<>();
    @Autowired
    TransactionTextService transactionTextService;

    public LeaveRequestOutboundDto create(LeaveRequestInboundDto leaveRequestInboundDto) {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        try{
            transactionCreateDto.setCreatedBy(UserContext.getCurrentUser());
            transactionCreateDto.setEmployeeResponsible(leaveRequestInboundDto.getEmployee());
            transactionCreateDto.setStatus(Status.PENDING);
            transactionCreateDto.setType(TransactionType.LEAVE_REQUEST);
            transactionCreateDto.setSubType(leaveRequestInboundDto.getType());
            System.out.println(transactionCreateDto.getType());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            transactionService.addDate(creationDate);

            if(leaveRequestInboundDto.getApprover() != null){
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.APPROVER);
                transactionPartnerDto.setPartner(leaveRequestInboundDto.getApprover());
                transactionService.addPartner(transactionPartnerDto);
            }
            if(leaveRequestInboundDto.getEmployee() != null){
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setFunction(PartnerFunction.EMPLOYEE);
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setPartner(leaveRequestInboundDto.getEmployee());
                transactionService.addPartner(transactionPartnerDto);
            }

            if(leaveRequestInboundDto.getStartDate() != null){
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.START_DATE);
                transactionDateDto.setValue(leaveRequestInboundDto.getStartDate());
                transactionService.addDate(transactionDateDto);
            }
            if(leaveRequestInboundDto.getEndDate() != null){
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.END_DATE);
                transactionDateDto.setValue(leaveRequestInboundDto.getEndDate());
                transactionService.addDate(transactionDateDto);
            }
            return get(transactionDto.getId());
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public LeaveRequestOutboundDto get(String id) throws DoesNotExist {
        LeaveRequestOutboundDto leaveRequestOutboundDto = new LeaveRequestOutboundDto();
        try{
            TransactionDto transactionDto = transactionService.get(id);
            String leaveType = transactionDto.getSubType();
            leaveRequestOutboundDto.setType(resolveFieldOption(
                    Field.LEAVE_TYPE, leaveType, leaveType == null || leaveType.isBlank() ? "LEAVE" : leaveType));
            leaveRequestOutboundDto.setStatus(resolveFieldOption(
                    Field.TRANSACTION_STATUS, transactionDto.getStatus(), transactionDto.getStatus()));
            leaveRequestOutboundDto.setId(transactionDto.getId());
            for(TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)){
                if (transactionPartnerDto.getFunction().equalsIgnoreCase(PartnerFunction.APPROVER)) {
                    try {
                        leaveRequestOutboundDto.setApprover(partnerService.get(transactionPartnerDto.getPartner()));
                    } catch (Exception ignored) {
                        // Keep legacy leave requests readable if the partner was removed.
                    }
                }
                if (transactionPartnerDto.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE)) {
                    try {
                        leaveRequestOutboundDto.setEmployee(partnerService.get(transactionPartnerDto.getPartner()));
                    } catch (Exception ignored) {
                        // Keep legacy leave requests readable if the partner was removed.
                    }
                }
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (transactionDateDto.getType().equalsIgnoreCase(DateType.START_DATE)) {
                    leaveRequestOutboundDto.setStartDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equalsIgnoreCase(DateType.END_DATE)) {
                    leaveRequestOutboundDto.setEndDate(transactionDateDto.getValue());
                }
            }
            if (leaveRequestOutboundDto.getStartDate() != null && leaveRequestOutboundDto.getEndDate() != null) {
                long inclusiveDays = ChronoUnit.DAYS.between(
                        leaveRequestOutboundDto.getStartDate().toInstant(),
                        leaveRequestOutboundDto.getEndDate().toInstant()) + 1;
                leaveRequestOutboundDto.setDays((int) Math.max(inclusiveDays, 0));
            }
        }
        catch(Exception e){
            throw new DoesNotExist(e.getMessage());
        }
        return leaveRequestOutboundDto;
    }

    public List<LeaveRequestOutboundDto> search() throws DoesNotExist {
        List<LeaveRequestOutboundDto> leaveRequestOutboundDtoList = new ArrayList<>();
        try{
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();

            transactionQueryDto.setType(TransactionType.LEAVE_REQUEST);
            for(String id: transactionService.search(transactionQueryDto)){
                try {
                    leaveRequestOutboundDtoList.add(get(id));
                } catch (Exception ignored) {
                    // Keep the leave request visible even if an old partner reference is no longer available.
                }
            }
        }
        catch(Exception e){
            throw new DoesNotExist(e.getMessage());
        }
        return leaveRequestOutboundDtoList;
    }

    public LeaveRequestOutboundDto edit(LeaveRequestEditDto leaveRequestEditDto, String id){
        try{
            TransactionEntity entity = transactionRepository.getById(id);
            if(!entity.equals(null)){
                if(leaveRequestEditDto.getEndDate() != null){
                    TransactionDateEdit dateEdit = new TransactionDateEdit();
                    dateEdit.setType(DateType.END_DATE);
                    dateEdit.setValue(Conversion.stringToDate(String.valueOf(leaveRequestEditDto.getEndDate())));
                    dateEdit.setTransaction(id);
                    transactionService.dateEdit(dateEdit);
                    entity.setValidTo(leaveRequestEditDto.getEndDate());
                }
                if(leaveRequestEditDto.getStartDate() != null){
                    entity.setValidFrom(leaveRequestEditDto.getStartDate());
                }
                if(leaveRequestEditDto.getEndDate() != null){
                    entity.setValidTo(leaveRequestEditDto.getEndDate());
                }
                if(leaveRequestEditDto.getStatus() != null){
                    entity.setStatus(leaveRequestEditDto.getStatus());
                }
                transactionRepository.save(entity);
            }
            return get(id);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public LeaveRequestOutboundDto submit(String id) throws DoesNotExist {
        try{
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.AWAITING_APPROVAL);
            transactionService.edit(transactionEditDto);

        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
        return get(id);
    }

    public LeaveRequestOutboundDto approve(String id) throws DoesNotExist {
        try{
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.APPROVED);
            transactionService.edit(transactionEditDto);
        }
        catch(Exception e){
        }
        return get(id);
    }

    public LeaveRequestOutboundDto reject(String id) throws  DoesNotExist{
        try{
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.REJECTED);
            transactionService.edit(transactionEditDto);
        }
        catch(Exception e){

        }
        return get(id);
    }

    public LeaveRequestOutboundDto cancel(LeaveRequestCancelDto leaveRequestCancelDto, String id) throws DoesNotExist {
        try{
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(leaveRequestCancelDto.getLeaveRequestId());
            transactionEditDto.setStatus(Status.CANCELLED);
            transactionEditDto.setStatusReason(leaveRequestCancelDto.getReason());
            transactionService.edit(transactionEditDto);
            TransactionTextDto transactionTextDto = new TransactionTextDto();
            transactionTextDto.setTransaction(leaveRequestCancelDto.getLeaveRequestId());
            transactionTextDto.setType(TextType.LEAVE_REQUEST_CANCEL);
            transactionTextService.add(transactionTextDto);
//            delete(id);
        }
        catch(Exception e){
        }
        return get(leaveRequestCancelDto.getLeaveRequestId());
    }

    public List<LeaveRequestOutboundDto> delete(String id) throws DoesNotExist {
        try {
            transactionService.delete(id);
        } catch (Exception e) {

        }
        return search();
    }
    private FieldOptionDto resolveFieldOption(String field, String code, String fallbackDescription) {
        if (code != null && !code.isBlank()) {
            try {
                FieldOptionDto option = fieldOptionService.getFieldOption(field, code);
                if (option != null) return option;
            } catch (Exception ignored) {
                // Fall through to a lightweight option so API serialization remains stable.
            }
        }
        FieldOptionDto fallback = new FieldOptionDto();
        fallback.setField(field);
        fallback.setCode(code == null || code.isBlank() ? fallbackDescription : code);
        fallback.setDescription(fallbackDescription == null || fallbackDescription.isBlank()
                ? fallback.getCode()
                : fallbackDescription);
        return fallback;
    }

}