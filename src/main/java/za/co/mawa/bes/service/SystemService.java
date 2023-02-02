package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.NumberRangeDto;
import za.co.mawa.bes.utils.TransactionType;

@Service
public class SystemService {
    @Autowired
    NumberRangeService numberRangeService;
    public void initialize(){
        generateNumberRanges();
    }
    private void generateNumberRanges(){
        NumberRangeDto numberRangeDto = new NumberRangeDto();
        numberRangeDto.setObject(TransactionType.QUOTATION);
        numberRangeService.create(numberRangeDto);
    }
}
