package com.itmao.aispringboot.service;

import com.itmao.aispringboot.entity.ConsultationSession;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.mapper.ConsultationSessionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ConsultationSessionAccessTest {

    @Mock
    private ConsultationSessionMapper consultationSessionMapper;

    @Mock
    private ConsultationMessageService consultationMessageService;

    @InjectMocks
    private ConsultationSessionService consultationSessionService;

    @Test
    void ownerCanAccessOwnSession() {
        when(consultationSessionMapper.selectById(8L)).thenReturn(session(8L, 21L));
        ConsultationSession session = consultationSessionService.getAccessibleSession(8L, 21L, 1);
        assertEquals(8L, session.getId());
    }

    @Test
    void strangerCannotAccess() {
        when(consultationSessionMapper.selectById(8L)).thenReturn(session(8L, 21L));
        BusinessException error = assertThrows(BusinessException.class,
                () -> consultationSessionService.getAccessibleSession(8L, 99L, 1));
        assertEquals("这段聊天不属于你，没法查看", error.getMessage());
    }

    @Test
    void adminCanAccessOthersSession() {
        when(consultationSessionMapper.selectById(8L)).thenReturn(session(8L, 21L));
        ConsultationSession session = consultationSessionService.getAccessibleSession(8L, 1L, 2);
        assertEquals(21L, session.getUserId());
    }

    @Test
    void missingSessionIsNotFound() {
        when(consultationSessionMapper.selectById(8L)).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class,
                () -> consultationSessionService.getAccessibleSession(8L, 21L, 1));
        assertEquals("这段聊天找不到了", error.getMessage());
    }

    @Test
    void deletingSessionDeletesPrivateMessagesFirst() {
        when(consultationSessionMapper.selectById(8L)).thenReturn(session(8L, 21L));

        consultationSessionService.deleteSession(8L, 21L, 1);

        var order = inOrder(consultationMessageService, consultationSessionMapper);
        order.verify(consultationMessageService).deleteBySessionId(8L);
        order.verify(consultationSessionMapper).deleteById(8L);
    }

    private static ConsultationSession session(Long id, Long userId) {
        return ConsultationSession.builder().id(id).userId(userId).build();
    }
}
