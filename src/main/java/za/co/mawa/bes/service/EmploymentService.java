package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.EmploymentDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.EmploymentPKEntity;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class EmploymentService implements EmploymentDao {
    @Autowired
    EmploymentRepository employmentRepository;
    @Autowired
    PartnerService partnerService;

    @Autowired
    FieldOptionService fieldOptionService;

    @Autowired
    TransactionService transactionService;
    @Override
    public boolean terminate(EmploymentDto employee) {
        boolean processed = false;
        EmploymentPKEntity employPK = new EmploymentPKEntity();
        employPK.setEmployeeId(employee.getEmployeeId());
        employPK.setStartDate(Conversion.stringToDate(employee.getStartDate()));

        EmploymentEntity employ = employmentRepository.getById(employPK);
        if (employ != null) {
            try {
                if (employee.getEndDate() != null) {
                    employ.setEndDate(Conversion.stringToDate(employee.getEndDate()));
                } else {
                    employ.setEndDate(new Date());
                }

                employ.setStatus(Status.TERMINATED);
                employmentRepository.save(employ);

                partnerService.removeRole(employee.getEmployeeId(), RoleType.EMPLOYEE);
                processed = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return processed;
    }

    @Override
    public boolean hire(EmploymentDto employment) {
        boolean hired = false;
        if (employment != null) {
            if (employment.getEmployeeId() != null) {
                try {
                    EmploymentPKEntity employPK = new EmploymentPKEntity();
                    employPK.setEmployeeId(employment.getEmployeeId());
                    employPK.setStartDate(Conversion.stringToDate(employment.getStartDate()));
                    EmploymentEntity employ = new EmploymentEntity();
                    employ.setEmploymentPK(employPK);
                    employ.setType(employment.getType());
                    employ.setEndDate(Conversion.stringToDate("9999-12-31"));
                    employ.setPosition(employment.getPosition());
                    employ.setStatus(Status.ACTIVE);
                    employ.setBranch(employment.getBranch());
                    employ.setDepartment(employment.getDepartment());
                    employmentRepository.save(employ);
                    hired = true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return hired;
    }

    @Override
    public boolean suspend(EmploymentDto employee) {
        boolean processed = false;
        EmploymentPKEntity employPK = new EmploymentPKEntity();
        employPK.setEmployeeId(employee.getEmployeeId());
        employPK.setStartDate(Conversion.stringToDate(employee.getStartDate()));

        EmploymentEntity employ = employmentRepository.getById(employPK);
        if (employ != null) {
            try {
                employ.setStatus(Status.SUSPENDED);
                employmentRepository.save(employ);
                processed = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return processed;
    }

    @Override
    public boolean rehire(EmploymentDto employee) {
        boolean processed = false;
        EmploymentPKEntity employPK = new EmploymentPKEntity();
        employPK.setEmployeeId(employee.getEmployeeId());
        employPK.setStartDate(Conversion.stringToDate(employee.getStartDate()));

        EmploymentEntity employ = employmentRepository.getById(employPK);
        if (employ != null) {
            try {
                employ.setEndDate(Conversion.stringToDate("9999-12-31"));
                employ.setStatus(Status.ACTIVE);
                employmentRepository.save(employ);
                processed = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return processed;
    }

    @Override
    public List<EmploymentDto> getAll() {
        List<EmploymentEntity> employess = employmentRepository.findAll();
        List<EmploymentDto> currentEmployment = new ArrayList<>();

        if (!employess.isEmpty()) {
            for (EmploymentEntity empDetails : employess) {
                EmploymentDto emp = get(empDetails.getEmploymentPK().getEmployeeId());
                if (emp != null) {
                    currentEmployment.add(emp);
                }
            }
        }

        return currentEmployment;
    }

    @Override
    public void getAllByPosition(String string) {

    }

    @Override
    public void assignPosition(String string) {

    }

    @Override
    public void getAllByPosition() {

    }

    @Override
    public void assignPosition() {

    }

    @Override
    public EmploymentDto get(String employee) {
        EmploymentDto currentEmployment = null;
        for (EmploymentEntity employment : employmentRepository.findEmploymentById(employee)) {
            if (employment.getEndDate().after(new Date())) {
                currentEmployment = entityToObject(employment);
            }
        }
        return currentEmployment;
    }

    private EmploymentDto entityToObject(EmploymentEntity entity) {
        EmploymentDto object = new EmploymentDto();
        object.setBranch(fieldOptionService.getFieldOptionDescription("BRANCH", entity.getBranch()));
        object.setDepartment(fieldOptionService.getFieldOptionDescription("DEPARTMENT", entity.getDepartment()));
        object.setEmployeeId(entity.getEmploymentPK().getEmployeeId());
        object.setEndDate(Conversion.dateToString(entity.getEndDate()));
        object.setPosition(fieldOptionService.getFieldOptionDescription("JOBTITLE", entity.getPosition()));
        object.setStartDate(Conversion.dateToString(entity.getEmploymentPK().getStartDate()));
        object.setStatus(StringConversion.capitalizeFully(entity.getStatus()));
        object.setType(fieldOptionService.getFieldOptionDescription("EMPLOYMENTTYPE", entity.getType()));
        ArrayList<RelationDto> partnerRelations = partnerService.getRelationByPartner2(object.getEmployeeId());
        if (!partnerRelations.isEmpty()) {
            for (RelationDto relation : partnerRelations) {

                if (relation.getType().equals(PartnerFunction.EMPLOYEE)) {
                    object.setGroupID(relation.getPartner1());
                    PartnerDto groupObject = partnerService.get(object.getGroupID());
                    if (groupObject != null) {
                        object.setGroupName(groupObject.getName2());
                    }
                }
            }
        }

        return object;
    }

    @Override
    public boolean edit(EmploymentDto employment) {
        EmploymentPKEntity employPK = new EmploymentPKEntity();
        employPK.setEmployeeId(employment.getEmployeeId());
        employPK.setStartDate(Conversion.stringToDate(employment.getStartDate()));

        EmploymentEntity employ = employmentRepository.getById(employPK);
        if (employ != null) {
            try {
                if (employment.getBranch() != null) {
                    employ.setBranch(employment.getBranch());
                }
                if (employment.getDepartment() != null) {
                    String department = employment.getDepartment().replaceAll(" ", "").toUpperCase();
                    employ.setDepartment(department);
                }
                if (employment.getPosition() != null) {
                    employ.setPosition(employment.getPosition());
                }
                if (employment.getType() != null) {
                    employ.setType(employment.getType());
                }
                employmentRepository.save(employ);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            String department = employment.getDepartment().replaceAll(" ", "").toUpperCase();
            employment.setDepartment(department);
            boolean hired = hire(employment);
            return hired;
        }

    }

    @Override
    public List<EmploymentDto> getByOrg(String orgID) {
        List<EmploymentEntity> employess = employmentRepository.findAll();
        List<EmploymentDto> currentEmployment = new ArrayList<>();

        if (!employess.isEmpty()) {
            for (EmploymentEntity empDetails : employess) {
                EmploymentDto emp = get(empDetails.getEmploymentPK().getEmployeeId());
                if (emp != null) {
                    if (emp.getGroupID() != null) {
                        if (emp.getGroupID().equals(orgID)) {
                            currentEmployment.add(emp);
                        }
                    }
                }
            }
        }

        return currentEmployment;
    }

    @Override
    public List<EmploymentDto> getByApprover(String approver) {
        List<EmploymentDto> currentEmployment = new ArrayList<>();
        ArrayList<OrderHeaderDto> orderHeaders = transactionService.getTransactionByApprover(approver);
        for(OrderHeaderDto orderHeaderObj : orderHeaders){
            ArrayList<OrderPartnerDto> orderPartners = transactionService.getPartners(orderHeaderObj.getId());
            for(OrderPartnerDto orderPartnerObj : orderPartners){
                if(orderPartnerObj.getFunction().equals(PartnerFunction.ASSIGNED_APPROVER)){
                    EmploymentDto employee = get(orderPartnerObj.getPartner());
                    if(employee != null){
                        currentEmployment.add(employee);
                    }
                }
            }
        }
        return currentEmployment;
    }
}
