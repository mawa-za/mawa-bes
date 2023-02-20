package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.PersonDao;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.utils.PartnerType;

import java.util.ArrayList;

@Service
public class PersonService implements PersonDao {
    @Autowired
    PartnerService partnerService;
    @Autowired
    UserService userService;
    @Override
    public String createPerson(PersonDto person) {
        String partnerNo;
        try {
            PartnerDto partner = new PartnerDto();
            partner.setType(PartnerType.PERSON);
            partner.setTitle(person.getTitle());
            partner.setName1(person.getLastName());
            partner.setName2(person.getFirstName());
            partner.setName3(person.getMiddleName());
            partner.setBirthDate(person.getBirthDate());
            partner.setGender(person.getGender());
            partner.setMaritalStatus(person.getMaritalStatus());
            partner.setLanguage(person.getLanguage());
            partner.setCreatedBy(userService.getCurrentUser());
            partnerNo = partnerService.create(partner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return partnerNo;
    }

    @Override
    public PersonDto getPerson(String partner) {
        PartnerDto object = partnerService.get(partner);
        PersonDto person = null;
        try {
            if (object != null) {
                person = new PersonDto(object);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return person;
    }

}
