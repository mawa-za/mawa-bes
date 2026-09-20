package za.co.mawa.bes.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class UserCardTerminalAssignmentRequest implements Serializable {
    private String cardTerminalId;
}
