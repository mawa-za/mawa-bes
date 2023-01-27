package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.NumberRangeEntity;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.NumberRangeRepository;
import za.co.mawa.bes.dao.NumberRangeDao;
import za.co.mawa.bes.dto.NumberRangeDto;

import java.util.List;
import java.util.Objects;

@Service
public class NumberRangeService implements NumberRangeDao {

    @Autowired
    NumberRangeRepository numberRangeRepository;

    @Override
    public void create(NumberRangeDto numberRangeDto) {
        List<NumberRangeEntity> numberRanges = numberRangeRepository.findAll();
//        numberRangeRepo.findById(range.getObject());
        if (!numberRanges.isEmpty()) {

            for (NumberRangeEntity numberRange : numberRanges) {


            }
        }

    }

    @Override
    public String generateNumber(String object) throws NumberRangeObjectNotFound {

        String newNumber = null;
        List<NumberRangeEntity> numberRanges = numberRangeRepository.findAll();

        List<NumberRangeEntity> result = numberRanges.stream()
                .filter(a -> Objects.equals(a.getObject(), object))
                .toList();

        if (!result.isEmpty()){
                {
                    NumberRangeEntity numberRangeEntity = result.iterator().next();
                    String currentNumber;
                    String withPrefix = numberRangeEntity.getCurrent();
                    if (numberRangeEntity.getPrefix() == null) {
                        currentNumber = numberRangeEntity.getCurrent();
                    } else {
                        currentNumber = withPrefix.replace(numberRangeEntity.getPrefix(), "");
                    }

                    int length = currentNumber.length();

                    Integer tempIntNum = Integer.parseInt(currentNumber);
                    tempIntNum = tempIntNum + 1;
                    newNumber = Integer.toString(tempIntNum);
                    while (newNumber.length() != length) {
                        newNumber = "0" + newNumber;
                    }
                    if (numberRangeEntity.getPrefix() == null) {
                        numberRangeEntity.setCurrent(newNumber);
                    } else {
                        numberRangeEntity.setCurrent(numberRangeEntity.getPrefix() + newNumber);
                    }
                    newNumber = numberRangeEntity.getCurrent();

                    try {
                        numberRangeRepository.save(numberRangeEntity);
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }else{
            throw new NumberRangeObjectNotFound();
        }

        return  newNumber;
    }
}
