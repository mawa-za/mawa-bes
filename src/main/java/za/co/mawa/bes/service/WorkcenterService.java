package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.WorkcenterDao;
import za.co.mawa.bes.dto.FieldDto;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.exception.FieldDoesNotExist;
import za.co.mawa.bes.exception.RoleDoesNotExist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class WorkcenterService implements WorkcenterDao {
    @Override
    public List<WorkcenterDto> getAll() {
        List<WorkcenterDto> workcenterDtoList = new ArrayList<>();
        workcenterDtoList.add(new WorkcenterDto("customer","Customers","search"));
        workcenterDtoList.add(new WorkcenterDto("organisation","Organisations","search"));
        workcenterDtoList.add(new WorkcenterDto("service-request","Service Requests","search"));
        workcenterDtoList.add(new WorkcenterDto("leave-request","Leave Requests","search"));
        workcenterDtoList.add(new WorkcenterDto("leave-approval","Leave Approvals","search"));
        workcenterDtoList.add(new WorkcenterDto("employee","Employees","search"));
        return workcenterDtoList;
    }

    @Override
    public WorkcenterDto getById(String id) throws RoleDoesNotExist {
        List<WorkcenterDto> workcenterDtoList = getAll().stream()
                .filter(a -> Objects.equals(a.getId(), id))
                .toList();
        if (!workcenterDtoList.isEmpty()) {
            return workcenterDtoList.iterator().next();
        } else {
            throw new RoleDoesNotExist();
        }
    }

}
