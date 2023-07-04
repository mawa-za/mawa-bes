package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.deposit.DepositCreateDto;
import za.co.mawa.bes.dto.deposit.DepositDto;
import za.co.mawa.bes.dto.deposit.DepositEditDto;
import za.co.mawa.bes.dto.deposit.DepositSearchDto;

import java.util.ArrayList;

public interface DepositDao {
    String create(DepositCreateDto depositCreate) throws Exception;
    DepositDto get(String id) throws Exception;
    ArrayList<DepositDto> search(DepositSearchDto searchDto) throws Exception;
    boolean delete(String id) throws  Exception;
    ArrayList<DepositDto> searchByTransactionLink(String linkId);
}
