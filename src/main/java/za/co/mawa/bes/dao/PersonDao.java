package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.PersonDto;

import java.util.ArrayList;

public interface PersonDao {
    String createPerson(PersonDto person);
    PersonDto getPerson(String partner);
    ArrayList<PersonDto> getPersons(String type);

}
