package za.co.mawa.bes.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class InternalAdminResponseDto {
    private boolean success;
    private String message;
    private String tenant;
}
