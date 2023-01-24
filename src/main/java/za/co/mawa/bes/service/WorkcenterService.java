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
        workcenterDtoList.add(new WorkcenterDto("Customer","Customers","Search"));
        workcenterDtoList.add(new WorkcenterDto("Organisation","Organisations","Search"));
        workcenterDtoList.add(new WorkcenterDto("ServiceRequest","Service Requests","Search"));
        workcenterDtoList.add(new WorkcenterDto("LeaveRequest","Leave Requests","Search"));
        workcenterDtoList.add(new WorkcenterDto("LeaveApproval","Leave Approvals","Search"));
        workcenterDtoList.add(new WorkcenterDto("Employee","Employees","Search"));
        return workcenterDtoList;
    }

}
