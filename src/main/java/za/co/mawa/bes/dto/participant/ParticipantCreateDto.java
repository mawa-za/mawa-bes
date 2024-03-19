package za.co.mawa.bes.dto.participant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ParticipantCreateDto {
    private String function;
    private String partner;
}
