package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.task.*;

import java.util.List;

public interface TaskDao {
    TaskDto create(TaskCreateDto taskCreateDto);
    TaskDto get(String id);
    List<TaskQueryResultDto> search(TaskQueryDto taskQueryDto);
    void edit(TaskEditDto taskEditDto);
    void delete(String id);
}
