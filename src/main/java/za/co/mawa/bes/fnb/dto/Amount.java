package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class Amount implements Serializable {
    private String currency;
    private double value;
}