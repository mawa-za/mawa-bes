package za.co.mawa.bes.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.leave.request.LeaveRequestInboundDto;
import za.co.mawa.bes.dto.leave.request.LeaveRequestOutboundDto;
import za.co.mawa.bes.dto.leave.request.LeaveRequestQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaveRequestService {
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionService transactionService;
    public LeaveRequestOutboundDto create(LeaveRequestInboundDto leaveRequestInboundDto) {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.LEAVE_REQUEST);
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);
        return get(transactionDto.getId());
    }
    public LeaveRequestOutboundDto get(String id) {
        LeaveRequestOutboundDto leaveRequestOutboundDto = new LeaveRequestOutboundDto();
        return leaveRequestOutboundDto;
    }
    public List<LeaveRequestOutboundDto> search(LeaveRequestQueryDto leaveRequestQueryDto) {
        List<LeaveRequestOutboundDto> leaveRequestOutboundDtoList = new ArrayList<>();
        return leaveRequestOutboundDtoList;
    }
    public void submit(String id) {
        LeaveRequestOutboundDto leaveRequestOutboundDto = new LeaveRequestOutboundDto();
    }
    public void approve(String id) {
        LeaveRequestOutboundDto leaveRequestOutboundDto = new LeaveRequestOutboundDto();
    }
    public void reject(String id) {
        LeaveRequestOutboundDto leaveRequestOutboundDto = new LeaveRequestOutboundDto();
    }
    public void cancel(String id) {
        LeaveRequestOutboundDto leaveRequestOutboundDto = new LeaveRequestOutboundDto();
    }
    public void delete(String id) {
        try {
            transactionService.delete(id);
        } catch (Exception e) {

        }
    }
}
