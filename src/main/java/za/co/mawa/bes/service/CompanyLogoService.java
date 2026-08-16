package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.co.mawa.bes.entity.v2.company.CompanyLogoEntity;
import za.co.mawa.bes.repository.v2.company.CompanyLogoRepository;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CompanyLogoService {
    public static final String ACTIVE_LOGO_ID = "ACTIVE";
    public static final int LOGO_FRAME_WIDTH_PX = 400;
    public static final int LOGO_FRAME_HEIGHT_PX = 400;
    public static final int MAX_WIDTH_PX = LOGO_FRAME_WIDTH_PX;
    public static final int MAX_HEIGHT_PX = LOGO_FRAME_HEIGHT_PX;
    public static final float PDF_WIDTH_PT = 72f;
    public static final float PDF_HEIGHT_PT = 72f;
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
        byte[] originalBytes = file.getBytes();
        if (originalBytes.length > MAX_SIZE_BYTES) throw new IllegalArgumentException("Company logo must not exceed 300KB");
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (source == null) throw new IllegalArgumentException("Company logo file is not a valid image");

        boolean png = contentType.equals("image/png");
        byte[] bytes = fitInsideSquare(source, png);
        String storedContentType = png ? "image/png" : "image/jpeg";

        CompanyLogoEntity logo = CompanyLogoEntity.builder()
                .id(ACTIVE_LOGO_ID)
                .fileName(file.getOriginalFilename() == null ? "company-logo" : file.getOriginalFilename())
                .contentType(storedContentType)
                .widthPx(LOGO_FRAME_WIDTH_PX)
                .heightPx(LOGO_FRAME_HEIGHT_PX)
                .sizeBytes((long) bytes.length)
                .content(bytes)
                .uploadedAt(LocalDateTime.now())
                .uploadedBy(currentUser)
                .build();
        return companyLogoRepository.save(logo);
    }

    private byte[] fitInsideSquare(BufferedImage source, boolean preserveTransparency) throws IOException {
        int canvasType = preserveTransparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage canvas = new BufferedImage(LOGO_FRAME_WIDTH_PX, LOGO_FRAME_HEIGHT_PX, canvasType);
        Graphics2D graphics = canvas.createGraphics();
        try {
            if (preserveTransparency) {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, LOGO_FRAME_WIDTH_PX, LOGO_FRAME_HEIGHT_PX);
                graphics.setComposite(AlphaComposite.SrcOver);
            } else {
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillRect(0, 0, LOGO_FRAME_WIDTH_PX, LOGO_FRAME_HEIGHT_PX);
            }

            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double scale = Math.min(1d, Math.min(
                    (double) LOGO_FRAME_WIDTH_PX / source.getWidth(),
                    (double) LOGO_FRAME_HEIGHT_PX / source.getHeight()));
            int targetWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
            int x = (LOGO_FRAME_WIDTH_PX - targetWidth) / 2;
            int y = (LOGO_FRAME_HEIGHT_PX - targetHeight) / 2;
            graphics.drawImage(source, x, y, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(canvas, preserveTransparency ? "png" : "jpg", output);
        return output.toByteArray();
    }

    public Optional<CompanyLogoEntity> getActiveLogo() {
        return companyLogoRepository.findById(ACTIVE_LOGO_ID);
    }

    public Map<String, Object> metadata() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxWidthPx", MAX_WIDTH_PX);
        result.put("maxHeightPx", MAX_HEIGHT_PX);
        // Kept for older clients; company logos are normalized into this exact square frame.
        result.put("requiredWidthPx", MAX_WIDTH_PX);
        result.put("requiredHeightPx", MAX_HEIGHT_PX);
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
