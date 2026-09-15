package com.itmao.aispringboot.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.Map;

public class EmotionAnalyzer {

    public static String analyzeText(String text, Integer moodScore) {
        String content = text == null ? "" : text;
        int score = moodScore != null ? moodScore * 10 : 50;
        boolean crisis = CrisisSupport.isCrisis(content);
        boolean negative = crisis
                || containsAny(content, List.of("难过", "焦虑", "痛苦", "崩溃", "绝望", "头疼", "压力", "迷茫", "孤独", "害怕"))
                || (moodScore != null && moodScore <= 4);
        int riskLevel = 0;
        if (crisis) {
            riskLevel = 3;
        } else if (negative && (moodScore == null || moodScore <= 3)) {
            riskLevel = 2;
        } else if (negative) {
            riskLevel = 1;
        }
        String emotion = crisis ? "很痛苦" : (negative ? "低落" : (moodScore != null && moodScore >= 8 ? "开心" : "平静"));
        String suggestion;
        String riskDescription;
        List<String> improvements;
        if (crisis) {
            suggestion = "先让自己安全。专业的人可以帮你，小光会陪着听。";
            riskDescription = "我很担心你。请立刻联系身边可信任的人；有立即危险请拨打 120 或 110，也可拨打全国心理援助热线 12356。";
            improvements = List.of("先联系身边一个信得过的人", "拨打全国心理援助热线 12356", "有立即危险请打 120 或 110");
        } else if (riskLevel >= 2) {
            suggestion = "你现在可能挺难受的，不必一个人硬撑。";
            riskDescription = "需要的话可以拨打全国心理援助热线 12356。小光也在这里。";
            improvements = List.of("先喝口水，停一下", "把此刻的感受写下来", "和信任的人说一声");
        } else if (negative) {
            suggestion = "先照顾好自己，把感受说出来就是很好的一步。";
            riskDescription = "慢慢来就好，小光在。";
            improvements = List.of("深呼吸放松一下", "写写今天的心情", "和信任的人聊聊");
        } else {
            suggestion = "今天还不错的话，就让这份感觉多停一会儿。";
            riskDescription = "慢慢来就好，小光在。";
            improvements = List.of("保持规律作息", "出去走走", "记下一点开心的事");
        }
        JSONObject json = JSONUtil.createObj()
                .set("icon", crisis ? "🤍" : (negative ? "😔" : "😊"))
                .set("label", emotion)
                .set("keywords", List.of())
                .set("riskLevel", riskLevel)
                .set("timestamp", System.currentTimeMillis())
                .set("isNegative", negative)
                .set("suggestion", suggestion)
                .set("emotionScore", Math.min(100, Math.max(0, score)))
                .set("primaryEmotion", emotion)
                .set("riskDescription", riskDescription)
                .set("improvementSuggestions", improvements);
        return json.toString();
    }

    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return JSONUtil.parseObj(analyzeText("", 5));
        }
        return JSONUtil.parseObj(json);
    }

    private static boolean containsAny(String content, List<String> words) {
        for (String word : words) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
