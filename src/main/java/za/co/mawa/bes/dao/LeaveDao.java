package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.LogLeaveRequestDto;

public interface LeaveDao {

    String request(LogLeaveRequestDto logLeaveDto);
}
