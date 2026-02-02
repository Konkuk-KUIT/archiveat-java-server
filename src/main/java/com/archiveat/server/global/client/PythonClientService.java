package com.archiveat.server.global.client;

import com.archiveat.server.domain.newsletter.dto.request.SummarizeYoutubeRequest;
import com.archiveat.server.domain.newsletter.dto.request.SummarizeNaverNewsRequest;
import com.archiveat.server.domain.newsletter.dto.response.PythonSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonClientService {

        private final WebClient pythonWebClient;

        /**
         * YouTube URL을 Python 서버로 전송하여 요약 결과 받아오기
         * 
         * @param url YouTube URL
         * @return CompletableFuture<PythonSummaryResponse> 비동기 응답
         * @throws RuntimeException Python 서버 호출 실패 시
         */
        public CompletableFuture<PythonSummaryResponse> requestYouTubeSummary(String url) {
                log.info("Requesting YouTube summary from Python server: {}", url);
                long startTime = System.currentTimeMillis();

                SummarizeYoutubeRequest request = new SummarizeYoutubeRequest(url);

                return pythonWebClient.post()
                                .uri("/api/v1/summarize/youtube")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(PythonSummaryResponse.class)
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                                .maxBackoff(Duration.ofSeconds(5))
                                                .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest))
                                                .doBeforeRetry(retrySignal -> log.warn(
                                                                "Retrying Python server request (attempt {}): {}",
                                                                retrySignal.totalRetries() + 1,
                                                                retrySignal.failure().getMessage())))
                                .doOnSuccess(response -> {
                                        long duration = System.currentTimeMillis() - startTime;
                                        log.info("Successfully received YouTube summary from Python server in {}ms: {}",
                                                        duration, url);
                                })
                                .doOnError(error -> {
                                        long duration = System.currentTimeMillis() - startTime;
                                        log.error("Failed to get YouTube summary from Python server after {}ms: {}",
                                                        duration, url, error);
                                })
                                .onErrorResume(WebClientResponseException.class, ex -> {
                                        log.error("Python server returned error status {}: {}",
                                                        ex.getStatusCode(), ex.getResponseBodyAsString());
                                        return Mono.error(new RuntimeException(
                                                        "Python server error: " + ex.getStatusCode() + " - "
                                                                        + ex.getResponseBodyAsString(),
                                                        ex));
                                })
                                .onErrorResume(Exception.class, ex -> {
                                        log.error("Unexpected error calling Python server", ex);
                                        return Mono.error(
                                                        new RuntimeException(
                                                                        "Failed to communicate with Python server: "
                                                                                        + ex.getMessage(),
                                                                        ex));
                                })
                                .toFuture();
        }

        /**
         * 일반 텍스트 콘텐츠 요약 요청 (향후 확장용)
         * 
         * @param title   콘텐츠 제목
         * @param content 콘텐츠 본문
         * @return CompletableFuture<PythonSummaryResponse> 비동기 응답
         */
        public CompletableFuture<PythonSummaryResponse> requestGenericSummary(String title, String content) {
                log.info("Requesting generic content summary from Python server: {}", title);

                return pythonWebClient.post()
                                .uri("/api/v1/summarize/generic")
                                .bodyValue(new GenericSummaryRequest(title, content))
                                .retrieve()
                                .bodyToMono(PythonSummaryResponse.class)
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                                .maxBackoff(Duration.ofSeconds(5))
                                                .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest)))
                                .doOnSuccess(response -> log
                                                .info("Successfully received generic content summary from Python server: {}",
                                                                title))
                                .doOnError(error -> log.error(
                                                "Failed to get generic content summary from Python server: {}", title,
                                                error))
                                .toFuture();
        }

        /**
         * 네이버 뉴스 또는 일반 웹 콘텐츠 요약 요청
         * 
         * @param url      네이버 뉴스 또는 일반 웹 URL
         * @param userMemo 사용자 메모 (분류 우선순위에 활용, 선택사항)
         * @return CompletableFuture<PythonSummaryResponse> 비동기 응답
         */
        public CompletableFuture<PythonSummaryResponse> requestNaverNewsSummary(String url, String userMemo) {
                log.info("Requesting Naver news summary from Python server: {}", url);
                if (userMemo != null && !userMemo.isEmpty()) {
                        log.info("User memo provided: {}", userMemo);
                }

                SummarizeNaverNewsRequest request = new SummarizeNaverNewsRequest(url, userMemo);

                return pythonWebClient.post()
                                .uri("/api/v1/summarize/naver-news")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(PythonSummaryResponse.class)
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                                .maxBackoff(Duration.ofSeconds(5))
                                                .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest)))
                                .doOnSuccess(response -> log
                                                .info("Successfully received Naver news summary from Python server: {}",
                                                                url))
                                .doOnError(error -> log.error("Failed to get Naver news summary from Python server: {}",
                                                url,
                                                error))
                                .toFuture();
        }

        // 내부 DTO
        private record GenericSummaryRequest(String title, String content) {
        }
}
