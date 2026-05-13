package com.user.returnsassistant.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.mapper.ProductProfileMapper;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.ProductInsight;
import com.user.returnsassistant.pojo.ProductProfile;
import com.user.returnsassistant.service.AiService;
import com.user.returnsassistant.service.ProductInsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ProductInsightServiceImpl implements ProductInsightService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Autowired
    private DemoOrderMapper orderMapper;
    @Autowired
    private ProductProfileMapper profileMapper;
    @Autowired
    private AiService aiService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ProductInsight buildByOrderId(Long orderId, String userIssue, Boolean useAi) {
        DemoOrder order = orderMapper.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return build(order, userIssue, null, useAi);
    }

    @Override
    public ProductInsight buildByOrderNo(String orderNo, String userIssue, Boolean useAi) {
        if (!hasText(orderNo)) {
            throw new BusinessException("订单号不能为空");
        }
        DemoOrder order = orderMapper.getByOrderNo(orderNo.trim());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return build(order, userIssue, null, useAi);
    }

    @Override
    public ProductInsight build(DemoOrder order, String userIssue, String intentCode, Boolean useAi) {
        if (order == null) {
            return noOrderInsight(userIssue);
        }

        ProductProfile profile = profileMapper.findBestMatch(order.getProductName());
        ProductInsight insight = new ProductInsight();
        insight.setOrderNo(order.getOrderNo());
        insight.setProductName(order.getProductName());
        insight.setSkuName(order.getSkuName());
        insight.setMatchedConcerns(extractConcerns(userIssue, order.getProductName()));

        if (profile != null) {
            fillFromProfile(insight, profile);
        } else {
            fillFallback(insight, order.getProductName());
        }
        insight.setLocalSummary(buildLocalSummary(insight, userIssue, intentCode));

        boolean enabledAi = useAi != null && useAi;
        if (enabledAi) {
            AiService.AiResult result = aiService.generate(buildPrompt(insight, userIssue, intentCode));
            insight.setAiStatus(result.status());
            insight.setAiErrorMessage(result.errorMessage());
            if (result.used() && hasText(result.reply())) {
                insight.setAiSummary(result.reply().trim());
            }
        } else {
            insight.setAiStatus("SKIPPED");
            insight.setAiErrorMessage("本轮未启用 AI 产品顾问摘要");
        }
        return insight;
    }

    private ProductInsight noOrderInsight(String userIssue) {
        ProductInsight insight = new ProductInsight();
        insight.setHasProfile(false);
        insight.setMatchType("NO_ORDER");
        insight.setMatchedConcerns(extractConcerns(userIssue, ""));
        insight.setLocalSummary("暂未绑定订单，无法读取具体商品档案。请先绑定订单后查看产品参数、排查步骤和同类对比。");
        insight.setAiStatus("SKIPPED");
        return insight;
    }

    private void fillFromProfile(ProductInsight insight, ProductProfile profile) {
        insight.setHasProfile(true);
        insight.setMatchType(profile.getProductName().equals(insight.getProductName()) ? "PRODUCT_NAME" : "ALIAS");
        insight.setCategory(profile.getCategory());
        insight.setPositioning(profile.getPositioning());
        insight.setSpecs(parseSpecJson(profile.getSpecJson()));
        insight.setSellingPoints(splitText(profile.getSellingPoints()));
        insight.setUsageScenarios(splitText(profile.getUsageScenarios()));
        insight.setCommonIssues(splitText(profile.getCommonIssues()));
        insight.setTroubleshootingSteps(splitText(profile.getTroubleshootingSteps()));
        insight.setComparisonText(profile.getComparisonText());
        insight.setRetentionScript(profile.getRetentionScript());
        insight.setAfterSaleAdvice(profile.getAfterSaleAdvice());
    }

    private void fillFallback(ProductInsight insight, String productName) {
        String category = inferCategory(productName);
        insight.setHasProfile(false);
        insight.setMatchType("CATEGORY_FALLBACK");
        insight.setCategory(category);
        insight.setPositioning(categoryPositioning(category, productName));
        insight.setSpecs(Map.of());
        insight.setSellingPoints(List.of("暂未维护该商品的详细卖点，系统根据商品品类提供通用说明。"));
        insight.setUsageScenarios(categoryScenarios(category));
        insight.setCommonIssues(categoryIssues(category));
        insight.setTroubleshootingSteps(categorySteps(category));
        insight.setComparisonText("暂未维护该商品的同类对比，建议以商品详情页和平台售后规则为准。");
        insight.setRetentionScript("建议先完成基础排查；若仍不能解决，再根据订单状态和售后规则申请退换货。");
        insight.setAfterSaleAdvice(categoryAfterSaleAdvice(category));
    }

    private Map<String, Object> parseSpecJson(String specJson) {
        if (!hasText(specJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(specJson, MAP_TYPE);
        } catch (Exception e) {
            return Map.of("raw", specJson);
        }
    }

    private List<String> extractConcerns(String issueText, String productName) {
        String text = nullToEmpty(issueText).toLowerCase(Locale.ROOT);
        Map<String, List<String>> rules = new LinkedHashMap<>();
        rules.put("NOISE_CONTROL", List.of("降噪", "隔音", "太吵", "听得到外面"));
        rules.put("CONNECTION", List.of("断连", "连不上", "不稳定", "配对"));
        rules.put("BATTERY", List.of("续航", "没电", "电量", "耗电"));
        rules.put("CHARGING", List.of("充电", "充不进", "充电盒"));
        rules.put("SINGLE_SIDE_AUDIO", List.of("左耳", "右耳", "单耳", "没声音", "无声音"));
        rules.put("SOUND_QUALITY", List.of("音质", "破音", "杂音", "声音小"));
        rules.put("KEY_FAILURE", List.of("连击", "失灵", "按键", "轴体"));
        rules.put("LOCATION", List.of("定位", "gps", "轨迹"));
        rules.put("SCREEN", List.of("屏幕", "黑屏", "花屏", "触控"));
        rules.put("CAPACITY", List.of("容量", "虚标", "不耐用"));

        List<String> concerns = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : rules.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                    concerns.add(entry.getKey());
                    break;
                }
            }
        }
        if (concerns.isEmpty()) {
            concerns.add("GENERAL_PRODUCT_USAGE");
        }
        return concerns;
    }

    private String buildLocalSummary(ProductInsight insight, String userIssue, String intentCode) {
        String concernText = insight.getMatchedConcerns() == null || insight.getMatchedConcerns().isEmpty()
                ? "通用使用体验"
                : String.join("、", insight.getMatchedConcerns());
        String profileText = Boolean.TRUE.equals(insight.getHasProfile()) ? "已命中商品档案" : "使用品类兜底档案";
        String advice = hasText(insight.getAfterSaleAdvice()) ? insight.getAfterSaleAdvice() : "建议结合订单状态和售后规则继续判断。";
        return profileText + "，用户关注点为 " + concernText + "。"
                + "系统会先解释商品定位和可能原因，再给出排查步骤；最终售后处理仍以订单状态和平台规则为准。"
                + advice;
    }

    private String buildPrompt(ProductInsight insight, String userIssue, String intentCode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是电商售后系统中的产品智能顾问。请基于商品档案和用户问题，输出一段 120 字以内的中文产品售后说明。\n");
        prompt.append("要求：不要夸大商品能力，不要承诺退款或驳回；如果是体验问题，先给排查建议；如果是质量问题，提示补充凭证并建议换货/检测。\n\n");
        prompt.append("用户问题：").append(nullToEmpty(userIssue)).append("\n");
        prompt.append("意图：").append(nullToEmpty(intentCode)).append("\n");
        prompt.append("商品：").append(nullToEmpty(insight.getProductName())).append("\n");
        prompt.append("商品定位：").append(nullToEmpty(insight.getPositioning())).append("\n");
        prompt.append("核心参数：").append(insight.getSpecs()).append("\n");
        prompt.append("用户关注点：").append(insight.getMatchedConcerns()).append("\n");
        prompt.append("常见问题：").append(insight.getCommonIssues()).append("\n");
        prompt.append("排查步骤：").append(insight.getTroubleshootingSteps()).append("\n");
        prompt.append("同类对比：").append(nullToEmpty(insight.getComparisonText())).append("\n");
        prompt.append("售后建议：").append(nullToEmpty(insight.getAfterSaleAdvice())).append("\n");
        prompt.append("请只输出顾问说明，不要输出分析过程。");
        return prompt.toString();
    }

    private String inferCategory(String productName) {
        String text = nullToEmpty(productName);
        if (containsAny(text, "耳机", "蓝牙")) {
            return "EARPHONE";
        }
        if (containsAny(text, "键盘", "轴")) {
            return "KEYBOARD";
        }
        if (containsAny(text, "手表", "腕表")) {
            return "WATCH";
        }
        if (containsAny(text, "电源", "充电宝")) {
            return "POWER_BANK";
        }
        return "GENERAL";
    }

    private String categoryPositioning(String category, String productName) {
        return switch (category) {
            case "EARPHONE" -> "面向日常通勤、学习和通话场景的无线耳机";
            case "KEYBOARD" -> "面向办公输入和桌面外设场景的键盘产品";
            case "WATCH" -> "面向运动记录、健康监测和日常提醒的智能穿戴产品";
            case "POWER_BANK" -> "面向外出应急补电的移动电源产品";
            default -> "商品 " + nullToEmpty(productName) + " 暂未维护详细定位";
        };
    }

    private List<String> categoryScenarios(String category) {
        return switch (category) {
            case "EARPHONE" -> List.of("通勤", "学习", "会议", "轻运动");
            case "KEYBOARD" -> List.of("办公输入", "宿舍学习", "轻度游戏");
            case "WATCH" -> List.of("运动记录", "消息提醒", "健康监测");
            case "POWER_BANK" -> List.of("通勤补电", "旅行备用", "应急充电");
            default -> List.of("日常使用");
        };
    }

    private List<String> categoryIssues(String category) {
        return switch (category) {
            case "EARPHONE" -> List.of("连接不稳定", "单耳无声", "降噪体验不明显", "续航偏短");
            case "KEYBOARD" -> List.of("按键连击", "按键失灵", "灯光异常", "轴体异响");
            case "WATCH" -> List.of("定位偏差", "无法充电", "屏幕异常", "表带不适");
            case "POWER_BANK" -> List.of("无法充电", "异常发热", "容量体验偏差", "接口松动");
            default -> List.of("使用体验和商品质量问题");
        };
    }

    private List<String> categorySteps(String category) {
        return switch (category) {
            case "EARPHONE" -> List.of("重新配对设备", "确认电量和佩戴方式", "在另一台手机上测试", "录制异常现象视频");
            case "KEYBOARD" -> List.of("更换 USB 接口测试", "清理键帽和轴体", "关闭宏或连发设置", "录制连击/失灵视频");
            case "WATCH" -> List.of("检查定位和后台权限", "更新固件", "清洁充电触点", "在室外空旷区域测试");
            case "POWER_BANK" -> List.of("更换线材和适配器", "检查剩余电量", "避免边充边放", "测试不同输出口");
            default -> List.of("补充商品照片或视频", "说明使用场景", "结合订单状态申请售后");
        };
    }

    private String categoryAfterSaleAdvice(String category) {
        return switch (category) {
            case "EARPHONE" -> "体验不满意且商品完好时可按退货规则处理；单耳无声、断连、无法充电时建议换货检测。";
            case "KEYBOARD" -> "按键连击或失灵建议补充故障视频后走换货检测；单纯手感不适应需结合退货规则判断。";
            case "WATCH" -> "定位、充电或屏幕异常建议补充截图/视频后检测；佩戴不适需结合商品完好情况判断。";
            case "POWER_BANK" -> "无法充电或异常发热应停止使用并申请检测；容量争议建议补充充放电记录。";
            default -> "建议补充具体问题和凭证，再根据订单状态和平台规则判断退换货路径。";
        };
    }

    private List<String> splitText(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] parts = normalized.split("[；;\\n]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String item = part.trim();
            if (hasText(item)) {
                result.add(item);
            }
        }
        if (result.isEmpty()) {
            result.add(text.trim());
        }
        return result;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
