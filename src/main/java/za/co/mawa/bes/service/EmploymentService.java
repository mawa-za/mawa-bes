package za.co.mawa.bes.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.EmploymentDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.entity.EmploymentEntity;
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
    @Autowired
    NumberRangeService numberRangeService;

    @Override
    public boolean terminate(String id) throws Exception{
        try{

            EmploymentEntity employ = employmentRepository.getById(id);
            if (employ != null) {
                employ.setStatus(Status.TERMINATED);
                employ.setEndDate(new Date());
                employmentRepository.save(employ);
                boolean removeRole = false;
                for(String role:partnerService.getRoles(employ.getPartnerId())){
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
    public String hire(EmploymentCreateDto employment) throws Exception{
                try {
                    PartnerDto partnerDto = partnerService.get(employment.getPartnerId());
                    if(partnerDto != null){
                        EmploymentEntity employ = new EmploymentEntity();
                        employ.setPartnerId(employment.getPartnerId());
                        employ.setType(employment.getType());
                        if(employment.getEndDate() != null && employment.getEndDate() != ""){
                            employ.setEndDate(Conversion.stringToDate(employment.getEndDate()));
                        }
                        else{
                            employ.setEndDate(Conversion.stringToDate("9999-12-31"));
                        }
                        if(employment.getStartDate() != null && employment.getStartDate() != ""){
                            employ.setStartDate(Conversion.stringToDate(employment.getStartDate()));
                        }
                        else{
                            employ.setStartDate(new Date());
                        }
                        employ.setPosition(employment.getPosition());
                        employ.setStatus(Status.ACTIVE);
                        employ.setBranch(employment.getBranch());
                        employ.setDepartment(employment.getDepartment());
                        if(employment.getEmployeeNumber() != null && employment.getEmployeeNumber() != ""){
                            employ.setEmployeeNumber(employment.getEmployeeNumber());
                        }
                        EmploymentEntity created = employmentRepository.save(employ);
                        boolean addRole = true;
                        for(String role:partnerService.getRoles(employment.getPartnerId())){
                            if(role.equalsIgnoreCase(RoleType.EMPLOYEE)){
                                addRole = false;
                            }
                        }
                        if(addRole){
                            partnerService.addRole(employment.getPartnerId(),RoleType.EMPLOYEE);
                        }

                        return created.getId();
                    }
                    else{
                        throw new PartnerNotFoundException();
                    }

                } catch (Exception e) {
                    throw new PartnerNotFoundException();
                }
            }

    @Override
    public boolean suspend(String id) throws Exception{
        try{
            EmploymentEntity employ = employmentRepository.getById(id);
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

            EmploymentEntity employ = employmentRepository.getById(id);
            if (employ != null) {
                employ.setStatus(Status.ACTIVE);
                if(endDate != null && endDate != ""){
                    employ.setEndDate(Conversion.stringToDate(endDate));
                }
                else{
                    employ.setEndDate(Conversion.stringToDate("9999-12-31"));
                }
                if(startDate != null && startDate != ""){
                    employ.setStartDate(Conversion.stringToDate(endDate));
                }
                else{
                    employ.setStartDate(new Date());
                }
                employmentRepository.save(employ);
                boolean addRole = true;
                for(String role:partnerService.getRoles(employ.getPartnerId())){
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
            Sort sort = Sort.by("id").descending();
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
    public EmploymentDto get(String id) {
        try{
            EmploymentDto employmentDto = entityToObject(employmentRepository.findById(id).get());
            return employmentDto;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private EmploymentDto entityToObject(EmploymentEntity entity) {
        EmploymentDto object = new EmploymentDto();
        object.setId(entity.getId());
        object.setBranch(fieldOptionService.getFieldOption(Field.BRANCH, entity.getBranch()));
        object.setDepartment(fieldOptionService.getFieldOption(Field.DEPARTMENT,entity.getDepartment()));
        object.setPosition(entity.getPosition());
        object.setType(fieldOptionService.getFieldOption(Field.EMPLOYMENT_TYPE, entity.getType()));
        object.setEndDate(Conversion.dateToString(entity.getEndDate()));
        object.setStartDate(Conversion.dateToString(entity.getStartDate()));
        object.setStatus(StringConversion.capitalizeFully(entity.getStatus()));
        object.setEmployeeNumber(entity.getEmployeeNumber());
        try {
            PartnerDto  partner = partnerService.getOptional(entity.getPartnerId());
            object.setEmployee(partner);
        } catch (PartnerNotFoundException e) {
           // throw new RuntimeException(e);
        }
        return object;
    }

    @Override
    public boolean edit(EmploymentEditDto employment,String id) throws Exception {
        try{
            EmploymentEntity entity = employmentRepository.getById(id);
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
            if(employment.getStartDate() != null && employment.getStartDate() != ""){
                entity.setStartDate(Conversion.stringToDate(employment.getStartDate()));
            }
            if(employment.getEmployeeNumber() != null && employment.getEmployeeNumber() != ""){
                entity.setEmployeeNumber(employment.getEmployeeNumber());
            }
            employmentRepository.save(entity);
            return true;
        }catch(Exception ex){
            throw new RuntimeException(ex);

        }

    }

    @Override
    public boolean deleteEmployment(String id) throws Exception {
        try{

            employmentRepository.deleteById(id);
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
                predicate = cb.and(predicate,cb.equal(root.get("startDate"),searchDto.getStartDate()));
            }
            if(searchDto.getDepartment() != null){
                predicate = cb.and(predicate,cb.equal(root.get("department"),searchDto.getDepartment()));
            }

            if(searchDto.getPartnerId() != null){
                predicate = cb.and(predicate,cb.equal(root.get("partnerId"),searchDto.getPartnerId()));
            }
            if(searchDto.getEmployeeNumber() != null){
                predicate = cb.and(predicate,cb.equal(root.get("employeeNumber"),searchDto.getEmployeeNumber()));
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
