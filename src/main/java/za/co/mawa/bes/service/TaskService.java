package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.TaskDao;
import za.co.mawa.bes.dto.task.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import za.co.mawa.bes.utils.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;

@Component
public class TaskService implements TaskDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionAttributeService transactionAttributeService;
    @Autowired
    PartnerService partnerService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    @Override
    public TaskDto create(TaskCreateDto taskCreateDto) {
        try {

            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.TASK);
            transactionCreateDto.setSubType(taskCreateDto.getType());
            transactionCreateDto.setDescription(taskCreateDto.getDescription());
            transactionCreateDto.setEmployeeResponsible(taskCreateDto.getEmployeeResponsibleId());
            String taskId = transactionService.create(transactionCreateDto).getId();
            TaskDto taskDto = new TaskDto();
            taskDto.setId(taskId);
            if (taskCreateDto.getParentId() != null) {
                TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                transactionLinkDto.setTransaction1(taskCreateDto.getParentId());
                transactionLinkDto.setTransaction2(taskId);
                transactionLinkDto.setCreationDate(new Date());
                transactionLinkDto.setType(TransactionType.TASK);
                transactionLinkDto.setCreateBy(UserContext.getCurrentUser());
                transactionService.addLink(transactionLinkDto);
            }
            if (taskCreateDto.getPlannedStartDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(taskId);
                transactionDateDto.setType(DateType.PLANNED_START_DATE);
                transactionDateDto.setValue(Conversion.stringToDate(taskCreateDto.getPlannedStartDate()));
                transactionService.addDate(transactionDateDto);
            }
            if (taskCreateDto.getPlannedEndDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(taskId);
                transactionDateDto.setType(DateType.PLANNED_END_DATE);
                transactionDateDto.setValue(Conversion.stringToDate(taskCreateDto.getPlannedEndDate()));
                transactionService.addDate(transactionDateDto);
            }

            if(taskCreateDto.getStartDate() != null){
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(taskId);
                transactionDateDto.setType(DateType.START_DATE);
                transactionDateDto.setValue(Conversion.stringToDate(taskCreateDto.getStartDate()));
                transactionService.addDate(transactionDateDto);
            }
            if(taskCreateDto.getDuration() != null){
                TransactionAttributeDto attributeDto = new TransactionAttributeDto();
                attributeDto.setAttribute("DURATION");
                attributeDto.setValue(taskCreateDto.getDuration());
                attributeDto.setTransaction(taskId);
                transactionAttributeService.add(attributeDto);
            }

            if (taskCreateDto.getEmployeeResponsibleId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(taskId);
                transactionPartnerDto.setFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
                transactionPartnerDto.setPartner(taskCreateDto.getEmployeeResponsibleId());
                transactionService.addPartner(transactionPartnerDto);
            }

            if (taskCreateDto.getEmployeeResponsibleId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(taskId);
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(taskCreateDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }
            return taskDto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TaskDto get(String id) {

        try {
            TaskDto taskDto = new TaskDto();
            TransactionDto transactionDto = transactionService.get(id);
            taskDto.setId(transactionDto.getId());
            taskDto.setDescription(transactionDto.getDescription());
            taskDto.setNumber(transactionDto.getNumber());
            taskDto.setType(transactionDto.getSubType());

            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                String formattedDate = Conversion.dateToString(transactionDateDto.getValue());

                if( transactionDateDto.getType().equals(DateType.PLANNED_START_DATE)){
                    taskDto.setPlannedStartDate(formattedDate);
                }
                if( transactionDateDto.getType().equals(DateType.PLANNED_END_DATE)){
                    taskDto.setPlannedEndDate(formattedDate);
                }
                if(transactionDateDto.getType().equalsIgnoreCase(DateType.START_DATE)){
                    taskDto.setStartDate(formattedDate);
                }
            }
            try{
                for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CUSTOMER)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            taskDto.setCustomer(partnerService.get(transactionPartnerDto.getPartner()));
                        }
                    }
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            taskDto.setEmployeeResponsible((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                }
            }catch (Exception e){

            }


            List<TransactionAttributeEntity> entities = transactionAttributeService.getByTransactionId(id);
            for(TransactionAttributeEntity entity : entities){
                if(entity.getAttribute().equalsIgnoreCase("DURATION")){
                    taskDto.setDuration(entity.getValue());
                }
            }

            return taskDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<TaskQueryResultDto> search(TaskQueryDto taskQueryDto) {
        List<TaskQueryResultDto> taskQueryResultDtoList = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.TASK);
        transactionService.search(transactionQueryDto);
        return taskQueryResultDtoList;
    }

    @Override
    public void edit(TaskEditDto taskEditDto) {

    }

    @Override
    public void delete(String id) {

    }

    public static Date stringToDate(String dateStr) {
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid date format. Expected format: dd-MM-yyyy", e);
        }
    }

    public static String dateToString(Date date) {
        return DATE_FORMAT.format(date);
    }


}
