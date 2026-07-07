package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.co.mawa.bes.entity.v2.company.CompanyLogoEntity;
import za.co.mawa.bes.repository.v2.company.CompanyLogoRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CompanyLogoService {
    public static final String ACTIVE_LOGO_ID = "ACTIVE";
    public static final int REQUIRED_WIDTH_PX = 600;
    public static final int REQUIRED_HEIGHT_PX = 180;
    public static final float PDF_WIDTH_PT = 160f;
    public static final float PDF_HEIGHT_PT = 48f;
    private static final long MAX_SIZE_BYTES = 300 * 1024;

    private final CompanyLogoRepository companyLogoRepository;

    public CompanyLogoService(CompanyLogoRepository companyLogoRepository) {
        this.companyLogoRepository = companyLogoRepository;
    }

    public CompanyLogoEntity upload(MultipartFile file, String currentUser) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Company logo file is required");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.equals("image/png") && !contentType.equals("image/jpeg") && !contentType.equals("image/jpg")) {
            throw new IllegalArgumentException("Company logo must be a PNG or JPG image");
        }
        byte[] bytes = file.getBytes();
        if (bytes.length > MAX_SIZE_BYTES) throw new IllegalArgumentException("Company logo must not exceed 300KB");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IllegalArgumentException("Company logo file is not a valid image");
        if (image.getWidth() != REQUIRED_WIDTH_PX || image.getHeight() != REQUIRED_HEIGHT_PX) {
            throw new IllegalArgumentException("Company logo must be exactly " + REQUIRED_WIDTH_PX + "x" + REQUIRED_HEIGHT_PX + " pixels");
        }
        CompanyLogoEntity logo = CompanyLogoEntity.builder()
                .id(ACTIVE_LOGO_ID)
                .fileName(file.getOriginalFilename() == null ? "company-logo" : file.getOriginalFilename())
                .contentType(contentType.equals("image/jpg") ? "image/jpeg" : contentType)
                .widthPx(image.getWidth())
                .heightPx(image.getHeight())
                .sizeBytes((long) bytes.length)
                .content(bytes)
                .uploadedAt(LocalDateTime.now())
                .uploadedBy(currentUser)
                .build();
        return companyLogoRepository.save(logo);
    }

    public Optional<CompanyLogoEntity> getActiveLogo() {
        return companyLogoRepository.findById(ACTIVE_LOGO_ID);
    }

    public Map<String, Object> metadata() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requiredWidthPx", REQUIRED_WIDTH_PX);
        result.put("requiredHeightPx", REQUIRED_HEIGHT_PX);
        result.put("pdfWidthPt", PDF_WIDTH_PT);
        result.put("pdfHeightPt", PDF_HEIGHT_PT);
        result.put("maxSizeBytes", MAX_SIZE_BYTES);
        getActiveLogo().ifPresent(logo -> {
            result.put("loaded", true);
            result.put("fileName", logo.getFileName());
            result.put("contentType", logo.getContentType());
            result.put("widthPx", logo.getWidthPx());
            result.put("heightPx", logo.getHeightPx());
            result.put("sizeBytes", logo.getSizeBytes());
            result.put("uploadedAt", logo.getUploadedAt());
        });
        result.putIfAbsent("loaded", false);
        return result;
    }
}
