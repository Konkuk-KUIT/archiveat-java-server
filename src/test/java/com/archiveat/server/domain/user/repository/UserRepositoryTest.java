package com.archiveat.server.domain.user.repository;

import com.archiveat.server.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 저장 및 식별자로 조회 성공")
    void saveAndFindById_Success() {
        // given
        User user = User.builder()
                .email("test@archiveat.com")
                .nickname("아카이빗")
                .build();

        // when
        User savedUser = userRepository.save(user);

        // then
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@archiveat.com");
        assertThat(foundUser.get().getNickname()).isEqualTo("아카이빗");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 빈 Optional을 반환한다")
    void findByEmail_ReturnEmpty_WhenNotFound() {
        // given
        String email = "notfound@test.com";

        // when
        Optional<User> foundUser = userRepository.findByEmail(email);

        // then
        assertThat(foundUser).isEmpty();
    }
}