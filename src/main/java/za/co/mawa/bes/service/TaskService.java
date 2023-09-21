package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dao.TaskDao;
import za.co.mawa.bes.dto.task.*;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.utils.TransactionType;

import java.util.List;

@Component
public class TaskService implements TaskDao {
    @Autowired
    TransactionService transactionService;
    @Override
    public String create(TaskCreateDto taskCreateDto) {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.TASK);
        transactionCreateDto.setSubType(taskCreateDto.getType());
        String taskId = transactionService.create(transactionCreateDto).getId();
        return taskId;
    }

    @Override
    public TaskDto get(String id) {
        return null;
    }

    @Override
    public List<TaskQueryResultDto> search(TaskQueryDto taskQueryDto) {
        return null;
    }

    @Override
    public void edit(TaskEditDto taskEditDto) {

    }

    @Override
    public void delete(String id) {

    }
}
