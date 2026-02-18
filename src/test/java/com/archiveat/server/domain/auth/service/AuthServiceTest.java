package com.archiveat.server.domain.auth.service;

import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.response.ErrorCode;
import com.archiveat.server.global.exception.CustomException;
import com.archiveat.server.global.jwt.JwtUtil;
import com.archiveat.server.global.security.TokenHashUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenHashUtil tokenHashUtil;

    @Nested
    @DisplayName("로그인 테스트")
    class Login {

        @Test
        @DisplayName("성공: 이메일과 비밀번호가 일치하면 토큰을 발급한다")
        void login_Success() {
            // given
            String email = "test@example.com";
            String password = "password123";
            User user = new User(email, "encoded_password", "nickname");
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);
            given(jwtUtil.generateAccessToken(1L)).willReturn("access_token");
            given(jwtUtil.generateRefreshToken(1L)).willReturn("refresh_token");
            given(tokenHashUtil.sha256Hex("refresh_token")).willReturn("hashed_refresh_token");

            // when
            AuthService.IssuedTokens result = authService.login(email, password);

            // then
            assertThat(result.accessToken()).isEqualTo("access_token");
            assertThat(result.refreshToken()).isEqualTo("refresh_token");
            assertThat(user.getRefreshTokenHash()).isEqualTo("hashed_refresh_token");
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("실패: 이메일이 존재하지 않으면 LOGIN_FAILED 예외가 발생한다")
        void login_Fail_UserNotFound() {
            // given
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login("wrong@example.com", "password"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
        }

        @Test
        @DisplayName("실패: 비밀번호가 일치하지 않으면 LOGIN_FAILED 예외가 발생한다")
        void login_Fail_PasswordMismatch() {
            // given
            String email = "test@example.com";
            User user = new User(email, "encoded_password", "nickname");
            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            // anyString() 대신 구체적인 값으로 stubbing 하여 테스트의 엄격함 강화
            given(passwordEncoder.matches("wrong_password", "encoded_password")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(email, "wrong_password"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
        }
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class Signup {

        @Test
        @DisplayName("성공: 중복되지 않은 이메일로 가입 시 유저를 저장하고 토큰을 발급한다")
        void signup_Success() {
            // given
            String email = "new@example.com";
            String password = "password123";
            String nickname = "newbie";

            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(password)).willReturn("encoded_password");

            // save 호출 시 반환될 유저 객체 (ID 주입)
            User savedUser = new User(email, "encoded_password", nickname);
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            given(jwtUtil.generateAccessToken(1L)).willReturn("access");
            given(jwtUtil.generateRefreshToken(1L)).willReturn("refresh");
            given(tokenHashUtil.sha256Hex("refresh")).willReturn("hashed_refresh");

            // when
            AuthService.IssuedTokens result = authService.signupAndLogin(email, password, nickname);

            // then
            assertThat(result.accessToken()).isEqualTo("access");
            assertThat(result.refreshToken()).isEqualTo("refresh");
            assertThat(savedUser.getRefreshTokenHash()).isEqualTo("hashed_refresh");
            verify(userRepository, atLeastOnce()).save(any(User.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 이메일이면 EMAIL_ALREADY_EXISTS 예외가 발생한다")
        void signup_Fail_DuplicateEmail() {
            // given
            given(userRepository.existsByEmail(anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signupAndLogin("exist@example.com", "p", "n"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class Reissue {

        @Test
        @DisplayName("성공: 유효한 Refresh 토큰이면 새로운 토큰 페어를 발급한다 (Rotation)")
        void reissue_Success() {
            // given
            String oldRefreshToken = "old_refresh";
            Long userId = 1L;
            User user = new User("test@example.com", "pw", "nick");
            ReflectionTestUtils.setField(user, "id", userId);
            ReflectionTestUtils.setField(user, "refreshTokenHash", "old_hash");

            given(jwtUtil.getUserId(oldRefreshToken)).willReturn(userId);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(tokenHashUtil.sha256Hex(oldRefreshToken)).willReturn("old_hash");
            given(jwtUtil.generateAccessToken(userId)).willReturn("new_access");
            given(jwtUtil.generateRefreshToken(userId)).willReturn("new_refresh");
            given(tokenHashUtil.sha256Hex("new_refresh")).willReturn("new_hash");

            // when
            AuthService.IssuedTokens result = authService.reissueTokensByRefresh(oldRefreshToken);

            // then
            assertThat(result.accessToken()).isEqualTo("new_access");
            assertThat(result.refreshToken()).isEqualTo("new_refresh");
            assertThat(user.getRefreshTokenHash()).isEqualTo("new_hash");
            verify(userRepository).save(user);
            verify(jwtUtil).validate(oldRefreshToken);
        }

        @Test
        @DisplayName("실패: DB에 저장된 해시값과 일치하지 않으면 REFRESH_TOKEN_INVALID 예외가 발생한다")
        void reissue_Fail_HashMismatch() {
            // given
            String incomingToken = "fake_refresh";
            User user = new User("t@e.com", "p", "n");
            ReflectionTestUtils.setField(user, "refreshTokenHash", "real_hash");

            given(jwtUtil.getUserId(incomingToken)).willReturn(1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(tokenHashUtil.sha256Hex(incomingToken)).willReturn("wrong_hash");

            // when & then
            assertThatThrownBy(() -> authService.reissueTokensByRefresh(incomingToken))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class Logout {
        @Test
        @DisplayName("성공: 유저의 Refresh 토큰 해시를 제거한다")
        void logout_Success() {
            Long userId = 1L;
            User user = new User("t@e.com", "p", "n");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            authService.logout(userId);

            assertThat(user.getRefreshTokenHash()).isNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저는 로그아웃할 수 없다")
        void logout_Fail_UserNotFound() {
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.logout(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}