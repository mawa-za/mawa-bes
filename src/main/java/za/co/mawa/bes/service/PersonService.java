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
        PartnerDto partnerDto;
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
            partnerDto = partnerService.create(partner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return partnerDto.getId();
    }

    @Override
    public PersonDto getPerson(String partner) {
        try {
            PartnerDto object = partnerService.get(partner);
            PersonDto person = null;
            if (object != null) {
                person = new PersonDto(object);
            }
            return person;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArrayList<PersonDto> getPersons(String type) {

        return null;
    }

}
