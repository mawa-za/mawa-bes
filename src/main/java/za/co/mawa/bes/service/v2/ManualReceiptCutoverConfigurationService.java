package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ManualReceiptCutoverConfigurationDto;
import za.co.mawa.bes.entity.v2.ManualReceiptCutoverConfigurationEntity;
import za.co.mawa.bes.repository.v2.ManualReceiptCutoverConfigurationRepository;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ManualReceiptCutoverConfigurationService {
    public static final String SINGLETON_ID = "DEFAULT";
    private final ManualReceiptCutoverConfigurationRepository repository;

    @Transactional(readOnly = true)
    public ManualReceiptCutoverConfigurationEntity getRequired() {
        return repository.findById(SINGLETON_ID)
            .orElseThrow(() -> new IllegalStateException("MAWAPay go-live configuration must be maintained before manual receipt capture"));
    }

    @Transactional(readOnly = true)
    public ManualReceiptCutoverConfigurationDto get() { return toDto(getRequired()); }

    @Transactional
    public ManualReceiptCutoverConfigurationDto save(ManualReceiptCutoverConfigurationDto dto) {
        if (dto.getMawaPayGoLiveDate() == null) throw new IllegalArgumentException("mawaPayGoLiveDate is required");
        ManualReceiptCutoverConfigurationEntity e = repository.findById(SINGLETON_ID).orElseGet(ManualReceiptCutoverConfigurationEntity::new);
        e.setId(SINGLETON_ID);
        e.setMawaPayGoLiveDate(dto.getMawaPayGoLiveDate());
        e.setLegacyCaptureCloseDate(dto.getLegacyCaptureCloseDate());
        e.setEmergencyReceiptRequiresProof(!Boolean.FALSE.equals(dto.getEmergencyReceiptRequiresProof()));
        e.setLegacyCaptureEnabled(!Boolean.FALSE.equals(dto.getLegacyCaptureEnabled()));
        e.setUpdatedAt(LocalDateTime.now());
        e.setUpdatedBy(dto.getUpdatedBy());
        return toDto(repository.save(e));
    }

    private ManualReceiptCutoverConfigurationDto toDto(ManualReceiptCutoverConfigurationEntity e) {
        ManualReceiptCutoverConfigurationDto d = new ManualReceiptCutoverConfigurationDto();
        d.setMawaPayGoLiveDate(e.getMawaPayGoLiveDate()); d.setLegacyCaptureCloseDate(e.getLegacyCaptureCloseDate());
        d.setEmergencyReceiptRequiresProof(e.getEmergencyReceiptRequiresProof()); d.setLegacyCaptureEnabled(e.getLegacyCaptureEnabled());
        d.setUpdatedBy(e.getUpdatedBy()); return d;
    }
}
