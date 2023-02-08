package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import za.co.mawa.bes.dao.PersonDao;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.utils.PartnerType;
import za.co.raretag.mawabes.dto.PartnerQueryDto;

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
    public String getPerson() {
//        PartnerQueryDto query = new PartnerQueryDto();
//        query.setIdType(idtype);
//        query.setIdNumber(idnumber);
//        query.setName1(surname);
//        query.setName2(name);
//        query.setName3(name);
//        query.setCellphone(cellnumber);
//        ArrayList<PartnerObject> objects = partnerBean.search(query);
        return null;
    }
}
