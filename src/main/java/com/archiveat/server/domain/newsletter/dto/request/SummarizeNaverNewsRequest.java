package com.archiveat.server.domain.newsletter.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SummarizeNaverNewsRequest {
    private String url;
    private String userMemo; // 사용자 메모 (분류 우선순위에 활용)

    public SummarizeNaverNewsRequest(String url, String userMemo) {
        this.url = url;
        this.userMemo = userMemo;
    }
}
