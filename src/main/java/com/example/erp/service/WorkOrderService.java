package com.example.erp.service;

import com.example.erp.domain.Style;
import com.example.erp.repository.StyleRepository;
import com.example.erp.repository.WorkOrderRepository;
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
import java.util.stream.Collectors;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final StyleRepository styleRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository, StyleRepository styleRepository) {
        this.workOrderRepository = workOrderRepository;
        this.styleRepository = styleRepository;
    }

    public WorkOrderDetail loadByStyleCode(String styleCode) {
        if (!StringUtils.hasText(styleCode)) {
            return WorkOrderDetail.empty();
        }

        String trimmed = styleCode.trim();

        Style styles = styleRepository.findByStyleCode(trimmed).orElse(null);
        if (styles == null) {
            return WorkOrderDetail.notFound(trimmed);
        }

        final Long stylesId = styles.getStylesId();
        final List<String> sizeCodes = workOrderRepository.findSizeCodes(stylesId);

        // order_id 조회/생성 (람다(orElseGet) 제거 -> final 이슈 원천 차단)
        Long orderId = workOrderRepository.findOrderIdByStylesId(stylesId).orElse(null);
        if (orderId == null) {
            orderId = workOrderRepository.insertWorkOrder(stylesId, LocalDateTime.now());
        }

        Map<String, SizeSpec> sizeSpecs = Collections.emptyMap();
        AttachmentPaths attachments = AttachmentPaths.empty();

        if (orderId != null) {
            Map<String, SizeSpecRow> rows = workOrderRepository.findSizeSpecs(orderId);
            sizeSpecs = rows.values().stream()
                    .collect(Collectors.toMap(
                            SizeSpecRow::sizeCode,
                            this::mapToSizeSpec,
                            (existing, replacement) -> existing,
                            LinkedHashMap::new
                    ));

            attachments = workOrderRepository.findAttachment(orderId)
                    .map(row -> new AttachmentPaths(row.illustrationPath(), row.sewingPath()))
                    .orElse(AttachmentPaths.empty());
        }

        return new WorkOrderDetail(stylesId, orderId, sizeCodes, sizeSpecs, attachments, false, trimmed);
    }

    @Transactional
    public SaveResult saveSizeSpecs(SaveRequest request) {
        if (request == null) {
            return SaveResult.failure("품번을 입력하세요.");
        }

        // 1) stylesId를 '최종값'으로 확정 (재할당 변수 제거)
        final Long resolvedStylesId = resolveStylesId(request);
        if (resolvedStylesId == null) {
            return SaveResult.failure("존재하지 않는 품번입니다.");
        }

        // 2) size 목록 확인
        final List<String> sizeCodes = workOrderRepository.findSizeCodes(resolvedStylesId);
        if (sizeCodes.isEmpty()) {
            return SaveResult.failure("등록된 사이즈가 없습니다.");
        }

        // 3) orderId 확보 (조회 -> 없으면 생성). 람다(orElseGet) 제거
        LocalDateTime now = LocalDateTime.now();
        Long orderId = request.orderId();

        if (orderId != null) {
            // 방어: 넘어온 orderId가 실제로 이 stylesId의 order인지 확인하고 싶으면 여기서 검증 가능
            // (지금은 기존 로직 유지: orderId가 있으면 그대로 사용)
        } else {
            orderId = workOrderRepository.findOrderIdByStylesId(resolvedStylesId).orElse(null);
            if (orderId == null) {
                orderId = workOrderRepository.insertWorkOrder(resolvedStylesId, now);
            }
        }

        if (orderId == null) {
            return SaveResult.failure("작업지시 저장에 실패했습니다.");
        }

        // 4) 요청 spec 맵 구성
        Map<String, SizeSpecRow> requestSpecs = new LinkedHashMap<>();
        if (request.specs() != null) {
            for (SizeSpecInput input : request.specs()) {
                if (input == null || !StringUtils.hasText(input.sizeCode())) {
                    continue;
                }
                String code = input.sizeCode().trim();
                requestSpecs.put(code, input.toRow());
            }
        }

        // 5) 저장 (기존 로직 유지: row별 exists -> update/insert)
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

    /**
     * stylesId를 한 번에 "확정"해서 반환.
     * - orderId가 있으면 orderId -> stylesId 조회
     * - 없으면 styleCode로 styles 조회
     * 이 메서드 밖에서 stylesId를 재할당하지 않게 만들기 위한 분리.
     */
    private Long resolveStylesId(SaveRequest request) {
        // orderId 기반 우선
        if (request.orderId() != null) {
            Long fromOrder = workOrderRepository.findStylesIdByOrderId(request.orderId()).orElse(null);
            if (fromOrder != null) {
                return fromOrder;
            }
        }

        // styleCode 기반
        if (StringUtils.hasText(request.styleCode())) {
            String trimmedCode = request.styleCode().trim();
            Style styles = styleRepository.findByStyleCode(trimmedCode).orElse(null);
            if (styles != null) {
                return styles.getStylesId();
            }
        }

        return null;
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
        return new SizeSpec(
                row.totalLength(),
                row.waistWidth(),
                row.thighWidth(),
                row.hipWidth(),
                row.inseamLength()
        );
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
