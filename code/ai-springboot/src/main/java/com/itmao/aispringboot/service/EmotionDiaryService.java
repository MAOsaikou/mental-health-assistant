package com.itmao.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itmao.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import com.itmao.aispringboot.DTO.query.EmotionDiaryQueryDTO;
import com.itmao.aispringboot.DTO.response.EmotionDiaryResponseDTO;
import com.itmao.aispringboot.entity.EmotionDiary;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.mapper.EmotionDiaryMapper;
import com.itmao.aispringboot.mapper.UserMapper;
import com.itmao.aispringboot.util.EmotionAnalyzer;
import com.itmao.aispringboot.util.PrivacyMask;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmotionDiaryService {
    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;
    @Resource
    private UserMapper userMapper;

    public EmotionDiary add(Long userId, EmotionDiaryCreateDTO dto) {
        LocalDate diaryDate = dto.getDiaryDate() != null ? dto.getDiaryDate() : LocalDate.now();
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId).eq(EmotionDiary::getDiaryDate, diaryDate);
        if (emotionDiaryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("今天已经记过一笔了。想多说几句的话，去找小光聊聊。");
        }
        String analysis = EmotionAnalyzer.analyzeText(
                (dto.getEmotionTriggers() == null ? "" : dto.getEmotionTriggers()) + " " + (dto.getDiaryContent() == null ? "" : dto.getDiaryContent()),
                dto.getMoodScore()
        );
        EmotionDiary diary = EmotionDiary.builder()
                .userId(userId)
                .diaryDate(diaryDate)
                .moodScore(dto.getMoodScore())
                .dominantEmotion(dto.getDominantEmotion())
                .emotionTriggers(dto.getEmotionTriggers())
                .diaryContent(dto.getDiaryContent())
                .sleepQuality(dto.getSleepQuality())
                .stressLevel(dto.getStressLevel())
                .aiEmotionAnalysis(analysis)
                .aiAnalysisUpdatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        emotionDiaryMapper.insert(diary);
        return diary;
    }

    public List<EmotionDiaryResponseDTO> listMine(Long userId) {
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
                .orderByDesc(EmotionDiary::getDiaryDate)
                .last("limit 30");
        return emotionDiaryMapper.selectList(wrapper).stream()
                .map(diary -> toUserResponse(diary, false))
                .toList();
    }

    public EmotionDiaryResponseDTO getMine(Long userId, Long id) {
        EmotionDiary diary = emotionDiaryMapper.selectById(id);
        if (diary == null || !diary.getUserId().equals(userId)) {
            throw new BusinessException("这篇日记找不到了");
        }
        return toUserResponse(diary, false);
    }

    public List<Map<String, Object>> trend(Long userId, int days) {
        int span = Math.min(31, Math.max(7, days));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(span - 1L);
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
                .ge(EmotionDiary::getDiaryDate, start)
                .le(EmotionDiary::getDiaryDate, end);
        Map<LocalDate, EmotionDiary> byDate = new HashMap<>();
        for (EmotionDiary diary : emotionDiaryMapper.selectList(wrapper)) {
            byDate.put(diary.getDiaryDate(), diary);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < span; i++) {
            LocalDate date = start.plusDays(i);
            EmotionDiary diary = byDate.get(date);
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.toString());
            point.put("moodScore", diary == null ? null : diary.getMoodScore());
            point.put("dominantEmotion", diary == null ? null : diary.getDominantEmotion());
            points.add(point);
        }
        return points;
    }

    public Page<Map<String, Object>> adminPage(EmotionDiaryQueryDTO query, boolean reveal) {
        Page<EmotionDiary> page = new Page<>(query.resolveCurrent(), query.resolveSize());
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getUserId() != null, EmotionDiary::getUserId, query.getUserId());
        if (query.getMoodScreRange() != null) {
            String[] range = query.getMoodScreRange().split("-");
            if (range.length == 2) {
                wrapper.ge(EmotionDiary::getMoodScore, Integer.parseInt(range[0]))
                        .le(EmotionDiary::getMoodScore, Integer.parseInt(range[1]));
            }
        }
        wrapper.orderByDesc(EmotionDiary::getDiaryDate);
        Page<EmotionDiary> entityPage = emotionDiaryMapper.selectPage(page, wrapper);
        Page<Map<String, Object>> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(diary -> toAdminMap(diary, reveal)).toList());
        return result;
    }

    public Map<String, Object> adminDetail(Long id, boolean reveal) {
        EmotionDiary diary = emotionDiaryMapper.selectById(id);
        if (diary == null) {
            throw new BusinessException("日记不存在");
        }
        return toAdminMap(diary, reveal);
    }

    public void delete(Long id) {
        if (emotionDiaryMapper.selectById(id) == null) {
            throw new BusinessException("日记不存在");
        }
        emotionDiaryMapper.deleteById(id);
    }

    private EmotionDiaryResponseDTO toUserResponse(EmotionDiary diary, boolean mask) {
        return EmotionDiaryResponseDTO.builder()
                .id(diary.getId())
                .diaryDate(diary.getDiaryDate())
                .moodScore(diary.getMoodScore())
                .dominantEmotion(diary.getDominantEmotion())
                .emotionTriggers(mask ? PrivacyMask.mask(diary.getEmotionTriggers()) : diary.getEmotionTriggers())
                .diaryContent(mask ? PrivacyMask.mask(diary.getDiaryContent()) : diary.getDiaryContent())
                .sleepQuality(diary.getSleepQuality())
                .stressLevel(diary.getStressLevel())
                .aiEmotionAnalysis(diary.getAiEmotionAnalysis())
                .createdAt(diary.getCreatedAt())
                .contentMasked(mask)
                .build();
    }

    private Map<String, Object> toAdminMap(EmotionDiary diary, boolean reveal) {
        User user = userMapper.selectById(diary.getUserId());
        Map<String, Object> map = new HashMap<>();
        map.put("id", diary.getId());
        map.put("userId", diary.getUserId());
        map.put("username", user != null ? user.getUsername() : "");
        map.put("nickname", user != null ? user.getDisplayName() : "");
        map.put("diaryDate", diary.getDiaryDate());
        map.put("moodScore", diary.getMoodScore());
        map.put("dominantEmotion", diary.getDominantEmotion());
        map.put("emotionTriggers", reveal ? diary.getEmotionTriggers() : PrivacyMask.mask(diary.getEmotionTriggers()));
        map.put("diaryContent", reveal ? diary.getDiaryContent() : PrivacyMask.mask(diary.getDiaryContent()));
        map.put("sleepQuality", diary.getSleepQuality());
        map.put("stressLevel", diary.getStressLevel());
        map.put("aiEmotionAnalysis", diary.getAiEmotionAnalysis());
        map.put("createdAt", diary.getCreatedAt());
        map.put("updatedAt", diary.getUpdatedAt());
        map.put("contentMasked", !reveal);
        return map;
    }
}
