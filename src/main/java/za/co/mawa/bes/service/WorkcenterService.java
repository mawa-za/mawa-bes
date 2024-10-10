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
//        workcenterDtoList.add(new WorkcenterDto("dashboard","Dashboard",""));
//        workcenterDtoList.add(new WorkcenterDto("home","Home",""));
//        workcenterDtoList.add(new WorkcenterDto("prospect","Prospects","search"));
        workcenterDtoList.add(new WorkcenterDto("customer","Customers","search"));
        workcenterDtoList.add(new WorkcenterDto("organisation","Organisations","search"));
//        workcenterDtoList.add(new WorkcenterDto("sales-order","Sales Orders","search"));
        workcenterDtoList.add(new WorkcenterDto("invoice","Invoices","search"));
//        workcenterDtoList.add(new WorkcenterDto("purchase-order","Purchase Orders","search"));
        workcenterDtoList.add(new WorkcenterDto("service-request","Service Requests","search"));
//        workcenterDtoList.add(new WorkcenterDto("service-order","Service Orders","search"));
//        workcenterDtoList.add(new WorkcenterDto("leave-request","Leave Requests","search"));
//        workcenterDtoList.add(new WorkcenterDto("leave-approval","Leave Approvals","search"));
        workcenterDtoList.add(new WorkcenterDto("employee","Employees","search"));
//        workcenterDtoList.add(new WorkcenterDto("quotation","Quotations","search"));
        workcenterDtoList.add(new WorkcenterDto("claim","Claims","search"));
        workcenterDtoList.add(new WorkcenterDto("membership","Memberships","search"));
        workcenterDtoList.add(new WorkcenterDto("product","Products","search"));
//        workcenterDtoList.add(new WorkcenterDto("complaint","Complaints","search"));
//        workcenterDtoList.add(new WorkcenterDto("identify-customer","Identify Customer",""));
//        workcenterDtoList.add(new WorkcenterDto("interaction-record","Interaction Record","list"));
        workcenterDtoList.add(new WorkcenterDto("calendar","Calendar",""));
        workcenterDtoList.add(new WorkcenterDto("appointment","Appointments","search"));
//        workcenterDtoList.add(new WorkcenterDto("time-tracker","Time Tracker","search"));
        workcenterDtoList.add(new WorkcenterDto("company-setup","Company Settings",""));
        workcenterDtoList.add(new WorkcenterDto("system-configuration","System Configuration",""));
//        workcenterDtoList.add(new WorkcenterDto("report","Reports",""));
        workcenterDtoList.add(new WorkcenterDto("user","Users","search"));
        workcenterDtoList.add(new WorkcenterDto("receipt","Receipts","search"));
        workcenterDtoList.add(new WorkcenterDto("cashup","Cashups","search"));
        workcenterDtoList.add(new WorkcenterDto("supplier","Suppliers","search"));
        workcenterDtoList.add(new WorkcenterDto("payment-request","Payment Request","search"));
        workcenterDtoList.add(new WorkcenterDto("claim-approval","Claim Approvals","search"));
        workcenterDtoList.add(new WorkcenterDto("partner","Partners","search"));
        workcenterDtoList.add(new WorkcenterDto("membership-approval","Membership Approval","search"));
        workcenterDtoList.add(new WorkcenterDto("group-society","Group Society","search"));
        workcenterDtoList.add(new WorkcenterDto("payment-request-approval","Payment Request Approval","search"));
        workcenterDtoList.add(new WorkcenterDto("case","Cases","search"));
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
