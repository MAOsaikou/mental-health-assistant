package com.itmao.aispringboot.service;

import com.itmao.aispringboot.DTO.command.UserRegisterCommandDTO;
import com.itmao.aispringboot.DTO.response.UserLoginResponseDTO;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceRegisterTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void rejectsMismatchedPasswords() {
        UserRegisterCommandDTO command = command("alice", "alice@test.com", "123456", "654321");
        BusinessException error = assertThrows(BusinessException.class, () -> userService.register(command));
        assertEquals("两次输入密码不一致", error.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void rejectsExistingUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        UserRegisterCommandDTO command = command("alice", "alice@test.com", "123456", "123456");
        BusinessException error = assertThrows(BusinessException.class, () -> userService.register(command));
        assertEquals("用户名已存在", error.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void rejectsExistingEmail() {
        when(userMapper.selectCount(any())).thenReturn(0L, 1L);
        UserRegisterCommandDTO command = command("alice", "alice@test.com", "123456", "123456");
        BusinessException error = assertThrows(BusinessException.class, () -> userService.register(command));
        assertEquals("邮箱已存在", error.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registersWhenUsernameAndEmailAreFree() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        UserRegisterCommandDTO command = command("alice", "alice@test.com", "123456", "123456");
        UserLoginResponseDTO.UserDetailResponseDTO created = userService.register(command);
        assertEquals("alice", created.getUsername());
        verify(userMapper).insert(any(User.class));
    }

    private static UserRegisterCommandDTO command(String username, String email, String password, String confirm) {
        UserRegisterCommandDTO dto = new UserRegisterCommandDTO();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setConfirmPassword(confirm);
        return dto;
    }
}
