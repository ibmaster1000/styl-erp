package com.example.erp.service;

import com.example.erp.repository.WorkOrderRepository;
import com.example.erp.repository.WorkOrderRepository.AttachmentRow;
import com.example.erp.repository.WorkOrderRepository.SizeSpecRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    public WorkOrderDetail loadByStyleCode(String styleCode) {
        if (!StringUtils.hasText(styleCode)) {
            return WorkOrderDetail.empty();
        }

        String trimmed = styleCode.trim();
        Optional<Long> stylesId = workOrderRepository.findStylesIdByStyleCode(trimmed);
        if (stylesId.isEmpty()) {
            return WorkOrderDetail.notFound(trimmed);
        }

        List<String> sizeCodes = workOrderRepository.findSizeCodes(stylesId.get());
        LocalDateTime now = LocalDateTime.now();
        Long orderId = workOrderRepository.findOrderIdByStylesId(stylesId.get())
                .orElseGet(() -> workOrderRepository.insertWorkOrder(stylesId.get(), now));

        Map<String, SizeSpec> sizeSpecs = Collections.emptyMap();
        AttachmentPaths attachments = AttachmentPaths.empty();
        if (orderId != null) {
            Map<String, SizeSpecRow> rows = workOrderRepository.findSizeSpecs(orderId);
            sizeSpecs = rows.values().stream()
                    .collect(Collectors.toMap(SizeSpecRow::sizeCode, this::mapToSizeSpec,
                            (existing, replacement) -> existing, LinkedHashMap::new));
            attachments = workOrderRepository.findAttachment(orderId)
                    .map(row -> new AttachmentPaths(row.illustrationPath(), row.sewingPath()))
                    .orElse(AttachmentPaths.empty());
        }

        return new WorkOrderDetail(stylesId.get(), orderId, sizeCodes, sizeSpecs, attachments, false, trimmed);
    }

    @Transactional
    public SaveResult saveSizeSpecs(SaveRequest request) {
        if (request == null) {
            return SaveResult.failure("품번을 입력하세요.");
        }
        Optional<Long> stylesId = Optional.empty();
        Long orderId = request.orderId();
        if (orderId != null) {
            stylesId = workOrderRepository.findStylesIdByOrderId(orderId);
        }
        if (stylesId.isEmpty() && StringUtils.hasText(request.styleCode())) {
            stylesId = workOrderRepository.findStylesIdByStyleCode(request.styleCode().trim());
        }
        if (stylesId.isEmpty()) {
            return SaveResult.failure("존재하지 않는 품번입니다.");
        }

        List<String> sizeCodes = workOrderRepository.findSizeCodes(stylesId.get());
        if (sizeCodes.isEmpty()) {
            return SaveResult.failure("등록된 사이즈가 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (orderId == null) {
            orderId = workOrderRepository.findOrderIdByStylesId(stylesId.get())
                    .orElseGet(() -> workOrderRepository.insertWorkOrder(stylesId.get(), now));
        }
        if (orderId == null) {
            return SaveResult.failure("작업지시 저장에 실패했습니다.");
        }

        Map<String, SizeSpecRow> requestSpecs = new LinkedHashMap<>();
        if (request.specs() != null) {
            for (SizeSpecInput input : request.specs()) {
                if (input == null || !StringUtils.hasText(input.sizeCode())) {
                    continue;
                }
                requestSpecs.put(input.sizeCode().trim(), input.toRow());
            }
        }

        for (String sizeCode : sizeCodes) {
            SizeSpecRow spec = requestSpecs.getOrDefault(sizeCode, SizeSpecRow.empty(sizeCode));
            if (workOrderRepository.existsSizeSpec(orderId, sizeCode)) {
                workOrderRepository.updateSizeSpec(orderId, sizeCode, spec, now);
            } else {
                workOrderRepository.insertSizeSpec(orderId, sizeCode, spec, now);
            }
        }
        workOrderRepository.updateWorkOrderUpdatedAt(orderId, now);

        return SaveResult.success(orderId);
    }

    @Transactional
    public boolean deleteAttachment(Long orderId, AttachmentType type) {
        if (orderId == null || type == null) {
            return false;
        }
        workOrderRepository.clearAttachment(orderId, type.toRepositoryType());
        return true;
    }

    public UploadResult uploadAttachment(Long orderId, AttachmentType type, org.springframework.web.multipart.MultipartFile file) {
        if (orderId == null || type == null || file == null || file.isEmpty()) {
            return UploadResult.failure("파일을 선택하세요.");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String safeExtension = (extension == null || extension.isBlank()) ? "" : "." + extension.toLowerCase();
        String filename = type.name().toLowerCase() + "_" + System.currentTimeMillis() + safeExtension;
        java.nio.file.Path dir = java.nio.file.Paths.get("uploads", "work-orders", String.valueOf(orderId));
        java.nio.file.Path destination = dir.resolve(filename);
        try {
            java.nio.file.Files.createDirectories(dir);
            file.transferTo(destination);
        } catch (java.io.IOException e) {
            return UploadResult.failure("업로드에 실패했습니다.");
        }

        String path = "/uploads/work-orders/" + orderId + "/" + filename;
        workOrderRepository.upsertAttachment(orderId, type.toRepositoryType(), path, LocalDateTime.now());
        return UploadResult.success(path);
    }

    private SizeSpec mapToSizeSpec(SizeSpecRow row) {
        return new SizeSpec(row.totalLength(), row.waistWidth(), row.thighWidth(), row.hipWidth(),
                row.inseamLength());
    }

    public record WorkOrderDetail(Long stylesId, Long orderId, List<String> sizeCodes,
                                  Map<String, SizeSpec> sizeSpecs, AttachmentPaths attachments,
                                  boolean notFound, String styleCode) {
        public static WorkOrderDetail empty() {
            return new WorkOrderDetail(null, null, List.of(), Map.of(), AttachmentPaths.empty(), false, null);
        }

        public static WorkOrderDetail notFound(String styleCode) {
            return new WorkOrderDetail(null, null, List.of(), Map.of(), AttachmentPaths.empty(), true, styleCode);
        }
    }

    public record SizeSpec(BigDecimal totalLength, BigDecimal waistWidth, BigDecimal thighWidth, BigDecimal hipWidth,
                           BigDecimal inseamLength) {
    }

    public record AttachmentPaths(String illustrationPath, String sewingPath) {
        public static AttachmentPaths empty() {
            return new AttachmentPaths(null, null);
        }
    }

    public record SaveRequest(Long orderId, String styleCode, List<SizeSpecInput> specs) {
    }

    public record SizeSpecInput(String sizeCode, BigDecimal totalLength, BigDecimal waistWidth,
                                BigDecimal thighWidth, BigDecimal hipWidth, BigDecimal inseamLength) {
        public SizeSpecRow toRow() {
            return new SizeSpecRow(sizeCode, totalLength, waistWidth, thighWidth, hipWidth, inseamLength);
        }
    }

    public record SaveResult(boolean success, String message, Long orderId) {
        public static SaveResult success(Long orderId) {
            return new SaveResult(true, "저장되었습니다.", orderId);
        }

        public static SaveResult failure(String message) {
            return new SaveResult(false, message, null);
        }
    }

    public record UploadResult(boolean success, String message, String path) {
        public static UploadResult success(String path) {
            return new UploadResult(true, "업로드되었습니다.", path);
        }

        public static UploadResult failure(String message) {
            return new UploadResult(false, message, null);
        }
    }

    public enum AttachmentType {
        ILLUSTRATION,
        SEWING;

        public static AttachmentType from(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                return AttachmentType.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        WorkOrderRepository.AttachmentType toRepositoryType() {
            return WorkOrderRepository.AttachmentType.valueOf(name());
        }
    }
}
