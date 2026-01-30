package com.archiveat.server.global.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HomeTabType {
    ALL("전체", "수집한 자료를 기반으로 발행된, 나만의 지식 뉴스레터"),
    INSPIRATION("영감수집", "잠깐의 틈을 채워줄, 현재의 관심사와 맞닿은 인사이트"),
    DEEP_DIVE("집중탐구", "관심 주제를 깊이 파고들어, 온전히 내 것으로 만드는 시간"),
    GROWTH("성장한입", "매일 조금씩 지식을 채우며 당신의 성장을 체감해 보세요"),
    VIEW_EXPANSION("관점확장", "새로운 시각으로 세상을 바라볼 수 있는 깊이 있는 통찰");

    private final String label;
    private final String subMessage;
}
