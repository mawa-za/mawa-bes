package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.RangeDto;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;


public interface NumberRangeDao {

    void create(RangeDto range);

    String generateNumber(String object) throws NumberRangeObjectNotFound;
}
