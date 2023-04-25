package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.NumberRangeEntity;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.NumberRangeRepository;
import za.co.mawa.bes.dao.NumberRangeDao;
import za.co.mawa.bes.dto.NumberRangeDto;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class NumberRangeService implements NumberRangeDao {

    @Autowired
    NumberRangeRepository numberRangeRepository;
    public static final String START = "0000000001";
    public static final String END = "9999999999";

    @Override
    public void create(NumberRangeDto numberRangeDto) {
        try {
            if (numberRangeRepository.getRangeByObject(numberRangeDto.getObject()) == null) {
                NumberRangeEntity numberRangeEntity = new NumberRangeEntity();
                numberRangeEntity.setObject(numberRangeDto.getObject());
                numberRangeEntity.setCurrent(START);
                numberRangeEntity.setStart(START);
                numberRangeEntity.setEnd(END);
                numberRangeEntity.setValidFrom(new Date());
                numberRangeRepository.save(numberRangeEntity);
            }
        } catch (Exception exception) {

        }

    }

    @Override
    public String generateNumber(String object) throws NumberRangeObjectNotFound {

        try {
            String newNumber = null;
            NumberRangeEntity numberRangeEntity = numberRangeRepository.getRangeByObject(object);
            if (numberRangeEntity != null) {

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
                numberRangeRepository.save(numberRangeEntity);
                if(numberRangeEntity.getPrefix() == null)
                {
                    return newNumber;
                }
                else {
                    return numberRangeEntity.getPrefix() + newNumber;
                }

            } else {
                throw new NumberRangeObjectNotFound();
            }

        } catch (Exception ex) {
            return null;
        }
    }

}

