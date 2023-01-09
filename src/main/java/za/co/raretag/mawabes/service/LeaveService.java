package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dao.LeaveDao;
import za.co.raretag.mawabes.dto.LogLeaveRequestDto;
import za.co.raretag.mawabes.dto.OrderHeaderDto;
import za.co.raretag.mawabes.entity.TransactionEntity;
import za.co.raretag.mawabes.repository.TransactionRepository;
import za.co.raretag.mawabes.utils.OrderType;
import za.co.raretag.mawabes.utils.Status;
import za.co.raretag.mawabes.utils.StatusReason;

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
            OrderHeaderDto orderHeader = new OrderHeaderDto();
            TransactionEntity transactionEntity = new TransactionEntity();
            if (logLeaveDto.getDescription() != null && logLeaveDto.getSubDescription() != null) {

                transactionEntity.setDescription(logLeaveDto.getDescription());
                transactionEntity.setSubDescription(logLeaveDto.getSubDescription());
            }
            transactionEntity.setType(OrderType.LEAVEREQUEST);
            transactionEntity.setSubType(type);
            transactionEntity.setStatus(Status.PENDING);
            transactionEntity.setStatusReason(StatusReason.AWAITING_APPROVAL);
            requestNr =      transactionService.create(transactionEntity);


        }

        return null;
    }
}
