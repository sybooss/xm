package com.user.returnsassistant.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.user.returnsassistant.mapper.AfterSaleProcessLogMapper;
import com.user.returnsassistant.mapper.ProductIssueAlertMapper;
import com.user.returnsassistant.pojo.AfterSaleProcessLog;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.ProductIssueAlert;
import com.user.returnsassistant.pojo.ProductIssueInsightSearch;
import com.user.returnsassistant.pojo.ProductIssueInsightSummary;
import com.user.returnsassistant.pojo.ProductIssueRefreshRequest;
import com.user.returnsassistant.pojo.ProductIssueRefreshResult;
import com.user.returnsassistant.pojo.ProductIssueSourceItem;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.ProductIssueInsightService;
import com.user.returnsassistant.utils.NoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProductIssueInsightServiceImpl implements ProductIssueInsightService {
    private static final List<String> KEYWORDS = List.of(
            "无声", "单耳", "断连", "降噪", "破音", "续航", "充电",
            "连击", "失灵", "灯光", "轴体", "蓝牙", "按键",
            "表带", "定位", "屏幕", "心率",
            "破损", "少件", "丢件", "延迟", "外包装", "漏发",
            "故障", "质量问题", "无法使用", "异响", "发热"
    );

    @Autowired
    private ProductIssueAlertMapper alertMapper;
    @Autowired
    private AfterSaleProcessLogMapper processLogMapper;

    @Transactional
    @Override
    public ProductIssueRefreshResult refresh(ProductIssueRefreshRequest request, UserAccount admin) {
        int days = normalizeDays(request == null ? null : request.getDays());
        List<ProductIssueSourceItem> sourceItems = alertMapper.listSourceItems(days);
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (ProductIssueSourceItem item : sourceItems) {
            if (!hasText(item.getProductName())) {
                continue;
            }
            for (String keyword : extractKeywords(item)) {
                String key = item.getProductName() + "\n" + keyword;
                buckets.computeIfAbsent(key, ignored -> new Bucket(item.getProductName(), keyword)).add(item);
            }
        }

        List<ProductIssueAlert> refreshed = new ArrayList<>();
        for (Bucket bucket : buckets.values()) {
            if (bucket.applicationIds.size() < 2 && bucket.lowRatingCount == 0 && bucket.ticketCount == 0) {
                continue;
            }
            ProductIssueAlert alert = buildAlert(bucket, days);
            ProductIssueAlert existing = alertMapper.getOpenByUniqueKey(alert.getProductName(), alert.getIssueKeyword(), days);
            boolean shouldWriteLog = existing == null
                    || !safeEquals(existing.getAlertLevel(), alert.getAlertLevel())
                    || !safeEquals(existing.getTrendScore(), alert.getTrendScore());
            if (existing == null) {
                alert.setAlertNo(NoUtils.productIssueAlertNo());
                alertMapper.insert(alert);
            } else {
                alert.setId(existing.getId());
                alert.setAlertNo(existing.getAlertNo());
                alertMapper.update(alert);
            }
            if (shouldWriteLog) {
                writeSampleLogs(alert, bucket, admin);
            }
            refreshed.add(hydrate(alert));
        }

        ProductIssueRefreshResult result = new ProductIssueRefreshResult();
        result.setRefreshedCount(refreshed.size());
        result.setHighCount((int) refreshed.stream().filter(item -> "HIGH".equals(item.getAlertLevel())).count());
        result.setMediumCount((int) refreshed.stream().filter(item -> "MEDIUM".equals(item.getAlertLevel())).count());
        result.setTopAlert(refreshed.stream()
                .max(Comparator.comparing(ProductIssueAlert::getTrendScore)
                        .thenComparing(ProductIssueAlert::getIssueCount))
                .orElseGet(() -> hydrate(alertMapper.getTop(days, "OPEN"))));
        return result;
    }

    @Override
    public PageResult<ProductIssueAlert> page(ProductIssueInsightSearch search) {
        if (search == null) {
            search = new ProductIssueInsightSearch();
        }
        search.setDays(normalizeDays(search.getDays()));
        if (!hasText(search.getStatus())) {
            search.setStatus("OPEN");
        }
        PageHelper.startPage(search.getPage(), search.getPageSize());
        Page<ProductIssueAlert> page = (Page<ProductIssueAlert>) alertMapper.page(search);
        return new PageResult<>(page.getTotal(), page.getResult().stream().map(this::hydrate).toList());
    }

    @Override
    public ProductIssueInsightSummary summary(Integer daysValue) {
        int days = normalizeDays(daysValue);
        ProductIssueInsightSummary summary = new ProductIssueInsightSummary();
        summary.setDays(days);
        summary.setOpenCount(alertMapper.countByLevel(days, "OPEN", null));
        summary.setHighCount(alertMapper.countByLevel(days, "OPEN", "HIGH"));
        summary.setMediumCount(alertMapper.countByLevel(days, "OPEN", "MEDIUM"));
        summary.setProductCount(alertMapper.countDistinctProducts(days, "OPEN"));
        summary.setSampleCount(alertMapper.sumApplications(days, "OPEN"));
        summary.setTopAlert(hydrate(alertMapper.getTop(days, "OPEN")));
        return summary;
    }

    @Override
    public List<ProductIssueAlert> listOpenByProduct(String productName, Integer daysValue) {
        if (!hasText(productName)) {
            return List.of();
        }
        int days = normalizeDays(daysValue);
        return alertMapper.listOpenByProduct(productName, days).stream().map(this::hydrate).toList();
    }

    private ProductIssueAlert buildAlert(Bucket bucket, int days) {
        ProductIssueAlert alert = new ProductIssueAlert();
        alert.setProductName(bucket.productName);
        alert.setIssueKeyword(bucket.issueKeyword);
        alert.setIssueCount(bucket.issueCount);
        alert.setApplicationCount(bucket.applicationIds.size());
        alert.setTicketCount(bucket.ticketCount);
        alert.setLowRatingCount(bucket.lowRatingCount);
        alert.setRefundAmount(bucket.refundAmount);
        alert.setTimeWindowDays(days);
        alert.setTrendScore(score(bucket, days));
        alert.setAlertLevel(level(alert.getTrendScore()));
        alert.setSampleApplicationIds(joinIds(bucket.applicationIds));
        alert.setSampleReasons(String.join("；", bucket.sampleReasons.stream().limit(5).toList()));
        alert.setSuggestedAction(suggest(alert));
        alert.setStatus("OPEN");
        return alert;
    }

    private int score(Bucket bucket, int days) {
        int score = 0;
        score += Math.min(48, bucket.applicationIds.size() * 12);
        score += Math.min(30, bucket.ticketCount * 10);
        score += Math.min(24, bucket.lowRatingCount * 8);
        score += Math.min(30, bucket.complaintCount * 10);
        if (bucket.refundAmount.compareTo(new BigDecimal("1000.00")) >= 0) {
            score += 15;
        } else if (bucket.refundAmount.compareTo(new BigDecimal("500.00")) >= 0) {
            score += 8;
        }
        if (days <= 7 && bucket.applicationIds.size() >= 3) {
            score += 15;
        }
        return Math.min(100, score);
    }

    private String level(int score) {
        if (score >= 65) {
            return "HIGH";
        }
        if (score >= 35) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String suggest(ProductIssueAlert alert) {
        String prefix = "【" + alert.getProductName() + "】近期集中出现“" + alert.getIssueKeyword() + "”问题，";
        if ("HIGH".equals(alert.getAlertLevel())) {
            return prefix + "建议运营立即检查批次、通知仓库复核出库质检，并对相关售后暂停直接退款，先补齐凭证后人工审核。";
        }
        if ("MEDIUM".equals(alert.getAlertLevel())) {
            return prefix + "建议客服重点关注同类问题，补充 FAQ 和排查话术，必要时抽查样本售后单。";
        }
        return prefix + "建议继续观察，并在后续售后处理中记录更多问题细节。";
    }

    private Set<String> extractKeywords(ProductIssueSourceItem item) {
        String text = joinText(item.getReasonText(), item.getCustomerIssue(), item.getReviewTags(), item.getReviewComment());
        String normalized = text.toLowerCase(Locale.ROOT);
        Set<String> result = new LinkedHashSet<>();
        for (String keyword : KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                result.add(keyword);
            }
        }
        if (result.isEmpty() && ("COMPLAINT".equals(item.getServiceType()) || (item.getRating() != null && item.getRating() <= 3))) {
            result.add("体验投诉");
        }
        if (result.isEmpty() && ("RETURN".equals(item.getServiceType()) || "REFUND".equals(item.getServiceType()))) {
            result.add("退货集中");
        }
        return result;
    }

    private ProductIssueAlert hydrate(ProductIssueAlert alert) {
        if (alert == null) {
            return null;
        }
        alert.setSampleApplicationIdList(splitIds(alert.getSampleApplicationIds()));
        alert.setSampleReasonList(splitReasons(alert.getSampleReasons()));
        return alert;
    }

    private void writeSampleLogs(ProductIssueAlert alert, Bucket bucket, UserAccount admin) {
        if (!"HIGH".equals(alert.getAlertLevel()) && !"MEDIUM".equals(alert.getAlertLevel())) {
            return;
        }
        for (Long applicationId : bucket.applicationIds.stream().limit(2).toList()) {
            AfterSaleProcessLog log = new AfterSaleProcessLog();
            log.setApplicationId(applicationId);
            log.setOperatorId(admin == null ? null : admin.getId());
            log.setOperatorName(admin == null ? "系统预警" : admin.getDisplayName());
            log.setOperatorRole(admin == null ? "SYSTEM" : admin.getRole());
            log.setAction("PRODUCT_ISSUE_ALERT");
            log.setRemark("商品质量预警 " + alert.getAlertNo()
                    + "：" + alert.getProductName()
                    + " / " + alert.getIssueKeyword()
                    + "，等级 " + alert.getAlertLevel()
                    + "，趋势分 " + alert.getTrendScore());
            processLogMapper.insert(log);
        }
    }

    private List<Long> splitIds(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String item : text.split(",")) {
            try {
                result.add(Long.parseLong(item.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private List<String> splitReasons(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : text.split("；")) {
            if (hasText(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private String joinIds(Set<Long> ids) {
        return String.join(",", ids.stream().limit(8).map(String::valueOf).toList());
    }

    private String joinText(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (hasText(part)) {
                builder.append(part).append(' ');
            }
        }
        return builder.toString();
    }

    private int normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return 7;
        }
        return Math.min(days, 90);
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private static class Bucket {
        private final String productName;
        private final String issueKeyword;
        private final Set<Long> applicationIds = new LinkedHashSet<>();
        private final Set<String> sampleReasons = new LinkedHashSet<>();
        private int issueCount;
        private int ticketCount;
        private int lowRatingCount;
        private int complaintCount;
        private BigDecimal refundAmount = BigDecimal.ZERO;

        private Bucket(String productName, String issueKeyword) {
            this.productName = productName;
            this.issueKeyword = issueKeyword;
        }

        private void add(ProductIssueSourceItem item) {
            issueCount++;
            if (item.getApplicationId() != null) {
                applicationIds.add(item.getApplicationId());
            }
            if (item.getCustomerIssue() != null && !item.getCustomerIssue().isBlank()) {
                ticketCount++;
            }
            if (item.getRating() != null && item.getRating() <= 3) {
                lowRatingCount++;
            }
            if ("COMPLAINT".equals(item.getServiceType())) {
                complaintCount++;
            }
            if (item.getRefundAmount() != null) {
                refundAmount = refundAmount.add(item.getRefundAmount());
            }
            String reason = firstText(item.getReasonText(), item.getCustomerIssue(), item.getReviewComment());
            if (reason != null) {
                sampleReasons.add(trim(reason, 120));
            }
        }

        private String firstText(String... parts) {
            for (String part : parts) {
                if (part != null && !part.isBlank()) {
                    return part.trim();
                }
            }
            return null;
        }

        private String trim(String text, int maxLength) {
            if (text == null || text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength);
        }
    }
}
