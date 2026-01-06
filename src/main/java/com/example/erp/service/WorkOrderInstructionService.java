package com.example.erp.service;

import com.example.erp.controller.dto.WorkOrderAgreementDetail;
import com.example.erp.controller.dto.WorkOrderAgreementSummary;
import com.example.erp.controller.dto.WorkOrderDetailSaveRequest;
import com.example.erp.controller.dto.WorkOrderDispatchRequest;
import com.example.erp.domain.Code;
import com.example.erp.domain.CodeId;
import com.example.erp.domain.ProductionAgreement;
import com.example.erp.domain.WorkOrder;
import com.example.erp.repository.CodeRepository;
import com.example.erp.repository.ProductionAgreementRepository;
import com.example.erp.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class WorkOrderInstructionService {

    private final ProductionAgreementRepository productionAgreementRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CodeRepository codeRepository;

    private final Map<String, WorkOrderDetailState> detailState = new ConcurrentHashMap<>();
    private final Set<String> dispatchedKeys = ConcurrentHashMap.newKeySet();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public WorkOrderInstructionService(ProductionAgreementRepository productionAgreementRepository,
            WorkOrderRepository workOrderRepository, CodeRepository codeRepository) {
        this.productionAgreementRepository = productionAgreementRepository;
        this.workOrderRepository = workOrderRepository;
        this.codeRepository = codeRepository;
    }

    public Map<String, List<String>> findFilters(String styleNo) {
        if (!StringUtils.hasText(styleNo)) {
            return Map.of("colors", Collections.emptyList(), "agreementCodes", Collections.emptyList());
        }
        List<ProductionAgreement> agreements = productionAgreementRepository
                .findByStyleNoOrderByAgreementCodeAscColorAscSizeAsc(styleNo.trim());
        List<String> colors = agreements.stream().map(ProductionAgreement::getColor).filter(StringUtils::hasText)
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<String> agreementCodes = agreements.stream().map(ProductionAgreement::getAgreementCode)
                .filter(StringUtils::hasText).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        return Map.of("colors", colors, "agreementCodes", agreementCodes);
    }

    public List<WorkOrderAgreementSummary> findAgreementSummaries(String styleNo) {
        if (!StringUtils.hasText(styleNo)) {
            return Collections.emptyList();
        }
        List<ProductionAgreement> agreements = productionAgreementRepository
                .findByStyleNoOrderByAgreementCodeAscColorAscSizeAsc(styleNo.trim());
        if (agreements.isEmpty()) {
            return Collections.emptyList();
        }

        Map<AgreementKey, Integer> aggregated = new LinkedHashMap<>();
        agreements.forEach(item -> {
            AgreementKey key = new AgreementKey(valueOf(item.getStyleNo()), valueOf(item.getAgreementCode()),
                    valueOf(item.getColor()));
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            aggregated.put(key, aggregated.getOrDefault(key, 0) + quantity);
        });

        return aggregated.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator
                        .comparing(AgreementKey::agreementCode, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AgreementKey::color, String.CASE_INSENSITIVE_ORDER)))
                .map(entry -> toSummary(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    public List<WorkOrderAgreementDetail> findAgreementDetails(String styleNo, String color, String agreementCode) {
        List<WorkOrderAgreementSummary> summaries = findAgreementSummaries(styleNo);
        if (summaries.isEmpty()) {
            return Collections.emptyList();
        }
        return summaries.stream()
                .filter(summary -> matches(color, summary.getColor()) && matches(agreementCode, summary.getAgreementCode()))
                .map(this::toDetail).toList();
    }

    public boolean dispatch(WorkOrderDispatchRequest request) {
        if (request == null || !StringUtils.hasText(request.getStyleNo()) || !StringUtils.hasText(request.getAgreementCode())
                || !StringUtils.hasText(request.getColor())) {
            return false;
        }
        String key = buildKey(request.getStyleNo(), request.getAgreementCode(), request.getColor());
        if (dispatchedKeys.contains(key)) {
            return true;
        }

        WorkOrder entity = new WorkOrder();
        entity.setWorkOrderId(UUID.randomUUID().toString());
        entity.setStyleNo(request.getStyleNo().trim());
        entity.setAgreementCode(request.getAgreementCode().trim());
        entity.setColor(request.getColor().trim());
        entity.setStatus("DISPATCHED");
        workOrderRepository.save(entity);

        dispatchedKeys.add(key);
        return true;
    }

    public int saveDetails(List<WorkOrderDetailSaveRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            return 0;
        }
        int saved = 0;
        for (WorkOrderDetailSaveRequest request : requests) {
            if (request == null || !StringUtils.hasText(request.getStyleNo())
                    || !StringUtils.hasText(request.getAgreementCode()) || !StringUtils.hasText(request.getColor())) {
                continue;
            }
            String key = buildKey(request.getStyleNo(), request.getAgreementCode(), request.getColor());
            WorkOrderDetailState state = detailState.computeIfAbsent(key, k -> new WorkOrderDetailState());

            state.setFactoryCode(trimToNull(request.getFactoryCode()));
            state.setFactoryName(resolveName("FACTORY", request.getFactoryCode(), request.getFactoryName()));

            state.setDeliveryPlaceCode(trimToNull(request.getDeliveryPlaceCode()));
            state.setDeliveryPlaceName(resolveName("WAREHOUSE", request.getDeliveryPlaceCode(),
                    request.getDeliveryPlaceName()));

            state.setDueDate(parseDate(request.getDueDate()));
            saved++;
        }
        return saved;
    }

    private WorkOrderAgreementSummary toSummary(AgreementKey key, Integer quantity) {
        return new WorkOrderAgreementSummary(key.styleNo(), key.agreementCode(), key.color(), quantity, "-", dispatchedKeys.contains(key.key()));
    }

    private WorkOrderAgreementDetail toDetail(WorkOrderAgreementSummary summary) {
        WorkOrderDetailState state = detailState.get(buildKey(summary.getStyleNo(), summary.getAgreementCode(), summary.getColor()));
        String agreementLabel = summary.getAgreementCode();
        return new WorkOrderAgreementDetail(summary.getStyleNo(), summary.getAgreementCode(), agreementLabel,
                summary.getColor(), summary.getQuantity(), summary.getProductionSite(),
                state != null ? state.getFactoryCode() : null,
                state != null ? state.getFactoryName() : null,
                state != null ? state.getDueDate() : null,
                state != null ? state.getDeliveryPlaceCode() : null,
                state != null ? state.getDeliveryPlaceName() : null);
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveName(String codeType, String code, String fallbackName) {
        if (!StringUtils.hasText(code)) {
            return trimToNull(fallbackName);
        }
        Optional<Code> found = codeRepository.findByIdAndDeletedFalse(new CodeId(codeType, code.trim()));
        return found.map(Code::getCodeName).orElseGet(() -> trimToNull(fallbackName));
    }

    private boolean matches(String expected, String actual) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return Objects.equals(expected.trim().toLowerCase(Locale.ROOT),
                actual != null ? actual.trim().toLowerCase(Locale.ROOT) : null);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildKey(String styleNo, String agreementCode, String color) {
        return String.join("|", valueOf(styleNo), valueOf(agreementCode), valueOf(color));
    }

    private String valueOf(String value) {
        return value != null ? value.trim() : "";
    }

    private record AgreementKey(String styleNo, String agreementCode, String color) {
        String key() {
            return String.join("|", styleNo, agreementCode, color);
        }
    }

    private static class WorkOrderDetailState {
        private String factoryCode;
        private String factoryName;
        private LocalDate dueDate;
        private String deliveryPlaceCode;
        private String deliveryPlaceName;

        public String getFactoryCode() {
            return factoryCode;
        }

        public void setFactoryCode(String factoryCode) {
            this.factoryCode = factoryCode;
        }

        public String getFactoryName() {
            return factoryName;
        }

        public void setFactoryName(String factoryName) {
            this.factoryName = factoryName;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
        }

        public String getDeliveryPlaceCode() {
            return deliveryPlaceCode;
        }

        public void setDeliveryPlaceCode(String deliveryPlaceCode) {
            this.deliveryPlaceCode = deliveryPlaceCode;
        }

        public String getDeliveryPlaceName() {
            return deliveryPlaceName;
        }

        public void setDeliveryPlaceName(String deliveryPlaceName) {
            this.deliveryPlaceName = deliveryPlaceName;
        }
    }
}
