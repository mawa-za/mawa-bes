package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.EmploymentDto;
import za.co.mawa.bes.entity.EmploymentEntity;

import java.util.List;

public interface EmploymentDao {
    boolean terminate(EmploymentDto employee);
    boolean hire(EmploymentDto employment);
    boolean suspend(EmploymentDto employee);
    boolean rehire(EmploymentDto employee);
    List<EmploymentDto> getAll();
    void getAllByPosition(String string);
    void assignPosition(String string);
    void getAllByPosition();
    void assignPosition();
    EmploymentDto get(String employee);
    boolean edit(EmploymentDto employment);
    List<EmploymentDto> getByOrg(String orgID);
    List<EmploymentDto> getByApprover(String approver);

    boolean assignRole(EmploymentDto employmentDto);

}
