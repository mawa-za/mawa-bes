package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.voucher.VoucherCreateDto;
import za.co.mawa.bes.dto.voucher.VoucherDto;
import za.co.mawa.bes.dto.voucher.VoucherEditDto;
import za.co.mawa.bes.dto.voucher.VoucherQuery;

import java.util.ArrayList;

public interface VoucherDao {
    String create(VoucherCreateDto createDto) throws Exception;
    VoucherDto get(String id) throws Exception;
    ArrayList<VoucherDto> search(VoucherQuery query) throws Exception;
    boolean edit(VoucherEditDto edit,String id) throws Exception;
    boolean delete(String id) throws Exception;
}
