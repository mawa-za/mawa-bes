package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.TaskDao;
import za.co.mawa.bes.dto.DateDto;
import za.co.mawa.bes.dto.task.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class TaskService implements TaskDao {
    @Autowired
    TransactionService transactionService;

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
            taskDto.setStatus(transactionDto.getStatus());
            taskDto.setDescription(transactionDto.getDescription());
            taskDto.setNumber(transactionDto.getNumber());
            taskDto.setType(transactionDto.getSubType());

            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {

                if( transactionDateDto.getType().equals(DateType.PLANNED_START_DATE)){
                    taskDto.setPlannedStartDate(transactionDateDto);
                }
                if( transactionDateDto.getType().equals(DateType.PLANNED_END_DATE)){
                    taskDto.setPlannedEndDate(transactionDateDto);
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


}
