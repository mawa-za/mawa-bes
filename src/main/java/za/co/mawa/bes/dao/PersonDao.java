package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.PersonDto;

public interface PersonDao {
    String createPerson(PersonDto person);
    String getPerson();
}
