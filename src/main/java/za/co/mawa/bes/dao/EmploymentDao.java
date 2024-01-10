package za.co.mawa.bes.dao;
import za.co.mawa.bes.dto.EmploymentCreateDto;
import za.co.mawa.bes.dto.EmploymentDto;
import za.co.mawa.bes.dto.EmploymentEditDto;
import za.co.mawa.bes.dto.EmploymentSearchDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.util.ArrayList;
import java.util.List;

public interface EmploymentDao {
    boolean terminate(String id,String partner) throws Exception;
    boolean hire(EmploymentCreateDto employment) throws Exception;
    boolean suspend(String id,String partner) throws Exception;
    boolean rehire(String id,String partner,String startDate,String endDate) throws Exception;
    List<EmploymentDto> getAll(EmploymentSearchDto search) throws Exception;
    EmploymentDto get(String employee) throws Exception;
    boolean edit(EmploymentEditDto employment,String id,String partner) throws Exception;
    boolean deleteEmployment(String id,String startDate) throws Exception;
    ArrayList<PartnerDto> getEmployees() throws Exception;
}
