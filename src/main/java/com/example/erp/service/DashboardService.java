package com.example.erp.service;

import com.example.erp.repository.DashboardQueryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final DashboardQueryRepository repository;

    public DashboardService(DashboardQueryRepository repository) {
        this.repository = repository;
    }

    public Map<String, Long> loadKpi() {
        Map<String, Long> kpi = new LinkedHashMap<>();
        kpi.put("workOrdersInProgress", repository.countProductionJobsNotDone());
        kpi.put("materialShortage", repository.countMaterialShortage());
        kpi.put("todayReceipts", repository.countTodayFgInbounds());
        kpi.put("todayShipments", repository.countTodayFgOutbounds());
        return kpi;
    }

    public Map<String, ProgressMetric> loadProgress() {
        Map<String, ProgressMetric> progress = new LinkedHashMap<>();

        long agreementTotal = repository.countProductionAgreementsTotal();
        progress.put("agreement", buildMetric(agreementTotal, agreementTotal));

        long jobsTotal = repository.countProductionJobsTotal();
        long jobsNotWait = repository.countProductionJobsNotWait();
        progress.put("workOrder", buildMetric(jobsNotWait, jobsTotal));

        long inboundTotal = repository.countFgInboundsTotal();
        progress.put("inbound", buildMetric(Math.min(inboundTotal, jobsTotal), jobsTotal));

        long outboundTotal = repository.countFgOutboundsTotal();
        progress.put("outbound", buildMetric(Math.min(outboundTotal, inboundTotal), inboundTotal));

        return progress;
    }

    public ChartData loadChartData() {
        Map<String, Long> statusCounts = repository.countProductionJobsByStatus();
        List<String> labels = new ArrayList<>(statusCounts.keySet());
        List<Long> values = new ArrayList<>(statusCounts.values());
        return new ChartData(labels, values);
    }

    private ProgressMetric buildMetric(long numerator, long denominator) {
        if (denominator <= 0) {
            return new ProgressMetric(0L, 0L, 0L);
        }
        long clampedNumerator = Math.min(numerator, denominator);
        long percent = Math.round((clampedNumerator * 100.0) / denominator);
        return new ProgressMetric(clampedNumerator, denominator, percent);
    }

    public record ProgressMetric(long numerator, long denominator, long percent) {
    }

    public record ChartData(List<String> labels, List<Long> values) {
    }
}
