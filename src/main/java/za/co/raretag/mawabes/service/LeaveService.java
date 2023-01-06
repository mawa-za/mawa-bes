package za.co.raretag.mawabes.service;

import za.co.raretag.mawabes.dao.LeaveDao;
import za.co.raretag.mawabes.dto.LogLeaveRequestDto;

import java.util.Calendar;

public class LeaveService implements LeaveDao {
    @Override
    public String request(LogLeaveRequestDto logLeaveDto) {

        String requestNr = null;
        String type = null;
        Calendar startDateCalendar = Calendar.getInstance();
        Calendar endDateCalendar = Calendar.getInstance();
        long numberOfDays = 0;
        if (logLeaveDto.getLeaveType() != null) {
            type = logLeaveDto.getLeaveType().replaceAll(" ", "").toUpperCase();

        }

        return null;
    }
}
