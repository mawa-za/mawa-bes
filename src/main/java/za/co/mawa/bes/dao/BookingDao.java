package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.booking.BookingCreateDto;
import za.co.mawa.bes.dto.booking.BookingDto;
import za.co.mawa.bes.dto.booking.BookingEditDto;
import za.co.mawa.bes.dto.booking.BookingQueryDto;

import java.util.ArrayList;

public interface BookingDao {
    String createBooking(BookingCreateDto createDto) throws Exception;
    BookingDto getBooking(String id) throws Exception;
    ArrayList<BookingDto> querBooking(BookingQueryDto queryDto) throws Exception;
    boolean editBooking(BookingEditDto editDto,String id) throws Exception;
    boolean removeBooking(String id) throws Exception;
}
