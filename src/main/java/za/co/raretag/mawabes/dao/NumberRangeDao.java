package za.co.raretag.mawabes.dao;

import org.springframework.beans.factory.annotation.Autowired;
import za.co.raretag.mawabes.dto.RangeDto;


public interface NumberRangeDao {

    void create(RangeDto range);

    String generateNumber(String object);
}
