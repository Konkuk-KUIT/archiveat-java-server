package com.archiveat.server.domain.user.service;

import com.archiveat.server.domain.user.dto.response.LoginResponse;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


//final 이거나 @NonNull 이 붙은 필드만 파라미터로 받는 생성자를 자동 생성
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    private static final String GRANT_TYPE = "Bearer";

    @Transactional
    public IssuedTokens login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        user.updateRefreshToken(refreshToken);

        //TODO 저장하는 거 맞는지 물어보기
        userRepository.save(user);

        return new IssuedTokens(accessToken, refreshToken);
    }

    @Transactional
    public boolean checkEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public IssuedTokens signupAndLogin(String email, String password, String nickname) {

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already exists");
        }

        String encoded = passwordEncoder.encode(password);
        User savedUser = userRepository.save(new User(email, encoded, nickname));

        String accessToken = jwtUtil.generateAccessToken(savedUser.getId());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        savedUser.updateRefreshToken(refreshToken);
        userRepository.save(savedUser);

        return new IssuedTokens(accessToken, refreshToken);
    }

    @Transactional
    public IssuedTokens reissueTokensByRefresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("Refresh token missing");
        }

        jwtUtil.validate(refreshToken);
        Long userId = jwtUtil.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // DB에 저장된 refresh와 일치해야 함
        if (!user.matchesRefreshToken(refreshToken)) {
            throw new IllegalStateException("Refresh token invalid");
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId);

        // Rotation: refresh도 새로 발급해서 교체 (보안↑)
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        user.updateRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new IssuedTokens(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        user.clearRefreshToken();
        userRepository.save(user);
    }

    // 서비스 내부용 토큰 페어
    public record IssuedTokens(String accessToken, String refreshToken) {}

}
