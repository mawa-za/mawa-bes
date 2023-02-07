package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.WorkcenterDao;
import za.co.mawa.bes.dto.WorkcenterDto;

import java.util.ArrayList;
import java.util.List;

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

}
