package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.NumberRangeEntity;
import za.co.mawa.bes.repository.NumberRangeRepository;
import za.co.mawa.bes.dao.NumberRangeDao;
import za.co.mawa.bes.dto.RangeDto;

import java.util.List;

@Service
public class NumberRangeService implements NumberRangeDao {

    @Autowired
    NumberRangeRepository numberRangeRepo;

    @Override
    public void create(RangeDto range) {
        List<NumberRangeEntity> numberRanges = numberRangeRepo.findAll();
//        numberRangeRepo.findById(range.getObject());
        if (!numberRanges.isEmpty()) {

            for (NumberRangeEntity numberRange : numberRanges) {


            }
        }

    }

    @Override
    public String generateNumber(String object) {

        String newNumber = null;
        List<NumberRangeEntity> numberRanges = numberRangeRepo.findAll();
        for (NumberRangeEntity num : numberRanges) {

            if (num.getObject().equals(object)) {
                {
                    String currentNumber;
                    String withPrefix = num.getCurrent();
                    if (num.getPrefix() == null) {
                        currentNumber = num.getCurrent();
                    } else {
                        currentNumber = withPrefix.replace(num.getPrefix(), "");
                    }

                    int length = currentNumber.length();

                    Integer tempIntNum = Integer.parseInt(currentNumber);
                    tempIntNum = tempIntNum + 1;
                    newNumber = Integer.toString(tempIntNum);
                    while (newNumber.length() != length) {
                        newNumber = "0" + newNumber;
                    }
                    if (num.getPrefix() == null) {
                        num.setCurrent(newNumber);
                    } else {
                        num.setCurrent(num.getPrefix() + newNumber);
                    }
                    newNumber = num.getCurrent();

                    try {
                        numberRangeRepo.save(num);
                    } catch (Exception ex) {
                        return null;
                    }

                }
            }
        }
        return  newNumber;
    }
}
