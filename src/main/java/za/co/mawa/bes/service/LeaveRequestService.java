package za.co.mawa.bes.service;


import org.hibernate.grammars.hql.HqlParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.claim.ClaimOutboundDto;
import za.co.mawa.bes.dto.leave.request.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.date.TransactionDateEditDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.transaction.text.TransactionTextDto;
import za.co.mawa.bes.dto.voucher.VoucherOutboundDto;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
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
            transactionCreateDto.setStatus(Status.AWAITING_APPROVAL);
            transactionCreateDto.setType(TransactionType.LEAVE_REQUEST);
            transactionCreateDto.setCreatedBy(UserContext.getCurrentUser());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
//            LeaveRequestOutboundDto leaveRequestOutboundDto = new LeaveRequestOutboundDto();
//            leaveRequestOutboundDto.setId(transactionDto.getId());
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
            leaveRequestOutboundDto.setType(fieldOptionService.getFieldOption(Field.LEAVE_TYPE, transactionDto.getType()));
            leaveRequestOutboundDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            leaveRequestOutboundDto.setId(transactionDto.getId());
            leaveRequestOutboundDto.setEmployee(transactionDto.getCreatedBy());
            for(TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)){
                if (transactionPartnerDto.getFunction().equalsIgnoreCase(PartnerFunction.APPROVER)) {
                    try {
                        leaveRequestOutboundDto.setApprover(partnerService.get(transactionPartnerDto.getPartner()));

                    } catch (Exception e) {
                        throw new DoesNotExist(e.getMessage());
                    }
                }
//                if (transactionPartnerDto.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
//                    try {
//                        leaveRequestOutboundDto.setEmployee(partnerService.get(transactionPartnerDto.getPartner()));
//                    } catch (Exception e) {
//                        throw new DoesNotExist(e.getMessage());
//                    }
//                }
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (transactionDateDto.getType().equalsIgnoreCase(DateType.END_DATE)) {
                    leaveRequestOutboundDto.setStartDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equalsIgnoreCase(DateType.START_DATE)) {
                    leaveRequestOutboundDto.setEndDate(transactionDateDto.getValue());
                }
            }
        }
        catch(Exception e){
            throw new DoesNotExist(e.getMessage());
        }
        return leaveRequestOutboundDto;
    }

    public List<LeaveRequestOutboundDto> search(LeaveRequestQueryDto leaveRequestQueryDto) throws DoesNotExist {
        List<LeaveRequestOutboundDto> leaveRequestOutboundDtoList = new ArrayList<>();

        try{
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();

            transactionQueryDto.setType(TransactionType.LEAVE_REQUEST);
            for(String id: transactionService.search(transactionQueryDto)){
                try {
                    leaveRequestOutboundDtoList.add(get(id));
                } catch (Exception e) {
                    throw new DoesNotExist(e.getMessage());
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

    public void submit(String id) {
        try{
            TransactionDto transactionDto = transactionService.get(id);
            if(autoApprovalTypeList.contains(transactionDto.getSubType())){
                TransactionEditDto transactionEditDto = new TransactionEditDto();
                transactionEditDto.setId(id);
                transactionEditDto.setStatus(Status.APPROVED);
                transactionService.edit(transactionEditDto);
                approve(id);
            }
            else{
                TransactionEditDto transactionEditDto = new TransactionEditDto();
                transactionEditDto.setId(id);
                transactionEditDto.setStatus(Status.AWAITING_APPROVAL);
                transactionService.edit(transactionEditDto);
            }
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    public void approve(String id) {
        try{
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.APPROVED);
            transactionService.edit(transactionEditDto);
        }

        catch(Exception e){

        }
    }

    public void reject(String id) {
        try{
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.REJECTED);
            transactionService.edit(transactionEditDto);
        }
        catch(Exception e){

        }
    }

    public void cancel(LeaveCancelDto leaveCancelDto) {
        try{
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(leaveCancelDto.getLeaveRequestId());
            transactionEditDto.setStatus(Status.CANCELLED);
            transactionEditDto.setStatusReason(leaveCancelDto.getReason());
            transactionService.edit(transactionEditDto);
            TransactionTextDto transactionTextDto = new TransactionTextDto();
            transactionTextDto.setTransaction(leaveCancelDto.getLeaveRequestId());
            transactionTextDto.setType(TextType.LEAVE_REQUEST_CANCEL);
            transactionTextService.add(transactionTextDto);
        }
        catch(Exception e){

        }
    }

    public void delete(String id) {
        try {
            transactionService.delete(id);
        } catch (Exception e) {

        }
    }

    public LeaveRequestOutboundDto  edit(LeaveRequestInboundDto leaveRequestInboundDto, String id) {
        return null;
    }
}
