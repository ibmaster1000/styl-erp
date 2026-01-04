package com.example.erp.service;

import com.example.erp.controller.dto.WorkOrderAttachmentView;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WorkOrderAttachmentService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public WorkOrderAttachmentService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<AttachmentType, List<WorkOrderAttachmentView>> fetchGroupedAttachments() {
        if (hasTable("work_order_attachments_v2")) {
            return fetchFromV2();
        }
        if (hasTable("work_order_attachments")) {
            return fetchFromLegacy();
        }
        return Collections.emptyMap();
    }

    private Map<AttachmentType, List<WorkOrderAttachmentView>> fetchFromV2() {
        String sql = """
                select work_order_id, attachment_type, file_path
                from work_order_attachments_v2
                """;
        Map<AttachmentType, List<WorkOrderAttachmentView>> grouped = new EnumMap<>(AttachmentType.class);
        jdbcTemplate.query(sql, rs -> {
            String type = normalize(rs.getString("attachment_type"));
            String workOrderId = rs.getString("work_order_id");
            String path = rs.getString("file_path");
            if (type == null || path == null) {
                return;
            }
            AttachmentType attachmentType = AttachmentType.from(type);
            if (attachmentType == null) {
                return;
            }
            grouped.computeIfAbsent(attachmentType, k -> new ArrayList<>())
                    .add(new WorkOrderAttachmentView(workOrderId, attachmentType.name(), path));
        });
        return grouped;
    }

    private Map<AttachmentType, List<WorkOrderAttachmentView>> fetchFromLegacy() {
        boolean hasIllustration = hasColumn("work_order_attachments", "illustration_path");
        boolean hasSewing = hasColumn("work_order_attachments", "sewing_path");
        boolean hasWorkOrderId = hasColumn("work_order_attachments", "work_order_id");
        if (!hasIllustration && !hasSewing) {
            return Collections.emptyMap();
        }

        String sql = "select " +
                (hasWorkOrderId ? "work_order_id" : "null as work_order_id") + ", " +
                (hasIllustration ? "illustration_path" : "null as illustration_path") + ", " +
                (hasSewing ? "sewing_path" : "null as sewing_path") +
                " from work_order_attachments";

        Map<AttachmentType, List<WorkOrderAttachmentView>> grouped = new EnumMap<>(AttachmentType.class);
        jdbcTemplate.query(sql, rs -> {
            String workOrderId = rs.getString("work_order_id");
            String illustrationPath = hasIllustration ? rs.getString("illustration_path") : null;
            String sewingPath = hasSewing ? rs.getString("sewing_path") : null;
            if (illustrationPath != null && !illustrationPath.isBlank()) {
                grouped.computeIfAbsent(AttachmentType.ILLUSTRATION, k -> new ArrayList<>())
                        .add(new WorkOrderAttachmentView(workOrderId, AttachmentType.ILLUSTRATION.name(), illustrationPath));
            }
            if (sewingPath != null && !sewingPath.isBlank()) {
                grouped.computeIfAbsent(AttachmentType.SEWING, k -> new ArrayList<>())
                        .add(new WorkOrderAttachmentView(workOrderId, AttachmentType.SEWING.name(), sewingPath));
            }
        });
        return grouped;
    }

    private boolean hasTable(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.tables
                    where upper(table_name) = upper(:tableName)
                    """, new MapSqlParameterSource("tableName", tableName), Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.columns
                    where upper(table_name) = upper(:tableName)
                      and upper(column_name) = upper(:columnName)
                    """, new MapSqlParameterSource()
                    .addValue("tableName", tableName)
                    .addValue("columnName", columnName), Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public enum AttachmentType {
        ILLUSTRATION,
        SEWING;

        static AttachmentType from(String value) {
            if (value == null) return null;
            String upper = value.trim().toUpperCase(Locale.ROOT);
            for (AttachmentType type : values()) {
                if (type.name().equals(upper)) {
                    return type;
                }
            }
            return null;
        }
    }
}