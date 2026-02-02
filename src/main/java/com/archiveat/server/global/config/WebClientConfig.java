package com.archiveat.server.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

        @Value("${python.server.base-url:http://python-server:8000}")
        private String pythonServerBaseUrl;

        @Value("${python.server.timeout.connect:10000}")
        private int connectTimeout;

        @Value("${python.server.timeout.response:600000}")
        private int responseTimeout;

        /**
         * Python 서버 통신용 WebClient 설정
         * 
         * - Connection Timeout: 10초
         * - Response Timeout: 600초 (10분 - LLM + Whisper 처리 시간)
         * - Retry: WebClient Retry 설정으로 처리 (3회)
         */
        @Bean
        public WebClient pythonWebClient() {
                HttpClient httpClient = HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                                .responseTimeout(Duration.ofMillis(responseTimeout))
                                .doOnConnected(
                                                conn -> conn.addHandlerLast(new ReadTimeoutHandler(responseTimeout,
                                                                TimeUnit.MILLISECONDS))
                                                                .addHandlerLast(new WriteTimeoutHandler(connectTimeout,
                                                                                TimeUnit.MILLISECONDS)));

                return WebClient.builder()
                                .baseUrl(pythonServerBaseUrl)
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .build();
        }
}
