package com.archiveat.server.domain.user.service;

import com.archiveat.server.domain.user.dto.response.LoginResponse;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.response.ApiResponse;
import com.archiveat.server.global.common.response.SuccessCode;
import com.archiveat.server.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountNotFoundException;
import java.util.Optional;

//final 이거나 @NonNull 이 붙은 필드만 파라미터로 받는 생성자를 자동 생성
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    private static final String GRANT_TYPE = "Bearer";

    @Transactional
    public LoginResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        return new LoginResponse(accessToken, GRANT_TYPE);
    }

    @Transactional
    public boolean checkEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public LoginResponse signupAndLogin(String email, String password, String nickname) {

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already exists");
        }

        String encoded = passwordEncoder.encode(password);
        User savedUser = userRepository.save(new User(email, encoded, nickname));

        String accessToken = jwtUtil.generateAccessToken(savedUser.getId());

        return new LoginResponse(accessToken, GRANT_TYPE);
    }

}
