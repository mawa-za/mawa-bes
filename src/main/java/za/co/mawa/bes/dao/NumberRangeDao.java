package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.NumberRangeDto;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;


public interface NumberRangeDao {

    void create(NumberRangeDto range);

    String generateNumber(String object) throws NumberRangeObjectNotFound;
}
