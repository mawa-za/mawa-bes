package za.co.mawa.bes.dto;

import java.io.Serializable;
import java.util.List;

public class EmailDto implements Serializable {
    private String from;
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;

}
