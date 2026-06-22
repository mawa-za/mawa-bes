package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.CaseNoteType;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "case_note")
public class CaseNoteEntity {
    @Id @GeneratedValue(generator = "system-uuid") @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "case_id", nullable = false) private String caseId;
    @Enumerated(EnumType.STRING) @Column(name = "note_type", nullable = false) private CaseNoteType noteType = CaseNoteType.GENERAL;
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false) private String note;
    @Column(name = "private_note", nullable = false) private Boolean privateNote = false;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by") private String createdBy;
}
