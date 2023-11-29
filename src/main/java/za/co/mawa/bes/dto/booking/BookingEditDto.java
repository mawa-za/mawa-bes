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
public class BookingEditDto implements Serializable {
    private String bookTime;
    private String bookDate;
    private String employeeId;
    private String status;

}
