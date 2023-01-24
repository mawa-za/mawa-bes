package za.co.mawa.bes.dto;

import java.util.List;

public class EmailDto {
    private String from;
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;

}
