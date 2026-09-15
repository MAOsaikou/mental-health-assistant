package com.itmao.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itmao.aispringboot.DTO.response.DailyAggDTO;
import com.itmao.aispringboot.entity.ConsultationSession;
import com.itmao.aispringboot.entity.EmotionDiary;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.mapper.AnalyticsMapper;
import com.itmao.aispringboot.mapper.ConsultationSessionMapper;
import com.itmao.aispringboot.mapper.EmotionDiaryMapper;
import com.itmao.aispringboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DataAnalyticsService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;
    @Resource
    private ConsultationSessionMapper consultationSessionMapper;
    @Resource
    private AnalyticsMapper analyticsMapper;

    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("systemOverview", systemOverview());
        result.put("emotionTrend", emotionTrend(7));
        result.put("consultationStats", consultationStats(7));
        result.put("userActivity", userActivity(7));
        return result;
    }

    private Map<String, Object> systemOverview() {
        Map<String, Object> overview = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        overview.put("totalUsers", userMapper.selectCount(null));
        overview.put("activeUsers", userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getUpdatedAt, todayStart)));
        overview.put("totalDiaries", emotionDiaryMapper.selectCount(null));
        overview.put("todayNewDiaries", emotionDiaryMapper.selectCount(new LambdaQueryWrapper<EmotionDiary>().ge(EmotionDiary::getCreatedAt, todayStart)));
        overview.put("totalSessions", consultationSessionMapper.selectCount(null));
        overview.put("todayNewSessions", consultationSessionMapper.selectCount(new LambdaQueryWrapper<ConsultationSession>().ge(ConsultationSession::getStartedAt, todayStart)));
        overview.put("avgMoodScore", roundOne(analyticsMapper.avgMoodScore()));
        return overview;
    }

    private List<Map<String, Object>> emotionTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        Map<String, DailyAggDTO> byDate = indexByDate(analyticsMapper.emotionTrend(start, end));
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = start.plusDays(i);
            DailyAggDTO row = byDate.get(date.toString());
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("recordCount", row == null || row.getRecordCount() == null ? 0 : row.getRecordCount());
            item.put("avgMoodScore", row == null ? 0 : roundOne(row.getAvgMoodScore()));
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> consultationStats(int days) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", consultationSessionMapper.selectCount(null));
        stats.put("avgDurationMinutes", roundOne(analyticsMapper.avgDurationMinutes()));
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        Map<String, DailyAggDTO> byDate = indexByDate(analyticsMapper.consultationDaily(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)));
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            DailyAggDTO row = byDate.get(date.toString());
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("sessionCount", row == null || row.getSessionCount() == null ? 0 : row.getSessionCount());
            item.put("userCount", row == null || row.getUserCount() == null ? 0 : row.getUserCount());
            dailyTrend.add(item);
        }
        stats.put("dailyTrend", dailyTrend);
        return stats;
    }

    private List<Map<String, Object>> userActivity(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Map<String, DailyAggDTO> newUsers = indexByDate(analyticsMapper.newUsersDaily(start, end));
        Map<String, DailyAggDTO> diaryUsers = indexByDate(analyticsMapper.diaryUsersDaily(startDate, endDate));
        Map<String, DailyAggDTO> consultationUsers = indexByDate(analyticsMapper.consultationUsersDaily(start, end));
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String key = date.toString();
            long newCount = valueOrZero(newUsers.get(key), DailyAggDTO::getNewUsers);
            long diaryCount = valueOrZero(diaryUsers.get(key), DailyAggDTO::getDiaryUsers);
            long consultCount = valueOrZero(consultationUsers.get(key), DailyAggDTO::getConsultationUsers);
            Map<String, Object> item = new HashMap<>();
            item.put("date", key);
            item.put("newUsers", newCount);
            item.put("diaryUsers", diaryCount);
            item.put("consultationUsers", consultCount);
            item.put("activeUsers", newCount + diaryCount + consultCount);
            list.add(item);
        }
        return list;
    }

    private static Map<String, DailyAggDTO> indexByDate(List<DailyAggDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        return rows.stream()
                .filter(row -> row.getDate() != null)
                .collect(Collectors.toMap(DailyAggDTO::getDate, Function.identity(), (left, right) -> left));
    }

    private static long valueOrZero(DailyAggDTO row, Function<DailyAggDTO, Long> getter) {
        if (row == null) {
            return 0;
        }
        Long value = getter.apply(row);
        return value == null ? 0 : value;
    }

    private static double roundOne(Number value) {
        if (value == null) {
            return 0;
        }
        return Math.round(value.doubleValue() * 10) / 10.0;
    }
}
