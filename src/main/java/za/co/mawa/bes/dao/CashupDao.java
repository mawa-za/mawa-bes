package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.cashup.CashupCreateDto;
import za.co.mawa.bes.dto.cashup.CashupDto;
import za.co.mawa.bes.dto.cashup.CashupEditDto;
import za.co.mawa.bes.dto.cashup.CashupSearchDto;
import za.co.mawa.bes.exception.DoesNotExist;
public interface CashupDao {
    String create(CashupCreateDto cashupCreateDto) throws Exception;
    CashupDto get(String id) throws Exception,DoesNotExist;
    CashupDto getCashups(CashupSearchDto search) throws Exception;
    boolean edit(CashupEditDto edit,String id) throws Exception;
}
