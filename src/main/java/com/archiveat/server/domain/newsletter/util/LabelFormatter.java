package com.archiveat.server.domain.newsletter.util;

import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;

/**
 * Label 문자열 생성 유틸리티
 * 
 * DepthType + PerspectiveType 조합으로 Label 문자열 생성
 */
public class LabelFormatter {

    /**
     * DepthType과 PerspectiveType을 조합하여 Label 문자열 반환
     * 
     * @param depthType       Light 또는 Deep
     * @param perspectiveType Now 또는 Future
     * @return label 문자열 ("영감수집", "집중탐구", "성장한입", "관점확장")
     */
    public static String formatLabel(DepthType depthType, PerspectiveType perspectiveType) {
        if (depthType == null || perspectiveType == null) {
            return null;
        }

        // Light + Now = 영감수집
        if (depthType == DepthType.LIGHT && perspectiveType == PerspectiveType.NOW) {
            return "영감수집";
        }
        // Deep + Now = 집중탐구
        else if (depthType == DepthType.DEEP && perspectiveType == PerspectiveType.NOW) {
            return "집중탐구";
        }
        // Light + Future = 성장한입
        else if (depthType == DepthType.LIGHT && perspectiveType == PerspectiveType.FUTURE) {
            return "성장한입";
        }
        // Deep + Future = 관점확장
        else if (depthType == DepthType.DEEP && perspectiveType == PerspectiveType.FUTURE) {
            return "관점확장";
        }

        return null;
    }
}
