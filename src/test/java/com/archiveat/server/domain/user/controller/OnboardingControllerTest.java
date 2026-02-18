package com.archiveat.server.domain.user.controller;

import com.archiveat.server.domain.user.dto.request.NicknameRequest;
import com.archiveat.server.domain.user.dto.request.OnboardingInfoRequest;
import com.archiveat.server.domain.user.dto.response.OnboardingMetadataResponse;
import com.archiveat.server.domain.user.service.OnboardingService;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.EmploymentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터를 비활성화하여 컨트롤러 단위 테스트에 집중
@Import(OnboardingControllerTest.TestConfig.class) // 아래 작성한 가짜 유저 주입 설정 임포트
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnboardingService onboardingService;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                    return 1L; // 모든 테스트에서 userId를 1L로 고정 주입
                }
            });
        }
    }

    @Test
    @DisplayName("닉네임 수정 성공: 200 OK를 반환하고 서비스를 호출한다")
    void editNickname_Success() throws Exception {
        // given
        NicknameRequest request = new NicknameRequest("새닉네임");

        // when & then
        mockMvc.perform(post("/user/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(print());

        // 서비스가 테스트용 ID(1L)와 함께 정확히 호출되었는지 검증
        verify(onboardingService).editNickname(eq(1L), eq("새닉네임"));
    }

    @Test
    @DisplayName("온보딩 메타데이터 조회 성공: 200 OK와 데이터를 반환한다")
    void getOnboardingMetadata_Success() throws Exception {
        // given
        OnboardingMetadataResponse response = new OnboardingMetadataResponse(
                List.of("STUDENT"), List.of("LIGHT"), List.of()
        );
        given(onboardingService.getOnboardingMetadata()).willReturn(response);

        // when & then
        mockMvc.perform(get("/user/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employmentTypes[0]").value("STUDENT"))
                .andDo(print());
    }

    @Test
    @DisplayName("온보딩 정보 제출 성공: 복잡한 DTO를 올바르게 전송한다")
    void submitOnboardingInfo_Success() throws Exception {
        // given
        OnboardingInfoRequest.AvailabilityRequest availability = new OnboardingInfoRequest.AvailabilityRequest(
                DepthType.LIGHT, DepthType.DEEP, DepthType.LIGHT, DepthType.DEEP
        );
        OnboardingInfoRequest.CategoryInterestRequest interest = new OnboardingInfoRequest.CategoryInterestRequest(1L, List.of(10L, 11L));
        OnboardingInfoRequest request = new OnboardingInfoRequest(EmploymentType.STUDENT, availability, List.of(interest));

        // when & then
        mockMvc.perform(post("/user/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(print());

        // 서비스 호출 인자값 검증 (ID가 1L로 잘 들어갔는지 확인)
        verify(onboardingService).submitOnboardingInfo(eq(1L), any(OnboardingInfoRequest.class));
    }

    @Test
    @DisplayName("온보딩 정보 제출 실패: 필수 값 누락 시 400 에러를 반환한다")
    void submitOnboardingInfo_Fail_Validation() throws Exception {
        // given - EmploymentType이 null인 부적절한 요청
        OnboardingInfoRequest request = new OnboardingInfoRequest(null, null, List.of());

        // when & then
        mockMvc.perform(post("/user/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andDo(print());
    }
}