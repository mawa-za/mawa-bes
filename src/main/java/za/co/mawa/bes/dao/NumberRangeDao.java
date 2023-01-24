package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.RangeDto;


public interface NumberRangeDao {

    void create(RangeDto range);

    String generateNumber(String object);
}
