package com.itmao.aispringboot.mapper;

import com.itmao.aispringboot.DTO.response.DailyAggDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AnalyticsMapper {

    @Select("SELECT AVG(mood_score) FROM emotion_diary WHERE mood_score IS NOT NULL")
    BigDecimal avgMoodScore();

    @Select("""
            SELECT DATE_FORMAT(diary_date, '%Y-%m-%d') AS date,
                   COUNT(*) AS recordCount,
                   AVG(mood_score) AS avgMoodScore
            FROM emotion_diary
            WHERE diary_date BETWEEN #{start} AND #{end}
            GROUP BY diary_date
            """)
    List<DailyAggDTO> emotionTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
            SELECT AVG(TIMESTAMPDIFF(MINUTE, s.started_at, COALESCE(last_msg.last_at, s.started_at)))
            FROM consultation_session s
            LEFT JOIN (
                SELECT session_id, MAX(created_at) AS last_at
                FROM consultation_message
                GROUP BY session_id
            ) last_msg ON last_msg.session_id = s.id
            """)
    BigDecimal avgDurationMinutes();

    @Select("""
            SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS date,
                   COUNT(*) AS sessionCount,
                   COUNT(DISTINCT user_id) AS userCount
            FROM consultation_session
            WHERE started_at BETWEEN #{start} AND #{end}
            GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')
            """)
    List<DailyAggDTO> consultationDaily(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS newUsers
            FROM user
            WHERE created_at BETWEEN #{start} AND #{end}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<DailyAggDTO> newUsersDaily(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE_FORMAT(diary_date, '%Y-%m-%d') AS date, COUNT(DISTINCT user_id) AS diaryUsers
            FROM emotion_diary
            WHERE diary_date BETWEEN #{start} AND #{end}
            GROUP BY diary_date
            """)
    List<DailyAggDTO> diaryUsersDaily(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
            SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS date, COUNT(DISTINCT user_id) AS consultationUsers
            FROM consultation_session
            WHERE started_at BETWEEN #{start} AND #{end}
            GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')
            """)
    List<DailyAggDTO> consultationUsersDaily(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
