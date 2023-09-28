package za.co.mawa.bes.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BookingCreateDto implements Serializable {
    String productId;
    String customerId;
    String employeeId;
    String bookDate;
    String bookTime;

}
