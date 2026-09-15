package com.itmao.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itmao.aispringboot.DTO.response.SessionMessageCountDTO;
import com.itmao.aispringboot.entity.ConsultationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConsultationMessageMapper extends BaseMapper<ConsultationMessage> {

    @Select("""
            SELECT COUNT(*) FROM consultation_message m
            INNER JOIN consultation_session s ON m.session_id = s.id
            WHERE s.user_id = #{userId} AND m.sender_type = 1
            """)
    int countUserTurns(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT session_id AS sessionId, COUNT(*) AS messageCount
            FROM consultation_message
            WHERE session_id IN
            <foreach collection="sessionIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY session_id
            </script>
            """)
    List<SessionMessageCountDTO> countBySessionIds(@Param("sessionIds") List<Long> sessionIds);

    @Select("""
            <script>
            SELECT m.id, m.session_id, m.sender_type, m.message_type, m.content, m.emotion_tag, m.ai_model, m.created_at
            FROM consultation_message m
            INNER JOIN (
                SELECT session_id, MAX(id) AS max_id
                FROM consultation_message
                WHERE session_id IN
                <foreach collection="sessionIds" item="id" open="(" separator="," close=")">#{id}</foreach>
                GROUP BY session_id
            ) latest ON m.id = latest.max_id
            </script>
            """)
    List<ConsultationMessage> findLastBySessionIds(@Param("sessionIds") List<Long> sessionIds);
}
