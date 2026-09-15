package com.itmao.aispringboot.util;

import java.util.ArrayList;
import java.util.List;

public class CrisisSupport {
    public static final String NATIONAL_HOTLINE = "全国统一心理援助热线 12356";
    public static final String BACKUP_HOTLINE = "希望24小时热线 400-161-9995";
    public static final String HOTLINE = NATIONAL_HOTLINE;

    public static final String FIXED_REPLY =
            "听到这些，我很担心你。你现在的感受是真的，也值得被认真对待。\n\n"
                    + "我是小光，不是医生，没法替代专业帮助。请现在联系："
                    + NATIONAL_HOTLINE
                    + "；也可以拨打" + BACKUP_HOTLINE + "。\n\n"
                    + "如果你已经准备实施、身边有可能伤害自己的东西，或此刻无法保证安全，"
                    + "请立即拨打 120 / 110，去有人的地方，并请一位信任的人现在陪着你。\n\n"
                    + "如果你愿意，可以只回复我：你现在安全吗？身边有人吗？我会继续陪着听。";

    public static final List<String> KEYWORDS = List.of(
            "不想活", "自杀", "结束生命", "自残", "没有活下去的意义",
            "想死", "去死", "了结自己", "结束自己", "割腕", "跳楼", "活不下去",
            "不如死了", "一了百了", "永远睡着", "再也不醒", "从这个世界消失",
            "告别这个世界", "撑不下去了", "准备遗书", "吞药自杀", "结束这一切"
    );

    public static boolean isCrisis(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("[\\s，。！？、,.!?；;：:]+", "");
        for (String word : KEYWORDS) {
            if (normalized.contains(word)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> splitChunks(String text, int size) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        int step = Math.max(1, size);
        for (int i = 0; i < text.length(); i += step) {
            chunks.add(text.substring(i, Math.min(text.length(), i + step)));
        }
        return chunks;
    }
}
