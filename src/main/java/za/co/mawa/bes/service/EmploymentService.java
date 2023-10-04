package za.co.mawa.bes.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.EmploymentDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.EmploymentPKEntity;
import za.co.mawa.bes.entity.PartnerRoleEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.PartnerRoleRepository;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class EmploymentService implements EmploymentDao {
    @Autowired
    EmploymentRepository employmentRepository;
    @Autowired
    PartnerRoleRepository partnerRoleRepository;
    @Autowired
    PartnerService partnerService;

    @Autowired
    FieldOptionService fieldOptionService;

    @Autowired
    TransactionService transactionService;

    @Override
    public boolean terminate(String id,String startDate) throws Exception{
        try{
            EmploymentPKEntity employPK = new EmploymentPKEntity();
            employPK.setEmployeeId(id);
            employPK.setStartDate(Conversion.stringToDate(startDate));
            EmploymentEntity employ = employmentRepository.getById(employPK);
            if (employ != null) {
                employ.setStatus(Status.TERMINATED);
                employ.setEndDate(new Date());
                employmentRepository.save(employ);
                boolean removeRole = false;
                for(String role:partnerService.getRoles(id)){
                    if(role.equalsIgnoreCase(RoleType.EMPLOYEE)){
                        removeRole = true;
                    }
                }
                if(removeRole){
                    partnerService.removeRole(id,RoleType.EMPLOYEE);
                }
                return true;

            }
            else{
                throw new DoesNotExist();
            }
        }catch (Exception ex){
           throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean hire(EmploymentCreateDto employment,String id) throws Exception{
                try {
                    PartnerDto partnerDto = partnerService.get(id);
                    if(partnerDto != null){
                        EmploymentPKEntity employPK = new EmploymentPKEntity();
                        employPK.setEmployeeId(id);
                        if(employment.getStartDate() != null && employment.getStartDate() != ""){
                            employPK.setStartDate(Conversion.stringToDate(employment.getStartDate()));
                        }
                        else{
                            employPK.setStartDate(new Date());
                        }
                        EmploymentEntity employ = new EmploymentEntity();
                        employ.setEmploymentPK(employPK);
                        employ.setType(employment.getType());
                        if(employment.getEndDate() != null && employment.getEndDate() != ""){
                            employ.setEndDate(Conversion.stringToDate(employment.getEndDate()));
                        }
                        else{
                            employ.setEndDate(Conversion.stringToDate("9999-12-31"));
                        }
                        employ.setPosition(employment.getPosition());
                        employ.setStatus(Status.ACTIVE);
                        employ.setBranch(employment.getBranch());
                        employ.setDepartment(employment.getDepartment());
                        employmentRepository.save(employ);
                        boolean addRole = true;
                        for(String role:partnerService.getRoles(id)){
                            if(role.equalsIgnoreCase(RoleType.EMPLOYEE)){
                                addRole = false;
                            }
                        }
                        if(addRole){
                            partnerService.addRole(id,RoleType.EMPLOYEE);
                        }
                        return true;
                    }
                    else{
                        throw new PartnerNotFoundException();
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

    @Override
    public boolean suspend(String id,String startDate) throws Exception{
        try{
            EmploymentPKEntity employPK = new EmploymentPKEntity();
            employPK.setEmployeeId(id);
            employPK.setStartDate(Conversion.stringToDate(startDate));
            EmploymentEntity employ = employmentRepository.getById(employPK);
            if (employ != null) {
                employ.setStatus(Status.SUSPENDED);
                employ.setEndDate(new Date());
                employmentRepository.save(employ);
                return true;
            }
            else{
                throw new DoesNotExist();
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean rehire(String id,String startDate,String endDate) throws Exception{
        try{
            EmploymentPKEntity employPK = new EmploymentPKEntity();
            employPK.setEmployeeId(id);
            employPK.setStartDate(Conversion.stringToDate(startDate));
            EmploymentEntity employ = employmentRepository.getById(employPK);
            if (employ != null) {
                employ.setStatus(Status.ACTIVE);
                if(endDate != null && endDate != ""){
                    employ.setEndDate(Conversion.stringToDate(endDate));
                }
                else{
                    employ.setEndDate(Conversion.stringToDate("9999-12-31"));
                }
                employmentRepository.save(employ);
                boolean addRole = true;
                for(String role:partnerService.getRoles(id)){
                    if(role.equalsIgnoreCase(RoleType.EMPLOYEE)){
                        addRole = false;
                    }
                }
                if(addRole){
                    partnerService.addRole(id,RoleType.EMPLOYEE);
                }
                return true;
            }
            else{
                throw new DoesNotExist();
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
    @Override
    public List<EmploymentDto> getAll(EmploymentSearchDto search) throws Exception{
        try {
            Sort sort = Sort.by("employmentPK").descending();
            List<EmploymentDto> employments = new ArrayList<>();
            for (EmploymentEntity employment : employmentRepository.findAll(findByCriteria(search), sort)) {
                EmploymentDto emp = new EmploymentDto();
                emp = entityToObject(employment);
                employments.add(emp);
            }
            return employments;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
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

        String branch = fieldOptionService.getOptionalFieldDescription("BRANCH", entity.getBranch());
        if (branch != null) {
            object.setBranch(branch);
        }
        String department = fieldOptionService.getOptionalFieldDescription("DEPARTMENT", entity.getDepartment());
        if (department != null) {
            object.setDepartment(department);
        }
        object.setEmployeeId(entity.getEmploymentPK().getEmployeeId());
        object.setEndDate(Conversion.dateToString(entity.getEndDate()));
        String title = fieldOptionService.getOptionalFieldDescription("JOBTITLE", entity.getPosition());
        if (title != null) {
            object.setPosition(title);
        }
        object.setStartDate(Conversion.dateToString(entity.getEmploymentPK().getStartDate()));
        object.setStatus(StringConversion.capitalizeFully(entity.getStatus()));
        String empType = fieldOptionService.getOptionalFieldDescription("EMPLOYMENTTYPE", entity.getType());
        if (empType != null) {
            object.setType(empType);
        }
        PartnerDto partner = partnerService.getOptional(entity.getEmploymentPK().getEmployeeId());
        if(partner != null){
            object.setEmployee(partner);
        }
        return object;
    }

    @Override
    public boolean edit(EmploymentEditDto employment,String id,String startDate) throws Exception {
        try{
            EmploymentPKEntity pkEntity = new EmploymentPKEntity();
            pkEntity.setStartDate(Conversion.stringToDate(startDate));
            pkEntity.setEmployeeId(id);
            EmploymentEntity entity = employmentRepository.getById(pkEntity);
            if(entity != null)
            {
                if(employment.getBranch() != null && employment.getBranch() != ""){
                    entity.setBranch(employment.getBranch());
                }
                if(employment.getDepartment() != null && employment.getDepartment() != ""){
                    entity.setDepartment(employment.getDepartment());
                }
                if(employment.getPosition() != null && employment.getPosition() != ""){
                    entity.setPosition(employment.getPosition());
                }
                if(employment.getType() != null && employment.getType() != ""){
                    entity.setType(employment.getType());
                }
                if(employment.getEndDate() != null && employment.getEndDate() != ""){
                    entity.setEndDate(Conversion.stringToDate(employment.getEndDate()));
                }
                employmentRepository.save(entity);
                return true;
            }
            else {
                throw new DoesNotExist();
            }

        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean deleteEmployment(String id, String startDate) throws Exception {
        try{
            EmploymentPKEntity pkEntity = new EmploymentPKEntity();
            pkEntity.setEmployeeId(id);
            pkEntity.setStartDate(Conversion.stringToDate(startDate));
            employmentRepository.deleteById(pkEntity);
            return true;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }


    @Override
    public ArrayList<PartnerDto> getEmployees() throws Exception {
        try{
            ArrayList<PartnerDto> employees = new ArrayList<>();
            for(PartnerRoleEntity role:partnerRoleRepository.findPartnerByRole(RoleType.EMPLOYEE)){
                PartnerDto partnerDto = partnerService.getOptional(role.getPartnerRolePK().getId());
                employees.add(partnerDto);
            }
            return employees;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }

    }
    private Specification<EmploymentEntity> findByCriteria(EmploymentSearchDto searchDto){
        return(root,query,cb) ->{
            Predicate predicate = cb.conjunction();
            if(searchDto.getStartDate() != null){
                predicate = cb.and(predicate,cb.equal(root.get("employmentPK").get("startDate"),searchDto.getStartDate()));
            }
            if(searchDto.getDepartment() != null){
                predicate = cb.and(predicate,cb.equal(root.get("department"),searchDto.getDepartment()));
            }
            if(searchDto.getEmployeeId() != null){
                predicate = cb.and(predicate,cb.equal(root.get("employmentPK").get("employeeId"),searchDto.getEmployeeId()));
            }
            if(searchDto.getEndDate() != null){
                predicate = cb.and(predicate,cb.equal(root.get("endDate"),searchDto.getEndDate()));
            }
            if(searchDto.getPosition() != null){
                predicate = cb.and(predicate,cb.equal(root.get("position"),searchDto.getPosition()));
            }
            if(searchDto.getBranch() != null){
                predicate = cb.and(predicate,cb.equal(root.get("branch"),searchDto.getBranch()));
            }
            if(searchDto.getType() != null){
                predicate = cb.and(predicate,cb.equal(root.get("type"),searchDto.getType()));
            }
            if(searchDto.getStatus() != null){
                predicate = cb.and(predicate,cb.equal(root.get("status"),searchDto.getStatus()));
            }
            return predicate;
        };

    }

}
