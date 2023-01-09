package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.dto.LogLeaveRequestDto;

public interface LeaveDao {

    String request(LogLeaveRequestDto logLeaveDto);
}
