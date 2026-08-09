package za.co.mawa.bes.entity.v2.company;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "company_logo")
public class CompanyLogoEntity {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "width_px", nullable = false)
    private Integer widthPx;

    @Column(name = "height_px", nullable = false)
    private Integer heightPx;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "uploaded_by", length = 36)
    private String uploadedBy;
}
