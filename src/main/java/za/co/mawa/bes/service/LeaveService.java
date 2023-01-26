package za.co.mawa.bes.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.OrderType;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.StatusReason;
import za.co.mawa.bes.dao.LeaveDao;
import za.co.mawa.bes.dto.LogLeaveRequestDto;
import za.co.mawa.bes.dto.TransactionDto;
import za.co.mawa.bes.entity.TransactionEntity;

import java.util.Calendar;
@Service
public class LeaveService implements LeaveDao {
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionService transactionService;
    @Override
    public String request(LogLeaveRequestDto logLeaveDto) {

        String requestNr = null;
        String type = null;

        Calendar startDateCalendar = Calendar.getInstance();
        Calendar endDateCalendar = Calendar.getInstance();
        long numberOfDays = 0;
        if (logLeaveDto.getLeaveType() != null) {
            type = logLeaveDto.getLeaveType().replaceAll(" ", "").toUpperCase();
            TransactionDto orderHeader = new TransactionDto();
            TransactionEntity transactionEntity = new TransactionEntity();
            if (logLeaveDto.getDescription() != null && logLeaveDto.getSubDescription() != null) {

                transactionEntity.setDescription(logLeaveDto.getDescription());
                transactionEntity.setSubDescription(logLeaveDto.getSubDescription());
            }
            transactionEntity.setType(OrderType.LEAVEREQUEST);
            transactionEntity.setSubType(type);
            transactionEntity.setStatus(Status.PENDING);
            transactionEntity.setStatusReason(StatusReason.AWAITING_APPROVAL);

//            requestNr =      transactionService.create(transactionEntity);


        }

        return null;
    }
}
