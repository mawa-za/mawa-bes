package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.voucher.*;

import java.util.ArrayList;
import java.util.List;

public interface VoucherDao {
    VoucherOutboundDto create(VoucherInboundDto createDto) throws Exception;
    VoucherOutboundDto get(String id) throws Exception;
    List<VoucherOutboundDto> search(VoucherQuery query) throws Exception;
    VoucherOutboundDto edit(String id, VoucherEditDto edit) throws Exception;
    boolean delete(String id) throws Exception;
}
